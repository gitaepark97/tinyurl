plugins {
    java
    id("org.springframework.boot") version "4.1.0"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.asciidoctor.jvm.convert") version "4.0.5"
}

group = "com.hugo"
version = "0.0.1-SNAPSHOT"
description = "tinyurl"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

extra["snippetsDir"] = file("build/generated-snippets")

configurations {
    all {
        // 로깅 프레임워크로 Logback 대신 Log4j2를 쓰므로 기본 Logback을 제외한다.
        exclude(group = "org.springframework.boot", module = "spring-boot-starter-logging")
    }
}

dependencies {
    // Web / API
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // Persistence
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.flywaydb:flyway-mysql")
    runtimeOnly("com.mysql:mysql-connector-j")

    // Observability: Actuator + Log4j2 + OpenTelemetry(LGTM 스택 연동)
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-log4j2")
    implementation("org.springframework.boot:spring-boot-starter-opentelemetry")
    implementation("io.opentelemetry.instrumentation:opentelemetry-log4j-appender-2.17:2.28.0-alpha")

    // 코드 생성
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    // 개발 편의
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    developmentOnly("org.springframework.boot:spring-boot-docker-compose")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    testImplementation("org.springframework.boot:spring-boot-starter-flyway-test")
    testImplementation("org.springframework.boot:spring-boot-starter-actuator-test")
    testImplementation("org.springframework.boot:spring-boot-starter-opentelemetry-test")
    testImplementation("org.springframework.boot:spring-boot-starter-restdocs")
    testImplementation("org.springframework.restdocs:spring-restdocs-mockmvc")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.testcontainers:testcontainers-mysql")
    testImplementation("org.testcontainers:testcontainers-grafana")
    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.test {
    outputs.dir(project.extra["snippetsDir"]!!)
}

tasks.asciidoctor {
    inputs.dir(project.extra["snippetsDir"]!!)
    dependsOn(tasks.test)
    baseDirFollowsSourceFile()
    attributes(mapOf("snippets" to project.extra["snippetsDir"]))
}

// asciidoctor는 test 결과(RestDocs 스니펫)가 있어야 하고, test는 classes(컴파일 결과 +
// processResources 산출물)가 있어야 하므로, 문서를 processResources의 산출물 디렉터리
// (build/resources/main)에 다시 섞어 넣으면 항상 순환 의존성이 생긴다. 그래서 문서는
// 완전히 독립된 디렉터리에 모아두고, bootJar가 패키징 시점에 그 디렉터리를
// BOOT-INF/classes/static/docs 위치로 직접 포함시킨다(Boot이 static 리소스를 읽는 경로).
tasks.register<Copy>("copyDocs") {
    dependsOn(tasks.asciidoctor)
    from(tasks.asciidoctor.get().outputDir)
    into(layout.buildDirectory.dir("generated-docs"))
}

// bootRun/IDE 실행은 이 문서를 보지 않는다(빌드된 jar가 아니라 컴파일된 클래스를 그대로
// 띄우는 방식이라 bootJar 패키징 단계 자체를 안 거치기 때문). API 문서 확인은 항상
// jar를 빌드해서 실행하는 것으로 통일한다 — src 트리에 복사하는 방식은 순서 문제와
// bootJar 중복 문제를 같이 끌고 와서 배보다 배꼽이 컸다.
tasks.bootJar {
    dependsOn("copyDocs")
    into("BOOT-INF/classes/static/docs") {
        from(layout.buildDirectory.dir("generated-docs"))
    }
}
