# AI Shopping Helper (ASH) — 전체 시스템 아키텍처 & 데이터 흐름

**K-Digital Training · 한국공학대학교 IoT 스마트융합 프로젝트 · 1팀**

---

## 1. 전체 시스템 아키텍처

```
┌─────────────────────────────────────────────────────────────────────────┐
│ 하드웨어 레이어                                                           │
│                                                                         │
│  ┌──────────────────────────┐   ┌──────────────────┐  ┌─────────────┐  │
│  │   스마트 쇼핑 카트          │   │   MQTT 브로커      │  │  카트 QR코드  │  │
│  │   (Raspberry Pi 5)       │──▶│                  │  │            │  │
│  │  · 전면 카메라: 바코드 스캔  │   │ smartcart/ 토픽   │  │ 앱-카트 페어링 │  │
│  │  · 하부 카메라: 구역 감지   │   │ 192.168.0.12    │  │ 로그인 시 스캔  │  │
│  └──────────────────────────┘   └────────┬─────────┘  └──────┬──────┘  │
└─────────────────────────────────────────│───────────────────│──────────┘
                                          │ MQTT              │ QR 스캔
┌─────────────────────────────────────────│───────────────────│──────────┐
│ 클라이언트 레이어                          │                   │          │
│                                         ▼                   ▼          │
│  ┌───────────────────────────┐   ┌──────────────────────────────────┐  │
│  │     고객 앱 (ASH)           │   │       관리자 앱 (AdminView)        │  │
│  │                           │   │                                  │  │
│  │ · 로그인: 성별/생년월일 입력  │   │ · 혼잡도 (구역별 체류 현황)         │  │
│  │ · 카트 QR 스캔으로 페어링    │   │ · 매출 데이터                     │  │
│  │ · 홈: 상품목록 / 검색       │   │ · 전환율 (방문 대비 구매)           │  │
│  │ · 장바구니: 스캔상품 자동추가 │   │ · 종합 리포트                     │  │
│  │ · AI 추천 Top-3 표시       │   │ · Grafana WebView 임베드          │  │
│  │ · 결제 처리                │   │                                  │  │
│  └─────────────┬─────────────┘   └────────────────┬─────────────────┘  │
└────────────────│────────────────────────────────────│───────────────────┘
                 │ REST API                           │ WebView
┌────────────────│────────────────────────────────────│───────────────────┐
│ 서버 레이어     │                                    │                   │
│                ▼                                    ▼                   │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                  Flask 백엔드 (main_server.py)                   │   │
│  │                                                                 │   │
│  │  ┌─────────────────┐ ┌─────────────────┐ ┌──────────────────┐  │   │
│  │  │  OrderService   │ │   DBHandler     │ │   Recommender    │  │   │
│  │  │                 │ │                 │ │                  │  │   │
│  │  │ POST /api/scan  │ │ GET /products   │ │ POST /recommend  │  │   │
│  │  │ POST /checkout  │ │ GET /product/id │ │ Apriori 분석     │  │   │
│  │  │ 재고 차감       │ │ 상품 정보 조회   │ │ LightGBM 순위    │  │   │
│  │  │ 관리자 알림      │ │                 │ │ Top-3 반환       │  │   │
│  │  └────────┬────────┘ └────────┬────────┘ └────────┬─────────┘  │   │
│  └───────────│──────────────────│──────────────────│─────────────┘   │
└──────────────│──────────────────│──────────────────│─────────────────┘
               │ SQL              │ SQL              │ SQL
┌──────────────│──────────────────│──────────────────│─────────────────┐
│ 데이터 레이어  ▼                  ▼                  ▼                  │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐   │
│  │  Product 테이블   │  │  Purchase 테이블  │  │ Cart_Zone_Dwell  │   │
│  │                  │  │                  │  │     테이블        │   │
│  │ product_id (PK)  │  │ customerid       │  │ customerid       │   │
│  │ product_name     │  │ gender           │  │ cartid           │   │
│  │ price            │  │ age_group        │  │ zone_id          │   │
│  │ quantity         │  │ cartid           │  │ entry_time       │   │
│  │                  │  │ product_id (FK)  │  │ exit_time        │   │
│  │                  │  │ quantity         │  │                  │   │
│  │                  │  │ purchase_time    │  │                  │   │
│  └──────────────────┘  └────────┬─────────┘  └────────┬─────────┘   │
└──────────────────────────────────│──────────────────────│─────────────┘
                                   │                      │ 데이터 시각화
                                   └──────────┬───────────┘
                                              ▼
                                   ┌──────────────────────┐
                                   │   Grafana 대시보드     │
                                   │  192.168.0.8:3000    │
                                   │ · 혼잡도 히트맵        │
                                   │ · 매출 차트            │
                                   │ · 전환율 분석          │
                                   └──────────────────────┘
```

---

## 2. 컴포넌트 상세

| 컴포넌트 | 주요 역할 | 핵심 기술 |
|---|---|---|
| 스마트 쇼핑 카트 (Raspberry Pi 5) | 전면 카메라로 상품 바코드/QR 스캔, 하부 카메라로 바닥 컬러 테이프 인식, 구역(Blue/Red/Yellow) 체류 시간 측정 | Picamera2, pyzbar, OpenCV HSV+Lab, paho-mqtt |
| 고객 앱 (ASH) | 카트 QR 스캔으로 로그인 및 카트 페어링, MQTT로 스캔된 상품 실시간 수신, AI 추천 Top-3 표시 및 결제 처리 | Android Java, ML Kit, CameraX, Retrofit2, Eclipse Paho |
| 관리자 앱 (AdminView) | 혼잡도·매출·전환율·리포트 모니터링, Grafana 대시보드를 WebView로 표시, 재고 부족 시 HTTP 알림 수신 | Android Java, WebView |
| Flask 백엔드 서버 | OrderService(스캔·결제·재고), DBHandler(상품 조회), Recommender(AI 추천) | Python, Flask, mariadb, mlxtend, LightGBM |
| MariaDB (smart_cart_db) | Product(상품/재고), Purchase(구매이력·AI학습데이터), Cart_Zone_Dwell(구역 체류 기록) | MariaDB, AWS RDS |
| Grafana | 구역별 혼잡도 히트맵, 매출 및 전환율 차트, 실시간 대시보드 | Grafana OSS, MariaDB 데이터소스 |

---

## 3. REST API 엔드포인트

| 메서드 | 엔드포인트 | 담당 모듈 | 설명 |
|---|---|---|---|
| POST | `/api/scan` | OrderService | 바코드 스캔 → 상품 정보 조회 |
| GET | `/products` | DBHandler | 전체 상품 목록 반환 |
| POST | `/recommend` | Recommender | 장바구니 + 성별 + 나이 → Top-3 추천 |
| POST | `/order/checkout` | OrderService | 결제 처리 + 재고 차감 + 관리자 알림 |

---

## 4. 데이터 흐름

### ① 로그인 & 카트 페어링

```
앱 실행
  → 성별 / 생년월일 입력
  → 카트에 부착된 QR코드 스캔
  → Cart ID 앱에 저장 (SharedPreferences)
  → MQTT 브로커 연결 수립
  → topic 구독 시작: smartcart/cart/{cart_id}/scan
```

### ② 상품 스캔 & 장바구니 자동 추가

```
카트 전면 카메라 (Raspberry Pi, scan_ver3.py)
  → pyzbar로 바코드/QR 인식
  → MQTT publish: smartcart/cart/{CART_NO}/scan
     payload: {"barcode": "101", "qty": 1}
  → 앱 MqttCartClient 수신
  → Flask POST /api/scan 호출
  → Product 테이블에서 상품 정보 반환
  → 장바구니 화면에 상품 자동 추가 및 금액 갱신
```

### ③ 구역 감지 & 체류 시간 기록

```
카트 하부 카메라 (zone_detector.py)
  → OpenCV HSV / Lab 색상 분석 (CLAHE 전처리)
  → 구역 판별:
       Zone 1 (Blue)   — HSV H: 0~10, 170~180
       Zone 2 (Red)    — HSV H: 90~140
       Zone 3 (Yellow) — HSV H: 48~95 + Lab a 보정
  → 구역 진입 확정 (STABLE_FRAMES = 2 이상 연속)
  → 구역 변경 시: Cart_Zone_Dwell INSERT (entry_time, exit_time)
  → MQTT publish: smartcart/{CART_ID}/zone
  → Grafana에서 혼잡도 / 전환율 시각화
```

### ④ AI 상품 추천

```
앱 장바구니 화면 → POST /recommend
  Request: {"cart": [101, 102], "gender": "M", "age": 23}

Recommender 처리:
  1. Purchase 테이블에서 구매 이력 로드
  2. 성별/연령대 토큰을 트랜잭션에 포함
     예: {101, 102, "gender_M", "20s"}
  3. Apriori로 빈발 항목 집합 추출 (min_support=0.001)
  4. 연관 규칙 생성 (metric="lift", min_threshold=0.5)
  5. 장바구니 상품별 후보 추출 → confidence × lift 점수 합산
  6. LightGBM / LogisticRegression으로 최종 순위 결정
     (정확도 비교 후 자동 선택)
  7. Top-3 상품 ID 반환
  8. (데이터 부족 시 인기 상품 순으로 fallback)

  Response: [103, 205, 108]  →  앱 화면에 추천 상품 표시
```

### ⑤ 결제 처리

```
앱 결제 버튼
  → POST /order/checkout
     Request: {
       customer: {id, gender, birth_year},
       items: [{barcode, qty}],
       cart_id: 3
     }

OrderService 처리:
  1. 고객 정보 파싱 (나이 계산, 연령대 변환)
  2. Purchase 테이블 INSERT (구매 이력 → 추천 모델 학습 데이터)
  3. Product 테이블 재고 차감
  4. 재고 0 도달 시 → 관리자 앱에 HTTP POST 알림 전송
  5. 결제 완료 응답 반환
```

### ⑥ 관리자 모니터링

```
Cart_Zone_Dwell + Purchase 데이터
  → MariaDB → Grafana 대시보드 (192.168.0.8:3000)

관리자 앱 화면 구성:
  · 혼잡도 (CongestionActivity)  — panel 2: 구역별 방문 히트맵
  · 매출    (SalesActivity)       — panel 1, 4: 상품별/시간별 매출
  · 리포트  (ReportActivity)      — panel 8: 종합 리포트
  · 전환율  (ConversionActivity)  — panel 5, 6, 7: 구역 방문 대비 구매 전환율

각 화면은 Grafana panel URL을 WebView로 임베드하여 실시간 갱신
```

---

## 5. 데이터베이스 스키마 (smart_cart_db)

### Product

| 컬럼명 | 타입 | 설명 |
|---|---|---|
| product_id | INT (PK) | 상품 고유 번호 (= 바코드 번호) |
| product_name | VARCHAR | 상품 이름 |
| price | DECIMAL | 상품 가격 |
| quantity | INT | 재고 수량 (결제 시 차감) |

### Purchase

| 컬럼명 | 타입 | 설명 |
|---|---|---|
| customerid | VARCHAR | 고객 ID |
| gender | VARCHAR | 성별 (남성 / 여성) |
| age_group | VARCHAR | 연령대 (10대 / 20대 / …) |
| cartid | INT | 카트 번호 |
| product_id | INT (FK) | 구매 상품 ID |
| quantity | INT | 구매 수량 |
| purchase_time | DATETIME | 결제 시각 |

### Cart_Zone_Dwell

| 컬럼명 | 타입 | 설명 |
|---|---|---|
| customerid | VARCHAR | 고객 ID |
| cartid | INT | 카트 번호 |
| zone_id | INT | 구역 번호 (1: Blue / 2: Red / 3: Yellow) |
| entry_time | DATETIME | 구역 진입 시각 |
| exit_time | DATETIME | 구역 이탈 시각 |

---

## 6. 기술 스택 요약

| 구분 | 기술/도구 | 용도 |
|---|---|---|
| 하드웨어 | Raspberry Pi 5 | 카트 메인 컴퓨터 (카메라 2대 연결) |
| | Picamera2 + pyzbar | 전면 카메라: QR/바코드 인식 |
| | OpenCV (HSV/Lab) + CLAHE | 하부 카메라: 컬러 테이프 구역 감지 |
| 통신 | MQTT (Eclipse Paho) | 카트-서버-앱 실시간 메시지 전송 |
| | Retrofit2 (Android) | 앱 ↔ 서버 REST API 통신 |
| 서버 | Python / Flask | 백엔드 API 서버 |
| | Flask-CORS | 크로스 오리진 요청 허용 |
| AI/ML | mlxtend Apriori | 구매 이력 연관 규칙 분석 |
| | LightGBM / LogisticRegression | 최종 추천 순위 모델 (정확도 비교 후 선택) |
| | Google ML Kit | Android 앱 내 바코드/QR 스캔 |
| 데이터베이스 | MariaDB (smart_cart_db) | Product / Purchase / Cart_Zone_Dwell 테이블 |
| | AWS RDS | 클라우드 DB 연동 |
| 모니터링 | Grafana | 혼잡도 / 매출 / 전환율 대시보드 |
| Android 앱 | Android Studio (Java) | 고객 앱(ASH) + 관리자 앱(AdminView) |
| | CameraX | 카메라 미리보기 및 이미지 분석 |
| | Navigation Component | Bottom Navigation 화면 전환 |

---

## 7. MQTT 토픽 구조

| 토픽 | 발행자 | 구독자 | 페이로드 예시 |
|---|---|---|---|
| `smartcart/cart/{CART_NO}/scan` | Raspberry Pi (전면 카메라) | 고객 앱 (MqttCartClient) | `{"barcode":"101","qty":1}` |
| `smartcart/cart/set` | 고객 앱 (로그인 시) | Raspberry Pi | `{"cart_number":34}` |
| `smartcart/pi/{PI_ID}/set_cart` | 고객 앱 | Raspberry Pi | `{"cart_id":"CART_0034"}` |
| `smartcart/pi/{PI_ID}/state` | Raspberry Pi | 모니터링 | `{"cart_number":1}` |
| `smartcart/{CART_ID}/zone` | zone_detector.py | Grafana / 서버 | `{"zone_id":2,"entry_time":"...","exit_time":"..."}` |
| `smartcart/{CART_ID}/control` | 서버/앱 | zone_detector.py | `{"cmd":"start","customer_id":"USER01"}` |
