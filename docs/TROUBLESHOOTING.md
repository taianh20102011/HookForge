# Troubleshooting

## Maven không resolve được paper-api

- Đảm bảo có internet và Maven có thể truy cập `https://repo.papermc.io/repository/maven-public/`.
- Chạy `mvn -U clean package` để làm mới metadata.
- Kiểm tra Java/Maven bằng `java -version` và `mvn -version`.

## Economy bridge báo thiếu provider

Vault là abstraction chứ không tự tạo tiền. Cần có plugin kinh tế thực sự đăng ký Vault Economy service. Chạy `/hookforge doctor` và xem log của provider lúc server khởi động.

## PlaceholderAPI chưa sẵn sàng

Cài và enable PlaceholderAPI rồi restart server. Kiểm tra `/plugins` và log. HookForge không ship hoặc bundle PlaceholderAPI.

## Plugin khác không load được HookForgeAPI

Thêm dependency `hookforge-api` với scope `provided`, khai báo `depend: [HookForge]`, không đóng gói bản sao `hookforge-api` vào plugin consumer. Nếu plugin consumer shade API riêng, class identity có thể khác và Bukkit ServicesManager không tìm thấy service.

## Cấu hình chưa được áp dụng

`/hookforge reload` validate trước khi thay đổi config hoạt động. Khi validator phát hiện giá trị không hợp lệ, sửa đúng khóa rồi chạy lại. `/hookforge setup` sẽ backup config cũ thành `config.yml.bak`.
