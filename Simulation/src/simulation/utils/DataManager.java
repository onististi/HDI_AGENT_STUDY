package simulation.utils;

import simulation.context.Regione;
import java.io.*;
import java.util.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;

public class DataManager {
	
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
}
