package simulation.utils;

import simulation.context.Regione;
import java.io.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Coordinate;

public class DataManager {

    public static void inizializzaLog(String path) { //header, crea se non c'è
        String header = "id_agente,categoria,origine,destinazione,famiglia,anni_stab,attrattivita,soglia,eta,emigrato,gravity,pp,uty T,anno";
        try {
            Files.writeString(Paths.get(path), header + "\n", StandardCharsets.UTF_8,StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Map<String, Regione> caricaRegioni(String path) {
        Map<String, Regione> regioni = new HashMap<>();
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(path), StandardCharsets.UTF_8)) {
            String line = reader.readLine();
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
                Coordinate coord = new Coordinate(Double.parseDouble(tokens[8]), Double.parseDouble(tokens[7]));

                Regione regione = new Regione(nome, popolazione, salario, occupazione, istruzione, affitto, servizi,coord);
                regioni.put(nome, regione);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return regioni;
    }

    public static void appendToCSV(String path, List<String> righe, int anno) {
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(path), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            for (String riga : righe) {
                writer.write(riga + "," + anno);
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Map<String, Map<String, Integer>>[] calcolaSaldi(String logPath, Map<String, Regione> regioni, Integer annoFiltro) {
        Map<String, Map<String, Integer>> entrate = new HashMap<>();
        Map<String, Map<String, Integer>> uscite = new HashMap<>();
        String[] categorie = {"Lavoratore_Laurea", "Lavoratore_Diploma", "Lavoratore_Licenza",
                             "Studente_Laurea", "Studente_Diploma", "Studente_Licenza",
                             "Disoccupato", "Pensionato"};

        // Initialize maps for all regions and categories
        for (String nomeRegione : regioni.keySet()) {
            entrate.put(nomeRegione, new HashMap<>());
            uscite.put(nomeRegione, new HashMap<>());
            for (String cat : categorie) {
                entrate.get(nomeRegione).put(cat, 0);
                uscite.get(nomeRegione).put(cat, 0);
            }
        }

        try (BufferedReader reader = Files.newBufferedReader(Paths.get(logPath), StandardCharsets.UTF_8)) {
            reader.readLine(); // Skip header
            String line;
            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split(",");
                if (tokens.length < 14) continue;

                int annoLog = Integer.parseInt(tokens[13]);
                if (annoFiltro != null && annoLog != annoFiltro) continue;

                String categoria = tokens[1];
                String origine = tokens[2];
                String destinazione = tokens[3];
                boolean emigrato = Boolean.parseBoolean(tokens[9]);

                if (!emigrato) continue;

                if (uscite.containsKey(origine) && uscite.get(origine).containsKey(categoria)) {
                    uscite.get(origine).merge(categoria, 1, Integer::sum);
                }
                if (entrate.containsKey(destinazione) && entrate.get(destinazione).containsKey(categoria)) {
                    entrate.get(destinazione).merge(categoria, 1, Integer::sum);
                }
            }
        } catch (IOException e) {
            System.err.println("Errore nella lettura del log: " + e.getMessage());
        }

        return new Map[]{entrate, uscite};
    }


    public static List<MigrationInfo> getMigrationsToVisualize(String logPath, int anno, double sogliaPercentuale) {
        Map<String, Integer> migrazioni = new HashMap<>();
        int totaleMigrazioniAnno = 0;

        try (BufferedReader reader = Files.newBufferedReader(Paths.get(logPath), StandardCharsets.UTF_8)) {
            String header = reader.readLine();
            String line;
            
            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split(",");
               
                int annoLog = Integer.parseInt(tokens[13]);
                if (annoLog != anno) continue;

                String categoria = tokens[1];
                String origine = tokens[2];
                String destinazione = tokens[3];
                boolean emigrato = Boolean.parseBoolean(tokens[9]);

                if (!emigrato) continue;

                String chiave = categoria + "||" + origine + "||" + destinazione;
                migrazioni.merge(chiave, 1, Integer::sum);
                totaleMigrazioniAnno++;
            }

            List<MigrationInfo> risultato = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : migrazioni.entrySet()) {
                double percentuale = (double) entry.getValue() / totaleMigrazioniAnno;
                
                if (percentuale >= sogliaPercentuale) {
                    String[] parti = entry.getKey().split("\\|\\|");
                    risultato.add(new MigrationInfo(parti[0], parti[1], parti[2], entry.getValue()));
                }
            }
            return risultato;

        } catch (IOException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }
    
    public static void stampaSaldoAnnuale(String logPath, Map<String, Regione> regioni, int anno, int agentiPerRegione) throws IOException {
        Map<String, Map<String, Integer>>[] saldi = calcolaSaldi(logPath, regioni, anno);
        Map<String, Map<String, Integer>> entrate = saldi[0];
        Map<String, Map<String, Integer>> uscite = saldi[1];

        List<String> righeCSV = new ArrayList<>();
        if (!Files.exists(Paths.get("data/saldo.csv"))) {
            righeCSV.add("Anno,Regione,Categoria,Entrate_permille,Uscite_permille,Saldo_permille");
        }

        System.out.println("\n" + "=".repeat(80));
        System.out.println(" SALDO MIGRATORIO ANNO " + (anno + 1) + " (‰ sulla popolazione reale per categoria)");
        System.out.println("=".repeat(80));

        for (String nomeRegione : regioni.keySet()) {
            Regione regione = regioni.get(nomeRegione);
            double popolazioneReale = regione.popolazione;
            System.out.println("\n  " + nomeRegione.toUpperCase() + ":");

            for (String categoria : entrate.get(nomeRegione).keySet()) {
                int in = entrate.get(nomeRegione).get(categoria);
                int out = uscite.get(nomeRegione).get(categoria);
                double popolazioneCategoria = calcolaPopulazioneCategoria(categoria, popolazioneReale);
                double fattoreScala = popolazioneCategoria / (agentiPerRegione / 8.0);
                double permilleEntrate = (in * fattoreScala / popolazioneCategoria) * 1000;
                double permilleUscite = (out * fattoreScala / popolazioneCategoria) * 1000;
                double saldoPermille = permilleEntrate - permilleUscite;

                if (in > 0 || out > 0) {
                    System.out.printf("   %s: +%.1f‰ guadagnati, -%.1f‰ persi → saldo %.1f‰\n",
                            categoria, permilleEntrate, permilleUscite, saldoPermille);
                }
                righeCSV.add(String.format(Locale.US, "%d,%s,%s,%.1f,%.1f,%.1f",
                        anno + 1, nomeRegione, categoria, permilleEntrate, permilleUscite, saldoPermille));
            }
        }
        System.out.println("=".repeat(80) + "\n");

        Files.write(Paths.get("data/saldo.csv"), righeCSV, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    public static void stampaSaldoFinale(String logPath, Map<String, Regione> regioni, int agentiPerRegione) throws IOException {
        Map<String, Map<String, Integer>>[] saldi = calcolaSaldi(logPath, regioni, null);
        Map<String, Map<String, Integer>> entrate = saldi[0];
        Map<String, Map<String, Integer>> uscite = saldi[1];

        List<String> righeCSV = new ArrayList<>();
        righeCSV.add("FINALE,TOTALE_SIMULAZIONE,---,---,---,---");

        System.out.println("\n" + "=".repeat(80));
        System.out.println(" SALDO MIGRATORIO FINALE - INTERA SIMULAZIONE (‰ sulla popolazione reale per categoria)");
        System.out.println("=".repeat(80));

        for (String nomeRegione : regioni.keySet()) {
            Regione regione = regioni.get(nomeRegione);
            double popolazioneReale = regione.popolazione;
            System.out.println("\n  " + nomeRegione.toUpperCase() + ":");

            for (String categoria : entrate.get(nomeRegione).keySet()) {
                int in = entrate.get(nomeRegione).get(categoria);
                int out = uscite.get(nomeRegione).get(categoria);
                double popolazioneCategoria = calcolaPopulazioneCategoria(categoria, popolazioneReale);
                double fattoreScala = popolazioneCategoria / (agentiPerRegione / 8.0);
                double permilleEntrate = (in * fattoreScala / popolazioneCategoria) * 1000;
                double permilleUscite = (out * fattoreScala / popolazioneCategoria) * 1000;
                double saldoPermille = permilleEntrate - permilleUscite;

                if (in > 0 || out > 0) {
                    System.out.printf("   %s: +%.1f‰ guadagnati, -%.1f‰ persi → saldo %.1f‰\n",
                            categoria, permilleEntrate, permilleUscite, saldoPermille);
                }
                righeCSV.add(String.format(Locale.US, "FINALE,%s,%s,%.1f,%.1f,%.1f",
                        nomeRegione, categoria, permilleEntrate, permilleUscite, saldoPermille));
            }
        }
        System.out.println("=".repeat(80) + "\n");

        Files.write(Paths.get("data/saldo.csv"), righeCSV, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }
    // Metodo helper per calcolare la popolazione approssimativa per categoria
    private static double calcolaPopulazioneCategoria(String categoria, double popolazioneTotale) {
        // Percentuali approssimative basate sui dati demografici italiani
        switch (categoria) {
            case "Lavoratore_Laurea": return popolazioneTotale * 0.08;      // 8%
            case "Lavoratore_Diploma": return popolazioneTotale * 0.25;     // 25%
            case "Lavoratore_Licenza": return popolazioneTotale * 0.15;     // 15%
            case "Studente_Laurea": return popolazioneTotale * 0.03;        // 3%
            case "Studente_Diploma": return popolazioneTotale * 0.05;       // 5%
            case "Studente_Licenza": return popolazioneTotale * 0.02;       // 2%
            case "Disoccupato": return popolazioneTotale * 0.10;            // 10%
            case "Pensionato": return popolazioneTotale * 0.22;             // 22%
            default: return popolazioneTotale * 0.125; // 1/8 se categoria sconosciuta
        }
    }

    public static class MigrationInfo {
        public String categoria;
        public String origine;
        public String destinazione;
        public int conteggio;

        public MigrationInfo(String categoria, String origine, String destinazione, int conteggio) {
            this.categoria = categoria;
            this.origine = origine;
            this.destinazione = destinazione;
            this.conteggio = conteggio;
        }
    }
}