package com.liceolapaz.acc.entidades;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "partida")
public class Partida {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    @Column(name = "categoria", nullable = false, length = 50)
    private String categoria;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Column(name = "completada", nullable = false)
    private boolean completada;

    @Column(name = "tipo_partida", nullable = false, length = 20)
    private String tipoPartida;

    @Column(name = "total_preguntas")
    private int totalPreguntas = 10;

    @Column(name = "duracion_segundos")
    private Long duracionSegundos;

    public Partida() {
    }

    public Partida(String categoria, boolean completada, String tipoPartida) {
        this.categoria = categoria;
        this.completada = completada;
        this.tipoPartida = tipoPartida;
        this.totalPreguntas = 10;
    }

    public Partida(String categoria, String tipoPartida) {
        this.categoria = categoria;
        this.completada = false;
        this.tipoPartida = tipoPartida;
        this.totalPreguntas = 10;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(LocalDateTime fechaHora) {
        this.fechaHora = fechaHora;
    }

    public boolean isCompletada() {
        return completada;
    }

    public void setCompletada(boolean completada) {
        this.completada = completada;
    }

    public String getTipoPartida() {
        return tipoPartida;
    }

    public void setTipoPartida(String tipoPartida) {
        this.tipoPartida = tipoPartida;
    }

    public int getTotalPreguntas() {
        return totalPreguntas;
    }

    public void setTotalPreguntas(int totalPreguntas) {
        this.totalPreguntas = totalPreguntas;
    }

    public Long getDuracionSegundos() {
        return duracionSegundos;
    }

    public void setDuracionSegundos(Long duracionSegundos) {
        this.duracionSegundos = duracionSegundos;
    }

    public boolean esSolo() {
        return "SOLO".equalsIgnoreCase(tipoPartida);
    }

    public boolean esMultijugador() {
        return "MULTIJUGADOR".equalsIgnoreCase(tipoPartida);
    }

    public void marcarComoCompletada() {
        this.completada = true;
    }

    public String getDuracionFormateada() {
        if (duracionSegundos == null) return "N/A";

        long minutos = duracionSegundos / 60;
        long segundos = duracionSegundos % 60;

        if (minutos > 0) {
            return String.format("%d min %d seg", minutos, segundos);
        } else {
            return String.format("%d seg", segundos);
        }
    }

    @PrePersist
    protected void onCreate() {
        this.fechaHora = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "Partida{" +
                "id=" + id +
                ", categoria='" + categoria + '\'' +
                ", fechaHora=" + fechaHora +
                ", completada=" + completada +
                ", tipoPartida='" + tipoPartida + '\'' +
                ", totalPreguntas=" + totalPreguntas +
                '}';
    }
}