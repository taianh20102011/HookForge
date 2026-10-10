# HookForge API quick start

Dependency Maven của consumer:

```xml
<dependency>
  <groupId>dev.hookforge</groupId>
  <artifactId>hookforge-api</artifactId>
  <version>0.1.0</version>
  <scope>provided</scope>
</dependency>
```

Thêm repository PaperMC nếu Maven của bạn chưa tải được `paper-api` phụ thuộc của API và đặt `depend: [HookForge]` trong `plugin.yml`.

```java
HookForgeAPI api = getServer().getServicesManager().load(HookForgeAPI.class);
if (api == null) {
    getLogger().warning("HookForge not available");
    return;
}
HookResult<Double> balance = api.economy().balance(player);
if (balance.isSuccess()) {
    player.sendMessage("Balance: " + balance.value());
} else {
    getLogger().warning("Could not get balance: " + balance.message());
}
```

Các lệnh thay đổi tiền `deposit`/`withdraw` cần được gọi trên main thread, theo quy tắc an toàn của hầu hết economy provider Bukkit. Không truyền amount âm, NaN hoặc Infinity. `balance` và `has` trả về `HookResult` chứa mô tả thành công/thất bại.
