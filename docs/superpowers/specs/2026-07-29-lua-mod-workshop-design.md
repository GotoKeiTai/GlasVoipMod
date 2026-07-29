# Mod Lua + publication Workshop (sous-projet 2b) — Design

**Statut :** approuvé, prêt pour planification
**Portée :** le mod Lua complet (touche de cycle, indicateur visuel, synchronisation réseau du palier) et sa préparation pour publication non répertoriée sur le Steam Workshop de Project Zomboid. Le patch Java (sous-projet 2a) est déjà terminé, fusionné et vérifié contre le vrai jeu.

Contexte complet : `docs/superpowers/specs/2026-07-29-voip-mod-recherche-technique-design.md` et `docs/superpowers/specs/2026-07-29-real-bytecode-patch-java-side-design.md`.

---

## Contrainte confirmée sur le vrai jeu (pas une supposition)

La structure exacte d'un mod prêt pour le Workshop a été vérifiée directement sur `Workshop/ModTemplate`, fourni avec l'installation du jeu :

```
<dossier d'upload>/
  Contents/
    mods/
      <NomDuMod>/
        mod.info          # name=, id=, description=, poster=
        poster.png         # 512x512, image de la fiche mod en jeu
        media/lua/...
  preview.png               # 256x256, vignette de la fiche Workshop
  workshop.txt               # version=, title=, description= (multi-lignes), tags=, visibility=
```

Le développement/test local se fait dans une structure différente, sans le dossier `Contents/` intermédiaire : `Zomboid/mods/<NomDuMod>/mod.info` + `media/lua/...` (l'emplacement que le jeu scanne pour activer un mod localement, avant toute publication).

Recherche complémentaire : pas d'indicateur circulaire de portée tout fait à copier en Lua vanilla, mais le pattern de rendu ancré au monde existe bien et est réutilisable (`media/lua/client/Foraging/ISBaseIcon.lua`, `ISWorldItemIcon.lua`) — confirme la faisabilité technique de l'indicateur sans avoir à deviner l'API de rendu.

## Décisions produit confirmées

- **Nom/ID du mod** : `GlasVoipMod` (cohérent avec le repo).
- **Visibilité Workshop** : non répertoriée (accessible par lien direct uniquement, pas cherchable publiquement).
- **Touche de cycle** : touche fixe codée en dur (pas d'intégration au menu de rebind PZ) — plus simple, suffisant pour un mod de serveur communautaire.
- **Images** : `poster.png` (512×512) et `preview.png` (256×256) générées à partir de `shield.png` du launcher (`~/Desktop/GlasLauncher/src/GlasLauncher.App/Assets/shield.png`, 349×285), redimensionné et centré sur un canevas carré à fond vert foncé (`#0d1f16`, la couleur de fond déjà utilisée dans le launcher) via `sips` (outil macOS déjà disponible, pas de nouvelle dépendance).

## Architecture

### Fichiers Lua (dans `media/lua/client/GlasVoip/`)

- `GlasVoipTierController.lua` — détecte la touche fixe à chaque frame (`isKeyDown`), fait cycler un état local `currentTier` (0=chuchoter, 1=parler, 2=hurler), déclenche la synchronisation et l'indicateur à chaque changement.
- `GlasVoipTierSync.lua` — envoie le palier choisi via `sendClientCommand` ; reçoit les changements des autres joueurs via `Events.OnClientCommand` ; alimente la table globale `_G.GlasVoipTiers[onlineId] = tier` — exactement la table que `LuaGlobalTierProvider` (déjà en place côté Java) lit déjà via `LuaManager.env.rawget("GlasVoipTiers")`.
- `GlasVoipIndicator.lua` — dessine un cercle au sol ancré à la position du joueur local (visible uniquement par lui-même), affiché ~1 seconde à chaque changement de palier, adapté du pattern de rendu ancré au monde déjà présent dans le Lua vanilla.

### Fichiers de packaging

- `mods/GlasVoipMod/mod.info`
- `mods/GlasVoipMod/poster.png`
- `mods/GlasVoipMod/media/lua/client/GlasVoip/*.lua` (les trois fichiers ci-dessus)
- `workshop.txt`, `preview.png` (à la racine du dossier d'upload, hors de `mods/GlasVoipMod/`)

## Données échangées (contrat déjà fixé côté Java, ne pas dévier)

- Clé de la table : `onlineId` (nombre Lua = toujours un `double` côté Kahlua, confirmé par décompilation lors du sous-projet précédent).
- Valeur : palier entier `0`, `1`, ou `2` — toute autre valeur (non entière, hors bornes) est déjà rejetée côté Java (`LuaGlobalTierProvider`), donc le Lua doit toujours écrire un entier Lua propre dans cette plage.

## Test et vérification

Comme pour `LuaGlobalTierProvider`/`ReflectiveVoiceManagerDistanceWriter` côté Java, le Lua ne peut pas être testé unitairement sans le vrai jeu — vérification manuelle uniquement : charger le mod localement (`Zomboid/mods/GlasVoipMod/`), lancer le jeu avec l'agent Java déjà construit, appuyer sur la touche, confirmer dans les logs que `~/glasvoipmod-voicemanager-dump.txt`-style ou la console affiche bien le changement de palier appliqué côté Java (le patch existant logue déjà les échecs de résolution — on pourra ajouter un log de succès temporaire côté `TierPatch` si besoin pour le debug, à retirer ensuite).

## Publication Workshop

**Étape manuelle, pas automatisable depuis cette session** : l'upload Workshop se fait via l'outil intégré au jeu (menu Mods → Upload to Workshop), qui nécessite une session Steam authentifiée avec le compte propriétaire — comparable aux autres actions Steam-authentifiées déjà rencontrées cette session (ex. l'installation Homebrew nécessitant le mot de passe admin). Le plan produit le dossier correctement structuré et prêt ; l'utilisateur effectue le clic d'upload lui-même, guidé par des instructions précises dans le README.

## Hors-scope (explicitement)

- Lecture de `server.json` pour les distances par palier configurables (déjà noté hors-scope dans le spec précédent — les valeurs restent celles codées en dur côté Java pour l'instant).
- Intégration au menu de rebind PZ pour la touche.
- Mise à jour automatisée du mod Workshop après publication initiale (changenote, versioning) — sera traité manuellement au cas par cas.
