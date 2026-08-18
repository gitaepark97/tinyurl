package com.hugo.tinyurl;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

// 산출물 자체를 assert하지 않는다 - 예외 없이 생성되는지만 확인하는 스모크 테스트다.
// 코드가 바뀌면 다음 빌드 때 build/spring-modulith-docs 아래 산출물이 자동으로 최신화된다.
class DocumenterTest {

    @Test
    void writesModuleDocumentation() {
        ApplicationModules modules = ApplicationModules.of(TinyurlApplication.class);
        new Documenter(modules)
            .writeModulesAsPlantUml()
            .writeIndividualModulesAsPlantUml()
            .writeModuleCanvases();
    }

}
