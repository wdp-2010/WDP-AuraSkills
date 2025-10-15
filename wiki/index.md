---
description: Welcome to the WDP AuraSkills wiki!
---

# WDP AuraSkills

**WDP AuraSkills** is a custom rework of AuraSkills (formerly **Aurelium Skills**), enhanced specifically for the **WDPServer** community at `play.wdpserver.com`. This version adds [skills](skills/), [stats](stats/), [abilities](abilities.md), and a revolutionary **[SkillCoins economy system](skillcoins.md)** that transforms progression into a dynamic marketplace.

This is a fork of the original AuraSkills by Archy-X, developed for the Spigot and Paper server platforms. While maintaining full compatibility with the core plugin features, this rework introduces a complete economy system with an integrated shop, player-to-player transfers, and Vault compatibility.

This wiki contains documentation on how to set up, configure, and use both the core AuraSkills features and the new SkillCoins economy. The source code is available on [GitHub](https://github.com/wdp-2010/WDP-AuraSkills).

## 💰 SkillCoins Economy (NEW!)

> Main article: [SkillCoins](skillcoins.md)

The biggest feature in this rework is the fully-integrated **SkillCoins economy system**. Players earn coins by leveling skills and can spend them in an interactive shop to buy levels, XP, abilities, and items.

Key features:
- **Earn coins** by leveling any skill with exponential scaling
- **Shop system** (`/shop`) with multiple categories
- **Player transfers** (`/pay`) for trading between players
- **Admin commands** for economy management
- **Vault integration** for compatibility with other plugins

See the complete [SkillCoins guide](skillcoins.md) for details on earning, spending, and managing the economy.

## Overview

Players level up [skills](skills/) by gaining skill XP through general Minecraft tasks, such as Farming, Mining, Fighting, or Enchanting. Increasing levels for each skill gives the player [stat buffs](stats/), unlocks and levels up [passive](abilities.md) and [mana abilities](mana-abilities.md), **earns [SkillCoins](skillcoins.md)**, and other customizable [rewards](rewards.md). 

Using `/skills`, players can view all the relevant information about skills and gameplay in fully-configurable inventory GUI [menus](menus.md). The new `/shop` command opens an interactive marketplace where SkillCoins can be spent on skill levels, XP boosts, custom abilities, and rare items.

Certain Fishing and Excavation abilities drop custom loot, which can be customized and extended to other skills through [loot tables](loot.md). Players can compete with each other through leaderboards and rankings, and now also trade SkillCoins using the `/pay` command.

Custom items can also be created that give [stat modifiers](stats/stat-modifiers.md) when held or worn, [skill requirements](skills/item-requirements.md) to use, and [XP multipliers](skills/xp-multipliers.md#item-and-armor-multipliers). Numerous [commands](commands.md) and [permissions](permissions.md) allow server admins to manage players, control access to features, and manage the economy.

## Skills

> Main article: [Skills](skills/)

There are 11 default skills included in AuraSkills which level up as players gain skill XP through various XP sources. By default, each skill has two [stats](stats/) that increase every 1 or 2 skill levels. Most skills also have 5 passive [abilities](abilities.md) that unlock the at first five levels and level up every 5 skill levels. Some skills have a [mana ability](mana-abilities.md), which is a special ability that must be activated by the player, costs mana, and has a cooldown.

The 11 skills are Farming, Foraging, Mining, Fishing, Excavation, Archery, Defense, Fighting, Agility, Enchanting, and Alchemy. The existing 15 skills from Beta can be added back by loading the legacy preset.

Skills can be viewed using `/skills` or by using the command for an individual skill, such as `/farming`, `/mining`, etc.

The `skills.yml` file is where skills are configured, including enabling/disabling skills, changing max levels and other skill-related options, as well as which abilities and mana abilities they have.

## Configuration

There are multiple configuration files in the `plugins/AuraSkills` directory that are used to configure the plugin.

### Main Config

> Main article: [Main Config](main-config/)

The main `config.yml` file is used for general or miscellaneous config options related to storage/database, external plugin hooks, languages, action bar, boss bar, worlds/regions, modifiers, requirements, **SkillCoins economy settings**, and more.

### SkillCoins & Shop Config

> Main article: [SkillCoins#Configuration](skillcoins.md#for-admins)

The `shop_config.yml` file controls all aspects of the SkillCoins economy including:

* Level purchase costs and per-skill pricing
* Level-up reward amounts and scaling
* Sellable items with prices and cooldowns
* Buyable items in the market
* Custom abilities with costs and requirements

### Skills

> Main article: [Skills#Configuration](skills/#configuration)

The `skills.yml` file is used to configure skills, including:

* Disabling/enabling skills
* Changing the max level of skills
* Removing or switching the abilities of skills
* Removing or switching the mana ability of skills
* Other skill-related config options

### Rewards

> Main article: [Rewards](rewards.md)

The `rewards/` folder contains the rewards files for each skill. The name of the file is the name of the skill it corresponds to. Rewards are given when a player levels up a skill and can be used to level stats, execute commands, grant permissions, give items, or deposite money (Vault). The `global.yml` is used to add rewards that apply to all skills (not a combined level, just saves time from adding same rewards to each file).

::: info
Removing or changing the default stats granted by a skill are done in the [rewards](rewards.md) file.
:::

### Loot

> Main article: [Loot](loot.md)

The `loot/` folder contains loot tables for certain skills. By default, Fishing and Excavation include loot tables that are dropped by certain abilities in those skills. Custom loot tables can be added for Mining or Foraging by creating a file of the skill name (mining.yml or foraging.yml).

### Menus

> Main article: [Menus](menus.md)

The `menus/` folder contains the menu files used to change the appearance of the plugin's GUI menus such as `/skills`. Almost anything related to a menu's appearance can be customized, including adding new items or adding actions to execute when clicking an item.

## API

> Main article: [API](api.md)

AuraSkills provides an extensive API that allows developers to hook into the plugin and interact with users, listen to events, and add custom content.
