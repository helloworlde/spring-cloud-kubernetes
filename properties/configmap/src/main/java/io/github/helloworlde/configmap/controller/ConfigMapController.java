package io.github.helloworlde.configmap.controller;

import io.github.helloworlde.configmap.config.ConfigProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author HelloWood
 */
@RestController
@Slf4j
public class ConfigMapController {

    @Autowired
    private ConfigProperties configProperties;

    @Value("${spring.profiles.active}")
    private String profile;


    @GetMapping("/version")
    public String config() {
        return configProperties.getApplicationVersion();
    }

    @GetMapping("/profile")
    public String profile() {
        return profile;
    }

}
