package io.github.helloworlde.secrets.controller;

import io.github.helloworlde.secrets.config.ConfigProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author HelloWood
 */
@RestController
@Slf4j
public class SecretsController {

    @Autowired
    private ConfigProperties configProperties;

    @GetMapping("/db")
    public String config() {
        return String.format("url:%s\nusername:%s\npassword:%s",
                configProperties.getUrl(),
                configProperties.getUsername(),
                configProperties.getPassword());
    }
}
