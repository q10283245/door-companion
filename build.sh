#!/bin/bash
# ============================================================================
# panel/build.sh —— 构建 com.wb.carpanel 面板 APK（离线管线，无 Android SDK）
#
# 工具链：javac + d8（复用 gate 编译环境）、aapt2（Google maven osx 版）、
#         keytool/jarsigner（v1 签名；targetSdk 29 在 Android 11 上允许 v1）
# 产物：panel.apk
# ============================================================================
set -e
DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$DIR"

# 持久工具链：work/tools/build/（/tmp 会被系统清理，勿放 /tmp）
TOOLS="${DOOR_BUILD_TOOLS:-$DIR/.tools}"

# JDK 探测：允许外部 JDK_HOME 覆盖；逐个校验完整性。
# 2026-11-07：持久 JDK 的 lib/modules 曾被清理掉，java 会报
# "Failed setting boot class path"，只看目录存在会误判，必须实际跑一次。
pick_jdk() {
  for c in ${JDK_HOME:-} "$TOOLS"/jdk-17*/Contents/Home \
           /Applications/HBuilderX.app/Contents/HBuilderX/plugins/amazon-corretto \
           /Applications/PhpStorm.app/Contents/jbr/Contents/Home \
           /tmp/jdk-17*/Contents/Home; do
    [ -n "$c" ] || continue
    [ -x "$c/bin/javac" ] && [ -x "$c/bin/jarsigner" ] || continue
    "$c/bin/java" -version >/dev/null 2>&1 || continue
    echo "$c"
    return 0
  done
  return 1
}
JDK_HOME=$(pick_jdk) || { echo "JDK 未找到或已损坏（需 java/javac/jarsigner 且能启动 VM）"; exit 1; }
echo "JDK: $JDK_HOME"
ANDROID_JAR="$TOOLS/android-all.jar"
R8="$TOOLS/r8.jar"
AAPT2="$DIR/.aapt2/aapt2"
if [ ! -e "$AAPT2" ]; then
  mkdir -p "$DIR/.aapt2"
  unzip -o -q "$TOOLS/aapt2-osx.jar" -d "$DIR/.aapt2" 2>/dev/null || true
  B=$(find "$DIR/.aapt2" -name aapt2 -type f | head -1)
  [ -n "$B" ] && { [ "$B" = "$AAPT2" ] || cp "$B" "$AAPT2"; chmod +x "$AAPT2"; }
fi
for f in "$ANDROID_JAR" "$AAPT2" "$R8"; do [ -e "$f" ] || { echo "缺工具: $f"; exit 1; }; done

echo "== [1/5] keytool 签名密钥（已存在则跳过） =="
if [ ! -f panel.ks ]; then
  "$JDK_HOME/bin/keytool" -genkeypair -keystore panel.ks -alias panel \
    -storepass carpanel -keypass carpanel -keyalg RSA -keysize 2048 -validity 10000 \
    -dname "CN=DoorCompanion" 2>/dev/null
fi

echo "== [2/5] javac =="
python3 -c 'import shutil; shutil.rmtree("classes",ignore_errors=True); shutil.rmtree("dexout",ignore_errors=True)'
mkdir -p classes dexout
"$JDK_HOME/bin/javac" -encoding UTF-8 -source 11 -target 11 -nowarn -Xlint:none \
  -cp "$ANDROID_JAR" -d classes wb/*.java

echo "== [3/5] d8 → classes.dex =="
"$JDK_HOME/bin/java" -cp "$R8" com.android.tools.r8.D8 \
  --release --min-api 29 --lib "$ANDROID_JAR" \
  --output dexout $(find classes -name '*.class')

echo "== [4/5] aapt2 link → base.apk =="
python3 -c 'from pathlib import Path; Path("base.apk").unlink(missing_ok=True)'
"$AAPT2" link -o base.apk \
  -I "$ANDROID_JAR" \
  -A assets --manifest AndroidManifest.xml \
  --min-sdk-version 29 --target-sdk-version 29 \
  --auto-add-overlay

echo "== [5/5] 打包 classes.dex + v1 签名 =="
cp dexout/classes.dex .
zip -q -j base.apk classes.dex
python3 -c 'from pathlib import Path; Path("panel.apk").unlink(missing_ok=True)'
cp base.apk panel.apk
"$JDK_HOME/bin/jarsigner" -keystore panel.ks -storepass carpanel \
  -sigalg SHA256withRSA -digestalg SHA-256 panel.apk panel 2>&1

ls -la panel.apk
echo "BUILD_OK"

cp panel.apk door-assistant.apk
