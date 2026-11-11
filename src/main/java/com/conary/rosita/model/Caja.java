package com.conary.rosita.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entidad que representa una sesion/turno de caja y su correspondiente arqueo.
 */
public class Caja implements Serializable {

    private int idCaja;
    private LocalDateTime fechaApertura;
    private LocalDateTime fechaCierre;
    private BigDecimal montoInicial;
    private BigDecimal totalTirilla;
    private BigDecimal efectivoContado;
    private BigDecimal diferencia;

    public Caja() {
        this.fechaApertura = LocalDateTime.now();
        this.montoInicial = BigDecimal.ZERO;
        this.totalTirilla = BigDecimal.ZERO;
        this.efectivoContado = BigDecimal.ZERO;
        this.diferencia = BigDecimal.ZERO;
    }

    public Caja(int idCaja, LocalDateTime fechaApertura, LocalDateTime fechaCierre,
                BigDecimal montoInicial, BigDecimal totalTirilla, BigDecimal efectivoContado, BigDecimal diferencia) {
        this.idCaja = idCaja;
        this.fechaApertura = fechaApertura != null ? fechaApertura : LocalDateTime.now();
        this.fechaCierre = fechaCierre;
        this.montoInicial = montoInicial != null ? montoInicial : BigDecimal.ZERO;
        this.totalTirilla = totalTirilla != null ? totalTirilla : BigDecimal.ZERO;
        this.efectivoContado = efectivoContado != null ? efectivoContado : BigDecimal.ZERO;
        this.diferencia = diferencia != null ? diferencia : BigDecimal.ZERO;
    }

    public Caja(BigDecimal montoInicial) {
        this();
        this.montoInicial = montoInicial != null ? montoInicial : BigDecimal.ZERO;
    }

    public int getIdCaja() {
        return idCaja;
    }

    public void setIdCaja(int idCaja) {
        this.idCaja = idCaja;
    }

    public LocalDateTime getFechaApertura() {
        return fechaApertura;
    }

    public void setFechaApertura(LocalDateTime fechaApertura) {
        this.fechaApertura = fechaApertura;
    }

    public LocalDateTime getFechaCierre() {
        return fechaCierre;
    }

    public void setFechaCierre(LocalDateTime fechaCierre) {
        this.fechaCierre = fechaCierre;
    }

    public BigDecimal getMontoInicial() {
        return montoInicial;
    }

    public void setMontoInicial(BigDecimal montoInicial) {
        this.montoInicial = montoInicial != null ? montoInicial : BigDecimal.ZERO;
    }

    public BigDecimal getTotalTirilla() {
        return totalTirilla;
    }

    public void setTotalTirilla(BigDecimal totalTirilla) {
        this.totalTirilla = totalTirilla != null ? totalTirilla : BigDecimal.ZERO;
    }

    public BigDecimal getEfectivoContado() {
        return efectivoContado;
    }

    public void setEfectivoContado(BigDecimal efectivoContado) {
        this.efectivoContado = efectivoContado != null ? efectivoContado : BigDecimal.ZERO;
    }

    public BigDecimal getDiferencia() {
        return diferencia;
    }

    public void setDiferencia(BigDecimal diferencia) {
        this.diferencia = diferencia != null ? diferencia : BigDecimal.ZERO;
    }

    public boolean isAbierta() {
        return fechaCierre == null;
    }

    /**
     * Calcula la diferencia de arqueo: Efectivo Contado - Total esperado en Tirilla.
     * Valor positivo indica sobrante, valor negativo indica faltante.
     */
    public BigDecimal recalcularDiferencia() {
        this.diferencia = this.efectivoContado.subtract(this.totalTirilla);
        return this.diferencia;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Caja caja = (Caja) o;
        return idCaja == caja.idCaja;
    }

    @Override
    public int hashCode() {
        return Objects.hash(idCaja);
    }

    @Override
    public String toString() {
        return "Caja #" + idCaja + " (" + (isAbierta() ? "ABIERTA" : "CERRADA") + ")";
    }
}

