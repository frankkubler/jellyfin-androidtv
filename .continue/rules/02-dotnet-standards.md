---
name: .NET 8 Standards
description: Règles de codage C# pour ce projet
globs: ["**/*.cs", "**/*.csproj"]
alwaysApply: false
---

# Standards C# / .NET 8

## Règles impératives

- **Nullable activé** : typer correctement, jamais de `!` pour supprimer un warning sans justification
- **async/await systématique** : jamais `.Result`, `.Wait()`, `.GetAwaiter().GetResult()`
- **CancellationToken** : dernier paramètre de toute méthode async publique
- **ILogger<T>** : logging structuré uniquement — pas d'interpolation dans le message de log
  ```csharp
  // ✅ Correct
  _logger.LogInformation("Transcoding item {ItemId} with codec {Codec}", itemId, codec);
  // ❌ Incorrect
  _logger.LogInformation($"Transcoding {itemId}");
  ```
- **Injection constructeur** : jamais ServiceLocator, jamais `IServiceProvider.GetService()` dans le code métier
- **ArgumentNullException.ThrowIfNull()** : validation en entrée de toute méthode publique
- **sealed** sur les classes non destinées à l'héritage
- **record** pour les DTOs immuables

## Pattern contrôleur API
```csharp
[HttpGet("{itemId}")]
[ProducesResponseType(typeof(MediaStreamDto), StatusCodes.Status200OK)]
[ProducesResponseType(StatusCodes.Status404NotFound)]
[Authorize(Policy = "DefaultAuthorization")]
public async Task<ActionResult<MediaStreamDto>> GetStream(
    [FromRoute] Guid itemId,
    CancellationToken cancellationToken)
{
    var item = await _libraryManager.GetItemByIdAsync(itemId, cancellationToken);
    return item is null ? NotFound() : Ok(_mapper.Map<MediaStreamDto>(item));
}
```

## Validation obligatoire avant commit
```bash
dotnet build --configuration Release  # 0 warning
dotnet format --verify-no-changes     # style enforced
dotnet test --configuration Release   # 0 échec
```
