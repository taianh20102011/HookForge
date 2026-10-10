# Cấu hình HookForge

## Profile beginner

Chỉ cần `integrations.*.enabled` và `provider: auto`. `auto` có nghĩa là tự phát hiện provider thuộc tập adapter HookForge đã triển khai, không phải tự tương thích mọi plugin.

## Profile advanced

- `integrations.economy.provider`: `auto`, `vault`, `none`.
- `integrations.placeholders.provider`: `auto`, `placeholderapi`, `none`.
- `diagnostics.level`: `info`, `normal`, `debug`.
- `diagnostics.compatibility-check`: bật/tắt kiểm tra optional integrations.
- `diagnostics.write-report`: ghi JSON sau `/hookforge doctor`.
- `diagnostics.report-file`: tên file JSON nằm ngay trong thư mục plugin.

Không có khóa provider trùng lặp: mỗi integration chỉ có một nơi cấu hình. `auto` tự phát hiện adapter mà HookForge đã triển khai; không đồng nghĩa hỗ trợ mọi plugin. Không đặt đường dẫn tùy ý cho report file; HookForge chủ động chặn path traversal.

## Mẹo sửa lỗi

1. Chạy `/hookforge doctor`.
2. Dùng `/hookforge explain <key>` để đọc mô tả.
3. Sửa config và chạy `/hookforge reload`.
4. Nếu preset mới cần thiết, `/hookforge setup beginner` hoặc `advanced`. Lệnh setup tạo backup `config.yml.bak`.

## Language / Ngôn ngữ

Set `language` to `vi`, `en`, or `cn` in `config.yml`, or use `/hookforge language <vi|en|cn>`. Editable translations are copied to `plugins/HookForge/lang/` on first start. Missing translation keys fall back to Vietnamese.
