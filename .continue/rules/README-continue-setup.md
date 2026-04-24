# Installation Continue.dev + Mistral pour Jellyfin

## Structure à déployer dans ton fork

```
ton-fork-jellyfin/
└── .continue/
    ├── config.yaml                    ← config principale Continue + Mistral
    └── rules/
        ├── 01-jellyfin-architecture.md  ← contexte projet (toujours actif)
        ├── 02-dotnet-standards.md       ← règles C# (actif sur *.cs)
        ├── 03-dlna-critical.md          ← protection DLNA (actif sur fichiers Dlna/*)
        └── 04-typescript-web.md         ← règles TS/React (actif sur *.ts, *.tsx)
```

## Étape 1 — Installer Continue dans VS Code

```bash
code --install-extension Continue.continue
```
Ou via l'interface : Extensions → chercher "Continue" → Installer.

## Étape 2 — Configurer ta clé Mistral

### Option A : secrets Continue (recommandé)

Dans le fichier `~/.continue/.env` (créer si absent) :
```env
MISTRAL_API_KEY=ta_cle_mistral_ici
# Si tu as une clé Codestral séparée (optionnel, plus rapide pour autocomplete) :
# CODESTRAL_API_KEY=ta_cle_codestral_ici
```

### Option B : Variable d'environnement système

```bash
export MISTRAL_API_KEY="ta_cle_mistral_ici"
# Ajouter dans ~/.bashrc ou ~/.zshrc pour persister
```

## Étape 3 — Copier les fichiers dans ton fork

```bash
cd /chemin/vers/ton-fork-jellyfin

# Créer le dossier .continue
mkdir -p .continue/rules

# Copier les fichiers (adapter le chemin source)
cp config.yaml .continue/config.yaml
cp rules/*.md .continue/rules/
```

## Étape 4 — Vérifier la configuration

1. Ouvrir VS Code dans le dossier du fork
2. Panneau Continue (icône dans la barre latérale)
3. En haut à droite → sélectionner **"Mistral Large"** comme modèle chat
4. Taper `@codebase hello` → doit déclencher une indexation du repo

## Comportement des règles

| Règle | Déclenchement |
|-------|---------------|
| `01-jellyfin-architecture.md` | **Toujours** (alwaysApply: true) |
| `02-dotnet-standards.md` | Automatique quand un `.cs` est ouvert/référencé |
| `03-dlna-critical.md` | Automatique quand fichier `Dlna/**` ou `*Dlna*.cs` |
| `04-typescript-web.md` | Automatique quand `.ts` ou `.tsx` ouvert |

## Utilisation avec Mistral Local (Ollama)

Si tu veux utiliser Mistral en local via Ollama au lieu de l'API cloud :

```yaml
# Dans config.yaml, remplacer les entrées Mistral par :
models:
  - name: Mistral Local (Ollama)
    provider: ollama
    model: mistral-nemo  # ou mistral:7b, mixtral:8x7b
    # Pas de clé API nécessaire
    roles:
      - chat
      - edit
      - agent

  - name: Codestral Local
    provider: ollama
    model: codestral  # si disponible sur ta machine
    roles:
      - autocomplete
```

```bash
# Télécharger le modèle Mistral via Ollama
ollama pull mistral-nemo
ollama pull codestral
```

## Raccourcis clavier utiles

| Action | Raccourci |
|--------|-----------|
| Chat avec contexte fichier courant | `Ctrl+L` |
| Inline edit (sélection) | `Ctrl+I` |
| Ajouter fichier courant au contexte | `Option+Enter` (Mac) / `Alt+Enter` (Linux) |
| @codebase — recherche sémantique | `@codebase` dans le chat |
| @diff — diff git courant | `@diff` dans le chat |

## Prompts utiles pour Jellyfin avec Mistral

```
@codebase Comment fonctionne le pipeline de transcoding FFmpeg ?
@diff Vérifie que ce changement respecte les règles DLNA
@problems Corrige les erreurs de compilation en respectant les standards C# du projet
@codebase Où est implémenté le ContentDirectory UPnP ?
```
