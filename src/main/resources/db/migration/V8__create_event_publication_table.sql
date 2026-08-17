-- Spring Modulith 이벤트 발행 레지스트리(spring-modulith-starter-jpa)가 검증하는 스키마.
-- 컬럼 정의는 추측이 아니라 실제 JPA 엔티티(DefaultJpaEventPublication)가 ddl-auto=update로
-- 생성한 결과를 그대로 옮긴 것이다.
CREATE TABLE event_publication
(
    id                     BINARY(16)                                                            NOT NULL,
    listener_id            VARCHAR(255)                                                           NOT NULL,
    event_type             VARCHAR(255)                                                           NOT NULL,
    -- 엔티티 매핑상 기본 길이는 255지만, 직렬화된 이벤트 페이로드가 그보다 커질 수 있어
    -- (예: 비정상적으로 긴 User-Agent) 여유 있게 넓힌다. Hibernate validate는 컬럼이 매핑보다
    -- 넓은 것은 허용한다(좁으면 실패).
    serialized_event       VARCHAR(4000)                                                          NOT NULL,
    publication_date       DATETIME(6)                                                             NOT NULL,
    completion_date        DATETIME(6)                                                             NULL,
    status                 ENUM ('PUBLISHED', 'PROCESSING', 'COMPLETED', 'FAILED', 'RESUBMITTED') NULL,
    completion_attempts    INT                                                                     NOT NULL,
    last_resubmission_date DATETIME(6)                                                             NULL,
    PRIMARY KEY (id),
    INDEX idx_event_publication_completion_date (completion_date)
);
