package com.liceolapaz.acc.entidades;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entidad que representa una partida de trivia en el sistema
 * Almacena información sobre las sesiones de juego individuales o multijugador
 * Mapea a la tabla 'partida' en la base de datos
 */
@Entity
@Table(name = "partida")
public class Partida {

    // ID único generado automáticamente por la base de datos
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    // Categoría de las preguntas de la partida - obligatorio, máximo 50 caracteres
    @Column(name = "categoria", nullable = false, length = 50)
    private String categoria;

    // Fecha y hora de creación de la partida - se establece automáticamente
    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    // Indica si la partida ha sido completada - obligatorio
    @Column(name = "completada", nullable = false)
    private boolean completada;

    // Tipo de partida (SOLO o MULTIJUGADOR) - obligatorio, máximo 20 caracteres
    @Column(name = "tipo_partida", nullable = false, length = 20)
    private String tipoPartida;

    // Número total de preguntas en la partida - valor por defecto 10
    @Column(name = "total_preguntas")
    private int totalPreguntas = 10;

    // Duración total de la partida en segundos - opcional
    @Column(name = "duracion_segundos")
    private Long duracionSegundos;

    /**
     * Constructor por defecto requerido por JPA
     */
    public Partida() {
    }

    /**
     * Constructor completo con todos los parámetros principales
     * @param categoria Categoría de las preguntas
     * @param completada Estado inicial de la partida
     * @param tipoPartida Tipo de partida (SOLO o MULTIJUGADOR)
     */
    public Partida(String categoria, boolean completada, String tipoPartida) {
        this.categoria = categoria;
        this.completada = completada;
        this.tipoPartida = tipoPartida;
        this.totalPreguntas = 10; // Valor por defecto
    }

    /**
     * Constructor para crear partida nueva (no completada)
     * @param categoria Categoría de las preguntas
     * @param tipoPartida Tipo de partida (SOLO o MULTIJUGADOR)
     */
    public Partida(String categoria, String tipoPartida) {
        this.categoria = categoria;
        this.completada = false; // Nueva partida no completada
        this.tipoPartida = tipoPartida;
        this.totalPreguntas = 10; // Valor por defecto
    }

    // Getters y Setters

    /**
     * Obtiene el ID único de la partida
     * @return ID de la partida
     */
    public int getId() {
        return id;
    }

    /**
     * Establece el ID de la partida (generalmente no se usa directamente)
     * @param id ID de la partida
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Obtiene la categoría de las preguntas
     * @return Categoría de la partida
     */
    public String getCategoria() {
        return categoria;
    }

    /**
     * Establece la categoría de las preguntas
     * @param categoria Categoría de la partida
     */
    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    /**
     * Obtiene la fecha y hora de creación de la partida
     * @return Fecha y hora de creación
     */
    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    /**
     * Establece la fecha y hora de la partida
     * @param fechaHora Fecha y hora de la partida
     */
    public void setFechaHora(LocalDateTime fechaHora) {
        this.fechaHora = fechaHora;
    }

    /**
     * Indica si la partida ha sido completada
     * @return true si está completada, false en caso contrario
     */
    public boolean isCompletada() {
        return completada;
    }

    /**
     * Establece el estado de completado de la partida
     * @param completada true si está completada, false en caso contrario
     */
    public void setCompletada(boolean completada) {
        this.completada = completada;
    }

    /**
     * Obtiene el tipo de partida
     * @return Tipo de partida (SOLO o MULTIJUGADOR)
     */
    public String getTipoPartida() {
        return tipoPartida;
    }

    /**
     * Establece el tipo de partida
     * @param tipoPartida Tipo de partida (SOLO o MULTIJUGADOR)
     */
    public void setTipoPartida(String tipoPartida) {
        this.tipoPartida = tipoPartida;
    }

    /**
     * Obtiene el número total de preguntas en la partida
     * @return Número total de preguntas
     */
    public int getTotalPreguntas() {
        return totalPreguntas;
    }

    /**
     * Establece el número total de preguntas en la partida
     * @param totalPreguntas Número total de preguntas
     */
    public void setTotalPreguntas(int totalPreguntas) {
        this.totalPreguntas = totalPreguntas;
    }

    /**
     * Obtiene la duración de la partida en segundos
     * @return Duración en segundos o null si no se ha establecido
     */
    public Long getDuracionSegundos() {
        return duracionSegundos;
    }

    /**
     * Establece la duración de la partida en segundos
     * @param duracionSegundos Duración en segundos
     */
    public void setDuracionSegundos(Long duracionSegundos) {
        this.duracionSegundos = duracionSegundos;
    }

    // Métodos de utilidad

    /**
     * Verifica si la partida es de tipo individual
     * @return true si es partida SOLO, false en caso contrario
     */
    public boolean esSolo() {
        return "SOLO".equalsIgnoreCase(tipoPartida);
    }

    /**
     * Verifica si la partida es de tipo multijugador
     * @return true si es partida MULTIJUGADOR, false en caso contrario
     */
    public boolean esMultijugador() {
        return "MULTIJUGADOR".equalsIgnoreCase(tipoPartida);
    }

    /**
     * Marca la partida como completada
     * Método de conveniencia para cambiar el estado
     */
    public void marcarComoCompletada() {
        this.completada = true;
    }

    /**
     * Formatea la duración de la partida en un formato legible
     * @return Duración formateada como "X min Y seg" o "Y seg" o "N/A" si no hay duración
     */
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

    /**
     * Callback de JPA que se ejecuta antes de persistir la entidad
     * Establece automáticamente la fecha y hora actual
     */
    @PrePersist
    protected void onCreate() {
        this.fechaHora = LocalDateTime.now();
    }

    /**
     * Representación en cadena del objeto Partida
     * Incluye información clave para depuración
     * @return String con información de la partida
     */
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