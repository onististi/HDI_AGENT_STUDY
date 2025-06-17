package simulation.visualization;

import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.render.BasicWWTexture;
import gov.nasa.worldwind.render.Offset;
import gov.nasa.worldwind.render.WWTexture;
import repast.simphony.context.Context;
import repast.simphony.space.gis.Geography;
import repast.simphony.util.ContextUtils;
import repast.simphony.visualization.gis3D.PlaceMark;
import repast.simphony.visualization.gis3D.style.MarkStyle;
import simulation.context.Regione;
import simulation.utils.DataManager.MigrationInfo;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.StreamSupport;

public class MigrationAnimation implements MarkStyle<MigrationMarker> {
    private Map<String, WWTexture> textureMap;

    public MigrationAnimation() {
        this.textureMap = new HashMap<>();
        loadTextures();
    }

    private void loadTextures() {
        loadTexture("disoccupato_laurea", "icons/disoccupato_laurea.png");
        loadTexture("disoccupato_diploma", "icons/disoccupato_diploma.png");
        loadTexture("lavoratore_laurea", "icons/lavoratore_laurea.png");
        loadTexture("lavoratore_diploma", "icons/lavoratore_diploma.png");
    }

    private void loadTexture(String key, String filePath) {
        URL localUrl = WorldWind.getDataFileStore().requestFile(filePath);
        if (localUrl != null) {
            textureMap.put(key, new BasicWWTexture(localUrl, false));
        } else {
            System.err.println("Error: Unable to load texture for " + key + " from " + filePath);
        }
    }

    @Override
    public PlaceMark getPlaceMark(MigrationMarker obj, PlaceMark mark) {
        if (mark == null) {
            mark = new PlaceMark();
        }

        mark.setAltitudeMode(WorldWind.RELATIVE_TO_GROUND);
        mark.setLineEnabled(false);

        MigrationInfo migration = obj.getMigration();
        Context<Object> context = ContextUtils.getContext(obj);
        if (context == null) return mark;

        Geography<Object> geography = (Geography<Object>) context.getProjection("Geography");
        if (geography == null) return mark;

        // Trova le regioni di origine e destinazione
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

        if (source == null || dest == null) {
            return mark;
        }

        // Calcola la posizione corrente basata sul progresso
        double sourceLat = source.coordinate.y;
        double sourceLon = source.coordinate.x;
        double destLat = dest.coordinate.y;
        double destLon = dest.coordinate.x;

        double progress = obj.getProgress();
        
        // Interpolazione fluida della posizione
        double currentLat = sourceLat + (destLat - sourceLat) * progress;
        double currentLon = sourceLon + (destLon - sourceLon) * progress;
        
        // Aggiungi una leggera elevazione per visibilità
        double elevation = 1000 + (Math.sin(progress * Math.PI) * 2000);

        mark.setPosition(Position.fromDegrees(currentLat, currentLon, elevation));

        return mark;
    }

    @Override
    public WWTexture getTexture(MigrationMarker obj, WWTexture currentTexture) {
        MigrationInfo migration = obj.getMigration();
        return textureMap.getOrDefault(
            migration.categoria.toLowerCase(),
            textureMap.get("disoccupato_diploma")
        );
    }

    @Override
    public Offset getIconOffset(MigrationMarker obj) {
        return Offset.CENTER;
    }

    @Override
    public double getElevation(MigrationMarker obj) {
        return 0; // L'elevazione è già gestita in getPlaceMark
    }

    @Override
    public double getScale(MigrationMarker obj) {
        // Scala dinamica basata sul progresso per effetto fade-in/fade-out
        double progress = obj.getProgress();
        double scale = 0.05;
        
        if (progress < 0.1) {
            // Fade-in all'inizio
            scale *= (progress / 0.1);
        } else if (progress > 0.9) {
            // Fade-out alla fine
            scale *= ((1.0 - progress) / 0.1);
        }
        
        return Math.max(scale, 0.01); // Scala minima per visibilità
    }

    @Override
    public double getHeading(MigrationMarker obj) {
        return 0;
    }

    @Override
    public String getLabel(MigrationMarker obj) {
        return null;
    }

    @Override
    public java.awt.Color getLabelColor(MigrationMarker obj) {
        return null;
    }

    @Override
    public java.awt.Font getLabelFont(MigrationMarker obj) {
        return null;
    }

    @Override
    public Offset getLabelOffset(MigrationMarker obj) {
        return null;
    }

    @Override
    public double getLineWidth(MigrationMarker obj) {
        return 0;
    }

    @Override
    public gov.nasa.worldwind.render.Material getLineMaterial(MigrationMarker obj, 
            gov.nasa.worldwind.render.Material lineMaterial) {
        return null;
    }
}