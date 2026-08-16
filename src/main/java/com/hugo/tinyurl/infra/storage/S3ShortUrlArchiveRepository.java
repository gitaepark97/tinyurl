package com.hugo.tinyurl.infra.storage;

import com.hugo.tinyurl.clickevent.model.ClickCount;
import com.hugo.tinyurl.clickevent.model.ClickEvent;
import com.hugo.tinyurl.common.port.ClockProvider;
import com.hugo.tinyurl.domain.model.ShortUrl;
import com.hugo.tinyurl.domain.port.ShortUrlArchiveRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import tools.jackson.databind.ObjectMapper;

@Component
class S3ShortUrlArchiveRepository implements ShortUrlArchiveRepository {

    private final ClockProvider clockProvider;
    private final S3Client s3Client;
    private final ObjectMapper objectMapper;
    private final String bucket;

    S3ShortUrlArchiveRepository(
        ClockProvider clockProvider,
        S3Client s3Client,
        ObjectMapper objectMapper,
        @Value("${app.storage.s3.bucket}") String bucket
    ) {
        this.clockProvider = clockProvider;
        this.s3Client = s3Client;
        this.objectMapper = objectMapper;
        this.bucket = bucket;
    }

    @Override
    public void archive(List<ShortUrl> shortUrls, List<ClickEvent> clickEvents, List<ClickCount> clickCounts) {
        byte[] payload = objectMapper.writeValueAsBytes(new ArchivePayload(shortUrls, clickEvents, clickCounts));
        s3Client.putObject(
            PutObjectRequest.builder().bucket(bucket).key(buildKey(shortUrls, clickEvents)).contentType("application/json").build(),
            RequestBody.fromBytes(payload));
    }

    // 배치 내용(id 범위)으로 키를 결정해서, 삭제 실패로 같은 배치가 재시도되면 새 오브젝트가 아니라 같은 키를 덮어쓰게 한다.
    private String buildKey(List<ShortUrl> shortUrls, List<ClickEvent> clickEvents) {
        String shortUrlRange = idRange(shortUrls.stream().map(ShortUrl::id).toList());
        String clickEventRange = clickEvents.isEmpty() ? "none" : idRange(clickEvents.stream().map(ClickEvent::id).toList());
        return "expired-short-url/%s/%s_%s.json".formatted(clockProvider.now().toLocalDate(), shortUrlRange, clickEventRange);
    }

    private String idRange(List<Long> ids) {
        return "%d-%d-%d".formatted(ids.get(0), ids.get(ids.size() - 1), ids.size());
    }

    private record ArchivePayload(List<ShortUrl> shortUrls, List<ClickEvent> clickEvents, List<ClickCount> clickCounts) {

    }

}
