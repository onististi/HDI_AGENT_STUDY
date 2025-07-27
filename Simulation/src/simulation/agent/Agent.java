package simulation.agent;

import simulation.context.Regione;
import simulation.utils.DataManager;
import simulation.utils.DecisionUtils;
import repast.simphony.engine.schedule.ScheduleParameters;
import java.util.*;

public class Agent {
    private static int counter = 0;
    private final int id;
    private Regione regione;
    private final String categoria;
    private int eta;
    private boolean famiglia;
    private int anniStabile;
    private int anniDisoccupato;
    private int anniLavorati;
    int iterazione = 1;
    private final Map<String, Double> utilityWeights;
    private final Map<String, Double> pushpullWeights;
    private final List<Regione> regioni;
    private final Random rnd = new Random();
    private double wG, wU, wP;
    private int decisionMonth;
    private int n_candidati;

    public Agent(Regione r, String cat, int eta, boolean fam, int anniStabile, int anniDisoccupato, int anniLavorati,
                 Map<String, Double> uw, Map<String, Double> ppw, List<Regione> regList, int n_candidati) {
        this.id = counter++;
        this.regione = r;
        this.categoria = cat;
        this.eta = eta;
        this.famiglia = fam;
        this.anniStabile = anniStabile;
        this.anniDisoccupato = anniDisoccupato;
        this.anniLavorati = anniLavorati; 
        this.utilityWeights = new HashMap<>(uw);
        this.pushpullWeights = new HashMap<>(ppw);
        this.regioni = regList;
        this.n_candidati = n_candidati;

        
        this.decisionMonth = rnd.nextInt(10);
        double startTick = 1 + decisionMonth;
        ScheduleParameters params = ScheduleParameters.createRepeating(startTick, 12); // metodo schedulato per distribuire scelta; 12 ticks (1 year)
        repast.simphony.engine.environment.RunEnvironment.getInstance().getCurrentSchedule().schedule(params, this, "step");
    }

    public static boolean checkFamiglia(String cat, int eta, int anniStabile, int anniDisoccupato, int anniLavorati, Regione regione, Random rnd) {
        
        double baseProb = switch (cat) {
            case "Disoccupato_Diploma" -> eta < 23 ? 0.025 : eta < 28 ? 0.045 : eta < 33 ? 0.15 : eta < 38 ? 0.2 : 0.5;
            case "Disoccupato_Laurea" -> eta < 23 ? 0.025 : eta < 28 ? 0.05 : eta < 33 ? 0.16 : eta < 38 ? 0.2 : 0.5;
            case "Lavoratore_Diploma" -> eta < 23 ? 0.035 : eta < 28 ? 0.08 : eta < 33 ? 0.22 : eta < 38 ? 0.40 : 0.75;
            case "Lavoratore_Laurea" -> eta < 23 ? 0.035 : eta < 28 ? 0.10 : eta < 33 ? 0.28 : eta < 38 ? 0.48 : 0.75;
            default -> eta < 23 ? 0.015 : eta < 28 ? 0.06 : eta < 33 ? 0.15 : eta < 38 ? 0.30 : 0.45;
        };

        //probbilita in base alla sua carriera lavorativa
        if (cat.contains("Disoccupato")) {
            baseProb = Math.max(0.005, baseProb - 0.02 * Math.min(anniDisoccupato, 10)); // penalita se unemplyed da molto max 0.20
            baseProb += 0.01 * Math.min(anniLavorati, 10); // bonus se lavoratore stabile 
        } else if (cat.contains("Lavoratore")) {
            baseProb = Math.min(1.0, baseProb + 0.01 * Math.min(anniStabile, 20)); 
        }

        // curva per smoothare il cambiamento delle eta con picco a 37 anni
        double logistic = 1.0 / (1.0 + Math.exp(-(eta - 37) / 6.0));

        double finalProb = baseProb * logistic * regione.fattoreFamiliare;
        return rnd.nextDouble() < Math.min(finalProb, 0.9);
    }

    private static int generateAge(Random rnd) {
        double randomValue = rnd.nextDouble();

        for (int i = 0; i < DecisionUtils.AGE_DISTRIBUTION_CUMULATIVE.length; i++) {
            if (randomValue <= DecisionUtils.AGE_DISTRIBUTION_CUMULATIVE[i]) {
                int baseAge = 18 + (i * 5);
                int rangeSize = (i == DecisionUtils.AGE_DISTRIBUTION_CUMULATIVE.length - 1) ? 2 : 5;
                return baseAge + rnd.nextInt(rangeSize);
            }
        }
        return 18 + rnd.nextInt(47); // Fallback: 18-64
    }

    public static Agent crea(Regione r, List<Regione> tutte, int n_candidati, String setFascia) {
        Random rnd = new Random();
        boolean isLaureato = rnd.nextDouble() < r.pctLaureati / 100;
        double tassoDisocc = isLaureato ? r.disoccLaurea : r.disoccDiploma;
        boolean isDisoccupato = rnd.nextDouble() < tassoDisocc / 100;
        String cat = (isDisoccupato ? "Disoccupato_" : "Lavoratore_") + (isLaureato ? "Laurea" : "Diploma");

        int eta, anniDisponibili;
        int etaMin = isLaureato ? 23 : 18; 
        if (setFascia.equals("False")) { // parametro eta non settato
            do
                eta = generateAge(rnd);
            while (eta < etaMin);
            anniDisponibili = eta - etaMin;
        } else {
            if (setFascia.contains("-")) { // range ("18-30")
                String[] parts = setFascia.split("-");
                int etaMinFascia = Integer.parseInt(parts[0]);
                int etaMaxFascia = Integer.parseInt(parts[1]);
                eta = etaMinFascia + rnd.nextInt(etaMaxFascia - etaMinFascia + 1);
            } else { // eta unica fissata 
                eta = Integer.parseInt(setFascia);
            }

            if (eta < 23 && isLaureato) { // troppo giovane per essere laureato
                isLaureato = false;
                tassoDisocc = r.disoccDiploma;
                isDisoccupato = rnd.nextDouble() < tassoDisocc / 100;
                cat = (isDisoccupato ? "Disoccupato_" : "Lavoratore_") + "Diploma";
                etaMin = isDisoccupato ? 18 : 19;
            }
            anniDisponibili = Math.max(0, eta - etaMin);
        }

        int anniStabile = 0;
        int anniDisoccupato = 0;
        int anniLavorati = 0;
        if (cat.contains("Disoccupato")) {
            if (anniDisponibili > 0) {
                double probHaLavorato = 1.0 - (tassoDisocc / 100.0); //probabilita di aver lavorato in base al tasso disoccupazione in base alla categoria
                if (rnd.nextDouble() < probHaLavorato) {

                    double maxAnniLavorati = anniDisponibili * (r.occupazione / 100.0);
                    anniLavorati = (int) Math.max(1, Math.min(anniDisponibili, rnd.nextInt((int) Math.ceil(maxAnniLavorati)) + 1));
                }
                anniDisoccupato = anniDisponibili - anniLavorati; // Remaining years as unemployed
                anniDisoccupato = Math.max(0, anniDisoccupato); // Ensure non-negative
            }
        } else { //lavoratore
            if (anniDisponibili > 0) {
                // stima anni lavorativi in base all'occupazione generale della regione come sopra
                double maxAnniLavorati = anniDisponibili * (r.occupazione / 100.0);
                anniLavorati = (int) Math.max(1, Math.min(anniDisponibili, rnd.nextInt((int) Math.ceil(maxAnniLavorati)) + 1));
                anniStabile = anniLavorati; 
            }
        }

        // Utility weights
        Map<String, Double> uw = switch (cat) {
            case "Disoccupato_Diploma" -> Map.of("Salario", 0.2, "Occupazione", 0.4, "Istruzione", 0.1, "Affitto", 0.25);
            case "Disoccupato_Laurea" -> Map.of("Salario", 0.25, "Occupazione", 0.25, "Istruzione", 0.25, "Affitto", 0.25);
            case "Lavoratore_Diploma" -> Map.of("Salario", 0.35, "Occupazione", 0.45, "Istruzione", 0.01, "Affitto", 0.19);
            case "Lavoratore_Laurea" -> Map.of("Salario", 0.3, "Occupazione", 0.3, "Istruzione", 0.3, "Affitto", 0.1);
            default -> Map.of("Salario", 0.25, "Occupazione", 0.25, "Istruzione", 0.25, "Affitto", 0.25);
        };

        // Push/pull weights
        Map<String, Double> pp = switch (cat) {
            case "Disoccupato_Diploma" -> Map.of("Salario", 0.3, "Occupazione", 0.5, "Servizi", 0.2);
            case "Disoccupato_Laurea" -> Map.of("Salario", 0.35, "Occupazione", 0.35, "Servizi", 0.3);
            case "Lavoratore_Diploma" -> Map.of("Salario", 0.3, "Occupazione", 0.35, "Servizi", 0.35);
            case "Lavoratore_Laurea" -> Map.of("Salario", 0.45, "Occupazione", 0.25, "Servizi", 0.3);
            default -> Map.of("Salario", 0.3, "Occupazione", 0.4, "Servizi", 0.3);
        };

        boolean f = checkFamiglia(cat, eta, anniStabile, anniDisoccupato, anniLavorati, r, rnd);

        Agent a = new Agent(r, cat, eta, f, anniStabile, anniDisoccupato, anniLavorati, uw, pp, tutte, n_candidati);
        DecisionUtils.adjustWeights(a);
        DecisionUtils.adjustUtilityPushWeights(a);
        return a;
    }

    public void step() {
        if (!famiglia) //per simulazioni lunghe cambiamento stato famigliare dell'agente
            famiglia = checkFamiglia(categoria, this.eta, this.anniStabile, this.anniDisoccupato, this.anniLavorati, this.regione, rnd);

        DecisionUtils.adjustWeights(this);
        DecisionUtils.adjustUtilityPushWeights(this);

        double utilOrigNorm = DecisionUtils.utilityNorm(categoria, regione, utilityWeights);

        List<Regione> candidati = new ArrayList<>(regioni);
        candidati.removeIf(r -> r.nome.equals(regione.nome));
        Collections.shuffle(candidati);
        candidati = candidati.subList(0, Math.min(n_candidati, candidati.size()));

        Regione sceltaFinale = null;
        double bestAttrattivita = Double.NEGATIVE_INFINITY;
        String rigaScelta = null;

        for (Regione dest : candidati) {
            double distanza = DecisionUtils.distanza(regione, dest) * 111.0;
            double soglia = DecisionUtils.sogliaDecisionale(this, distanza);

            double gravityNorm = DecisionUtils.gravityNorm(categoria, regione, dest);
            double utilDestNorm = DecisionUtils.utilityNorm(categoria, dest, utilityWeights);
            double pushNorm = DecisionUtils.pushpullNorm(categoria, regione, dest, pushpullWeights);
            double rumore = DecisionUtils.noise(rnd, eta, famiglia, categoria.contains("Laurea"));

            double attr = wG * gravityNorm + wU * (utilDestNorm - utilOrigNorm) + wP * pushNorm;

            if (attr > bestAttrattivita) {
                bestAttrattivita = attr;

                boolean emigrato = attr > soglia + rumore;
                sceltaFinale = emigrato ? dest : null;

                rigaScelta = String.format(Locale.US, "%d,%s,%s,%s,%b,%d,%d,%.4f,%.4f,%d,%b,%.4f,%.4f,%.4f",
                        id, categoria, regione.nome, dest.nome, famiglia, anniStabile, anniLavorati, attr, soglia, eta,
                        emigrato, gravityNorm, pushNorm, utilDestNorm - utilOrigNorm);
            }
        }

        if (sceltaFinale != null) {
            regione = sceltaFinale;
            anniStabile = 0;
            if (categoria.contains("Disoccupato")) anniDisoccupato++;
        } else {
            if (categoria.contains("Disoccupato")) {
                anniDisoccupato++;
            } else {
                anniStabile++;
                anniLavorati++;
            }
        }

        eta++;

        if (rigaScelta != null) {
            List<String> righe = new ArrayList<>();
            righe.add(rigaScelta);
            DataManager.appendToCSV("data/log.csv", righe, iterazione);
        }
        iterazione++;
    }

    public String getCategoria() { return categoria; }
    public boolean isFamiglia() { return famiglia; }
    public int getEta() { return eta; }
    public int getAnniStabile() { return anniStabile; }
    public int getAnniDisoccupato() { return anniDisoccupato; }
    public int getAnniLavorati() { return anniLavorati; } // Getter for new variable
    public Map<String, Double> getUtilityWeights() { return utilityWeights; }
    public Map<String, Double> getPushpullWeights() { return pushpullWeights; }
    public void setW(double wg, double wu, double wp) { this.wG = wg; this.wU = wu; this.wP = wp; }
}