package simulation.utils;

import repast.simphony.context.Context;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.space.gis.Geography;
import repast.simphony.util.ContextUtils;
import simulation.context.Regione;
import simulation.visualization.MigrationAnimation;
import simulation.visualization.MigrationMarker;

import java.util.*;

public class MigrationReporter {
    private Map<String, Regione> regioniMap;
    private String logPath;
    private int agentiPerRegione;
    private double migrationThreshold;
    private Geography<Object> geography;
    private List<MigrationMarker> activeAnimations;

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

    @ScheduledMethod(start = 12, interval = 1, priority = 1) // Esecuzione ad ogni tick per fluidità
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

        // Aggiorna le animazioni esistenti
        Iterator<MigrationMarker> iterator = activeAnimations.iterator();
        while (iterator.hasNext()) {
            MigrationMarker marker = iterator.next();
            marker.updateProgress();
            
            if (marker.isComplete()) {
                // Rimuovi solo dal contesto
                context.remove(marker);
                iterator.remove();
                
                System.out.println("Animazione completata per migrazione: " + 
                    marker.getMigration().origine + " -> " + marker.getMigration().destinazione);
            }
        }
    }

    @ScheduledMethod(start = 12, interval = 12, priority = 1) // Creazione nuove animazioni ogni anno
    public void createNewAnimations() {
        Context<Object> context = ContextUtils.getContext(this);
        if (context == null) return;
        
        Geography<Object> currentGeography = (Geography<Object>) context.getProjection("Geography");
        if (currentGeography == null) return;

        int anno = (int) RunEnvironment.getInstance().getCurrentSchedule().getTickCount() / 12;
        List<DataManager.MigrationInfo> migrations = DataManager.getMigrationsToVisualize(
            logPath, anno, migrationThreshold);

        for (DataManager.MigrationInfo migration : migrations) {
            double percentage = (double) migration.conteggio / agentiPerRegione;
            
            if (percentage >= migrationThreshold) {
                // Verifica che non esista già un'animazione per questa migrazione
                boolean alreadyExists = activeAnimations.stream()
                    .anyMatch(m -> m.getMigration().origine.equals(migration.origine) && 
                                  m.getMigration().destinazione.equals(migration.destinazione));
                
                if (!alreadyExists) {
                    MigrationMarker marker = new MigrationMarker(migration);
                    context.add(marker);
                    
                    // Posiziona il marker alla posizione di origine
                    Regione sourceRegion = regioniMap.get(migration.origine);
                    if (sourceRegion != null) {
                        currentGeography.move(marker, sourceRegion.location);
                        activeAnimations.add(marker);
                        
                        System.out.println("Nuova animazione creata: " + 
                            migration.origine + " -> " + migration.destinazione + 
                            " (conteggio: " + migration.conteggio + ")");
                    }
                }
            }
        }
    }
}