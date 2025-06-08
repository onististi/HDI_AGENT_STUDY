package simulation.utils;

import simulation.context.Regione;
import java.util.*;

public class DecisionUtils {
    public static double distanza(Regione a, Regione b) {
        double dx = a.latitudine - b.latitudine;
        double dy = a.longitudine - b.longitudine;
        return Math.sqrt(dx * dx + dy * dy);
    }

    public static double gravity(Regione a, Regione b) {
        double dist = distanza(a, b) + 0.001;
        return (a.popolazione * b.popolazione) / (dist * dist);
    }

    public static double utility(Regione r, Map<String, Double> w) {
        return w.get("Salario") * r.salario +
               w.get("Occupazione") * r.occupazione +
               w.get("Istruzione") * r.istruzione -
               w.get("Affitto") * r.affitto;
    }

    public static double pushpull(Regione orig, Regione dest, Map<String, Double> w) {
        return w.get("Salario") * (dest.salario - orig.salario) +
               w.get("Occupazione") * (dest.occupazione - orig.occupazione) +
               w.get("Servizi") * (dest.servizi - orig.servizi);
    }

    public static double noise(Random rnd, int eta, boolean famiglia, boolean laurea) {
        double sigma = eta < 30 ? 0.04 : (eta < 40 ? 0.02 : 0.005);
        if (famiglia) sigma *= 0.5;
        if (laurea) sigma *= 0.8;
        return rnd.nextGaussian() * sigma;
    }

    public static double normalizza(double x, double min, double max) {
        return Math.max(0, Math.min(1, (x - min) / (max - min + 1e-6)));
    }
}