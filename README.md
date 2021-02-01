# Search Recipe

## 서비스 시나리오
### 기능적 요구사항
1. 회원이 레시피를 검색한다.
1. 회원이 검색된 내용을 보고 부족한 재료를 주문한다.
1. 주문이 되면 주문 내역에 대한 배송이 시작된다.
1. 배송 정보는 주문 정보에 업데이트 된다.
1. 고객이 주문을 취소할 수 있다.
1. 주문이 취소되면 배송도 취소된다.
1. 고객이 주문상태를 중간중간 조회한다.

### 비기능적 요구사항
1. 트랜잭션
    1. 고객이 주문을 취소하면 주문도 즉시 주문이 취소된다. → Sync 호출
1. 장애격리
    1. 배송 서비스가 정상 기능이 되지 않더라도 주문을 받을 수 있다. → Async (event-driven), Eventual Consistency
    1. 주문시스템이 과중되면 사용자를 잠시동안 받지 않고 주문을 잠시후에 하도록 유도한다 → Circuit breaker, fallback
1. 성능
    1. 고객이 배달상태를 주문시스템에서 확인할 수 있어야 한다. → CQRS 

# 체크포인트
https://workflowy.com/s/assessment/qJn45fBdVZn4atl3

## 분석/설계
### AS-IS 조직 (Horizontally-Aligned)
![image](https://user-images.githubusercontent.com/16534043/106468971-f7a2e880-64e1-11eb-9e3e-faf334166094.png)
### TO-BE 조직 (Vertically-Aligned)
![image](https://user-images.githubusercontent.com/16534043/106469623-de4e6c00-64e2-11eb-9c5d-bd3d43fa6340.png)
### EventStorming 결과
#### 이벤트 도출
#### 부적격 이벤트 탈락
#### 폴리시 부착
#### 액터, 커맨드 부착하여 읽기 좋게
#### 어그리게잇으로 묶기
#### 바운디드 컨텍스트로 묶기
#### 폴리시의 이동과 컨텍스트 매핑 (점선은 Pub/Sub , 실선은 Req/Res)
#### 완성된 1차 모형
<img src="./images/2021-01-31 155439.png" />
#### 1차 완성본에 대한 기능적/비기능적 요구사항을 커버하는지 검증
### 헥사고날 아키텍쳐 다이어그램 도출 (Polyglot)

## 구현
분석/설계 단계에서 도출된 헥사고날 아키텍처에 따라, 각 BC별로 대변되는 마이크로 서비스들을 스프링부트와 파이선으로 구현하였다. 구현한 각 서비스를 로컬에서 실행하는 방법은 아래와 같다 (각자의 포트넘버는 8081 ~ 808n 이다)
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

### DDD 의 적용
각 서비스내에 도출된 핵심 Aggregate Root 객체를 Entity 로 선언하였다. (아래 예시는 order 마이크로 서비스)
이 서비스 조직의 특이성으로 현업에서도 영어를 사용하기에, 영어를 그대로 사용하려고 노력했다. (유비쿼터스 랭귀지)
일부 구현에 있어서 영문이 아닌 경우는 실행이 불가능한 경우가 있기 때문에, 향후에도 영문으로 유비쿼터스 랭귀지를 유지하고자 한다.
(Maven pom.xml, Kafka의 topic id, FeignClient 의 서비스 id 등은 한글로 식별자를 사용하는 경우 오류가 발생하는 것을 확인하였다)
```
order 마이크로 서비스 관련 코드 넣기
```
Entity Pattern 과 Repository Pattern 을 적용하여 JPA 를 통하여 다양한 데이터소스 유형 (RDB or NoSQL) 에 대한 별도의 처리가 없도록 데이터 접근 어댑터를 자동 생성하기 위하여 Spring Data REST 의 RestRepository 를 적용하였다
```
소스 넣기
```
적용 후 REST API의 테스트
```
테스트 결과 
```

### Gateway 적용
API Gateway를 통하여 마이크로 서비스들의 진입점을 통일하였다.
```
gateway > application.yaml 파일 소스
```


### 폴리그랏 퍼시스턴스
- recipe의 경우, 다른 마이크로 서비스들과 달리 조회 기능도 제공해야 하기에, HSQL을 사용하여 구현하였다. 이를 통해, 마이크로 서비스 간 서로 다른 종류의 데이터베이스를 사용해도 문제 없이 동작하여 폴리그랏 퍼시스턴스를 충족시켰다.
```
recipe에 h2sql을 넣어서 할 수 있도록 수정 필요
```

### 유비쿼터스 랭귀지
- 조직명, 서비스 명에서 사용되고, 업무현장에서도 쓰이며, 모든 이해관계자들이 직관적으로 의미를 이해할 수 있도록 영어 단어를 사용함 (recipe, order, delivery 등)

### 동기식 호출과 Fallback 처리
- 분석단계에서의 조건 중 하나로 주문 취소(order)와 배송 취소(delivery) 간의 호출은 동기식 일관성을 유지하는 트랜잭션으로 처리하기로 하였다. 호출 프로토콜은 이미 앞서 Rest Repository 에 의해 노출되어있는 REST 서비스를 FeignClient 를 이용하여 호출하도록 한다.
- 배송 취소 서비스를 호출하기 위하여 FeignClient를 이용하여 Service 대행 인터페이스(Proxy)를 구현
```
FeignClient 소스코드
```
- 주문이 취소된 직후(@PostPersist) 배송이 취소되도록 처리
```
Order 쪽 cancel의 PostPersist 관련 코드
```
- 동기식 출에서는 호출 시간에 따른 타임 커플링이 발생하여, 주문 취소 시스템에 장애가 나면 배송도 취소되지 않는 다는 것을 확인
```
# 배송(Delivery) 서비스를 잠시 내려놓음 (ctrl+c)
# 주문 취소(cancel) 요청
http http://????코드 넣어야함
```
- 에러 난 화면 표시
```
# 배송(Delivery) 서비스 재기동
cd delivery
mvn spring-boot:run
# 주문 취소(cancel) 요청
http http://????코드 넣어야함
```
- 주문 취소 된 화면 표시
- 또한 과도한 요청시에 서비스 장애가 도미노 처럼 벌어질 수 있다. (서킷브레이커, 폴백 처리는 운영단계에서 설명한다.)

### 비동기식 호출 / 시간적 디커플링 / 장애격리 / 최종 (Eventual) 일관성 테스트
### CQRS


## 운영
### CI/CD 설정
- root 폴더 하위에 폴더 만들기
```
cd ~
mkdir gitsource
cd gitsource
```
- git에서 소스 가져오기
```
git clone http://github.com/WonGil/searchrecipe
```
- Build 하기
```
cd ~/gitsource/searchrecipe
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
cd ~/gitsource/searchrecipe
cd recipe
az acr build --registry skcc04 --image skcc04.azurecr.io/recipe:v1 .

cd ..
cd order
az acr build --registry skcc04 --image skcc04.azurecr.io/order:v1 .

cd ..
cd delivery
az acr build --registry skcc04 --image skcc04.azurecr.io/delivery:v1 .

cd ..
cd gateway
az acr build --registry skcc04 --image skcc04.azurecr.io/gateway:v1 .

cd ..
cd mypage
az acr build --registry skcc04 --image skcc04.azurecr.io/mypage:v1 .
```
- ACR에서 이미지 가져와서 Kubernetes에서 Deploy하기
```
kubectl create deploy recipe --image=skcc04.azurecr.io/recipe:v1
kubectl create deploy recipe --image=skcc04.azurecr.io/order:v1
kubectl create deploy recipe --image=skcc04.azurecr.io/delivery:v1
kubectl create deploy recipe --image=skcc04.azurecr.io/gateway:v1
kubectl create deploy recipe --image=skcc04.azurecr.io/mypage:v1
kubectl get all
```
- Kubectl get all 결과 보여주기
- Kubernetes에서 서비스 생성하기 (Docker 생성이기에 Port는 8080이며, Gateway는 LoadBalancer로 생성)
```
kubectl expose deploy recipe --type="ClusterIP" --port=8080
kubectl expose deploy order --type="ClusterIP" --port=8080
kubectl expose deploy delivery --type="ClusterIP" --port=8080
kubectl expose deploy gateway --type="LoadBalancer" --port=8080
kubectl expose deploy mypage --type="ClusterIP" --port=8080
kubectl get all
```
- Kubectl get all 결과 보여주기
- 테스트를 위해서 Kafka zookeeper와 server도 별도로 실행 필요
- deployment.yaml 편집 후 배포 방안 적어두기

### 동기식 호출 / 서킷 브레이킹 / 장애격리
- istio 사용 (Destination Rule)
- Retry 적용
- Pool Ejection
### 오토스케일 아웃
- HPA 사용
- siege로 테스트
### 모니터링, 앨럿팅
- Kiali 활용
- Jager 활용
### 무정지 재배포
- Autoscaler, CB 설정 제거
- 테스트 후, Readiness Probe 설정 후 kubectl apply
### 운영 유연성 - Persistence Volume, Persistence Volume Claim 적용
- yaml 파일로 만들어서 붙이기
### Canary Deploy
- istio로 실행
### ConfigMap 적용
- 외부 주입 ConfigMap 적용
### Secret 적용
- secret 적용





