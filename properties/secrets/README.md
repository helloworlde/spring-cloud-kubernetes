# Spring Cloud 使用 Kubernetes 作为配置中心 - 使用加密配置

> Spring Cloud 可以通过使用 Kubernetes 的 Secrets 作为加密配置

## 创建应用
 
### 添加依赖

- build.gradle 

```groovy
dependencies {
	implementation 'org.springframework.cloud:spring-cloud-starter-kubernetes-config'
}	
```

### 添加配置

#### 编码 

内容通过 Base64 编码后添加到 Kubernetes 中

- secrets.yaml
 
```bash
apiVersion: v1
kind: Secret
metadata:
  name: secrets-service
  namespace: default
data:
  url: amRiYzpteXNxbDovL2xvY2FsaG9zdDozMzA2L3NlYXRhP3VzZVVuaWNvZGU9dHJ1ZSZjaGFyYWN0ZXJFbmNvZGluZz11dGY4JmFsbG93TXVsdGlRdWVyaWVzPXRydWUmdXNlU1NMPWZhbHNl
  username: aGVsbG93b29k
  password: MTIzNDU2
```

将Secrets 添加到 Kubernetes 中

```bash
kubectl apply -f secrets.yaml
```

#### application.properties

```properties
spring.application.name=secrets-service
server.port=8081
management.endpoint.restart.enabled=true
# Secret Config
spring.cloud.kubernetes.secrets.enabled=true
spring.cloud.kubernetes.secrets.name=secrets-service
spring.cloud.kubernetes.secrets.namespace=default
spring.cloud.kubernetes.reload.monitoring-secrets=true
# DataSource Config
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
```

- `spring.cloud.kubernetes.secrets.enabled`默认是关闭的，所以需要主动开启
- `spring.cloud.kubernetes.secrets.name`默认和`spring.application.name`一致
- `spring.cloud.kubernetes.secrets.namespace`指定 Namespace
- `spring.cloud.kubernetes.reload.monitoring-secrets`监听配置更新事件


### 添加接口

- ConfigProperties.java

```java
@Data
@Component
@ConfigurationProperties(prefix = "spring.datasource")
public class ConfigProperties {
    private String url;
    private String username;
    private String password;
}
```

- ConfigMapController.java

```java
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
```

### 部署应用 

- 构建并上传镜像

- secrets-service.yaml

```yaml
apiVersion: v1
kind: Service
metadata:
  name: secrets-service
  labels:
    app.kubernetes.io/name: secrets-service
spec:
  type: NodePort
  ports:
    - port: 8081
      nodePort: 30082
      protocol: TCP
      name: http
  selector:
    app.kubernetes.io/name: secrets-service

---

apiVersion: apps/v1
kind: Deployment
metadata:
  name: secrets-service
  labels:
    app.kubernetes.io/name: secrets-service
spec:
  replicas: 1
  selector:
    matchLabels:
      app.kubernetes.io/name: secrets-service
  template:
    metadata:
      labels:
        app.kubernetes.io/name: secrets-service
    spec:
      containers:
        - name: secrets-service
          image: "hellowoodes/spring-cloud-k8s-secrets:1.3"
          imagePullPolicy: IfNotPresent
          env:
            - name: DB_URL
              valueFrom:
                secretKeyRef:
                  name: secrets-service
                  key: url
            - name: DB_USERNAME
              valueFrom:
                secretKeyRef:
                  name: secrets-service
                  key: username
            - name: DB_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: secrets-service
                  key: password
          ports:
            - name: http
              containerPort: 8081
              protocol: TCP
```

其中Deployment 的`spec.template.spec.env`的参数从 secrets 中获取

```bash
kubectl apply -f secrets-service.yaml
```

### 测试

待应用启动后， 访问相应的 URL，查看此时的配置

```bash
curl 192.168.0.110:30082/db
url:jdbc:mysql://localhost:3306/seata?useUnicode=true&characterEncoding=utf8&allowMultiQueries=true&useSSL=false
username:root
password:123456%
```