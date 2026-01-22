<div align="center">

# 💎 BungeeChatDiscord (BCD)
### Advanced Chat Bridge for BungeeCord Networks

![Java](https://img.shields.io/badge/Java-17%2B-ed8b00?style=for-the-badge&logo=java&logoColor=white)
![Platform](https://img.shields.io/badge/Platform-BungeeCord-blue?style=for-the-badge)
![License](https://img.shields.io/badge/License-MIT-green?style=for-the-badge)
![Version](https://img.shields.io/badge/Version-1.5.2--RELEASE-purple?style=for-the-badge)

**Professional bi-directional chat synchronization between Minecraft (BungeeCord) and Discord.** *Developed by ATRCORE*

[Report Bug](https://github.com/YOUR_USERNAME/BungeeChatDiscord/issues) · [Request Feature](https://github.com/YOUR_USERNAME/BungeeChatDiscord/issues)

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
# Discord Bot Token
token: "YOUR_TOKEN_HERE"
channel_id: "123456789012345678"

# Bot Status
status:
  text: "ExoticLegacy Network"
  type: "PLAYING"

# Rename your servers for Discord
aliases:
  lobby: "HUB"
  survival: "SURVIVAL"

# Rename Discord roles for Minecraft
roles:
  "Owner": "&4[Owner]"
  "Member": "&7[Player]"

# Anti-Spam Settings
anti_spam:
  enabled: true
  max_repeats: 3
  time_window: 2000
