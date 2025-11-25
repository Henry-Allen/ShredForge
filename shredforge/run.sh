#!/bin/bash
# Run ShredForge with JCEF
cd "$(dirname "$0")"

# Build if needed
mvn -q -DskipTests compile

# Run with required JVM args for JCEF
java \
  --add-opens java.desktop/sun.awt=ALL-UNNAMED \
  --add-opens java.desktop/java.awt=ALL-UNNAMED \
  --add-opens java.desktop/sun.lwawt=ALL-UNNAMED \
  --add-opens java.desktop/sun.lwawt.macosx=ALL-UNNAMED \
  -cp "target/classes:$(mvn -q dependency:build-classpath -Dmdep.outputFile=/dev/stdout)" \
  com.shredforge.SwingApp
