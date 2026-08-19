package com.hugo.tinyurl.member.infra.persistence.jpa;

import com.hugo.tinyurl.common.model.Role;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface MemberEntityRepository extends JpaRepository<MemberJpaEntity, Long> {

    Optional<MemberJpaEntity> findByEmail(String email);

    long countByRole(Role role);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("UPDATE MemberJpaEntity m SET m.role = :role WHERE m.id = :id")
    void updateRole(@Param("id") Long id, @Param("role") Role role);

}
