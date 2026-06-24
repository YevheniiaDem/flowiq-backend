package com.flowiq;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;
import com.flowiq.config.FeatureFlags;
import com.flowiq.audit.config.AuditProperties;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({FeatureFlags.class, AuditProperties.class})
public class FlowiqBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(FlowiqBackendApplication.class, args);
	}

}
