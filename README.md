# PiShock-Zap

## 🔞 Not intended for users below the age of 18!

This is a project that carries some element of personal risk and was developed
purely as an instrument of masochism. If you are below the age of majority or
are an employer looking for interesting personal projects, please leave and
pretend you never saw this.

## With that said

Hi, this is my first Fabric mod. I still suck at Gradle.

It connects PiShock devices to Minecraft, allowing them to react to ingame
damage, and has a comprehensive set of options to configure that experience.

In other words, it zaps you with a shock collar when you get hit.

## Notable features

- Works in multiplayer, even on non-modded servers
- Vibration/shock threshold
- Vibration-only mode
- Careful limit respecting with multiple layers of failsafe
- Multiple shocker support, with built-in patterns to choose which ones get
  activated for each shock
- Millisecond-precise duration settings
- Queues and combines damage events that occur in quick succession
- Low-latency usage via local serial API (advanced users only; requires PiShock
  or OpenShock hub to be connected directly to the computer running Minecraft)
- Wide range of supported Minecraft versions
- Third-party device support
  - Supports OpenShock (yes really!)
  - Webhook so that you can connect it to your own custom software
  - Extension points for other mods to add support for their own custom devices
- An API for other mod developers to use!

## Before you use

If you are struggling and need to switch it off ASAP, the fastest way is to
exit the game. On Windows, this is Alt+F4. Test it out before you use it.

There is also an in-game toggle hotkey which defaults to F12. Equally, configure
this and check that it is working before you use it.

By default, this mod starts with the actual API calls **disabled**, with the
expectation that you edit the settings to respect your personal limits and then
enable it manually.

## Requirements

- A supported Minecraft version (see the [Releases page][releases])
- Fabric or Quilt
- Fabric API
- Cloth Config
- Mod Menu (optional but strongly recommended; gives access to the settings
  screen)

## Installation

### Prism Launcher

This approach is recommended for users who are not familiar with modding.

1. Download the latest release from the [releases page][releases]. Make sure you
   get the one corresponding to the version of Minecraft you are using. (The
   number after the `+` in the version name is the version of Minecraft it
   supports.)
2. Download and install [Prism Launcher].
3. Open Prism Launcher and click "Add Instance".
4. Create a new custom instance
    * Choose a Minecraft version
    * Choose either Fabric or Quilt as the mod loader, at the latest version.
    * Click OK.
5. Right-click your newly-created instance and click "Edit".
6. Go to the "Mods" tab and click "Add file".
    * Choose the JAR file you downloaded in step 1.
7. Click "Download mods" and select the following mods: (these should all be
   close to the top of the list)
    * Fabric API
    * Cloth Config API
    * Mod Menu
8. Click "Review and Confirm" and then "OK".
9. Click "Launch" to start the game. You can launch the game in future just by
   double-clicking the instance in Prism Launcher.

### Manual

1. Prepare a Minecraft instance modded with Fabric or Quilt.
2. Download the latest release from the [releases page][releases].
3. Put the downloaded JAR file in your `.minecraft/mods` folder.
4. Download any missing required mods (see the Requirements section).
5. Start the game.

## Alternatives

There are several other mods that do similar things to PiShock-Zap. I will
compare them and then note some more specific quirks of each.

## Big comparison table


| Feature                         | PiShock-Zap        | [Raith's PiShock mod] | [pishock-mc]                         | [PiShockForMc]         | [Shockcraft]                  | [Minecraft Shock Collar]      | [The original Forge mod][original-forge-mod] |
|---------------------------------|--------------------|-----------------------|--------------------------------------|------------------------|-------------------------------|-------------------------------|----------------------------------------------|
| Minecraft versions              | 1.17.x - 26.1.x    | 1.21.x                | 1.21                                 | 1.19.x, 1.20.x, 1.21.x | 1.19.3, 1.19.4                | Wide range                    | 1.18.2                                       |
| Author                          | [ScoreUnder]       | [Raith]               | [PancakeTAS]                         | [ojaha065]             | [yanchan09]                   | [Hepno]                       | [DrasticLp]                                  |
| Mod loader                      | Fabric             | NeoForge              | Fabric                               | Forge                  | Fabric                        | Bukkit                        | Forge                                        |
| Still works in 2026\*           | :white_check_mark: | :white_check_mark:    | :white_check_mark: (Must use serial) | :x:                    | :x:                           | :x:                           | :x:                                          |
| Actively maintained             | :white_check_mark: | :white_check_mark:    | Archived 2025-03-15                  | Archived 2024-10-26    | Unmaintained since 2023-03-24 | Unmaintained since 2023-04-21 | Dead                                         |
| Client-side                     | :white_check_mark: | :white_check_mark:    | :white_check_mark:                   | :white_check_mark:     | :white_check_mark:            | :x:                           | :white_check_mark:                           |
| Singleplayer                    | :white_check_mark: | :white_check_mark:    | :white_check_mark:                   | :white_check_mark:     | :white_check_mark:            | :x:                           | :white_check_mark:                           |
| Multiplayer                     | :white_check_mark: | :white_check_mark:    | :white_check_mark:                   | :white_check_mark:     | :white_check_mark:            | :white_check_mark:            | :white_check_mark:                           |
| Works on vanilla servers        | :white_check_mark: | :white_check_mark:    | :x:                                  | :white_check_mark:     | :white_check_mark:            | :x:                           | :x:                                          |
| Low-latency local serial API    | :white_check_mark: | :x:                   | :white_check_mark:                   | :x:                    | :x:                           | :x:                           | :x:                                          |
| Multiple simultaneous shockers  | :white_check_mark: | :x:                   | :x:                                  | :x:                    | :x:                           | :x:                           | :x:                                          |
| Vibration support               | :white_check_mark: | :white_check_mark:    | :x:                                  | :white_check_mark:     | :x:                           | :x:                           | :x:                                          |
| Vibration/shock threshold       | :white_check_mark: | :x:                   | :x:                                  | :x:                    | :x:                           | :x:                           | :x:                                          |
| API connectivity checks         | Sorta              | :white_check_mark:    | :white_check_mark:                   | :white_check_mark:     | :x:                           | :x:                           | :x:                                          |
| Vibration test button           | :x:                | :white_check_mark:    | :x:                                  | :white_check_mark:     | :x:                           | :x:                           | :x:                                          |
| In-game quick toggle            | Via hotkey         | Via hotkey            | :x:                                  | :x:                    | Via command                   | :x:                           | :x:                                          |
| Damage curves                   | :white_check_mark: | :white_check_mark:    | :white_check_mark:                   | :white_check_mark:     | :x:                           | :x:                           | :white_check_mark:                           |
| Queued/combined damage events   | :white_check_mark: | :white_check_mark:    | :x:                                  | :white_check_mark:     | :x:                           | :x:                           | :x:                                          |
| Separate shock-on-death config  | :white_check_mark: | :white_check_mark:    | :white_check_mark:                   | :white_check_mark:     | :x:                           | :x:                           | :white_check_mark:                           |
| Millisecond-precise duration    | :white_check_mark: | :white_check_mark:    | :white_check_mark:                   | :x:                    | :x:                           | :white_check_mark:            | :x:                                          |
| Alternative/third-party devices | :white_check_mark: | :x:                   | :x:                                  | :x:                    | :x:                           | :x:                           | :x:                                          |
| Usable by other mods            | :white_check_mark: | :x:                   | :x:                                  | :x:                    | :x:                           | :x:                           | :x:                                          |
| Configuration method            | In-game settings   | In-game settings      | In-game settings                     | In-game settings       | Slash commands                | Configuration file            | In-game settings                             |
| Configurability                 | Control-freak      | Moderate              | Simple                               | Simple                 | Basic                         | Basic                         | Simple                                       |
| Known performance issues        | :ok:               | :ok:                  | :ok:                                 | :ok:                   | :ok:                          | :warning:                     | :warning:                                    |
| Known limit-exceeding bugs      | :ok:               | :ok:                  | :ok:                                 | :ok:                   | :ok:                          | :ok:                          | :warning: :bangbang:                         |
| Limit-respecting failsafes      | Multi-level        | Multi-level           | :x:                                  | Some                   | N/A                           | N/A                           | :x:                                          |
| Source code available           | :white_check_mark: | :white_check_mark:    | :white_check_mark:                   | :white_check_mark:     | :white_check_mark:            | :white_check_mark:            | :x:                                          |
| Unit tests                      | :white_check_mark: | :white_check_mark:    | :x:                                  | :x:                    | :x:                           | :x:                           | :question:                                   |

\* PiShock's Web V1 API got taken down without warning in early 2026, which
broke certain mods/integrations. All mods which were not updated to use other
PiShock APIs now will no longer work on any version of Minecraft.

### [Raith's PiShock mod]

This mod is a NeoForge rebuild of the original PiShock Forge mod, modernising
it, patching some known bugs, and bringing the feature set up to something more
competitive.

I would recommend this mod if you are using NeoForge.

### Unmaintained mods

#### [pishock-mc]

This is a Fabric mod which I have not yet had the chance to test in detail. It
fulfils a similar niche to PiShock-Zap.

It uses a different method of damage detection, which is accurate but requires
a server-side mod to be installed. This means it is not suitable for use on
vanilla servers.

### Unmaintained and broken mods

#### [PiShockForMc]

It has a hardcoded duration of 0.6 seconds for all shocks except the shock on
death.
It uses a different method of damage detection, which may result in slightly
different behaviour in edge cases. I am not sure if it is more or less reliable
than PiShock-Zap.

#### [Shockcraft]

This uses a slightly different method of damage detection, but also different
from PiShockForMc. Again, I am not sure which method is more reliable.

#### [Minecraft Shock Collar]

You will need to have control over the server to use this mod. It may cause
performance issues on the server side due to blocking HTTP requests being made
on the main thread.

It zaps the configured person when *any* player gets damaged, while most other
mods zap the configured person when the local player gets damaged. Because of
how it is implemented, it can only be configured to zap one person at a time per
server.

It supports millisecond-precise duration settings, but only on a technicality;
the duration is meant to be in seconds, but if you configure a duration higher
than 100, it will be interpreted by the PiShock API as milliseconds instead.

#### [The original Forge mod][original-forge-mod]

**Not recommended** due to known bugs and no clear path to updates.

This was originally distributed as a jar file on the PiShock discord, but has
since disappeared. I had a copy lying around and have mirrored it.

It can be used on multiplayer servers, but only if the server has the mod
installed. It may cause small client-side stutters when the player is damaged
due to performing blocking HTTP requests on the main thread.

[releases]: https://github.com/ScoreUnder/pishock-zap-fabric/releases
[Prism Launcher]: https://prismlauncher.org/
[Minecraft Shock Collar]: https://github.com/Hepno/MinecraftShockCollar
[original-forge-mod]: https://score.moe/a/ps/pishock-1.1.1.jar
[DrasticLp]: https://github.com/DrasticLp
[Shockcraft]: https://codeberg.org/yanchan09/shockcraft
[yanchan09]: https://codeberg.org/yanchan09
[PiShockForMc]: https://github.com/ojaha065/PiShockForMC
[ojaha065]: https://github.com/ojaha065
[Hepno]: https://github.com/Hepno
[ScoreUnder]: https://github.com/ScoreUnder
[Raith's PiShock mod]: https://jenkins.raith.one/
[Raith]: https://github.com/RaithSphere
[pishock-mc]: https://github.com/PancakeTAS/pishock-mc
[PancakeTAS]: https://github.com/PancakeTAS
