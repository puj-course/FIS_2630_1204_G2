package com.gastroflow.model;

import java.math.BigDecimal;

public class CuentaPago {

    private final long pedidoId;
    private final String numeroPedido;
    private final Integer numeroMesa;
    private final BigDecimal subtotal;
    private final BigDecimal impuestos;
    private BigDecimal descuento;
    private BigDecimal propina;
    private final boolean pagada;

    public CuentaPago(long pedidoId,
                      String numeroPedido,
                      Integer numeroMesa,
                      BigDecimal subtotal,
                      BigDecimal impuestos,
                      BigDecimal descuento,
                      BigDecimal propina,
                      boolean pagada) {
        this.pedidoId = pedidoId;
        this.numeroPedido = numeroPedido;
        this.numeroMesa = numeroMesa;
        this.subtotal = valorSeguro(subtotal);
        this.impuestos = valorSeguro(impuestos);
        this.descuento = valorSeguro(descuento);
        this.propina = valorSeguro(propina);
        this.pagada = pagada;
    }

    private BigDecimal valorSeguro(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }

    public BigDecimal calcularTotal() {
        return subtotal
                .add(impuestos)
                .add(propina)
                .subtract(descuento);
    }

    public long getPedidoId() {
        return pedidoId;
    }

    public String getNumeroPedido() {
        return numeroPedido;
    }

    public Integer getNumeroMesa() {
        return numeroMesa;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getImpuestos() {
        return impuestos;
    }

    public BigDecimal getDescuento() {
        return descuento;
    }

    public void setDescuento(BigDecimal descuento) {
        this.descuento = valorSeguro(descuento);
    }

    public BigDecimal getPropina() {
        return propina;
    }

    public void setPropina(BigDecimal propina) {
        this.propina = valorSeguro(propina);
    }

    public boolean isPagada() {
        return pagada;
    }
}
