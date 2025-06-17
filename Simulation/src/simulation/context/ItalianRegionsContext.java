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

        MigrationReporter reporter = new MigrationReporter(regioniMap, "data/log.csv", agentiPerRegione, 0, geography, activeAnimations);
        context.add(reporter);

        RunEnvironment.getInstance().addRunListener(new RunListener() {
            @Override
            public void stopped() {
                System.out.println("Simulazione terminata. Calcolo del saldo migratorio finale...");
               // DataManager.stampaSaldoMigratorioFinale("data/log.csv", regioniMap);
            }

            @Override
            public void paused() {}

            @Override
            public void started() {}

            @Override
            public void restarted() {}
        });

        RunEnvironment.getInstance().endAt(24);
        return context;
    }
}