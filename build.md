# ShredForge — Build, Run, and Package (macOS)

This project is a JavaFX app built with Maven. These instructions are tailored for macOS on Apple Silicon (arm64) and JDK 17+.

## Prerequisites
- JDK 17 or newer (JDK 24 works too and includes jpackage)
- Maven 3.8+
- macOS Apple Silicon (dependencies use mac-aarch64 JavaFX classifiers)

Verify Java:

```
java -version
```

If you need to switch to JDK 17+ for this shell:

```
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
export PATH="$JAVA_HOME/bin:$PATH"
```

## Project layout
- App module: `shredforge/`
- Main class: `com.shredforge.App`
- POM: `shredforge/pom.xml`

## Install dependencies and build JAR

```
mvn -f shredforge/pom.xml clean package
```
- Output JAR: `shredforge/target/shredforge-1.jar`
- Note: This JAR is thin and not directly runnable without JavaFX on the module path.

## Run during development (Maven)

```
mvn -f shredforge/pom.xml javafx:run
```

## Create a runtime image (bundles Java + JavaFX)

```
mvn -f shredforge/pom.xml javafx:jlink
```
- Launcher produced at: `shredforge/target/image/bin/ShredForge`

## Package a macOS .app using jpackage (manual)
1) Ensure a runtime image exists (see previous step)
2) Run jpackage (ships with JDK 17+):

```
jpackage \
  --type app-image \
  --name ShredForge \
  --app-version 1 \
  --input shredforge/target \
  --main-jar shredforge-1.jar \
  --main-class com.shredforge.App \
  --runtime-image shredforge/target/image \
  --dest shredforge/target/jpackage \
  --java-options '--enable-native-access=javafx.graphics'
```

- Resulting app: `shredforge/target/jpackage/ShredForge.app`
- Open it:

```
open shredforge/target/jpackage/ShredForge.app
```

Gatekeeper: On first launch, macOS may block the app. Right‑click the app → Open → confirm.

## One‑command packaging via Maven profile (recommended)
The POM includes a mac-only profile that wires `javafx:jlink` into the package phase and invokes `jpackage` via the Exec Maven Plugin.

Build everything and produce `ShredForge.app` in one command:

```
mvn -P mac-app-image clean package -f shredforge/pom.xml
```
- Outputs:
  - Runtime image: `shredforge/target/image`
  - App bundle: `shredforge/target/jpackage/ShredForge.app`

## Troubleshooting
- Error: No main manifest attribute when running the JAR
  - The JAR is thin. Use `mvn javafx:run`, the runtime image launcher, or the .app as shown above.

- Error: Could not find JavaFX classes (NoClassDefFoundError)
  - Use `mvn javafx:run`, or build the runtime image and .app to bundle JavaFX.

- jpackage not found
  - Ensure you are on JDK 17+; `jpackage --version` should work. On macOS, it’s included with the JDK.

- App not found after jpackage
  - Check `shredforge/target/jpackage/` or re-run with `--verbose`:

```
jpackage --verbose ...
```

## Cleaning builds

```
mvn -f shredforge/pom.xml clean
```

## Notes
- JavaFX dependencies in the POM use the `mac-aarch64` classifier for Apple Silicon.
- The `mac-app-image` profile uses the system `jpackage` to avoid plugin resolution issues.
