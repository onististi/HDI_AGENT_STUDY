package simulation.utils;import simulation.agent.Agent;
import simulation.context.Regione;import java.util.*;public class DecisionUtils {

public static final Map<String, Map<String, Double>> utilityWeightsByCat = Map.of(
        "Disoccupato_Diploma", Map.of("Salario", 0.2, "Occupazione", 0.4, "Istruzione", 0.1, "Affitto", 0.25),
        "Disoccupato_Laurea", Map.of("Salario", 0.25, "Occupazione", 0.25, "Istruzione", 0.25, "Affitto", 0.25),
        "Lavoratore_Diploma", Map.of("Salario", 0.35, "Occupazione", 0.45, "Istruzione", 0.01, "Affitto", 0.19),
        "Lavoratore_Laurea", Map.of("Salario", 0.3, "Occupazione", 0.3, "Istruzione", 0.3, "Affitto", 0.1)
    );

    public static final Map<String, Map<String, Double>> pushpullWeightsByCat = Map.of(
        "Disoccupato_Diploma", Map.of("Salario", 0.3, "Occupazione", 0.5, "Servizi", 0.2),
        "Disoccupato_Laurea", Map.of("Salario", 0.35, "Occupazione", 0.35, "Servizi", 0.3),
        "Lavoratore_Diploma", Map.of("Salario", 0.3, "Occupazione", 0.35, "Servizi", 0.35),
        "Lavoratore_Laurea", Map.of("Salario", 0.45, "Occupazione", 0.25, "Servizi", 0.3)
    );
    
      /**
     * Mappa degli estremi per ciascuna categoria.
     * 
     * Chiave esterna: nome della categoria (es. "Disoccupato_Laurea")
     * Valore: mappa di stringhe che rappresentano i nomi dei parametri normalizzati e i relativi valori.
     *
     * Chiavi interne comuni:
     * - "min_utility", "max_utility" → per normalizzazione dell'utilità
     * - "min_gravity", "max_gravity" → per normalizzazione della gravità
     * - "min_pushpull", "max_pushpull" → estremi del push-pull
     * - "mid_pushpull", "half_range_pushpull" → per normalizzazione simmetrica push-pull (-1, 1)
     *
     * Esempio di accesso:
     *   double minU = estremiPerCategoria.get("Lavoratore_Laurea").get("min_utility");
     */
    private static final Map<String, Map<String, Double>> estremiPerCategoria = new HashMap<>();
    public static void calcolaEstremiPerCategoria(List<Regione> regioni) {
        estremiPerCategoria.clear();

        for (String cat : utilityWeightsByCat.keySet()) {
            Map<String, Double> uw = utilityWeightsByCat.get(cat);
            Map<String, Double> ppw = pushpullWeightsByCat.get(cat);

            double minU = Double.MAX_VALUE, maxU = -Double.MAX_VALUE;
            double minG = Double.MAX_VALUE, maxG = -Double.MAX_VALUE;
            double minP = Double.MAX_VALUE, maxP = -Double.MAX_VALUE;

            for (Regione r : regioni) {
                double u = utilityRaw(r, uw);
                minU = Math.min(minU, u);
                maxU = Math.max(maxU, u);
            }

            for (Regione r1 : regioni) {
                for (Regione r2 : regioni) {
                    if (!r1.nome.equals(r2.nome)) {
                        double g = gravityRaw(r1, r2);
                        double p = pushpullRaw(r1, r2, ppw);
                        minG = Math.min(minG, g);
                        maxG = Math.max(maxG, g);
                        minP = Math.min(minP, p);
                        maxP = Math.max(maxP, p);
                    }
                }
            }

            double midP = (minP + maxP) / 2.0;
            double halfRangeP = (maxP - minP) / 2.0 + 1e-6;

            Map<String, Double> estremi = new HashMap<>();
            estremi.put("min_utility", minU);
            estremi.put("max_utility", maxU);
            estremi.put("min_gravity", minG);
            estremi.put("max_gravity", maxG);
            estremi.put("min_pushpull", minP);
            estremi.put("max_pushpull", maxP);
            estremi.put("mid_pushpull", midP);
            estremi.put("half_range_pushpull", halfRangeP);

            estremiPerCategoria.put(cat, estremi);
        }
    }
// Metodi normalizzati per una data categoria
public static double utilityNorm(String categoria, Regione r, Map<String, Double> w) {
    double u = utilityRaw(r, w);
    Map<String, Double> e = estremiPerCategoria.get(categoria);
    return normalizza(u, e.get("min_utility"), e.get("max_utility"));
}

public static double gravityNorm(String categoria, Regione a, Regione b) {
    double g = gravityRaw(a, b);
    Map<String, Double> e = estremiPerCategoria.get(categoria);
    return normalizza(g, e.get("min_gravity"), e.get("max_gravity"));
}

public static double pushpullNorm(String categoria, Regione a, Regione b, Map<String, Double> w) {
    double p = pushpullRaw(a, b, w);
    Map<String, Double> e = estremiPerCategoria.get(categoria);
    return (p - e.get("mid_pushpull")) / e.get("half_range_pushpull");
}

// Metodi RAW
private static double utilityRaw(Regione r, Map<String, Double> w) {
    return w.get("Salario") * r.salario +
           w.get("Occupazione") * r.occupazione +
           w.get("Istruzione") * r.istruzione -
           w.get("Affitto") * r.affitto;
}

private static double gravityRaw(Regione a, Regione b) {
    double dist = distanza(a, b) + 0.001;
    return (a.popolazione * b.popolazione) / (dist * dist);
}

private static double pushpullRaw(Regione orig, Regione dest, Map<String, Double> w) {
    return w.get("Salario") * (dest.salario - orig.salario) +
           w.get("Occupazione") * (dest.occupazione - orig.occupazione) +
           w.get("Servizi") * (dest.servizi - orig.servizi);
}

// Normalizzazione standard [0,1]
private static double normalizza(double x, double min, double max) {
    return Math.max(0, Math.min(1, (x - min) / (max - min + 1e-6)));
}

// Rumore
public static double noise(Random rnd, int eta, boolean famiglia, boolean laurea) {
    double sigma = eta < 30 ? 0.04 : (eta < 40 ? 0.02 : 0.005);
    if (famiglia) sigma *= 0.5;
    if (laurea) sigma *= 0.8;
    return rnd.nextGaussian() * sigma;
}

public static double distanza(Regione a, Regione b) {
    double dx = a.latitudine - b.latitudine;
    double dy = a.longitudine - b.longitudine;
    return Math.sqrt(dx * dx + dy * dy);
}

public static double sogliaDecisionale(Agent a, double distanza) {
    String categoria = a.getCategoria();
    int eta = a.getEta();
    int anniStabile = a.getAnniStabile();
    int anniDisoccupato = a.getAnniDisoccupato();
    boolean famiglia = a.isFamiglia();

    double soglia = switch (categoria) {
        case "Disoccupato_Diploma" -> 0.3;
        case "Disoccupato_Laurea" -> 0.2;
        case "Lavoratore_Diploma" -> 0.35;
        case "Lavoratore_Laurea" -> 0.25;
        default -> 0.25;
    };

    if (famiglia) soglia += 0.12;
    if (eta > 35) soglia += Math.ceil(eta / 10.0) * 0.05;
    if (anniStabile > 2) soglia += anniStabile * 0.08;
    if (categoria.contains("Disoccupato") && anniDisoccupato < 2) soglia += 0.01;

    return soglia + ((distanza > 200) ? ((distanza - 200) / 100.0) * 0.05 : 0);
}

public static void adjustWeights(Agent a) {
    double wG = 0.33, wU = 0.33, wP = 0.34;
    if (a.isFamiglia()) {
        wG *= 1.5; wU *= 0.7; wP *= 1.2;
    }
    if (a.getEta() < 30) {
        wG *= 0.7; wU *= 0.95; wP *= 0.95;
    }
    double total = wG + wU + wP;
    a.setW(wG / total, wU / total, wP / total);
}

public static void adjustUtilityPushWeights(Agent a) {
    Map<String, Double> uw = a.getUtilityWeights();
    if (a.isFamiglia()) uw.put("Affitto", uw.get("Affitto") * 1.4);
    if (a.getEta() > 40) uw.put("Istruzione", uw.get("Istruzione") * 0.4);
    double sumU = uw.values().stream().mapToDouble(Double::doubleValue).sum();
    uw.replaceAll((k, v) -> v / sumU);

    Map<String, Double> pp = a.getPushpullWeights();
    if (a.isFamiglia()) pp.put("Servizi", pp.get("Servizi") * 1.2);
    if (a.getEta() < 30) pp.put("Salario", pp.get("Salario") * 1.2);
    double sumP = pp.values().stream().mapToDouble(Double::doubleValue).sum();
    pp.replaceAll((k, v) -> v / sumP);
}
}