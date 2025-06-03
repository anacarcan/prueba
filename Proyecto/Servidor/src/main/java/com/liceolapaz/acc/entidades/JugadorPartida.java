package com.liceolapaz.acc.entidades;

import jakarta.persistence.*;

@Entity
@Table(name = "jugador_partida")
public class JugadorPartida {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partida_id", nullable = false)
    private Partida partida;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "jugador_id", nullable = false)
    private Jugador jugador;

    @Column(name = "respuestas_correctas")
    private int respuestasCorrectas = 0;

    @Column(name = "respuestas_incorrectas")
    private int respuestasIncorrectas = 0;

    @Column(name = "puntos_obtenidos")
    private int puntosObtenidos = 0;

    @Column(name = "tiempo_total_segundos")
    private Long tiempoTotalSegundos;

    @Column(name = "posicion")
    private Integer posicion;

    @Column(name = "ganador")
    private boolean ganador = false;

    public JugadorPartida() {
    }

    public JugadorPartida(Partida partida, Jugador jugador) {
        this.partida = partida;
        this.jugador = jugador;
    }

    public JugadorPartida(Partida partida, Jugador jugador, int respuestasCorrectas, int puntosObtenidos) {
        this.partida = partida;
        this.jugador = jugador;
        this.respuestasCorrectas = respuestasCorrectas;
        this.puntosObtenidos = puntosObtenidos;
        this.respuestasIncorrectas = partida.getTotalPreguntas() - respuestasCorrectas;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Partida getPartida() {
        return partida;
    }

    public void setPartida(Partida partida) {
        this.partida = partida;
    }

    public Jugador getJugador() {
        return jugador;
    }

    public void setJugador(Jugador jugador) {
        this.jugador = jugador;
    }

    public int getRespuestasCorrectas() {
        return respuestasCorrectas;
    }

    public void setRespuestasCorrectas(int respuestasCorrectas) {
        this.respuestasCorrectas = respuestasCorrectas;
    }

    public int getRespuestasIncorrectas() {
        return respuestasIncorrectas;
    }

    public void setRespuestasIncorrectas(int respuestasIncorrectas) {
        this.respuestasIncorrectas = respuestasIncorrectas;
    }

    public int getPuntosObtenidos() {
        return puntosObtenidos;
    }

    public void setPuntosObtenidos(int puntosObtenidos) {
        this.puntosObtenidos = puntosObtenidos;
    }

    public Long getTiempoTotalSegundos() {
        return tiempoTotalSegundos;
    }

    public void setTiempoTotalSegundos(Long tiempoTotalSegundos) {
        this.tiempoTotalSegundos = tiempoTotalSegundos;
    }

    public Integer getPosicion() {
        return posicion;
    }

    public void setPosicion(Integer posicion) {
        this.posicion = posicion;
    }

    public boolean isGanador() {
        return ganador;
    }

    public void setGanador(boolean ganador) {
        this.ganador = ganador;
    }

    public int getTotalRespuestas() {
        return respuestasCorrectas + respuestasIncorrectas;
    }

    public double getPorcentajeAciertos() {
        int total = getTotalRespuestas();
        if (total == 0) return 0.0;
        return (double) respuestasCorrectas / total * 100.0;
    }

    public void incrementarRespuestaCorrecta() {
        this.respuestasCorrectas++;
    }

    public void incrementarRespuestaIncorrecta() {
        this.respuestasIncorrectas++;
    }

    public String getResultadoTexto() {
        if (partida != null && partida.esSolo()) {
            return String.format("%d/%d correctas (%.1f%%)",
                    respuestasCorrectas, partida.getTotalPreguntas(), getPorcentajeAciertos());
        } else {
            if (ganador) {
                return "¡Ganador! " + respuestasCorrectas + " correctas";
            } else {
                return respuestasCorrectas + " correctas";
            }
        }
    }

    public String getTiempoFormateado() {
        if (tiempoTotalSegundos == null) return "N/A";

        long minutos = tiempoTotalSegundos / 60;
        long segundos = tiempoTotalSegundos % 60;

        if (minutos > 0) {
            return String.format("%d:%02d", minutos, segundos);
        } else {
            return String.format("%d seg", segundos);
        }
    }

    @Override
    public String toString() {
        return "JugadorPartida{" +
                "id=" + id +
                ", jugador=" + (jugador != null ? jugador.getNombre() : "null") +
                ", respuestasCorrectas=" + respuestasCorrectas +
                ", puntosObtenidos=" + puntosObtenidos +
                ", ganador=" + ganador +
                '}';
    }
}