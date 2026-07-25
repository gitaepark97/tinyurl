---
name: create-pr
description: gh CLI로 이 저장소에 Pull Request를 생성한다. 사용자가 "PR 만들어줘", "PR 올려줘" 등 PR 생성을 요청할 때 사용한다.
---

# PR 생성

`gh pr create`를 사용해 `gitaepark97/tinyurl` 저장소에 Pull Request를 생성한다.

이 저장소에서 PR은 `feature/*` 브랜치를 `develop`으로 병합할 때만 사용한다. `release/*`, `hotfix/*` 브랜치는 PR 없이 `main`/`develop`에 직접 병합하므로 이 skill의 대상이 아니다.

## 절차

1. 현재 브랜치 상태를 파악한다.
   - `git status`, `git diff`(staged/unstaged), 원격 추적 브랜치 여부와 최신 상태인지 확인한다.
   - 브랜치가 원격에 없거나 뒤처져 있으면 push가 필요한지 판단한다.
   - 현재 브랜치가 `feature/*`가 아니라면(`release/*`, `hotfix/*`, `main`, `develop` 등) 이 저장소 규칙상 PR 없이 직접 병합 대상이다. PR을 만들지 않고 사용자에게 이 사실을 안내한 뒤, 정말 PR이 필요한 예외 상황인지 먼저 확인한다.
2. base 브랜치는 `develop`으로 고정한다(`.claude/rules/branch-convention.md`의 Git flow 규칙에 따라 `feature`는 `develop`에서 분기해 `develop`으로 병합).
3. base 브랜치 대비 diff(`git diff develop...HEAD`)와 커밋 로그(`git log`)를 모두 확인해 PR에 포함될 전체 변경 사항을 파악한다 — 최신 커밋 하나만 보지 않는다.
4. PR 제목과 본문 초안을 작성한다.
   - 제목은 `.claude/rules/commit-convention.md`의 type(`feature`, `fix`, `refactor` 등)을 참고해 간결하게 작성한다.
   - 본문은 변경 요약과 테스트 계획(체크리스트)을 포함한다. 예:
     ```
     ## 요약
     - ...

     ## 테스트 계획
     - [ ] ...
     ```
5. 초안을 사용자에게 보여주고 확인받는다 — PR 생성은 다른 사람에게 보이는 작업이므로, 사용자가 이미 정확한 내용을 명시적으로 승인한 경우가 아니라면 반드시 먼저 확인한다.
6. 필요 시 원격에 push한 뒤 생성한다:

```bash
git push -u origin <현재 브랜치>

gh pr create --repo gitaepark97/tinyurl --base develop --title "<제목>" --body "$(cat <<'EOF'
<본문>
EOF
)"
```

7. `gh pr create`가 반환한 PR URL을 사용자에게 알려준다.

## 참고

- `--web` 옵션은 브라우저를 열려고 시도하므로 절대 사용하지 않는다 — 항상 위 CLI 플래그로 바로 생성한다.
- `gh auth status`가 실패하면 직접 로그인을 시도하지 말고 사용자에게 `gh auth login`을 직접 실행하도록 안내한다.
- push, PR 생성 모두 원격/공유 상태에 영향을 주는 작업이므로 사용자의 명시적 확인 없이 진행하지 않는다.
