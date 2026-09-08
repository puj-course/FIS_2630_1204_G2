package models.ReglasDescuento;

public class ReglaDescuento {
    // Atributos
    private int id;// identificador único de la regla
    private String tipoCambio;      // "modificacion" o "adicion"
    private double valor;           // porcentaje
    private boolean esPorcentaje;   // true = %, false = valor fijo en pesos

    public ReglaDescuento() {}

    public ReglaDescuento(String tipoCambio, double valor, boolean esPorcentaje) {// Constructor
        this.tipoCambio = tipoCambio;
        this.valor = valor;
        this.esPorcentaje = esPorcentaje;
    }
    // Getters y Setters
    public int getId() { return id; }

    public void setId(int id) { this.id = id; }

    public String getTipoCambio() { return tipoCambio; }

    public void setTipoCambio(String tipoCambio) { this.tipoCambio = tipoCambio; }

    public double getValor() { return valor; }

    public void setValor(double valor) { this.valor = valor; }

    public boolean isEsPorcentaje() { return esPorcentaje; }
    
    public void setEsPorcentaje(boolean esPorcentaje) { this.esPorcentaje = esPorcentaje; }
}
