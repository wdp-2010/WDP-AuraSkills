#!/bin/bash

# Quick menu loading test script
# This script builds the project and checks for menu loading issues

set -e

echo "=== AuraSkills Menu Loading Test ==="
echo "Building project..."

cd "$(dirname "$0")"

# Clean build
./gradlew clean build -q

echo "✅ Build successful"

# Extract the JAR to check menu files
BUILD_DIR="build/test-extract"
rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR"

echo "📁 Extracting JAR to check menu files..."
JAR_FILE=$(ls bukkit/build/libs/AuraSkills-*.jar | head -1)
if [ ! -f "$JAR_FILE" ]; then
    echo "❌ No JAR file found in bukkit/build/libs/"
    exit 1
fi

cd "$BUILD_DIR"
jar -xf "../../$JAR_FILE"

echo "📋 Menu files found in JAR:"
find . -name "*.yml" -path "*/menus/*" | sort

echo ""
echo "🔍 Checking for menu loading issues..."

# Check for common YAML syntax issues and problematic placeholders
echo "Checking for problematic template materials..."
for file in $(find . -name "*.yml" -path "*/menus/*"); do
    if grep -q "material: '{material}'" "$file"; then
        echo "❌ Found {material} placeholder in $file"
    elif grep -q "material:.*{.*}" "$file"; then
        echo "⚠️  Found placeholder material in $file: $(grep "material:" "$file" | head -1)"
    fi
done

echo ""
echo "🧪 Testing MenuFileManager MENU_NAMES array..."

# Extract MenuFileManager and check MENU_NAMES
MENU_MANAGER_FILE="../../bukkit/src/main/java/dev/aurelium/auraskills/bukkit/menus/MenuFileManager.java"
if [ -f "$MENU_MANAGER_FILE" ]; then
    echo "📊 MENU_NAMES array contains:"
    # Extract the full array definition across multiple lines
    sed -n '/MENU_NAMES.*=/,/};/p' "$MENU_MANAGER_FILE" | grep '"' | sed 's/.*"\([^"]*\)".*/  - \1/'
    
    EXPECTED_COUNT=$(sed -n '/MENU_NAMES.*=/,/};/p' "$MENU_MANAGER_FILE" | grep -c '".*"')
    ACTUAL_FILES=$(find . -name "*.yml" -path "*/menus/*" | wc -l)
    
    echo ""
    echo "📈 Statistics:"
    echo "  Expected menus in MENU_NAMES: $EXPECTED_COUNT"
    echo "  Actual menu files in JAR: $ACTUAL_FILES"
    
    if [ "$EXPECTED_COUNT" -eq "$ACTUAL_FILES" ]; then
        echo "✅ Menu count matches!"
    else
        echo "❌ Menu count mismatch!"
    fi
else
    echo "❌ Could not find MenuFileManager.java"
fi

cd ..
rm -rf "$BUILD_DIR"

echo ""
echo "🚀 Test complete! You can now run: ./gradlew :bukkit:runServer"
echo "   Look for: '[AuraSkills] Loaded X menus' in the server log"
echo "   Expected: 12 menus should load"