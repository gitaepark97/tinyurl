package com.hugo.tinyurl.infra.persistence;

import com.hugo.tinyurl.domain.model.ShortUrl;
import com.hugo.tinyurl.domain.port.ShortUrlRepository;
import com.hugo.tinyurl.infra.persistence.jpa.ShortUrlEntityRepository;
import com.hugo.tinyurl.infra.persistence.jpa.ShortUrlJpaEntity;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class JpaShortUrlRepository implements ShortUrlRepository {

    private final ShortUrlEntityRepository shortUrlEntityRepository;

    @Override
    public ShortUrl save(ShortUrl shortUrl) {
        return shortUrlEntityRepository.save(ShortUrlJpaEntity.from(shortUrl)).toDomain();
    }

    @Override
    public Optional<ShortUrl> findById(Long id) {
        return shortUrlEntityRepository.findById(id).map(ShortUrlJpaEntity::toDomain);
    }

    @Override
    public Optional<ShortUrl> findByShortKey(String shortKey) {
        return shortUrlEntityRepository.findByShortKey(shortKey).map(ShortUrlJpaEntity::toDomain);
    }

    @Override
    public List<ShortUrl> findByIdLessThanOrderByIdDesc(Long id, int limit) {
        return shortUrlEntityRepository.findByIdLessThanOrderByIdDesc(id, PageRequest.of(0, limit)).stream()
            .map(ShortUrlJpaEntity::toDomain)
            .toList();
    }

    @Override
    public List<ShortUrl> findByMemberIdAndIdLessThanOrderByIdDesc(Long memberId, Long id, int limit) {
        return shortUrlEntityRepository.findByMemberIdAndIdLessThanOrderByIdDesc(memberId, id, PageRequest.of(0, limit)).stream()
            .map(ShortUrlJpaEntity::toDomain)
            .toList();
    }

    @Override
    public List<ShortUrl> findByExpiresAtBeforeOrderByIdAsc(LocalDateTime dateTime, int limit) {
        return shortUrlEntityRepository.findByExpiresAtBeforeOrderByIdAsc(dateTime, PageRequest.of(0, limit)).stream()
            .map(ShortUrlJpaEntity::toDomain)
            .toList();
    }

    @Override
    public void deleteById(Long id) {
        // isNew()가 항상 true인 AppendOnlyJpaEntity는 일반 deleteById()가 조용히 무효화되므로
        // (SimpleJpaRepository.doDelete()의 isNew() 가드) 벌크 삭제로 우회한다.
        shortUrlEntityRepository.deleteAllByIdInBatch(List.of(id));
    }

    @Override
    public void deleteAllById(Iterable<Long> ids) {
        shortUrlEntityRepository.deleteAllByIdInBatch(ids);
    }

}
