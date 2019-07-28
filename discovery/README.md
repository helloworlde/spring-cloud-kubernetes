# Spring Cloud 使用 Kubernetes 作为注册中心

> Spring Cloud 可以使用 Kubernetes 作为注册中心，实现服务注册和发现

创建两个应用，Consumer 和 Provider，Provider 提供一个 REST 接口供 Consumer 调用

## Provider 

### 添加依赖

- build.gradle 

```groovy
dependencies {
    compile project(":discovery/common")

    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.cloud:spring-cloud-starter-kubernetes-all'

    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'

    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}
```

### 添加配置 

- application.properties 

指定服务的名称，用于实现调用

```properties
spring.application.name=provider-service
server.port=8082
```

- ProviderApplication.java

添加 `@EnableDiscoveryClient`启用服务发现

```java
@SpringBootApplication
@EnableDiscoveryClient
public class ProviderApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProviderApplication.class, args);
    }
}
```

### 添加接口 

```java
    @GetMapping("/ping")
    @ResponseBody
    public String ping() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "Pong";
        }
    }
```

### 部署到 Kubernetes

- Dockerfile

```dockerfile
FROM openjdk:8-jdk-alpine
VOLUME /tmp
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone
ARG JAR_FILE
ADD discovery/provider/build/libs/provider-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom", "-Duser.timezone=GMT+08", "-jar","/app.jar"]
```

构建并上传镜像 

- provider-service.yaml 

```yaml
apiVersion: v1
kind: Service
metadata:
  name: provider-service
  labels:
    app.kubernetes.io/name: provider-service
spec:
  type: ClusterIP
  ports:
    - port: 8082
      targetPort: 8082
      protocol: TCP
      name: http
  selector:
    app.kubernetes.io/name: provider-service

---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: provider-service
  labels:
    app.kubernetes.io/name: provider-service
spec:
  replicas: 1
  selector:
    matchLabels:
      app.kubernetes.io/name: provider-service
  template:
    metadata:
      labels:
        app.kubernetes.io/name: provider-service
        app.kubernetes.io/instance: sad-markhor
    spec:
      containers:
        - name: provider-service
          image: "docker.io/hellowoodes/spring-cloud-k8s-provider:1.2"
          imagePullPolicy: IfNotPresent
          ports:
            - name: http
              containerPort: 8082
              protocol: TCP
```

## Consumer 

### 添加依赖

- build.gradle 

```groovy
dependencies {
    compile project(":discovery/common")

    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'org.springframework.boot:spring-boot-starter-web'

    implementation 'org.springframework.cloud:spring-cloud-starter-kubernetes-all'
    implementation 'org.springframework.cloud:spring-cloud-starter-openfeign'
    implementation 'org.springframework.cloud:spring-cloud-starter-netflix-ribbon'
    implementation 'io.github.openfeign:feign-java8'

    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'

    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}
```

不同的是 Consumer 还添加了 Feign 和 Ribbon 的依赖

### 添加配置

- application.properties

```properties
spring.application.name=consumer-service
server.port=8081
```

- ConsumerApplication.java

启用服务发现和 Feign 远程调用

```java
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class ConsumerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConsumerApplication.class, args);
    }

}
```

### 添加接口

- ConsumerController.java

```java
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
```

- ProviderClient.java

```java
@FeignClient(name = "provider-service", fallback = ProviderClientFallback.class)
public interface ProviderClient {

    @RequestMapping(value = "/ping", method = RequestMethod.GET)
    String ping();
}

@Component
class ProviderClientFallback implements ProviderClient {

    @Override
    public String ping() {
        return "Error";
    }
}
```

### 部署到 Kubernetes

- Dockerfile

```dockerfile
FROM openjdk:8-jdk-alpine
VOLUME /tmp
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone
ARG JAR_FILE
ADD discovery/consumer/build/libs/consumer-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom", "-Duser.timezone=GMT+08", "-jar","/app.jar"]
```

构建并上传镜像 

- consumer-service.yaml

```yaml
apiVersion: v1
kind: Service
metadata:
  name: consumer-service
  labels:
    app.kubernetes.io/name: consumer-service
spec:
  type: NodePort
  ports:
    - port: 8081
      nodePort: 30081
      protocol: TCP
      name: http
  selector:
    app.kubernetes.io/name: consumer-service

---

apiVersion: apps/v1
kind: Deployment
metadata:
  name: consumer-service
  labels:
    app.kubernetes.io/name: consumer-service
spec:
  replicas: 1
  selector:
    matchLabels:
      app.kubernetes.io/name: consumer-service
  template:
    metadata:
      labels:
        app.kubernetes.io/name: consumer-service
    spec:
      containers:
        - name: consumer-service
          image: "docker.io/hellowoodes/spring-cloud-k8s-consumer:1.2"
          imagePullPolicy: IfNotPresent
          ports:
            - name: http
              containerPort: 8081
              protocol: TCP
```

## 部署测试

### 部署


```bash
kubectl apply -f discovery/provider/provider-service.yaml
kubectl apply -f discovery/consumer/consumer-service.yaml
```

### 测试 

待部署完成后，访问 `${NODE_IP}:30081/${PATH}`即可得到服务信息

#### 测试接口

- ping 

```bash
curl http://192.168.0.110:30081/ping
{"success":true,"message":null,"data":"provider-service-bb8844f9b-c74rj"}%
```

- service

```bash
curl http://192.168.0.110:30081/service
["backend","consumer-service","kubernetes","provider-service","traefik","traefik-dashboard"]%
```

- instance 

```bash
curl http://192.168.0.110:30081/instance\?instanceId\=provider-service

[{"instanceId":"87f35232-cb2a-4a35-a094-f4577bce195e","serviceId":"provider-service","secure":false,"metadata":{"app.kubernetes.io/name":"provider-service","kubectl.kubernetes.io/last-applied-configuration":"{\"apiVersion\":\"v1\",\"kind\":\"Service\",\"metadata\":{\"annotations\":{},\"labels\":{\"app.kubernetes.io/name\":\"provider-service\"},\"name\":\"provider-service\",\"namespace\":\"default\"},\"spec\":{\"ports\":[{\"name\":\"http\",\"port\":8082,\"protocol\":\"TCP\",\"targetPort\":8082}],\"selector\":{\"app.kubernetes.io/name\":\"provider-service\"},\"type\":\"ClusterIP\"}}\n","port.http":"8082"},"uri":"http://10.32.0.8:8082","scheme":"http://","host":"10.32.0.8","port":8082}]%
```

#### 添加 Provider-Service 实例

```bash
kubectl scale deploy/provider-service --replicas=3
```

待扩容完成后，多次访问 ping 接口，会看到在轮询访问每个 Provider 实例，每次返回的实例 ID 都不一样

```bash
curl http://192.168.0.110:30081/ping
{"success":true,"message":null,"data":"provider-service-bb8844f9b-lm589"}%                                                                                                                                       

curl http://192.168.0.110:30081/ping
{"success":true,"message":null,"data":"provider-service-bb8844f9b-n5szb"}%                                                                                                                                       

curl http://192.168.0.110:30081/ping
{"success":true,"message":null,"data":"provider-service-bb8844f9b-c74rj"}%
```
