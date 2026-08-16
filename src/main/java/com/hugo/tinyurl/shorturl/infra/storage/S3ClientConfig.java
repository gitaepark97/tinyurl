package com.hugo.tinyurl.shorturl.infra.storage;

import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
class S3ClientConfig {

    @Bean
    S3Client s3Client(
        @Value("${app.storage.s3.region}") String region,
        @Value("${app.storage.s3.endpoint:}") String endpoint,
        @Value("${app.storage.s3.access-key:}") String accessKey,
        @Value("${app.storage.s3.secret-key:}") String secretKey
    ) {
        var builder = S3Client.builder().region(Region.of(region));
        // LocalStack 등 로컬 개발용 엔드포인트가 있을 때만 오버라이드한다 - 운영은 기본 AWS 엔드포인트를 쓴다.
        if (!endpoint.isBlank()) {
            builder = builder.endpointOverride(URI.create(endpoint)).forcePathStyle(true);
        }
        // 자격 증명이 명시된 경우에만 고정 값을 쓰고, 그 외엔 기본 체인(IAM Role 등)을 그대로 쓴다.
        if (!accessKey.isBlank() && !secretKey.isBlank()) {
            builder = builder.credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)));
        }
        return builder.build();
    }

}
