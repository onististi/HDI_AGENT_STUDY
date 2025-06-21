package simulation.visualization;

import java.util.stream.StreamSupport;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

import repast.simphony.context.Context;
import repast.simphony.space.gis.Geography;
import repast.simphony.util.ContextUtils;
import simulation.context.Regione;
import simulation.utils.DataManager.MigrationInfo;

public class MigrationMarker {
    private final MigrationInfo migration;
    private double progress = 0.0;
    private long startTime;
    private static final double ANIMATION_DURATION = 10.0;
    private final double startDelay;

    public MigrationMarker(MigrationInfo migration) {
        this.migration = migration;
        this.startTime = (long) repast.simphony.engine.environment.RunEnvironment.getInstance()
                .getCurrentSchedule().getTickCount();

        this.startDelay =0;
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
        double elapsed = currentTime - startTime - startDelay;

        if (elapsed < 0) {
            progress = 0.0;
            return;
        }

        double rawProgress = elapsed / ANIMATION_DURATION;
        progress = Math.max(0.0, Math.min(1.0, rawProgress));

        Context<Object> context = ContextUtils.getContext(this);
        if (context != null) {
            Geography<Object> geo = (Geography<Object>) context.getProjection("Geography");
            if (geo != null) {
                Regione source = StreamSupport.stream(context.getObjects(Regione.class).spliterator(), false)
                    .map(o -> (Regione) o)
                    .filter(r -> r.getNome().equals(migration.origine))
                    .findFirst()
                    .orElse(null);

                Regione dest = StreamSupport.stream(context.getObjects(Regione.class).spliterator(), false)
                    .map(o -> (Regione) o)
                    .filter(r -> r.getNome().equals(migration.destinazione))
                    .findFirst()
                    .orElse(null);

                if (source != null && dest != null) {
                    double sourceLat = source.coordinate.y;
                    double sourceLon = source.coordinate.x;
                    double destLat = dest.coordinate.y;
                    double destLon = dest.coordinate.x;

                    double currentLat = sourceLat + (destLat - sourceLat) * progress;
                    double currentLon = sourceLon + (destLon - sourceLon) * progress;

                    Point newPoint = new GeometryFactory().createPoint(new Coordinate(currentLon, currentLat));
                    geo.move(this, newPoint);
                }
            }
        }
    }

    public boolean isComplete() {
        return progress >= 1.0;
    }

    public double getStartDelay() {
        return startDelay;
    }
}