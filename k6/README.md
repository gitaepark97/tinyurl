# 부하/스트레스 테스트 (k6)

`POST /api/v1/urls`(생성), `GET /{shortKey}`(리다이렉트) 두 API에 대해 smoke/load/stress
세 시나리오를 각각 실행한다.

## 준비

    docker compose up -d
    ./gradlew bootRun --args='--spring.profiles.active=local --server.port=8081' &
    # BASE_URL 기본값은 http://localhost:8081 — 다른 포트로 띄웠다면 BASE_URL 환경변수로 지정

## 실행

    ./k6/run.sh create smoke
    ./k6/run.sh create load
    ./k6/run.sh create stress
    ./k6/run.sh redirect smoke
    ./k6/run.sh redirect load
    ./k6/run.sh redirect stress

결과는 `reports/<api>/<scenario>/`에 쌓인다. 아카이빙 규칙은 `reports/README.md` 참고.
