# RoomMate Mobile

Ứng dụng Android React Native + Expo Router được chuyển từ giao diện web RoomMate và kết nối với API Spring Boot trong `Fontend_room (2).zip`. Mã ứng dụng chỉ dùng JavaScript/JSX, không dùng HTML, DOM, WebView, React Native Web hay dữ liệu mock.

## Cài đặt

```bash
npm install --legacy-peer-deps
copy .env.example .env
```

Sửa `.env` để trỏ đến backend:

```env
EXPO_PUBLIC_API_URL=http://192.168.1.10:8083/api
```

- Điện thoại thật: dùng IPv4 LAN của máy chạy backend, xem bằng `ipconfig`. Không dùng `localhost`.
- Android Emulator: dùng `http://10.0.2.2:8083/api`.
- Điện thoại và máy tính phải cùng Wi-Fi; Windows Firewall phải cho phép cổng `8083`.
- Bản production bắt buộc dùng API HTTPS; ứng dụng chủ động từ chối URL HTTP khi không ở chế độ development.

Khởi động backend trước, sau đó chạy:

```bash
npx expo start --clear
```

Mở Expo Go trên Android và quét QR. Dự án này không cấu hình web, vì vậy không chạy `npx expo start --web`.

## Chức năng đã nối API

- Đăng nhập, đăng ký, khôi phục phiên còn hạn, đăng xuất cục bộ an toàn.
- Route bảo vệ bằng `AuthContext` và Expo Router.
- Xem/lọc bài đăng; tạo, cập nhật, xóa; chọn và upload tối đa 5 ảnh.
- Gửi yêu cầu vào phòng; xem phòng đã tham gia.
- Hồ sơ, hoàn tất hồ sơ, cập nhật thông tin và đổi mật khẩu.
- Thông báo đã gửi/đã nhận; chấp nhận hoặc từ chối yêu cầu.
- Danh sách chat, yêu cầu tin nhắn, lịch sử, chấp nhận chat và chat realtime qua STOMP WebSocket.
- Trang công khai của người đăng, thư viện ảnh và mở địa chỉ bằng ứng dụng bản đồ.
- Trang quản trị dành cho role `ADMIN`: tổng số và danh sách người dùng có phân trang.
- Loading, empty, error, retry, pull-to-refresh, SafeAreaView và KeyboardAvoidingView.

## API đang sử dụng

`POST /login`, `POST /register`, `GET /profile`, `PATCH /update-info`, `POST /update-password`, `POST /logout`

`GET/POST /posts`, `GET/PATCH /posts/{id}`, `DELETE /post/{id}`, `GET /my-posts`, `GET /user_joined`

`POST/PATCH /request_join`, `GET /Received_notifications`, `GET /Sent_notifications`

`GET /users?page={page}&size={size}` dành cho quản trị viên

`GET /chat-rooms`, `GET /chat-rooms/requests`, `GET /chat-rooms/{id}/messages`, `PATCH /chat-rooms/{id}/accept`, `PATCH /chat-rooms/{id}/read`

WebSocket `/ws`, subscribe `/user/queue/chat-rooms`, `/topic/chat-rooms/{id}`, send `/app/chat.direct`, `/app/chat.send`.

## Bảo mật phía mobile

- Access token chỉ giữ trong RAM; refresh token lưu bằng `expo-secure-store`, không dùng AsyncStorage.
- Axios nằm tập trung tại `src/services/api.js`; token Bearer, timeout, refresh token rotation và 401 được xử lý bằng interceptor.
- Khi access token hết hạn hoặc app vừa khởi động lại, mobile gửi `{ refreshToken }` tới `POST /refresh`, giữ access token mới trong RAM và thử lại request cũ đúng một lần.
- URL API chỉ lấy từ `EXPO_PUBLIC_API_URL`; component không chứa URL backend.
- Không ghi token, mật khẩu, payload chat hoặc dữ liệu nhạy cảm ra log.
- Upload giới hạn loại ảnh, tối đa 5 ảnh và 10 MB/ảnh theo cấu hình backend.
- Các request chỉ được thử lại sau phản hồi 401 và sau khi refresh access token thành công.

## Giới hạn cần sửa ở backend

Các điểm sau không thể xử lý đúng chỉ bằng frontend mobile:

1. Luồng mobile yêu cầu `POST /refresh` và `POST /logout` nhận `{ refreshToken }` trong JSON; production phải dùng HTTPS. Backend nên xoay vòng refresh token, chỉ lưu hash, ràng buộc thiết bị và hỗ trợ thu hồi.
2. Cookie đăng nhập web đang đặt `secure(false)` và WebSocket cho phép origin `*`; production cần `Secure`, cấu hình SameSite/CORS/origin rõ ràng và HTTPS/WSS.
3. `GET /announcement/{id}` đang trả `null`, nên chưa có màn hình chi tiết thông báo.
4. Backend không có API lấy cuộc chat `PENDING` do chính người dùng gửi, nên cuộc chat chờ có thể không tìm lại được sau khi rời màn hình.
5. `PATCH /posts/{id}` khai báo ảnh không bắt buộc nhưng hàm validation vẫn bắt buộc ít nhất một ảnh. Mobile hiện yêu cầu chọn ảnh mới khi sửa để khớp backend.
6. DTO cập nhật bài không hỗ trợ giữ trạng thái `SCHEDULED`; mobile vô hiệu hóa chỉnh sửa bài đã lên lịch để tránh vô tình xuất bản ngay.

## Kiểm tra dự án

```bash
npm run check
npx expo install --check
npx expo-doctor
npx expo export --platform android --output-dir .expo-test
```

Lệnh cuối tạo bundle kiểm thử; có thể xóa `.expo-test` sau khi kiểm tra.

`npm audit` hiện báo cảnh báo gián tiếp trong chuỗi Expo 54/Metro. `expo-doctor` phải đạt 18/18. Không chạy `npm audit fix --force`, vì nó nâng major Expo và làm sai bộ React Native tương thích. Expo vẫn hướng dẫn dùng SDK 54 khi thử bằng Expo Go trên điện thoại thật trong giai đoạn chuyển đổi SDK 57.
