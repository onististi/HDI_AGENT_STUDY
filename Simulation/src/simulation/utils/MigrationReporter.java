package simulation.utils;
import repast.simphony.context.Context;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.space.gis.Geography;
import repast.simphony.util.ContextUtils;
import simulation.context.Regione;
import simulation.utils.DataManager;
import simulation.visualization.MigrationMarker;

import java.io.IOException;
import java.util.*;

public class MigrationReporter {
    private Map<String, Regione> regioniMap;
    private String logPath;
    private int agentiPerRegione;
    private double migrationThreshold;
    private Geography<Object> geography;
    private List<MigrationMarker> activeAnimations;
    private int lastProcessedYear = -1;

    public MigrationReporter(Map<String, Regione> regioniMap, String logPath, 
                           int agentiPerRegione, double migrationThreshold, 
                           Geography<Object> geography, List<MigrationMarker> activeAnimations) {
        this.regioniMap = regioniMap;
        this.logPath = logPath;
        this.agentiPerRegione = agentiPerRegione;
        this.migrationThreshold = migrationThreshold;
        this.geography = geography;
        this.activeAnimations = activeAnimations;
    }

    
    @ScheduledMethod(start = 12, interval = 12, priority = 1)
    public void stampa() throws IOException {
        int currentTick = (int) RunEnvironment.getInstance().getCurrentSchedule().getTickCount();
        int anno = (currentTick - 12) / 12; // Year 0 at tick 12, year 1 at tick 24
        DataManager.stampaSaldoAnnuale(logPath, regioniMap, anno, agentiPerRegione);
    }
    
    @ScheduledMethod(start = 10, interval = 0.5, priority = 1)
    public void updateAnimations() {
        Context<Object> context = ContextUtils.getContext(this);
        if (context == null) {
            System.err.println("Errore: contesto non trovato per MigrationReporter");
            return;
        }
        
        Geography<Object> currentGeography = (Geography<Object>) context.getProjection("Geography");
        if (currentGeography == null) {
            System.err.println("Errore: geografia non trovata per MigrationReporter");
            return;
        }

        double currentTick = RunEnvironment.getInstance().getCurrentSchedule().getTickCount();
        List<MigrationMarker> toRemove = new ArrayList<>();
        
        for (MigrationMarker marker : activeAnimations) {
            marker.updateProgress();
            if (marker.isComplete()) {
                toRemove.add(marker);
            }
        }
        

        for (MigrationMarker marker : toRemove) {
            try {
                context.remove(marker);
                activeAnimations.remove(marker);
                
                //System.out.println("MARKER RIMOSSO COMPLETAMENTE al tick " + currentTick + " per migrazione: " + marker.getMigration().origine + " -> " +  marker.getMigration().destinazione);
            } catch (Exception e) {
                System.err.println("Errore nella rimozione del marker: " + e.getMessage());
            }
        }
        
      /*  if (currentTick % 5 == 0) { // Ogni 5 tick per non riempire troppo i log
            System.out.println("=== STATO ANIMAZIONI al tick " + currentTick + " ===");
            System.out.println("Animazioni attive: " + activeAnimations.size());
            for (MigrationMarker marker : activeAnimations) {
                System.out.println("  - " + marker.getMigration().origine + " -> " + 
                                 marker.getMigration().destinazione + 
                                 " (Progress: " + String.format("%.3f", marker.getProgress()) + ")");
            }
            System.out.println("=====================================");
        }*/
    }

    @ScheduledMethod(start = 12, interval = 12, priority = 2)
    public void createNewAnimations() {
        Context<Object> context = ContextUtils.getContext(this);
        if (context == null) return;
        
        Geography<Object> currentGeography = (Geography<Object>) context.getProjection("Geography");
        if (currentGeography == null) return;

        double currentTick = RunEnvironment.getInstance().getCurrentSchedule().getTickCount();
        int anno = (int) (currentTick / 12);
        
        if (anno == lastProcessedYear)return;
        lastProcessedYear = anno;
        
        List<DataManager.MigrationInfo> migrations = DataManager.getMigrationsToVisualize(
            logPath, anno, migrationThreshold);

        for (DataManager.MigrationInfo migration : migrations) {
            double percentage = (double) migration.conteggio / agentiPerRegione;
            
            if (percentage >= migrationThreshold) {
                boolean alreadyExists = activeAnimations.stream()
                    .anyMatch(m -> m.getMigration().origine.equals(migration.origine) && 
                                  m.getMigration().destinazione.equals(migration.destinazione) &&
                                  m.getMigration().categoria.equals(migration.categoria));
                
                if (!alreadyExists) {
                    MigrationMarker marker = new MigrationMarker(migration);
                    context.add(marker);
                    
                    Regione sourceRegion = regioniMap.get(migration.origine);
                    if (sourceRegion != null) {
                        currentGeography.move(marker, sourceRegion.location);
                        activeAnimations.add(marker);
                        
                       // System.out.println("NUOVA ANIMAZIONE CREATA: " + migration.origine + " -> " + migration.destinazione + " (conteggio: " + migration.conteggio + ", percentuale: " + String.format("%.2f%%", percentage * 100) + ")");
                    }
                } else {
                    System.out.println("Animazione già esistente per: " + 
                        migration.origine + " -> " + migration.destinazione);
                }
            }
        }
    }
}