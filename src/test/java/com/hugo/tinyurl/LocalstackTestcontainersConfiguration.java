package com.hugo.tinyurl;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.localstack.LocalStackContainer.Service;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class LocalstackTestcontainersConfiguration {

    static final String TEST_ARCHIVE_BUCKET = "tinyurl-expired-archive-test";

    @Bean
    LocalStackContainer localStackContainer() {
        return new LocalStackContainer(DockerImageName.parse("localstack/localstack:3.8")).withServices(Service.S3).withReuse(true);
    }

    @Bean
    DynamicPropertyRegistrar s3Properties(LocalStackContainer localStackContainer) throws Exception {
        // 버킷은 LocalStack init 스크립트가 아니라 여기서 직접 만든다 - 테스트 전용 버킷 이름을 앱 설정과 분리하기 위함.
        localStackContainer.execInContainer("awslocal", "s3", "mb", "s3://" + TEST_ARCHIVE_BUCKET);
        return registry -> {
            registry.add("app.storage.s3.endpoint", () -> localStackContainer.getEndpointOverride(Service.S3).toString());
            registry.add("app.storage.s3.region", localStackContainer::getRegion);
            registry.add("app.storage.s3.bucket", () -> TEST_ARCHIVE_BUCKET);
        };
    }

}
