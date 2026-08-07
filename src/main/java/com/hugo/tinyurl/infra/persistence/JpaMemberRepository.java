package com.hugo.tinyurl.infra.persistence;

import com.hugo.tinyurl.domain.model.Member;
import com.hugo.tinyurl.domain.port.MemberRepository;
import com.hugo.tinyurl.infra.persistence.jpa.MemberEntityRepository;
import com.hugo.tinyurl.infra.persistence.jpa.MemberJpaEntity;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class JpaMemberRepository implements MemberRepository {

    private final MemberEntityRepository memberEntityRepository;

    @Override
    public Member save(Member member) {
        return memberEntityRepository.save(MemberJpaEntity.from(member)).toDomain();
    }

    @Override
    public Optional<Member> findById(Long id) {
        return memberEntityRepository.findById(id).map(MemberJpaEntity::toDomain);
    }

    @Override
    public Optional<Member> findByEmail(String email) {
        return memberEntityRepository.findByEmail(email).map(MemberJpaEntity::toDomain);
    }

    @Override
    public void deleteById(Long id) {
        memberEntityRepository.deleteById(id);
    }

}
