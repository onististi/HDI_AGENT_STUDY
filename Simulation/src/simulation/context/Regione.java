package simulation.context;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Point;

public class Regione {
    public String nome;
    public double popolazione;
    public double salario;
    public double occupazione;
    public double istruzione;
    public double affitto;
    public double servizi;
    public Coordinate coordinate;
    
    public Point location;
    public double fattoreFamiliare; 
    public double pctLaureati;
    public double disoccLaurea;
    public double disoccDiploma;

    public Regione(String nome, double popolazione, double salario, double occupazione,
                   double istruzione, double affitto, double servizi, double fattoreFamiliare,
                   Coordinate coord, double pctLaureati, double disoccLaurea, double disoccDiploma) {
        this.nome = nome;
        this.popolazione = popolazione;
        this.salario = salario;
        this.occupazione = occupazione;
        this.istruzione = istruzione;
        this.affitto = affitto;
        this.servizi = servizi;
        this.coordinate = coord;
        this.fattoreFamiliare = fattoreFamiliare;
        this.pctLaureati = pctLaureati;
        this.disoccLaurea = disoccLaurea;
        this.disoccDiploma = disoccDiploma;
    }
    public String getNome() {return this.nome;}
	}