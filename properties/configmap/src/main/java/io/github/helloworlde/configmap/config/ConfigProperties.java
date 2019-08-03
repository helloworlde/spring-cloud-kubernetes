package io.github.helloworlde.configmap.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author HelloWood
 */
@Data
@Component
@ConfigurationProperties(prefix = "config")
public class ConfigProperties {

    private String applicationVersion;

}
