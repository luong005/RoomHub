# Đăng nhập: hiển thị giới hạn từ backend

Phần này chỉ bổ sung xử lý HTTP 429 vào frontend Expo Go. Backend Spring Boot quyết định giới hạn và lưu sự kiện trong bảng độc lập `security_events`; frontend không ghi log bảo mật, không gửi mật khẩu/token vào log và không thêm bảng dữ liệu.

## Phạm vi thay đổi

- `src/services/api.js`: giữ thông báo và metadata của 429; ưu tiên `details.retryAfterSeconds`, dự phòng bằng header `Retry-After`. Không tự gửi lại request hay refresh token vì 429.
- `src/utils/loginRateLimit.js`: đọc thời gian chờ, phân biệt `IP` và `IP_PHONE`, tính thời gian còn lại từ một mốc kết thúc.
- `src/hooks/useLoginRateLimit.js`: cập nhật đếm ngược mỗi giây và khi app trở lại foreground; dọn timer/listener khi hết hạn hoặc rời màn hình.
- `src/app/login.jsx`: dùng lại ô thông báo và nút có sẵn. Khi bị chặn, hiển thị `mm:ss` và tạm vô hiệu hóa nút đăng nhập.
- `src/services/auth.service.js`: xử lý riêng trường hợp đăng ký đã thành công nhưng đăng nhập tự động nhận 429. Lần thử tiếp theo chỉ gọi login, tránh tạo lại tài khoản.

Không đổi bố cục, StyleSheet, màu sắc, nhãn nút, component dùng chung, điều hướng, validation hiện có, cách lưu token, các API nghiệp vụ khác, `.env` hoặc dependency. Luồng thành công và refresh token vẫn như cũ. Chỉ bổ sung hành vi khi backend giới hạn đăng nhập.

## Quy tắc sử dụng

Backend hiện dùng số điện thoại (`phone_number`), không phải username:

- Sai 5 lần theo IP + số điện thoại trong 5 phút: lần thứ 5 trả 429, chặn cặp đó thêm 5 phút.
- Tổng 20 request login/IP trong 5 phút: từ request 21 trả 429 đến hết cửa sổ IP.
- Trong lúc chờ, app không tự thử lại. Hết đếm ngược, người dùng tự nhấn nút; backend vẫn là nơi quyết định cho phép đăng nhập.

Chặn `IP_PHONE` chỉ áp dụng cho số đang bị chặn. Đổi số rồi quay lại không đặt lại đếm ngược. Chặn `IP` áp dụng cho mọi số trên màn hình. Chuyển tab không xóa thời gian chờ.

Đăng ký tài khoản mới vẫn được phép vì backend chỉ giới hạn login. Nếu đăng ký xong nhưng login bị giới hạn, ô thông báo xác nhận tài khoản đã tạo và hiển thị thời gian chờ. Sau đó nhấn lại nút hiện có để thử login; không gửi lại `/register`.

Trạng thái chờ nằm trong bộ nhớ của màn hình; dấu nhớ đăng ký thành công chỉ giữ số điện thoại trong bộ nhớ của tiến trình app, không giữ mật khẩu/token. Khi tải lại app, backend vẫn giữ bộ đếm của nó. Nếu đã tạo tài khoản trước khi tải lại, dùng tab **Đăng nhập**.

Nếu 429 thiếu thời gian hợp lệ, app hiển thị thông báo backend, không tự đặt thời gian khóa vô hạn. Không thay cấu hình API; tiếp tục dùng `EXPO_PUBLIC_API_URL` hiện có.

## Kiểm thử tự động

Từ thư mục frontend, với các dependency đã cài:

```powershell
npm run check
node --test scripts/test-login-rate-limit.mjs
```

Bộ test sử dụng Axios thật với adapter mô phỏng response, Babel có sẵn trong dependency của Expo và clock giả. Không gọi backend, tạo tài khoản thật hay ghi vào database. Có kiểm tra luồng đăng nhập, đăng ký, refresh token, 401/429, metadata thiếu/sai, phạm vi IP/số điện thoại, thời gian sau khi app về nền và việc dọn timer/listener.

## Kiểm tra thủ công trên Expo Go

1. Khởi động backend và Expo như hiện tại; không cần đổi giao diện hay cấu hình API.
2. Dùng tài khoản demo với mật khẩu sai dài ít nhất 6 ký tự. Thử 5 lần: 4 lần đầu hiện lỗi cũ; lần 5 hiện thời gian chờ, nút đăng nhập tạm bị vô hiệu hóa.
3. Chuyển tab rồi quay lại; đổi số rồi quay về số cũ; kiểm tra thời gian chờ không bắt đầu lại.
4. Đưa app về nền khoảng một phút rồi mở lại. Thời gian chờ phải phản ánh thời gian thực đã trôi qua.
5. Hết giờ, nhập đúng mật khẩu và nhấn đăng nhập. Luồng tải hồ sơ và điều hướng phải giống trước.
6. Khi kiểm tra giới hạn IP, tính cả mọi lần thử login trước đó. Có thể dùng Postman để vượt ngưỡng rồi thử từ app nếu hai bên được backend nhận là cùng IP.
7. Xem sự kiện ở MySQL/backend, không ở log của frontend.

Đối với đăng ký bị ảnh hưởng bởi giới hạn IP, chỉ dùng tài khoản demo mới khi muốn kiểm tra thao tác tạo tài khoản thực sự. Sau khi đăng ký thành công và login nhận 429, chờ hết hạn rồi thử lại: backend không được nhận thêm request đăng ký cho cùng lần tạo tài khoản đó.

Kiểm thử tự động không thay thế kiểm tra trực tiếp trên thiết bị Expo Go.
