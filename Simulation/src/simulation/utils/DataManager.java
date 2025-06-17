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

    public static void inizializzaLog(String path) {
        String header = "id_agente,categoria,origine,destinazione,famiglia,anni_stab,attrattivita,soglia,eta,emigrato,gravity,pp,uty T,anno";
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

    public static void salvaMigrazione(String path, List<String> righe) {
        try {
            Files.write(Paths.get(path), righe, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
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

    public static void calcolaSaldoAnnuale(String logPath, String outputPath) {
        Map<String, Map<String, Integer>> entrate = new HashMap<>();
        Map<String, Map<String, Integer>> uscite = new HashMap<>();
        Map<String, Integer> saldoTotaleRegione = new HashMap<>();
        int saldoTotaleItalia = 0;

        try (BufferedReader reader = Files.newBufferedReader(Paths.get(logPath), StandardCharsets.UTF_8)) {
            String header = reader.readLine();
            String line;

            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split(",");
                if (tokens.length < 14) continue;

                String categoria = tokens[1];
                String origine = tokens[2];
                String destinazione = tokens[3];
                boolean emigrato = Boolean.parseBoolean(tokens[9]);

                if (!emigrato) continue;

                uscite.putIfAbsent(origine, new HashMap<>());
                uscite.get(origine).merge(categoria, 1, Integer::sum);
                saldoTotaleRegione.merge(origine, -1, Integer::sum);

                entrate.putIfAbsent(destinazione, new HashMap<>());
                entrate.get(destinazione).merge(categoria, 1, Integer::sum);
                saldoTotaleRegione.merge(destinazione, 1, Integer::sum);

                saldoTotaleItalia++;
            }

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

            righe.add(String.format(Locale.US, "Totale,,,,%d,", saldoTotaleItalia));

            Files.write(Paths.get(outputPath), righe, StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void stampaSaldoMigratorioConsole(String logPath, Map<String, Regione> regioni, int annoCorrente) throws IOException {
        Map<String, Integer> entrate = new HashMap<>();
        Map<String, Integer> uscite = new HashMap<>();
        Map<String, Double> popolazioneOriginale = new HashMap<>();

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
                if (tokens.length < 14) continue;

                int annoLog = Integer.parseInt(tokens[13]);
                if (annoLog != (annoCorrente / 12)) continue;

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
            System.out.println("📊 SALDO MIGRATORIO ANNO " + (annoCorrente / 12 + 1) + " (% sulla popolazione iniziale)");
            System.out.println("=".repeat(80));

            for (RegioneSaldo r : risultati) {
                String icona = r.saldo > 0 ? "🟢" : (r.saldo < 0 ? "🔴" : "🟡");
                System.out.printf("%s %s: +%.2f%% guadagnati, -%.2f%% persi → saldo %.2f%%\n",
                        icona, r.nome, r.percEntrate, r.percUscite, r.saldo);
            }
            System.out.println("=".repeat(80) + "\n");
        }
    }

    public static void stampaSaldoMigratorioFinale(String logPath, Map<String, Regione> regioni) {
        Map<String, Integer> entrate = new HashMap<>();
        Map<String, Integer> uscite = new HashMap<>();
        Map<String, Double> popolazioneOriginale = new HashMap<>();

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
                if (tokens.length < 14) continue;

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