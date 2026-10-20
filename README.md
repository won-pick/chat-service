# Won-pick Chat Service

상품별 구매자·판매자 대화를 관리하는 독립 Spring Boot 4 REST API 서버입니다. 채팅 메시지 전송과 조회는 각각 HTTP API로 제공하며, 현재 React 화면은 메시지 목록을 3초마다 다시 조회합니다. WebSocket은 사용하지 않습니다.

## 팀 구성과 호출 흐름

```text
React (5173) ──HTTP──> Market (8082, market_db)
     ├───────────────> Trade  (8083, trade_db) ──HTTP──> Payment (8084, 계획)
     └───────────────> Chat   (8080, chat_db)
```

React가 상품과 거래 정보로 채팅방 생성을 요청하고, Chat의 API로 메시지를 주고받습니다. 각 서버는 별도 애플리케이션과 DB를 사용합니다. 현재 Chat 서버의 Market 상품·판매자 확인 및 Trade 거래 생성 호출은 **구현되지 않았습니다**. 위 도식의 Payment는 팀 통합 예정 부분입니다. 서버 간 호출이 필요한 핵심 거래 흐름은 Trade 담당 서버의 계약을 함께 확인해야 합니다.

## 실행, 저장소, Swagger

JDK 21과 포함된 Gradle Wrapper를 사용합니다.

```sh
# 빠른 로컬 실습: H2 인메모리 DB + JDBC (재시작 시 초기화)
./gradlew bootRun

# 가이드 기준 MySQL 실습
mysql -u root -p -e 'CREATE DATABASE IF NOT EXISTS chat_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;'
CHAT_DB_URL='jdbc:mysql://localhost:3306/chat_db?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Seoul' \
CHAT_DB_USER=root CHAT_DB_PASSWORD='your-password' \
./gradlew bootRun --args='--spring.profiles.active=mysql'

./gradlew test bootJar
```

Windows PowerShell에서는 `./gradlew` 대신 `.\gradlew.bat`을 사용하고 환경 변수는 `$env:CHAT_DB_USER='root'` 형태로 설정합니다. `schema.sql`이 서버 시작 시 Chat 테이블을 생성합니다. MySQL에서는 메시지와 읽음 위치, 멱등 요청 기록이 재시작 후에도 유지됩니다. 기존 메모리 저장 구현을 확인하려면 `--spring.profiles.active=memory`를 사용할 수 있습니다.

- 기본 주소: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Chat DB 스키마: `chat_rooms`, `chat_messages`, `chat_read_positions`, `chat_message_requests` (`src/main/resources/schema.sql`)
- 팀 서버별 포트/DB: Market `8082`/`market_db`, Trade `8083`/`trade_db`, Chat `8080`/`chat_db`, Payment `8084`/팀 확정 필요

## Chat API

| 메서드 | 경로 | 기능 |
| --- | --- | --- |
| POST | `/chat/rooms` | 방 생성: 신규 201, 기존 200 |
| GET | `/chat/rooms?memberId=1` | 내 방 목록, 마지막 메시지, 안 읽은 수 |
| GET | `/chat/rooms/{roomId}?memberId=1` | 방 상세 |
| POST | `/chat/rooms/{roomId}/messages` | 메시지 전송 |
| GET | `/chat/rooms/{roomId}/messages?memberId=1` | 메시지 조회 |
| GET | `/chat/rooms/{roomId}/messages/{messageId}?memberId=1` | 메시지 단건 |
| PATCH | `/chat/rooms/{roomId}/read-position` | 읽음 위치 갱신 |
| GET | `/chat/rooms/{roomId}/read-position?memberId=1` | 내 읽음 위치 |
| GET | `/chat/rooms/{roomId}/read-position/peer?memberId=1` | 상대 읽음 위치 |

메시지 조회는 `limit`(기본 50, 최대 100), `afterId` 또는 `beforeId`를 지원합니다. 두 커서는 함께 사용할 수 없습니다. 전송에는 `Idempotency-Key` 헤더가 필수입니다. 동일 방·발신자·키·내용의 재요청은 기존 메시지를 반환하고, 같은 키로 내용만 바꾸면 409를 반환합니다.

### 핵심 흐름 예시

1. `POST /chat/rooms`에 `{"productId":10,"buyerId":1,"sellerId":2}`를 보내고 반환된 `id`를 저장합니다.
2. 구매자가 `POST /chat/rooms/{id}/messages`에 `Idempotency-Key: message-001`, `{"senderId":1,"content":"아직 판매 중인가요?"}`를 보냅니다.
3. 판매자가 `GET /chat/rooms/{id}/messages?memberId=2`로 대화를 조회합니다.
4. 판매자가 답장을 보낸 뒤 `GET /chat/rooms?memberId=1`에서 `unreadCount`를 확인합니다.
5. 구매자가 마지막 메시지 ID로 `PATCH /chat/rooms/{id}/read-position`에 `{"memberId":1,"lastReadMessageId":2}`를 보내고 안 읽은 수가 0이 되는지 확인합니다.

Apidog에서 [요청 모음](docs/chat-api.postman_collection.json)을 가져와 위 흐름을 실행할 수 있습니다. 전체 팀의 Market→Trade→Payment 연동 시나리오는 각 서버 API 계약과 실행 환경을 합친 뒤 Apidog 프로젝트에 별도로 구성해야 합니다.

## 상태 변화와 HATEOAS

| 자원 | 시작 | 동작 | 결과 |
| --- | --- | --- | --- |
| 채팅방 | 없음 | 동일 상품·구매자·판매자로 방 생성 | 방 1개 생성; 재요청은 기존 방 반환 |
| 메시지 | 없음 | 참여자가 전송 | 방에 메시지 추가; 동일 멱등 키 재요청은 같은 메시지 |
| 읽음 위치 | 0 또는 이전 ID | 참여자가 해당 방 메시지 ID로 갱신 | 더 큰 ID로만 이동 |

Chat에는 예약·결제처럼 취소/확정 버튼을 결정하는 상태 전이가 없습니다. 이번 팀 실습의 HATEOAS는 다른 기능에서 적용하고, Chat 응답에는 `_links`를 추가하지 않습니다. 거래의 결제 요청·완료·취소 상태 전이표와 동작 링크는 Trade 담당 문서에서 관리합니다.

## 범위와 통합 전 확인할 점

채팅방 중복 방지, 참여자 검사, 자기 채팅 금지, 입력 검증, ProblemDetail 오류 응답, 메시지 멱등 처리, 읽음 위치 역행 방지를 구현했습니다. 테스트는 HTTP 요청으로 정상·오류·접근 제한·읽음·안 읽은 수·페이지 조회·동시 요청을 검증합니다. JDBC 구현의 동일 방 생성과 멱등 처리는 현재 단일 서버 인스턴스 기준으로 직렬화합니다. 여러 Chat 인스턴스를 운영한다면 DB 고유 키 충돌의 재조회·재시도 처리가 추가로 필요합니다.

가이드에 따라 이번 실습에는 Spring Security/로그인 인증을 넣지 않습니다. `memberId`·`senderId`는 요청자가 전달하며 참여 여부만 검사합니다. 실제 서비스에서 이 값을 신뢰하면 다른 사용자를 사칭할 수 있으므로 실서비스 배포 전에 인증된 사용자와 연결해야 합니다.

Market 상품·판매자 확인, Trade 결제 요청, Payment 상태 동기화는 아직 연동되지 않았습니다. 팀 API 계약이 확정되면 [외부 연동 합의 항목](docs/INTEGRATION-CONTRACT.md)에 맞춰 구현해야 합니다. 가이드의 공통 요구사항은 별도 `step-00`~`step-07` 원문이 제공되지 않아 이 문서만으로 모두 검증할 수 없습니다.
