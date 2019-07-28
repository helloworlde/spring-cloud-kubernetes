package io.github.helloworlde.consumer.controller;

import io.github.helloworlde.common.OperationResponse;
import io.github.helloworlde.consumer.feign.ProviderClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author HelloWood
 */
@RestController
@Slf4j
public class ConsumerController {

    @Autowired
    private DiscoveryClient discoveryClient;

    @Autowired
    private ProviderClient providerClient;


    @GetMapping("/service")
    public Object getClient() {
        return discoveryClient.getServices();
    }

    @GetMapping("/instance")
    public List<ServiceInstance> getInstance(String instanceId) {
        return discoveryClient.getInstances(instanceId);
    }

    @GetMapping("/ping")
    public OperationResponse ping() {
        return OperationResponse
                .builder()
                .success(true)
                .data(providerClient.ping())
                .build();
    }
}
