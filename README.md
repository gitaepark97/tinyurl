# tinyurl

단축 URL 서비스.

## 기술 스택

- Java 25 / Spring Boot 4.1.0
- Gradle (Kotlin DSL)
- Spring Web MVC, Spring Data JPA, Flyway, MySQL
- Log4j2 (로깅), OpenTelemetry (Trace/Metric/Log 연동)
- Spring REST Docs + Asciidoctor
- Docker Compose (로컬 개발 인프라), Testcontainers (테스트)

## 로컬 실행

MySQL, Grafana LGTM(Loki/Tempo/Mimir) 스택을 담은 `compose.yaml`이 있어 별도로 `docker compose up`을 미리 실행할 필요가 없다. `spring-boot-docker-compose` 의존성이 애플리케이션 기동 시 필요한 컨테이너를 자동으로 띄우고 연결해준다.

```bash
./gradlew bootRun
```

- 기본 활성 profile은 `local`이며, DB/observability 접속 정보는 Docker Compose 자동 연결로 채워지므로 별도 환경변수가 필요 없다.
- 컨테이너는 `spring.docker.compose.lifecycle-management: start-only`로 설정되어 있어 애플리케이션을 껐다 켜도 계속 떠 있는다. 필요하면 직접 내린다.

```bash
docker compose down
```

### profile

| profile | 용도 | DB / observability 설정 |
| --- | --- | --- |
| `local` | 로컬 개발 (기본값) | Docker Compose 자동 연결 |
| `dev` | 개발 서버 | 환경변수(`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `OTLP_TRACING_ENDPOINT`, `OTLP_METRICS_ENDPOINT`, `OTLP_LOGGING_ENDPOINT`), 없으면 로컬 compose 스택 기준 기본값 사용 |
| `prod` | 운영 | 위 환경변수 전부 필수(기본값 없음) |

```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
```

## 관측성 (LGTM)

`docker compose up` 이후 Grafana에서 로그(Loki)/트레이스(Tempo)/메트릭(Mimir)을 확인할 수 있다.

- Grafana: http://localhost:3000

## API 문서

내용만 빠르게 보고 싶다면 앱을 띄우지 않고 문서만 생성해서 바로 열어본다.

```bash
./gradlew asciidoctor
open build/docs/asciidoc/index.html
```

실제로 애플리케이션이 그 경로(`/docs/index.html`)로 정상 서빙하는지까지 확인하려면 jar를 빌드해서 실행한다 — Spring REST Docs 문서는 `bootJar`가 빌드 시점에 정적 리소스로 넣어주는데, `./gradlew bootRun`/IDE 실행은 jar를 만들지 않으므로 이 경로를 제공하지 않는다.

```bash
docker compose up -d   # 이미 떠 있다면 생략
./gradlew bootJar
java -jar build/libs/tinyurl-*.jar --spring.profiles.active=dev
```

- http://localhost:8080/docs/index.html

패키징된 jar에는 `spring-boot-docker-compose`가 없어서(개발용 의존성이라 제외됨) Docker Compose 자동 연결을 못 쓴다. `dev` profile은 `DB_URL` 등에 로컬 compose 스택 기준 기본값(`localhost:3306`)이 이미 들어 있어 별도 환경변수 없이도 그대로 붙는다. `local` profile로 확인하고 싶다면(로그 레벨/트레이스 샘플링이 다르다) datasource를 커맨드라인 인자로 직접 넘긴다.

```bash
java -jar build/libs/tinyurl-*.jar --spring.profiles.active=local \
  --spring.datasource.url=jdbc:mysql://localhost:3306/tinyurl \
  --spring.datasource.username=tinyurl \
  --spring.datasource.password=tinyurl
```

## 빌드 / 테스트

```bash
./gradlew build
```

`test` → `asciidoctor`(API 문서 생성) → `copyDocs`(문서를 별도 빌드 디렉터리로 복사) → `bootJar`(jar 패키징 시점에 문서를 `BOOT-INF/classes/static/docs`로 포함) 순으로 수행된다.

## 컨벤션

- 브랜치: [`.claude/rules/branch-convention.md`](.claude/rules/branch-convention.md)
- 커밋: [`.claude/rules/commit-convention.md`](.claude/rules/commit-convention.md)
