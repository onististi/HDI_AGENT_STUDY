package simulation.visualization;
import java.awt.Color;
import java.awt.Font;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import simulation.agent.Agent;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.render.BasicWWTexture;
import gov.nasa.worldwind.render.Material;
import gov.nasa.worldwind.render.Offset;
import gov.nasa.worldwind.render.WWTexture;
import repast.simphony.visualization.gis3D.PlaceMark;
import repast.simphony.visualization.gis3D.style.MarkStyle;

public class AgentStyle implements MarkStyle<Agent>{

		private Map<String, WWTexture> textureMap;
		
		public AgentStyle() {
			/**
			 * Use of a map to store textures significantly reduces CPU and memory use
			 * since the same texture can be reused.  Textures can be created for different
			 * agent states and re-used when needed.
			 */
			textureMap = new HashMap<String, WWTexture>();
			
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
		public PlaceMark getPlaceMark(Agent agent, PlaceMark mark) {
			// PlaceMark is null on first call.
			if (mark == null)
				mark = new PlaceMark();
			
			/**
			 * The Altitude mode determines how the mark appears using the elevation.
			 *   WorldWind.ABSOLUTE places the mark at elevation relative to sea level
			 *   WorldWind.RELATIVE_TO_GROUND places the mark at elevation relative to ground elevation
			 *   WorldWind.CLAMP_TO_GROUND places the mark at ground elevation
			 */
			mark.setAltitudeMode(WorldWind.RELATIVE_TO_GROUND);
			mark.setLineEnabled(false);
			
			return mark;
		}

		/**
		 * Here we set the appearance of the TowerAgent using a non-changing icon.
		 */
		@Override
		public WWTexture getTexture(Agent agent, WWTexture currentTexture) {

				switch(agent.getCategoria()){
				case "Disoccupato_Diploma":
					return textureMap.get("disoccupato_diploma");
				case "Disoccupato_Laurea":
					return textureMap.get("disoccupato_laurea");
				case "Lavoratore_Diploma":
					return textureMap.get("lavoratore_diploma");
				case "Lavoratore_Laurea":
					return textureMap.get("lavoratore_laurea");
				}
				return textureMap.get("disoccupato_diploma");
		}
		
		@Override
		public Offset getIconOffset(Agent agent){
			return Offset.CENTER;
		}

		@Override
		public double getElevation(Agent obj) {
			return 0;
		}

		@Override
		public double getScale(Agent obj) {
			return 0.05;
		}

		@Override
		public double getHeading(Agent obj) {
			return 0;
		}

		@Override
		public String getLabel(Agent obj) {
			return null;
		}

		@Override
		public Color getLabelColor(Agent obj) {
			return null;
		}

		@Override
		public Font getLabelFont(Agent obj) {
			return null;
		}

		@Override
		public Offset getLabelOffset(Agent obj) {
			return null;
		}

		@Override
		public double getLineWidth(Agent obj) {
			return 0;
		}

		@Override
		public Material getLineMaterial(Agent obj, Material lineMaterial) {
			return null;
		}
		
}
