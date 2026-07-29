#!/bin/bash
# Regenerates poster.png (512x512) and preview.png (256x256) from the Glas Launcher's
# shield.png brand asset. Re-run this after copying in an updated shield-source.png
# (from ~/Desktop/GlasLauncher/src/GlasLauncher.App/Assets/shield.png) if the launcher's
# branding ever changes -- these two derived files are not otherwise kept in sync
# automatically. Uses only sips (macOS built-in), no new dependency.
set -euo pipefail
cd "$(dirname "$0")"

sips -s format png -Z 512 shield-source.png --out shield-512-tmp.png
sips --padToHeightWidth 512 512 --padColor 0d1f16 shield-512-tmp.png --out poster.png
rm shield-512-tmp.png

sips -s format png -Z 256 shield-source.png --out shield-256-tmp.png
sips --padToHeightWidth 256 256 --padColor 0d1f16 shield-256-tmp.png --out preview.png
rm shield-256-tmp.png

echo "Regenerated poster.png (512x512) and preview.png (256x256) from shield-source.png."
