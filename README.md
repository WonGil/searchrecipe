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
### EventStorming 결과
<img src="./images/2021-01-31 155439.png" />

### 헥사고날 아키텍쳐

## 구현
### DDD 의 구현
### Gateway 적용
### Gateway 인증
### 폴리그랏 퍼시스턴스
- h2db, hsqldb
### 유비쿼터스 랭귀지
- 조직명, 서비스 명에서 사용되고, 업무현장에서도 쓰이며, 모든 이해관계자들이 직관적으로 의미를 이해할 수 있도록 영어 단어를 사용함 (recipe, order, delivery 등)
### 동기식 호출과 Fallback 처리
### 비동기식 호출 / 시간적 디커플링 / 장애격리 / 최종 (Eventual) 일관성 테스트
### CQRS


## 운영
### CI/CD 설정
- git clone 실행
- mvn package 실행
- az acr build 실행
- kubectl create deploy
- kubectl expose
- kubectl get all
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





