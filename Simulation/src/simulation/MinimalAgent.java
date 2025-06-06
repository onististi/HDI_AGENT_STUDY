package simulation;

import repast.simphony.engine.schedule.ScheduledMethod;

public class MinimalAgent {

    @ScheduledMethod(start = 1, interval = 3)
    public void step() {
        System.out.println("✅ Tick attivo");
    }
}
