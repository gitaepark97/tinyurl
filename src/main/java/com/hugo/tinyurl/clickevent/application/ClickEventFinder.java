package com.hugo.tinyurl.clickevent.application;

import com.hugo.tinyurl.clickevent.model.ClickEvent;
import com.hugo.tinyurl.clickevent.port.ClickEventRepository;
import com.hugo.tinyurl.domain.model.ShortUrl;
import com.hugo.tinyurl.domain.port.ShortUrlRepository;
import com.hugo.tinyurl.member.model.Role;
import com.hugo.tinyurl.support.exception.BusinessException;
import com.hugo.tinyurl.support.exception.ErrorCode;
import com.hugo.tinyurl.support.page.Page;
import com.hugo.tinyurl.support.page.PageParam;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Observed
@Component
@RequiredArgsConstructor
class ClickEventFinder {

    private final ClickEventRepository clickEventRepository;
    private final ShortUrlRepository shortUrlRepository;

    @Transactional(readOnly = true)
    Page<ClickEvent> findAll(Long shortUrlId, Long requesterMemberId, Role requesterRole, PageParam pageParam) {
        ShortUrl shortUrl = shortUrlRepository.findById(shortUrlId)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (requesterRole != Role.ADMIN && !shortUrl.isOwnedBy(requesterMemberId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        List<ClickEvent> overFetched = clickEventRepository.findByShortUrlIdAndIdLessThanOrderByIdDesc(
            shortUrlId, pageParam.cursorOrInitial(), pageParam.size() + 1);

        boolean hasNext = overFetched.size() > pageParam.size();
        List<ClickEvent> events = hasNext ? overFetched.subList(0, pageParam.size()) : overFetched;
        return Page.of(events, hasNext);
    }

}
