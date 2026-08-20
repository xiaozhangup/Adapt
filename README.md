# Adapt

[![image](https://github.com/VolmitSoftware/Adapt/raw/main/storepage/adapt-tc.png)](https://github.com/VolmitSoftware/Adapt/wiki/Why-did-you-click)

## 修改声明如下

本项目仅针对 HAPPYLAND AssiahLand 服务器的需求进行修改，可能不能直接在其他服务器运行
目前已转变为 HARD-FORK 项目，和原版的差异只会越来越大
当前唯一支持的运行与构建环境为 Octopus/Paper 26.2 和 Java 25。

### 修改内容如下：

```
   - 移除原版的 Redis 同步机制, 改用数据库记录时间戳来进行同步锁避免同步问题
   - 新增了一个精简的技能面板，书架默认打开精简面板
   - 移除了自带的保护内容，新增了我们自己的保护插件兼容
   - 修改了技能的配色和图标，更加统一和美观
   - 部分技能的代码进行了修改，提高兼容性和性能
   - 线程执行相关部分被大量修改，减少占用
   - 修复了原版的各种异常占用的问题，优化了部分地方的实现
   - 修复了因为使用 Spigot 服务端构建导致在 Paper 上遇到的问题
   - 移除了我们不需要的版本的支持，提高构建速度
   - 更多详情见提交记录
```

## Overview

[![gitlocalized ](https://gitlocalize.com/repo/8085/whole_project/badge.svg)](https://gitlocalize.com/repo/8085/whole_project?utm_source=badge)

_Adapt is a drag and drop solution for balanced passive abilities and skills that players can enjoy on a server._

### Description

Adapt Abilities are all accessible in the in-game GUI by right-clicking any bookshelf **face**, providing a more
user-friendly experience to a "skills" system. Most servers aim to enhance the quality of the "vanilla" experience.
However, most skill-based plugins offer powers, game-breaking systems, and are riddled with bugs. That's where Adapt
comes in, being lightweight on the server and providing mere quality-of-life enhancements to a user's experience.

Below is a **WIP** list of features _(and descriptions)_ that I'll fill in when I can. But this should give you a good
idea of the roadmap for this plugin! Keep in mind that this is all WIP, can change at any time, and all of these
features can be configured/disabled!

The master branch is for the latest version of Minecraft.

### Language and Localization

Do you know a language other than English? Do you want to play a big part in Adapt's localization into different
languages? Join the [Discord](https://discord.gg/volmit) and let us know or visit
the [gitlocalize repository](https://gitlocalize.com/repo/8085) to help remotely with language localizations!

If you don't see a language you can easily add it, or let us know here in discussions! We take this on an honor system,
so please submit a translation key only if you are confident in the language, and they will be verified.

# [Support](https://discord.gg/volmit) **|** [Documentation](https://docs.volmit.com/adapt/)

## Building

### Download .jar release

**Consider supporting our development by buying Adapt**
On [SpigotMC](https://www.spigotmc.org/resources/adapt-leveling-skills-and-abilities.103790/)! We work hard to make
Adapt
the best it can be for everyone.

### Build your own .jar

Install JDK 25, select it as Gradle's JVM, then run:

```shell
./gradlew adapt
```

The deployable jar is written to `build/Adapt-<version>.jar`.


<details>
<summary> SKILLS </summary>

_The skills listed below are the fundamentals that we want to implement. However, please feel free to make an issue
request for any ideas or additional abilities that you would like to see in Adapt. Keep in mind that simpler ideas are
preferred, but complex ones are welcome too!_

## Agility:

- [ ] Slide?
- [X] Super jump (Allows a Crouch jump to launch yourself up to 5 blocks High)
- [X] Wall jump (Jump on walls)
- [X] Wind-Up (Sprint and go faster)
- [X] Armor-Up (Sprint and get more armor)(you need to have it equipped)
- [ ] Running start, Sprint = Jump boost
- [ ] Climb WOod

## Architect:

- [X] Temporary blocks (Crouch off a ledge)
- [X] BuildersWand (Small) (You can place up to 16 blocks at once)
- [ ] TypeReplace Blocks
- [X] DontBreakGlass (Passive Silk-Touch for Glass only)
- [ ] Forced Leaf Decay

## Axe:

- [ ] Tomahawk Throw
- [X] Drop to inventory
- [X] Axe Ground-Smash
- [X] Axe TreeFeller
- [ ] StripLogger (Sticks got from stripping)
- [ ] Speedy/Hasty Axe
- [ ] Wood Dupe?

## Brewing:

- [ ] Chance not to consume potion
- [ ] Chance to refund ingredients
- [X] Lingering Potions (Crafted potions last longer)
- [X] Splash Range Increase (Chance to increase Range)

## Crafting:

- [X] Xp for crafting
- [ ] Chance for Extras
- [ ] offhand autocrafting
- [X] Deconstruction Table (De-craft to basics)

## Discovery:

- [ ] Tiny Potato
- [ ] Armored Elytras
- [X] Worldly Armor
- [X] Passive XP
- [ ] Villager Attitude
- [X] Xp Damage Mitigation

## Enchanting:

- [X] XP Refund
- [X] Lapis Refund (Chance per enchant to give Lapis)
- [X] In-Inventory Enchanting (Books to Items)
- [ ] Xp for making Bookshelf/Book/Table
- [ ] Better Enchant Levels

## Excavation**:

- [ ] Dirt/Grass does not consume Durability
- [X] Haste while digging
- [X] Drop to inventory
- [X] MultiTool (Merge multiple tools into one)

## Herbalism:

- [X] Hunger Shield (up to 50% less hunger consumption)
- [X] Drop to inventory
- [X] Replanted (replant items by right-clicking)
- [ ] Harvest Dupes
- [X] Food feeds more
- [X] Herbalist Luck (breaking things can give you things)
- [X] Herbalist's Myconid (craftable Mycelia)
- [ ] Instant Food Consumption (Cooldown)
- [ ] Xp Gain
- [X] Faster Grow Aura

## Hunter:

- [X] Adrenaline (more damage lower the health)
- [X] Drop to inventory
- [X] Regen while in combat -> massive loss in hunger
- [X] Resistance in combat -> massive loss in hunger
- [X] Speed while in combat -> massive loss in hunger
- [X] JumpBoost while in combat -> massive loss in hunger
- [X] Luck while in combat -> massive loss in hunger
- [X] Invisibility while in combat -> massive loss in hunger
- [ ] Prevent the first damage proc

## Nether:

- [X] Wither Resist (Resistance to wither)
- [X] Wither Skull Throw (Pvsshhh)
- [ ] Soul Speed
- [ ] Nether Tools Apply Wither
- [ ] Nearby Withering applies regen

## Pickaxe:

- [X] Chisel ores (more ore, less durability)
- [X] Vein-miner (Vein-miner)
- [ ] Locate Nearest Ore:
- [ ] HammerMiner -> more duration cost
- [X] Auto-smelt % chance
- [X] Drop to inventory
- [ ] Chance not to eat Durability

## Ranged**:

- [X] Ranged Arrow Recovery (On hit, chance to refund)
- [X] Ranged Force (More dps at range)
- [X] Lunge SHot (Lunging will do damage)
- [X] Piercing Shot (Pierce through enemies)

## Rift:

- [X] Remote Container Access (Remote Container Access)
- [X] Short-Ranged "blink" (teleport)
- [X] No-Place Enderchest (like /ec )
- [X] Rift Recall (Teleport to a location)
- [X] Resilience based on Ender Artifact Used (blink = 10% Enderperal = 25% etc)

## Seaborn:

- [X] WaterBreathing
- [X] Passive Speed bonus while swimming
- [ ] Night vision underwater
- [ ] Passive Fish?
- [ ] Water Refiles Hunger/regen

## Stealth:

- [X] Snatching (close-range item Vacuum)
- [X] Sneak-Speed (Destroy FOV in a single button press)
- [X] Ghost Armor (Armor passively that grown on you, but only works for 1 hit)
- [X] StealthSight
- [ ] Sneak Attack

## Swords:

- [X] Machete (chopping blocks down)
- [ ] Throwing Knife
- [ ] Bleed Damage
- [ ] More damage to Non-Armored Enemies
- [ ] Turrets, Deploy Swords, that fling to a target

## Taming:

- [X] Tame Health Boost (Tames have more health)
- [X] Tame Damage Boost (Tames do more DPS)
- [X] Tame Health Regen (Tames have passive regen)
- [ ] Tamed Vampirism  (Familiar)

## Unarmed:

- [X] Unarmed Power (Make unarmed Viable)
- [X] Sucker Punch (One PunCh!)
- [ ] One-Punch man?
- [X] Glass Cannon (Less Armor = More damage to / from you)
- [ ] Remote Grab?
- [ ] Increased Boss Damage
- [ ] Passive Strength while unarmed

## Chronos: _(Unimplemented)_

- [ ] Chronos Slowdown (Passive Slowdown for entities in the world near you)
- [ ] Chronos Speed (Passive Speed for entities in the world near you)

## TragOul: _(Unimplemented)_

- [ ] Blood Mechanich and hurt yourself to get X

</details>

## Credits

Helping out in any way you can is appreciated, and you will be listed here for your contributions :)
<details>
<summary> Language </summary>

* [NextdoorPsycho](https://github.com/NextdoorPsycho): English Translation
* [Nowhere (Armin231)](https://github.com/Armin231): German Translation

</details>
<details>
<summary> Code </summary>

* [Vatuu](https://github.com/Vatuu)
* [Cyberpwn](https://github.com/cyberpwnn)
* [NextdoorPsycho](https://github.com/NextdoorPsycho)

</details>
