#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SOURCE_DIR="$ROOT_DIR/resources/original/images"
OUTPUT_DIR="$ROOT_DIR/resources/images"
QUALITY=82
FORCE=0

usage() {
    cat <<EOF
Usage: bash scripts/png-to-webp.sh [options]

Convert all PNG/JPG/JPEG files under resources/original/images to WebP files
under resources/images, preserving the directory structure.

Options:
  -q, --quality <1-100>  WebP quality. Default: $QUALITY
  -f, --force            Overwrite existing .webp files
  -h, --help             Show this help
EOF
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        -q|--quality)
            if [[ $# -lt 2 ]]; then
                echo "Missing value for $1" >&2
                exit 1
            fi
            QUALITY="$2"
            shift 2
            ;;
        -f|--force)
            FORCE=1
            shift
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        *)
            echo "Unknown option: $1" >&2
            usage >&2
            exit 1
            ;;
    esac
done

if [[ ! "$QUALITY" =~ ^[0-9]+$ ]] || (( QUALITY < 1 || QUALITY > 100 )); then
    echo "Quality must be an integer from 1 to 100." >&2
    exit 1
fi

if [[ ! -d "$SOURCE_DIR" ]]; then
    echo "Source directory not found: $SOURCE_DIR" >&2
    exit 1
fi

CONVERTER=""
if command -v cwebp >/dev/null 2>&1; then
    CONVERTER="cwebp"
elif command -v img2webp >/dev/null 2>&1; then
    CONVERTER="img2webp"
elif command -v magick >/dev/null 2>&1; then
    CONVERTER="magick"
elif command -v convert >/dev/null 2>&1; then
    CONVERTER="convert"
else
    cat >&2 <<EOF
No WebP converter found.

Install one of:
  Ubuntu/Debian: sudo apt install webp
  macOS:         brew install webp

Then rerun:
  bash scripts/png-to-webp.sh
EOF
    exit 1
fi

converted=0
skipped=0

while IFS= read -r -d '' source_file; do
    relative_path="${source_file#"$SOURCE_DIR"/}"
    output_file="$OUTPUT_DIR/${relative_path%.*}.webp"

    mkdir -p "$(dirname "$output_file")"

    if [[ -f "$output_file" && "$FORCE" -ne 1 ]]; then
        echo "[skip] ${output_file#"$ROOT_DIR"/}"
        skipped=$((skipped + 1))
        continue
    fi

    echo "[webp] ${source_file#"$ROOT_DIR"/} -> ${output_file#"$ROOT_DIR"/}"
    case "$CONVERTER" in
        cwebp)
            cwebp -quiet -q "$QUALITY" "$source_file" -o "$output_file"
            ;;
        img2webp)
            img2webp -quiet -q "$QUALITY" "$source_file" -o "$output_file"
            ;;
        magick)
            magick "$source_file" -quality "$QUALITY" "$output_file"
            ;;
        convert)
            convert "$source_file" -quality "$QUALITY" "$output_file"
            ;;
    esac

    converted=$((converted + 1))
done < <(find "$SOURCE_DIR" -type f \( -iname '*.png' -o -iname '*.jpg' -o -iname '*.jpeg' \) -print0 | sort -z)

echo ""
echo "Done. Converted: $converted, skipped: $skipped"
echo "Output: ${OUTPUT_DIR#"$ROOT_DIR"/}"
