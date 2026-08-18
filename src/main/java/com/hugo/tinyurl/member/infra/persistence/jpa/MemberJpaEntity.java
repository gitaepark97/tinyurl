package com.hugo.tinyurl.member.infra.persistence.jpa;

import com.hugo.tinyurl.common.infra.persistence.jpa.AppendOnlyJpaEntity;
import com.hugo.tinyurl.common.model.Role;
import com.hugo.tinyurl.member.model.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "member")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class MemberJpaEntity extends AppendOnlyJpaEntity<Long> {

    @Id
    private Long id;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static MemberJpaEntity from(Member member) {
        return new MemberJpaEntity(member.id(), member.email(), member.password(), member.role(), member.createdAt());
    }

    public Member toDomain() {
        return new Member(id, email, password, role, createdAt);
    }

}
