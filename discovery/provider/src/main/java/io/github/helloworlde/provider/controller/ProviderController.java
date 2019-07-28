package io.github.helloworlde.provider.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @author HelloWood
 */
@RestController
@Slf4j
public class ProviderController {

    @GetMapping("/ping")
    @ResponseBody
    public String ping() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "Pong";
        }
    }
}
