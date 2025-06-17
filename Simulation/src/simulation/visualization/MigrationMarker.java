package simulation.visualization;

import simulation.utils.DataManager.MigrationInfo;

public class MigrationMarker {
    private final MigrationInfo migration;
    private double progress = 0.0;
    private long startTime;
    private static final double ANIMATION_DURATION = 11.0; // durata in tick

    public MigrationMarker(MigrationInfo migration) {
        this.migration = migration;
        this.startTime = (long) repast.simphony.engine.environment.RunEnvironment.getInstance()
            .getCurrentSchedule().getTickCount();
    }

    public MigrationInfo getMigration() {
        return migration;
    }

    public double getProgress() {
        return progress;
    }

    public void updateProgress() {
        long currentTime = (long) repast.simphony.engine.environment.RunEnvironment.getInstance()
            .getCurrentSchedule().getTickCount();
        double elapsed = currentTime - startTime;

        // Interpolazione fluida basata sul tempo
        progress = Math.min(1.0, elapsed / ANIMATION_DURATION);

        // Easing cubico per un movimento più morbido
        if (progress < 0.5) {
            progress = 4 * progress * progress * progress;
        } else {
            progress = 1 - 4 * (1 - progress) * (1 - progress) * (1 - progress);
        }

        // Clamping per sicurezza
        progress = Math.max(0.0, Math.min(1.0, progress));
    }

    public boolean isComplete() {
        return progress >= 1.0;
    }
}