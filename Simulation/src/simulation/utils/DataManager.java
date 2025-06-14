package simulation.utils;import simulation.context.Regione;
import java.io.*;
import java.util.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;public class DataManager {

public static void inizializzaLog(String path) {
    String header = "id_agente,categoria,origine,destinazione,famiglia,anni_stab,attrattivita,soglia,eta,emigrato,gravity,pp,uty T";
    try {
        Files.writeString(Paths.get(path), header + "\n", StandardCharsets.UTF_8,
                          StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    } catch (IOException e) {
        e.printStackTrace();
    }
}

public static Map<String, Regione> caricaRegioni(String path) {
    Map<String, Regione> regioni = new HashMap<>();
    try (BufferedReader reader = Files.newBufferedReader(Paths.get(path), StandardCharsets.UTF_8)) {
        String line = reader.readLine(); // header
        while ((line = reader.readLine()) != null) {
            String[] tokens = line.split(",");
            if (tokens.length < 9) continue;
            String nome = tokens[0];
            double popolazione = Double.parseDouble(tokens[1]);
            double salario = Double.parseDouble(tokens[2]);
            double occupazione = Double.parseDouble(tokens[3]);
            double istruzione = Double.parseDouble(tokens[4]);
            double affitto = Double.parseDouble(tokens[5]);
            double servizi = Double.parseDouble(tokens[6]);
            double lat = Double.parseDouble(tokens[7]);
            double lon = Double.parseDouble(tokens[8]);

            Regione regione = new Regione(nome, popolazione, salario, occupazione,
                                          istruzione, affitto, servizi, lat, lon);
            regioni.put(nome, regione);
        }
    } catch (IOException e) {
        e.printStackTrace();
    }
    return regioni;
}

public static void salvaMigrazione(String path, List<String> righe) {
    try {
        Files.write(Paths.get(path), righe, StandardCharsets.UTF_8);
    } catch (IOException e) {
        e.printStackTrace();
    }
}

public static void appendToCSV(String path, List<String> righe) {
    try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(path), StandardCharsets.UTF_8,
            StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
        for (String riga : righe) {
            writer.write(riga);
            writer.newLine();
        }
    } catch (IOException e) {
        e.printStackTrace();
    }
}

public static void calcolaSaldoAnnuale(String logPath, String outputPath) {
    Map<String, Map<String, Integer>> entrate = new HashMap<>();
    Map<String, Map<String, Integer>> uscite = new HashMap<>();
    Map<String, Integer> saldoTotaleRegione = new HashMap<>();
    int saldoTotaleItalia = 0;

    try (BufferedReader reader = Files.newBufferedReader(Paths.get(logPath), StandardCharsets.UTF_8)) {
        String header = reader.readLine(); // salta intestazione
        String line;

        while ((line = reader.readLine()) != null) {
            String[] tokens = line.split(",");
            if (tokens.length < 13) continue;

            String categoria = tokens[1];
            String origine = tokens[2];
            String destinazione = tokens[3];
            boolean emigrato = Boolean.parseBoolean(tokens[9]);

            if (!emigrato) continue;

            // aggiorna uscite
            uscite.putIfAbsent(origine, new HashMap<>());
            uscite.get(origine).merge(categoria, 1, Integer::sum);
            saldoTotaleRegione.merge(origine, -1, Integer::sum);

            // aggiorna entrate
            entrate.putIfAbsent(destinazione, new HashMap<>());
            entrate.get(destinazione).merge(categoria, 1, Integer::sum);
            saldoTotaleRegione.merge(destinazione, 1, Integer::sum);

            saldoTotaleItalia++;
        }

        // prepara righe da salvare
        List<String> righe = new ArrayList<>();
        righe.add("Regione,Categoria,Entrate,Uscite,Saldo,Saldo_Regionale");

        Set<String> tutteRegioni = new HashSet<>();
        tutteRegioni.addAll(entrate.keySet());
        tutteRegioni.addAll(uscite.keySet());

        for (String regione : tutteRegioni) {
            Set<String> tutteCategorie = new HashSet<>();
            if (entrate.containsKey(regione)) tutteCategorie.addAll(entrate.get(regione).keySet());
            if (uscite.containsKey(regione)) tutteCategorie.addAll(uscite.get(regione).keySet());

            for (String cat : tutteCategorie) {
                int in_ = entrate.getOrDefault(regione, Map.of()).getOrDefault(cat, 0);
                int out = uscite.getOrDefault(regione, Map.of()).getOrDefault(cat, 0);
                int saldo = in_ - out;
                int saldoReg = saldoTotaleRegione.getOrDefault(regione, 0);
                righe.add(String.format(Locale.US, "%s,%s,%d,%d,%d,%d", regione, cat, in_, out, saldo, saldoReg));
            }
        }

        // saldo complessivo
        righe.add(String.format(Locale.US, "Totale,,,,%d,", saldoTotaleItalia));

        // salva
        Files.write(Paths.get(outputPath), righe, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);

    } catch (IOException e) {
        e.printStackTrace();
    }
}

/**
 * Stampa il saldo migratorio di ogni regione sulla console in formato percentuale
 * ordinato per saldo decrescente per un anno specifico
 * @throws IOException 
 */
public static void stampaSaldoMigratorioConsole(String logPath, Map<String, Regione> regioni, int annoCorrente) throws IOException {
    Map<String, Integer> entrate = new HashMap<>();
    Map<String, Integer> uscite = new HashMap<>();
    Map<String, Double> popolazioneOriginale = new HashMap<>();

    // Inizializza le popolazioni originali
    for (Regione r : regioni.values()) {
        popolazioneOriginale.put(r.nome, r.popolazione * 10); // moltiplicato per 10 come nel context
        entrate.put(r.nome, 0);
        uscite.put(r.nome, 0);
    }

    try (BufferedReader reader = Files.newBufferedReader(Paths.get(logPath), StandardCharsets.UTF_8)) {
        String header = reader.readLine(); // salta intestazione
        String line;

        while ((line = reader.readLine()) != null) {
            String[] tokens = line.split(",");
            if (tokens.length < 10) continue;

            String origine = tokens[2];
            String destinazione = tokens[3];
            boolean emigrato = Boolean.parseBoolean(tokens[9]);

            if (!emigrato) continue;

            // Conta le migrazioni
            uscite.merge(origine, 1, Integer::sum);
            entrate.merge(destinazione, 1, Integer::sum);
        }

        // Calcola percentuali e crea lista per ordinamento
        List<RegioneSaldo> risultati = new ArrayList<>();
        
        for (String nomeRegione : regioni.keySet()) {
            int in = entrate.getOrDefault(nomeRegione, 0);
            int out = uscite.getOrDefault(nomeRegione, 0);
            double popOriginale = popolazioneOriginale.get(nomeRegione);
            
            double percEntrate = (in / popOriginale) * 100;
            double percUscite = (out / popOriginale) * 100;
            double saldo = percEntrate - percUscite;
            
            risultati.add(new RegioneSaldo(nomeRegione, percEntrate, percUscite, saldo));
        }

        // Ordina per saldo decrescente
        risultati.sort((a, b) -> Double.compare(b.saldo, a.saldo));

        // Stampa risultati
        System.out.println("\n" + "=".repeat(80));
        System.out.println("📊 SALDO MIGRATORIO ANNO " + (annoCorrente/12 + 1) + " (% sulla popolazione iniziale)");
        System.out.println("=".repeat(80));
        
        for (RegioneSaldo r : risultati) {
            String icona = r.saldo > 0 ? "🟢" : (r.saldo < 0 ? "🔴" : "🟡");
            System.out.printf("%s %s: +%.2f%% guadagnati, -%.2f%% persi → saldo %.2f%%\n",
                icona, r.nome, r.percEntrate, r.percUscite, r.saldo);
        }
        System.out.println("=".repeat(80) + "\n");
    }
}
/**
 * Stampa il saldo migratorio finale di ogni regione sulla console
 */
public static void stampaSaldoMigratorioFinale(String logPath, Map<String, Regione> regioni) {
    Map<String, Integer> entrate = new HashMap<>();
    Map<String, Integer> uscite = new HashMap<>();
    Map<String, Double> popolazioneOriginale = new HashMap<>();

    // Inizializza le popolazioni originali
    for (Regione r : regioni.values()) {
        popolazioneOriginale.put(r.nome, r.popolazione * 10);
        entrate.put(r.nome, 0);
        uscite.put(r.nome, 0);
    }

    try (BufferedReader reader = Files.newBufferedReader(Paths.get(logPath), StandardCharsets.UTF_8)) {
        String header = reader.readLine();
        String line;

        while ((line = reader.readLine()) != null) {
            String[] tokens = line.split(",");
            if (tokens.length < 10) continue;

            String origine = tokens[2];
            String destinazione = tokens[3];
            boolean emigrato = Boolean.parseBoolean(tokens[9]);

            if (!emigrato) continue;

            uscite.merge(origine, 1, Integer::sum);
            entrate.merge(destinazione, 1, Integer::sum);
        }

        List<RegioneSaldo> risultati = new ArrayList<>();
        
        for (String nomeRegione : regioni.keySet()) {
            int in = entrate.getOrDefault(nomeRegione, 0);
            int out = uscite.getOrDefault(nomeRegione, 0);
            double popOriginale = popolazioneOriginale.get(nomeRegione);
            
            double percEntrate = (in / popOriginale) * 100;
            double percUscite = (out / popOriginale) * 100;
            double saldo = percEntrate - percUscite;
            
            risultati.add(new RegioneSaldo(nomeRegione, percEntrate, percUscite, saldo));
        }

        risultati.sort((a, b) -> Double.compare(b.saldo, a.saldo));

        System.out.println("\n" + "=".repeat(80));
        System.out.println("🏆 SALDO MIGRATORIO FINALE - 10 ANNI (% sulla popolazione iniziale)");
        System.out.println("=".repeat(80));
        
        for (RegioneSaldo r : risultati) {
            String icona = r.saldo > 0 ? "🟢" : (r.saldo < 0 ? "🔴" : "🟡");
            System.out.printf("%s %s: +%.2f%% guadagnati, -%.2f%% persi → saldo %.2f%%\n",
                icona, r.nome, r.percEntrate, r.percUscite, r.saldo);
        }
        System.out.println("=".repeat(80) + "\n");

    } catch (IOException e) {
        System.err.println("Errore nel calcolo del saldo migratorio finale: " + e.getMessage());
        e.printStackTrace();
    }
}

/**
 * Classe di supporto per ordinare i risultati
 */
	private static class RegioneSaldo {
	    String nome;
	    double percEntrate;
	    double percUscite;
	    double saldo;
	    
	    RegioneSaldo(String nome, double percEntrate, double percUscite, double saldo) {
	        this.nome = nome;
	        this.percEntrate = percEntrate;
	        this.percUscite = percUscite;
	        this.saldo = saldo;
	    }
	}
}