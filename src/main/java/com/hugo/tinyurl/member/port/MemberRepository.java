package com.hugo.tinyurl.member.port;

import com.hugo.tinyurl.member.model.Member;
import com.hugo.tinyurl.member.model.Role;
import java.util.Optional;

public interface MemberRepository {

    Member save(Member member);

    Optional<Member> findById(Long id);

    Optional<Member> findByEmail(String email);

    void updateRole(Long id, Role role);

    long countByRole(Role role);

    void deleteById(Long id);

}
