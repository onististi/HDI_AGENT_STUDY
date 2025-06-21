package simulation.visualization;

import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.render.BasicWWTexture;
import gov.nasa.worldwind.render.Offset;
import gov.nasa.worldwind.render.WWTexture;
import repast.simphony.visualization.gis3D.PlaceMark;
import repast.simphony.visualization.gis3D.style.MarkStyle;
import simulation.utils.DataManager.MigrationInfo;
import java.awt.*;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class MigrationAnimation implements MarkStyle<MigrationMarker> {
    private final Map<String, WWTexture> textureMap;
    private WWTexture defaultTexture;

    public MigrationAnimation() {
        this.textureMap = new HashMap<>();
        loadTextures();
    }

    private void loadTextures() {
        loadTexture("disoccupato_laurea", "icons/disoccupato_laurea.png");
        loadTexture("disoccupato_diploma", "icons/disoccupato_diploma.png");
        loadTexture("lavoratore_laurea", "icons/lavoratore_laurea.png");
        loadTexture("lavoratore_diploma", "icons/lavoratore_diploma.png");

        createDefaultTexture();
    }

    private void loadTexture(String key, String filePath) {
        try {
            URL localUrl = WorldWind.getDataFileStore().requestFile(filePath);
            if (localUrl != null) {
                textureMap.put(key, new BasicWWTexture(localUrl, false));
            } else {
                System.err.println("Errore: Impossibile caricare la texture per " + key + " da " + filePath);
            }
        } catch (Exception e) {
            System.err.println("Errore nel caricamento della texture " + key + ": " + e.getMessage());
        }
    }

    private void createDefaultTexture() {
        try {
            defaultTexture = new BasicWWTexture(Color.RED, false);
        } catch (Exception e) {
            System.err.println("Errore nella creazione della texture di default: " + e.getMessage());
        }
    }

    @Override
    public PlaceMark getPlaceMark(MigrationMarker obj, PlaceMark mark) {
        if (mark == null) {
            mark = new PlaceMark();
        }

        mark.setAltitudeMode(WorldWind.RELATIVE_TO_GROUND);
        mark.setLineEnabled(false);

        // posizione già aggiornata dal marker con Geography.move()
        return mark;
    }

    @Override
    public WWTexture getTexture(MigrationMarker obj, WWTexture currentTexture) {
        MigrationInfo migration = obj.getMigration();
        String category = migration.categoria != null ? migration.categoria.toLowerCase() : "default";

        WWTexture texture = textureMap.get(category);
        if (texture == null) {
            texture = textureMap.get("disoccupato_diploma");
        }
        if (texture == null) {
            texture = defaultTexture;
        }

        return texture;
    }

    @Override
    public Offset getIconOffset(MigrationMarker obj) {
        return Offset.CENTER;
    }

    @Override
    public double getElevation(MigrationMarker obj) {
        double progress = obj.getProgress();
        return 2000 + (Math.sin(progress * Math.PI) * 15000);
    }

    @Override
    public double getScale(MigrationMarker obj) { 
        return 0.05;
    }

    @Override
    public double getHeading(MigrationMarker obj) {
        return 0;
    }

    @Override
    public String getLabel(MigrationMarker obj) {
        MigrationInfo migration = obj.getMigration();
        return migration.origine.substring(0, Math.min(3, migration.origine.length())) + "→" +
               migration.destinazione.substring(0, Math.min(3, migration.destinazione.length()));
    }

    @Override
    public Color getLabelColor(MigrationMarker obj) {
        return Color.WHITE;
    }

    @Override
    public Font getLabelFont(MigrationMarker obj) {
        return new Font("Arial", Font.BOLD, 12);
    }

    @Override
    public Offset getLabelOffset(MigrationMarker obj) {
        return Offset.fromFraction(0.5, -0.2);
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
