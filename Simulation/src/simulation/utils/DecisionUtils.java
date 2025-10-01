package simulation.utils;import simulation.agent.Agent;
import simulation.context.Regione;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Point;
import java.util.*;
public class DecisionUtils {

	public static final double[] AGE_DISTRIBUTION_CUMULATIVE = {
		    0.071,   // 18–22 → 7.1%
		    0.164,   // 23–27 → 9.3%
		    0.276,   // 28–32 → 11.2%
		    0.404,   // 33–37 → 12.8%
		    0.541,   // 38–42 → 13.7%
		    0.687,   // 43–47 → 14.6%
		    0.824,   // 48–52 → 13.7%
		    0.931,   // 53–57 → 10.7%
		    0.996,   // 58–62 → 6.5%
		    1.000    // 63–64 → 1.4%
		};


    public static final Map<String, Map<String, Double>> utilityWeightsByCat = Map.of(
        "Disoccupato_Diploma", Map.of("Salario", 0.2, "Occupazione", 0.4, "Istruzione", 0.15, "Affitto", 0.25),
        "Disoccupato_Laurea", Map.of("Salario", 0.25, "Occupazione", 0.25, "Istruzione", 0.25, "Affitto", 0.25),
        "Lavoratore_Diploma", Map.of("Salario", 0.35, "Occupazione", 0.45, "Istruzione", 0.01, "Affitto", 0.19),
        "Lavoratore_Laurea", Map.of("Salario", 0.3, "Occupazione", 0.3, "Istruzione", 0.3, "Affitto", 0.1)
    );

    public static final Map<String, Map<String, Double>> pushpullWeightsByCat = Map.of(
        "Disoccupato_Diploma", Map.of("Salario", 0.3, "Occupazione", 0.5, "Servizi", 0.2),
        "Disoccupato_Laurea", Map.of("Salario", 0.35, "Occupazione", 0.3, "Servizi", 0.35),
        "Lavoratore_Diploma", Map.of("Salario", 0.3, "Occupazione", 0.40, "Servizi", 0.3),
        "Lavoratore_Laurea", Map.of("Salario", 0.45, "Occupazione", 0.2, "Servizi", 0.35)
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
    double dx = a.coordinate.y - b.coordinate.y; // y is latitude
    double dy = a.coordinate.x - b.coordinate.x; // x is longitude
    return Math.sqrt(dx * dx + dy * dy);
}


public static double sogliaDecisionale(Agent a, double distanza) {
    String categoria = a.getCategoria();
    int eta = a.getEta();
    int anniStabile = a.getAnniStabile();
    int anniDisoccupato = a.getAnniDisoccupato();
    int anniLavorati = a.getAnniLavorati();
    boolean famiglia = a.isFamiglia();

    // Base soglia per categoria
    double soglia = switch (categoria) {
        case "Disoccupato_Diploma" -> 0.25;
        case "Disoccupato_Laurea" -> 0.20; 
        case "Lavoratore_Diploma" -> 0.32;
        case "Lavoratore_Laurea" -> 0.2;   // Più mobili dei diplomati
        default -> 0.20;
    };


    double etaFactor = 0.0;
    if (eta <= 27) {
        double t = (27 - Math.max(18, eta)) / 9.0;  // normalizzato da 18 a 27 anni
        etaFactor = -0.035 * t;
        
        if (categoria.contains("Laurea") && eta <= 26) { // Bonus neolaureati (leggermente ridotto: spesso sono gia emigrati in un altra regione per l'universita)
            etaFactor -= 0.04;
        }
    }
    else if (eta <= 35) {
        // Fascia 28-35: LEGGERO BONUS per correggere il crollo dato da percentuale famiglia
        etaFactor = -0.02 * (1.0 - (eta - 28) / 7.0);  // Piccolo bonus, max -0.02 per i 28enni
    }
    else if (eta <= 40) {
        // Fascia 35-40: leggera resistenza crescente
        etaFactor = 0.08 * ((eta - 35) / 5.0);  
    }
    else if (eta <= 50) {
        // Fascia 40-50
        etaFactor = 0.09 * ((eta - 35) / 15.0);  // max +0.09 a 50 anni
    }
    else { // Over 50
        etaFactor = 0.095 + 0.19 * Math.min(1.0, (eta - 50) / 15.0);  // Da +0.095 (a 50) a +0.28 max
    }

    soglia += etaFactor;

//famiglia considerando gia l'età che influenza la probabilità di averla per evitare doppie penalizzazioni
    if (famiglia) {
        double famigliaFactor = 0.11;  // Base penalty uniforme
        
        // Mitigazione per professionisti qualificati con esperienza che possono permettersi trasferimenti familiari
        if (categoria.contains("Laurea") && anniLavorati > 5) {
            famigliaFactor *= 0.75;  // Riduce penalty del 25%
        }
        
        // Mitigazione per lavoratori stabili con buon reddito
        if (categoria.contains("Lavoratore") && anniStabile > 8) {
            famigliaFactor *= 0.85;  // Riduce penalty del 15%
        }
        
        soglia += famigliaFactor;
    }


    // stabilità lavorativa che considera sia esperienza che stabilità
    if (categoria.contains("Lavoratore")) {
        // Per i lavoratori: stabilità aumenta resistenza, ma esperienza può facilitare lo spostamento verso zone >utility
        double stabilitaFactor = 0.0;
        
        if (anniStabile > 0) {
            // Resistenza cresce logaritmicamente (non linearmente)
            stabilitaFactor = 0.12 * Math.log(1 + anniStabile / 3.0);
            
            // Ma se ha molta esperienza totale, è più facile trovare lavoro altrove
            if (anniLavorati > 8) {
                stabilitaFactor *= 0.7;  // Riduce la resistenza del 30%
            }
            
            // Bonus mobilità per professionisti senior altamente qualificati
            if (categoria.contains("Laurea") && anniLavorati > 10 && eta < 45) {
                stabilitaFactor *= 0.5;  // Dimezza la resistenza
            }
        }
        
        soglia += stabilitaFactor;
    }
    else if (categoria.contains("Disoccupato")) {
        //disoccupati da poco incentivati a rimanere nel mercato del lavoro locale, bilanciato dall'esperienza che abbassa la soglia
        
        if (anniLavorati > 0) {
            if (anniDisoccupato <= 1) {
                soglia += 0.08;  
            } else if (anniDisoccupato <= 3) {
                soglia -= 0.05 * (anniDisoccupato - 1); 
            } else {
                soglia -= 0.10;  
                
                if (anniLavorati > 5) {
                    soglia -= 0.08;
                }
            }
        } else {             // Nessuna esperienza: più disposto a spostarsi se giovane (neo exstudente)
            if (eta <= 25) {
                soglia -= 0.12;
            } else {
                soglia -= 0.05;
            }
        }
    }

    //Distanza crescita esponenziale per mitigare punti centrali emigrazione
    if (distanza > 100) {
        double distanzaFactor = 0.0;
        
        if (distanza <= 300) { // 100-300 km: resistenza moderat
            
            distanzaFactor = (distanza - 100) / 200.0 * 0.15;  // Max +0.15
        } else if (distanza <= 600) {
            // 300-600 km: resistenza crescente
            double t = (distanza - 300) / 300.0;
            distanzaFactor = 0.15 + 0.25 * t;  // Da +0.15 a +0.40
        } else {
            // Over 600 km: resistenza alta ma non proibitiva
            double t = Math.min(1.0, (distanza - 600) / 400.0);
            distanzaFactor = 0.40 + 0.20 * t;  // Da +0.40 a +0.60 max
        }
        
        // Mitigazione per professionisti altamente qualificati
        if (categoria.contains("Laurea") && anniLavorati > 5) {
            distanzaFactor *= 0.83;
        }
        
        // Mitigazione per giovani (più adattabili) bilanciato da probabilità famiglia con crescita elevata dai 33
        if (eta <= 30)
            distanzaFactor *= 0.7;
        
        soglia += distanzaFactor;
    }

    return Math.max(0.05, Math.min(3.5, soglia));  // Range esteso per gestire penalità distanza più alte
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