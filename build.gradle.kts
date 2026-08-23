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

    // 분산 캐시
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    // 분산 Rate Limiting(Redis Lettuce 기반 토큰 버킷)
    implementation("com.bucket4j:bucket4j_jdk17-core:8.14.0")
    implementation("com.bucket4j:bucket4j_jdk17-lettuce:8.14.0")

    // 분산 유일 카운터(ZooKeeper)
    implementation("org.apache.curator:curator-recipes:5.9.0")

    // 클릭 이벤트 비동기 발행/구독(Kafka)
    implementation("org.springframework.boot:spring-boot-starter-kafka")

    // 인증/인가(필터 체인) — spring-security-crypto(비밀번호 해시)도 여기 포함돼 있어
    // 별도로 선언하지 않는다
    implementation("org.springframework.boot:spring-boot-starter-security")

    // JWT 발급/검증
    implementation("io.jsonwebtoken:jjwt-api:0.13.0")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.13.0")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.13.0")

    // 카운터 값을 예측 어려운 문자열로 변환(검증된 오픈소스 알고리즘)
    implementation("org.sqids:sqids:0.1.0")

    // Observability: Actuator + Log4j2 + OpenTelemetry(LGTM 스택 연동)
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-log4j2")
    implementation("org.springframework.boot:spring-boot-starter-opentelemetry")
    implementation("io.opentelemetry.instrumentation:opentelemetry-log4j-appender-2.17:2.28.0-alpha")
    implementation("net.ttddyy.observation:datasource-micrometer-spring-boot:2.2.1")

    // S3
    implementation(platform("software.amazon.awssdk:bom:2.46.7"))
    implementation("software.amazon.awssdk:s3")

    // Spring Modulith — 모듈 경계 검증 + 모듈 간 통신용 애플리케이션 이벤트(발행 레지스트리 포함)
    implementation("org.springframework.modulith:spring-modulith-starter-core")
    implementation("org.springframework.modulith:spring-modulith-starter-jpa")
    runtimeOnly("org.springframework.modulith:spring-modulith-actuator")
    runtimeOnly("org.springframework.modulith:spring-modulith-observability")
    testImplementation("org.springframework.modulith:spring-modulith-starter-test")

    // 코드 생성
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    // 개발 편의
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    developmentOnly("org.springframework.boot:spring-boot-docker-compose")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-security-test")
    testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    testImplementation("org.springframework.boot:spring-boot-starter-flyway-test")
    testImplementation("org.springframework.boot:spring-boot-starter-actuator-test")
    testImplementation("org.springframework.boot:spring-boot-starter-opentelemetry-test")
    testImplementation("org.springframework.boot:spring-boot-starter-restdocs")
    testImplementation("org.springframework.restdocs:spring-restdocs-mockmvc")
    testImplementation("org.springframework.boot:spring-boot-restclient")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.testcontainers:testcontainers-mysql")
    testImplementation("com.redis:testcontainers-redis:2.2.4")
    testImplementation("org.testcontainers:testcontainers-kafka")
    testImplementation("org.testcontainers:localstack:1.21.3")
    testImplementation("org.awaitility:awaitility")
    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

val springModulithVersion by extra("2.1.0")
dependencyManagement {
    imports {
        mavenBom("org.springframework.modulith:spring-modulith-bom:$springModulithVersion")
    }
}

tasks.jar {
    enabled = false
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

tasks.register<Copy>("copyDocs") {
    description = "생성된 API 문서를 bootJar 패키징용 디렉터리로 복사한다"
    dependsOn(tasks.asciidoctor)
    from(tasks.asciidoctor.get().outputDir)
    into(layout.buildDirectory.dir("generated-docs"))
}

tasks.bootJar {
    dependsOn("copyDocs")
    into("BOOT-INF/classes/static/docs") {
        from(layout.buildDirectory.dir("generated-docs"))
    }
}
