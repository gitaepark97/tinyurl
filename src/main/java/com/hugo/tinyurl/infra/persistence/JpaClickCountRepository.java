package com.hugo.tinyurl.infra.persistence;

import com.hugo.tinyurl.domain.model.ClickCount;
import com.hugo.tinyurl.domain.port.ClickCountRepository;
import com.hugo.tinyurl.infra.persistence.jpa.ClickCountEntityRepository;
import com.hugo.tinyurl.infra.persistence.jpa.ClickCountJpaEntity;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class JpaClickCountRepository implements ClickCountRepository {

    private final ClickCountEntityRepository clickCountEntityRepository;

    @Override
    public void increment(Long shortUrlId) {
        clickCountEntityRepository.increment(shortUrlId);
    }

    @Override
    public Optional<ClickCount> findById(Long shortUrlId) {
        return clickCountEntityRepository.findById(shortUrlId).map(ClickCountJpaEntity::toDomain);
    }

    @Override
    public List<ClickCount> findAllById(Iterable<Long> shortUrlIds) {
        return clickCountEntityRepository.findAllById(shortUrlIds).stream()
            .map(ClickCountJpaEntity::toDomain)
            .toList();
    }

    @Override
    public void deleteById(Long shortUrlId) {
        clickCountEntityRepository.deleteById(shortUrlId);
    }

}
