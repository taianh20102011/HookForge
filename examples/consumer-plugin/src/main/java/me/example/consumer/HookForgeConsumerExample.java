package me.example.consumer;

import dev.hookforge.api.HookForgeAPI;
import dev.hookforge.api.result.HookResult;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.CommandExecutor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class HookForgeConsumerExample extends JavaPlugin implements CommandExecutor {
    private HookForgeAPI hookForge;

    @Override public void onEnable() {
        if (getCommand("hfdemo") != null) getCommand("hfdemo").setExecutor(this);
        hookForge = getServer().getServicesManager().load(HookForgeAPI.class);
        if (hookForge == null) {
            getLogger().severe("HookForge API service not found. Ensure depend: [HookForge] and scope provided.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        getLogger().info("Connected to HookForge API " + hookForge.version());
    }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Run this command in-game.");
            return true;
        }
        HookResult<Double> result = hookForge.economy().balance(player);
        if (result.isSuccess()) player.sendMessage("Your balance: " + result.value());
        else player.sendMessage("HookForge could not read balance: " + result.message());
        return true;
    }
}
