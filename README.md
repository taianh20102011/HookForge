# HookForge — Universal API Toolkit for Paper

**Mục tiêu:** giúp developer Minecraft tích hợp API dễ hơn, giải thích cấu hình/lỗi rõ ràng và cho phép mở rộng bằng adapter.

> Đây là bản MVP mã nguồn mở, target Paper 1.21.10 + Java 21 + Maven. MVP thực hiện bridge cho Vault Economy, PlaceholderAPI và quyền Bukkit (tương thích với cách LuckPerms cung cấp quyền cho Bukkit). Không tuyên bố hỗ trợ mọi plugin: mỗi hệ thống mới cần adapter và kiểm thử riêng.

## Chức năng trong bản này

- Maven multi-module: `hookforge-api`, `hookforge-core`, adapter tách riêng và plugin Paper.
- `/hookforge setup beginner|advanced`: cài preset; bản config cũ được sao lưu thành `config.yml.bak`.
- `/hookforge doctor`: kiểm tra cấu hình và trạng thái tích hợp, có thể ghi `diagnostics.json`.
- `/hookforge explain <key>`: giải thích khóa cấu hình kèm ví dụ và lưu ý.
- `/hookforge test economy|placeholders|permissions`: kiểm tra nhanh từng module.
- `/hookforge reload`: kiểm tra config trước khi áp dụng; nếu phát hiện lỗi validation thì giữ cấu hình đang chạy.
- Đa ngôn ngữ `vi` (Tiếng Việt), `en` (English), `cn` (简体中文); đổi trực tiếp bằng `/hookforge language <vi|en|cn>`. Các file dịch có thể chỉnh sửa trong `plugins/HookForge/lang/`.
- API công khai cho plugin khác qua Bukkit `ServicesManager`.
- Tích hợp Vault và PlaceholderAPI được gọi bằng reflection nên thiếu plugin tùy chọn không làm HookForge lỗi class loading.
- Báo lỗi dưới dạng `HookResult<T>` thay vì che giấu ngoại lệ.

## Yêu cầu

- JDK 21 trở lên.
- Maven 3.9+ (cần internet ở lần build đầu để tải dependency).
- Paper 1.21.10 (hoặc bản tương thích API 1.21.10).
- Vault và một plugin economy nếu muốn dùng chức năng kinh tế. PlaceholderAPI chỉ cần nếu muốn mở rộng placeholder.

## Build trên Windows

1. Cài JDK 21 và Maven, kiểm tra bằng `java -version` và `mvn -version`.
2. Mở terminal tại thư mục có file `pom.xml`.
3. Chạy:

   ```bat
   mvn clean package
   ```

4. Lấy file `hookforge-paper/target/HookForge-0.1.0.jar` và thả vào thư mục `plugins/`.
5. Khởi động server. Không nên dùng `/reload` của Bukkit/Paper; nếu đổi config hãy dùng `/hookforge reload`.

Có thể chạy `build.bat` để build nhanh trên Windows đã cài Maven.

## Ngôn ngữ giao diện

Trong `config.yml`, đặt `language: vi`, `language: en` hoặc `language: cn`. Có thể đổi ngay trong game/console bằng `/hookforge language en` (hoặc `vi`, `cn`). Bản dịch nằm trong `plugins/HookForge/lang/messages_vi.yml`, `messages_en.yml`, `messages_cn.yml`; các khóa bị thiếu sẽ tự dùng bản tiếng Việt dự phòng.

## Cài đặt server

- Khởi động server một lần để sinh `plugins/HookForge/config.yml`.
- Làm theo hướng dẫn trong config hoặc chạy `/hookforge setup beginner`.
- Chạy `/hookforge doctor` để xem adapter nào có thể dùng.
- Các lệnh quản trị yêu cầu permission `hookforge.admin` (mặc định OP có quyền).

### Lệnh

| Lệnh | Tác dụng |
|---|---|
| `/hookforge` | Hiện trợ giúp |
| `/hookforge setup beginner` | Tạo cấu hình đơn giản |
| `/hookforge setup advanced` | Tạo cấu hình nâng cao |
| `/hookforge doctor` | Chẩn đoán và ghi báo cáo nếu bật |
| `/hookforge explain integrations.economy.provider` | Giải thích một khóa |
| `/hookforge test economy` | Kiểm tra Vault Economy provider |
| `/hookforge test placeholders` | Kiểm tra PlaceholderAPI |
| `/hookforge test permissions` | Kiểm tra lớp quyền Bukkit |
| `/hookforge reload` | Validate và nạp cấu hình |
| `/hookforge language <vi|en|cn>` | Đổi ngôn ngữ giao diện |

## Dùng API từ plugin khác

Thêm dependency `hookforge-api` với scope `provided` vào project Maven của bạn. Cần khai báo HookForge là dependency trong `plugin.yml` để đảm bảo thứ tự khởi động và class loading:

```yaml
name: ExamplePlugin
version: 1.0.0
main: me.example.ExamplePlugin
depend: [HookForge]
api-version: '1.21'
```

Ví dụ Java đầy đủ hơn ở `examples/consumer-plugin/`. Sau khi build source chính, dùng `mvn install` ở thư mục gốc để cài `hookforge-api` vào Maven local repository rồi mới build riêng project example. API được đăng ký thành Bukkit service; plugin phụ thuộc có thể lấy API như sau:

```java
HookForgeAPI api = getServer().getServicesManager().load(HookForgeAPI.class);
if (api == null) {
    getLogger().warning("HookForge API service is unavailable.");
    return;
}
```

Ví dụ gọi economy:

```java
HookResult<Double> balance = api.economy().balance(player);
if (balance.isSuccess()) {
    player.sendMessage("Balance: " + balance.value());
} else {
    getLogger().warning(balance.message());
}
```

## Module và mở rộng

- `hookforge-api`: interface và kiểu kết quả ổn định.
- `hookforge-core`: implementation API và registry giải thích cấu hình.
- `adapter-vault`: bridge economy thông qua Vault service.
- `adapter-placeholderapi`: mở rộng placeholder an toàn qua reflection.
- `adapter-bukkit-permissions`: kiểm tra quyền qua Bukkit API. LuckPerms tự tích hợp với hệ thống quyền Bukkit nên adapter không cần import API riêng của LuckPerms.
- `hookforge-paper`: lệnh, config, báo cáo và vòng đời plugin.

Để thêm hệ thống mới, tạo một module phụ thuộc `hookforge-api`, thực thi interface phù hợp, thêm dependency vào `hookforge-paper`, rồi viết kiểm thử với trường hợp plugin đích không được cài / bị tắt / service không có.

## Giới hạn MVP

- Chưa có GUI quản trị, web dashboard, registry tải adapter trực tuyến hay cache chung.
- Adapter vùng bảo vệ, chat, items, quests và database chưa được triển khai.
- `permissions().has(...)` sử dụng `Player#hasPermission`; nó không thao tác trực tiếp vào nhóm/quyền của người chơi.
- Không tải mã adapter từ Internet. Chỉ dùng jar từ nguồn tin cậy.
- Kiểm chứng tương thích cuối cùng bằng cách build trên môi trường có Maven và chạy trên server thử nghiệm trước khi cài vào server chính.

## License

MIT. Xem `LICENSE`.
