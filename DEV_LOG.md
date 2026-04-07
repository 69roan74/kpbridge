# KPBridge 개발 로그

> 마지막 업데이트: 2026-04-07 (2차 세션)

---

## 프로젝트 개요
- **스택**: Spring Boot 3.x, Thymeleaf, Spring Security, MariaDB, JPA/Hibernate
- **VPS**: HostMeNow, Ubuntu 20.04, IP: 139.99.39.33, 싱가포르
- **SSH**: `ssh root@139.99.39.33`
- **도메인**: kpbrdg.agency (기업은행 계좌 연동)
- **GitHub**: https://github.com/69roan74/kpbridge

---

## VPS 배포 / 업데이트 방법

```bash
ssh root@139.99.39.33
cd /root/kpbridge
git pull
./mvnw package -DskipTests
systemctl restart kpbridge
```

### 서비스 관리
```bash
systemctl status kpbridge    # 상태 확인
systemctl restart kpbridge   # 재시작
journalctl -u kpbridge -f    # 실시간 로그
```

---

## DB 정보
- **DB명**: kpbridge_db
- **계정**: root / tiger
- **접속**: MariaDB 10.3 on localhost

---

## 구현된 기능 목록

### ✅ 1. Telegram 완전 제거
- `TransactionService`에서 텔레그램 HTTP 호출 제거
- `application.properties`에서 `telegram.bot-token`, `telegram.chat-id` 제거

---

### ✅ 2. 1:1 채팅 시스템 (폴링 방식)

#### 엔티티
- `ChatMessage.java`: id, member(ManyToOne), senderType(USER/ADMIN), content(TEXT), sentAt, isRead

#### 레포지토리
- `ChatMessageRepository.java`
  - `findByMemberIdOrderBySentAtAsc(Long memberId)`
  - `countByMemberIdAndSenderTypeAndIsReadFalse(Long memberId, String senderType)`
  - `countBySenderTypeAndIsReadFalse(String senderType)`
  - `findDistinctMemberIds()` — **수정됨**: `m.member.id` GROUP BY로 회원별 최근순 반환

#### 서비스
- `ChatService.java`
  - `record ChatListItem(Member member, long unreadCount, ChatMessage lastMessage)`
  - `getChatList()`: 미확인 많은 순 → 최근 메시지 순 정렬
  - `sendUserMessage()`, `sendAdminMessage()`, `sendSystemMessage()`
  - `getMessagesForUser()`: ADMIN 메시지 읽음 처리
  - `getMessagesForAdmin()`: USER 메시지 읽음 처리

#### 컨트롤러
- `ChatController.java`
  - `GET /chat` → `chat.html` (사용자 채팅창)
  - `POST /api/chat/send` → 사용자 메시지 전송
  - `GET /api/chat/messages` → 사용자 폴링
  - `GET /admin/chat` → `admin-chat.html` (관리자 채팅 목록)
  - `GET /admin/chat/{memberId}` → `admin-chat-room.html` (개별 채팅방)
  - `POST /api/admin/chat/send` → 관리자 답장
  - `GET /api/admin/chat/messages/{memberId}` → 관리자 폴링

#### 템플릿
- `chat.html`: 5초 폴링, ADMIN 메시지만 추가
- `admin-chat.html`: 미확인 배너(🚨), 빨간 테두리 카드, 바운싱 배지
- `admin-chat-room.html`: 5초 폴링, USER 메시지만 추가

---

### ✅ 3. 채팅 자동 아카이빙

- `ChatArchiveService.java`
  - `@Scheduled(cron = "0 0 0 * * *")`: **매일 자정** 전날 채팅 자동 ZIP 저장
  - 저장 위치: `/root/kpbridge/chat-archives/YYYY-MM-DD.zip`
  - **30일 이상 된 파일 자동 삭제**
  - `buildZipBytes(LocalDate)`: 브라우저 다운로드용 메모리 ZIP 생성
  - `KpbridgeApplication.java`에 `@EnableScheduling` 추가

- `AdminController.java`
  - `GET /admin/chat/archive?date=YYYY-MM-DD`: 브라우저로 ZIP 다운로드

- `admin-chat.html` 하단에 날짜 선택 + ZIP 다운로드 버튼 추가

---

### ✅ 4. 거래 주문 상태 시스템

#### 상태 흐름
`거래대기중` → `거래중` → `거래완료`

#### Transaction 엔티티 추가 필드
- `tradeStatus`: 거래 전용 상태
- `coinType`: 코인 종류
- `route`: 거래 경로 (예: Binance→Upbit)
- `memo`: 출금처 정보 (지갑 주소 or 계좌번호)

#### TransactionService
- `submitOrder()`: 거래 주문 생성 (상태: 거래대기중)
- `completeOrder(txId)`: 수익 3.5~5% 계산 → 잔액 반영 → 추천인 보상 전파
- `updateTradeStatus(txId, newStatus)`: 상태 변경

#### AdminController
- `POST /admin/order/status`: 거래 상태 변경 (거래완료 시 completeOrder 호출)

---

### ✅ 5. 피라미드 추천인 네트워크 트리

- `ReferralService.java`: 잔액별 색상 클래스, 마스킹 제거 (실명 표시)
- `mypage.html`: 가로 확장 org-chart CSS
  - 루트 `.org-tree`: `flex-direction: column`
  - 자식 `ul`: `flex-direction: row` ← **핵심 버그 수정**
  - 잔액별 색상: 🟢 500만+ / 🟡 200만+ / 노랑 50만+ / 🔴 10만+ / 🔵 미만

---

### ✅ 6. 추천인 코드 필수화

- `MemberService.java`: 추천인 코드 없으면 예외 발생
  ```java
  if (inviteCode == null || inviteCode.isBlank()) {
      throw new RuntimeException("추천인 코드는 필수입니다.");
  }
  ```

---

### ✅ 7. 전체 모바일 반응형 (iPhone 13 mini 375px 기준)

- `login.html`, `register.html`, `mypage.html`, `admin.html`, `about.html`, `faq.html`, `member-edit.html`
- `viewport` 메타 태그 전체 추가
- `clamp()` 폰트 크기, `min()` 너비
- input `font-size: 16px` (iOS 줌 방지)
- 관리자 페이지: 햄버거 메뉴 + 사이드바 오버레이

---

### ✅ 8. 충전/출금 모달 (USDT + 원화)

#### 충전 모달
- **USDT 탭**: 회사 TRC20 지갑 주소 표시 + 복사 버튼
- **원화 탭**: 회사 은행 계좌 표시 + 복사 버튼
- 금액 입력 후 신청

#### 출금 모달
- **USDT 탭**: 사용자 TRC20 지갑 주소 입력
- **원화 탭**: 사용자 은행 계좌 입력
- 출금처 정보가 `Transaction.memo`에 저장됨
- 관리자 장부 테이블에 **출금처 컬럼** 추가

---

### ✅ 9. 관리자 입금 정보 설정 (동적)

- `SiteConfig.java`: `SITE_CONFIG` 테이블 key-value 엔티티
- `SiteConfigRepository.java`
- `SiteConfigService.java`: `get(key, default)` / `set(key, value)`
- 관리자 페이지 → **💳 입금 정보 설정** 메뉴
  - USDT 지갑 주소 수정 가능
  - 은행 계좌 수정 가능
  - 저장 즉시 사용자 마이페이지에 반영

---

### ✅ 10. 버그 수정 이력

| 날짜 | 파일 | 내용 |
|------|------|------|
| 2026-04-07 | `ChatMessageRepository.java` | `findDistinctMemberIds()` 쿼리 오류: `m.id` → `m.member.id` GROUP BY 수정 |
| 2026-04-07 | `mypage.html` | org-chart 트리 단일 컬럼 버그: 자식 ul에 `flex-direction: row` 적용 |
| 2026-04-07 | `AdminController.java` | 중복 닫는 괄호 `}` 제거 |
| 2026-04-07 | `TransactionController.java` | withdraw 요청 타입 `Map<String,BigDecimal>` → `Map<String,Object>` 변경 |

---

## 파일 구조 (주요)

```
src/main/java/com/kpbridge/kpbridge/
├── config/
│   └── SecurityConfig.java
├── controller/
│   ├── AdminController.java      # 관리자 전체 + 아카이브 다운로드
│   ├── ChatController.java       # 1:1 채팅 API
│   ├── MemberController.java     # 마이페이지
│   └── TransactionController.java
├── entity/
│   ├── ChatMessage.java
│   ├── Member.java
│   ├── SiteConfig.java           # 신규: 사이트 설정
│   ├── Transaction.java          # memo 필드 추가
│   └── ...
├── repository/
│   ├── ChatMessageRepository.java
│   ├── SiteConfigRepository.java # 신규
│   └── ...
└── service/
    ├── ChatArchiveService.java   # 신규: 자동 아카이빙
    ├── ChatService.java          # 신규: 채팅 로직
    ├── SiteConfigService.java    # 신규: 사이트 설정
    ├── TransactionService.java
    └── ...

src/main/resources/templates/
├── admin.html
├── admin-chat.html
├── admin-chat-room.html
├── chat.html
├── mypage.html
├── login.html
├── register.html
└── ...
```

---

## 임시 설정값 (변경 필요)
> 관리자 페이지 → 💳 입금 정보 설정에서 수정 가능

| 항목 | 현재 임시값 |
|------|------------|
| USDT TRC20 지갑 | `TKpBridge9xCzFm2nHqRsUvWy3D7K` |
| 은행 계좌 | `기업은행 123-45-6789012` |

---

---

## 2차 세션 작업 내역 (2026-04-07 오후)

### ✅ 입출금 관리자 승인 시스템
- 충전/출금 신청 → 즉시 잔액 반영 ❌ → **관리자 승인 후 반영**으로 변경
- `TransactionService`: `approveDeposit()`, `approveWithdraw()`, `rejectRequest()` 추가
- `TransactionRepository`: `findByTypeContainingAndStatusOrderByDateDesc()` 추가
- `AdminController`: 승인/거절 POST 엔드포인트 3개 추가
- `admin.html`:
  - 사이드바 **💳 입출금 승인** 메뉴 추가 (대기 건수 뱃지)
  - 대시보드 파란 알림 배너 추가
  - 충전/출금 각각 테이블 + 승인/거절 버튼

### ✅ 채팅 UI 개선
- `admin-chat-room.html`: max-width 제거 → 화면 꽉 채움
- 메시지 `justify-content: flex-end` → 카카오톡처럼 아래서부터 표시
- 말풍선 `max-width: 90%`, 패딩/폰트 확대
- 모바일에서 패딩 제거, 모서리 각지게

### ✅ 관리자 버튼 네브바 추가
- `main.html`, `mypage.html`: ADMIN 역할일 때 빨간 **⚙️ 관리자** 버튼 표시

### ✅ 메인 페이지 플로팅 버튼 개편
- 1:1 문의 파란 플로팅 버튼 거래하기 **위에** 추가
- 두 버튼 세로로 묶어서 우하단 고정

### ✅ 거래소 선택 모바일 정렬
- 국내/해외 드롭다운 버튼 모바일에서 한 줄 나란히 표시
- 버튼 크기 축소 (padding, font-size, img 사이즈)

### ✅ 버그 수정
- `ChatMessageRepository.findDistinctMemberIds()`: `m.id` → `m.member.id` GROUP BY 수정 (치명적 버그)

---

## 내일 할 일
- 시장 분석 섹션 UI 개선 (코인 시세 테이블 하단 부분)
- 모바일 화면 추가 점검

---

## 남은 작업 / 추후 수정 예정
- 시장분석 금액 표시 부분 상세 개선 (다음 세션)
