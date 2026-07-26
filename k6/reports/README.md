# 부하 테스트 결과 아카이브

`../run.sh <api> <scenario>` 실행 시 `k6 run --summary-export`로 생성된 집계 요약(JSON)이
`<api>/<scenario>/<실행시각>.json`으로 저장된다. 개별 요청 로그가 아니라 요청 수/실패율/응답시간
percentile 등 집계 값만 담겨 있어 커밋해서 시계열로 비교한다.

## 사용법

    ./k6/run.sh create smoke
    ./k6/run.sh redirect load

## 비교 기준

- smoke 결과가 baseline이다. load/stress 결과의 p95/p99, 실패율을 smoke와 비교해 부하에 따른
  저하 정도를 판단한다.
- 개선 작업(예: 캐싱 도입) 전/후 같은 시나리오를 재실행해 수치를 비교한다.

## 사람이 보기 좋은 리포트

원본 JSON을 직접 읽기보다, `k6-report` skill(사용자 레벨 `~/.claude/skills/k6-report`)로 해석이
포함된 HTML을 생성해서 본다.

- **sections 모드**: 같은 API의 smoke/load/stress를 한 문서에 모아서 본다(처음 리포트를 만들 때).
- **compare 모드**: 같은 시나리오를 시점을 달리해 여러 번 실행했다면(같은 디렉터리에 JSON이 여러 개
  쌓였다면), 시점별 비교표로 개선/악화를 추적한다. 이 저장소는 실행마다 파일명에 타임스탬프를 붙여
  기존 결과를 덮어쓰지 않으므로, 재실행만 하면 compare 모드에 필요한 이력이 자연히 쌓인다.

    /k6-report

생성된 HTML은 `.gitignore`에 등록해 커밋하지 않는다(원본 JSON이 소스). 자세한 사용법은 skill 문서
참고.
