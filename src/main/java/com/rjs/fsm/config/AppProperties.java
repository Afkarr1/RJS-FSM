package com.rjs.fsm.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app")
@Getter @Setter
public class AppProperties {

    private TenantProps tenant = new TenantProps();
    private StorageProps storage = new StorageProps();
    private FonnteProps fonnte = new FonnteProps();
    private String baseUrl = "http://localhost:8080";

    @Getter @Setter
    public static class TenantProps {
        private String defaultId = "00000000-0000-0000-0000-000000000001";
    }

    @Getter @Setter
    public static class StorageProps {
        private String uploadDir = "./uploads";
        private int maxImageWidth = 1920;
        private double compressQuality = 0.80;
    }

    @Getter @Setter
    public static class FonnteProps {
        private String apiKey = "";
        private String sender = "";
    }
}
