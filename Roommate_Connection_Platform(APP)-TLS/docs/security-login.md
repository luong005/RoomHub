# Security logging và giới hạn đăng nhập

Áp dụng cho `POST /api/login` trên một tiến trình backend. API nhận `phone_number` và `password`; đăng nhập thành công vẫn trả `accessToken` và `refreshToken` theo cấu trúc cũ.

## Database

Chỉ thêm bảng `security_events`. Entity `SecurityEvent` không kế thừa entity nghiệp vụ và không có quan hệ JPA, khóa ngoại, trigger hay cascade đến các bảng khác. `phone_number` trong log là giá trị tại thời điểm gửi request; cho phép NULL khi IP bị chặn trước khi đọc body.

Script tạo bảng: [sql/001_create_security_events.sql](sql/001_create_security_events.sql). Chạy trong database được cấu hình bằng `spring.datasource.url`, hiện là `roommate connection platform`. Khi dùng MySQL Workbench, chọn đúng schema trước khi chạy script. Nếu bảng đã có, kiểm tra cấu trúc trước; script không thay thế bảng hoặc xóa dữ liệu.

`spring.jpa.hibernate.ddl-auto=validate` kiểm tra mapping khi khởi động và không tự sửa schema. Nếu triển khai sang database khác, chạy script trước khi khởi động. Nếu Hibernate báo lệch mapping của bảng cũ, xử lý như một vấn đề riêng; không đổi về `update` để tự sửa các bảng đó.

Kiểm tra bảng và xem log:

```sql
SHOW CREATE TABLE security_events;

SELECT COUNT(*) AS foreign_key_count
FROM information_schema.KEY_COLUMN_USAGE
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'security_events'
  AND REFERENCED_TABLE_NAME IS NOT NULL;

SELECT id, event_type, status, phone_number, ip_address,
       endpoint, created_at, message
FROM security_events
ORDER BY id DESC
LIMIT 50;
```

`created_at` lưu giờ UTC trong `DATETIME(6)`. Có index theo thời gian, IP + thời gian và số điện thoại + thời gian. Chưa có API công khai đọc log hoặc tác vụ tự xóa log.

## Quy tắc

```properties
app.security.login.max-failures=5
app.security.login.failure-window-seconds=300
app.security.login.block-duration-seconds=300
app.security.login.ip-max-requests=20
app.security.login.ip-window-seconds=300
```

Tất cả giá trị phải lớn hơn 0. Thay cấu hình cần khởi động lại backend.

| Bộ đếm | Cửa sổ và kết quả |
|---|---|
| IP | Cửa sổ cố định bắt đầu từ request đầu tiên. Request 1–20 được qua bước kiểm tra IP; từ request 21 trả 429 đến hết cửa sổ. |
| IP + số điện thoại | Cửa sổ đếm sai bắt đầu từ lần sai đầu tiên. Sai lần 1–4 trả 401; lần 5 trả 429 và bắt đầu chặn thêm 300 giây. |

Đăng nhập thành công xóa bộ đếm sai của cặp, giữ bộ đếm IP. Request sai JSON, vi phạm validation hoặc đang bị chặn theo cặp vẫn tiêu thụ lượt IP. Chỉ `BusinessException` có code `LOGIN_FAILED` và status 401 từ hàm đăng nhập hiện tại mới tăng số lần sai; lỗi hệ thống không tăng bộ đếm này.

Request gửi trong lúc bị chặn không kéo dài thời gian chặn. Khi cửa sổ đếm sai hết hạn, trạng thái đang chặn vẫn được giữ đến `blockedUntil`. Kiểm tra IP có ưu tiên trước; hết chặn IP chưa chắc đã hết chặn cặp.

Ví dụ: sai lúc 19:00, 19:01, 19:02, 19:03 và 19:04 → chặn đến 19:09. Tại 19:05, tác vụ dọn RAM không được xóa trạng thái chặn. Tại 19:06, request trả thời gian chờ còn 180 giây.

Bộ đếm dùng RAM và dọn mục hết hạn mỗi 60 giây. Việc kiểm tra, xác thực và cập nhật được tuần tự hóa cho từng định danh bằng một tập khóa cố định; cập nhật IP là nguyên tử. Khởi động lại xóa bộ đếm nhưng giữ log MySQL. Khi chạy nhiều instance, mỗi instance có bộ đếm riêng; cần kho bộ đếm dùng chung trước khi triển khai mô hình đó.

## Cách xác định IP và số điện thoại

IP lấy từ `HttpServletRequest.getRemoteAddr()`. Filter không tự tin cậy `X-Forwarded-For` do client gửi. Nếu sau này có reverse proxy, cấu hình nguồn forwarded headers đáng tin cậy trước khi thay cách nhận diện IP.

Database hiện dùng collation `utf8mb4_0900_ai_ci`. `LoginIdentityResolver` dùng cùng `UserRepository.findByPhoneNumber()` với luồng xác thực để các cách viết được MySQL coi là cùng tài khoản có chung bộ đếm. ID tài khoản chỉ dùng trong khóa RAM; bảng log không có `user_id` hoặc quan hệ với `users`. Số điện thoại chưa tồn tại dùng khóa SHA-256 có độ dài cố định từ chuỗi đã strip/lowercase. Không sửa số điện thoại hoặc cách xác thực của tài khoản hiện có.

## Luồng backend

```text
POST /api/login
  → LoginRateLimitFilter: đếm IP trước khi đọc body
  → MVC validation
  → LoginSecurityService: kiểm tra cặp IP + định danh
  → IUserService.login(): xác thực và tạo token theo logic cũ
  → cập nhật bộ đếm, giải phóng khóa
  → SecurityEventService: ghi sự kiện bằng transaction riêng
  → trả token, 401 hoặc 429
```

Filter chỉ được đăng ký trong Spring Security, trước `JwtTokenFilter`; đăng ký servlet tự động của filter mới bị tắt để tránh đếm hai lần. Chỉ đường dẫn `/api/login` với phương thức POST được tính; OPTIONS và API khác được chuyển tiếp.

`LoginSecurityService` không mở transaction bao ngoài. Transaction của `UserService.login()` hoàn tất trước khi ghi audit. `SecurityEventService` dùng `TransactionTemplate` với `REQUIRES_NEW`, bắt lỗi cả khi lưu và khi commit. Lỗi ghi audit được cảnh báo bằng tên loại exception, không thay kết quả đăng nhập hoặc quyết định chặn.

| event_type | status | Console |
|---|---|---|
| LOGIN_SUCCESS | SUCCESS | INFO |
| LOGIN_FAILED | DENIED | INFO |
| LOGIN_RATE_LIMITED | BLOCKED | WARN |
| LOGIN_IP_RATE_LIMITED | BLOCKED | WARN |

Lần sai thứ 5 ghi cả hai sự kiện thất bại và bắt đầu chặn trong cùng transaction audit. Những request đã bị chặn không ghi lại sự kiện bắt đầu chặn. Khi IP hết cửa sổ và vượt ngưỡng trong cửa sổ mới, có thể phát cảnh báo mới.

Không đưa DTO đăng nhập, entity người dùng, mật khẩu, OTP, token, body hoặc exception message thô vào log. Các trường đầu vào được giới hạn độ dài và loại ký tự điều khiển; `message` được tạo từ các mô tả cố định của backend.

## Response cho Expo

HTTP 429 có `Retry-After` là số giây còn lại, làm tròn lên, và `Cache-Control: no-store`.

```json
{
  "timestamp": "2026-09-09T12:00:00Z",
  "status": 429,
  "code": "LOGIN_RATE_LIMITED",
  "message": "Bạn đã thử đăng nhập quá nhiều lần. Vui lòng thử lại sau.",
  "details": {
    "retryAfterSeconds": 300,
    "scope": "IP_PHONE"
  }
}
```

Khi giới hạn IP có hiệu lực: `code=LOGIN_IP_RATE_LIMITED`, `scope=IP`.

Source Expo chưa có trong workspace này. Khi tích hợp vào hàm gọi API/màn hình đăng nhập:

1. Khi status là 429, đọc `details.retryAfterSeconds`, dùng header `Retry-After` làm dự phòng.
2. Lưu thời điểm có thể thử lại bằng `Date.now() + seconds * 1000`.
3. Hiển thị số giây còn lại từ thời điểm này, vô hiệu hóa nút đăng nhập trong lúc chờ, cập nhật lại khi app trở về foreground.
4. Hết giờ thì cho phép người dùng tự thử lại. Backend tiếp tục quyết định có cho phép xác thực hay không.
5. Không tự gửi lại login hoặc refresh token vì nhận 429. Với Expo Web, nếu frontend đọc header ở origin khác, cấu hình CORS phải expose `Retry-After`; đọc trường JSON không cần expose header này.

## Kiểm thử

Kết quả kiểm tra ngày 09/09/2026: 32 test đạt, không lỗi hoặc test bị bỏ qua. Spring context khởi động với MySQL và `ddl-auto=validate`. Bản dump cấu trúc của 11 bảng cũ trước và sau triển khai giống nhau; database có thêm đúng `security_events`, với 0 khóa ngoại và 0 trigger. Sau kiểm thử, bảng log không còn dòng dữ liệu test (các lần chèn kiểm tra đã rollback).

```powershell
.\mvnw.cmd test
```

Cần Java 21, Maven/dependency cache hoặc kết nối tải dependency, MySQL đang chạy và cấu hình `.env` hiện có. Với máy hiện tại có thể dùng Maven đã cài trong `C:\Program Files\Java\apache-maven-3.9.11` và JDK trong `C:\Program Files\Java\jdk-21`.

Unit test dùng `MutableClock` để kiểm tra ranh giới 300 giây và chạy đồng thời mà không phải chờ thật. HTTP test kiểm tra header/JSON, bộ đếm trước validation, giả mạo forwarded header, lỗi qua JWT filter và việc các API khác vẫn được chuyển tiếp.

Test context dùng database đã cấu hình ở chế độ `validate`, mock scheduler để không chạy tác vụ đăng bài hẹn giờ. Các request kiểm tra qua context dùng tài khoản không tồn tại và mock audit writer; kiểm tra JPA thật chèn một dòng log rồi rollback, không để lại dữ liệu kiểm thử. Kiểm thử riêng của audit writer bao gồm lỗi insert, lỗi commit và không lộ nội dung exception.

Để demo bằng Postman hoặc Expo: khởi động backend như hiện tại, dùng tài khoản demo, gửi 5 lần sai rồi thử mật khẩu đúng trong lúc bị chặn. Xem `Retry-After` và bảng log. Khi kiểm tra IP, các lần thử trước đó cũng tính vào 20 lượt; đợi hết cửa sổ hoặc khởi động lại backend để bắt đầu kịch bản mới. Khởi động lại không xóa lịch sử log.

## Phạm vi thay đổi

Giữ nguyên `UserService`, `IUserService`, các entity/repository nghiệp vụ và cấu trúc token. Các file cũ được sửa là `UserController`, `WebSecurityConfig`, `GlobalExceptionHandler`, `application.properties` và test context. Các thao tác ghi refresh token của đăng nhập thành công vẫn diễn ra theo logic cũ.

Nếu cần quay lại phiên bản cũ, gỡ phần tích hợp controller/filter/handler và các lớp mới tương ứng. Có thể giữ bảng log để bảo toàn lịch sử; không cần xóa hoặc thay đổi bảng cũ. Việc quản lý schema vẫn cần được kiểm soát riêng.

Tham khảo: [OWASP Authentication](https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html), [OWASP Logging](https://cheatsheetseries.owasp.org/cheatsheets/Logging_Cheat_Sheet.html), [Spring transaction propagation](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/tx-propagation.html), [HTTP 429](https://www.rfc-editor.org/rfc/rfc6585.html#section-4).
