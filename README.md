# GlasVoipMod

Companion mod for the *Pour qui sonne le glas* Project Zomboid roleplay
server, letting players self-adjust their own VOIP range for roleplay:
chuchoter (whisper), parler (talk), hurler (shout). It has two halves
living in this same repo:

- A **Lua mod** (`mods/GlasVoipMod/`) that lets a player pick their
  tier in-game and broadcasts the choice to every connected client.
- A **Java bytecode-patching agent** (`src/main/java/glas/voip/patch`
  and `glas/voip/spike`) that reads the tier the Lua mod chose and
  rewrites the game's own VOIP audio distance for that player
  accordingly.

## How it works

1. Press **B** (default) in-game to cycle through the three tiers. A
   speech bubble confirms the new tier, and a gold dot-ring briefly
   appears on the ground around you (visible only to yourself). The
   key is fully rebindable: Options -> Keybinding -> **Glas VOIP** ->
   **Cycle VOIP Tier** (`GlasVoipTierController.lua` registers this
   entry via `Events.OnGameBoot`; the category/label translations
   live in `media/lua/shared/Translate/{EN,FR}/UI_*.txt`).
2. The chosen tier is sent to the server (`GlasVoipTierController.lua`),
   which broadcasts it to every connected client
   (`GlasVoipTierRelay.lua`).
3. Every client (including yours) stores the broadcast in a global
   Lua table, `GlasVoipTiers[onlineId] = tier`
   (`GlasVoipTierSync.lua`).
4. The Java agent's `glas.voip.patch.LuaGlobalTierProvider` reads that
   exact table directly from the running game's Lua environment
   (`LuaManager.env`), `glas.voip.patch.TierDistanceCalculator` maps
   the tier to a min/max distance pair, and
   `glas.voip.patch.ReflectiveVoiceManagerDistanceWriter` writes those
   distances onto the game's own `VoiceManager` fields. The call chain
   is reached via bytecode injected by
   `glas.voip.spike.VoiceManagerTransformer` into
   `VoiceManager.UpdateVMClient()` (the game's per-frame audio loop),
   so the distance is refreshed continuously while voice chat is
   active. Tier 1 (talk) matches the vanilla
   `VoiceMinDistance`/`VoiceMaxDistance` defaults, so a player who
   never changes tier keeps today's exact behavior.

The agent's classes still live under a `glas.voip.spike` package (a
holdover from the project's original bytecode-dump spike) alongside
the newer `glas.voip.patch` package that does the real distance
patching; `VoiceManagerTransformer` also still writes a full bytecode
disassembly of `VoiceManager` to
`~/glasvoipmod-voicemanager-dump.txt` on every load, which is useful
for diagnosing a future game update renaming/relocating the class but
plays no role in the tier patch itself.

## Build (Java agent)

Requires JDK 17.

```bash
./gradlew clean jar
```

Produces `build/libs/GlasVoipMod-1.0.0.jar` (a self-contained
fat jar, since it's loaded via `-javaagent` before the game's own
classpath exists) — the filename tracks whatever `version` is
currently set to in `build.gradle.kts`.

## Local testing (before Workshop publishing)

1. Copy (or symlink) `mods/GlasVoipMod/` into your Project Zomboid user
   data folder's `mods/` directory (`~/Zomboid/mods/` on macOS/Linux,
   `%UserProfile%\Zomboid\mods\` on Windows).
2. Build the jar (see above) and add this Steam launch option to
   Project Zomboid (Steam > right-click Project Zomboid > Properties >
   General > Launch Options):

   ```
   -javaagent:/absolute/path/to/GlasVoipMod-1.0.0.jar --
   ```

   (the trailing `--` matches the convention already used for
   ZombieBuddy-style agents in this project, keeping the game's own
   arguments intact)
3. Enable "Glas VOIP" in the in-game Mods menu, start/join a game.
4. Check the game's own console/log output for the line:
   `[GlasVoipMod spike] agent attached, watching for zombie/core/raknet/VoiceManager -- ...`,
   followed by `[GlasVoipMod] injected tier patch into
   zombie/core/raknet/VoiceManager.UpdateVMClient()` once voice chat
   initializes.
5. Press **B** -- confirm the speech bubble and ground indicator both
   appear, and that the tier actually cycles (whisper -> talk -> shout
   -> whisper).
6. With a second player/account if available, confirm the other
   client also receives the broadcast (their tier choice should be
   visible to you too, once VoiceManager next reads the distance).

If the bytecode dump file never appears, or the injection log line is
missing, check the console for exceptions from
`VoiceManagerTransformer` -- most likely cause is the class name filter
(`zombie/core/raknet/VoiceManager`) not matching if a future game
update renamed or relocated the class.

## Keeping the two mod copies in sync

`mods/GlasVoipMod/` (used for local testing above) and
`Contents/mods/GlasVoipMod/` (the Steam Workshop upload copy) are two
separate, git-tracked copies of the same `mod.info` and `.lua` files --
they are not kept in sync automatically. After editing anything under
`mods/GlasVoipMod/`, re-copy it (and the images) into
`Contents/mods/GlasVoipMod/`/root `preview.png` before re-publishing to
the Workshop:

```bash
cp mods/GlasVoipMod/mod.info Contents/mods/GlasVoipMod/mod.info
cp assets/poster.png Contents/mods/GlasVoipMod/poster.png
cp mods/GlasVoipMod/media/lua/client/GlasVoip/*.lua Contents/mods/GlasVoipMod/media/lua/client/GlasVoip/
cp mods/GlasVoipMod/media/lua/server/GlasVoip/*.lua Contents/mods/GlasVoipMod/media/lua/server/GlasVoip/
cp assets/preview.png preview.png
```

## Publishing to the Steam Workshop (manual step)

This can't be automated from outside the game -- Steam Workshop
uploads require an authenticated Steam session inside the actual game
client, the same category of action as the admin-password-gated
Homebrew install earlier in this project.

1. In Project Zomboid's main menu, go to Mods -> the mod's context
   menu -> **Upload to Workshop** (or the equivalent entry current PZ
   versions expose -- menu wording has changed across game versions).
2. Point it at this repo's root folder (containing `workshop.txt`,
   `preview.png`, and `Contents/`).
3. **Before confirming the upload**, check the visibility/privacy
   setting in the upload dialog and set it to **Unlisted** -- do not
   rely on `workshop.txt`'s own `visibility=public` line, which
   mirrors the game's own template default and was not confirmed
   during this project's research to actually control this setting
   the way a Workshop-side "unlisted" flag would.
4. Complete the upload, then copy the resulting Workshop item's direct
   URL/ID for sharing with the server's players (not searchable
   publicly, per the unlisted setting).
