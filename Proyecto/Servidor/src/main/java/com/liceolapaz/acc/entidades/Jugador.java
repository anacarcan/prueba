package com.liceolapaz.acc.entidades;

import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;


@Entity
@Table(name = "jugador")
public class Jugador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    @Column(name = "nombre", nullable = false, unique = true, length = 50)
    private String nombre;

    @Column(name = "puntuacion_total")
    @ColumnDefault("0")
    private int puntuacionTotal;

    @Column(name = "partidas_jugadas")
    @ColumnDefault("0")
    private int partidasJugadas;

    @Column(name = "partidas_ganadas")
    @ColumnDefault("0")
    private int partidasGanadas;

    public Jugador() {
    }

    public Jugador(String nombre) {
        this.nombre = nombre;
        this.puntuacionTotal = 0;
        this.partidasJugadas = 0;
        this.partidasGanadas = 0;
    }

    public Jugador(String nombre, int puntuacionTotal) {
        this.nombre = nombre;
        this.puntuacionTotal = puntuacionTotal;
        this.partidasJugadas = 0;
        this.partidasGanadas = 0;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public int getPuntuacionTotal() {
        return puntuacionTotal;
    }

    public void setPuntuacionTotal(int puntuacionTotal) {
        this.puntuacionTotal = puntuacionTotal;
    }

    public int getPartidasJugadas() {
        return partidasJugadas;
    }

    public void setPartidasJugadas(int partidasJugadas) {
        this.partidasJugadas = partidasJugadas;
    }

    public int getPartidasGanadas() {
        return partidasGanadas;
    }

    public void setPartidasGanadas(int partidasGanadas) {
        this.partidasGanadas = partidasGanadas;
    }

    public double getPorcentajeVictorias() {
        if (partidasJugadas == 0) return 0.0;
        return (double) partidasGanadas / partidasJugadas * 100;
    }

    public void incrementarPartidaJugada() {
        this.partidasJugadas++;
    }

    public void incrementarPartidaGanada() {
        this.partidasGanadas++;
    }

    @Override
    public String toString() {
        return "Jugador{" +
                "id=" + id +
                ", nombre='" + nombre + '\'' +
                ", puntuacionTotal=" + puntuacionTotal +
                ", partidasJugadas=" + partidasJugadas +
                ", partidasGanadas=" + partidasGanadas +
                '}';
    }
}