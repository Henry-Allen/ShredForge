## ShredForge – Agent Guide

This repo contains the prototype for **ShredForge**, a desktop guitar–practice assistant.  
It is a single Maven module (`shredforge`) using **Swing + JCEF** (Chromium Embedded Framework) for the UI, which enables full Web Audio API support for AlphaTab playback.

| Package | Role |
| --- | --- |
| `com.shredforge` | Application entry point (`SwingApp.java`) and JCEF initialization. |
| `com.shredforge.ui` | Swing UI components. `MainFrame` is the main window with toolbar and JCEF browser panel. |
| `com.shredforge.core` | Repository layer and immutable DTOs (records). UI code should go through `ShredforgeRepository`. |
| `com.shredforge.tabs` | Access to remote songs plus GP file downloads. `TabManager` coordinates `TabGetService` (search/download) and `TabDataDao` (HTTP downloads). |
| `com.shredforge.scoring` | Audio analysis (TarsosDSP wrapper) and real-time practice scoring. Includes `LivePracticeScoringService` for real-time practice mode and `CqtNoteDetectionEngine` for pitch detection. |
| `com.shredforge.calibration` | Tuning: `GuitarTunerService` for real-time pitch detection during tuning, `TuningSession` for managing string-by-string tuning flow, `TuningPreset`/`TuningLibrary` for preset tunings. |

### Typical Flow

1. UI calls `ShredforgeRepository`:
   - For discovery it delegates to `TabManager.searchSongs(TabSearchRequest)` and `TabManager.downloadGpFile(SongSelection)`.
   - For practice it uses `LivePracticeScoringService` to compare detected notes against expected notes from AlphaTab.
   - For tuning it uses `GuitarTunerService` with YIN pitch detection.
2. `TabManager` coordinates song search and GP file downloads:
   - Search returns `SongSelection` objects (song-level, containing all tracks).
   - Downloads are GP files via HTTP to Songsterr's API.
   - GP files are cached in `~/.shredforge/gp-files/`.
3. AlphaTab (embedded in JCEF browser) renders GP files and provides note extraction for scoring.

### Key Domain Objects

| Type | Notes |
| --- | --- |
| `TabData` | Represents a downloaded GP file (sourceId, title, artist, gpFilePath). |
| `SongSelection` | Song-level selection from Songsterr search (contains all tracks). Used for GP file downloads. |
| `ExpectedNote` | A note expected from the tab at a specific time, extracted from AlphaTab. |
| `LiveScoreSnapshot` | Real-time scoring state with overall and partial accuracy metrics. |
| `PracticeConfig` | Configuration for practice sessions (audio device, tolerances, confidence). |
| `AudioDeviceInfo` | Represents an available audio input device for practice mode. |
| `TuningSession` | Manages string-by-string tuning flow with target frequencies and progress tracking. |
| `TuningString` | Single guitar string with target note name, frequency, and MIDI number. |
| `TuningPreset` | Named tuning preset with frequencies for all strings (e.g., Standard, Drop D). |

### Services

* `PracticeScoringService` – real-time practice scoring that syncs with AlphaTab playback (default: `LivePracticeScoringService`).
* `GuitarTunerService` – real-time pitch detection for tuning using TarsosDSP's YIN algorithm.
* `TabManager` – coordinates song search and GP file downloads.

### Build & Run

```bash
cd shredforge
mvn -q -DskipTests compile      # fast compile / validation
./run.sh                        # launches the Swing + JCEF UI
```

Or run manually with required JVM arguments:
```bash
java \
  --add-opens java.desktop/sun.awt=ALL-UNNAMED \
  --add-opens java.desktop/java.awt=ALL-UNNAMED \
  --add-opens java.desktop/sun.lwawt=ALL-UNNAMED \
  --add-opens java.desktop/sun.lwawt.macosx=ALL-UNNAMED \
  -cp "target/classes:$(mvn -q dependency:build-classpath -Dmdep.outputFile=/dev/stdout)" \
  com.shredforge.SwingApp
```

* Java 17+ is required (project uses records & modern APIs).
* `target/` is gitignored; no custom build tooling beyond Maven.
* JCEF binaries are downloaded to `~/.shredforge/jcef` on first run (~200MB).

### Practice Mode

Practice mode allows users to play along with a tab and receive real-time scoring feedback:

1. **Note Extraction**: When a GP file is loaded in AlphaTab, JavaScript functions in `index.html` extract all notes with timing information via `window.extractNotesForScoring()`.

2. **Audio Detection**: `CqtNoteDetectionEngine` uses TarsosDSP with spectral analysis (FFT-based, optimized for guitar frequencies 65-1319 Hz) to detect played notes from the selected audio input device.

3. **Scoring**: `LivePracticeScoringService` compares detected notes against expected notes in real-time:
   - **Overall Accuracy**: Hits / Total notes in song (final score if played completely)
   - **Partial Accuracy**: Hits / Notes encountered so far (how well you're doing on what you've played)
   - Configurable pitch tolerance (cents) and timing tolerance (ms)

4. **UI Integration**: `MainFrame` provides:
   - Audio device selector dropdown
   - Practice button to start/stop sessions
   - Live score display with color coding (green ≥90%, yellow ≥70%, red <70%)
   - Final results dialog with detailed statistics

**Practice Mode API** (via `ShredforgeRepository`):
```java
repository.listAudioDevices();                    // Get available audio inputs
repository.loadExpectedNotes(notes, durationMs);  // Load notes from AlphaTab
repository.startPracticeSession(config, listener); // Start with callback
repository.updatePlaybackPosition(positionMs);    // Sync with AlphaTab playback
repository.stopPracticeSession();                 // Stop and get final score
```

### Tuning Mode

Tuning mode allows users to tune their guitar to match the tuning of the currently selected track:

1. **Tuning Extraction**: When the user clicks "Tune", JavaScript functions in `index.html` extract the tuning information from the current track via `window.extractTrackTuning()`. This returns MIDI note numbers for each string.

2. **Pitch Detection**: `GuitarTunerService` uses TarsosDSP's YIN pitch detection algorithm (same as the working prototype) to detect the pitch of played notes in real-time. YIN is more accurate than FFT peak-finding for monophonic pitch detection.

3. **Tuning Session**: `TuningSession` manages the string-by-string tuning flow:
   - Guides user through each string sequentially (low E to high E - the natural tuning order)
   - Tracks which strings have been tuned
   - Provides cents deviation from target pitch
   - Requires holding in-tune for 500ms to confirm

4. **UI Integration**: `MainFrame` provides:
   - Tune button in toolbar to start/stop tuning mode
   - Tuning dialog with real-time pitch meter
   - Visual feedback: green (in tune), yellow (almost), red (flat/sharp)
   - String selector buttons to jump to any string
   - Previous/Next navigation

**Tuning Mode API** (via `ShredforgeRepository`):
```java
repository.createTuningSession(tuningName, midiNotes);  // Create from MIDI notes
repository.createTuningSession(preset);                  // Create from TuningPreset
repository.createStandardTuningSession();                // Standard EADGBE
repository.startTuning(session, audioDevice, listener);  // Start with callback
repository.tuningNextString();                           // Advance to next string
repository.tuningPreviousString();                       // Go back
repository.stopTuning();                                 // Stop tuning session
```

### Conventions & Tips

* **Records everywhere:** most DTOs are Java records—remember constructor validation blocks.
* **Null handling:** public APIs call `Objects.requireNonNull` aggressively; propagate optional data with `java.util.Optional`.
* **Scoring:** Real-time detection uses TarsosDSP, so anything in `scoring.detection` may touch audio hardware—avoid running those pieces in CI unless mocked.
* **JCEF browser:** The `MainFrame` embeds a Chromium browser via JCEF. Use `browser.executeJavaScript()` to interact with the AlphaTab page.

### How to Extend

1. **New data sources:** extend `TabGetService` to talk to a different provider.
2. **Custom scoring:** implement `PracticeScoringService` interface for alternative scoring logic.
3. **UI integration:** `MainFrame` shows how the Swing UI talks to `ShredforgeRepository`; keep interactions confined to the repository layer.

### Useful Entry Points

* `ShredforgeRepository.builder()` – use in tests or tooling to bootstrap the system.
* `TabManager.createDefault()` – spins up DAO-backed search/save stack without external wiring.

Keep this file up to date as APIs move—future agents will thank you!
