# CLAUDE.md — Jellyfin Fork: Instructions pour Claude

Ce fichier fournit à Claude (Anthropic) le contexte architectural, les standards de codage
et les règles de contribution pour ce fork de Jellyfin.

---

## 1. Architecture du projet

### Vue d'ensemble

Jellyfin est un serveur multimédia **auto-hébergé** composé de plusieurs dépôts indépendants :

| Dépôt | Technologie | Rôle |
|-------|-------------|------|
| `jellyfin` (server) | C# / .NET 8 | Backend HTTP, transcoding, bibliothèque |
| `jellyfin-web` | TypeScript / React | Client web SPA |
| `jellyfin-android` | Kotlin / Jetpack Compose | Client Android natif |
| `jellyfin-androidtv` | Kotlin / Leanback | Client Android TV |
| `jellyfin-expo` (iOS) | TypeScript / React Native | Client iOS |
| `jellyfin-roku` | BrightScript | Client Roku |
| `jellyfin-sdk-typescript` | TypeScript | SDK client généré OpenAPI |

### Architecture serveur (C# / .NET 8)

```
Jellyfin.Server/           ← Point d'entrée, DI, configuration hôte
Jellyfin.Api/              ← Contrôleurs ASP.NET Core (REST + WebSocket)
  Controllers/
    DlnaController.cs      ← Endpoints DLNA/UPnP
    MediaInfoController.cs ← Probing, streams info
    PlaystateController.cs ← Lecture, progression
Jellyfin.Data/             ← Entités EF Core, migrations SQLite
MediaBrowser.Controller/   ← Interfaces & contrats métier (ILibraryManager, etc.)
MediaBrowser.Common/       ← Utilitaires partagés, extensions, modèles communs
MediaBrowser.Model/        ← DTOs, enums, modèles de données purs
Emby.Server.Implementations/ ← Implémentations concrètes des interfaces
  Dlna/                    ← Stack DLNA complète
  Library/                 ← LibraryManager, MediaSourceManager
  LiveTv/                  ← HDHR, M3U, XMLTV
Emby.Naming/               ← Règles de nommage de fichiers
Jellyfin.Networking/       ← Détection réseau, SSDP, interfaces
src/Jellyfin.Plugin.*/     ← Plugins officiels (structure recommandée)
tests/                     ← Tests xUnit
```

### Pipeline de requête HTTP

```
Client HTTP
  → ASP.NET Core Middleware (auth JWT, CORS, rate-limit)
  → ApiController (validation DTO)
  → ILibraryManager / IMediaSourceManager
  → FFmpeg transcoder (si nécessaire)
  → StreamResult → Client
```

### Stack DLNA / UPnP

- **Discovery** : SSDP via `Jellyfin.Networking.Ssdp` (UDP multicast 239.255.255.250:1900)
- **Device Description** : XML UPnP servi par `DlnaController`
- **ContentDirectory** : Service UPnP CDS 1.0 — Browse/Search/GetSystemUpdateID
- **ConnectionManager** : Gestion des connexions DLNA
- **AVTransport** : Contrôle lecture à distance (Play/Pause/Seek/Stop)
- **Renderer profiles** : `DlnaProfiles/` — XML de capacités par appareil

---

## 2. Standards de codage .NET

### Versioning et SDK

- Cible **net8.0** (LTS). Ne pas descendre à net6.0 sans discussion.
- `global.json` pin la version SDK : respecter la version indiquée.
- Nullable reference types **activés** (`<Nullable>enable</Nullable>`) : tout le code doit être null-safe.

### Style de code (enforced via .editorconfig + StyleCop)

```csharp
// ✅ CORRECT
public sealed class MediaStreamSelector
{
    private readonly ILogger<MediaStreamSelector> _logger;

    public MediaStreamSelector(ILogger<MediaStreamSelector> logger)
    {
        _logger = logger;
    }

    public MediaStream? SelectAudioStream(IReadOnlyList<MediaStream> streams, string? preferredLanguage)
    {
        ArgumentNullException.ThrowIfNull(streams);

        return streams
            .Where(s => s.Type == MediaStreamType.Audio)
            .OrderByDescending(s => s.Language == preferredLanguage)
            .FirstOrDefault();
    }
}
```

Règles clés :
- `sealed` par défaut pour les classes non héritées
- Inject via constructeur uniquement (pas de `ServiceLocator`)
- `ILogger<T>` pour tout logging — jamais `Console.WriteLine`
- `ArgumentNullException.ThrowIfNull()` en entrée de méthode publique
- `IReadOnlyList<T>` / `IReadOnlyCollection<T>` pour les paramètres en lecture seule
- `async/await` partout où I/O — jamais `.Result` ou `.Wait()`
- `CancellationToken` propagé dans toute la chaîne async
- Records pour les DTOs immuables : `public record MediaStreamInfo(string Codec, int Bitrate);`

### Logging

```csharp
// Structured logging avec source generators (net6+)
_logger.LogInformation("Transcoding started for item {ItemId} with codec {Codec}", itemId, codec);

// ✅ Utiliser LoggerMessage pour les hot paths
private static readonly Action<ILogger, Guid, Exception?> _transcodingStarted =
    LoggerMessage.Define<Guid>(LogLevel.Information, new EventId(1, "TranscodingStarted"),
        "Transcoding started for item {ItemId}");
```

### Tests

- Framework : **xUnit 2.x**
- Mocking : **Moq 4.x** ou **NSubstitute**
- Assertions : **FluentAssertions**
- Nommage : `MethodName_StateUnderTest_ExpectedBehavior`
- Coverage minimum pour nouvelles features : **80%** sur le code métier

```csharp
[Fact]
public void SelectAudioStream_WithPreferredLanguage_ReturnsMatchingStream()
{
    // Arrange
    var streams = new List<MediaStream>
    {
        new() { Type = MediaStreamType.Audio, Language = "fra", Index = 0 },
        new() { Type = MediaStreamType.Audio, Language = "eng", Index = 1 }
    };

    // Act
    var result = _selector.SelectAudioStream(streams, "fra");

    // Assert
    result.Should().NotBeNull();
    result!.Language.Should().Be("fra");
}
```

---

## 3. Règles de contribution

### Workflow Git

1. **Fork** → branche depuis `master` avec convention : `feature/description-courte`, `fix/issue-123`, `refactor/component-name`
2. **Commits** : format Conventional Commits obligatoire
   ```
   feat(dlna): add HEVC transcoding profile for Samsung TVs
   fix(api): correct null reference in PlaystateController #4521
   refactor(library): extract IMediaProber interface
   ```
3. **PR** : une fonctionnalité = une PR. Pas de PR "fourre-tout".
4. **Rebase** sur `master` avant PR (pas de merge commits).
5. **Changelog** : mettre à jour `CHANGELOG.md` dans chaque PR selon Keep a Changelog.

### Checklist PR obligatoire

- [ ] Tests unitaires pour toute logique métier nouvelle
- [ ] Zéro warning de compilation (`<TreatWarningsAsErrors>true</TreatWarningsAsErrors>`)
- [ ] `dotnet format` passé (CI bloque sinon)
- [ ] Mise à jour des DTOs dans `jellyfin-sdk-typescript` si API modifiée
- [ ] Migration EF Core ajoutée si schéma DB modifié
- [ ] Documentation XML (`/// <summary>`) sur toutes les APIs publiques
- [ ] Pas de breaking change API sans version bump et deprecation notice

### Breaking changes

- Toute modification d'API REST doit être rétrocompatible pendant **2 versions mineures**
- Utiliser `[Obsolete("Use X instead. Will be removed in v11.", DiagnosticId = "JFOBSOLETE001")]`
- Les suppressions de champs JSON doivent passer par le projet `jellyfin-sdk-typescript`

---

## 4. Implémenter de nouvelles features média

### Étapes canoniques

**Exemple : ajouter un nouveau format de sous-titres (ex: ASS étendu)**

#### Étape 1 — Modèle de données

```csharp
// MediaBrowser.Model/Entities/SubtitleFormat.cs
public enum SubtitleFormat
{
    SRT,
    ASS,
    SSA,
    VTT,
    TTML,
    MyNewFormat  // ← ajouter ici
}
```

#### Étape 2 — Détection dans Emby.Naming

```csharp
// Emby.Naming/Common/NamingOptions.cs
SubtitleFileExtensions = new[] { ".srt", ".ass", ".ssa", ".vtt", ".mynew" };
```

#### Étape 3 — Probing FFmpeg

```csharp
// Emby.Server.Implementations/MediaEncoder/MediaEncoder.cs
// Ajouter la reconnaissance dans ParseMediaInfo()
```

#### Étape 4 — Contrôleur API

```csharp
// Jellyfin.Api/Controllers/SubtitleController.cs
[HttpGet("{itemId}/Subtitles/{index}/Stream.mynew")]
[ProducesResponseType(StatusCodes.Status200OK)]
public async Task<ActionResult> GetSubtitleStream(
    [FromRoute] Guid itemId,
    [FromRoute] int index,
    CancellationToken cancellationToken)
{
    // Implémentation
}
```

#### Étape 5 — Tests

```
tests/Jellyfin.Naming.Tests/SubtitleTests/MyNewFormatTests.cs
tests/Jellyfin.Api.Tests/SubtitleControllerTests.cs
```

#### Étape 6 — Mise à jour OpenAPI spec + SDK TypeScript

```bash
cd Jellyfin.Server
dotnet run -- --generateopenapispec
# Puis régénérer le SDK TypeScript
```

### Transcoding / FFmpeg

- Les profils de transcoding sont dans `MediaBrowser.Model/Dlna/StreamBuilder.cs`
- Tout nouveau codec doit avoir un profil dans `DlnaProfiles/` et dans `StreamBuilder`
- **Ne jamais** modifier le process FFmpeg directement : passer par `IMediaEncoder.GetMediaInfo()`
- Les sessions de transcoding sont trackées par `ITranscodingJobHelper`

---

## 5. Compatibilité DLNA / UPnP — Règles anti-régression

### ⚠️ Zone critique — Lire avant toute modification

Les changements dans ces fichiers/classes peuvent casser des appareils DLNA en production :

```
Emby.Server.Implementations/Dlna/
Jellyfin.Api/Controllers/DlnaController.cs
Jellyfin.Api/Controllers/DlnaServerController.cs
MediaBrowser.Model/Dlna/
src/Jellyfin.MediaEncoding/
DlnaProfiles/*.xml
```

### Règles strictes DLNA/UPnP

1. **XML UPnP** : Les réponses SOAP doivent rester strictement conformes à UPnP 1.0/1.1.
   Ne jamais ajouter d'attributs XML non standard sans namespace propre.

2. **Headers HTTP** : Les DMRs (Digital Media Renderers) vérifient des headers spécifiques :
   ```
   Content-Type: video/mpeg          ← exact, pas video/mpeg2
   transferMode.dlna.org: Streaming  ← casse sensible
   contentFeatures.dlna.org: DLNA.ORG_PN=AVC_MP4_HP_HD_AAC;DLNA.ORG_OP=01
   ```

3. **SSDP** : Ne pas modifier les intervalles d'annonce (défaut 1800s) sans test sur réseau isolé.
   Le TTL UDP et le délai de re-annonce sont critiques pour la découverte.

4. **Profiles XML** : Chaque profil `DlnaProfiles/*.xml` correspond à des appareils réels testés.
   Modifier un profil existant = risque de régression sur cet appareil.
   Ajouter un nouveau profil : toujours en copiant un profil existant similaire comme base.

5. **ContentDirectory Browse** : La pagination (StartingIndex/RequestedCount) doit être respectée
   à la lettre — les DMCs anciens plantent si le Count retourné > RequestedCount.

6. **AVTransport** : Les états (`STOPPED`, `PLAYING`, `PAUSED_PLAYBACK`, `TRANSITIONING`)
   doivent suivre exactement la FSM UPnP AV 1.0 spec.

### Tests de non-régression DLNA

Avant tout PR touchant DLNA :
```bash
# Lancer le test suite DLNA
dotnet test tests/Jellyfin.Dlna.Tests/ --filter "Category=DLNA"

# Test d'intégration avec appareil virtuel
dotnet test tests/Jellyfin.Integration.Tests/ --filter "Category=UPnP"
```

Appareils à tester manuellement si possible :
- Samsung Smart TV (Tizen 4+)
- LG webOS TV
- Denon/Marantz AVR
- Kodi (client DLNA)
- VLC (client DLNA)
- BubbleUPnP

### Matrice de compatibilité minimale

| Appareil | Protocole | Profil critique |
|----------|-----------|-----------------|
| Samsung TV 2018+ | DLNA 1.5 | `Samsung_SmartTV_2018` |
| LG TV webOS 4+ | DLNA 1.5 | `LG_Smart_TV` |
| Sony Bravia | DLNA 1.5 | `Sony_Bravia` |
| Xbox One | DLNA 1.5 | `Xbox_One` |
| PS4/PS5 | DLNA 1.5 | `PlayStation_4` |
| Denon AVR | UPnP AV | `Denon_AVR` |

---

## 6. Environnement de développement

```bash
# Prérequis
dotnet --version  # 8.0.x requis
ffmpeg -version   # 6.0+ recommandé

# Build
git clone https://github.com/jellyfin/jellyfin
cd jellyfin
dotnet build

# Tests
dotnet test --configuration Release

# Linting
dotnet format --verify-no-changes

# Run serveur local
JELLYFIN_DATA_DIR=~/.local/share/jellyfin \
JELLYFIN_LOG_DIR=~/.local/share/jellyfin/log \
dotnet run --project Jellyfin.Server -- --nowebclient
```

### Variables d'environnement utiles

```bash
JELLYFIN_LOG_DIR          # Répertoire des logs
JELLYFIN_DATA_DIR         # Données (config, db)
JELLYFIN_CACHE_DIR        # Cache transcodings
JELLYFIN_CONFIG_DIR       # jellyfin.xml, network.xml
DOTNET_ENVIRONMENT        # Development | Production
JELLYFIN_FFMPEG           # Chemin FFmpeg custom
```

---

## 7. Sécurité

- **JWT** : Tokens générés par `IAuthenticationManager` — ne jamais bypasser l'auth
- **API Keys** : Stockées hashées (Argon2id) dans SQLite — ne jamais logger les clés brutes
- **Path traversal** : Toujours utiliser `IFileSystem.DirectoryExists()` et valider les chemins avec `Path.GetFullPath()`
- **SSRF** : Les URLs externes (artwork, metadata) doivent passer par `IHttpClientFactory` configuré
- **Headers** : `Content-Security-Policy` et `X-Content-Type-Options` gérés par `SecurityHeadersMiddleware`

Ne jamais :
- Désactiver l'authentification pour un endpoint sans annotation explicite `[AllowAnonymous]` + justification en commentaire
- Stocker des credentials en clair dans la config
- Exécuter du shell arbitraire à partir d'input utilisateur
