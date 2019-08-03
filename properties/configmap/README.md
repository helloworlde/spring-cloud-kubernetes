# Spring Cloud 使用 Kubernetes 作为配置中心

> Spring Cloud 可以通过使用 Kubernetes 的 ConfigMap 作为配置中心，实现配置的拉取和刷新

## 创建应用
 
### 添加依赖

- build.gradle 

```groovy
dependencies {
	implementation 'org.springframework.cloud:spring-cloud-starter-kubernetes-config'
}	
```

### 添加配置

#### ConfigMap 

```
kind: ConfigMap
apiVersion: v1
metadata:
  name: config-map-service
data:
  application.yaml: |-
    spring:
      profiles: dev
    config:
        applicationVersion: dev-0.0.1
    ---
    spring:
      profiles: prod
    config:
        applicationVersion: prod-0.0.1
```

ConfigMap的名称要和应用名称一致，否则需要指定相应的名称；应用的配置信息可以使用`application.yaml|properties`作为 key，需要注意的是环境配置是`spring.profiles`，而不是`spring.profiles.active`，否则只会使用最后一行的配置

通过以下命令将 ConfigMap 添加到 Kubernetes 中

```bash
kubectl apply -f config-map.yaml 
```

#### application.properties

```
spring.application.name=config-map-service
spring.profiles.active=${PROFILE:dev}
management.endpoint.restart.enabled=true
# Reload Config
spring.cloud.kubernetes.reload.enabled=true
spring.cloud.kubernetes.reload.mode=polling
spring.cloud.kubernetes.reload.period=5000
spring.cloud.kubernetes.reload.strategy=refresh
```

- `spring.profiles.active=${PROFILE:dev}`用于获取 Deployment 传递的参数来决定使用什么配置

- `spring.cloud.kubernetes.reload.enabled`默认是关闭的，所以需要主动开启
- `spring.cloud.kubernetes.reload.mode`有两种模式，`polling`和`event`：
	- `polling`会启动一个定时任务，定时拉取配置，时间由`spring.cloud.kubernetes.reload.period`配置
	- `event`会监听 Kubernetes 的事件，当 ConfigMap 或者 Secrets 发生变化时会触发配置更新事件
- `spring.cloud.kubernetes.reload.strategy`配置更新策略，有 `refresh`,`restart_context`和`shutdown`：
	- `refresh`可以通过设置`@RefreshContext`或者`@ConfigurationProperties`更新配置
	- `restart_context`会主动重启应用
	- `shutdown`会关闭应用，由 Deployment 检测存活失败后重启应用

需要注意的是，开启了 reload 后，还需要允许应用重启`management.endpoint.restart.enabled=true`，否则会提示有两个用于重启的 Bean 冲突

### 添加接口

- ConfigProperties.java

```java
@Data
@Component
@ConfigurationProperties(prefix = "config")
public class ConfigProperties {
    private String applicationVersion;
}
```

- ConfigMapController.java

```java
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
```

### 部署应用 

- 构建并上传镜像

- config-map-service.yaml

```yaml
apiVersion: v1
kind: Service
metadata:
  name: config-map-service
  labels:
    app.kubernetes.io/name: config-map-service
spec:
  type: NodePort
  ports:
    - port: 8081
      nodePort: 30081
      protocol: TCP
      name: http
  selector:
    app.kubernetes.io/name: config-map-service

---

apiVersion: apps/v1
kind: Deployment
metadata:
  name: config-map-service
  labels:
    app.kubernetes.io/name: config-map-service
spec:
  replicas: 1
  selector:
    matchLabels:
      app.kubernetes.io/name: config-map-service
  template:
    metadata:
      labels:
        app.kubernetes.io/name: config-map-service
    spec:
      containers:
        - name: config-map-service
          image: "hellowoodes/spring-cloud-k8s-config-map:1.2"
          imagePullPolicy: IfNotPresent
          env:
            - name: PROFILE
              value: prod
          ports:
            - name: http
              containerPort: 8081
              protocol: TCP
```

其中Deployment 的`spec.template.spec.env`的`PROFILE`用于指定环境

```bash
kubectl apply -f config-map-service.yaml
```

### 测试

待应用启动后， 访问相应的 URL 

- 查看此时的配置

```bash
curl 192.168.0.110:30081/profile
prod%
```

- 查看配置的版本

```bash
curl 192.168.0.110:30081/version
prod-0.0.1%
```

修改应用配置，将版本号改为 `prod-0.0.2`并应用到 Kubernetes 中后再次访问

```bash
curl 192.168.0.110:30081/version
prod-0.0.2%
```

- 修改`spec.template.spec.env.PROFILE`配置为 dev 并应用到 Kubernetes 中

```bash
curl 192.168.0.110:30081/profile
dev%
```

查看版本

```bash
curl 192.168.0.110:30081/version
dev-0.0.1%
```