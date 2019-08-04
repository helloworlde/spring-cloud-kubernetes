package io.github.helloworlde.secrets.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author HelloWood
 */
@Data
@Component
@ConfigurationProperties(prefix = "spring.datasource")
public class ConfigProperties {

    private String url;

    private String username;

    private String password;

}
