package tr.com.allianz.ysv.services;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Entry point of the SBM YSV declaration transfer service.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class DeclarationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DeclarationServiceApplication.class, args);
    }
}
