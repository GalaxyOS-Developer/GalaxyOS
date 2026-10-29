#!/usr/bin/env bash
set -euo pipefail

cd "$(git rev-parse --show-toplevel)"

echo "=== GalaxyOS: controllo prima della pubblicazione ==="
echo
git status --short --ignored=no
echo

if [[ -z "$(git status --porcelain)" ]]; then
    echo "Nessuna modifica da pubblicare."
    exit 0
fi

echo "Controlla i file elencati sopra."
read -r -p "Vuoi vedere il diff delle modifiche tracciate? [s/N] " mostra

if [[ "$mostra" =~ ^[sS]$ ]]; then
    git diff
fi

echo
read -r -p "Vuoi aggiungere le modifiche e preparare il commit? [s/N] " conferma

if [[ ! "$conferma" =~ ^[sS]$ ]]; then
    echo "Operazione annullata."
    exit 0
fi

git add -A
git diff --cached --check
git status --short

echo
echo "File preparati per il commit:"
git diff --cached --stat
echo

read -r -p "Confermi commit e pubblicazione su origin/main? [s/N] " pubblica

if [[ ! "$pubblica" =~ ^[sS]$ ]]; then
    echo "Pubblicazione annullata. I file restano preparati in Git."
    exit 0
fi

git commit -m "Aggiorna GalaxyOS Launcher"
git push origin main

echo
echo "Pubblicazione completata."
