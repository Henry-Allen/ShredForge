## ShredForge – Agent Guide

This repo contains the prototype for **ShredForge**, a desktop guitar–practice assistant.  
It is a single Maven/JavaFX module (`shredforge`) that stitches together four main subsystems:

| Package | Role |
| --- | --- |
| `com.shredforge.core` | Domain façade, orchestration logic, and immutable DTOs (records). UI code should go through `ShredforgeRepository`, which wraps `ShredforgeFacade` and a `TabManager`. |
| `com.shredforge.tabs` | Access to remote tabs plus local persistence. `TabManager` coordinates `TabGetService` (search/download) and `TabSaveService` (cache). Implements the `TabGateway` port used by the core façade. |
| `com.shredforge.tabview` | Converts `TabData` JSON into visual fragments (`FormattedTab`) that the UI can embed. `TabRenderingService` is the default `TabFormatter`. |
| `com.shredforge.scoring` | Audio analysis (TarsosDSP wrapper), detected-note models, and score calculation utilities that power `SessionScoringService` implementations. |
| `com.shredforge.calibration` | Lightweight tuning helpers: a simplified signal processor, preset tunings, and the default `CalibrationService` implementation used by the façade + UI calibration screen. |

### Typical Flow

1. UI calls `ShredforgeRepository`:
   - For discovery it delegates to `TabManager.searchTabs(TabSearchRequest)` and `TabManager.downloadSelection(TabSelection)`.
   - For practice it pipes `TabData` through `ShredforgeFacade.formatTab` → `CalibrationService.calibrate` → `SessionScoringService.score`.
2. `TabManager` keeps track of both remote selections and saved copies, letting you
   - search (`TabGetService.search` → Songsterr style API stub),
   - download (`TabGetService.download`),
   - persist (`TabSaveService.save` / `TabDataDao`).
3. Rendering is handled by `TabRenderingService`, which writes SVG fragments stored in `FormattedTab.svgFragments()`.
4. Scoring combines note detection (`scoring.detection.*`), parsing (`TabNoteParser`), and aggregation (`ScoreCalculator`) to produce `SessionResult`.

### Key Domain Objects

| Type | Notes |
| --- | --- |
| `SongRequest` | Identifies the desired track; reused across search, download, and persistence. |
| `TabData` | Canonical tab payload (sourceId + raw content + fetch metadata). |
| `FormattedTab` | Tab ready for UI consumption (list of SVG strings, metadata). |
| `CalibrationInput` / `CalibrationProfile` | User/device gain and noise information. |
| `SessionRequest` / `SessionResult` | Inputs & outputs of the practice scoring pipeline. |
| `TabSelection`, `TabSearchResult`, `SavedTabSummary` | Songsterr-style selection metadata surfaced to the UI. |

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
mvn clean javafx:run            # launches the JavaFX sandbox UI (uses App.java)
```

* Java 17+ is recommended (project uses records & modern APIs).
* `target/` is gitignored; no custom build tooling beyond Maven.

### Conventions & Tips

* **Records everywhere:** most DTOs are Java records—remember constructor validation blocks (see `SessionRequest`, `CalibrationInput`).
* **Null handling:** public APIs call `Objects.requireNonNull` aggressively; propagate optional data with `java.util.Optional`.
* **SVG rendering:** `com.shredforge.tabview.render` holds low-level drawing helpers (currently Songsterr-compatible). When editing, keep the performance characteristics in mind—renderers are used in the JavaFX UI thread.
* **Scoring:** Real-time detection uses TarsosDSP, so anything in `scoring.detection` may touch audio hardware—avoid running those pieces in CI unless mocked.
* **Demo utilities:** `ShredforgeRepository.runDemoSession()` exercises the full stack with canned data; useful for smoke tests.

### How to Extend

1. **New data sources:** implement `TabGateway` or extend `TabGetService` to talk to a different provider, then wire via `ShredforgeFacade.Builder.withTabGateway`.
2. **Custom renderers:** provide another `TabFormatter` and inject it (same builder).
3. **Real scoring:** swap `SessionScoringService` with a concrete engine under `com.shredforge.scoring`.
4. **UI integration:** `PrimaryController` shows how the current JavaFX UI talks to `ShredforgeRepository`; keep interactions confined to the repository layer to avoid leaking infrastructure concerns upward.

### Useful Entry Points

* `ShredforgeRepository.builder()` – use in tests or tooling to bootstrap the system.
* `TabManager.createDefault()` – spins up DAO-backed search/save stack without external wiring.
* `TabRenderingService` – stateless; safe to reuse as a singleton.
* `ScoreCalculator.generateReport()` – produces `ScoreReport` objects you can display or persist.

Keep this file up to date as APIs move—future agents will thank you!
