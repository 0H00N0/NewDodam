# 🛍️ Dodam E-Commerce Platform - 프로젝트 보고서

## 📋 목차
1. [프로젝트 개요](#프로젝트-개요)
2. [기술 스택](#기술-스택)
3. [프로젝트 구조](#프로젝트-구조)
4. [도메인별 상세 설명](#도메인별-상세-설명)
5. [데이터베이스 설계](#데이터베이스-설계)
6. [API 엔드포인트](#api-엔드포인트)
7. [예외 처리 전략](#예외-처리-전략)
8. [설치 및 실행 방법](#설치-및-실행-방법)
9. [향후 개발 계획](#향후-개발-계획)

---

## 프로젝트 개요

### 프로젝트명: Dodam (도담)
- **목적**: 상품 관리, 리뷰, 기프티콘, 이벤트 리워드를 통합 관리하는 이커머스 플랫폼
- **개발 기간**: 2024년 8월 - 9월
- **개발 인원**: 1명 (개인 프로젝트)
- **주요 특징**: 
  - 계층형 아키텍처 기반의 Spring Boot 애플리케이션
  - RESTful API 설계
  - 도메인 중심 개발 (DDD 일부 적용)

---

## 기술 스택

### Backend Framework
| 기술 | 버전 | 용도 |
|------|------|------|
| **Spring Boot** | 3.5.4 | 메인 프레임워크 |
| **Java** | 21 | 프로그래밍 언어 |
| **Spring Data JPA** | - | ORM 및 데이터 접근 |
| **Spring Security** | - | 보안 및 인증 |
| **Spring Validation** | - | 데이터 유효성 검증 |

### Database
| 기술 | 용도 |
|------|------|
| **H2 Database** | 개발/테스트용 인메모리 DB |
| **JPA/Hibernate** | 객체-관계 매핑 |

### Development Tools
| 도구 | 용도 |
|------|------|
| **Lombok** | 보일러플레이트 코드 감소 |
| **Spring DevTools** | 개발 생산성 향상 |
| **Gradle** | 빌드 및 의존성 관리 |

---

## 프로젝트 구조

```
Dodam/
├── src/
│   ├── main/
│   │   ├── java/com/dodam/
│   │   │   ├── DodamApplication.java      # 메인 진입점
│   │   │   ├── admin/                     # 관리자 모듈
│   │   │   ├── board/                     # 게시판 모듈
│   │   │   ├── config/                    # 설정 클래스
│   │   │   ├── member/                    # 회원 관리 모듈
│   │   │   └── product/                   # 상품 관리 모듈 (핵심)
│   │   │       ├── dto/                   # 데이터 전송 객체
│   │   │       │   ├── request/           # 요청 DTO
│   │   │       │   ├── response/          # 응답 DTO
│   │   │       │   └── statistics/        # 통계 DTO
│   │   │       ├── entity/                # JPA 엔티티
│   │   │       ├── exception/             # 예외 클래스
│   │   │       ├── repository/            # 데이터 접근 계층
│   │   │       └── service/               # 비즈니스 로직
│   │   └── resources/
│   │       └── application.properties     # 애플리케이션 설정
│   └── test/
│       └── resources/
│           └── application-test.properties # 테스트 설정
└── build.gradle                            # 빌드 설정
```

### 패키지 구조 설명
- **계층형 아키텍처**: Controller → Service → Repository → Entity
- **도메인별 패키지 분리**: 각 도메인이 독립적으로 관리됨
- **DTO 패턴**: 요청/응답/통계 DTO로 세분화하여 관리

---

## 도메인별 상세 설명

### 1. Product 도메인 (핵심 모듈)

#### 1.1 엔티티 구조
```java
Product          # 상품 정보
├── id           # 상품 ID
├── name         # 상품명
├── price        # 가격
├── stock        # 재고
├── description  # 설명
├── category     # 카테고리 (Many-to-One)
└── reviews      # 리뷰 목록 (One-to-Many)

Category         # 카테고리
├── id           # 카테고리 ID
├── name         # 카테고리명
└── products     # 상품 목록

Review           # 리뷰
├── id           # 리뷰 ID
├── content      # 내용
├── rating       # 평점
├── product      # 상품 (Many-to-One)
└── likes        # 좋아요 목록

ReviewLike       # 리뷰 좋아요
├── id           # 좋아요 ID
├── review       # 리뷰 (Many-to-One)
└── userId       # 사용자 ID

Gifticon         # 기프티콘
├── id           # 기프티콘 ID
├── code         # 코드
├── product      # 상품
└── expiryDate   # 만료일

EventReward      # 이벤트 리워드
├── id           # 리워드 ID
├── name         # 이벤트명
├── reward       # 보상 내용
└── conditions   # 조건
```

#### 1.2 주요 기능
- **상품 관리**: CRUD 작업, 재고 관리, 가격 정책
- **카테고리 관리**: 상품 분류 체계
- **리뷰 시스템**: 상품 리뷰 작성, 평점, 좋아요
- **기프티콘**: 디지털 상품권 관리
- **이벤트 리워드**: 프로모션 및 보상 시스템
- **통계**: 각 도메인별 통계 데이터 제공

#### 1.3 서비스 계층
| 서비스 | 책임 |
|--------|------|
| ProductService | 상품 CRUD, 재고 관리 |
| CategoryService | 카테고리 관리 |
| ReviewService | 리뷰 작성 및 관리 |
| ReviewLikeService | 리뷰 좋아요 처리 |
| GifticonService | 기프티콘 발급 및 검증 |
| EventRewardService | 이벤트 및 리워드 관리 |

### 2. Member 도메인
- **기능**: 회원 가입, 로그인, 정보 관리
- **구성**: Controller, Service, Repository, Entity, DTO
- **상태**: 기본 구조 구현 완료

### 3. Board 도메인
- **기능**: 게시글 CRUD, 조회
- **구성**: Controller, Service, Repository, Entity, DTO
- **상태**: 기본 구조 구현 완료

### 4. Admin 도메인
- **기능**: 관리자 기능 (개발 예정)
- **상태**: 스켈레톤 코드만 존재

---

## 데이터베이스 설계

### 데이터베이스 설정
```properties
# H2 Database 설정
spring.datasource.url=jdbc:h2:tcp://localhost/~/dodam
spring.datasource.username=sa
spring.datasource.password=

# JPA 설정
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

### 주요 테이블 관계
```
Product (1) ─── (*) Review
   │                  │
   │                  └─── (*) ReviewLike
   │
   └─── (*) Category
   │
   └─── (1) Gifticon
   │
   └─── (*) EventReward
```

---

## API 엔드포인트

### Product API
| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | /api/products | 상품 목록 조회 |
| GET | /api/products/{id} | 상품 상세 조회 |
| POST | /api/products | 상품 등록 |
| PUT | /api/products/{id} | 상품 수정 |
| DELETE | /api/products/{id} | 상품 삭제 |
| GET | /api/products/statistics | 상품 통계 조회 |

### Category API
| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | /api/categories | 카테고리 목록 |
| POST | /api/categories | 카테고리 생성 |
| PUT | /api/categories/{id} | 카테고리 수정 |
| DELETE | /api/categories/{id} | 카테고리 삭제 |

### Review API
| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | /api/reviews | 리뷰 목록 |
| POST | /api/reviews | 리뷰 작성 |
| POST | /api/reviews/{id}/like | 리뷰 좋아요 |
| DELETE | /api/reviews/{id}/like | 좋아요 취소 |

---

## 예외 처리 전략

### 예외 계층 구조
```
BaseException (추상 클래스)
├── BusinessException
│   ├── DuplicateResourceException  # 중복 리소스
│   ├── ResourceNotFoundException   # 리소스 없음
│   └── InsufficientStockException  # 재고 부족
├── ValidationException              # 유효성 검증 실패
├── UnauthorizedException            # 인증/인가 실패
└── ExpiredException                 # 만료 (기프티콘 등)
```

### 예외 처리 원칙
1. **명확한 에러 메시지**: 사용자가 이해할 수 있는 메시지 제공
2. **적절한 HTTP 상태 코드**: RESTful 규약 준수
3. **로깅**: 모든 예외 상황 로깅
4. **복구 가능성**: 가능한 경우 대안 제시

---

## 설치 및 실행 방법

### 필수 요구사항
- Java 21 이상
- Gradle 8.x
- H2 Database

### 설치 단계

1. **저장소 클론**
```bash
git clone https://github.com/young0508/claude.git
cd Dodam
```

2. **의존성 설치**
```bash
./gradlew clean build
```

3. **H2 데이터베이스 설정**
```bash
# H2 Console 실행
# URL: jdbc:h2:tcp://localhost/~/dodam
# 사용자명: sa
# 비밀번호: (빈 값)
```

4. **애플리케이션 실행**
```bash
./gradlew bootRun
```

5. **접속 확인**
```
http://localhost:8080
```

### 개발 환경 설정
```bash
# IDE: IntelliJ IDEA 또는 VS Code
# VS Code 확장: Java Extension Pack
# Lombok 플러그인 설치 필수
```

---

## 향후 개발 계획

### Phase 1: 기능 완성 (1개월)
- [ ] Controller 계층 구현
- [ ] REST API 완성
- [ ] 통합 테스트 작성
- [ ] API 문서화 (Swagger)

### Phase 2: 기능 고도화 (2개월)
- [ ] 검색 기능 강화 (Elasticsearch)
- [ ] 캐싱 적용 (Redis)
- [ ] 페이징 및 정렬 기능
- [ ] 이미지 업로드 기능

### Phase 3: 운영 준비 (1개월)
- [ ] MySQL 마이그레이션
- [ ] Docker 컨테이너화
- [ ] CI/CD 파이프라인 구축
- [ ] 모니터링 시스템 구축

### Phase 4: 추가 기능 (지속적)
- [ ] 결제 시스템 통합
- [ ] 추천 시스템
- [ ] 실시간 알림
- [ ] 다국어 지원

---

## 프로젝트 특징 및 장점

### 1. 확장 가능한 구조
- 도메인별 독립적인 패키지 구조
- 인터페이스 기반 설계로 구현체 교체 용이
- 명확한 계층 분리

### 2. 유지보수성
- 일관된 코딩 컨벤션
- 명확한 네이밍 규칙
- 체계적인 예외 처리

### 3. 성능 최적화 준비
- JPA 지연 로딩 전략
- DTO를 통한 필요 데이터만 전송
- 향후 캐싱 레이어 추가 가능

### 4. 보안
- Spring Security 적용
- 입력 데이터 검증
- 예외 정보 최소 노출

---

## 문의 및 기여

- **GitHub**: [https://github.com/young0508/claude](https://github.com/young0508/claude)
- **이메일**: dev@dodam.com
- **라이선스**: MIT License

---

*이 문서는 2024년 9월 2일 기준으로 작성되었습니다.*