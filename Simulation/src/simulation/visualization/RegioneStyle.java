package simulation.visualization;

import gov.nasa.worldwind.render.Material;
import gov.nasa.worldwind.render.Offset;
import gov.nasa.worldwind.render.WWTexture;
import repast.simphony.visualization.gis3D.style.MarkStyle;
import repast.simphony.visualization.gis3D.PlaceMark;
import simulation.context.Regione;
import gov.nasa.worldwind.geom.Position;
import java.awt.Color;
import java.awt.Font;

public class RegioneStyle implements MarkStyle<Regione> {

    @Override
    public PlaceMark getPlaceMark(Regione obj, PlaceMark mark) {
        if (mark == null) {
            mark = new PlaceMark();
        }
        mark.setAltitudeMode(gov.nasa.worldwind.WorldWind.RELATIVE_TO_GROUND);
        mark.setLineEnabled(false);
        return mark;
    }

    @Override
    public WWTexture getTexture(Regione obj, WWTexture currentTexture) {
        return null; // Use default marker (dot)
    }

    @Override
    public Offset getIconOffset(Regione obj) {
        return Offset.CENTER;
    }

    @Override
    public double getElevation(Regione obj) {
        return 0;
    }

    @Override
    public double getScale(Regione obj) {
        return 15.0; // Size 15 for large dots
    }

    @Override
    public double getHeading(Regione obj) {
        return 0;
    }

    @Override
    public String getLabel(Regione obj) {
        return obj.getNome(); // Use getNome() for label
    }

    @Override
    public Color getLabelColor(Regione obj) {
        return Color.BLACK; // Black labels
    }

    @Override
    public Font getLabelFont(Regione obj) {
        return new Font("SansSerif", Font.PLAIN, 12);
    }

    @Override
    public Offset getLabelOffset(Regione obj) {
    	return Offset.CENTER; // Usa il costruttore a 2 argomenti
    }	
    @Override
    public double getLineWidth(Regione obj) {
        return 0;
    }

    @Override
    public Material getLineMaterial(Regione obj, Material lineMaterial) {
        return null;
    }
}