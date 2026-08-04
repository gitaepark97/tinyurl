# Java 컨벤션

이 프로젝트의 Java 코드 작성 시 지키는 스타일 규칙을 정리한다.

## 클래스 멤버 순서

클래스 안에서 필드와 메서드를 나열하는 순서는 다음 기준을 따른다.

### 필드 순서

의존성 필드는 인터페이스 타입을 먼저 두고, 구현체(concrete class) 타입을 뒤에 둔다.

```java
private final ShortUrlRepository shortUrlRepository;       // interface
private final ClickCountRepository clickCountRepository;   // interface
private final ClockProvider clockProvider;                 // interface
private final ShortUrlCacheRepository shortUrlCacheRepository; // concrete class
```

### 메서드 순서

접근 제어자 기준으로 `public` → `package-private`(default) → `private` 순서로 둔다. `private`
메서드는 그것을 호출하는 `public`/`package-private` 메서드보다 항상 아래에 위치한다.

```java
@Transactional(readOnly = true)
ShortUrl find(String shortKey) {          // package-private
    ...
}

@Transactional(readOnly = true)
Page<ShortUrlWithClickCount> findAll(PageParam pageParam) {  // package-private
    ...
}

private ShortUrl findValidByShortKey(String shortKey) {      // private, 맨 아래
    ...
}
```

### 규칙

- 같은 접근 제어자 안에서의 순서는 강제하지 않는다(호출 순서/의미적 그룹핑 등 자유롭게 판단).
- 필드/메서드 정렬은 새로 작성하거나 수정하는 클래스에 적용한다. 기존 코드를 이 컨벤션만을
  위해 일괄로 리팩터링하지는 않는다.
