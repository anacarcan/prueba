package com.liceolapaz.acc.entidades;

import jakarta.persistence.*;

@Entity
@Table(name = "pregunta")
public class Pregunta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    @Column(name = "texto_pregunta", nullable = false, length = 500)
    private String textoPregunta;

    @Column(name = "opcion_a", nullable = false, length = 200)
    private String opcionA;

    @Column(name = "opcion_b", nullable = false, length = 200)
    private String opcionB;

    @Column(name = "opcion_c", nullable = false, length = 200)
    private String opcionC;

    @Column(name = "opcion_d", nullable = false, length = 200)
    private String opcionD;

    @Column(name = "respuesta_correcta", nullable = false)
    private int respuestaCorrecta;

    @Column(name = "categoria", nullable = false, length = 50)
    private String categoria;

    @Column(name = "dificultad", length = 20)
    private String dificultad = "medio";

    @Column(name = "activa")
    private boolean activa = true;

    public Pregunta() {
    }

    public Pregunta(String textoPregunta, String opcionA, String opcionB, String opcionC, String opcionD,
                    int respuestaCorrecta, String categoria) {
        this.textoPregunta = textoPregunta;
        this.opcionA = opcionA;
        this.opcionB = opcionB;
        this.opcionC = opcionC;
        this.opcionD = opcionD;
        this.respuestaCorrecta = respuestaCorrecta;
        this.categoria = categoria;
        this.dificultad = "medio";
        this.activa = true;
    }

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
        this.activa = true;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTextoPregunta() {
        return textoPregunta;
    }

    public void setTextoPregunta(String textoPregunta) {
        this.textoPregunta = textoPregunta;
    }

    public String getOpcionA() {
        return opcionA;
    }

    public void setOpcionA(String opcionA) {
        this.opcionA = opcionA;
    }

    public String getOpcionB() {
        return opcionB;
    }

    public void setOpcionB(String opcionB) {
        this.opcionB = opcionB;
    }

    public String getOpcionC() {
        return opcionC;
    }

    public void setOpcionC(String opcionC) {
        this.opcionC = opcionC;
    }

    public String getOpcionD() {
        return opcionD;
    }

    public void setOpcionD(String opcionD) {
        this.opcionD = opcionD;
    }

    public int getRespuestaCorrecta() {
        return respuestaCorrecta;
    }

    public void setRespuestaCorrecta(int respuestaCorrecta) {
        this.respuestaCorrecta = respuestaCorrecta;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public String getDificultad() {
        return dificultad;
    }

    public void setDificultad(String dificultad) {
        this.dificultad = dificultad;
    }

    public boolean isActiva() {
        return activa;
    }

    public void setActiva(boolean activa) {
        this.activa = activa;
    }

    public String[] getOpciones() {
        return new String[]{opcionA, opcionB, opcionC, opcionD};
    }

    public String getRespuestaTexto() {
        String[] opciones = getOpciones();
        if (respuestaCorrecta >= 0 && respuestaCorrecta < opciones.length) {
            return opciones[respuestaCorrecta];
        }
        return "Respuesta no válida";
    }

    public char getLetraRespuesta() {
        return (char) ('A' + respuestaCorrecta);
    }

    // FIXED: Corrected answer validation methods
    public boolean esRespuestaCorrecta(String respuesta) {
        if (respuesta == null || respuesta.length() != 1) return false;

        char letra = respuesta.toUpperCase().charAt(0);
        int indiceRespuesta = letra - 'A'; // A=0, B=1, C=2, D=3

        System.out.println("🔍 DEBUG: Validando respuesta '" + respuesta + "' para pregunta ID:" + id);
        System.out.println("   Letra: " + letra + ", Índice calculado: " + indiceRespuesta);
        System.out.println("   Respuesta correcta esperada: " + respuestaCorrecta + " (" + getLetraRespuesta() + ")");

        boolean esCorrecta = (indiceRespuesta == respuestaCorrecta);
        System.out.println("   Resultado: " + (esCorrecta ? "CORRECTA ✅" : "INCORRECTA ❌"));

        return esCorrecta;
    }

    public boolean esRespuestaCorrecta(int indice) {
        System.out.println("🔍 DEBUG: Validando respuesta por índice " + indice + " para pregunta ID:" + id);
        System.out.println("   Respuesta correcta esperada: " + respuestaCorrecta);

        boolean esCorrecta = (indice == respuestaCorrecta);
        System.out.println("   Resultado: " + (esCorrecta ? "CORRECTA ✅" : "INCORRECTA ❌"));

        return esCorrecta;
    }

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