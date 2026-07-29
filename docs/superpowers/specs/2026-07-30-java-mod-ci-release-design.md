<!-- title: CI + Release réelle du mod Java — Design -->

# CI + Release réelle du mod Java — Design

**Statut :** approuvé, prêt pour planification
**Portée :** mettre en place une vraie distribution du jar Java de GlasVoipMod (CI build+test, release GitHub déclenchée par tag, manifest.json consommable par GlasLauncher), débloquant le sous-projet #4 de GlasLauncher (`docs/session-notes.md` côté GlasLauncher, hébergement du manifeste). N'affecte pas le mod Lua (déjà publié, indépendant, sur le Steam Workshop).

Contexte complet : `docs/superpowers/specs/2026-07-30-java-mod-launch-option-correction.md` (déjà committé, décrit le blocage restant que ce sous-projet résout).

## Contrainte confirmée avant conception (pas une supposition)

Le repo n'a jamais été buildé sans `local.properties` (fichier gitignoré, machine-spécifique, contient le chemin vers l'installation locale du jeu). `build.gradle.kts` rend `compileOnly(files(pzInstallPath))` conditionnel à sa présence — absent, cette dépendance est simplement sautée. Un runner GitHub Actions n'aura jamais le jeu installé : `local.properties` y sera toujours absent.

Un seul fichier du code de production importe des classes réelles du jeu : `src/main/java/glas/voip/patch/LuaGlobalTierProvider.java` (`se.krka.kahlua.vm.KahluaTable`, `zombie.Lua.LuaManager`). Sans ces classes disponibles au moment de la compilation, `./gradlew jar`/`test` échouerait immédiatement en CI — un vrai blocage, découvert en préparant ce design, pas une supposition.

## Décisions confirmées avec l'utilisateur

- **Versionnage** : passage à un semver propre, `1.0.0` (le mod n'est plus un spike — Lua publié sur le Workshop, patch Java vérifié en jeu). `build.gradle.kts` : `version = "0.1.0-spike"` → `version = "1.0.0"`.
- **Déclencheur de release** : tag `vX.Y.Z` poussé sur `main`, même convention que GlasLauncher (`release.yml`, notes de version = message du tag annoté).
- **Hébergement du manifest.json** : asset de la même Release GitHub que le jar (lien stable via `/releases/latest/download/`), pas un fichier committé servi en raw — garantit que le manifeste et le jar sont toujours de la même version, un seul processus à maintenir.
- **Blocage de compilation CI** : résolu par des classes stub écrites à la main (signatures minimales, jamais de code décompilé du jeu), utilisées uniquement quand `local.properties` est absent.

## Architecture

### Stubs de compilation (`stubs/src/main/java/`)

Deux classes minimales, avec seulement les membres réellement utilisés par `LuaGlobalTierProvider.java` :

`stubs/src/main/java/se/krka/kahlua/vm/KahluaTable.java` :
```java
package se.krka.kahlua.vm;

public interface KahluaTable {
    Object rawget(Object key);
}
```

`stubs/src/main/java/zombie/Lua/LuaManager.java` :
```java
package zombie.Lua;

import se.krka.kahlua.vm.KahluaTable;

public class LuaManager {
    public static KahluaTable env;
}
```

Ce ne sont pas des extraits décompilés du jeu — des déclarations de signature écrites à la main, juste suffisantes pour que `javac` résolve les symboles utilisés par ce projet. Jamais exécutées : au runtime réel (agent attaché au jeu en cours d'exécution), ce sont les vraies classes du jeu qui sont chargées par la JVM, pas ces stubs.

### `build.gradle.kts` — changement du `compileOnly`

```kotlin
if (pzInstallPath != null) {
    compileOnly(files(pzInstallPath))
} else {
    compileOnly(files("stubs/src/main/java"))
}
```

En local (avec `local.properties`), rien ne change — les vraies classes du jeu restent utilisées, comportement identique à aujourd'hui. Seul le cas "absent" (CI) bascule vers les stubs.

### `.github/workflows/ci.yml`

Déclenché à chaque push/PR sur `main` :
1. Checkout
2. Setup JDK 17 (Temurin, cohérent avec `sourceCompatibility`/`targetCompatibility` déjà fixés dans `build.gradle.kts`)
3. `./gradlew test` (compile via les stubs, exécute la suite JUnit existante)

### `.github/workflows/release.yml`

Déclenché par un tag `v*.*.*` :
1. Checkout, setup JDK 17
2. `./gradlew jar` → produit `build/libs/GlasVoipMod-<version>.jar`
3. Copie vers un nom stable : `GlasVoipMod.jar` (le nom stable, pas versionné, est ce qui est réellement installé dans le dossier du jeu et référencé par l'option de lancement — versionner le nom de fichier casserait le lien stable `/releases/latest/download/`)
4. Calcule le SHA-256 du jar stable
5. Génère `manifest.json` :
   ```json
   {
     "files": [
       {
         "fileName": "GlasVoipMod.jar",
         "version": "<version extraite du tag, sans le 'v'>",
         "sha256": "<calculé à l'étape précédente>",
         "downloadUrl": "https://github.com/GotoKeiTai/GlasVoipMod/releases/latest/download/GlasVoipMod.jar"
       }
     ],
     "requiredLaunchOptions": ["-javaagent:GlasVoipMod.jar"]
   }
   ```
6. Crée la Release GitHub (`gh release create` ou action équivalente), notes = message du tag annoté, assets attachés : `GlasVoipMod.jar` + `manifest.json`.

`requiredLaunchOptions` est un tableau (pas une chaîne unique) dès maintenant, pour que le jour où un autre mod Java du serveur exige réellement ZombieBuddy en plus, il suffise d'ajouter une deuxième entrée (`-agentlib:zbNative --`) sans changer le schéma — cohérent avec la recommandation déjà écrite dans `2026-07-30-java-mod-launch-option-correction.md`.

## Changement côté GlasLauncher (mécanique, hors périmètre de conception)

`JavaModManifestFetcher.ManifestUrl` pointe aujourd'hui vers un repo placeholder qui n'existe pas (`GotoKeiTai/glas-launcher-hosting`). Une fois la première release `v1.0.0` de GlasVoipMod publiée, cette constante est mise à jour vers `https://github.com/GotoKeiTai/GlasVoipMod/releases/latest/download/manifest.json`. C'est un changement d'une ligne, pas une décision d'architecture — documenté ici pour traçabilité, effectué directement (pas laissé à deviner par la session Windows), avec un commit explicite dans GlasLauncher.

Le modèle C# `JavaModManifest`/`JavaFileEntry` ne connaît pas encore le champ `requiredLaunchOptions` — `System.Text.Json` avec `PropertyNameCaseInsensitive = true` ignore silencieusement les champs inconnus à la désérialisation (vérifié dans `JavaModManifestFetcher.cs`), donc publier ce champ dès maintenant ne casse rien côté C# actuel. L'ajout du champ au modèle C# (et son utilisation réelle dans `SteamLaunchOptionInspector`) reste une tâche pour la session Windows, déjà documentée dans `2026-07-30-java-mod-launch-option-correction.md`.

## Gestion des erreurs

- `ci.yml`/`release.yml` : échec de compilation ou de test bloque le workflow normalement (pas de suppression d'erreur) — même sévérité que n'importe quel CI standard.
- Si le calcul SHA-256 ou la génération du `manifest.json` échoue, le workflow échoue avant de créer la Release GitHub (pas de release partielle/incohérente publiée).

## Test et vérification

- `ci.yml` est lui-même le test principal : sa réussite sur push confirme que la compilation fonctionne sans `local.properties` (donc via les stubs).
- Un premier tag `v1.0.0` poussé manuellement sert de vérification de bout en bout du `release.yml` : Release GitHub créée, `GlasVoipMod.jar` et `manifest.json` présents et téléchargeables, SHA-256 du jar téléchargé correspondant à celui du manifeste (vérifiable avec `shasum -a 256`).
- Vérification manuelle restante, hors scope de ce sous-projet : que `GlasLauncher` (session Windows) parvient réellement à fetcher ce manifeste et à télécharger/installer le jar une fois sa propre correction (`RequiredLaunchOptions` dans le modèle C#) faite.

## Hors-scope (explicitement)

- Le contenu du modèle C# `JavaModManifest`/`SteamLaunchOptionInspector` (ajout réel du champ `RequiredLaunchOptions`) — déjà documenté comme tâche de la session Windows dans `2026-07-30-java-mod-launch-option-correction.md`, seule la constante d'URL est mise à jour ici.
- Signature de code / notarisation du jar — pas mentionné dans le cahier des charges de GlasLauncher pour le mod Java (contrairement à l'installeur GlasLauncher lui-même, §8.4), pas ajouté ici sans demande explicite.
- Le mod Lua et sa publication Workshop — travail déjà terminé, totalement indépendant de ce sous-projet.
