---
name: TypeScript / React Web Client
description: Standards pour jellyfin-web
globs: ["**/*.ts", "**/*.tsx", "**/*.js", "**/*.jsx"]
alwaysApply: false
---

# Standards TypeScript / React (jellyfin-web)

- **strict mode** activé dans tsconfig — pas de `any`, utiliser `unknown` + type-guard
- **Composants fonctionnels** uniquement, hooks React (`useState`, `useEffect`, `useMemo`, `useCallback`)
- **Appels API** : uniquement via le SDK `@jellyfin/sdk` — pas de `fetch()` direct vers `/api`
- **Gestion d'erreurs** : toujours `.catch()` ou `try/catch` sur les appels async
- **Types du SDK** : importer depuis `@jellyfin/sdk/lib/generated-client/models`

```typescript
// ✅ Correct
import { Api } from '@jellyfin/sdk';
import type { BaseItemDto } from '@jellyfin/sdk/lib/generated-client/models';

// ❌ Incorrect
const response = await fetch('/api/Items/' + itemId);
```
