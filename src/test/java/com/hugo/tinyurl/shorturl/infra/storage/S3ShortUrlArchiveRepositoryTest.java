package com.hugo.tinyurl.shorturl.infra.storage;

import static org.assertj.core.api.Assertions.assertThat;

import com.hugo.tinyurl.TestcontainersConfiguration;
import com.hugo.tinyurl.TinyurlApplication;
import com.hugo.tinyurl.shorturl.model.ShortUrl;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;

@SpringBootTest(classes = TinyurlApplication.class, webEnvironment = WebEnvironment.NONE)
@Import(TestcontainersConfiguration.class)
class S3ShortUrlArchiveRepositoryTest {

    @Autowired
    S3ShortUrlArchiveRepository s3ShortUrlArchiveRepository;

    @Autowired
    S3Client s3Client;

    @Value("${app.storage.s3.bucket}")
    String bucket;

    @Test
    void archivesShortUrlsAsJsonObjectInBucket() {
        ShortUrl shortUrl = new ShortUrl(9_000_000L, "abcd1234", "https://example.com", null,
            LocalDateTime.now().minusDays(1), LocalDateTime.now().minusDays(8));

        s3ShortUrlArchiveRepository.archive(List.of(shortUrl), List.of(), List.of());

        var objects = s3Client.listObjectsV2(ListObjectsV2Request.builder()
            .bucket(bucket)
            .prefix("expired-short-url/")
            .build());
        assertThat(objects.contents()).isNotEmpty();
    }

}
