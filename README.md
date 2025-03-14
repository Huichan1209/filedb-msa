[프로젝트 설명]

Java, Spring Boot, File I/O, MSA, 트랜잭션에 관한 내용을 주로 타겟팅해서 7일 동안 개발한 샘플 프로젝트이며, **자체 개발 File DB**를 사용한 것이 가장 큰 특징임.

상품 도메인과 주문 도메인이 존재하며, 사용자가 주문을 생성하면 상품의 재고가 차감되고 주문을 취소하면 재고가 복구됨. 구체적인 로직은 하단에 첨부된 링크 중 **개발 히스토리**에서 자세히 설명함.

모든 마이크로서비스는 독립적인 Java & Spring Boot 기반의 Application으로 구현했으며 목록은 다음과 같음.

- 상품(Product)서비스
- 주문(Order)서비스
- Spring Cloud Gateway(SCG)
- Spring Cloud Eureka Server
- Spring Cloud Eureka Client
- Spring Cloud Config Server

[제약 조건]

- RDBMS를 사용하지 않고 CRUD, 트랜잭션 처리를 지원하는 **자체 개발 File DB를 사용함**.
- 특정 마이크로서비스에 장애가 발생해도 다른 서비스는 독립적으로 동작할 수 있어야함.
- Gateway를 제외한 서비스로는 사용자의 직접적인 요청이 모두 차단된다고 가정하고 filter를 설정해 각 도메인으로 요청을 전달함.
- Spring Cloud Eureka를 사용해 로드벨런싱을 적용함.
- 가독성과 유지보수를 위해 서비스 계층의 로직은 Spring Data JPA를 사용하는 것과 똑같이 보이도록 모방하여 구현함. 
- 주문(Order) 생성시 상품(Product) 재고의 차감 로직에 동시성 이슈가 발생하지 않아야 함.
- SAGA 패턴과 kafka를 사용함.

[실행화면]
![image](https://github.com/user-attachments/assets/9e68ed59-343c-46fc-bf04-c6b3d5447836)
![image](https://github.com/user-attachments/assets/7a50ef68-6344-4edf-8edc-74b327c1ea6a)

[버전]

- jdk 17
- Spring 2.7.15
- Spring cloud 2021.0.8
- kafka lastest(docker)

[링크]

Config Repository (application.yml의 값은 전부 여기 존재하고, 각 서비스에 Spring Cloud Config 의존성을 추가해서 서비스 기동시 이 설정값을 받아서 사용하도록 함)
https://github.com/Huichan1209/filedb-msa-config

개발 히스토리
https://nine-week-6e6.notion.site/1916cfeb5e1d808f80dbccc3d572cafe

MSA 인프라 서비스
https://nine-week-6e6.notion.site/Spring-Cloud-MSA-1846cfeb5e1d8028a3b2d3ae2ab57802
