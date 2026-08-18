package com.hugo.tinyurl;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ApplicationModulesTest {

    @Test
    void verifiesModularStructure() {
        ApplicationModules.of(TinyurlApplication.class).verify();
    }

}
