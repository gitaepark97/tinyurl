package com.hugo.tinyurl.domain.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hugo.tinyurl.TestcontainersConfiguration;
import com.hugo.tinyurl.TinyurlApplication;
import com.hugo.tinyurl.domain.model.Role;
import com.hugo.tinyurl.domain.model.ShortUrl;
import com.hugo.tinyurl.domain.model.ShortUrlWithClickCount;
import com.hugo.tinyurl.domain.port.ClickCountRepository;
import com.hugo.tinyurl.domain.port.IdGenerator;
import com.hugo.tinyurl.domain.port.ShortUrlRepository;
import com.hugo.tinyurl.support.exception.BusinessException;
import com.hugo.tinyurl.support.exception.ErrorCode;
import com.hugo.tinyurl.support.page.Page;
import com.hugo.tinyurl.support.page.PageParam;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;

@SpringBootTest(classes = TinyurlApplication.class, webEnvironment = WebEnvironment.NONE)
@Import(TestcontainersConfiguration.class)
class ShortUrlFinderTest {

    @Autowired
    ShortUrlRepository shortUrlRepository;

    @Autowired
    ClickCountRepository clickCountRepository;

    @Autowired
    ShortUrlFinder shortUrlFinder;

    @Autowired
    ShortUrlManager shortUrlManager;

    @Autowired
    ShortKeyGenerator shortKeyGenerator;

    @Autowired
    IdGenerator idGenerator;

    @Autowired
    MeterRegistry meterRegistry;

    private final List<Long> createdShortUrlIds = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        createdShortUrlIds.forEach(clickCountRepository::deleteById);
        shortUrlRepository.deleteAllById(createdShortUrlIds);
        createdShortUrlIds.clear();
    }

    @Test
    void findsShortUrlByValidShortKey() {
        ShortUrl shortUrl = ShortUrlTestSupport.create(shortUrlManager, "https://example.com", createdShortUrlIds);
        double before = redirectCount("success");

        ShortUrl found = shortUrlFinder.find(shortUrl.shortKey());

        assertThat(found.id()).isEqualTo(shortUrl.id());
        assertThat(redirectCount("success")).isEqualTo(before + 1);
    }

    @Test
    void throwsNotFoundForUnknownShortKey() {
        double before = redirectCount("not_found");

        assertThatThrownBy(() -> shortUrlFinder.find("nope0000"))
            .isInstanceOf(BusinessException.class)
            .extracting(e -> ((BusinessException) e).errorCode())
            .isEqualTo(ErrorCode.NOT_FOUND);
        assertThat(redirectCount("not_found")).isEqualTo(before + 1);
    }

    @Test
    void throwsNotFoundForExpiredShortKey() {
        ShortUrl expired = ShortUrlTestSupport.createExpired(shortUrlRepository, shortKeyGenerator, idGenerator, createdShortUrlIds);
        double before = redirectCount("expired");

        assertThatThrownBy(() -> shortUrlFinder.find(expired.shortKey()))
            .isInstanceOf(BusinessException.class)
            .extracting(e -> ((BusinessException) e).errorCode())
            .isEqualTo(ErrorCode.NOT_FOUND);
        assertThat(redirectCount("expired")).isEqualTo(before + 1);
    }

    private double redirectCount(String result) {
        return meterRegistry.get("short_url.redirect").tag("result", result).counter().count();
    }

    @Test
    void returnsPageWithHasNextWhenMoreShortUrlsExist() {
        ShortUrlTestSupport.create(shortUrlManager, "https://example.com/1", createdShortUrlIds);
        ShortUrl second = ShortUrlTestSupport.create(shortUrlManager, "https://example.com/2", createdShortUrlIds);
        ShortUrl third = ShortUrlTestSupport.create(shortUrlManager, "https://example.com/3", createdShortUrlIds);

        Page<ShortUrlWithClickCount> page = shortUrlFinder.findAll(new PageParam(null, 2));

        assertThat(page.content()).extracting(view -> view.shortUrl().id())
            .containsExactly(third.id(), second.id());
        assertThat(page.hasNext()).isTrue();
    }

    @Test
    void continuesListFromGivenCursor() {
        ShortUrl first = ShortUrlTestSupport.create(shortUrlManager, "https://example.com/1", createdShortUrlIds);
        ShortUrl second = ShortUrlTestSupport.create(shortUrlManager, "https://example.com/2", createdShortUrlIds);
        ShortUrlTestSupport.create(shortUrlManager, "https://example.com/3", createdShortUrlIds);

        Page<ShortUrlWithClickCount> page = shortUrlFinder.findAll(new PageParam(second.id(), 20));

        assertThat(page.content()).extracting(view -> view.shortUrl().id()).contains(first.id());
        assertThat(page.content()).extracting(view -> view.shortUrl().id()).doesNotContain(second.id());
    }

    @Test
    void findsByIdIncludingExpired() {
        ShortUrl expired = ShortUrlTestSupport.createExpired(shortUrlRepository, shortKeyGenerator, idGenerator, createdShortUrlIds);

        ShortUrlWithClickCount found = shortUrlFinder.get(expired.id(), null, Role.ADMIN);

        assertThat(found.shortUrl().id()).isEqualTo(expired.id());
        assertThat(found.shortUrl().isExpired(LocalDateTime.now())).isTrue();
        assertThat(found.clickCount()).isZero();
    }

    @Test
    void throwsNotFoundForUnknownIdOnGet() {
        assertThatThrownBy(() -> shortUrlFinder.get(Long.MAX_VALUE, null, Role.ADMIN))
            .isInstanceOf(BusinessException.class)
            .extracting(e -> ((BusinessException) e).errorCode())
            .isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    void getAndFindAllReflectClickCount() {
        ShortUrl shortUrl = ShortUrlTestSupport.create(shortUrlManager, "https://example.com", createdShortUrlIds);
        clickCountRepository.increment(shortUrl.id());
        clickCountRepository.increment(shortUrl.id());

        ShortUrlWithClickCount found = shortUrlFinder.get(shortUrl.id(), null, Role.ADMIN);

        assertThat(found.clickCount()).isEqualTo(2L);
        assertThat(shortUrlFinder.findAll(new PageParam(null, 20)).content())
            .filteredOn(view -> view.shortUrl().id().equals(shortUrl.id()))
            .singleElement()
            .extracting(ShortUrlWithClickCount::clickCount)
            .isEqualTo(2L);
    }

    @Test
    void getAllowsOwnerToViewOwnUrl() {
        ShortUrl shortUrl = shortUrlManager.create(1L, "https://example.com", null, null);
        createdShortUrlIds.add(shortUrl.id());

        ShortUrlWithClickCount found = shortUrlFinder.get(shortUrl.id(), 1L, Role.MEMBER);

        assertThat(found.shortUrl().id()).isEqualTo(shortUrl.id());
    }

    @Test
    void getRejectsNonOwnerNonAdminWithForbidden() {
        ShortUrl shortUrl = shortUrlManager.create(1L, "https://example.com", null, null);
        createdShortUrlIds.add(shortUrl.id());

        assertThatThrownBy(() -> shortUrlFinder.get(shortUrl.id(), 2L, Role.MEMBER))
            .isInstanceOf(BusinessException.class)
            .extracting(e -> ((BusinessException) e).errorCode())
            .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    void getRejectsAnonymousUrlForNonAdmin() {
        ShortUrl shortUrl = ShortUrlTestSupport.create(shortUrlManager, "https://example.com", createdShortUrlIds);

        assertThatThrownBy(() -> shortUrlFinder.get(shortUrl.id(), 1L, Role.MEMBER))
            .isInstanceOf(BusinessException.class)
            .extracting(e -> ((BusinessException) e).errorCode())
            .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    void findAllByMemberReturnsOnlyOwnUrls() {
        ShortUrl owned = shortUrlManager.create(1L, "https://example.com/mine", null, null);
        createdShortUrlIds.add(owned.id());
        ShortUrl others = shortUrlManager.create(2L, "https://example.com/others", null, null);
        createdShortUrlIds.add(others.id());

        Page<ShortUrlWithClickCount> page = shortUrlFinder.findAllByMember(1L, new PageParam(null, 20));

        assertThat(page.content()).extracting(view -> view.shortUrl().id()).containsExactly(owned.id());
    }

}
