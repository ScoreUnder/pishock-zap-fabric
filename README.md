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

## Notable features

- Works in multiplayer, even on non-modded servers
- Vibration/shock threshold
- Vibration-only mode
- Careful limit respecting with multiple layers of failsafe
- Multiple share code support
- Millisecond-precise duration settings
- Queues and combines damage events that occur in quick succession
- Low-latency usage via local serial API (advanced users only; requires PiShock
  to be connected directly to the computer running Minecraft)

## Before you use

If you are struggling and need to switch it off ASAP, the fastest way is to
simply exit the game unceremoniously. On Windows, this is Alt+F4. Test it out
before you use it.

There is also an in-game toggle hotkey which defaults to F12. Equally, configure
this and check that it is working before you use it.

By default, this mod starts with the actual API calls **disabled**, with the
expectation that you edit the settings to respect your personal limits and then
enable it manually.

## Requirements

- Minecraft 1.21 (see the [Releases page][releases] or other branches for
  other versions)
- Fabric Loader 0.15.11
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
    * Choose a Minecraft version in the 1.21 series.
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

| Feature                        | PiShock-Zap            | [PiShockForMc]         | [Shockcraft]       | [Minecraft Shock Collar] | [Raith's PiShock mod]  | [The original Forge mod][original-forge-mod] | [pishock-mc]       |
|--------------------------------|------------------------|------------------------|--------------------|--------------------------|------------------------|----------------------------------------------|--------------------|
| Minecraft versions             | 1.18.2, 1.20.x, 1.21.x | 1.19.x, 1.20.x, 1.21.x | 1.19.3, 1.19.4     | Wide range               | 1.19.x, 1.20.x, 1.21.x | 1.18.2                                       | 1.21               |
| Author                         | [ScoreUnder]           | [ojaha065]             | [yanchan09]        | [Hepno]                  | [Raith]                | [DrasticLp]                                  | [PancakeTAS]       |
| Mod loader                     | Fabric                 | Forge                  | Fabric             | Bukkit                   | Forge, NeoForge        | Forge                                        | Fabric             |
| Client-side                    | :white_check_mark:     | :white_check_mark:     | :white_check_mark: | :x:                      | :white_check_mark:     | :white_check_mark:                           | :white_check_mark: |
| Singleplayer                   | :white_check_mark:     | :white_check_mark:     | :white_check_mark: | :x:                      | :white_check_mark:     | :white_check_mark:                           | :white_check_mark: |
| Multiplayer                    | :white_check_mark:     | :white_check_mark:     | :white_check_mark: | :white_check_mark:       | :white_check_mark:     | :white_check_mark:                           | :white_check_mark: |
| Works on vanilla servers       | :white_check_mark:     | :white_check_mark:     | :white_check_mark: | :x:                      | :white_check_mark:     | :x:                                          | :x:                |
| Low-latency local serial API   | :white_check_mark:     | :x:                    | :x:                | :x:                      | :x:                    | :x:                                          | :white_check_mark: |
| Multiple share codes           | :white_check_mark:     | :x:                    | :x:                | :x:                      | :x:                    | :x:                                          | :x:                |
| Vibration support              | :white_check_mark:     | :white_check_mark:     | :x:                | :x:                      | :white_check_mark:     | :x:                                          | :x:                |
| Vibration/shock threshold      | :white_check_mark:     | :x:                    | :x:                | :x:                      | :x:                    | :x:                                          | :x:                |
| API connectivity checks        | :x:                    | :white_check_mark:     | :x:                | :x:                      | :x:                    | :x:                                          | :white_check_mark: |
| Vibration test button          | :x:                    | :white_check_mark:     | :x:                | :x:                      | :x:                    | :x:                                          | :x:                |
| In-game quick toggle           | Via hotkey             | :x:                    | Via command        | :x:                      | :x:                    | :x:                                          | :x:                |
| Damage curves                  | :white_check_mark:     | :white_check_mark:     | :x:                | :x:                      | :white_check_mark:     | :white_check_mark:                           | :white_check_mark: |
| Queued/combined damage events  | :white_check_mark:     | :white_check_mark:     | :x:                | :x:                      | :x:                    | :x:                                          | :x:                |
| Separate shock-on-death config | :white_check_mark:     | :white_check_mark:     | :x:                | :x:                      | :white_check_mark:     | :white_check_mark:                           | :white_check_mark: |
| Millisecond-precise duration   | :white_check_mark:     | :x:                    | :x:                | :white_check_mark:       | :x:                    | :x:                                          | :white_check_mark: |
| Configuration method           | In-game settings       | In-game settings       | Slash commands     | Configuration file       | In-game settings       | In-game settings                             | In-game settings   |
| Configurability                | Control-freak          | Simple                 | Basic              | Basic                    | Simple                 | Simple                                       | Simple             |
| Known performance issues       | :ok:                   | :ok:                   | :ok:               | :warning:                | :ok:                   | :warning:                                    | :ok:               |
| Known limit-exceeding bugs     | :ok:                   | :ok:                   | :ok:               | :ok:                     | :warning: :bangbang:   | :warning: :bangbang:                         | :ok:               |
| Limit-respecting failsafes     | Multi-level            | Some                   | N/A                | N/A                      | :x:                    | :x:                                          | :x:                |
| Source code available          | :white_check_mark:     | :white_check_mark:     | :white_check_mark: | :white_check_mark:       | :white_check_mark:     | :x:                                          | :white_check_mark: |
| Unit tests                     | :white_check_mark:     | :x:                    | :x:                | :x:                      | :x:                    | :question:                                   | :x:                |

### [PiShockForMc]

It has a hardcoded duration of 0.6 seconds for all shocks except the shock on
death.
It uses a different method of damage detection, which may result in slightly
different behaviour in edge cases. I am not sure if it is more or less reliable
than PiShock-Zap.

### [Shockcraft]

This uses a slightly different method of damage detection, but also different
from PiShockForMc. Again, I am not sure which method is more reliable.

### [Minecraft Shock Collar]

You will need to have control over the server to use this mod. It may cause
performance issues on the server side due to blocking HTTP requests being made
on the main thread.

It zaps the configured person when *any* player gets damaged, while most other
mods zap the configured person when the local player gets damaged. Because of
how it is implemented, it can only be configured to zap one person at a time per
server.

It is much simpler than PiShock-Zap, which makes it a better starting point for
new developers to understand Minecraft and PiShock integration, or to modify it
to their needs.

It supports millisecond-precise duration settings, but only on a technicality;
the duration is meant to be in seconds, but if you configure a duration higher
than 100, it will be interpreted by the PiShock API as milliseconds instead.

### Raith's PiShock mod

(Neo)Forge mod which seems to be based on the original PiShock Forge mod(?), but
with some extra bugfixes and better version compatibility.

### [The original Forge mod][original-forge-mod]

**Not recommended** due to known bugs and no clear path to updates.

This was originally distributed as a jar file on the PiShock discord, but has
since disappeared. I had a copy lying around and have mirrored it.

It can be used on multiplayer servers, but only if the server has the mod
installed. It may cause small client-side stutters when the player is damaged
due to performing blocking HTTP requests on the main thread.

### [pishock-mc]

This is a relatively new Fabric mod which I have not yet had the chance to
test in detail. It fulfils a similar niche to PiShock-Zap.

It uses a different method of damage detection, which is accurate but requires
a server-side mod to be installed. This means it is not suitable for use on
vanilla servers.

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
