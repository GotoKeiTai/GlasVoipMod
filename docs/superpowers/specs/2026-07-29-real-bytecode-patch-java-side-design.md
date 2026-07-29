# Real bytecode patch — Java side (sub-project 2a) — Design

**Statut :** approuvé, prêt pour planification
**Portée :** implémentation réelle du patch de distance VOIP par palier, côté Java uniquement. Le mod Lua (touche de cycle, indicateur visuel, synchronisation réseau du palier choisi) est un sous-projet séparé, cadré après celui-ci une fois le patch Java vérifié.

Contexte : `docs/superpowers/specs/2026-07-29-voip-mod-recherche-technique-design.md` et `docs/superpowers/specs/2026-07-29-voip-mod-spike-bytecode-patch-design.md` (spike déjà fusionné dans `main`, agent + `VoiceManagerTransformer` existants).

---

## Ce qu'on sait déjà (confirmé sur le vrai bytecode du jeu)

- Dans `VoiceManager.UpdateVMClient()`, il n'y a **qu'un seul point de lecture** de `minDistance`/`maxDistance` (deux `GETSTATIC` consécutifs, juste avant `INVOKESTATIC IsoUtils.smoothstep`). Les autres usages de ces champs statiques appartiennent à d'autres méthodes (`getCanHearAllVolume`, `getUserPlaySound`) qui ne concernent pas le calcul de volume par frame — pas besoin d'y toucher.
- À ce point précis, la variable locale 8 contient la référence `IsoPlayer` du joueur **qui parle** (confirmé en comparant le bytecode réel au code source décompilé).
- `zombie.Lua.LuaManager.env` est un champ `public static KahluaTable` — directement lisible depuis Java (`LuaManager.env.rawget(...)`), sans appel dans la VM Lua. C'est exactement le mécanisme que `VoiceManager` utilise déjà lui-même pour s'exposer à Lua (`LuaManager.init()` appelle `VoiceManager.instance.LuaRegister(platform, env)`).

## But

Injecter, juste avant les deux `GETSTATIC` existants, un appel à une méthode Java qui recalcule `minDistance`/`maxDistance` en fonction du palier choisi par le joueur qui parle — lu dans une table Lua globale (que le mod Lua, sous-projet suivant, alimentera). Si aucun palier n'est connu pour ce joueur (mod Lua pas encore chargé, ou joueur jamais synchronisé), on **ne touche à rien** : les champs gardent leur valeur vanilla synchronisée depuis le serveur, ce qui correspond exactement au palier "Parler" par défaut (confirmé) — pas de cas spécial à coder pour ce fallback.

## Dépendance de compilation au jeu (nouveau)

Pour écrire du Java typé contre `zombie.characters.IsoPlayer`, `zombie.Lua.LuaManager`, `se.krka.kahlua.vm.KahluaTable`, on ajoute une dépendance Gradle `compileOnly` (jamais embarquée dans le jar produit, donc jamais redistribuée) pointant vers le dossier `Contents/Java` de l'installation locale du jeu. Le chemin est lu depuis un fichier `local.properties` **non committé** (gitignored, un par poste de développement) — pratique standard dans le modding Java (Minecraft Forge/Fabric et la communauté PZ l'utilisent de la même façon). Rien de propriétaire n'est jamais commité dans le repo, ni le chemin (spécifique à chaque machine), ni les classes elles-mêmes.

## Architecture

### Nouveaux fichiers

- `local.properties.example` — modèle committé montrant le format attendu (`pz.install.path=/chemin/vers/Project Zomboid.app/Contents/Java`), pour que quiconque clone le repo sache quoi créer.
- `src/main/java/glas/voip/patch/TierDistanceCalculator.java` — logique pure : palier (0=chuchoter, 1=parler, 2=hurler) + configuration des distances par palier → paire `(minDistance, maxDistance)`. Aucune dépendance au jeu, aucune I/O — entièrement testable.
- `src/main/java/glas/voip/patch/PlayerTierProvider.java` — interface : `Integer getTierFor(IsoPlayer speaker)`, retourne `null` si le palier de ce joueur n'est pas connu.
- `src/main/java/glas/voip/patch/LuaGlobalTierProvider.java` — implémentation réelle, lit `LuaManager.env.rawget("GlasVoipTiers")` puis `.rawget(onlineId)`. Dépend de `LuaManager`/`KahluaTable` (dépendance `compileOnly` ci-dessus). Non testable unitairement (nécessite une vraie VM Lua en cours d'exécution, comme le reste de l'interaction avec le jeu) — vérifié manuellement en lançant le jeu, même processus que le spike.
- `src/main/java/glas/voip/patch/TierPatch.java` — point d'entrée appelé depuis le bytecode injecté : `applyTierDistances(IsoPlayer speaker, PlayerTierProvider provider)`. Calcule les distances via `TierDistanceCalculator` si un palier est trouvé, puis écrit dans les champs privés statiques `VoiceManager.minDistance`/`maxDistance` par réflexion (`Field.setAccessible(true)`) — la réflexion est nécessaire ici uniquement à cause de la visibilité `private` des champs, pas à cause d'une dépendance de compilation manquante.
- `src/test/java/glas/voip/patch/TierDistanceCalculatorTest.java` — tests unitaires complets sur la logique pure.
- `src/test/java/glas/voip/patch/InjectionFixture.java` — petite classe de test **écrite par nous** (pas du code du jeu), reproduisant volontairement la forme exacte du point d'injection réel (deux champs `static float`, une méthode avec deux `GETSTATIC` consécutifs suivis d'un calcul) — sert de cible synthétique et sans ambiguïté de droits pour tester le comportement du transformer, sans jamais avoir besoin des vraies classes du jeu dans le repo.
- `src/test/java/glas/voip/spike/VoiceManagerTransformerTest.java` — étendu avec de nouveaux tests vérifiant que le bytecode transformé injecte bien l'appel attendu (vérifié en désassemblant le résultat via ASM, comme le spike le fait déjà pour dumper).

### Fichiers modifiés

- `build.gradle.kts` — ajout de la dépendance `compileOnly` conditionnelle (lue depuis `local.properties`).
- `.gitignore` — ajout de `local.properties`.
- `src/main/java/glas/voip/spike/VoiceManagerTransformer.java` — au lieu de seulement dumper puis retourner `null`, transforme réellement le bytecode : repère le `GETSTATIC zombie/core/raknet/VoiceManager.maxDistance` à l'intérieur de `UpdateVMClient`, insère juste avant `ALOAD 8` + `INVOKESTATIC glas/voip/patch/TierPatch.applyTierDistances`, laisse le reste de la méthode inchangé, retourne le nouveau bytecode (plus `null`).

## Gestion des erreurs

- `LuaGlobalTierProvider` : si `LuaManager.env` est `null`, si la table `GlasVoipTiers` n'existe pas encore, ou si la valeur pour ce joueur n'est pas un nombre valide → retourne `null` (pas d'exception), `TierPatch` interprète `null` comme "aucun override, laisser vanilla".
- `TierPatch.applyTierDistances` : toute exception (réflexion échouée, palier hors bornes) est attrapée et loggée (`System.err`, même pattern que `VoiceManagerTransformer` dans le spike — jamais de silence total), puis on laisse les champs vanilla inchangés plutôt que de propager une erreur dans la boucle audio du jeu.

## Tests

- `TierDistanceCalculatorTest` : tests unitaires classiques (palier→distances, bornes, palier inconnu).
- Tests du transformer étendu (`VoiceManagerTransformerTest`) : contre `InjectionFixture` (classe synthétique), vérifie que le bytecode transformé contient bien la séquence `ALOAD 8` + `INVOKESTATIC TierPatch.applyTierDistances` juste avant le premier `GETSTATIC` ciblé, et que la classe transformée reste chargeable (`ClassLoader` custom qui charge les bytes transformés et vérifie qu'aucune `VerifyError` ne survient).
- Vérification manuelle finale contre le vrai jeu (même processus que le spike : lancer avec l'agent, observer les logs, confirmer qu'aucune régression n'apparaît sur le VOIP normal) — nécessaire avant de considérer ce sous-projet terminé, puisque `LuaGlobalTierProvider` et l'écriture par réflexion ne sont pas testables sans le jeu réel.

## Hors-scope (explicitement)

- Le mod Lua lui-même (touche de cycle, indicateur visuel, table `GlasVoipTiers` alimentée via `sendClientCommand`/`Events.OnClientCommand`, `mod.info`, structure `media/lua/`) — sous-projet suivant, qui dépendra de ce patch Java déjà en place et vérifié.
- Lecture de `server.json` pour les distances par palier configurables — sera câblé dans le sous-projet Lua (ou un point d'entrée de configuration à définir alors), `TierDistanceCalculator` accepte déjà une configuration en paramètre donc n'a pas besoin d'être retouché à ce moment-là.
- Packaging/distribution du mod complet (Velopack côté launcher, ou Steam Workshop) — hors-scope de ce repo pour l'instant.
