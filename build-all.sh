#!/bin/bash
# Builds PlayerFinder for every supported Minecraft version into dist/.
cd "$(dirname "$0")" || exit 1
mkdir -p dist

build() {
  local mc="$1" api="$2" modmenu="$3"
  echo "=== building $mc (fabric-api $api, modmenu $modmenu) ==="
  ./gradlew build --no-daemon --console=plain \
      -Pminecraft_version="$mc" -Pfabric_api_version="$api" -Pmodmenu_version="$modmenu" \
      > "/tmp/playerfinder-$mc.log" 2>&1
  if [ $? -eq 0 ]; then
    cp build/libs/playerfinder-*+"$mc".jar "dist/" && echo "  ✅ built $mc -> $(ls dist/playerfinder-*+"$mc".jar 2>/dev/null | xargs -n1 basename)"
  else
    echo "  ❌ BUILD FAILED ($mc):"
    grep -E "error:|: error|FAILED|Exception" "/tmp/playerfinder-$mc.log" | head -20
  fi
  # clear per-version outputs so jars don't leak between versions
  rm -rf build/libs
}

# Allow building a subset:  ./build-all.sh 1.21.11
#      MC        fabric-api          modmenu
if [ $# -gt 0 ]; then
  for v in "$@"; do
    case "$v" in
      1.21.4)  build 1.21.4  0.119.2+1.21.4   13.0.4 ;;
      1.21.10) build 1.21.10 0.138.4+1.21.10  16.0.1 ;;
      1.21.11) build 1.21.11 0.141.4+1.21.11  17.0.0 ;;
      *) echo "unknown version: $v" ;;
    esac
  done
else
  build 1.21.4  0.119.2+1.21.4   13.0.4
  build 1.21.10 0.138.4+1.21.10  16.0.1
  build 1.21.11 0.141.4+1.21.11  17.0.0
fi

echo ""; echo "=== dist/ ==="; ls -la dist/*.jar 2>/dev/null
