package com.shredforge.scoring;

import com.shredforge.core.model.TabData;
import com.shredforge.scoring.detection.NoteDetectionConfig;
import com.shredforge.scoring.detection.NoteDetectionEngine;
import com.shredforge.scoring.detection.NoteDetectionListener;
import com.shredforge.scoring.model.DetectedNote;
import com.shredforge.scoring.model.NoteMatch;
import com.shredforge.scoring.model.ScoreReport;
import com.shredforge.scoring.model.TabNote;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Consumes a tab and compares it against live note detections to build an accuracy report.
 */
public final class ScoreCalculator {

    private final NoteDetectionEngine detectionEngine;
    private final TabNoteParser parser = new TabNoteParser();

    public ScoreCalculator(NoteDetectionEngine detectionEngine) {
        this.detectionEngine = Objects.requireNonNull(detectionEngine, "detectionEngine");
    }

    public ScoreReport calculate(TabData tabData, Duration maxDuration) {
        return calculate(tabData, maxDuration, NoteDetectionConfig.defaults());
    }

    public ScoreReport calculate(TabData tabData, Duration maxDuration, NoteDetectionConfig config) {
        List<TabNote> plan = parser.parse(tabData);
        if (plan.isEmpty()) {
            return ScoreReport.empty("Tab JSON contained no playable notes.");
        }
        NoteDetectionConfig cfg = config == null ? NoteDetectionConfig.defaults() : config;
        LinkedBlockingQueue<DetectedNote> queue = new LinkedBlockingQueue<>();
        List<String> warnings = new ArrayList<>();

        NoteDetectionListener listener = new NoteDetectionListener() {
            @Override
            public void onNoteDetected(DetectedNote note) {
                queue.offer(note);
            }

            @Override
            public void onError(Throwable throwable) {
                warnings.add("Detection error: " + throwable.getMessage());
            }
        };

        detectionEngine.start(cfg, listener);

        try {
            long deadline = System.nanoTime() + maxDuration.toNanos();
            Deque<TabNote> remaining = new ArrayDeque<>(plan);
            List<NoteMatch> matches = new ArrayList<>();
            int hits = 0;

            while (!remaining.isEmpty() && System.nanoTime() < deadline) {
                DetectedNote detected = queue.poll(25, TimeUnit.MILLISECONDS);
                if (detected == null) {
                    continue;
                }
                TabNote expected = remaining.peek();
                if (expected == null) {
                    break;
                }
                double timeDelta = Math.abs(detected.timestampMillis() - expected.timestampMillis());
                double centsDelta = Math.abs(detected.centsFromReference());
                boolean hit = !detected.isSilence()
                        && centsDelta <= cfg.pitchToleranceCents()
                        && timeDelta <= cfg.timingToleranceMillis();

                matches.add(new NoteMatch(expected, detected, timeDelta, centsDelta, hit));
                if (hit) {
                    remaining.removeFirst();
                    hits++;
                } else if (timeDelta > cfg.timingToleranceMillis() * 2) {
                    // detection came from a different moment; mark expected as miss and advance
                    matches.add(NoteMatch.miss(remaining.removeFirst()));
                }
            }

            while (!remaining.isEmpty()) {
                matches.add(NoteMatch.miss(remaining.removeFirst()));
            }

            double durationMillis = plan.isEmpty() ? 0 : plan.get(plan.size() - 1).timestampMillis();
            return new ScoreReport(matches, plan.size(), hits, plan.size() - hits, durationMillis, warnings);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            warnings.add("Scoring interrupted: " + ex.getMessage());
            return ScoreReport.empty("Scoring interrupted.");
        } finally {
            detectionEngine.stop();
        }
    }
}
