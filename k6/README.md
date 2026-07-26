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

## 인프라 리소스

`compose.yaml`의 `mysql`, `grafana-lgtm` 컨테이너에 CPU/메모리 제한을 걸어뒀다(각각 2 CPU/1GB,
1 CPU/2GB). 제한이 없으면 stress 시나리오에서 컨테이너가 호스트 자원을 무제한으로 끌어써서 결과가
머신마다 달라지고 재현이 안 된다 — 응답시간 결과는 이 자원 한도 안에서 측정된 값으로 해석해야 한다.
