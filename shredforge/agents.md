## ShredForge – Agent Guide

This repo contains the prototype for **ShredForge**, a desktop guitar–practice assistant.  
It is a single Maven module (`shredforge`) using **Swing + JCEF** (Chromium Embedded Framework) for the UI, which enables full Web Audio API support for AlphaTab playback.

| Package | Role |
| --- | --- |
| `com.shredforge` | Application entry point (`SwingApp.java`) and JCEF initialization. |
| `com.shredforge.ui` | Swing UI components. `MainFrame` is the main window with toolbar and JCEF browser panel. |
| `com.shredforge.core` | Domain façade, orchestration logic, and immutable DTOs (records). UI code should go through `ShredforgeRepository`, which wraps `ShredforgeFacade` and a `TabManager`. |
| `com.shredforge.tabs` | Access to remote songs plus GP file downloads. `TabManager` coordinates `TabGetService` (search/download) and `TabDataDao` (JCEF browser automation for GP downloads). Implements the `TabGateway` port used by the core façade. |
| `com.shredforge.tabview` | Converts `TabData` JSON into visual fragments (`FormattedTab`) that the UI can embed. `TabRenderingService` is the default `TabFormatter`. Also contains `SongsterrToAlphaTab` for JSON conversion. |
| `com.shredforge.scoring` | Audio analysis (TarsosDSP wrapper), detected-note models, and score calculation utilities that power `SessionScoringService` implementations. |
| `com.shredforge.calibration` | Lightweight tuning helpers: a simplified signal processor, preset tunings, and the default `CalibrationService` implementation used by the façade + UI calibration screen. |

### Typical Flow

1. UI calls `ShredforgeRepository`:
   - For discovery it delegates to `TabManager.searchSongs(TabSearchRequest)` and `TabManager.downloadGpFile(SongSelection)`.
   - For practice it pipes `TabData` through `ShredforgeFacade.formatTab` → `CalibrationService.calibrate` → `SessionScoringService.score`.
2. `TabManager` coordinates song search and GP file downloads:
   - Search returns `SongSelection` objects (song-level, containing all tracks).
   - Downloads are GP files via `TabDataDao.downloadGpFile()` using JCEF browser automation.
   - GP files are cached in `~/.shredforge/gp-files/`.
   
   **GP File Downloads:** The `TabDataDao` loads Songsterr in a hidden JCEF browser and clicks the download button to get actual Guitar Pro files. GP files contain all tracks for a song and work directly with AlphaTab.
3. Rendering is handled by `TabRenderingService`, which writes SVG fragments stored in `FormattedTab.svgFragments()`.
4. Scoring combines note detection (`scoring.detection.*`), parsing (`TabNoteParser`), and aggregation (`ScoreCalculator`) to produce `SessionResult`.

### Key Domain Objects

| Type | Notes |
| --- | --- |
| `SongRequest` | Identifies the desired track; reused across search, download, and persistence. |
| `TabData` | Canonical tab payload (sourceId + raw content + fetch metadata). Supports both JSON content and GP file paths. |
| `FormattedTab` | Tab ready for UI consumption (list of SVG strings, metadata). |
| `CalibrationInput` / `CalibrationProfile` | User/device gain and noise information. |
| `SessionRequest` / `SessionResult` | Inputs & outputs of the practice scoring pipeline. |
| `SongSelection` | Song-level selection from Songsterr search (contains all tracks). Used for GP file downloads. |

### Adapters & Ports

The core module defines ports so subsystems can be mocked easily:

* `TabGateway` – search/download/persist tabs (default: `TabManager`).
* `TabFormatter` – converts `TabData → FormattedTab` (default: `TabRenderingService`).
* `CalibrationService` – turns `CalibrationInput → CalibrationProfile` (demo implementation inside `ShredforgeRepository`).
* `SessionScoringService` – runs an evaluated session; there is a `MockSessionScoringService` helper for the UI.

`ShredforgeFacade.Builder` wires these ports together. Tests/agents can drop in alt implementations by supplying their own builders to `ShredforgeRepository`.

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

### Conventions & Tips

* **Records everywhere:** most DTOs are Java records—remember constructor validation blocks (see `SessionRequest`, `CalibrationInput`).
* **Null handling:** public APIs call `Objects.requireNonNull` aggressively; propagate optional data with `java.util.Optional`.
* **SVG rendering:** `com.shredforge.tabview.render` holds low-level drawing helpers (currently Songsterr-compatible).
* **Scoring:** Real-time detection uses TarsosDSP, so anything in `scoring.detection` may touch audio hardware—avoid running those pieces in CI unless mocked.
* **Demo utilities:** `ShredforgeRepository.runDemoSession()` exercises the full stack with canned data; useful for smoke tests.
* **JCEF browser:** The `MainFrame` embeds a Chromium browser via JCEF. Use `browser.executeJavaScript()` to interact with the AlphaTab page.

### How to Extend

1. **New data sources:** implement `TabGateway` or extend `TabGetService` to talk to a different provider, then wire via `ShredforgeFacade.Builder.withTabGateway`.
2. **Custom renderers:** provide another `TabFormatter` and inject it (same builder).
3. **Real scoring:** swap `SessionScoringService` with a concrete engine under `com.shredforge.scoring`.
4. **UI integration:** `MainFrame` shows how the Swing UI talks to `ShredforgeRepository`; keep interactions confined to the repository layer to avoid leaking infrastructure concerns upward.

### Useful Entry Points

* `ShredforgeRepository.builder()` – use in tests or tooling to bootstrap the system.
* `TabManager.createDefault()` – spins up DAO-backed search/save stack without external wiring.
* `TabRenderingService` – stateless; safe to reuse as a singleton.
* `ScoreCalculator.generateReport()` – produces `ScoreReport` objects you can display or persist.

Keep this file up to date as APIs move—future agents will thank you!
