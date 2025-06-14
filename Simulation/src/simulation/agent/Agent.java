package simulation.agent;import simulation.context.Regione;
import simulation.utils.DataManager;
import simulation.utils.DecisionUtils;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.space.grid.Grid;import java.util.*;public class Agent {
    private static int counter = 0;
    private final int id;
    private Regione regione;
    private final String categoria;
    private int eta;
    private boolean famiglia;
    private int anniStabile;
    private int anniDisoccupato;

private final Map<String, Double> utilityWeights;
private final Map<String, Double> pushpullWeights;
private final List<Regione> regioni;
private final Grid<Object> griglia;
private final Random rnd = new Random();

private double wG, wU, wP;

public Agent(Regione r, String cat, int eta, boolean fam,
             int anniStabile, int anniDisoccupato,
             Map<String, Double> uw, Map<String, Double> ppw,
             List<Regione> regList, Grid<Object> grid) {
    this.id = counter++;
    this.regione = r;
    this.categoria = cat;
    this.eta = eta;
    this.famiglia = fam;
    this.anniStabile = anniStabile;
    this.anniDisoccupato = anniDisoccupato;
    this.utilityWeights = new HashMap<>(uw);
    this.pushpullWeights = new HashMap<>(ppw);
    this.regioni = regList;
    this.griglia = grid;
}

public static Agent creaRandom(Regione r, List<Regione> tutte, Grid<Object> grid) {
    Random rnd = new Random();
    String[] categorie = {
        "Disoccupato_Diploma", "Disoccupato_Laurea",
        "Lavoratore_Diploma", "Lavoratore_Laurea"
    };
    String cat = categorie[rnd.nextInt(categorie.length)];

    boolean famiglia = rnd.nextBoolean();

    int etaMin;
    switch (cat) {
        case "Disoccupato_Diploma" -> etaMin = 18;
        case "Disoccupato_Laurea" -> etaMin = 23;
        case "Lavoratore_Diploma" -> etaMin = 19;
        case "Lavoratore_Laurea" -> etaMin = 24;
        default -> etaMin = 18;
    }

    // Dai all'agente tra 0 e 20 anni di "storia" dopo l'età minima
    int eta = etaMin + rnd.nextInt(21); // Età massima = 38-43

    int anniDisponibili = eta - etaMin;

    int anniStabile = 0;
    int anniDisoccupato = 0;

    if (cat.contains("Disoccupato")) {
        anniDisoccupato = anniDisponibili > 0 ? rnd.nextInt(anniDisponibili + 1) : 0;
    } else {
        anniStabile = anniDisponibili > 0 ? rnd.nextInt(anniDisponibili + 1) : 0;
    }

    // Utility weights
    Map<String, Double> uw = switch (cat) {
        case "Disoccupato_Diploma" -> Map.of("Salario", 0.2, "Occupazione", 0.4, "Istruzione", 0.1, "Affitto", 0.25);
        case "Disoccupato_Laurea" -> Map.of("Salario", 0.25, "Occupazione", 0.25, "Istruzione", 0.25, "Affitto", 0.25);
        case "Lavoratore_Diploma" -> Map.of("Salario", 0.35, "Occupazione", 0.45, "Istruzione", 0.01, "Affitto", 0.19);
        case "Lavoratore_Laurea" -> Map.of("Salario", 0.3, "Occupazione", 0.3, "Istruzione", 0.3, "Affitto", 0.1);
        default -> Map.of("Salario", 0.25, "Occupazione", 0.25, "Istruzione", 0.25, "Affitto", 0.25);
    };

    // Push-pull weights
    Map<String, Double> pp = switch (cat) {
        case "Disoccupato_Diploma" -> Map.of("Salario", 0.3, "Occupazione", 0.5, "Servizi", 0.2);
        case "Disoccupato_Laurea" -> Map.of("Salario", 0.35, "Occupazione", 0.35, "Servizi", 0.3);
        case "Lavoratore_Diploma" -> Map.of("Salario", 0.3, "Occupazione", 0.35, "Servizi", 0.35);
        case "Lavoratore_Laurea" -> Map.of("Salario", 0.45, "Occupazione", 0.25, "Servizi", 0.3);
        default -> Map.of("Salario", 0.3, "Occupazione", 0.4, "Servizi", 0.3);
    };

    Agent a = new Agent(r, cat, eta, famiglia, anniStabile, anniDisoccupato, uw, pp, tutte, grid);
    DecisionUtils.adjustWeights(a);
    DecisionUtils.adjustUtilityPushWeights(a);
    return a;
}

@ScheduledMethod(start = 1, interval = 12)
public void step() {
    if (rnd.nextDouble() < 0.05) famiglia = !famiglia;

    DecisionUtils.adjustWeights(this);
    DecisionUtils.adjustUtilityPushWeights(this);

    double utilOrigNorm = DecisionUtils.utilityNorm(categoria, regione, utilityWeights);

    List<Regione> candidati = new ArrayList<>(regioni);
    candidati.removeIf(r -> r.nome.equals(regione.nome));
    Collections.shuffle(candidati);
    candidati = candidati.subList(0, Math.min(3, candidati.size()));

    List<String> righe = new ArrayList<>();
    boolean emigrato = false;

    for (Regione dest : candidati) {
        double distanza = DecisionUtils.distanza(regione, dest) * 111.0;
        double soglia = DecisionUtils.sogliaDecisionale(this, distanza);

        double gravityNorm = DecisionUtils.gravityNorm(categoria, regione, dest);
        double utilDestNorm = DecisionUtils.utilityNorm(categoria, dest, utilityWeights);
        double pushNorm = DecisionUtils.pushpullNorm(categoria, regione, dest, pushpullWeights);
        double rumore = DecisionUtils.noise(rnd, eta, famiglia, categoria.contains("Laurea"));

        double attr = wG * gravityNorm + wU * (utilDestNorm - utilOrigNorm) + wP * pushNorm;
        emigrato = attr > soglia + rumore;

        righe.add(String.format(Locale.US, "%d,%s,%s,%s,%b,%d,%.4f,%.4f,%d,%b,%.4f,%.4f,%.4f",
                id, categoria, regione.nome, dest.nome, famiglia, anniStabile,
                attr, soglia, eta, emigrato, gravityNorm, pushNorm, utilDestNorm - utilOrigNorm));

        if (emigrato) {
            regione = dest;
            griglia.moveTo(this, dest.getX(), dest.getY());
            anniStabile = 0;
            if (categoria.contains("Disoccupato")) anniDisoccupato++;
            break;
        }
    }

    if (!emigrato) 
        if (categoria.contains("Disoccupato")) anniDisoccupato++; else anniStabile++;
    

    if (!righe.isEmpty())
        DataManager.appendToCSV("data/log.csv", righe);

    eta++;
}

public String getCategoria() { return categoria; }
public boolean isFamiglia() { return famiglia; }
public int getEta() { return eta; }
public int getAnniStabile() { return anniStabile; }
public int getAnniDisoccupato() { return anniDisoccupato; }
public Map<String, Double> getUtilityWeights() { return utilityWeights; }
public Map<String, Double> getPushpullWeights() { return pushpullWeights; }
public void setW(double wg, double wu, double wp) { this.wG = wg; this.wU = wu; this.wP = wp; }

}