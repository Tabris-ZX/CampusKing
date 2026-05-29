package zx.campusking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import zx.campusking.config.AssetProperties;

@SpringBootApplication
@EnableConfigurationProperties(AssetProperties.class)
public class CampusKingApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampusKingApplication.class, args);
    }

}
