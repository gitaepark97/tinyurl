package com.hugo.tinyurl.domain.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hugo.tinyurl.TestcontainersConfiguration;
import com.hugo.tinyurl.TinyurlApplication;
import com.hugo.tinyurl.domain.model.ShortUrl;
import com.hugo.tinyurl.domain.model.ShortUrlWithClickCount;
import com.hugo.tinyurl.domain.port.ClickCountRepository;
import com.hugo.tinyurl.domain.port.ShortUrlRepository;
import com.hugo.tinyurl.support.exception.BusinessException;
import com.hugo.tinyurl.support.exception.ErrorCode;
import com.hugo.tinyurl.support.page.Page;
import com.hugo.tinyurl.support.page.PageParam;
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

        ShortUrl found = shortUrlFinder.find(shortUrl.shortKey());

        assertThat(found.id()).isEqualTo(shortUrl.id());
    }

    @Test
    void throwsNotFoundForUnknownShortKey() {
        assertThatThrownBy(() -> shortUrlFinder.find("nope0000"))
            .isInstanceOf(BusinessException.class)
            .extracting(e -> ((BusinessException) e).errorCode())
            .isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    void throwsNotFoundForExpiredShortKey() {
        ShortUrl expired = ShortUrlTestSupport.createExpired(shortUrlRepository, shortKeyGenerator, createdShortUrlIds);

        assertThatThrownBy(() -> shortUrlFinder.find(expired.shortKey()))
            .isInstanceOf(BusinessException.class)
            .extracting(e -> ((BusinessException) e).errorCode())
            .isEqualTo(ErrorCode.NOT_FOUND);
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
        ShortUrl expired = ShortUrlTestSupport.createExpired(shortUrlRepository, shortKeyGenerator, createdShortUrlIds);

        ShortUrlWithClickCount found = shortUrlFinder.get(expired.id());

        assertThat(found.shortUrl().id()).isEqualTo(expired.id());
        assertThat(found.shortUrl().isExpired(LocalDateTime.now())).isTrue();
        assertThat(found.clickCount()).isZero();
    }

    @Test
    void throwsNotFoundForUnknownIdOnGet() {
        assertThatThrownBy(() -> shortUrlFinder.get(Long.MAX_VALUE))
            .isInstanceOf(BusinessException.class)
            .extracting(e -> ((BusinessException) e).errorCode())
            .isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    void getAndFindAllReflectClickCount() {
        ShortUrl shortUrl = ShortUrlTestSupport.create(shortUrlManager, "https://example.com", createdShortUrlIds);
        clickCountRepository.increment(shortUrl.id());
        clickCountRepository.increment(shortUrl.id());

        ShortUrlWithClickCount found = shortUrlFinder.get(shortUrl.id());

        assertThat(found.clickCount()).isEqualTo(2L);
        assertThat(shortUrlFinder.findAll(new PageParam(null, 20)).content())
            .filteredOn(view -> view.shortUrl().id().equals(shortUrl.id()))
            .singleElement()
            .extracting(ShortUrlWithClickCount::clickCount)
            .isEqualTo(2L);
    }

}
