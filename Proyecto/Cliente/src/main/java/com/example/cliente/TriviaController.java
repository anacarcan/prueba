package com.example.cliente;

import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.*;
import java.net.Socket;
import java.util.Optional;

public class TriviaController {

    private static final String SERVIDOR = "localhost";
    private static final int PUERTO = 65001;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    private String categoriaSeleccionada = "";
    private String modoSeleccionado = "";
    private int preguntaActual = 0;
    private int totalPreguntas = 10;
    private int puntosJ1 = 0;
    private int puntosJ2 = 0;
    private Timeline contadorTiempo;
    private boolean esPartidaSolo = true;
    private String oponente = "";

    @FXML private VBox contenedorPrincipal;

    // Pantalla 1: Nombre
    @FXML private VBox pantallaNombre;
    @FXML private TextField nombreField;
    @FXML private Label estadoLabel;
    @FXML private Button botonEnviarNombre;

    // Pantalla 2: Categoría y Modo
    @FXML private VBox pantallaCategorias;
    @FXML private Label categoriaLabel;
    @FXML private ComboBox<String> comboCategorias;
    @FXML private ComboBox<String> comboModo;
    @FXML private Button botonIniciarJuego;
    @FXML private Button botonEstadisticas;
    @FXML private Button botonPuntuacion;
    @FXML private Label puntuacionTotal;

    // Pantalla 3: Juego
    @FXML private VBox pantallaJuego;
    @FXML private Label preguntaLabel;
    @FXML private Label contadorLabel;
    @FXML private Label numeroPreguntaLabel;
    @FXML private Label puntosLabel;
    @FXML private Label oponenteLabel;
    @FXML private RadioButton opcionA, opcionB, opcionC, opcionD;
    @FXML private ToggleGroup opcionesGroup;
    @FXML private Button botonResponder;
    @FXML private Label feedbackLabel;
    @FXML private Button botonCancelarJuego;

    // Pantalla 4: Resultados/Saliendo
    @FXML private VBox pantallaResultados;
    @FXML private Label resultadoLabel;
    @FXML private Label mensajeResultado;
    @FXML private ProgressIndicator ruedaCarga;

    @FXML
    public void initialize() {
        conectarServidor();
        configurarComboBoxes();
        configurarToggleGroup();
        mostrarSolo(pantallaNombre);
    }

    // ===================== CONFIGURACIÓN INICIAL =====================

    private void configurarComboBoxes() {
        comboModo.getItems().addAll("solo", "esperar");
        comboModo.setValue("solo");
    }

    private void configurarToggleGroup() {
        opcionesGroup = new ToggleGroup();
        opcionA.setToggleGroup(opcionesGroup);
        opcionB.setToggleGroup(opcionesGroup);
        opcionC.setToggleGroup(opcionesGroup);
        opcionD.setToggleGroup(opcionesGroup);
    }

    // ===================== MANEJO DE BOTONES =====================

    @FXML
    public void onEnviarNombre() {
        String nombre = nombreField.getText().trim();
        if (!nombre.isEmpty()) {
            System.out.println("📤 Enviando nombre: " + nombre);
            enviar(nombre);
            nombreField.setDisable(true);
            botonEnviarNombre.setDisable(true);
        }
    }

    @FXML
    public void onIniciarJuego() {
        categoriaSeleccionada = comboCategorias.getValue();
        modoSeleccionado = comboModo.getValue();

        if (categoriaSeleccionada == null || modoSeleccionado == null) {
            mostrarAlerta("Por favor selecciona una categoría y modo de juego");
            return;
        }

        String mensaje = categoriaSeleccionada.toLowerCase() + ":" + modoSeleccionado.toLowerCase();
        System.out.println("📤 Enviando selección: " + mensaje);
        enviar(mensaje);

        botonIniciarJuego.setDisable(true);
        comboCategorias.setDisable(true);
        comboModo.setDisable(true);

        if ("esperar".equals(modoSeleccionado)) {
            categoriaLabel.setText("Buscando otro jugador para " + categoriaSeleccionada + "...");
        } else {
            categoriaLabel.setText("Iniciando partida individual de " + categoriaSeleccionada + "...");
        }
    }

    @FXML
    public void onMostrarEstadisticas() {
        enviar("estadisticas");
    }

    @FXML
    public void onMostrarPuntuacion() {
        enviar("puntuacion");
    }

    // FIXED: Método de responder corregido
    @FXML
    public void onResponder() {
        RadioButton seleccionada = (RadioButton) opcionesGroup.getSelectedToggle();
        if (seleccionada == null) {
            mostrarAlerta("Por favor selecciona una respuesta");
            return;
        }

        String respuesta = "";
        if (seleccionada == opcionA) respuesta = "A";
        else if (seleccionada == opcionB) respuesta = "B";
        else if (seleccionada == opcionC) respuesta = "C";
        else if (seleccionada == opcionD) respuesta = "D";

        System.out.println("📤 Enviando respuesta: " + respuesta);
        enviar(respuesta);

        // FIXED: Deshabilitar interfaz después de responder
        deshabilitarRespuestas();
        botonResponder.setDisable(true);

        // Detener el contador
        if (contadorTiempo != null) {
            contadorTiempo.stop();
        }
    }

    @FXML
    public void onCancelarJuego() {
        enviar("cancelar");
    }

    @FXML
    public void onSalir() {
        System.exit(0);
    }

    // ===================== COMUNICACIÓN CON SERVIDOR =====================

    private void conectarServidor() {
        new Thread(() -> {
            try {
                System.out.println("🔗 Conectando al servidor " + SERVIDOR + ":" + PUERTO + "...");
                socket = new Socket(SERVIDOR, PUERTO);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                System.out.println("✅ Conectado al servidor exitosamente");

                String mensaje;
                while ((mensaje = in.readLine()) != null) {
                    String finalMensaje = mensaje;
                    System.out.println("📥 Recibido del servidor: " + finalMensaje);
                    Platform.runLater(() -> procesarMensaje(finalMensaje));
                }

                System.out.println("🔌 Conexión con servidor cerrada");

            } catch (IOException e) {
                System.out.println("❌ Error de conexión: " + e.getMessage());
                Platform.runLater(() -> {
                    estadoLabel.setText("No se pudo conectar con el servidor.");
                    mostrarAlerta("Error de conexión: " + e.getMessage());
                });
            }
        }).start();
    }

    private void enviar(String mensaje) {
        if (out != null) {
            System.out.println("📤 Enviando: " + mensaje);
            out.println(mensaje);
        }
    }

    private void cerrarConexion() {
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {}
    }

    // ===================== PROCESAMIENTO DE MENSAJES =====================

    private void procesarMensaje(String mensaje) {
        System.out.println("🔄 Procesando: " + mensaje);

        if (mensaje.startsWith("SOLICITUD_NOMBRE")) {
            estadoLabel.setText("Introduce tu nombre:");
            mostrarSolo(pantallaNombre);

        } else if (mensaje.startsWith("CATEGORIAS_DISPONIBLES")) {
            String[] partes = mensaje.split(";");
            comboCategorias.getItems().clear();
            for (int i = 1; i < partes.length; i++) {
                comboCategorias.getItems().add(partes[i]);
            }
            if (!comboCategorias.getItems().isEmpty()) {
                comboCategorias.setValue(comboCategorias.getItems().get(0));
            }
            categoriaLabel.setText("Selecciona categoría y modo de juego:");
            mostrarSolo(pantallaCategorias);

        } else if (mensaje.startsWith("ESTADISTICAS")) {
            String stats = mensaje.substring(12).replace("|", "\n");
            mostrarAlerta("Estadísticas", stats);

        } else if (mensaje.startsWith("PUNTUACION_TOTAL")) {
            String puntos = mensaje.split(";")[1];
            puntuacionTotal.setText("Puntuación Total: " + puntos);
            activarNodos(puntuacionTotal);

        } else if (mensaje.startsWith("PARTIDA_ENCONTRADA")) {
            procesarPartidaEncontrada(mensaje);

        } else if (mensaje.startsWith("PARTIDA_SOLO_INICIADA") || mensaje.startsWith("PARTIDA_INICIADA")) {
            procesarInicioPartida(mensaje);

        } else if (mensaje.startsWith("PREGUNTA")) {
            procesarPregunta(mensaje);

        } else if (mensaje.startsWith("SOLICITAR_RESPUESTA")) {
            // FIXED: Habilitar respuestas cuando el servidor lo solicite
            habilitarRespuestas();

        } else if (mensaje.startsWith("RESPUESTA_CORRECTA")) {
            feedbackLabel.setText("¡Correcto!");
            feedbackLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
            activarNodos(feedbackLabel);

        } else if (mensaje.startsWith("RESPUESTA_INCORRECTA")) {
            feedbackLabel.setText("Incorrecto");
            feedbackLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
            activarNodos(feedbackLabel);

        } else if (mensaje.startsWith("TIMEOUT")) {
            feedbackLabel.setText("¡Tiempo agotado!");
            feedbackLabel.setStyle("-fx-text-fill: orange; -fx-font-weight: bold;");
            activarNodos(feedbackLabel);

        } else if (mensaje.startsWith("RESULTADO")) {
            procesarResultadoPregunta(mensaje);

        } else if (mensaje.startsWith("FIN_PARTIDA")) {
            procesarFinPartida(mensaje);

        } else if (mensaje.startsWith("PARTIDA_CANCELADA") || mensaje.startsWith("CONEXION_CANCELADA")) {
            mostrarFinAbrupto("La partida ha sido cancelada");

        } else if (mensaje.startsWith("SELECCION_INVALIDA") || mensaje.startsWith("CATEGORIA_INVALIDA") ||
                mensaje.startsWith("MODO_INVALIDO")) {
            mostrarAlerta("Selección inválida", mensaje);
            habilitarSeleccion();

        } else if (mensaje.startsWith("PARTIDA_EN_CURSO")) {
            mostrarAlerta("Hay una partida en curso. Espera o juega solo.");
            habilitarSeleccion();

        } else {
            System.out.println("⚠️ Mensaje no reconocido: " + mensaje);
        }
    }

    private void procesarPartidaEncontrada(String mensaje) {
        String[] partes = mensaje.split(";");
        for (String parte : partes) {
            if (parte.startsWith("TIPO:")) {
                String tipo = parte.substring(5);
                esPartidaSolo = "SOLO".equals(tipo);
            } else if (parte.startsWith("OPONENTE:")) {
                oponente = parte.substring(9);
            }
        }

        if (esPartidaSolo) {
            categoriaLabel.setText("¡Partida individual iniciando...");
        } else {
            categoriaLabel.setText("¡Oponente encontrado: " + oponente + "!");
        }
    }

    private void procesarInicioPartida(String mensaje) {
        if (!esPartidaSolo && !oponente.isEmpty()) {
            oponenteLabel.setText("Oponente: " + oponente);
            activarNodos(oponenteLabel);
        }

        preguntaActual = 0;
        puntosJ1 = 0;
        puntosJ2 = 0;

        mostrarSolo(pantallaJuego);
        activarNodos(botonCancelarJuego);
    }

    private void procesarPregunta(String mensaje) {
        // PREGUNTA;NUMERO:1;TOTAL:10;TEXTO:¿Pregunta?;A:opción1;B:opción2;C:opción3;D:opción4
        String[] partes = mensaje.split(";");

        for (String parte : partes) {
            if (parte.startsWith("NUMERO:")) {
                preguntaActual = Integer.parseInt(parte.substring(7));
            } else if (parte.startsWith("TOTAL:")) {
                totalPreguntas = Integer.parseInt(parte.substring(6));
            } else if (parte.startsWith("TEXTO:")) {
                preguntaLabel.setText(parte.substring(6));
            } else if (parte.startsWith("A:")) {
                opcionA.setText("A) " + parte.substring(2));
            } else if (parte.startsWith("B:")) {
                opcionB.setText("B) " + parte.substring(2));
            } else if (parte.startsWith("C:")) {
                opcionC.setText("C) " + parte.substring(2));
            } else if (parte.startsWith("D:")) {
                opcionD.setText("D) " + parte.substring(2));
            }
        }

        // FIXED: Limpiar estilos anteriores y estado
        limpiarEstilosRespuestas();

        numeroPreguntaLabel.setText("Pregunta " + preguntaActual + " de " + totalPreguntas);
        opcionesGroup.selectToggle(null);
        ocultarNodos(feedbackLabel);

        // FIXED: Activar elementos de la pregunta pero NO las respuestas aún
        activarNodos(preguntaLabel, numeroPreguntaLabel, opcionA, opcionB, opcionC, opcionD);

        // FIXED: Deshabilitar botón responder hasta que se seleccione una opción
        botonResponder.setDisable(true);

        iniciarContador();
    }

    private void procesarResultadoPregunta(String mensaje) {
        // RESULTADO;CORRECTA:B;PUNTOS_J1:3;PUNTOS_J2:2
        String[] partes = mensaje.split(";");

        for (String parte : partes) {
            if (parte.startsWith("PUNTOS_J1:")) {
                puntosJ1 = Integer.parseInt(parte.substring(10));
            } else if (parte.startsWith("PUNTOS_J2:")) {
                puntosJ2 = Integer.parseInt(parte.substring(10));
            } else if (parte.startsWith("CORRECTA:")) {
                String correcta = parte.substring(9);
                mostrarRespuestaCorrecta(correcta);
            }
        }

        // FIXED: Mostrar puntos solo para jugador 1 en modo solo
        if (esPartidaSolo) {
            puntosLabel.setText("Aciertos: " + puntosJ1 + "/" + preguntaActual);
        } else {
            puntosLabel.setText("Tú: " + puntosJ1 + " - " + oponente + ": " + puntosJ2);
        }
        activarNodos(puntosLabel);
    }

    private void procesarFinPartida(String mensaje) {
        String textoResultado = "";
        String puntosGanados = "0";

        if (mensaje.contains("GANADOR")) {
            textoResultado = "¡Has ganado la partida!";
        } else if (mensaje.contains("PERDEDOR")) {
            textoResultado = "Has perdido la partida";
        } else if (mensaje.contains("EMPATE")) {
            textoResultado = "¡Empate!";
        } else {
            textoResultado = "Partida completada";
        }

        // Extract points gained
        String[] partes = mensaje.split(";");
        for (String parte : partes) {
            if (parte.startsWith("PUNTOS_GANADOS:")) {
                puntosGanados = parte.substring(15);
                break;
            }
        }

        resultadoLabel.setText(textoResultado);

        if (esPartidaSolo) {
            mensajeResultado.setText("Respuestas correctas: " + puntosJ1 + "/" + totalPreguntas +
                    "\nPuntos ganados: " + puntosGanados);
        } else {
            mensajeResultado.setText("Tus respuestas correctas: " + puntosJ1 + "/" + totalPreguntas +
                    "\nPuntos ganados: " + puntosGanados);
        }

        activarNodos(resultadoLabel, mensajeResultado);
        mostrarCarga(true);
        mostrarSolo(pantallaResultados);

        PauseTransition pausa = new PauseTransition(Duration.seconds(5));
        pausa.setOnFinished(e -> volverAlInicio());
        pausa.play();
    }

    private void mostrarFinAbrupto(String motivo) {
        resultadoLabel.setText(motivo);
        mensajeResultado.setText("Regresando al menú principal...");

        activarNodos(resultadoLabel, mensajeResultado);
        mostrarCarga(true);
        mostrarSolo(pantallaResultados);

        PauseTransition pausa = new PauseTransition(Duration.seconds(3));
        pausa.setOnFinished(e -> volverAlInicio());
        pausa.play();
    }

    // ===================== UTILIDADES DE INTERFAZ =====================

    private void mostrarSolo(Node nodoVisible) {
        for (Node node : contenedorPrincipal.getChildren()) {
            node.setVisible(false);
            node.setManaged(false);
        }
        nodoVisible.setVisible(true);
        nodoVisible.setManaged(true);
    }

    private void ocultarNodos(Node... nodos) {
        for (Node nodo : nodos) {
            nodo.setVisible(false);
            nodo.setManaged(false);
        }
    }

    private void activarNodos(Node... nodos) {
        for (Node nodo : nodos) {
            nodo.setVisible(true);
            nodo.setManaged(true);
        }
    }

    // FIXED: Habilitar respuestas cuando el servidor lo solicite
    private void habilitarRespuestas() {
        // Limpiar estilos antes de habilitar
        limpiarEstilosRespuestas();

        // Habilitar radio buttons
        opcionA.setDisable(false);
        opcionB.setDisable(false);
        opcionC.setDisable(false);
        opcionD.setDisable(false);

        // FIXED: Habilitar botón responder
        botonResponder.setDisable(false);
        activarNodos(botonResponder);

        // FIXED: Agregar listener para habilitar botón cuando se seleccione una opción
        opcionesGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            botonResponder.setDisable(newToggle == null);
        });
    }

    private void deshabilitarRespuestas() {
        opcionA.setDisable(true);
        opcionB.setDisable(true);
        opcionC.setDisable(true);
        opcionD.setDisable(true);
        botonResponder.setDisable(true);
    }

    private void habilitarSeleccion() {
        botonIniciarJuego.setDisable(false);
        comboCategorias.setDisable(false);
        comboModo.setDisable(false);
    }

    private void mostrarRespuestaCorrecta(String correcta) {
        // PRIMERO: Limpiar todos los estilos
        limpiarEstilosRespuestas();

        // SEGUNDO: Resaltar solo la respuesta correcta
        if (correcta.equals("A")) {
            opcionA.setStyle("-fx-background-color: lightgreen; -fx-background-radius: 5;");
        } else if (correcta.equals("B")) {
            opcionB.setStyle("-fx-background-color: lightgreen; -fx-background-radius: 5;");
        } else if (correcta.equals("C")) {
            opcionC.setStyle("-fx-background-color: lightgreen; -fx-background-radius: 5;");
        } else if (correcta.equals("D")) {
            opcionD.setStyle("-fx-background-color: lightgreen; -fx-background-radius: 5;");
        }
    }

    private void limpiarEstilosRespuestas() {
        // Limpiar TODOS los estilos de las opciones
        opcionA.setStyle("");
        opcionB.setStyle("");
        opcionC.setStyle("");
        opcionD.setStyle("");

        // También restablecer el contador
        if (contadorLabel != null) {
            contadorLabel.setStyle("-fx-text-fill: #BF616A; -fx-font-weight: bold;");
        }
    }

    private void iniciarContador() {
        if (contadorTiempo != null) {
            contadorTiempo.stop();
        }

        contadorLabel.setText("20");
        activarNodos(contadorLabel);

        contadorTiempo = new Timeline(
                new KeyFrame(Duration.seconds(1), event -> {
                    int segundosRestantes = Integer.parseInt(contadorLabel.getText()) - 1;
                    contadorLabel.setText(String.valueOf(segundosRestantes));

                    if (segundosRestantes <= 5) {
                        contadorLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                    }
                })
        );
        contadorTiempo.setCycleCount(20);
        contadorTiempo.play();
    }

    private void mostrarCarga(boolean mostrar) {
        ruedaCarga.setVisible(mostrar);
        ruedaCarga.setManaged(mostrar);
    }

    private void mostrarAlerta(String mensaje) {
        mostrarAlerta("Información", mensaje);
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private void volverAlInicio() {
        mostrarCarga(false);
        resetearUI();
        cerrarConexion();
        conectarServidor();
    }

    private void resetearUI() {
        nombreField.setDisable(false);
        nombreField.clear();
        botonEnviarNombre.setDisable(false);

        habilitarSeleccion();
        comboCategorias.getItems().clear();
        comboModo.setValue("solo");

        preguntaActual = 0;
        puntosJ1 = 0;
        puntosJ2 = 0;
        esPartidaSolo = true;
        oponente = "";

        ocultarNodos(puntuacionTotal, oponenteLabel, feedbackLabel, puntosLabel, contadorLabel, botonCancelarJuego, botonResponder);

        // IMPORTANTE: Limpiar todos los estilos
        limpiarEstilosRespuestas();

        if (contadorTiempo != null) {
            contadorTiempo.stop();
        }
    }

    public void setStage(Stage stage) {
        stage.setOnCloseRequest(event -> {
            Alert alerta = new Alert(Alert.AlertType.CONFIRMATION);
            alerta.setTitle("Confirmación");
            alerta.setHeaderText("¿Estás seguro de que quieres salir?");
            alerta.setContentText("Si estás en partida se cancelará automáticamente.");
            Optional<ButtonType> resultado = alerta.showAndWait();
            if (resultado.isPresent() && resultado.get() == ButtonType.OK) {
                enviar("cancelar");
                cerrarConexion();
            } else {
                event.consume();
            }
        });
    }
}