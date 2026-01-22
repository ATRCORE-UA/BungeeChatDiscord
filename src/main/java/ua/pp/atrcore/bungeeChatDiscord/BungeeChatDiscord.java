package ua.pp.atrcore;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import net.md_5.bungee.event.EventHandler;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BungeeChatDiscord extends Plugin implements Listener {

    private JDA jda;
    private Configuration config;
    private final Map<UUID, SpamData> spamFilter = new HashMap<>();

    @Override
    public void onEnable() {
        // 1. Завантаження
        loadConfig();
        startBot();

        // 2. Реєстрація подій і команд
        getProxy().getPluginManager().registerListener(this, this);
        getProxy().getPluginManager().registerCommand(this, new BCDCommand("bcd"));

        // 3. Красивий логотип
        getLogger().info("§b");
        getLogger().info("§b  ____   ____ ____  ");
        getLogger().info("§b | __ ) / ___|  _ \\   §eBCD v" + getDescription().getVersion());
        getLogger().info("§b |  _ \\| |   | | | |   §7Pro Edition by ATRCORE");
        getLogger().info("§b |____/ \\____|____/    §aRunning on BungeeCord");
        getLogger().info("§b");
    }

    @Override
    public void onDisable() {
        if (jda != null) {
            jda.shutdown();
        }
        getLogger().info("§c[BCD] Plugin disabled.");
    }

    // === КОМАНДА /BCD RELOAD ===
    public class BCDCommand extends Command {
        public BCDCommand(String name) {
            super(name, "bcd.admin", "bungeechatdiscord");
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                if (jda != null) jda.shutdown(); // Вимикаємо старого бота
                loadConfig(); // Вантажимо новий конфіг
                startBot(); // Запускаємо нового
                sender.sendMessage(new TextComponent(colorize(config.getString("messages.reload_success"))));
            } else {
                sender.sendMessage(new TextComponent(colorize(config.getString("messages.unknown_command"))));
            }
        }
    }

    // === EVENTS ===

    @EventHandler
    public void onJoin(PostLoginEvent e) {
        if (!config.getBoolean("messages.join.enabled")) return;
        String msg = config.getString("messages.join.text").replace("%player%", e.getPlayer().getName());
        sendToDiscordSimple(msg);
    }

    @EventHandler
    public void onLeave(PlayerDisconnectEvent e) {
        if (!config.getBoolean("messages.leave.enabled")) return;
        String msg = config.getString("messages.leave.text").replace("%player%", e.getPlayer().getName());
        sendToDiscordSimple(msg);
    }

    @EventHandler
    public void onMinecraftChat(ChatEvent e) {
        if (e.isCommand() || !(e.getSender() instanceof ProxiedPlayer)) return;

        ProxiedPlayer player = (ProxiedPlayer) e.getSender();
        String message = e.getMessage();

        if (isSpamming(player.getUniqueId(), message)) {
            player.sendMessage(new TextComponent(colorize(config.getString("anti_spam.warning_message"))));
            return;
        }

        sendToDiscordChat(player, message);
    }

    // === DISCORD LISTENER ===
    private class DiscordListener extends ListenerAdapter {
        @Override
        public void onMessageReceived(MessageReceivedEvent event) {
            if (event.getAuthor().isBot()) return;
            if (!event.getChannel().getId().equals(config.getString("channel_id"))) return;

            String roleAlias = config.getString("roles.default", "&7[Player]");
            Member member = event.getMember();

            if (member != null && !member.getRoles().isEmpty()) {
                String roleName = member.getRoles().get(0).getName();
                roleAlias = config.getSection("roles").getString(roleName, roleName);
            }

            String format = config.getString("formats.discord_to_minecraft");
            String msg = format
                    .replace("%user%", event.getAuthor().getEffectiveName())
                    .replace("%message%", event.getMessage().getContentDisplay())
                    .replace("%role_alias%", roleAlias);

            getProxy().broadcast(new TextComponent(colorize(msg)));
        }
    }

    // === HELPERS ===

    private void sendToDiscordSimple(String msg) {
        if (jda != null) {
            TextChannel ch = jda.getTextChannelById(config.getString("channel_id"));
            if (ch != null) ch.sendMessage(msg).queue();
        }
    }

    private void sendToDiscordChat(ProxiedPlayer player, String message) {
        if (jda == null) return;
        TextChannel channel = jda.getTextChannelById(config.getString("channel_id"));
        if (channel == null) return;

        String serverName = player.getServer() != null ? player.getServer().getInfo().getName() : "Unknown";
        String serverAlias = config.getSection("aliases").getString(serverName, serverName);

        String prefix = "";
        try {
            if (getProxy().getPluginManager().getPlugin("LuckPerms") != null) {
                User user = LuckPermsProvider.get().getUserManager().getUser(player.getUniqueId());
                if (user != null && user.getCachedData().getMetaData().getPrefix() != null) {
                    prefix = user.getCachedData().getMetaData().getPrefix();
                }
            }
        } catch (Throwable ignored) {}

        String cleanPrefix = ChatColor.stripColor(colorize(prefix));
        String format = config.getString("formats.minecraft_to_discord");

        String finalMsg = format
                .replace("%server%", serverAlias)
                .replace("%player%", player.getName())
                .replace("%prefix%", cleanPrefix)
                .replace("%message%", message);

        channel.sendMessage(finalMsg).queue();
    }

    // Hex Color Support (&#RRGGBB)
    private String colorize(String text) {
        Pattern pattern = Pattern.compile("&#[a-fA-F0-9]{6}");
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            String color = text.substring(matcher.start(), matcher.end());
            text = text.replace(color, ChatColor.of(color.substring(1)).toString());
            matcher = pattern.matcher(text);
        }
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    // Config Loader
    private void loadConfig() {
        try {
            if (!getDataFolder().exists()) getDataFolder().mkdir();
            File file = new File(getDataFolder(), "config.yml");
            if (!file.exists()) {
                try (InputStream in = getResourceAsStream("config.yml")) {
                    Files.copy(in, file.toPath());
                }
            }
            config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Bot Starter
    private void startBot() {
        try {
            String token = config.getString("token");
            if (token == null || token.contains("INSERT")) return;

            String statusText = config.getString("status.text", "Minecraft");
            String type = config.getString("status.type", "PLAYING").toUpperCase();

            Activity act = switch (type) {
                case "WATCHING" -> Activity.watching(statusText);
                case "LISTENING" -> Activity.listening(statusText);
                default -> Activity.playing(statusText);
            };

            jda = JDABuilder.createDefault(token)
                    .enableIntents(GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MEMBERS)
                    .setActivity(act)
                    .addEventListeners(new DiscordListener())
                    .build();
            jda.awaitReady();
        } catch (Exception e) {
            getLogger().severe("Failed to start Discord Bot: " + e.getMessage());
        }
    }

    // Anti-Spam
    private boolean isSpamming(UUID uuid, String msg) {
        if (!config.getBoolean("anti_spam.enabled")) return false;
        long now = System.currentTimeMillis();
        int max = config.getInt("anti_spam.max_repeats", 3);
        int time = config.getInt("anti_spam.time_window", 2000);

        SpamData data = spamFilter.getOrDefault(uuid, new SpamData("", 0, 0));
        if (data.msg.equalsIgnoreCase(msg) && (now - data.time < time)) {
            data.count++;
            data.time = now;
            spamFilter.put(uuid, data);
            return data.count > max;
        }
        spamFilter.put(uuid, new SpamData(msg, 1, now));
        return false;
    }

    private static class SpamData {
        String msg; int count; long time;
        SpamData(String m, int c, long t) { msg = m; count = c; time = t; }
    }
}