---
name: Jellyfin Architecture
description: Contexte architectural du projet Jellyfin
alwaysApply: true
---

# Contexte Jellyfin

Ce projet est un fork de **Jellyfin**, serveur multimédia open-source.

## Stack technique
- **Backend** : C# 12 / .NET 8, ASP.NET Core, Entity Framework Core (SQLite)
- **API** : REST/JSON + WebSocket, spec OpenAPI générée depuis les attributs
- **Clients** : TypeScript/React (web), Kotlin (Android), React Native (iOS)
- **Protocoles** : DLNA 1.5, UPnP AV 1.0/1.1, HLS, MPEG-DASH, RTSP

## Structure des dossiers clés
```
Jellyfin.Server/           → Point d'entrée, DI, hôte
Jellyfin.Api/Controllers/  → Endpoints REST (ASP.NET Core)
MediaBrowser.Controller/   → Interfaces métier (ILibraryManager, etc.)
Emby.Server.Implementations/ → Implémentations concrètes
MediaBrowser.Model/        → DTOs, enums, modèles purs
Emby.Server.Implementations/Dlna/ → ⚠️ DLNA critique
DlnaProfiles/*.xml         → ⚠️ Profils appareils — ne pas modifier sans test
tests/                     → Tests xUnit
```

## Règle de couches
```
Model (DTOs) → Controller (interfaces) → Implementation → Api (contrôleurs)
```
Ne pas créer de dépendance inverse (l'API ne doit pas référencer les implémentations directement).
