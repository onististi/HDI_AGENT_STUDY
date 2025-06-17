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
    private int coordX;
    private int coordY;

    public Regione(String nome, double popolazione, double salario, double occupazione,
                   double istruzione, double affitto, double servizi,
                   Coordinate coord) {
        this.nome = nome;
        this.popolazione = popolazione;
        this.salario = salario;
        this.occupazione = occupazione;
        this.istruzione = istruzione;
        this.affitto = affitto;
        this.servizi = servizi;
        this.coordinate = coord;
    }

    public int getX() {return coordX;}

    public int getY() {	return coordY;}

    public void setCoord(int x, int y) {
        this.coordX = x;
        this.coordY = y;
    }
    public String getNome() {return this.nome;}
	}