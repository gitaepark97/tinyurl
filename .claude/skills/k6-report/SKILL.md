---
name: k6-report
description: Turn k6 performance test results into a plain-language Korean HTML report, including before/after comparisons across runs over time. Use when the user asks for a k6 성능 테스트 리포트, wants k6 결과를 해석/보고용으로 정리, asks to compare 개선 전/후 or 버전별 성능, or asks to summarize/interpret k6 load test output in a readable format (as opposed to raw k6-reporter dashboards or terminal summaries).
---

# k6 결과 → 해석 리포트

k6 원시 결과(터미널 요약, k6-reporter HTML)는 지표명이 영어고 임계치 통과 여부가 한눈에 안 들어온다.
이 skill은 k6의 `--summary-export` JSON을 읽어서 한국어 HTML 리포트를 생성한다. 두 가지 모드가 있다.

- **sections 모드** (기본): 서로 다른 테스트 유형(smoke/load/stress/...)을 한 문서에 섹션별로 모은다.
- **compare 모드**: **같은 테스트를 시점을 달리해 여러 번 돌린 결과**를 지표별 비교표로 보여준다.
  개선 작업을 적용해가며 "이전 대비 나아졌는지"를 추적할 때 쓴다. 첫 번째 `--input`이 기준(baseline)이
  되고, 이후 입력들은 기준 대비 변화량(▲/▼, %, 개선/악화 색상)으로 표시된다.

## 언제 쓰나

- "k6 결과 리포트 만들어줘", "성능 테스트 결과 해석해줘", "보고용 리포트" → sections 모드
- "개선 전/후 비교해줘", "이전 버전이랑 비교", "버전 1로 해줘" 같이 **시간에 따른 추적**을 원할 때
  → compare 모드
- k6-reporter HTML(원시 대시보드)만으로는 부족하고, 요약/해석이 포함된 문서가 필요할 때

## 사용 조건

- 대상 프로젝트에 k6 테스트 스크립트가 있어야 한다 (없으면 먼저 스크립트를 만들거나 사용자에게 물어본다).
- 스크립트가 커스텀 `Trend` 지표를 `*_duration` 이름으로 노출하면 (예: `create_duration`,
  `redirect_duration`) 리포트에 구간별 표가 자동으로 생성된다. 없으면 전체 `http_req_duration`만
  보여준다. 여러 엔드포인트를 섞어서 테스트하는 스크립트라면, 각 단계에 `Trend('<step>_duration', true)`를
  추가하고 `.add(response.timings.duration)`으로 값을 기록하도록 스크립트를 먼저 고쳐주는 게 좋다.
- compare 모드로 추적하려면, 실행할 때마다 JSON 파일을 **버전/시점이 구분되는 이름으로 보존**해야 한다
  (예: `k6/reports/v1/smoke-summary.json`, `k6/reports/v2/smoke-summary.json`, 또는
  `smoke-summary-2026-07-11.json`처럼 날짜를 붙인다). 매번 같은 파일명으로 덮어쓰면 비교할 이전
  데이터가 사라지므로, 사용자가 "버전 1로 해줘" 같은 요청을 하면 현재 산출물을 버전 디렉토리/파일명으로
  옮기거나 복사해서 보존해준다.

## 절차

1. 어떤 k6 스크립트/실행 결과를 리포트로 만들지, sections/compare 중 어떤 모드인지 확인한다.
   프로젝트에 스크립트가 여러 개(예: smoke/load/stress/spike/soak)면, 사용자가 "전체"/"나머지도" 같은
   표현을 쓴 게 아니면 어떤 것을 포함할지 먼저 확인한다 — 기본은 모두 포함이 아니라 방금 논의 중인
   것 하나다.
   - 사용자가 이미 실행한 결과(JSON)가 있으면 그걸 쓴다.
   - 없으면 각 스크립트를 `k6 run --summary-export=<output-dir>/<name>-summary.json <script>.js`로
     직접 실행한다. `<output-dir>`은 보통 프로젝트의 k6 리포트 디렉토리(예: `k6/reports/`)를 따른다 —
     없으면 만든다. `load`/`stress`/`soak`처럼 원래 실행 시간이 긴 스크립트는, 검증/데모 목적이면
     `--vus`/`--duration`으로 짧게 줄여서 돌리되 **축약 실행이라는 걸 리포트/채팅에 명시**한다. 실제
     의사결정용 리포트라면 원래 정의된 시간대로 돌려야 한다고 알려준다.
2. 리포트를 생성한다.
   - **sections 모드** (다른 테스트 유형들을 한 문서로): `label=path`를 여러 개 넘기면 상단에 전체
     합격/불합격 요약표가 자동으로 붙는다.
     ```
     python3 .claude/skills/k6-report/generate_report.py \
       --mode sections \
       --input "Smoke=<dir>/smoke-summary.json" "Load=<dir>/load-summary.json" ... \
       --output <output-dir>/full-report-ko.html \
       --title "<프로젝트> 성능 테스트 통합 리포트"
     ```
   - **compare 모드** (같은 테스트, 시점별 추적): 첫 `--input`이 기준. 라벨은 "버전 1"/"개선 전"처럼
     사람이 알아볼 수 있는 이름으로 준다.
     ```
     python3 .claude/skills/k6-report/generate_report.py \
       --mode compare \
       --input "버전 1=<dir>/v1/smoke-summary.json" "버전 2=<dir>/v2/smoke-summary.json" \
       --output <output-dir>/smoke-compare-ko.html \
       --title "<프로젝트> Smoke 성능 비교 (버전 1 → 버전 2)"
     ```
3. 생성된 HTML을 열거나(`open <path>`) 경로를 안내하고, 핵심 수치를 채팅에도 요약해준다. compare
   모드면 기준 대비 무엇이 개선/악화됐는지 짚어준다. sections 모드로 여러 테스트를 합쳤다면 어떤
   유형이 불합격인지 짚어준다.
4. 리포트 산출물(JSON/HTML)은 실행마다 값이 바뀌는 생성물이므로, 해당 디렉토리가 `.gitignore`에
   없다면 추가할지 물어본다. 단, compare 모드용으로 보존해야 하는 버전별 JSON은 지우지 않는다 —
   `.gitignore`는 "git에 커밋 안 함"이지 "로컬에서 지워도 됨"이 아니다.

## 주의

- 스크립트는 표준 라이브러리(json/argparse/datetime/re/pathlib)만 쓰므로 별도 설치 없이 `python3`만
  있으면 된다.
- 지표 이름을 임의로 예쁘게 바꾸지 않는다 — 원래 k6 metric 이름(`create_duration` 등)을 그대로
  보여줘야 원본 데이터와 대조 가능하다.
- 임계치(threshold)가 하나라도 실패하면 전체 판정은 반드시 "불합격"으로 표시된다 (스크립트가 자동
  처리하므로 별도로 판단할 필요 없음).
- compare 모드는 변화량이 2% 미만이면 "변화 없음"으로 취급해 표시를 생략한다 (노이즈 제거).
