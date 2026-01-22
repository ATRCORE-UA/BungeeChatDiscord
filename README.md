<div align="center">

# 💎 BungeeChatDiscord (BCD)
### Advanced Chat Bridge for BungeeCord Networks

![Java](https://img.shields.io/badge/Java-17%2B-ed8b00?style=for-the-badge&logo=java&logoColor=white)
![Platform](https://img.shields.io/badge/Platform-BungeeCord-blue?style=for-the-badge)
![License](https://img.shields.io/badge/License-MIT-green?style=for-the-badge)
![Version](https://img.shields.io/badge/Version-1.5.2--RELEASE-purple?style=for-the-badge)

**Professional bi-directional chat synchronization between Minecraft (BungeeCord) and Discord.** *Developed by ATRCORE*

[Report Bug](https://github.com/ATRCORE-UA/BungeeChatDiscord/issues) · [Request Feature](https://github.com/ATRCORE-UA/BungeeChatDiscord/issues)

</div>

---

## ✨ Features

**BCD** is built for performance and customization. Unlike other bloatware, it does exactly what you need:

* 🔄 **Bi-Directional Sync:** Chat flows seamlessly from Minecraft to Discord and vice versa.
* 🔰 **LuckPerms Integration:** Displays player prefixes (Ranks) from Minecraft in Discord.
* 🎭 **Discord Roles:** Displays the user's top Discord role in Minecraft chat with custom aliases (e.g., `Owner` -> `&4[Admin]`).
* 🛡️ **Smart Anti-Spam:** Prevents Discord channel flooding without blocking in-game chat.
* 🌍 **Server Aliases:** Renames internal server names (e.g., `main` -> `SURVIVAL`) for a cleaner look.
* 🎨 **Full Color Support:** Supports legacy (`&a`) and Hex colors (`&#RRGGBB`).
* ⚙️ **Hot Reload:** Configure everything without restarting the proxy via `/bcd reload`.

## 📦 Installation

1.  Download the latest `.jar` from the [Releases](https://github.com/YOUR_USERNAME/BungeeChatDiscord/releases) page.
2.  Drop the file into your **BungeeCord** `plugins` folder.
3.  Restart your proxy.
4.  Open `plugins/BungeeChatDiscord/config.yml`.
5.  Paste your **Discord Bot Token** and **Channel ID**.
6.  Run `/bcd reload` or restart.

## ⚙️ Configuration

Everything is configurable in `config.yml`.

```yaml
# ==========================================
#         BungeeChatDiscord (BCD)
#           Created by ATRCORE
# ==========================================

# --- DISCORD SETTINGS ---
token: "INSERT_TOKEN_HERE"
channel_id: "000000000000000000"

# Bot Status Settings
# Type: PLAYING, WATCHING, LISTENING, COMPETING
status:
  enabled: true
  text: "ExoticLegacy Network"
  type: "PLAYING"

# --- FEATURES ---

# Server Aliases (Rename your servers for Discord)
# RealName: "Nice Name"
aliases:
  lobby: "HUB"
  main: "SURVIVAL"
  minigames: "GAMES"

# Role Aliases (Rename Discord roles for Minecraft chat)
# DiscordRole: "MinecraftFormat"
roles:
  "Owner": "&4[Owner]"
  "Admin": "&c[Admin]"
  "Moderator": "&9[Mod]"
  "VIP": "&6[VIP]"
  "default": "&7[Player]"

# Anti-Spam System
anti_spam:
  enabled: true
  max_repeats: 3        # How many identical messages allowed?
  time_window: 2000     # Time in ms (2 seconds)
  warning_message: "&cPlease do not spam!"

# --- FORMATTING & MESSAGES (LOCALIZATION) ---
# Supports standard color codes (&a) and Hex colors (&#RRGGBB)

formats:
  # Variables: %role_alias%, %user%, %message%
  discord_to_minecraft: "&b[Discord] %role_alias% &f%user%: &f%message%"
  
  # Variables: %server%, %prefix%, %player%, %message%
  minecraft_to_discord: "**[%server%]** %prefix% %player%: %message%"

messages:
  # Join/Leave Messages for Discord
  join:
    enabled: true
    text: "🟢 **%player%** joined the server!"
  leave:
    enabled: true
    text: "🔴 **%player%** left the server."
  
  # Plugin Messages (In-Game)
  reload_success: "&a[BCD] Configuration reloaded successfully!"
  no_permission: "&c[BCD] You do not have permission to use this command."
  unknown_command: "&c[BCD] Unknown command. Use /bcd reload"
