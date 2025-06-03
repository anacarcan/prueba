package com.liceolapaz.acc.entidades;

import jakarta.persistence.*;

/**
 * Entidad que representa una pregunta de trivia en el sistema
 * Almacena el texto de la pregunta, las opciones de respuesta y la respuesta correcta
 * Mapea a la tabla 'pregunta' en la base de datos
 */
@Entity
@Table(name = "pregunta")
public class Pregunta {

    // ID único generado automáticamente por la base de datos
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    // Texto de la pregunta - obligatorio, máximo 500 caracteres
    @Column(name = "texto_pregunta", nullable = false, length = 500)
    private String textoPregunta;

    // Opción A de respuesta - obligatoria, máximo 200 caracteres
    @Column(name = "opcion_a", nullable = false, length = 200)
    private String opcionA;

    // Opción B de respuesta - obligatoria, máximo 200 caracteres
    @Column(name = "opcion_b", nullable = false, length = 200)
    private String opcionB;

    // Opción C de respuesta - obligatoria, máximo 200 caracteres
    @Column(name = "opcion_c", nullable = false, length = 200)
    private String opcionC;

    // Opción D de respuesta - obligatoria, máximo 200 caracteres
    @Column(name = "opcion_d", nullable = false, length = 200)
    private String opcionD;

    // Índice de la respuesta correcta (0=A, 1=B, 2=C, 3=D) - obligatorio
    @Column(name = "respuesta_correcta", nullable = false)
    private int respuestaCorrecta;

    // Categoría de la pregunta - obligatoria, máximo 50 caracteres
    @Column(name = "categoria", nullable = false, length = 50)
    private String categoria;

    // Nivel de dificultad de la pregunta - valor por defecto "medio"
    @Column(name = "dificultad", length = 20)
    private String dificultad = "medio";

    // Indica si la pregunta está activa en el sistema - valor por defecto true
    @Column(name = "activa")
    private boolean activa = true;

    /**
     * Constructor por defecto requerido por JPA
     */
    public Pregunta() {
    }

    /**
     * Constructor completo con dificultad por defecto
     * @param textoPregunta Texto de la pregunta
     * @param opcionA Opción A de respuesta
     * @param opcionB Opción B de respuesta
     * @param opcionC Opción C de respuesta
     * @param opcionD Opción D de respuesta
     * @param respuestaCorrecta Índice de la respuesta correcta (0-3)
     * @param categoria Categoría de la pregunta
     */
    public Pregunta(String textoPregunta, String opcionA, String opcionB, String opcionC, String opcionD,
                    int respuestaCorrecta, String categoria) {
        this.textoPregunta = textoPregunta;
        this.opcionA = opcionA;
        this.opcionB = opcionB;
        this.opcionC = opcionC;
        this.opcionD = opcionD;
        this.respuestaCorrecta = respuestaCorrecta;
        this.categoria = categoria;
        this.dificultad = "medio"; // Valor por defecto
        this.activa = true; // Activa por defecto
    }

    /**
     * Constructor completo con dificultad personalizada
     * @param textoPregunta Texto de la pregunta
     * @param opcionA Opción A de respuesta
     * @param opcionB Opción B de respuesta
     * @param opcionC Opción C de respuesta
     * @param opcionD Opción D de respuesta
     * @param respuestaCorrecta Índice de la respuesta correcta (0-3)
     * @param categoria Categoría de la pregunta
     * @param dificultad Nivel de dificultad (facil, medio, dificil)
     */
    public Pregunta(String textoPregunta, String opcionA, String opcionB, String opcionC, String opcionD,
                    int respuestaCorrecta, String categoria, String dificultad) {
        this.textoPregunta = textoPregunta;
        this.opcionA = opcionA;
        this.opcionB = opcionB;
        this.opcionC = opcionC;
        this.opcionD = opcionD;
        this.respuestaCorrecta = respuestaCorrecta;
        this.categoria = categoria;
        this.dificultad = dificultad;
        this.activa = true; // Activa por defecto
    }

    // Getters y Setters

    /**
     * Obtiene el ID único de la pregunta
     * @return ID de la pregunta
     */
    public int getId() {
        return id;
    }

    /**
     * Establece el ID de la pregunta (generalmente no se usa directamente)
     * @param id ID de la pregunta
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Obtiene el texto de la pregunta
     * @return Texto de la pregunta
     */
    public String getTextoPregunta() {
        return textoPregunta;
    }

    /**
     * Establece el texto de la pregunta
     * @param textoPregunta Texto de la pregunta
     */
    public void setTextoPregunta(String textoPregunta) {
        this.textoPregunta = textoPregunta;
    }

    /**
     * Obtiene la opción A de respuesta
     * @return Texto de la opción A
     */
    public String getOpcionA() {
        return opcionA;
    }

    /**
     * Establece la opción A de respuesta
     * @param opcionA Texto de la opción A
     */
    public void setOpcionA(String opcionA) {
        this.opcionA = opcionA;
    }

    /**
     * Obtiene la opción B de respuesta
     * @return Texto de la opción B
     */
    public String getOpcionB() {
        return opcionB;
    }

    /**
     * Establece la opción B de respuesta
     * @param opcionB Texto de la opción B
     */
    public void setOpcionB(String opcionB) {
        this.opcionB = opcionB;
    }

    /**
     * Obtiene la opción C de respuesta
     * @return Texto de la opción C
     */
    public String getOpcionC() {
        return opcionC;
    }

    /**
     * Establece la opción C de respuesta
     * @param opcionC Texto de la opción C
     */
    public void setOpcionC(String opcionC) {
        this.opcionC = opcionC;
    }

    /**
     * Obtiene la opción D de respuesta
     * @return Texto de la opción D
     */
    public String getOpcionD() {
        return opcionD;
    }

    /**
     * Establece la opción D de respuesta
     * @param opcionD Texto de la opción D
     */
    public void setOpcionD(String opcionD) {
        this.opcionD = opcionD;
    }

    /**
     * Obtiene el índice de la respuesta correcta
     * @return Índice de la respuesta correcta (0=A, 1=B, 2=C, 3=D)
     */
    public int getRespuestaCorrecta() {
        return respuestaCorrecta;
    }

    /**
     * Establece el índice de la respuesta correcta
     * @param respuestaCorrecta Índice de la respuesta correcta (0-3)
     */
    public void setRespuestaCorrecta(int respuestaCorrecta) {
        this.respuestaCorrecta = respuestaCorrecta;
    }

    /**
     * Obtiene la categoría de la pregunta
     * @return Categoría de la pregunta
     */
    public String getCategoria() {
        return categoria;
    }

    /**
     * Establece la categoría de la pregunta
     * @param categoria Categoría de la pregunta
     */
    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    /**
     * Obtiene el nivel de dificultad de la pregunta
     * @return Nivel de dificultad (facil, medio, dificil)
     */
    public String getDificultad() {
        return dificultad;
    }

    /**
     * Establece el nivel de dificultad de la pregunta
     * @param dificultad Nivel de dificultad
     */
    public void setDificultad(String dificultad) {
        this.dificultad = dificultad;
    }

    /**
     * Indica si la pregunta está activa en el sistema
     * @return true si está activa, false si está deshabilitada
     */
    public boolean isActiva() {
        return activa;
    }

    /**
     * Establece si la pregunta está activa en el sistema
     * @param activa true para activar, false para desactivar
     */
    public void setActiva(boolean activa) {
        this.activa = activa;
    }

    // Métodos de utilidad

    /**
     * Obtiene todas las opciones de respuesta como un array
     * @return Array con las cuatro opciones de respuesta [A, B, C, D]
     */
    public String[] getOpciones() {
        return new String[]{opcionA, opcionB, opcionC, opcionD};
    }

    /**
     * Obtiene el texto de la respuesta correcta
     * @return Texto de la opción correcta o mensaje de error si el índice es inválido
     */
    public String getRespuestaTexto() {
        String[] opciones = getOpciones();
        if (respuestaCorrecta >= 0 && respuestaCorrecta < opciones.length) {
            return opciones[respuestaCorrecta];
        }
        return "Respuesta no válida";
    }

    /**
     * Obtiene la letra correspondiente a la respuesta correcta
     * @return Letra de la respuesta correcta (A, B, C, o D)
     */
    public char getLetraRespuesta() {
        return (char) ('A' + respuestaCorrecta);
    }

    /**
     * Valida si una respuesta dada por letra es correcta
     * @param respuesta Letra de la respuesta (A, B, C, o D)
     * @return true si la respuesta es correcta, false en caso contrario
     */
    public boolean esRespuestaCorrecta(String respuesta) {
        if (respuesta == null || respuesta.length() != 1) return false;

        char letra = respuesta.toUpperCase().charAt(0);
        int indiceRespuesta = letra - 'A'; // Convertir A=0, B=1, C=2, D=3

        System.out.println("🔍 DEBUG: Validando respuesta '" + respuesta + "' para pregunta ID:" + id);
        System.out.println("   Letra: " + letra + ", Índice calculado: " + indiceRespuesta);
        System.out.println("   Respuesta correcta esperada: " + respuestaCorrecta + " (" + getLetraRespuesta() + ")");

        boolean esCorrecta = (indiceRespuesta == respuestaCorrecta);
        System.out.println("   Resultado: " + (esCorrecta ? "CORRECTA ✅" : "INCORRECTA ❌"));

        return esCorrecta;
    }

    /**
     * Valida si una respuesta dada por índice es correcta
     * @param indice Índice de la respuesta (0=A, 1=B, 2=C, 3=D)
     * @return true si la respuesta es correcta, false en caso contrario
     */
    public boolean esRespuestaCorrecta(int indice) {
        System.out.println("🔍 DEBUG: Validando respuesta por índice " + indice + " para pregunta ID:" + id);
        System.out.println("   Respuesta correcta esperada: " + respuestaCorrecta);

        boolean esCorrecta = (indice == respuestaCorrecta);
        System.out.println("   Resultado: " + (esCorrecta ? "CORRECTA ✅" : "INCORRECTA ❌"));

        return esCorrecta;
    }

    /**
     * Representación en cadena del objeto Pregunta
     * Incluye información clave para depuración
     * @return String con información de la pregunta
     */
    @Override
    public String toString() {
        return "Pregunta{" +
                "id=" + id +
                ", textoPregunta='" + textoPregunta + '\'' +
                ", categoria='" + categoria + '\'' +
                ", dificultad='" + dificultad + '\'' +
                ", respuestaCorrecta=" + getLetraRespuesta() +
                '}';
    }
}