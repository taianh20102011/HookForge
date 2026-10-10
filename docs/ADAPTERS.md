# Viết adapter mới

1. Tạo module Maven mới và thêm làm module trong `pom.xml` root.
2. Thêm dependency `hookforge-api` (và Paper API `provided` nếu cần Bukkit types).
3. Implement một trong các interface công khai: `EconomyService`, `PlaceholderService` hoặc `PermissionService`; nếu là một domain mới, đề xuất thêm interface vào `hookforge-api` trước.
4. Không tham chiếu class từ plugin tùy chọn trực tiếp khi chưa đảm bảo dependency/class loading. Kiểm tra plugin được cài và enabled; với adapter optional, reflection là một lựa chọn để tránh `NoClassDefFoundError`.
5. Trả `HookResult.failure` với thông tin cụ thể khi provider không có, method không tồn tại hoặc operation thất bại.
6. Thêm module vào `hookforge-paper/pom.xml`; adapter phải được đưa vào shaded plugin jar nhưng không shade Paper API hoặc mã của plugin phụ thuộc.
7. Thêm trạng thái vào doctor, config explanation, README và test tối thiểu cho trường hợp dependency vắng mặt.

Không nên tạo fallback im lặng cho thao tác có tác dụng phụ (ví dụ chuyển tiền). Chỉ chọn provider khác khi biết rõ việc đổi provider không làm sai dữ liệu hoặc tạo hai giao dịch.
