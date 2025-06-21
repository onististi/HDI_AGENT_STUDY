package simulation.context;

import simulation.agent.Agent;
import simulation.utils.DataManager;
import simulation.utils.DecisionUtils;
import simulation.utils.MigrationReporter;
import simulation.visualization.MigrationAnimation;
import simulation.visualization.MigrationMarker;
import repast.simphony.context.Context;
import repast.simphony.context.space.grid.GridFactory;
import repast.simphony.context.space.grid.GridFactoryFinder;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.environment.RunListener;
import repast.simphony.parameter.Parameters;
import repast.simphony.space.grid.*;
import repast.simphony.space.gis.Geography;
import repast.simphony.space.gis.GeographyParameters;
import repast.simphony.context.space.gis.GeographyFactory;
import repast.simphony.context.space.gis.GeographyFactoryFinder;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

import gov.nasa.worldwind.WorldWindow;

import java.io.IOException;
import java.util.*;

public class ItalianRegionsContext implements ContextBuilder<Object> {

    private Map<String, Regione> regioniMap;
    private List<MigrationMarker> activeAnimations;

    @Override
    public Context<Object> build(Context<Object> context) {
        context.setId("Italia");

        Parameters params = RunEnvironment.getInstance().getParameters();
        int agentiPerRegione = params.getInteger("agentiPerRegione");
        double migrationThreshold = params.getDouble("migrationThreshold");

        String selectedDataset = params.getString("dataset");
        if (selectedDataset == null)
            selectedDataset = "regioni_istat.csv";
        String csvPath = "data/" + selectedDataset;

        regioniMap = DataManager.caricaRegioni(csvPath);
        List<Regione> regList = new ArrayList<>(regioniMap.values());

        DataManager.inizializzaLog("data/log.csv");
        DecisionUtils.calcolaEstremiPerCategoria(regList);
        
        GeographyParameters<Object> geoParams = new GeographyParameters<Object>();
		Geography<Object> geography = GeographyFactoryFinder.createGeographyFactory(null).createGeography("Geography", context, geoParams);
       
		activeAnimations = new ArrayList<>();

        for (Regione r : regList) {
            context.add(r);
            r.location =  new GeometryFactory().createPoint(r.coordinate);
            geography.move(r, r.location);

            for (int i = 0; i < agentiPerRegione; i++) {
                Agent p = Agent.creaRandom(r, regList);
                context.add(p);
            }
        }
        /*
         * <?xml version="1.0" encoding="UTF-8"?>
<parameters>
    <parameter name="randomSeed" type="int" defaultValue="0" displayName="Default Random Seed"/>
    
    <parameter name="dataset" type="string" defaultValue="regioni_istat.csv" displayName="Dataset da utilizzare" inputType="select">
        <choice value="regioni_istat.csv" displayValue="Regioni ISTAT"/>
        <choice value="divario_ampliato.csv" displayValue="Divario Ampliato"/>
    </parameter>
    
    <parameter name="agentiPerRegione" type="int" defaultValue="60" displayName="Agenti per regione" inputType="select">
        <choice value="60" displayValue="60 (veloce)"/>
        <choice value="120" displayValue="120 (bilanciato)"/>
        <choice value="300" displayValue="300 (precisa)"/>
        <choice value="500" displayValue="500 (molto precisa)"/>
    </parameter>
    
    <parameter name="migrationThreshold" type="double" defaultValue="0.1" displayName="Soglia di migrazione" inputType="select">
        <choice value="0.1" displayValue="10%"/>
        <choice value="0.05" displayValue="5%"/>
        <choice value="0.2" displayValue="20%"/>
    </parameter>
</parameters>*/

        MigrationReporter reporter = new MigrationReporter(regioniMap, "data/log.csv", agentiPerRegione, 0, geography, activeAnimations);
        context.add(reporter);

        RunEnvironment.getInstance().addRunListener(new RunListener() {
        	@Override
        	public void stopped() {
        	    System.out.println("Simulazione terminata. Calcolo del saldo migratorio finale...");
        	    try {
					DataManager.stampaSaldoFinale("data/log.csv", regioniMap, agentiPerRegione);
				} catch (IOException e) {
					e.printStackTrace();
				}
        	}

            @Override
            public void paused() {}

            @Override
            public void started() {}

            @Override
            public void restarted() {}
        });

        RunEnvironment.getInstance().endAt(23);
        return context;
    }
}