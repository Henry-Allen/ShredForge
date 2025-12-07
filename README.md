# ShredForge 🎸

A desktop guitar practice assistant that helps you learn songs, practice with real-time scoring feedback, and tune your guitar.

## Features

- **Tab Browser** – Search and download Guitar Pro files from Songsterr
- **Interactive Playback** – View and play tabs with AlphaTab rendering
- **Practice Mode** – Play along with tabs and receive real-time accuracy scoring
- **Tuning Mode** – Tune your guitar string-by-string with visual pitch feedback
- **Modern UI** – Dark-themed Swing interface with embedded Chromium browser

## Requirements

- **Java 17** or higher (JDK required for building)
- **Maven 3.6+**
- **Audio input device** (microphone or audio interface) for practice and tuning modes
- **Internet connection** for first run (downloads ~200MB of browser engine files)

## Quick Start

### Windows

```batch
cd shredforge
run.bat
```

### macOS / Linux

```bash
cd shredforge
chmod +x run.sh
./run.sh
```

## Building from Source

### 1. Clone the Repository

```bash
git clone https://github.com/Henry-Allen/ShredForge.git
cd ShredForge/shredforge
```

### 2. Compile

```bash
mvn compile
```

### 3. Run

**Windows:**
```batch
run.bat
```

**macOS / Linux:**
```bash
./run.sh
```

### Manual Run (if scripts don't work)

```bash
# Compile first
mvn -DskipTests compile

# Get classpath and run
java \
  --add-opens java.desktop/sun.awt=ALL-UNNAMED \
  --add-opens java.desktop/java.awt=ALL-UNNAMED \
  --add-opens java.desktop/sun.lwawt=ALL-UNNAMED \
  --add-opens java.desktop/sun.lwawt.macosx=ALL-UNNAMED \
  --enable-native-access=ALL-UNNAMED \
  -cp "target/classes:$(mvn -q dependency:build-classpath -Dmdep.outputFile=/dev/stdout)" \
  com.shredforge.SwingApp
```

## First Run

On first launch, ShredForge will:
1. Display a splash screen while initializing
2. Download JCEF (Chromium Embedded Framework) binaries (~200MB) to `~/.shredforge/jcef/`
3. Launch the main application window

This download only happens once. Subsequent launches will be faster.

## Usage

### Browsing & Loading Tabs

1. Use the search bar to find songs by title or artist
2. Click on a song to download and view the Guitar Pro file
3. Use the playback controls to play/pause and navigate the tab

### Practice Mode

1. Load a tab you want to practice
2. Select your audio input device from the dropdown
3. Click **Practice** to start a practice session
4. Play along with the tab – your accuracy is scored in real-time
5. Click **Stop** to end the session and see your final score

**Scoring:**
- **Green** (≥90%) – Excellent
- **Yellow** (≥70%) – Good
- **Red** (<70%) – Needs practice

### Tuning Mode

1. Load a tab (the tuning will match the track's tuning)
2. Click **Tune** to open the tuning dialog
3. Play each string and watch the pitch meter
4. Hold the string in tune for 0.5 seconds to confirm
5. Navigate between strings with Previous/Next or click directly

## Data Storage

ShredForge stores data in your home directory:

| Location | Contents |
|----------|----------|
| `~/.shredforge/jcef/` | Chromium browser engine (~200MB) |
| `~/.shredforge/gp-files/` | Downloaded Guitar Pro files |

## Creating Installers (Optional)

### Windows Installer (.exe)

```bash
mvn package -Pwindows-installer -Dskip.installer=false
```

Output: `target/jpackage/ShredForge.exe`

### macOS App Bundle

```bash
mvn package -Pmac-installer -Dskip.installer=false
```

Output: `target/jpackage/ShredForge.app`

> **Note:** Creating installers requires JDK 17+ with `jpackage` tool available.

## Troubleshooting

### "Failed to initialize browser"
- Ensure you have a stable internet connection for the first run
- Check that `~/.shredforge/jcef/` has write permissions
- Try deleting `~/.shredforge/jcef/` and restarting

### No audio input devices detected
- Ensure your microphone/audio interface is connected
- Grant microphone permissions to Java if prompted by your OS

### Application won't start
- Verify Java 17+ is installed: `java -version`
- Verify Maven is installed: `mvn -version`
- Check for error messages in the terminal

## Tech Stack

- **Java 17** – Core language
- **Swing + FlatLaf** – Modern dark-themed UI
- **JCEF** – Chromium Embedded Framework for AlphaTab rendering
- **TarsosDSP** – Real-time audio analysis and pitch detection
- **AlphaTab** – Guitar Pro file rendering (embedded in browser)
- **Maven** – Build and dependency management

## License

This project is for educational purposes as part of CS370.
