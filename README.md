# Won-pick Chat Service

구매자와 판매자의 상품 관련 대화를 관리하는 Spring Boot REST API 서버입니다. 백엔드만 포함합니다.

## 실행 및 테스트

JDK 21이 필요하며, 포함된 Gradle Wrapper를 사용합니다.

```sh
./gradlew bootRun
# 테스트 및 JAR 생성
./gradlew test bootJar
java -jar build/libs/chat-server-0.0.1-SNAPSHOT.jar
```

기본 주소: `http://localhost:8080`

## API

| 메서드 | 경로 | 기능 |
| --- | --- | --- |
| POST | `/chat/rooms` | 방 생성: 신규 201, 기존 200 |
| GET | `/chat/rooms?memberId=1` | 내 방 목록, 마지막 메시지, 안 읽은 수 |
| GET | `/chat/rooms/{roomId}?memberId=1` | 방 상세 |
| POST | `/chat/rooms/{roomId}/messages` | 메시지 전송 |
| GET | `/chat/rooms/{roomId}/messages?memberId=1` | 메시지 목록 |
| GET | `/chat/rooms/{roomId}/messages/{messageId}?memberId=1` | 메시지 단건 |
| PATCH | `/chat/rooms/{roomId}/read-position` | 읽음 위치 갱신 |
| GET | `/chat/rooms/{roomId}/read-position?memberId=1` | 내 읽음 위치 |
| GET | `/chat/rooms/{roomId}/read-position/peer?memberId=1` | 상대 읽음 위치 |

메시지 조회는 `limit`(기본 50, 최대 100), `afterId` 또는 `beforeId`를 지원합니다. 두 커서는 함께 사용할 수 없습니다.

### 채팅방 생성

```sh
curl -i -X POST http://localhost:8080/chat/rooms \
  -H 'Content-Type: application/json' \
  -d '{"productId":10,"buyerId":1,"sellerId":2}'
```

### 메시지 전송

반환된 방 번호를 URL에 사용합니다.

```sh
curl -i -X POST http://localhost:8080/chat/rooms/1/messages \
  -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: message-001' \
  -d '{"senderId":1,"content":"아직 판매 중인가요?"}'
```

동일 방·발신자·키·내용의 재요청은 기존 메시지를 반환합니다. 같은 키로 내용을 바꾸면 409이며, 새 메시지는 새로운 키를 사용합니다.

### 읽음 처리

```sh
curl -i -X PATCH http://localhost:8080/chat/rooms/1/read-position \
  -H 'Content-Type: application/json' \
  -d '{"memberId":2,"lastReadMessageId":1}'
```

Apidog/Postman에 [요청 모음](docs/chat-api.postman_collection.json)을 가져올 수 있습니다.

## 현재 구현 범위

채팅방 중복 방지, 참여자 검사, 자기 상품 채팅 금지, 입력 검증, ProblemDetail 오류 응답, 메시지 멱등 처리, 읽음 위치 역행 방지를 구현했습니다. 테스트는 HTTP 요청으로 정상·오류·접근 제한·읽음·안 읽은 수·페이지 조회·동시 요청을 검증합니다.

현재 학습용 메모리 저장 방식으로 서버 재시작 시 모든 데이터와 멱등 기록이 초기화됩니다. 동시성 제어와 멱등 처리는 단일 서버 프로세스 기준입니다.

로그인 인증은 아직 연결되지 않았습니다. 입력받은 `memberId`·`senderId`의 참여 여부 검사만 하며 실제 로그인 사용자 검증을 대체하지 않습니다. Market 상품·판매자 확인과 Trade 거래 생성·상태 조회는 미연동이며, `integration` 패키지는 인터페이스만 포함합니다.

결제 요청 메시지와 거래 상태 표시는 팀 API 계약 확정 후 구현할 예정입니다. [외부 연동 합의 항목](docs/INTEGRATION-CONTRACT.md)을 참고하세요.
