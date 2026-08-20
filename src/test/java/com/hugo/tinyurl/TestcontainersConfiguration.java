package com.hugo.tinyurl;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

// 전체 인프라가 다 필요한 테스트(shorturl/clickevent, 전체 컨텍스트 테스트 등)용 조합이다.
// 필요한 인프라가 일부뿐이면 아래 개별 XxxTestcontainersConfiguration을 직접 @Import한다.
@TestConfiguration(proxyBeanMethods = false)
@Import({
    MySqlTestcontainersConfiguration.class,
    RedisTestcontainersConfiguration.class,
    ZookeeperTestcontainersConfiguration.class,
    KafkaTestcontainersConfiguration.class,
    LocalstackTestcontainersConfiguration.class
})
public class TestcontainersConfiguration {
}
