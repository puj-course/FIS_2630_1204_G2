package PlatoInsumo;

public class PlatoInsumo {
    private int id;
    private int platoId;
    private int insumoId;
    private int cantidadNecesaria;

    public PlatoInsumo() {}

    public PlatoInsumo(int platoId, int insumoId, int cantidadNecesaria) {
        this.platoId = platoId;
        this.insumoId = insumoId;
        this.cantidadNecesaria = cantidadNecesaria;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getPlatoId() { return platoId; }
    public void setPlatoId(int platoId) { this.platoId = platoId; }

    public int getInsumoId() { return insumoId; }
    public void setInsumoId(int insumoId) { this.insumoId = insumoId; }

    public int getCantidadNecesaria() { return cantidadNecesaria; }
    public void setCantidadNecesaria(int cantidadNecesaria) { this.cantidadNecesaria = cantidadNecesaria; }
}