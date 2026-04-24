---
name: DLNA/UPnP Critical Zone
description: Règles de protection pour la stack DLNA/UPnP
globs: ["**/Dlna/**", "**/DlnaProfiles/**", "**/Ssdp/**", "**/*Dlna*.cs", "**/*Upnp*.cs"]
alwaysApply: false
---

# ⚠️ Zone DLNA/UPnP — Critique

Les modifications dans cette zone peuvent casser des appareils DLNA en production
(TV Samsung, LG, Sony, Xbox, PS4/5, AVRs Denon/Marantz).

## Règles strictes

1. **XML SOAP UPnP** : Les réponses doivent être strictement conformes UPnP 1.0/1.1.
   Ne jamais ajouter d'attributs non standard sans namespace propre.

2. **Headers HTTP DLNA** (casse exacte requise) :
   ```
   transferMode.dlna.org: Streaming
   contentFeatures.dlna.org: DLNA.ORG_PN=AVC_MP4_HP_HD_AAC;DLNA.ORG_OP=01
   Content-Type: video/mpeg
   ```

3. **DlnaProfiles/*.xml** : Chaque profil correspond à des appareils testés manuellement.
   Modifier = risque de régression. Toujours copier un profil existant comme base.

4. **ContentDirectory Browse** : Respecter la pagination (StartingIndex/RequestedCount).
   Count retourné ne doit JAMAIS dépasser RequestedCount.

5. **AVTransport FSM** : États valides selon spec UPnP AV 1.0 uniquement :
   `STOPPED` → `TRANSITIONING` → `PLAYING` → `PAUSED_PLAYBACK` → `PLAYING`

6. **SSDP** : Ne pas modifier les intervalles d'annonce (1800s par défaut).

## Test obligatoire si modification DLNA
```bash
dotnet test tests/Jellyfin.Dlna.Tests/ --filter "Category=DLNA"
dotnet test tests/Jellyfin.Integration.Tests/ --filter "Category=UPnP"
```

## Appareils à valider manuellement
Samsung Smart TV, LG webOS, Sony Bravia, Xbox One, PS4/PS5, Kodi, VLC, BubbleUPnP
