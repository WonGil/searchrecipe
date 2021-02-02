# Search Recipe

# 서비스 시나리오
## 기능적 요구사항
1. 회원이 레시피를 검색한다.
1. 회원이 검색된 내용을 보고 부족한 재료를 주문한다.
1. 주문이 되면 주문 내역에 대한 배송이 시작된다.
1. 배송 정보는 주문 정보에 업데이트 된다.
1. 고객이 주문을 취소할 수 있다.
1. 주문이 취소되면 배송도 취소된다.
1. 고객이 주문상태를 중간중간 조회한다.

## 비기능적 요구사항
1. 트랜잭션
    1. 고객이 주문을 취소하면 주문도 즉시 주문이 취소된다. → Sync 호출
1. 장애격리
    1. 배송 서비스가 정상 기능이 되지 않더라도 주문을 받을 수 있다. → Async (event-driven), Eventual Consistency
    1. 주문시스템이 과중되면 사용자를 잠시동안 받지 않고 주문을 잠시후에 하도록 유도한다 → Circuit breaker, fallback
1. 성능
    1. 고객이 배달상태를 주문시스템에서 확인할 수 있어야 한다. → CQRS 

# 체크포인트
https://workflowy.com/s/assessment/qJn45fBdVZn4atl3

# 분석/설계
## AS-IS 조직 (Horizontally-Aligned)
![image](https://user-images.githubusercontent.com/16534043/106468971-f7a2e880-64e1-11eb-9e3e-faf334166094.png)
## TO-BE 조직 (Vertically-Aligned)
![image](https://user-images.githubusercontent.com/16534043/106469623-de4e6c00-64e2-11eb-9c5d-bd3d43fa6340.png)
## EventStorming 결과
### 완성된 1차 모형
![image](https://user-images.githubusercontent.com/12531980/106534309-28f9d380-6537-11eb-878b-ae136d43cdcc.png)

### 1차 완성본에 대한 기능적/비기능적 요구사항을 커버하는지 검증
![image](https://user-images.githubusercontent.com/12531980/106551677-18f2eb80-6559-11eb-907a-7da3b69ce975.png)
```
- 고객이 등록한 레시피를 확인한다. (1, ok)
- 레시피를 등록하면 필요한 재료가 주문이 된다. (2 -> 3, ok)
- 주문 접수가 되면 배송이 되고 주문 상태가 '배송 시작'으로 변경된다. (3 -> 4, ok)
- 고객이 주문 취소를 하게 되면 배달이 취소된다. (5 -> 6, ok)
- 고객은 중간마다 주문상태를 My Page 를 통해 확인 할 수 있다. (7, ok)
```
## 헥사고날 아키텍쳐 다이어그램 도출 (Polyglot)
![image](https://user-images.githubusercontent.com/12531980/106552529-dd592100-655a-11eb-9d86-dbb94faebe62.png)

# 구현
분석/설계 단계에서 도출된 헥사고날 아키텍처에 따라, 각 BC별로 대변되는 마이크로 서비스들을 스프링부트로 구현하였다. 구현한 각 서비스를 로컬에서 실행하는 방법은 아래와 같다 (각자의 포트넘버는 8081 ~ 8084, 8088 이다)
```
cd recipe
mvn spring-boot:run  

cd order
mvn spring-boot:run

cd delivery
mvn spring-boot:run 

cd mypage
mvn spring-boot:run  

cd gateway
mvn spring-boot:run  
```

## DDD 의 적용
msaez.io 를 통해 구현한 Aggregate 단위로 Entity 를 선언 후, 구현을 진행하였다.

Entity Pattern 과 Repository Pattern 을 적용하기 위해 Spring Data REST 의 RestRepository 를 적용하였다.

```java
package searchrecipe;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;
import java.util.List;

@Entity
@Table(name="Order_table")
public class Order {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    private String materialNm;
    private Integer qty;
    private String status;

    @PostPersist
    public void onPostPersist(){
        Ordered ordered = new Ordered();
        BeanUtils.copyProperties(this, ordered);
        ordered.publishAfterCommit();
    }

    @PrePersist
    public void onPrePersist(){
        try {
            Thread.currentThread().sleep((long) (800 + Math.random() * 220));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @PreRemove
    public void onPreRemove(){
        OrderCanceled orderCanceled = new OrderCanceled();
        BeanUtils.copyProperties(this, orderCanceled);
        orderCanceled.publishAfterCommit();

        //Following code causes dependency to external APIs
        // it is NOT A GOOD PRACTICE. instead, Event-Policy mapping is recommended.

        searchrecipe.external.Cancellation cancellation = new searchrecipe.external.Cancellation();
        // mappings goes here
        cancellation.setOrderId(this.getId());
        cancellation.setStatus("Delivery Cancelled");
        OrderApplication.applicationContext.getBean(searchrecipe.external.CancellationService.class)
            .cancel(cancellation);

    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    public String getMaterialNm() {
        return materialNm;
    }

    public void setMaterialNm(String materialNm) {
        this.materialNm = materialNm;
    }
    public Integer getQty() {
        return qty;
    }

    public void setQty(Integer qty) {
        this.qty = qty;
    }
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

}

```

적용 후 REST API의 테스트를 통하여 정상적으로 동작하는 것을 확인할 수 있었다.
![image](https://user-images.githubusercontent.com/12531980/106535000-9c501500-6538-11eb-89be-f5c1078ad4c3.png)

![image](https://user-images.githubusercontent.com/12531980/106535116-d6b9b200-6538-11eb-8498-46b2d9398b79.png)

## Gateway 적용
API Gateway를 통하여 마이크로 서비스들의 진입점을 통일하였다.
```yaml
server:
  port: 8088

---

spring:
  profiles: default
  cloud:
    gateway:
      routes:
        - id: recipe
          uri: http://localhost:8081
          predicates:
            - Path=/recipes/** 
        - id: order
          uri: http://localhost:8082
          predicates:
            - Path=/orders/** 
        - id: delivery
          uri: http://localhost:8083
          predicates:
            - Path=/deliveries/**,/cancellations/**
        - id: mypage
          uri: http://localhost:8084
          predicates:
            - Path= /mypages/**
      globalcors:
        corsConfigurations:
          '[/**]':
            allowedOrigins:
              - "*"
            allowedMethods:
              - "*"
            allowedHeaders:
              - "*"
            allowCredentials: true


---

spring:
  profiles: docker
  cloud:
    gateway:
      routes:
        - id: recipe
          uri: http://recipe:8080
          predicates:
            - Path=/recipes/** 
        - id: order
          uri: http://order:8080
          predicates:
            - Path=/orders/** 
        - id: delivery
          uri: http://delivery:8080
          predicates:
            - Path=/deliveries/**,/cancellations/**
        - id: mypage
          uri: http://mypage:8080
          predicates:
            - Path= /mypages/**
      globalcors:
        corsConfigurations:
          '[/**]':
            allowedOrigins:
              - "*"
            allowedMethods:
              - "*"
            allowedHeaders:
              - "*"
            allowCredentials: true

server:
  port: 8080

```


## 폴리그랏 퍼시스턴스
- recipe의 경우, 다른 마이크로 서비스들과 달리 조회 기능도 제공해야 하기에, HSQL을 사용하여 구현하였다. 이를 통해, 마이크로 서비스 간 서로 다른 종류의 데이터베이스를 사용해도 문제 없이 동작하여 폴리그랏 퍼시스턴스를 충족시켰다.

![image](https://user-images.githubusercontent.com/12531980/106535831-70359380-653a-11eb-8e81-1654226aa9e9.png)


## 유비쿼터스 랭귀지
- 조직명, 서비스 명에서 사용되고, 업무현장에서도 쓰이며, 모든 이해관계자들이 직관적으로 의미를 이해할 수 있도록 영어 단어를 사용함 (recipe, order, delivery 등)

## 동기식 호출(Req/Res 방식)과 Fallback 처리
- 분석단계에서의 조건 중 하나로 주문 취소(order)와 배송 취소(delivery) 간의 호출은 동기식 일관성을 유지하는 트랜잭션으로 처리하기로 하였다. 호출 프로토콜은 이미 앞서 Rest Repository 에 의해 노출되어있는 REST 서비스를 FeignClient 를 이용하여 호출하도록 한다.
- 배송 취소 서비스를 호출하기 위하여 FeignClient를 이용하여 Service 대행 인터페이스(Proxy)를 구현
```java
package searchrecipe.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Date;

@FeignClient(name="delivery", url="${api.delivery.url}")
public interface CancellationService {

    @RequestMapping(method= RequestMethod.POST, path="/cancellations")
    public void cancel(@RequestBody Cancellation cancellation);

}
```
- 주문이 취소된 직후(@PreRemove) 배송이 취소되도록 처리
```java
//...
public class Order {
    //...

    @PreRemove
    public void onPreRemove(){
        OrderCanceled orderCanceled = new OrderCanceled();
        BeanUtils.copyProperties(this, orderCanceled);
        orderCanceled.publishAfterCommit();

        //Following code causes dependency to external APIs
        // it is NOT A GOOD PRACTICE. instead, Event-Policy mapping is recommended.

        searchrecipe.external.Cancellation cancellation = new searchrecipe.external.Cancellation();
        // mappings goes here
        cancellation.setOrderId(this.getId());
        cancellation.setStatus("Delivery Cancelled");
        OrderApplication.applicationContext.getBean(searchrecipe.external.CancellationService.class)
            .cancel(cancellation);
    }
    //...
}
```
- 동기식 호출에서는 호출 시간에 따른 타임 커플링이 발생하여, 주문 취소 시스템에 장애가 나면 배송도 취소되지 않는다는 것을 확인

  - 배송(Delivery) 서비스를 잠시 내려놓음 (ctrl+c)
  ![image](https://user-images.githubusercontent.com/12531980/106551276-425f4780-6558-11eb-87d0-db00d11f70cb.png)

  - 주문 취소(cancel) 요청 및 에러 난 화면 표시
  ![image]](https://user-images.githubusercontent.com/12531980/106551103-da106600-6557-11eb-8609-4593a0b7d8c2.png)

  - 배송(Delivery) 서비스 재기동 후 다시 주문 취소 요청
  ![image](https://user-images.githubusercontent.com/12531980/106551365-6d499b80-6558-11eb-84b7-b454b1df15c8.png)

## 비동기식 호출 (Pub/Sub 방식)
- Recipe.java 내에서 아래와 같이 서비스 Pub 구현
```java
//...
public class Recipe {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    private String recipeNm;
    private String cookingMethod;
    private String materialNm;
    private Integer qty;

    @PostPersist
    public void onPostPersist(){
        MaterialOrdered materialOrdered = new MaterialOrdered();
        BeanUtils.copyProperties(this, materialOrdered);
        materialOrdered.publishAfterCommit();
    }
    //...
}
```
- Order.java 내 Policy Handler 에서 아래와 같이 Sub 구현
```java
//...
@Service
public class PolicyHandler{

    //...
    @Autowired
    OrderRepository orderRepository;

    //...
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverMaterialOrdered_Order(@Payload MaterialOrdered materialOrdered){

        if(materialOrdered.isMe()){
            System.out.println("##### listener  : " + materialOrdered.toJson());
            Order order = new Order();
            order.setMaterialNm(materialOrdered.getMaterialNm());
            order.setQty(materialOrdered.getQty());
            order.setStatus("Received Order");
            orderRepository.save(order);
        }
    }
}
```
- 비동기식 호출은 다른 서비스가 비정상이여도 이상없이 동작가능하여, 주문 서비스에 장애가 나도 레시피 서비스는 정상 동작을 확인
  - Recipe 서비스와 Order 서비스가 둘 다 동시에 돌아가고 있을때 Recipe 서비스 실행시 이상 없음
  ![image](https://user-images.githubusercontent.com/12531980/106556204-5f007d00-6562-11eb-8087-e0260a54d7bd.png)
  - Order 서비스를 내림
  ![image](https://user-images.githubusercontent.com/12531980/106555946-e699bc00-6561-11eb-81de-15ea39698d35.png)
  - Recipe 서비스를 실행하여도 이상 없이 동작
  ![image](https://user-images.githubusercontent.com/12531980/106556261-7ccde200-6562-11eb-82d1-cd38eb3075fe.png)

## CQRS
viewer를 별도로 구현하여 아래와 같이 view 가 출력된다.
![image](https://user-images.githubusercontent.com/12531980/106536654-3c5b6d80-653c-11eb-8c20-2853c1743a12.png)


# 운영
## CI/CD 설정
- git에서 소스 가져오기
```
git clone http://github.com/WonGil/searchrecipe
```
- Build 하기
```
cd /searchrecipe
cd recipe
mvn package

cd ..
cd order
mvn package

cd ..
cd delivery
mvn package

cd ..
cd gateway
mvn package

cd ..
cd mypage
mvn package
```
- Dockerlizing, ACR(Azure Container Registry에 Docker Image Push하기
```
cd /searchrecipe
cd recipe
az acr build --registry skccteam02 --image skccteam02.azurecr.io/recipe:v1 .

cd ..
cd order
az acr build --registry skccteam02 --image skccteam02.azurecr.io/order:v1 .

cd ..
cd delivery
az acr build --registry skccteam02 --image skccteam02.azurecr.io/delivery:v1 .

cd ..
cd gateway
az acr build --registry skccteam02 --image skccteam02.azurecr.io/gateway:v1 .

cd ..
cd mypage
az acr build --registry skccteam02 --image skccteam02.azurecr.io/mypage:v1 .
```
- ACR에서 이미지 가져와서 Kubernetes에서 Deploy하기
```
kubectl create deploy recipe --image=skccteam02.azurecr.io/recipe:v1
kubectl create deploy order --image=skccteam02.azurecr.io/order:v1
kubectl create deploy delivery --image=skccteam02.azurecr.io/delivery:v1
kubectl create deploy gateway --image=skccteam02.azurecr.io/gateway:v1
kubectl create deploy mypage --image=skccteam02.azurecr.io/mypage:v1
kubectl get all
```
- Kubectl Deploy 결과 확인  
![image](https://user-images.githubusercontent.com/16534043/106553685-34f88c00-655d-11eb-87cb-e59a6f920a5b.png)
- Kubernetes에서 서비스 생성하기 (Docker 생성이기에 Port는 8080이며, Gateway는 LoadBalancer로 생성)
```
kubectl expose deploy recipe --type="ClusterIP" --port=8080
kubectl expose deploy order --type="ClusterIP" --port=8080
kubectl expose deploy delivery --type="ClusterIP" --port=8080
kubectl expose deploy gateway --type="LoadBalancer" --port=8080
kubectl expose deploy mypage --type="ClusterIP" --port=8080
kubectl get all
```
- Kubectl Expose 결과 확인  
![image](https://user-images.githubusercontent.com/16534043/106554016-e0a1dc00-655d-11eb-8439-f4326cecda5a.png)
- 테스트를 위해서 Kafka zookeeper와 server도 별도로 실행 필요
- deployment.yaml 편집 후 배포 방안 적어두기
## 무정지 재배포
- Autoscaler, CB 설정 제거
- 테스트 후, Readiness Probe 설정 후 kubectl apply
## 오토스케일 아웃
- 서킷 브레이커는 시스템을 안정되게 운영할 수 있게 해줬지만, 사용자의 요청이 급증하는 경우, 오토스케일 아웃이 필요하다.
- 단, 부하가 제대로 걸리기 위해서, recipe 서비스의 리소스를 줄여서 재배포한다.
```
kubectl apply -f - <<EOF
  apiVersion: apps/v1
  kind: Deployment
  metadata:
    name: recipe
    namespace: default
    labels:
      app: recipe
  spec:
    replicas: 1
    selector:
      matchLabels:
        app: recipe
    template:
      metadata:
        labels:
          app: recipe
      spec:
        containers:
          - name: recipe
            image: skccteam02.azurecr.io/recipe:v1
            ports:
              - containerPort: 8080
            resources:
              limits:
                cpu: 500m
              requests:
                cpu: 200m
EOF
```
- 다시 expose 해준다.
```
kubectl expose deploy recipe --type="ClusterIP" --port=8080

```
- recipe 시스템에 replica를 자동으로 늘려줄 수 있도록 HPA를 설정한다. 설정은 CPU 사용량이 15%를 넘어서면 replica를 10개까지 늘려준다.
```
kubectl autoscale deploy recipe --min=1 --max=10 --cpu-percent=15
```
- hpa 설정 확인  
![image](https://user-images.githubusercontent.com/16534043/106558142-9709bf00-6566-11eb-9340-12959204fee8.png)
- hpa 상세 설정 확인  
![image](https://user-images.githubusercontent.com/16534043/106558218-b3a5f700-6566-11eb-9b74-0c93679d2b31.png)
![image](https://user-images.githubusercontent.com/16534043/106558245-c0c2e600-6566-11eb-89fe-8a6178e1f976.png)
- - siege를 활용해서 워크로드를 2분간 걸어준다. (Cloud 내 siege pod에서 부하줄 것)
```
kubectl exec -it (siege POD 이름) -- /bin/bash
siege -c1000 -t120S -r100 -v --content-type "application/json" 'http://recipe:8080/recipes POST {"recipeNm": "apple_Juice"}'
```
- 오토스케일이 어떻게 되고 있는지 모니터링을 걸어둔다.
```
watch kubectl get all
```
- siege 실행 결과 표시  
![image](https://user-images.githubusercontent.com/16534043/106560612-a12dbc80-656a-11eb-8213-5a07a0a03561.png)
- 오토스케일이 되지 않아, siege 성공률이 낮다.

- 스케일 아웃이 자동으로 되었음을 확인
![image](https://user-images.githubusercontent.com/16534043/106560501-75aad200-656a-11eb-99dc-fe585ef7e741.png)
- siege 재실행
```
kubectl exec -it (siege POD 이름) -- /bin/bash
siege -c1000 -t120S -r100 -v --content-type "application/json" 'http://recipe:8080/recipes POST {"recipeNm": "apple_Juice"}'
```
- siege 의 로그를 보아도 전체적인 성공률이 높아진 것을 확인 할 수 있다.  
![image](https://user-images.githubusercontent.com/16534043/106560930-3335c500-656b-11eb-8165-bcb066a03f15.png)

## 동기식 호출 / 서킷 브레이킹 / 장애격리
- istio 사용 (Destination Rule)
- Retry 적용
- Pool Ejection
## 모니터링, 앨럿팅
- Kiali 활용
- Jager 활용
## Canary Deploy
- istio로 실행
## 운영 유연성 - Persistence Volume, Persistence Volume Claim 적용
- yaml 파일로 만들어서 붙이기
## ConfigMap 적용
- 외부 주입 ConfigMap 적용
## Secret 적용
- secret 적용





