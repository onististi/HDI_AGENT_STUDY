package simulation.context;
import simulation.agent.Agent;
import simulation.utils.DataManager;
import simulation.utils.DecisionUtils;
import simulation.utils.MigrationReporter;
import repast.simphony.context.Context;
import repast.simphony.context.space.grid.GridFactory;
import repast.simphony.context.space.grid.GridFactoryFinder;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.environment.RunListener;
import repast.simphony.engine.environment.RunState;
import repast.simphony.space.grid.*;
import java.util.*;public class ItalianRegionsContext implements ContextBuilder<Object> {

	private Map<String, Regione> regioniMap; // Aggiungi questa variabile per mantenerla

	public static final Map<String, int[]> posizioniRegioni = Map.ofEntries(
	    Map.entry("Piemonte", new int[]{0, 0}),
	    Map.entry("Valle d'Aosta", new int[]{0, 1}),
	    Map.entry("Lombardia", new int[]{0, 2}),
	    Map.entry("Trentino-Alto Adige", new int[]{0, 3}),
	    Map.entry("Veneto", new int[]{0, 4}),
	    Map.entry("Friuli-Venezia Giulia", new int[]{0, 5}),
	    Map.entry("Liguria", new int[]{1, 0}),
	    Map.entry("Emilia-Romagna", new int[]{1, 2}),
	    Map.entry("Toscana", new int[]{2, 1}),
	    Map.entry("Umbria", new int[]{2, 2}),
	    Map.entry("Marche", new int[]{2, 3}),
	    Map.entry("Lazio", new int[]{3, 2}),
	    Map.entry("Abruzzo", new int[]{3, 3}),
	    Map.entry("Molise", new int[]{3, 4}),
	    Map.entry("Campania", new int[]{4, 2}),
	    Map.entry("Puglia", new int[]{4, 3}),
	    Map.entry("Basilicata", new int[]{4, 4}),
	    Map.entry("Calabria", new int[]{5, 3}),
	    Map.entry("Sicilia", new int[]{5, 1}),
	    Map.entry("Sardegna", new int[]{5, 0})
	);

	@Override
	public Context<Object> build(Context<Object> context) {
	    context.setId("Italia");
	    
	    regioniMap = DataManager.caricaRegioni("data/regioni_istat.csv");
	    List<Regione> regList = new ArrayList<>(regioniMap.values());
	    
	    DataManager.inizializzaLog("data/log.csv");
	    DecisionUtils.calcolaEstremiPerCategoria(regList);
	    
	    GridFactory factory = GridFactoryFinder.createGridFactory(null);
	    Grid<Object> grid = factory.createGrid("GridItalia", context,
	        new GridBuilderParameters<>(new WrapAroundBorders(), new SimpleGridAdder<>(), true, 6, 6));
	    
	    for (Regione r : regList) {
	        context.add(r);
	        int[] coord = posizioniRegioni.get(r.nome);
	        r.setCoord(coord[0], coord[1]);
	        grid.moveTo(r, coord[0], coord[1]);
	        int n = (int) (r.popolazione * 10); // ridotto per debug ogni agente 100.000 persone
	        for (int i = 0; i < n; i++) {
	            Agent p = Agent.creaRandom(r, regList, grid);
	            context.add(p);
	            grid.moveTo(p, coord[0], coord[1]);
	        }
	    }
	    
	    MigrationReporter reporter = new MigrationReporter(regioniMap, "data/log.csv");
	    context.add(reporter);
	    
	    RunEnvironment.getInstance().addRunListener(new RunListener() {
	        @Override
	        public void stopped() {
	            System.out.println("Simulazione terminata. Calcolo del saldo migratorio finale...");
	            DataManager.stampaSaldoMigratorioFinale("data/log.csv", regioniMap);
	        }

			@Override
			public void paused() {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void started() {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void restarted() {
				// TODO Auto-generated method stub
				
			}
	    });
	    
	    RunEnvironment.getInstance().endAt(13); // 1 anni
	    return context;
	}
}