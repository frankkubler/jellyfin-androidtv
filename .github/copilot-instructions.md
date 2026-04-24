# GitHub Copilot Instructions — Jellyfin Fork

Ce fichier configure GitHub Copilot (via `.github/copilot-instructions.md`)
pour ce fork de Jellyfin.

---

## Contexte du projet

**Jellyfin** est un serveur multimédia open-source auto-hébergé.
- **Backend** : C# 12 / .NET 8, ASP.NET Core, Entity Framework Core (SQLite)
- **Frontend** : TypeScript / React (jellyfin-web)
- **Mobile** : Kotlin/Jetpack (Android), React Native/Expo (iOS)
- **Protocoles** : REST/JSON, WebSocket, DLNA/UPnP, HLS, MPEG-DASH

---

## Instructions de complétion de code

### Pour C# (backend)

Lors de la complétion dans `Jellyfin.Api/`, `Emby.Server.Implementations/`, etc. :

**Toujours** :
- Propager `CancellationToken cancellationToken` en dernier paramètre des méthodes async
- Utiliser `ILogger<T>` avec des log messages structurés (pas d'interpolation)
- Retourner `ActionResult<T>` dans les contrôleurs API (pas de types concrets directs)
- Valider les paramètres nuls avec `ArgumentNullException.ThrowIfNull()`
- Utiliser `ConfigureAwait(false)` dans les libs (pas dans ASP.NET Core controllers)

**Exemple de pattern contrôleur** :
```csharp
[HttpGet("{itemId}")]
[ProducesResponseType(typeof(MediaStream), StatusCodes.Status200OK)]
[ProducesResponseType(StatusCodes.Status404NotFound)]
[Authorize(Policy = "DefaultAuthorization")]
public async Task<ActionResult<MediaStream>> GetMediaStream(
    [FromRoute] Guid itemId,
    CancellationToken cancellationToken)
{
    var item = await _libraryManager.GetItemByIdAsync(itemId, cancellationToken)
        .ConfigureAwait(false);

    if (item is null)
    {
        return NotFound();
    }

    return Ok(_mapper.Map<MediaStream>(item));
}
```

**Jamais** :
- `.Result`, `.Wait()`, `.GetAwaiter().GetResult()` (deadlock risk)
- `new HttpClient()` (utiliser `IHttpClientFactory`)
- `File.ReadAllBytes()` sans vérification de taille (DoS risk)
- Logs avec données personnelles (chemins complets utilisateur, tokens)

### Pour TypeScript (jellyfin-web)

- Composants React fonctionnels avec hooks (`useState`, `useEffect`, `useMemo`)
- Types stricts : pas de `any`, utiliser les types du SDK `@jellyfin/sdk`
- Appels API via le SDK généré, pas de `fetch()` direct vers `/api`
- Gestion d'erreurs : toujours catch les rejets de Promise dans les composants

### Pour DLNA/UPnP (zone critique)

Lorsque Copilot complète du code dans `Emby.Server.Implementations/Dlna/` :
- Les réponses SOAP UPnP doivent être du XML valide avec l'envelope SOAP correcte
- Les namespaces UPnP (`urn:schemas-upnp-org:*`) ne doivent pas être modifiés
- Les codes d'erreur UPnP (401, 402, 501, etc.) ont une sémantique précise — ne pas les altérer

---

## Génération de tests

Lors de la génération de tests unitaires :

```csharp
// Pattern attendu pour les tests xUnit
public class MediaStreamSelectorTests
{
    private readonly Mock<ILogger<MediaStreamSelector>> _logger = new();
    private readonly MediaStreamSelector _sut; // System Under Test

    public MediaStreamSelectorTests()
    {
        _sut = new MediaStreamSelector(_logger.Object);
    }

    [Theory]
    [InlineData("fra", 0)]
    [InlineData("eng", 1)]
    public void SelectAudioStream_WithLanguage_ReturnsCorrectStream(
        string language, int expectedIndex)
    {
        // Arrange / Act / Assert pattern obligatoire
    }
}
```

---

## Génération de migrations EF Core

Si une entité est modifiée, proposer :
```bash
dotnet ef migrations add <MigrationName> --project Jellyfin.Data --startup-project Jellyfin.Server
dotnet ef migrations script --idempotent --output migration.sql
```

---

## Revue de code automatique (Copilot Review)

Lors d'une revue de PR, signaler prioritairement :
1. Méthodes async sans `CancellationToken`
2. `HttpClient` instancié manuellement
3. Modifications dans `DlnaProfiles/` ou `Emby.Server.Implementations/Dlna/` sans commentaire de test
4. Endpoints API manquant `[ProducesResponseType]`
5. Champs EF Core sans migration correspondante
6. Logging avec données potentiellement sensibles
