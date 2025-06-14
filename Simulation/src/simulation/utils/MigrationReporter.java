package simulation.utils;

import repast.simphony.engine.schedule.ScheduledMethod;
import simulation.context.Regione;

import java.io.IOException;
import java.util.Map;

public class MigrationReporter {
    private final Map<String, Regione> regioniMap;
    private final String logPath;

    public MigrationReporter(Map<String, Regione> regioniMap, String logPath) {
        this.regioniMap = regioniMap;
        this.logPath = logPath;
    }

    @ScheduledMethod(start = 12, interval = 12)
    public void report() {
        try {
            int annoCorrente = (int) repast.simphony.engine.environment.RunEnvironment.getInstance().getCurrentSchedule().getTickCount();
            System.out.println("📅 Fine Anno " + (annoCorrente / 12) + " - Calcolo saldo migratorio...");
            DataManager.stampaSaldoMigratorioConsole(logPath, regioniMap, annoCorrente);
        } catch (IOException e) {
            System.err.println("Errore nel report annuale: " + e.getMessage());
        }
    }
}