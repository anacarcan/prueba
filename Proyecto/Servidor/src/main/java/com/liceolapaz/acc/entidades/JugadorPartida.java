package com.liceolapaz.acc.entidades;

import jakarta.persistence.*;

/**
 * Entidad que representa la participación de un jugador en una partida específica
 * Tabla de unión que almacena el rendimiento y estadísticas del jugador en cada partida
 * Mapea a la tabla 'jugador_partida' en la base de datos
 */
@Entity
@Table(name = "jugador_partida")
public class JugadorPartida {

    // ID único generado automáticamente por la base de datos
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    // Relación muchos-a-uno con Partida (carga perezosa para optimización)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partida_id", nullable = false)
    private Partida partida;

    // Relación muchos-a-uno con Jugador (carga perezosa para optimización)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "jugador_id", nullable = false)
    private Jugador jugador;

    // Número de respuestas correctas del jugador en esta partida
    @Column(name = "respuestas_correctas")
    private int respuestasCorrectas = 0;

    // Número de respuestas incorrectas del jugador en esta partida
    @Column(name = "respuestas_incorrectas")
    private int respuestasIncorrectas = 0;

    // Puntos obtenidos por el jugador en esta partida específica
    @Column(name = "puntos_obtenidos")
    private int puntosObtenidos = 0;

    // Tiempo total empleado por el jugador en completar la partida (en segundos)
    @Column(name = "tiempo_total_segundos")
    private Long tiempoTotalSegundos;

    // Posición final del jugador en la partida (1 = primer lugar)
    @Column(name = "posicion")
    private Integer posicion;

    // Indica si el jugador fue el ganador de esta partida
    @Column(name = "ganador")
    private boolean ganador = false;

    /**
     * Constructor por defecto requerido por JPA
     */
    public JugadorPartida() {
    }

    /**
     * Constructor básico con jugador y partida
     * @param partida La partida en la que participó
     * @param jugador El jugador participante
     */
    public JugadorPartida(Partida partida, Jugador jugador) {
        this.partida = partida;
        this.jugador = jugador;
    }

    /**
     * Constructor completo con estadísticas de rendimiento
     * Calcula automáticamente las respuestas incorrectas
     * @param partida La partida en la que participó
     * @param jugador El jugador participante
     * @param respuestasCorrectas Número de respuestas correctas
     * @param puntosObtenidos Puntos obtenidos en la partida
     */
    public JugadorPartida(Partida partida, Jugador jugador, int respuestasCorrectas, int puntosObtenidos) {
        this.partida = partida;
        this.jugador = jugador;
        this.respuestasCorrectas = respuestasCorrectas;
        this.puntosObtenidos = puntosObtenidos;
        this.respuestasIncorrectas = partida.getTotalPreguntas() - respuestasCorrectas;
    }

    // Getters y Setters

    /**
     * Obtiene el ID único del registro
     * @return ID del registro JugadorPartida
     */
    public int getId() {
        return id;
    }

    /**
     * Establece el ID del registro (generalmente no se usa directamente)
     * @param id ID del registro
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Obtiene la partida asociada
     * @return La partida en la que participó el jugador
     */
    public Partida getPartida() {
        return partida;
    }

    /**
     * Establece la partida asociada
     * @param partida La partida en la que participó
     */
    public void setPartida(Partida partida) {
        this.partida = partida;
    }

    /**
     * Obtiene el jugador asociado
     * @return El jugador que participó en la partida
     */
    public Jugador getJugador() {
        return jugador;
    }

    /**
     * Establece el jugador asociado
     * @param jugador El jugador participante
     */
    public void setJugador(Jugador jugador) {
        this.jugador = jugador;
    }

    /**
     * Obtiene el número de respuestas correctas
     * @return Número de respuestas correctas
     */
    public int getRespuestasCorrectas() {
        return respuestasCorrectas;
    }

    /**
     * Establece el número de respuestas correctas
     * @param respuestasCorrectas Número de respuestas correctas
     */
    public void setRespuestasCorrectas(int respuestasCorrectas) {
        this.respuestasCorrectas = respuestasCorrectas;
    }

    /**
     * Obtiene el número de respuestas incorrectas
     * @return Número de respuestas incorrectas
     */
    public int getRespuestasIncorrectas() {
        return respuestasIncorrectas;
    }

    /**
     * Establece el número de respuestas incorrectas
     * @param respuestasIncorrectas Número de respuestas incorrectas
     */
    public void setRespuestasIncorrectas(int respuestasIncorrectas) {
        this.respuestasIncorrectas = respuestasIncorrectas;
    }

    /**
     * Obtiene los puntos obtenidos en esta partida
     * @return Puntos obtenidos
     */
    public int getPuntosObtenidos() {
        return puntosObtenidos;
    }

    /**
     * Establece los puntos obtenidos en esta partida
     * @param puntosObtenidos Puntos obtenidos
     */
    public void setPuntosObtenidos(int puntosObtenidos) {
        this.puntosObtenidos = puntosObtenidos;
    }

    /**
     * Obtiene el tiempo total empleado en segundos
     * @return Tiempo total en segundos o null si no se registró
     */
    public Long getTiempoTotalSegundos() {
        return tiempoTotalSegundos;
    }

    /**
     * Establece el tiempo total empleado en segundos
     * @param tiempoTotalSegundos Tiempo total en segundos
     */
    public void setTiempoTotalSegundos(Long tiempoTotalSegundos) {
        this.tiempoTotalSegundos = tiempoTotalSegundos;
    }

    /**
     * Obtiene la posición final en la partida
     * @return Posición final (1 = primer lugar) o null si no aplica
     */
    public Integer getPosicion() {
        return posicion;
    }

    /**
     * Establece la posición final en la partida
     * @param posicion Posición final
     */
    public void setPosicion(Integer posicion) {
        this.posicion = posicion;
    }

    /**
     * Indica si el jugador fue el ganador de esta partida
     * @return true si fue ganador, false en caso contrario
     */
    public boolean isGanador() {
        return ganador;
    }

    /**
     * Establece si el jugador fue el ganador de esta partida
     * @param ganador true si fue ganador, false en caso contrario
     */
    public void setGanador(boolean ganador) {
        this.ganador = ganador;
    }

    // Métodos de utilidad

    /**
     * Calcula el total de respuestas contestadas
     * @return Suma de respuestas correctas e incorrectas
     */
    public int getTotalRespuestas() {
        return respuestasCorrectas + respuestasIncorrectas;
    }

    /**
     * Calcula el porcentaje de aciertos del jugador
     * @return Porcentaje de aciertos (0-100) o 0.0 si no contestó preguntas
     */
    public double getPorcentajeAciertos() {
        int total = getTotalRespuestas();
        if (total == 0) return 0.0;
        return (double) respuestasCorrectas / total * 100.0;
    }

    /**
     * Incrementa en 1 el contador de respuestas correctas
     */
    public void incrementarRespuestaCorrecta() {
        this.respuestasCorrectas++;
    }

    /**
     * Incrementa en 1 el contador de respuestas incorrectas
     */
    public void incrementarRespuestaIncorrecta() {
        this.respuestasIncorrectas++;
    }

    /**
     * Genera un texto descriptivo del resultado según el tipo de partida
     * @return Texto formateado con el resultado de la participación
     */
    public String getResultadoTexto() {
        if (partida != null && partida.esSolo()) {
            // Para partidas individuales: mostrar estadísticas detalladas
            return String.format("%d/%d correctas (%.1f%%)",
                    respuestasCorrectas, partida.getTotalPreguntas(), getPorcentajeAciertos());
        } else {
            // Para partidas multijugador: enfocarse en el resultado competitivo
            if (ganador) {
                return "¡Ganador! " + respuestasCorrectas + " correctas";
            } else {
                return respuestasCorrectas + " correctas";
            }
        }
    }

    /**
     * Formatea el tiempo total empleado en un formato legible
     * @return Tiempo formateado como "M:SS" o "S seg" o "N/A" si no hay tiempo
     */
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

    /**
     * Representación en cadena del objeto JugadorPartida
     * Incluye información clave para depuración
     * @return String con información del registro
     */
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