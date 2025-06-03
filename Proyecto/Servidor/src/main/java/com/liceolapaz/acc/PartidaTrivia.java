package com.liceolapaz.acc;

import com.liceolapaz.acc.DAO.JugadorDAO;
import com.liceolapaz.acc.DAO.JugadorPartidaDAO;
import com.liceolapaz.acc.DAO.PreguntaDAO;
import com.liceolapaz.acc.DAO.PartidaDAO;
import com.liceolapaz.acc.entidades.Jugador;
import com.liceolapaz.acc.entidades.Partida;
import com.liceolapaz.acc.entidades.Pregunta;

import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * FIXED: Clase que maneja la lógica de una partida de trivia con sistema de puntos corregido
 */
public class PartidaTrivia implements Runnable {

    // Configuración del juego
    private static final int TOTAL_PREGUNTAS = 10;
    private static final int TIEMPO_RESPUESTA = 20; // segundos por pregunta

    // Conexiones de red
    private final Socket jugador1, jugador2;
    private final String nombreJ1, nombreJ2;
    private final String categoria;
    private PrintWriter salida1, salida2;

    // Estado del juego - FIXED: Variables corregidas
    private final List<Pregunta> preguntas;
    private int preguntaActual = 0; // Índice 0-based para la lista
    private int aciertosJ1 = 0, aciertosJ2 = 0; // Contar aciertos correctamente
    private boolean partidaFinalizada = false;
    private long inicioPartida;

    // Comunicación asíncrona
    private final BlockingQueue<String> colaJ1 = new LinkedBlockingQueue<>();
    private final BlockingQueue<String> colaJ2 = new LinkedBlockingQueue<>();

    public PartidaTrivia(Socket jugador1, Socket jugador2, String nombreJ1, String nombreJ2, String categoria) {
        this.jugador1 = jugador1;
        this.jugador2 = jugador2;
        this.nombreJ1 = nombreJ1;
        this.nombreJ2 = nombreJ2;
        this.categoria = categoria;
        this.preguntas = PreguntaDAO.obtenerPreguntasPorCategoria(categoria, TOTAL_PREGUNTAS);
        this.inicioPartida = System.currentTimeMillis();

        System.out.println("🎯 Nueva partida de trivia - Categoría: " + categoria +
                (jugador2 != null ? " (Multijugador: " + nombreJ1 + " vs " + nombreJ2 + ")" : " (Solo: " + nombreJ1 + ")"));

        if (preguntas.isEmpty()) {
            System.out.println("❌ ERROR: No se pudieron cargar preguntas para la categoría: " + categoria);
        } else {
            System.out.println("✅ Cargadas " + preguntas.size() + " preguntas para la partida");
            // DEBUG: Mostrar las respuestas correctas
            for (int i = 0; i < preguntas.size(); i++) {
                Pregunta p = preguntas.get(i);
                System.out.println("   Pregunta " + (i+1) + " (ID:" + p.getId() + "): Respuesta correcta = " +
                        p.getLetraRespuesta() + " (" + p.getRespuestaTexto() + ")");
            }
        }
    }

    @Override
    public void run() {
        try {
            if (preguntas.isEmpty()) {
                enviarError("No hay preguntas disponibles para esta categoría");
                return;
            }

            configurarConexiones();
            iniciarPartida();
            jugarTodasLasPreguntas();

            if (!partidaFinalizada) {
                finalizarPartida();
            }

        } catch (Exception e) {
            System.out.println("❌ Error en la partida de trivia: " + e.getMessage());
            e.printStackTrace();
            cancelarPartida("error del sistema");
        }
    }

    private void configurarConexiones() throws IOException {
        BufferedReader in1 = new BufferedReader(new InputStreamReader(jugador1.getInputStream()));
        salida1 = new PrintWriter(jugador1.getOutputStream(), true);
        new Thread(() -> escucharJugador(in1, colaJ1, nombreJ1)).start();

        if (jugador2 != null) {
            BufferedReader in2 = new BufferedReader(new InputStreamReader(jugador2.getInputStream()));
            salida2 = new PrintWriter(jugador2.getOutputStream(), true);
            new Thread(() -> escucharJugador(in2, colaJ2, nombreJ2)).start();
        }
    }

    private void iniciarPartida() {
        System.out.println("🎮 Iniciando partida de trivia...");

        if (jugador2 == null) {
            salida1.println("PARTIDA_SOLO_INICIADA;CATEGORIA:" + categoria);
            System.out.println("👤 Partida individual iniciada para " + nombreJ1);
        } else {
            salida1.println("PARTIDA_INICIADA;OPONENTE:" + nombreJ2 + ";CATEGORIA:" + categoria);
            salida2.println("PARTIDA_INICIADA;OPONENTE:" + nombreJ1 + ";CATEGORIA:" + categoria);
            System.out.println("🆚 Partida multijugador iniciada: " + nombreJ1 + " vs " + nombreJ2);
        }

        // Pausa para que los clientes procesen
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void jugarTodasLasPreguntas() throws InterruptedException {
        while (!partidaFinalizada && preguntaActual < preguntas.size()) {
            System.out.println("🔄 Iniciando pregunta " + (preguntaActual + 1) + "/" + preguntas.size());

            if (!jugarPregunta()) {
                System.out.println("❌ Partida cancelada durante pregunta " + (preguntaActual + 1));
                return;
            }

            preguntaActual++;

            // Pausa entre preguntas (excepto la última)
            if (preguntaActual < preguntas.size()) {
                System.out.println("⏱️ Pausa entre preguntas...");
                Thread.sleep(3000);
            }
        }

        System.out.println("✅ Todas las preguntas completadas - Aciertos J1: " + aciertosJ1 + ", J2: " + aciertosJ2);
    }

    // FIXED: Lógica de pregunta completamente corregida
    private boolean jugarPregunta() throws InterruptedException {
        Pregunta pregunta = preguntas.get(preguntaActual);
        System.out.println("❓ Pregunta " + (preguntaActual + 1) + " (ID:" + pregunta.getId() + "): " + pregunta.getTextoPregunta());
        System.out.println("✅ Respuesta correcta: " + pregunta.getLetraRespuesta() + ") " + pregunta.getRespuestaTexto());

        // Enviar pregunta a ambos jugadores
        enviarPregunta(pregunta);

        // Solicitar respuestas
        salida1.println("SOLICITAR_RESPUESTA");
        if (jugador2 != null) salida2.println("SOLICITAR_RESPUESTA");

        // Obtener respuestas con timeout
        String respuestaJ1 = colaJ1.poll(TIEMPO_RESPUESTA, TimeUnit.SECONDS);
        String respuestaJ2 = jugador2 != null ? colaJ2.poll(TIEMPO_RESPUESTA, TimeUnit.SECONDS) : null;

        System.out.println("📥 Respuestas recibidas - J1: '" + respuestaJ1 + "', J2: '" + respuestaJ2 + "'");

        // Verificar cancelaciones
        if (esCancelacion(respuestaJ1, nombreJ1) || esCancelacion(respuestaJ2, nombreJ2)) {
            return false;
        }

        // FIXED: Procesar respuestas y actualizar aciertos correctamente
        boolean correctaJ1 = procesarRespuesta(respuestaJ1, pregunta, nombreJ1, salida1);
        boolean correctaJ2 = jugador2 != null ? procesarRespuesta(respuestaJ2, pregunta, nombreJ2, salida2) : false;

        // FIXED: Actualizar aciertos (no puntos todavía)
        if (correctaJ1) {
            aciertosJ1++;
            System.out.println("✅ " + nombreJ1 + " acertó (total aciertos: " + aciertosJ1 + ")");
        }
        if (correctaJ2) {
            aciertosJ2++;
            System.out.println("✅ " + nombreJ2 + " acertó (total aciertos: " + aciertosJ2 + ")");
        }

        // Enviar resultado de la pregunta
        enviarResultadoPregunta(pregunta);

        return true;
    }

    private void enviarPregunta(Pregunta pregunta) {
        String mensajePregunta = String.format("PREGUNTA;NUMERO:%d;TOTAL:%d;TEXTO:%s;A:%s;B:%s;C:%s;D:%s",
                preguntaActual + 1, TOTAL_PREGUNTAS,
                pregunta.getTextoPregunta(),
                pregunta.getOpcionA(),
                pregunta.getOpcionB(),
                pregunta.getOpcionC(),
                pregunta.getOpcionD()
        );

        System.out.println("📤 Enviando pregunta: " + mensajePregunta);
        salida1.println(mensajePregunta);
        if (jugador2 != null) salida2.println(mensajePregunta);
    }

    // FIXED: Procesamiento de respuesta corregido
    private boolean procesarRespuesta(String respuesta, Pregunta pregunta, String nombreJugador, PrintWriter salida) {
        if (respuesta == null) {
            // Timeout
            salida.println("TIMEOUT");
            System.out.println("⏰ Timeout para " + nombreJugador);
            return false;
        }

        // FIXED: Validación de respuesta usando el método corregido de la entidad
        boolean correcta = pregunta.esRespuestaCorrecta(respuesta);

        // Enviar feedback al jugador
        salida.println(correcta ? "RESPUESTA_CORRECTA" : "RESPUESTA_INCORRECTA");

        System.out.println((correcta ? "✅" : "❌") + " " + nombreJugador +
                " respondió: '" + respuesta + "' (Correcta: " + pregunta.getLetraRespuesta() + ") - " +
                (correcta ? "ACIERTO" : "FALLO"));

        return correcta;
    }

    private void enviarResultadoPregunta(Pregunta pregunta) {
        String resultado;

        if (jugador2 == null) {
            // FIXED: Para modo SOLO, no enviar puntos del jugador 2
            resultado = String.format("RESULTADO;CORRECTA:%s;PUNTOS_J1:%d",
                    pregunta.getLetraRespuesta(), aciertosJ1);
        } else {
            // Para modo multijugador, enviar ambos
            resultado = String.format("RESULTADO;CORRECTA:%s;PUNTOS_J1:%d;PUNTOS_J2:%d",
                    pregunta.getLetraRespuesta(), aciertosJ1, aciertosJ2);
        }

        System.out.println("📊 Enviando resultado: " + resultado);
        salida1.println(resultado);
        if (jugador2 != null) salida2.println(resultado);
    }

    // FIXED: Finalización de partida con cálculo de puntos correcto
    private void finalizarPartida() {
        long duracionSegundos = (System.currentTimeMillis() - inicioPartida) / 1000;
        System.out.println("🏁 Finalizando partida - Duración: " + duracionSegundos + " segundos");
        System.out.println("📊 Aciertos finales - " + nombreJ1 + ": " + aciertosJ1 + "/" + TOTAL_PREGUNTAS +
                (jugador2 != null ? ", " + nombreJ2 + ": " + aciertosJ2 + "/" + TOTAL_PREGUNTAS : ""));

        // Crear registro de partida
        String tipoPartida = jugador2 != null ? "MULTIJUGADOR" : "SOLO";
        Partida partida = PartidaDAO.registrarPartida(categoria, true, tipoPartida);

        if (partida != null) {
            PartidaDAO.establecerDuracionPartida(partida.getId(), duracionSegundos);
        }

        if (jugador2 == null) {
            finalizarPartidaIndividual(partida);
        } else {
            finalizarPartidaMultijugador(partida);
        }

        cerrarConexiones();
        partidaFinalizada = true;
        System.out.println("✅ Partida completamente finalizada");
    }

    // FIXED: Finalización de partida individual con puntos correctos
    private void finalizarPartidaIndividual(Partida partida) {
        // FIXED: Calcular puntos basado en aciertos, no en el número de pregunta
        int puntosFinales = calcularPuntosFinales(aciertosJ1);

        System.out.println("📈 Finalizando partida individual - Aciertos: " + aciertosJ1 + "/" + TOTAL_PREGUNTAS +
                ", Puntos ganados: " + puntosFinales);

        // FIXED: Actualizar base de datos correctamente
        try {
            JugadorDAO.incrementarPartidasJugadas(nombreJ1);
            JugadorDAO.actualizarPuntuacionJugador(nombreJ1, puntosFinales);

            // Registrar en base de datos
            Jugador jugador = JugadorDAO.obtenerJugador(nombreJ1);
            if (jugador != null && partida != null) {
                JugadorPartidaDAO.registrarJugadorPartida(jugador, partida, aciertosJ1, puntosFinales);
            }

            System.out.println("✅ Datos guardados en BD para " + nombreJ1);
        } catch (Exception e) {
            System.out.println("❌ Error guardando en BD: " + e.getMessage());
            e.printStackTrace();
        }

        salida1.println(String.format("FIN_PARTIDA;PUNTOS:%d;TOTAL_PREGUNTAS:%d;PUNTOS_GANADOS:%d",
                aciertosJ1, TOTAL_PREGUNTAS, puntosFinales));
    }

    // FIXED: Finalización multijugador corregida
    private void finalizarPartidaMultijugador(Partida partida) {
        System.out.println("📈 Finalizando partida multijugador - " + nombreJ1 + ": " + aciertosJ1 +
                ", " + nombreJ2 + ": " + aciertosJ2);

        Jugador jugador1Obj = JugadorDAO.obtenerJugador(nombreJ1);
        Jugador jugador2Obj = JugadorDAO.obtenerJugador(nombreJ2);

        try {
            if (aciertosJ1 > aciertosJ2) {
                procesarVictoria(nombreJ1, aciertosJ1, nombreJ2, aciertosJ2, jugador1Obj, jugador2Obj, partida,
                        salida1, salida2);
            } else if (aciertosJ2 > aciertosJ1) {
                procesarVictoria(nombreJ2, aciertosJ2, nombreJ1, aciertosJ1, jugador2Obj, jugador1Obj, partida,
                        salida2, salida1);
            } else {
                procesarEmpate(jugador1Obj, jugador2Obj, partida);
            }
        } catch (Exception e) {
            System.out.println("❌ Error finalizando partida multijugador: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // FIXED: Procesamiento de victoria corregido
    private void procesarVictoria(String ganador, int aciertosGanador, String perdedor, int aciertosPerdedor,
                                  Jugador jugadorGanador, Jugador jugadorPerdedor, Partida partida,
                                  PrintWriter salidaGanador, PrintWriter salidaPerdedor) {

        // FIXED: Puntos basados en aciertos reales
        int puntosFinalesGanador = calcularPuntosFinales(aciertosGanador);
        int puntosFinalesPerdedor = (aciertosPerdedor >= 3) ? 1 : 0;

        System.out.println("🏆 Victoria: " + ganador + " (" + aciertosGanador + " aciertos) vs " +
                perdedor + " (" + aciertosPerdedor + " aciertos)");
        System.out.println("💰 Puntos: " + ganador + " = " + puntosFinalesGanador + ", " +
                perdedor + " = " + puntosFinalesPerdedor);

        // Update winner
        JugadorDAO.incrementarPartidasJugadas(ganador);
        JugadorDAO.incrementarPartidasGanadas(ganador);
        JugadorDAO.actualizarPuntuacionJugador(ganador, puntosFinalesGanador);

        // Update loser
        JugadorDAO.incrementarPartidasJugadas(perdedor);
        JugadorDAO.actualizarPuntuacionJugador(perdedor, puntosFinalesPerdedor);

        // Register in database
        if (jugadorGanador != null && partida != null) {
            JugadorPartidaDAO.registrarJugadorPartida(jugadorGanador, partida, aciertosGanador, puntosFinalesGanador);
            JugadorPartidaDAO.marcarComoGanador(jugadorGanador, partida);
        }

        if (jugadorPerdedor != null && partida != null) {
            JugadorPartidaDAO.registrarJugadorPartida(jugadorPerdedor, partida, aciertosPerdedor, puntosFinalesPerdedor);
        }

        salidaGanador.println(String.format("FIN_PARTIDA;RESULTADO:GANADOR;PUNTOS:%d;OPONENTE_PUNTOS:%d;PUNTOS_GANADOS:%d",
                aciertosGanador, aciertosPerdedor, puntosFinalesGanador));
        salidaPerdedor.println(String.format("FIN_PARTIDA;RESULTADO:PERDEDOR;PUNTOS:%d;OPONENTE_PUNTOS:%d;PUNTOS_GANADOS:%d",
                aciertosPerdedor, aciertosGanador, puntosFinalesPerdedor));
    }

    // FIXED: Procesamiento de empate corregido
    private void procesarEmpate(Jugador jugador1Obj, Jugador jugador2Obj, Partida partida) {
        int puntosEmpate = Math.max(calcularPuntosFinales(aciertosJ1) / 2, 1);

        System.out.println("🤝 Empate: " + nombreJ1 + " y " + nombreJ2 + " (" + aciertosJ1 + " aciertos c/u)");

        // Update both players
        JugadorDAO.incrementarPartidasJugadas(nombreJ1);
        JugadorDAO.incrementarPartidasJugadas(nombreJ2);
        JugadorDAO.actualizarPuntuacionJugador(nombreJ1, puntosEmpate);
        JugadorDAO.actualizarPuntuacionJugador(nombreJ2, puntosEmpate);

        if (jugador1Obj != null && partida != null) {
            JugadorPartidaDAO.registrarJugadorPartida(jugador1Obj, partida, aciertosJ1, puntosEmpate);
        }
        if (jugador2Obj != null && partida != null) {
            JugadorPartidaDAO.registrarJugadorPartida(jugador2Obj, partida, aciertosJ2, puntosEmpate);
        }

        salida1.println(String.format("FIN_PARTIDA;RESULTADO:EMPATE;PUNTOS:%d;PUNTOS_GANADOS:%d", aciertosJ1, puntosEmpate));
        salida2.println(String.format("FIN_PARTIDA;RESULTADO:EMPATE;PUNTOS:%d;PUNTOS_GANADOS:%d", aciertosJ2, puntosEmpate));
    }

    // FIXED: Cálculo de puntos basado en porcentaje de aciertos
    private int calcularPuntosFinales(int respuestasCorrectas) {
        double porcentaje = (double) respuestasCorrectas / TOTAL_PREGUNTAS;
        if (porcentaje >= 0.9) return 5;      // 90%+ = 5 puntos
        else if (porcentaje >= 0.7) return 3; // 70%+ = 3 puntos
        else if (porcentaje >= 0.5) return 1; // 50%+ = 1 punto
        else return 0;                        // <50% = 0 puntos
    }

    private boolean esCancelacion(String respuesta, String nombreJugador) {
        if (respuesta != null && respuesta.equalsIgnoreCase("cancelar")) {
            cancelarPartida(nombreJugador);
            return true;
        }
        return false;
    }

    private void escucharJugador(BufferedReader lector, BlockingQueue<String> cola, String nombre) {
        try {
            String linea;
            while ((linea = lector.readLine()) != null) {
                System.out.println("📥 " + nombre + " envió: '" + linea + "'");
                cola.put(linea);
            }
        } catch (Exception e) {
            System.out.println("🔌 " + nombre + " se ha desconectado");
            cola.offer("cancelar");
        }
    }

    private void cancelarPartida(String responsable) {
        if (partidaFinalizada) return;

        System.out.println("❌ Partida cancelada por: " + responsable);
        partidaFinalizada = true;

        if (salida1 != null) salida1.println("PARTIDA_CANCELADA");
        if (salida2 != null) salida2.println("PARTIDA_CANCELADA");

        cerrarConexiones();
    }

    private void enviarError(String mensaje) {
        System.out.println("❌ Enviando error: " + mensaje);
        if (salida1 != null) salida1.println("ERROR;" + mensaje);
        if (salida2 != null) salida2.println("ERROR;" + mensaje);
        cerrarConexiones();
    }

    private void cerrarConexiones() {
        try {
            if (jugador1 != null && !jugador1.isClosed()) jugador1.close();
            if (jugador2 != null && !jugador2.isClosed()) jugador2.close();
        } catch (IOException e) {
            System.out.println("⚠️ Error al cerrar conexiones: " + e.getMessage());
        }
    }
}