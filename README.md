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

- Vibration/shock threshold
- Vibration-only mode
- More careful limit respecting than the original
- Multiple share code support
- Duration/intensity accumulation during backoff period
- Probably still has bugs, user beware

## Before you use

If you are struggling and need to switch it off ASAP, the fastest way is to
simply exit the game unceremoniously. On Windows, this is Alt+F4. Test it out
before you use it.

There is also an in-game toggle hotkey which defaults to F12. Equally, configure
this and check that it is working before you use it.

By default, this mod starts with the actual API calls **disabled**, with the
expectation that you edit the settings to respect your personal limits and then
enable it manually.

Duration accumulation without separate queueing of different intensities will
cause fractional durations to be sent, which may be incompatible with the
limits on some share codes, and may not function correctly on older devices.

## Requirements

- Minecraft 1.18.2
- Fabric Loader 0.14.21
- Cloth Config
- Mod Menu (optional but strongly recommended; gives access to the settings
  screen)
