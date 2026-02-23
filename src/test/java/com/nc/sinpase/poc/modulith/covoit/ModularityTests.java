package com.nc.sinpase.poc.modulith.covoit;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularityTests {

    ApplicationModules modules = ApplicationModules.of(CovoitBackendApplication.class);

    @Test
    void verifiesModularStructure() {
        modules.verify();
    }
}
