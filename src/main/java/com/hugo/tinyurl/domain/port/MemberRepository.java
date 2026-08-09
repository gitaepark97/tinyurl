package com.hugo.tinyurl.domain.port;

import com.hugo.tinyurl.domain.model.Member;
import java.util.Optional;

public interface MemberRepository {

    Member save(Member member);

    Optional<Member> findById(Long id);

    Optional<Member> findByEmail(String email);

    void deleteById(Long id);

}
