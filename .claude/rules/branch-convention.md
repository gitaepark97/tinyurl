# Branch Convention

브랜치 이름은 다음 형식을 따른다.

```
<type>/<description>
```

- `type`: 브랜치의 목적을 나타내는 접두사
- `description`: 영어 단어를 하이픈(`-`)으로 연결한 요약. 소문자만 사용한다.

Git flow 전략을 따르며, `main`과 `develop`은 상시 유지되는 브랜치로 접두사를 붙이지 않는다.

## Type 종류

| type | 설명 |
| --- | --- |
| `feature` | 새로운 기능 개발 및 버그 수정 등 모든 일반 작업 |
| `release` | 배포 준비 및 버전 릴리즈 |
| `hotfix` | 배포된 서비스의 긴급 수정 |

## 예시

```
feature/short-url-redirect
feature/duplicate-url-key-fix
release/v1.2.0
hotfix/redirect-500-error
```

## 규칙

- `feature`는 `develop`에서 분기하여 `develop`으로 병합한다. 버그 수정, 리팩터링, 문서 작업 등도 별도 타입 없이 `feature`로 분기한다.
- `release`는 `develop`에서 분기하여 배포 준비(버전 표기, 문서 정리 등)를 마친 뒤 `main`과 `develop` 양쪽에 병합한다.
- `hotfix`는 `main`에서 분기하여 수정 후 `main`과 `develop` 양쪽에 병합한다.
- 관련 이슈가 있다면 이슈 번호를 앞에 붙인다. 예: `feature/42-short-url-redirect`
- 병합이 완료된 브랜치는 삭제한다.
