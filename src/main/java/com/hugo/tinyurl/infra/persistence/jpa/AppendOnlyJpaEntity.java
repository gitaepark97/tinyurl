package com.hugo.tinyurl.infra.persistence.jpa;

import org.springframework.data.domain.Persistable;

// PK를 애플리케이션(Snowflake)에서 채번하고 update 유스케이스가 없는 append-only 엔티티가 상속한다.
// isNew()를 항상 true로 고정해 save()가 merge 대신 항상 persist(INSERT) 경로를 타도록 강제한다.
abstract class AppendOnlyJpaEntity<ID> implements Persistable<ID> {

    @Override
    public boolean isNew() {
        return true;
    }

}
