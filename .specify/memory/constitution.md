<!--
Sync Impact Report
- Version change: (none) → 1.0.0
- Modified principles: n/a (initial ratification)
- Added sections: Core Principles (I–V), 기술 스택 및 제약, 개발 워크플로, Governance
- Removed sections: none
- Templates requiring follow-up: none — this is the initial constitution, derived from the
  existing codebase (README.md, .claude/rules/*.md) rather than from a prior version.
- Deferred placeholders: none
-->

# tinyurl Constitution

## Core Principles

### I. 모듈 경계 무결성 (Spring Modulith)
`member`/`shorturl`/`clickevent`/`common` 모듈은 `@NamedInterface`로 노출한 표면으로만 서로
통신한다. 한 모듈이 다른 모듈의 내부(패키지 비공개 클래스)를 직접 참조해서는 안 되며, 공통으로
쓰는 개념만 `common`으로 옮긴다 — "여러 모듈이 프로토콜을 다룬다" 같은 피상적 기준이 아니라
실제로 공유되는 도메인/에러 신호/자료구조인지로 판단한다. `ApplicationModulesTest`와
`DocumenterTest`는 이 경계를 기계적으로 검증하는 게이트이며, 모든 PR은 이 두 테스트를 통과해야
한다.
**근거**: 모듈 경계가 느슨해지면 응집도가 무너지고 리팩터링 비용이 기하급수적으로 늘어난다 —
이 프로젝트는 `support` 모듈을 없애고 `common`의 관심사별 패키지로 흡수하는 리팩터링을 실제로
거치며 이 경계를 다시 세운 바 있다.

### II. 실제 인프라로 검증하는 테스트
데이터베이스, Redis, Kafka, Zookeeper를 다루는 코드는 목(mock)이 아니라 Testcontainers로 띄운
실제 인스턴스로 검증한다. 필터/설정처럼 등록 상태에서만 의미가 있는 컴포넌트는 격리된 단위
테스트가 아니라 `@SpringBootTest` + `TestRestTemplate`로 실제 등록된 체인을 통해 엔드투엔드로
검증한다.
**근거**: 목으로 통과하던 테스트가 실제 마이그레이션/설정 문제를 못 잡아내는 사고를 반복하지
않기 위함이다. 인프라 재사용(`.withReuse(true)`) 등으로 발생하는 실행 순서 의존성은 테스트
설계(고유한 키 사용 등)로 직접 없앤다.

### III. 보호 장치는 fail-open, 예외는 좁게 잡는다
Rate limit, 캐시처럼 진실의 원천이 아니라 보호/최적화 장치인 컴포넌트는 백엔드 장애 시
요청을 막지 않고 통과시킨다(fail-open). 이때 잡는 예외는 해당 장애를 명확히 나타내는 타입으로
좁게 한정한다 — 원인을 알 수 없는 예외까지 넓게 삼켜 감춰서는 안 된다.
**근거**: 보호 장치의 장애가 핵심 기능의 장애로 번지면 안전장치가 오히려 가용성을 해친다.
동시에 예외를 넓게 잡으면 진짜 버그를 조용히 감추게 된다.

### IV. 최소한의 추상화
중복이 실제로 존재하고 반복될 때만 공통화한다 — 아직 일어나지 않은 미래 요구를 가정한
추상화, 사용처가 하나뿐인 인터페이스, 방어적 검증/폴백은 추가하지 않는다. 진짜 동일한 로직
(예: 두 rate limit 필터의 fail-open 소비 로직)은 작은 공유 헬퍼로 뽑되, 서로 다른 부분
(키 추출, 스킵 조건)까지 억지로 하나의 추상화에 밀어 넣지 않는다.
**근거**: 비슷해 보이는 코드 세 줄이 이르게 만든 추상화보다 낫다 — 잘못된 추상화는 나중에
갈라내는 비용이 통합하는 비용보다 크다.

### V. 기본으로 켜져 있는 관측성
비즈니스적으로 의미 있는 분기(리다이렉트 성공/실패/만료, 클릭 이벤트 중복 등)는
Micrometer `Counter`와 `@Observed`로 계측하고, OpenTelemetry로 트레이스/로그와 연결한다.
새 기능을 추가할 때 그 기능의 성공/실패를 구분할 지표가 없다면 빠뜨린 것으로 간주한다.
**근거**: 운영 중 발생하는 문제를 로그 grep이 아니라 대시보드로 먼저 알아챌 수 있어야 한다.

## 기술 스택 및 제약

- Java 25 / Spring Boot 4.1, Gradle(Kotlin DSL). Spring Web MVC, Spring Data JPA, Flyway, MySQL.
- Apache Zookeeper(Curator) — shortKey 발급용 전역 유일 카운터. Redis — 단축 URL 조회 캐시 및
  rate limit 버킷 저장소. Apache Kafka — 클릭 이벤트 비동기 기록.
- Log4j2 + OpenTelemetry(Trace/Metric/Log), Spring REST Docs + Asciidoctor로 API 문서 생성.
- `local`(Docker Compose 자동 연결) / `dev`(환경변수, 없으면 로컬 compose 기본값) / `prod`
  (환경변수 필수, 기본값 없음) 세 profile을 유지한다. 새 설정값은 이 세 profile 모두에
  일관되게 추가한다.
- 회원(`ADMIN`/`MEMBER`) 관련 보안/HTTP 프로토콜 관심사는 `member`/`common.web` 계층에 두고,
  `infra`는 외부 자원 클라이언트로 한정한다.

## 개발 워크플로

- 브랜치/커밋 컨벤션은 [`.claude/rules/branch-convention.md`](../../.claude/rules/branch-convention.md),
  [`.claude/rules/commit-convention.md`](../../.claude/rules/commit-convention.md)를 그대로
  따른다 — `feature/*`는 `develop`에서 분기해 `develop`으로 병합하고, 커밋은 하나의 변경 사항만
  담아 `<type>: <한글 subject>` 형식으로 작성한다.
- Java 클래스 멤버 순서(필드: 인터페이스 → 구현체, 메서드: public → package-private → private)는
  [`.claude/rules/java-convention.md`](../../.claude/rules/java-convention.md)를 따른다.
- 원격/공유 상태에 영향을 주는 작업(push, PR 생성, merge, 이슈 생성)은 초안을 먼저 보여주고
  명시적 확인을 받은 뒤에만 실행한다.
- 매 변경은 최소한 `compileJava`/`compileTestJava`와 영향받은 테스트로 좁게 검증한 뒤,
  `ApplicationModulesTest`/`DocumenterTest`와 전체 스위트로 마무리 검증한다.

## Governance

이 constitution은 이 저장소의 다른 관례보다 우선한다. 상세 규칙(`.claude/rules/*.md`)은 이
문서의 원칙을 구체화한 것으로, 서로 충돌하면 이 문서가 우선하고 상세 규칙을 갱신한다.

개정은 이 문서를 직접 수정하고 아래 버전을 시맨틱 버저닝으로 올리는 방식으로 이뤄진다 —
원칙의 하위 호환 없는 삭제/재정의는 MAJOR, 원칙 추가나 실질적 내용 확장은 MINOR, 표현 수정
같은 비의미적 변경은 PATCH. 모든 PR은 이 원칙들을 위반하지 않는지 검토 대상에 포함한다.
복잡도를 늘리는 예외는 PR 설명에 근거를 남겨야 한다.

**Version**: 1.0.0 | **Ratified**: 2026-08-30 | **Last Amended**: 2026-08-30
