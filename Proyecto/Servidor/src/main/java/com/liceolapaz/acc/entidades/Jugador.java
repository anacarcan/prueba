package com.liceolapaz.acc.entidades;

import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;

/**
 * Entidad que representa un jugador en el sistema de trivia
 * Mapea a la tabla 'jugador' en la base de datos
 */
@Entity
@Table(name = "jugador")
public class Jugador {

    // ID único generado automáticamente por la base de datos
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    // Nombre del jugador - único y obligatorio, máximo 50 caracteres
    @Column(name = "nombre", nullable = false, unique = true, length = 50)
    private String nombre;

    // Puntuación total acumulada del jugador - valor por defecto 0
    @Column(name = "puntuacion_total")
    @ColumnDefault("0")
    private int puntuacionTotal;

    // Número total de partidas jugadas - valor por defecto 0
    @Column(name = "partidas_jugadas")
    @ColumnDefault("0")
    private int partidasJugadas;

    // Número total de partidas ganadas - valor por defecto 0
    @Column(name = "partidas_ganadas")
    @ColumnDefault("0")
    private int partidasGanadas;

    /**
     * Constructor por defecto requerido por JPA
     */
    public Jugador() {
    }

    /**
     * Constructor con nombre del jugador
     * Inicializa las estadísticas a cero
     * @param nombre Nombre del jugador
     */
    public Jugador(String nombre) {
        this.nombre = nombre;
        this.puntuacionTotal = 0;
        this.partidasJugadas = 0;
        this.partidasGanadas = 0;
    }

    /**
     * Constructor con nombre y puntuación inicial
     * @param nombre Nombre del jugador
     * @param puntuacionTotal Puntuación inicial del jugador
     */
    public Jugador(String nombre, int puntuacionTotal) {
        this.nombre = nombre;
        this.puntuacionTotal = puntuacionTotal;
        this.partidasJugadas = 0;
        this.partidasGanadas = 0;
    }

    // Getters y Setters

    /**
     * Obtiene el ID único del jugador
     * @return ID del jugador
     */
    public int getId() {
        return id;
    }

    /**
     * Establece el ID del jugador (generalmente no se usa directamente)
     * @param id ID del jugador
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Obtiene el nombre del jugador
     * @return Nombre del jugador
     */
    public String getNombre() {
        return nombre;
    }

    /**
     * Establece el nombre del jugador
     * @param nombre Nombre del jugador
     */
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    /**
     * Obtiene la puntuación total acumulada
     * @return Puntuación total del jugador
     */
    public int getPuntuacionTotal() {
        return puntuacionTotal;
    }

    /**
     * Establece la puntuación total del jugador
     * @param puntuacionTotal Nueva puntuación total
     */
    public void setPuntuacionTotal(int puntuacionTotal) {
        this.puntuacionTotal = puntuacionTotal;
    }

    /**
     * Obtiene el número de partidas jugadas
     * @return Número de partidas jugadas
     */
    public int getPartidasJugadas() {
        return partidasJugadas;
    }

    /**
     * Establece el número de partidas jugadas
     * @param partidasJugadas Número de partidas jugadas
     */
    public void setPartidasJugadas(int partidasJugadas) {
        this.partidasJugadas = partidasJugadas;
    }

    /**
     * Obtiene el número de partidas ganadas
     * @return Número de partidas ganadas
     */
    public int getPartidasGanadas() {
        return partidasGanadas;
    }

    /**
     * Establece el número de partidas ganadas
     * @param partidasGanadas Número de partidas ganadas
     */
    public void setPartidasGanadas(int partidasGanadas) {
        this.partidasGanadas = partidasGanadas;
    }

    /**
     * Calcula el porcentaje de victorias del jugador
     * @return Porcentaje de victorias (0-100) o 0.0 si no ha jugado partidas
     */
    public double getPorcentajeVictorias() {
        if (partidasJugadas == 0) return 0.0;
        return (double) partidasGanadas / partidasJugadas * 100;
    }

    /**
     * Incrementa en 1 el contador de partidas jugadas
     */
    public void incrementarPartidaJugada() {
        this.partidasJugadas++;
    }

    /**
     * Incrementa en 1 el contador de partidas ganadas
     */
    public void incrementarPartidaGanada() {
        this.partidasGanadas++;
    }

    /**
     * Representación en cadena del objeto Jugador
     * @return String con información completa del jugador
     */
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