package simulation.context;

public class Regione {
    public String nome;
    public double popolazione;
    public double salario;
    public double occupazione;
    public double istruzione;
    public double affitto;
    public double servizi;
    public double latitudine;
    public double longitudine;
    private int coordX;
    private int coordY;

    public Regione(String nome, double popolazione, double salario, double occupazione,
                   double istruzione, double affitto, double servizi,
                   double latitudine, double longitudine) {
        this.nome = nome;
        this.popolazione = popolazione;
        this.salario = salario;
        this.occupazione = occupazione;
        this.istruzione = istruzione;
        this.affitto = affitto;
        this.servizi = servizi;
        this.latitudine = latitudine;
        this.longitudine = longitudine;
    }

    public int getX() {return coordX;}

    public int getY() {	return coordY;}

    public void setCoord(int x, int y) {
        this.coordX = x;
        this.coordY = y;
    }
}