# GlasVoipMod

Java agent that patches Project Zomboid's VOIP distance behavior for
the *Pour qui sonne le glas* server, letting players self-adjust their
voice range (whisper/talk/shout) for roleplay.

## Current status: bytecode-dump spike

This repo currently proves the toolchain, not the real feature. The
agent attaches to the game, intercepts the loading of
`zombie.core.raknet.VoiceManager`, and dumps its real bytecode
(disassembled via ASM) to `~/glasvoipmod-voicemanager-dump.txt` --
nothing about VOIP behavior is changed yet.

See `docs/superpowers/plans/2026-07-29-bytecode-patch-spike.md` for
the full plan, and the `GlasLauncher` repo's
`docs/superpowers/specs/2026-07-29-voip-mod-recherche-technique-design.md`
for the research this is based on.

## Build

Requires JDK 17.

```bash
./gradlew clean jar
```

Produces `build/libs/GlasVoipMod-0.1.0-spike.jar`.

## Manual verification

1. Build the jar (see above).
2. Add this Steam launch option to Project Zomboid (Steam > right-click
   Project Zomboid > Properties > General > Launch Options):

   ```
   -javaagent:/absolute/path/to/GlasVoipMod-0.1.0-spike.jar --
   ```

   (the trailing `--` matches the convention already used for
   ZombieBuddy-style agents in this project, keeping the game's own
   arguments intact)
3. Launch the game normally.
4. Check the game's own console/log output for the line:
   `[GlasVoipMod spike] agent attached, watching for zombie/core/raknet/VoiceManager -- ...`
5. Join a multiplayer server (or start a session where `VoiceManager`
   gets loaded -- it's a client-side class, loaded once voice chat
   initializes).
6. Check `~/glasvoipmod-voicemanager-dump.txt` was created and
   contains a full disassembly, including the `UpdateVMClient` method.

If the dump file never appears, check the console for exceptions from
`VoiceManagerTransformer` -- most likely cause is the class name filter
(`zombie/core/raknet/VoiceManager`) not matching if a future game
update renamed or relocated the class.
