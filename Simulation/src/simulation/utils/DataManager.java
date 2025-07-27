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
        String header = "id_agente,categoria,origine,destinazione,famiglia,anni_stab,anni_lavoro,attrattivita,soglia,eta,emigrato,gravity,pp,uty_T,anno";
        try {
            Files.writeString(Paths.get(path), header + "\n", StandardCharsets.UTF_8,StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            
            String headerSaldo = "Anno,Regione,Categoria,Entrate_permille,Uscite_permille,Saldo_permille";
            Files.writeString(Paths.get("data/saldo.csv"), headerSaldo + "\n", StandardCharsets.UTF_8,StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
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
                double fattoreFamiliare = Double.parseDouble(tokens[9]);
                double pctLaureati = Double.parseDouble(tokens[10]);
                double disoccLaurea = Double.parseDouble(tokens[11]);
                double disoccDiploma = Double.parseDouble(tokens[12]);
                Regione regione = new Regione(nome, popolazione, salario, occupazione, istruzione, affitto, servizi, fattoreFamiliare, coord, pctLaureati, disoccLaurea, disoccDiploma);
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

    public static List<String> leggiCSV(String filePath) throws IOException {
        List<String> righe = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            boolean firstLine = true; // Flag per saltare l'intestazione
            while ((line = br.readLine()) != null) {
                if (firstLine) {
                    firstLine = false;
                    continue;
                }
                righe.add(line);
            }
        }
        return righe;
    }

    public static void stampaSaldoAnnuale(int annoCorrente, int agentiPerRegione, Map<String, Regione> regioniMap) throws IOException {
        List<String> righe = leggiCSV("data/log.csv");

        // Mappa per ogni regione: categoria -> [emigrazioni, immigrazioni]
        Map<String, Map<String, int[]>> saldoPerRegione = new HashMap<>();
        
        // Inizializza le mappe per tutte le regioni e categorie
        String[] categorie = {"Disoccupato_Diploma", "Disoccupato_Laurea", "Lavoratore_Diploma", "Lavoratore_Laurea"};
        
        for (String nomeRegione : regioniMap.keySet()) {
            Map<String, int[]> categorieMap = new HashMap<>();
            for (String categoria : categorie) {
                categorieMap.put(categoria, new int[2]); // [emigrazioni, immigrazioni]
            }
            saldoPerRegione.put(nomeRegione, categorieMap);
        }

        // Elabora i dati
        for (String riga : righe) {
            String[] campi = riga.split(",");
            if (campi.length < 15) continue;
            
            String categoria = campi[1].trim();
            String origine = campi[2].trim();
            String destinazione = campi[3].trim();
            boolean emigrato = Boolean.parseBoolean(campi[9].trim());
            int anno = Integer.parseInt(campi[14].trim());

            if (anno == annoCorrente && emigrato && !origine.equals(destinazione)) {
                // Incrementa emigrazioni per la regione di origine
                saldoPerRegione.get(origine).get(categoria)[0]++;
                // Incrementa immigrazioni per la regione di destinazione
                saldoPerRegione.get(destinazione).get(categoria)[1]++;
            }
        }

        // Scrivi il file CSV con le percentuali
        boolean fileExists = Files.exists(Paths.get("data/saldo.csv"));
        
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get("data/saldo.csv"), 
                StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            
            // Scrivi header solo se il file non esiste
            if (!fileExists) {
                writer.write("Anno,Regione,Categoria,Emigrazioni_assolute,Immigrazioni_assolute,Emigrazioni_permille,Immigrazioni_permille,Saldo_assoluto,Saldo_permille");
                writer.newLine();
            }
            
            for (String regione : regioniMap.keySet()) {
                for (String categoria : categorie) {
                    int[] saldo = saldoPerRegione.get(regione).get(categoria);
                    int emigrazioni = saldo[0];
                    int immigrazioni = saldo[1];
                    int saldoAssoluto = immigrazioni - emigrazioni;
                    
                    // Calcola tassi per mille rispetto alla popolazione iniziale della regione
                    double emigrazioniPermille = (double) emigrazioni / agentiPerRegione * 10;
                    double immigrazioniPermille = (double) immigrazioni / agentiPerRegione * 10;
                    double saldoPermille = immigrazioniPermille - emigrazioniPermille;
                    
                    writer.write(String.format(Locale.US, "%d,%s,%s,%d,%d,%.2f,%.2f,%d,%.2f",
                        annoCorrente, regione, categoria, emigrazioni, immigrazioni, 
                        emigrazioniPermille, immigrazioniPermille, saldoAssoluto, saldoPermille));
                    writer.newLine();
                }
            }
        }
        
        System.out.println("Saldo annuale scritto per anno: " + annoCorrente);
    }

    public static void stampaSaldoFinale(int agentiPerRegione, Map<String, Regione> regioniMap) throws IOException {
        List<String> righe = leggiCSV("data/log.csv");

        // Mappa regione → anno → categoria → [emigrazioni, immigrazioni]
        Map<String, Map<Integer, Map<String, int[]>>> saldoCompleto = new HashMap<>();
        
        String[] categorie = {"Disoccupato_Diploma", "Disoccupato_Laurea", "Lavoratore_Diploma", "Lavoratore_Laurea"};

        // Inizializza le strutture dati
        for (String nomeRegione : regioniMap.keySet()) {
            saldoCompleto.put(nomeRegione, new HashMap<>());
        }

        // Elabora tutti i dati
        for (String riga : righe) {
            String[] campi = riga.split(",");
            if (campi.length < 15) continue;
            
            String categoria = campi[1].trim();
            String origine = campi[2].trim();
            String destinazione = campi[3].trim();
            boolean emigrato = Boolean.parseBoolean(campi[10].trim());
            int anno = Integer.parseInt(campi[14].trim());

            if (emigrato && !origine.equals(destinazione)) {
                // Inizializza le mappe se necessario
                saldoCompleto.get(origine).putIfAbsent(anno, new HashMap<>());
                saldoCompleto.get(destinazione).putIfAbsent(anno, new HashMap<>());
                
                for (String cat : categorie) {
                    saldoCompleto.get(origine).get(anno).putIfAbsent(cat, new int[2]);
                    saldoCompleto.get(destinazione).get(anno).putIfAbsent(cat, new int[2]);
                }

                // Incrementa i contatori
                saldoCompleto.get(origine).get(anno).get(categoria)[0]++; // emigrazione
                saldoCompleto.get(destinazione).get(anno).get(categoria)[1]++; // immigrazione
            }
        }

        // Scrivi il file finale
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("data/saldo_finale.csv"))) {
            writer.write("=== SALDO MIGRATORIO FINALE PER REGIONE ===\n\n");
            
            // Totali generali per regione
            Map<String, int[]> totaliRegione = new HashMap<>();
            
            for (String regione : regioniMap.keySet()) {
                writer.write("REGIONE: " + regione + "\n");
                writer.write("Anno,Categoria,Emigrazioni,Immigrazioni,Saldo_Assoluto,Emigrazioni_‰,Immigrazioni_‰,Saldo_‰\n");
                
                Map<Integer, Map<String, int[]>> perAnno = saldoCompleto.getOrDefault(regione, new HashMap<>());
                List<Integer> anni = new ArrayList<>(perAnno.keySet());
                Collections.sort(anni);
                
                int[] totaleRegione = new int[2]; // [tot_emigrazioni, tot_immigrazioni]
                
                for (int anno : anni) {
                    Map<String, int[]> perCategoria = perAnno.get(anno);
                    
                    for (String categoria : categorie) {
                        int[] saldo = perCategoria.getOrDefault(categoria, new int[2]);
                        int emigrazioni = saldo[0];
                        int immigrazioni = saldo[1];
                        int saldoAssoluto = immigrazioni - emigrazioni;
                        
                        double emigrazioniPermille = (double) emigrazioni / agentiPerRegione * 10;
                        double immigrazioniPermille = (double) immigrazioni / agentiPerRegione * 10;
                        double saldoPermille = immigrazioniPermille - emigrazioniPermille;
                        
                        totaleRegione[0] += emigrazioni;
                        totaleRegione[1] += immigrazioni;
                        
                        if (emigrazioni > 0 || immigrazioni > 0) { // scrivi solo se c'è movimento
                            writer.write(String.format(Locale.US, "%d,%s,%d,%d,%d,%.2f,%.2f,%.2f\n",
                                anno, categoria, emigrazioni, immigrazioni, saldoAssoluto,
                                emigrazioniPermille, immigrazioniPermille, saldoPermille));
                        }
                    }
                }
                
                totaliRegione.put(regione, totaleRegione);
                
                // Scrivi totale per regione
                int totEmi = totaleRegione[0];
                int totImm = totaleRegione[1];
                int totSaldo = totImm - totEmi;
                double totEmiPermille = (double) totEmi / agentiPerRegione * 10;
                double totImmPermille = (double) totImm / agentiPerRegione * 10;
                double totSaldoPermille = totImmPermille - totEmiPermille;
                
                writer.write(String.format(Locale.US, "TOTALE,%s,%d,%d,%d,%.2f,%.2f,%.2f\n\n",
                    regione, totEmi, totImm, totSaldo, totEmiPermille, totImmPermille, totSaldoPermille));
            }
            
            // Riassunto finale
            writer.write("=== RIASSUNTO GENERALE ===\n");
            writer.write("Regione,Emigrazioni_Totali,Immigrazioni_Totali,Saldo_Assoluto,Emigrazioni_percento,Immigrazioni_percento,Saldo_percento\n");
            
            for (String regione : regioniMap.keySet()) {
                int[] totale = totaliRegione.getOrDefault(regione, new int[2]);
                int totEmi = totale[0];
                int totImm = totale[1];
                int saldo = totImm - totEmi;
                
                double totEmiPerc = (double) totEmi / agentiPerRegione * 100;
                double totImmPerc = (double) totImm / agentiPerRegione * 100;
                double saldoPerc = totImmPerc - totEmiPerc;
                
                writer.write(String.format(Locale.US, "%s,%d,%d,%d,%.2f,%.2f,%.2f\n",
                    regione, totEmi, totImm, saldo, totEmiPerc, totImmPerc, saldoPerc));
            }
        }
        
        System.out.println("Saldo finale scritto con successo!");
    }

    
    public static List<MigrationInfo> getMigrationsToVisualize(String logPath, int anno, double sogliaPercentuale) {
        Map<String, Integer> migrazioni = new HashMap<>();
        int totaleMigrazioniAnno = 0;

        try (BufferedReader reader = Files.newBufferedReader(Paths.get(logPath), StandardCharsets.UTF_8)) {
            String header = reader.readLine();
            String line;
            
            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split(",");
                if (tokens.length < 15) continue;
               
                int annoLog = Integer.parseInt(tokens[14].trim());
                if (annoLog != anno) continue;

                String categoria = tokens[1].trim();
                String origine = tokens[2].trim();
                String destinazione = tokens[3].trim();
                boolean emigrato = Boolean.parseBoolean(tokens[10].trim());

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
}