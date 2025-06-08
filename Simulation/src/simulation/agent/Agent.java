package simulation.agent;

import simulation.context.Regione;
import simulation.utils.DataManager;
import simulation.utils.DecisionUtils;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.space.grid.Grid;

import java.util.*;

public class Agent {
    private static int counter = 0;
    private final int id;
    private Regione regione;
    private final String categoria;
    private final int eta;
    private final boolean famiglia;
    private final Map<String, Double> utilityWeights;
    private final Map<String, Double> pushpullWeights;
    private final List<Regione> regioni;
    private final Grid<Object> griglia;
    private final Random rnd = new Random();
    private final int anniStabile;

    public Agent(Regione r, String cat, int eta, boolean fam,
                 Map<String, Double> uw, Map<String, Double> ppw,
                 List<Regione> regList, Grid<Object> grid) {
        this.id = counter++;
        this.regione = r;
        this.categoria = cat;
        this.eta = eta;
        this.famiglia = fam;
        this.utilityWeights = new HashMap<>(uw);
        this.pushpullWeights = new HashMap<>(ppw);
        this.regioni = regList;
        this.griglia = grid;
        this.anniStabile = 1 + rnd.nextInt(10);
    }

    public static Agent creaRandom(Regione r, List<Regione> tutte, Grid<Object> grid) {
        Random rnd = new Random();
        String[] categorie = { "Disoccupato_Diploma", "Disoccupato_Laurea", "Lavoratore_Diploma", "Lavoratore_Laurea" };
        String cat = categorie[rnd.nextInt(categorie.length)];

        boolean famiglia = rnd.nextBoolean();
        int eta = switch (cat) {
            case "Disoccupato_Diploma" -> 18 + rnd.nextInt(5);
            case "Disoccupato_Laurea" -> 23 + rnd.nextInt(8);
            case "Lavoratore_Diploma", "Lavoratore_Laurea" -> 25 + rnd.nextInt(35);
            default -> 30;
        };

        Map<String, Double> uw = switch (cat) {
            case "Disoccupato_Diploma" -> Map.of("Salario", 0.2, "Occupazione", 0.4, "Istruzione", 0.1, "Affitto", 0.25);
            case "Disoccupato_Laurea" -> Map.of("Salario", 0.25, "Occupazione", 0.25, "Istruzione", 0.25, "Affitto", 0.25);
            case "Lavoratore_Diploma" -> Map.of("Salario", 0.35, "Occupazione", 0.45, "Istruzione", 0.01, "Affitto", 0.19);
            case "Lavoratore_Laurea" -> Map.of("Salario", 0.3, "Occupazione", 0.3, "Istruzione", 0.3, "Affitto", 0.1);
            default -> Map.of("Salario", 0.2, "Occupazione", 0.4, "Istruzione", 0.1, "Affitto", 0.25);
        };

        Map<String, Double> pp = switch (cat) {
            case "Disoccupato_Diploma" -> Map.of("Salario", 0.3, "Occupazione", 0.5, "Servizi", 0.2);
            case "Disoccupato_Laurea" -> Map.of("Salario", 0.35, "Occupazione", 0.35, "Servizi", 0.3);
            case "Lavoratore_Diploma" -> Map.of("Salario", 0.3, "Occupazione", 0.35, "Servizi", 0.35);
            case "Lavoratore_Laurea" -> Map.of("Salario", 0.45, "Occupazione", 0.25, "Servizi", 0.3);
            default -> Map.of("Salario", 0.3, "Occupazione", 0.5, "Servizi", 0.2);
        };

        return new Agent(r, cat, eta, famiglia, uw, pp, tutte, grid);
    }

    @ScheduledMethod(start = 1, interval = 12)
    public void step() {
        double soglia = sogliaDecisione();
        double utilOrig = DecisionUtils.utility(regione, utilityWeights);

        List<Regione> candidati = new ArrayList<>(regioni);
        candidati.removeIf(r -> r.nome.equals(regione.nome));
        Collections.shuffle(candidati);
        candidati = candidati.subList(0, Math.min(3, candidati.size()));

        List<String> righe = new ArrayList<>();
        for (Regione r : candidati) {
            double gravity = DecisionUtils.gravity(regione, r);
            double utilDest = DecisionUtils.utility(r, utilityWeights);
            double push = DecisionUtils.pushpull(regione, r, pushpullWeights);
            double rumore = DecisionUtils.noise(rnd, eta, famiglia, categoria.contains("Laurea"));
            
            double attr = 0.3 * gravity + 0.5 * (utilDest - utilOrig) + 0.2 * push;
            
            boolean emigrato = attr > soglia + rumore;

            righe.add(String.format(Locale.US,
                "%d,%s,%s,%s,%b,%d,%.4f,%.4f,%d,%b,%.4f,%.4f,%.4f",
                id, categoria, regione.nome, r.nome, famiglia, anniStabile,
                attr, soglia, eta, emigrato, gravity, push, utilDest - utilOrig
            ));

            if (emigrato) {
                regione = r;
                break; // solo una migrazione per anno
            }
        }

        if (!righe.isEmpty()) {
            DataManager.appendToCSV("data/log.csv", righe);
        }
    }

    private double sogliaDecisione() {
        double base = switch (categoria) {
            case "Disoccupato_Diploma" -> 0.07;
            case "Disoccupato_Laurea" -> 0.06;
            case "Lavoratore_Diploma" -> 0.16;
            case "Lavoratore_Laurea" -> 0.13;
            default -> 0.1;
        };
        if (famiglia) base += 0.08;
        return base + ((eta / 10.0) * 0.01) + ( anniStabile * 0.02 );
    }
}
