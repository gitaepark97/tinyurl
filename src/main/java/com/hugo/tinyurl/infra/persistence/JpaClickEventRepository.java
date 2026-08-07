package com.hugo.tinyurl.infra.persistence;

import com.hugo.tinyurl.domain.model.ClickEvent;
import com.hugo.tinyurl.domain.port.ClickEventRepository;
import com.hugo.tinyurl.infra.persistence.jpa.ClickEventEntityRepository;
import com.hugo.tinyurl.infra.persistence.jpa.ClickEventJpaEntity;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class JpaClickEventRepository implements ClickEventRepository {

    private final ClickEventEntityRepository clickEventEntityRepository;

    @Override
    public ClickEvent save(ClickEvent clickEvent) {
        return clickEventEntityRepository.save(ClickEventJpaEntity.from(clickEvent)).toDomain();
    }

    @Override
    public List<ClickEvent> findByShortUrlIdAndIdLessThanOrderByIdDesc(Long shortUrlId, Long id, int limit) {
        return clickEventEntityRepository.findByShortUrlIdAndIdLessThanOrderByIdDesc(shortUrlId, id, PageRequest.of(0, limit)).stream()
            .map(ClickEventJpaEntity::toDomain)
            .toList();
    }

    @Override
    public void deleteAll(Iterable<ClickEvent> clickEvents) {
        List<ClickEventJpaEntity> entities = new ArrayList<>();
        clickEvents.forEach(clickEvent -> entities.add(ClickEventJpaEntity.from(clickEvent)));
        clickEventEntityRepository.deleteAllInBatch(entities);
    }

}
