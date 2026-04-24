# AGENTS.md — Jellyfin Fork: Instructions pour agents autonomes (Codex, Devin, etc.)

Ce fichier guide les agents autonomes (OpenAI Codex, Devin, SWE-agent, etc.)
sur la manière d'opérer dans ce dépôt Jellyfin.

---

## Règles d'exécution impératives

### Ce que les agents PEUVENT faire autonomément

- Créer des branches `feature/*`, `fix/*`, `refactor/*` depuis `master`
- Modifier du code dans `Jellyfin.Api/`, `MediaBrowser.Controller/`, `Emby.Server.Implementations/`
- Ajouter/modifier des tests dans `tests/`
- Créer des migrations EF Core (`dotnet ef migrations add`)
- Modifier des fichiers `.csproj`, `global.json`, `Directory.Build.props`
- Mettre à jour `CHANGELOG.md`

### Ce que les agents NE DOIVENT PAS faire sans validation humaine

- ❌ Modifier `DlnaProfiles/*.xml` (risque de régression sur appareils réels)
- ❌ Modifier `Emby.Server.Implementations/Dlna/` (stack DLNA critique)
- ❌ Changer le schéma de base de données sans migration EF Core validée
- ❌ Modifier les middlewares d'authentification (`AuthenticationMiddleware`, `ApiKeyMiddleware`)
- ❌ Supprimer des endpoints API existants (breaking change)
- ❌ Modifier `jellyfin-sdk-typescript` directement — passer par la regen OpenAPI
- ❌ Pusher sur `master` ou `release/*` directement

---

## Pipeline de validation obligatoire

Avant toute PR, l'agent doit exécuter dans l'ordre :

```bash
# 1. Build complet
dotnet build --configuration Release --no-incremental
# → Doit sortir avec code 0, zéro warning

# 2. Formatage
dotnet format --verify-no-changes
# → Si échec : exécuter `dotnet format` puis re-vérifier

# 3. Tests unitaires
dotnet test --configuration Release --logger "console;verbosity=detailed"
# → 0 test en échec toléré

# 4. Tests DLNA spécifiques (si fichiers DLNA modifiés)
dotnet test tests/Jellyfin.Dlna.Tests/ --filter "Category=DLNA"

# 5. Analyse statique
dotnet build /p:EnableNETAnalyzers=true /p:AnalysisMode=AllEnabledByDefault
```

---

## Contexte de la codebase

### Entry points clés

| Fichier | Rôle |
|---------|------|
| `Jellyfin.Server/Program.cs` | Point d'entrée, configuration DI |
| `Jellyfin.Server/Startup.cs` | Middleware ASP.NET Core |
| `Jellyfin.Api/Controllers/` | Tous les endpoints REST |
| `MediaBrowser.Controller/Library/ILibraryManager.cs` | Interface principale bibliothèque |
| `Emby.Server.Implementations/Library/LibraryManager.cs` | Implémentation principale |
| `MediaBrowser.Model/Configuration/ServerConfiguration.cs` | Config serveur centralisée |

### Conventions de nommage

```
IMyService.cs          → Interface dans MediaBrowser.Controller/ ou Jellyfin.*/
MyService.cs           → Implémentation dans Emby.Server.Implementations/
MyServiceTests.cs      → Tests dans tests/Jellyfin.*.Tests/
MyController.cs        → Contrôleur dans Jellyfin.Api/Controllers/
MyDto.cs               → DTO dans MediaBrowser.Model/ ou Jellyfin.Api/Models/
```

### Injection de dépendances

```csharp
// Enregistrement dans Jellyfin.Server/Startup.cs ou un IServiceCollectionExtensions
services.AddSingleton<IMyService, MyService>();
services.AddScoped<IMyRepository, MyRepository>();  // EF Core contexts
services.AddTransient<IMyFactory, MyFactory>();
```

### Gestion des erreurs API

```csharp
// Utiliser les helpers intégrés
return NotFound($"Item {itemId} not found");
return BadRequest("Invalid codec specified");
return StatusCode(StatusCodes.Status500InternalServerError, "Transcoding failed");

// Pour les erreurs métier : lancer des exceptions typées
throw new ResourceNotFoundException($"Media stream {streamId} not found");
throw new SecurityException("Insufficient permissions for admin endpoint");
```

---

## Heuristiques pour les tâches courantes

### Ajouter un champ à une API existante

1. Modifier le DTO dans `MediaBrowser.Model/`
2. Peupler le champ dans l'implémentation correspondante
3. Vérifier que le champ est sérialisable JSON (pas de circular ref)
4. Mettre à jour les tests existants du contrôleur
5. NE PAS supprimer de champs existants (rétrocompatibilité)

### Corriger un bug de transcoding

1. Reproduire via `dotnet test` ou log FFmpeg dans `JELLYFIN_LOG_DIR`
2. Localiser dans `src/Jellyfin.MediaEncoding/Encoder/MediaEncoder.cs`
3. Vérifier l'impact sur les profils DLNA dans `MediaBrowser.Model/Dlna/StreamBuilder.cs`
4. Ajouter un test de régression dans `tests/Jellyfin.MediaEncoding.Tests/`

### Corriger un bug de bibliothèque (scan, métadonnées)

1. Reproduire via un scan de dossier de test minimal
2. Localiser dans `Emby.Server.Implementations/Library/`
3. Vérifier `Emby.Naming/` pour les règles de parsing de noms de fichiers
4. Les providers de métadonnées sont dans `MediaBrowser.Providers/`

---

## Format de commit attendu

```
<type>(<scope>): <description courte en anglais>

[Corps optionnel : contexte, why, what]

[Footer : Fixes #123, Breaking-Change: oui/non]
```

Types valides : `feat`, `fix`, `refactor`, `test`, `docs`, `chore`, `perf`, `ci`
Scopes valides : `dlna`, `api`, `library`, `transcoding`, `auth`, `db`, `naming`, `networking`

---

## Signaux d'alerte — Stopper et demander validation

L'agent doit s'arrêter et créer une issue de discussion si :

- La modification implique un changement de comportement DLNA observable
- Le build produit des warnings Roslyn de type `CS8600`-`CS8634` (nullable)
- Un test existant casse après la modification (ne pas simplement le supprimer)
- La feature nécessite une dépendance NuGet non présente dans la solution
- Le code généré dépasse 500 lignes pour une seule PR (décomposer)
