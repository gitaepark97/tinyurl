package com.hugo.tinyurl.infra.persistence;

import com.hugo.tinyurl.domain.model.Member;
import com.hugo.tinyurl.domain.model.Role;
import com.hugo.tinyurl.domain.port.MemberRepository;
import com.hugo.tinyurl.infra.persistence.jpa.MemberEntityRepository;
import com.hugo.tinyurl.infra.persistence.jpa.MemberJpaEntity;
import java.util.List;
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
    public void updateRole(Long id, Role role) {
        memberEntityRepository.updateRole(id, role);
    }

    @Override
    public long countByRole(Role role) {
        return memberEntityRepository.countByRole(role);
    }

    @Override
    public void deleteById(Long id) {
        // isNew()가 항상 true인 AppendOnlyJpaEntity는 일반 deleteById()가 조용히 무효화되므로
        // (SimpleJpaRepository.doDelete()의 isNew() 가드) 벌크 삭제로 우회한다.
        memberEntityRepository.deleteAllByIdInBatch(List.of(id));
    }

}
