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
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
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
import java.util.concurrent.TimeUnit;

public class BungeeChatDiscord extends Plugin implements Listener {

    private JDA jda;
    private Configuration config;

    // Для анти-спаму: UUID -> Останнє повідомлення + Час
    private final Map<UUID, SpamData> spamFilter = new HashMap<>();

    @Override
    public void onEnable() {
        loadConfig();
        startBot();
        getProxy().getPluginManager().registerListener(this, this);

        getLogger().info("§b");
        getLogger().info("§b  ____   ____ ____  ");
        getLogger().info("§b | __ ) / ___|  _ \\ ");
        getLogger().info("§b |  _ \\| |   | | | |   §eBCD v1.5.2R");
        getLogger().info("§b | |_) | |___| |_| |   §7DISCORD+MINECRAFT CHAT");
        getLogger().info("§b |____/ \\____|____/    §aENABLED SUCCESSFULLY");
        getLogger().info("§b");
        getLogger().info("§e  >> Developer: §fATRCORE");
    }

    @Override
    public void onDisable() {
        getLogger().info("§c[BCD] §eShutting down...");
        if (jda != null) jda.shutdown();
        getLogger().info("§c[BCD] §4DISABLED.");
    }

    // --- ПОДІЇ ВХОДУ / ВИХОДУ ---

    @EventHandler
    public void onJoin(PostLoginEvent e) {
        if (!config.getBoolean("join_message.enabled")) return;
        sendToDiscord(config.getString("join_message.text"), e.getPlayer(), null);
    }

    @EventHandler
    public void onLeave(PlayerDisconnectEvent e) {
        if (!config.getBoolean("leave_message.enabled")) return;
        sendToDiscord(config.getString("leave_message.text"), e.getPlayer(), null);
    }

    // --- MINECRAFT -> DISCORD ---
    @EventHandler
    public void onMinecraftChat(ChatEvent e) {
        if (e.isCommand()) return;
        if (!(e.getSender() instanceof ProxiedPlayer)) return;

        ProxiedPlayer player = (ProxiedPlayer) e.getSender();
        String message = e.getMessage();

        // --- ANTI-SPAM LOGIC ---
        if (isSpamming(player.getUniqueId(), message)) {
            // Якщо спамить - просто не відправляємо в ДС (return), але в грі повідомлення пройде
            return;
        }

        sendToDiscord(config.getString("minecraft_to_discord_format"), player, message);
    }

    // Допоміжний метод відправки
    private void sendToDiscord(String format, ProxiedPlayer player, String message) {
        if (jda == null) return;
        TextChannel channel = jda.getTextChannelById(config.getString("channel_id"));
        if (channel == null) return;

        // 1. Отримуємо аліас сервера
        String rawServer = (player.getServer() != null) ? player.getServer().getInfo().getName() : "HUB";
        String serverAlias = config.getSection("server_aliases").getString(rawServer, rawServer);

        // 2. Отримуємо префікс LuckPerms
        String prefix = "";
        try {
            if (getProxy().getPluginManager().getPlugin("LuckPerms") != null) {
                LuckPerms lp = LuckPermsProvider.get();
                User user = lp.getUserManager().getUser(player.getUniqueId());
                if (user != null) {
                    String metaPrefix = user.getCachedData().getMetaData().getPrefix();
                    if (metaPrefix != null) prefix = metaPrefix;
                }
            }
        } catch (Exception ignored) {} // Щоб не крашнуло, якщо LP немає

        // 3. Формуємо повідомлення
        // Видаляємо кольорові коди для Діскорда, щоб не було сміття типу &4
        String cleanPrefix = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', prefix));
        String finalMsg = format
                .replace("%server%", serverAlias)
                .replace("%player%", player.getName())
                .replace("%prefix%", cleanPrefix)
                .replace("%message%", (message != null) ? message : "");

        channel.sendMessage(finalMsg).queue();
    }

    // --- DISCORD -> MINECRAFT ---
    private class DiscordListener extends ListenerAdapter {
        @Override
        public void onMessageReceived(MessageReceivedEvent event) {
            if (event.getAuthor().isBot()) return;
            if (!event.getChannel().getId().equals(config.getString("channel_id"))) return;

            Member member = event.getMember();
            String user = event.getAuthor().getEffectiveName();
            String message = event.getMessage().getContentDisplay();

            // 1. Отримуємо роль і перевіряємо аліас
            String roleName = "Player";
            String roleAlias = "&7[Player]"; // Дефолт

            if (member != null && !member.getRoles().isEmpty()) {
                roleName = member.getRoles().get(0).getName();
                // Шукаємо аліас у конфігу. Якщо немає - беремо назву ролі
                roleAlias = config.getSection("role_aliases").getString(roleName, roleName);
            }

            // 2. Форматуємо
            String format = config.getString("discord_to_minecraft_format", "&b[Discord] %role_alias% &f%user%: &f%message%");
            String finalMsg = ChatColor.translateAlternateColorCodes('&', format
                    .replace("%user%", user)
                    .replace("%message%", message)
                    .replace("%role_alias%", roleAlias)); // Вставляємо вже готовий аліас (напр. &4[Власник])

            getProxy().broadcast(new TextComponent(finalMsg));
        }
    }

    // --- ЛОГІКА АНТИ-СПАМУ ---
    private boolean isSpamming(UUID uuid, String currentMsg) {
        if (!config.getBoolean("anti_spam.enabled")) return false;

        long now = System.currentTimeMillis();
        int maxRepeats = config.getInt("anti_spam.max_repeats", 3);
        int timeWindow = config.getInt("anti_spam.time_window_ms", 2000);

        SpamData data = spamFilter.getOrDefault(uuid, new SpamData("", 0, 0));

        // Якщо повідомлення таке саме і пройшло мало часу
        if (data.lastMessage.equalsIgnoreCase(currentMsg) && (now - data.lastTime < timeWindow)) {
            data.count++;
            data.lastTime = now;
            spamFilter.put(uuid, data);

            // Якщо перевищив ліміт - це спам
            return data.count > maxRepeats;
        }

        // Якщо повідомлення нове або час пройшов - скидаємо лічильник
        spamFilter.put(uuid, new SpamData(currentMsg, 1, now));
        return false;
    }

    // Клас для збереження даних про спам
    private static class SpamData {
        String lastMessage;
        int count;
        long lastTime;

        SpamData(String msg, int c, long t) {
            this.lastMessage = msg;
            this.count = c;
            this.lastTime = t;
        }
    }

    // --- КОНФІГ ---
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

    private void startBot() {
        try {
            String token = config.getString("token");
            if (token == null || token.contains("ЗМІНИ_МЕНЕ")) return;

            // Статус
            String status = config.getString("bot_status");
            String type = config.getString("activity_type", "PLAYING").toUpperCase();
            Activity act = switch (type) {
                case "WATCHING" -> Activity.watching(status);
                case "LISTENING" -> Activity.listening(status);
                case "COMPETING" -> Activity.competing(status);
                default -> Activity.playing(status);
            };

            jda = JDABuilder.createDefault(token)
                    .enableIntents(GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MEMBERS)
                    .setActivity(act)
                    .addEventListeners(new DiscordListener())
                    .build();
            jda.awaitReady();
        } catch (Exception e) {
            getLogger().severe("Bot Error: " + e.getMessage());
        }
    }
}