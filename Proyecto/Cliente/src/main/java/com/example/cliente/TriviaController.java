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

/**
 * Controlador principal de la interfaz de usuario del cliente de Trivia
 * Maneja la comunicación TCP con el servidor y controla todas las pantallas de la aplicación
 * Gestiona el flujo completo desde la conexión hasta la finalización de partidas
 */
public class TriviaController {

    // Configuración de conexión al servidor
    private static final String SERVIDOR = "localhost"; // Dirección del servidor de trivia
    private static final int PUERTO = 65001; // Puerto TCP del servidor

    // Componentes de comunicación TCP
    private Socket socket; // Socket para la conexión con el servidor
    private BufferedReader in; // Flujo de entrada para recibir mensajes del servidor
    private PrintWriter out; // Flujo de salida para enviar mensajes al servidor

    // Variables de estado del juego
    private String categoriaSeleccionada = ""; // Categoría de preguntas elegida
    private String modoSeleccionado = ""; // Modo de juego (solo/esperar)
    private int preguntaActual = 0; // Número de pregunta actual (1-based)
    private int totalPreguntas = 10; // Total de preguntas en la partida
    private int puntosJ1 = 0; // Aciertos del jugador 1 (usuario)
    private int puntosJ2 = 0; // Aciertos del jugador 2 (oponente)
    private Timeline contadorTiempo; // Temporizador para tiempo límite de respuesta
    private boolean esPartidaSolo = true; // Flag para determinar si es partida individual
    private String oponente = ""; // Nombre del oponente en partidas multijugador

    // Contenedor principal que alberga todas las pantallas
    @FXML private VBox contenedorPrincipal;

    // Pantalla 1: Solicitud de nombre del jugador
    @FXML private VBox pantallaNombre;
    @FXML private TextField nombreField; // Campo de texto para introducir el nombre
    @FXML private Label estadoLabel; // Etiqueta para mostrar el estado de la conexión
    @FXML private Button botonEnviarNombre; // Botón para confirmar el nombre

    // Pantalla 2: Selección de categoría y modo de juego
    @FXML private VBox pantallaCategorias;
    @FXML private Label categoriaLabel; // Etiqueta informativa sobre selección
    @FXML private ComboBox<String> comboCategorias; // Selector de categorías disponibles
    @FXML private ComboBox<String> comboModo; // Selector de modo (solo/esperar)
    @FXML private Button botonIniciarJuego; // Botón para iniciar la partida
    @FXML private Button botonEstadisticas; // Botón para consultar estadísticas
    @FXML private Button botonPuntuacion; // Botón para consultar puntuación total
    @FXML private Label puntuacionTotal; // Etiqueta para mostrar puntuación acumulada

    // Pantalla 3: Interfaz de juego durante la partida
    @FXML private VBox pantallaJuego;
    @FXML private Label preguntaLabel; // Etiqueta para mostrar el texto de la pregunta
    @FXML private Label contadorLabel; // Contador de tiempo restante
    @FXML private Label numeroPreguntaLabel; // Indicador de progreso (ej: "Pregunta 3 de 10")
    @FXML private Label puntosLabel; // Marcador de puntos/aciertos
    @FXML private Label oponenteLabel; // Nombre del oponente (solo multijugador)
    @FXML private RadioButton opcionA, opcionB, opcionC, opcionD; // Opciones de respuesta múltiple
    @FXML private ToggleGroup opcionesGroup; // Grupo que permite solo una selección
    @FXML private Button botonResponder; // Botón para enviar la respuesta seleccionada
    @FXML private Label feedbackLabel; // Feedback inmediato (correcto/incorrecto)
    @FXML private Button botonCancelarJuego; // Botón para abandonar la partida

    // Pantalla 4: Resultados finales y transición
    @FXML private VBox pantallaResultados;
    @FXML private Label resultadoLabel; // Resultado final (ganó/perdió/empate)
    @FXML private Label mensajeResultado; // Detalles del resultado y puntos ganados
    @FXML private ProgressIndicator ruedaCarga; // Indicador de carga durante transiciones

    /**
     * Método de inicialización llamado automáticamente por JavaFX
     * Configura la interfaz inicial y establece conexión con el servidor
     */
    @FXML
    public void initialize() {


        conectarServidor();
        configurarComboBoxes();
        configurarToggleGroup();
        mostrarSolo(pantallaNombre);
    }

    // ===================== CONFIGURACIÓN INICIAL =====================

    /**
     * Configura los valores iniciales de los ComboBox
     */
    private void configurarComboBoxes() {
        comboModo.getItems().addAll("solo", "esperar");
        comboModo.setValue("solo"); // Modo individual por defecto
    }

    /**
     * Configura el grupo de opciones de respuesta múltiple
     * Asegura que solo se pueda seleccionar una opción a la vez
     */
    private void configurarToggleGroup() {
        opcionesGroup = new ToggleGroup();
        opcionA.setToggleGroup(opcionesGroup);
        opcionB.setToggleGroup(opcionesGroup);
        opcionC.setToggleGroup(opcionesGroup);
        opcionD.setToggleGroup(opcionesGroup);
    }

    // ===================== MANEJO DE BOTONES =====================

    /**
     * Maneja el envío del nombre del jugador al servidor
     * Valida que el nombre no esté vacío antes de enviarlo
     */
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

    /**
     * Maneja el inicio de una nueva partida
     * Valida la selección de categoría y modo antes de proceder
     */
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

        // Deshabilitar controles durante la búsqueda de partida
        botonIniciarJuego.setDisable(true);
        comboCategorias.setDisable(true);
        comboModo.setDisable(true);

        // Mostrar mensaje apropiado según el modo seleccionado
        if ("esperar".equals(modoSeleccionado)) {
            categoriaLabel.setText("Buscando otro jugador para " + categoriaSeleccionada + "...");
        } else {
            categoriaLabel.setText("Iniciando partida individual de " + categoriaSeleccionada + "...");
        }
    }

    /**
     * Solicita las estadísticas del jugador al servidor
     */
    @FXML
    public void onMostrarEstadisticas() {
        enviar("estadisticas");
    }

    /**
     * Solicita la puntuación total del jugador al servidor
     */
    @FXML
    public void onMostrarPuntuacion() {
        enviar("puntuacion");
    }

    /**
     * CORREGIDO: Método de responder corregido
     * Envía la respuesta seleccionada al servidor y deshabilita la interfaz
     */
    @FXML
    public void onResponder() {
        RadioButton seleccionada = (RadioButton) opcionesGroup.getSelectedToggle();
        if (seleccionada == null) {
            mostrarAlerta("Por favor selecciona una respuesta");
            return;
        }

        // Convertir la selección a letra (A, B, C, D)
        String respuesta = "";
        if (seleccionada == opcionA) respuesta = "A";
        else if (seleccionada == opcionB) respuesta = "B";
        else if (seleccionada == opcionC) respuesta = "C";
        else if (seleccionada == opcionD) respuesta = "D";

        System.out.println("📤 Enviando respuesta: " + respuesta);
        enviar(respuesta);

        // CORREGIDO: Deshabilitar interfaz después de responder
        deshabilitarRespuestas();
        botonResponder.setDisable(true);

        // Detener el contador de tiempo
        if (contadorTiempo != null) {
            contadorTiempo.stop();
        }
    }

    /**
     * Cancela la partida actual enviando señal al servidor
     */
    @FXML
    public void onCancelarJuego() {
        enviar("cancelar");
    }

    /**
     * Cierra la aplicación
     */
    @FXML
    public void onSalir() {
        System.exit(0);
    }

    // ===================== COMUNICACIÓN CON SERVIDOR =====================

    /**
     * Establece conexión TCP con el servidor en un hilo separado
     * Maneja la recepción continua de mensajes del servidor
     */
    private void conectarServidor() {
        new Thread(() -> {
            try {
                System.out.println("🔗 Conectando al servidor " + SERVIDOR + ":" + PUERTO + "...");
                socket = new Socket(SERVIDOR, PUERTO);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                System.out.println("✅ Conectado al servidor exitosamente");

                // Bucle de recepción de mensajes
                String mensaje;
                while ((mensaje = in.readLine()) != null) {
                    String finalMensaje = mensaje;
                    System.out.println("📥 Recibido del servidor: " + finalMensaje);
                    // Procesar mensajes en el hilo de JavaFX UI
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

    /**
     * Envía un mensaje al servidor si la conexión está activa
     * @param mensaje Mensaje a enviar al servidor
     */
    private void enviar(String mensaje) {
        if (out != null) {
            System.out.println("📤 Enviando: " + mensaje);
            out.println(mensaje);
        }
    }

    /**
     * Cierra la conexión TCP de forma segura
     */
    private void cerrarConexion() {
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {}
    }

    // ===================== PROCESAMIENTO DE MENSAJES =====================

    /**
     * Procesa los mensajes recibidos del servidor y actualiza la interfaz
     * Centraliza toda la lógica de respuesta a diferentes tipos de mensaje
     * @param mensaje Mensaje recibido del servidor
     */
    private void procesarMensaje(String mensaje) {
        System.out.println("🔄 Procesando: " + mensaje);

        if (mensaje.startsWith("SOLICITUD_NOMBRE")) {
            estadoLabel.setText("Introduce tu nombre:");
            mostrarSolo(pantallaNombre);

        } else if (mensaje.startsWith("CATEGORIAS_DISPONIBLES")) {
            // Parsear categorías disponibles y poblar el ComboBox
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
            // Mostrar estadísticas del jugador en un diálogo
            String stats = mensaje.substring(12).replace("|", "\n");
            mostrarAlerta("Estadísticas", stats);

        } else if (mensaje.startsWith("PUNTUACION_TOTAL")) {
            // Actualizar y mostrar puntuación total
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
            // CORREGIDO: Habilitar respuestas cuando el servidor lo solicite
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

    /**
     * Procesa el mensaje de partida encontrada
     * Extrae información sobre tipo de partida y oponente
     */
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

        // Mostrar mensaje apropiado según el tipo de partida
        if (esPartidaSolo) {
            categoriaLabel.setText("¡Partida individual iniciando...");
        } else {
            categoriaLabel.setText("¡Oponente encontrado: " + oponente + "!");
        }
    }

    /**
     * Procesa el inicio oficial de la partida
     * Configura la interfaz de juego y resetea contadores
     */
    private void procesarInicioPartida(String mensaje) {
        // Mostrar información del oponente si es partida multijugador
        if (!esPartidaSolo && !oponente.isEmpty()) {
            oponenteLabel.setText("Oponente: " + oponente);
            activarNodos(oponenteLabel);
        }

        // Resetear contadores para nueva partida
        preguntaActual = 0;
        puntosJ1 = 0;
        puntosJ2 = 0;

        // Cambiar a pantalla de juego
        mostrarSolo(pantallaJuego);
        activarNodos(botonCancelarJuego);
    }

    /**
     * Procesa una nueva pregunta recibida del servidor
     * Parsea todos los componentes de la pregunta y configura la interfaz
     */
    private void procesarPregunta(String mensaje) {
        // Formato: PREGUNTA;NUMERO:1;TOTAL:10;TEXTO:¿Pregunta?;A:opción1;B:opción2;C:opción3;D:opción4
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

        // CORREGIDO: Limpiar estilos anteriores y estado
        limpiarEstilosRespuestas();

        numeroPreguntaLabel.setText("Pregunta " + preguntaActual + " de " + totalPreguntas);
        opcionesGroup.selectToggle(null); // Limpiar selección anterior
        ocultarNodos(feedbackLabel);

        // CORREGIDO: Activar elementos de la pregunta pero NO las respuestas aún
        activarNodos(preguntaLabel, numeroPreguntaLabel, opcionA, opcionB, opcionC, opcionD);

        // CORREGIDO: Deshabilitar botón responder hasta que se seleccione una opción
        botonResponder.setDisable(true);

        iniciarContador();
    }

    /**
     * Procesa el resultado de una pregunta
     * Actualiza marcadores y resalta la respuesta correcta
     */
    private void procesarResultadoPregunta(String mensaje) {
        // Formato: RESULTADO;CORRECTA:B;PUNTOS_J1:3;PUNTOS_J2:2
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

        // CORREGIDO: Mostrar puntos solo para jugador 1 en modo solo
        if (esPartidaSolo) {
            puntosLabel.setText("Aciertos: " + puntosJ1 + "/" + preguntaActual);
        } else {
            puntosLabel.setText("Tú: " + puntosJ1 + " - " + oponente + ": " + puntosJ2);
        }
        activarNodos(puntosLabel);
    }

    /**
     * Procesa el final de la partida
     * Determina resultado, extrae puntos ganados y muestra pantalla de resultados
     */
    private void procesarFinPartida(String mensaje) {
        String textoResultado = "";
        String puntosGanados = "0";

        // Determinar resultado de la partida
        if (mensaje.contains("GANADOR")) {
            textoResultado = "¡Has ganado la partida!";
        } else if (mensaje.contains("PERDEDOR")) {
            textoResultado = "Has perdido la partida";
        } else if (mensaje.contains("EMPATE")) {
            textoResultado = "¡Empate!";
        } else {
            textoResultado = "Partida completada";
        }

        // Extraer puntos ganados del mensaje
        String[] partes = mensaje.split(";");
        for (String parte : partes) {
            if (parte.startsWith("PUNTOS_GANADOS:")) {
                puntosGanados = parte.substring(15);
                break;
            }
        }

        resultadoLabel.setText(textoResultado);

        // Mostrar mensaje diferente según tipo de partida
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

        // Pausa automática antes de volver al menú principal
        PauseTransition pausa = new PauseTransition(Duration.seconds(5));
        pausa.setOnFinished(e -> volverAlInicio());
        pausa.play();
    }

    /**
     * Maneja finalizaciones abruptas de partida (cancelaciones, desconexiones)
     * @param motivo Razón de la finalización abrupta
     */
    private void mostrarFinAbrupto(String motivo) {
        resultadoLabel.setText(motivo);
        mensajeResultado.setText("Regresando al menú principal...");

        activarNodos(resultadoLabel, mensajeResultado);
        mostrarCarga(true);
        mostrarSolo(pantallaResultados);

        // Pausa más corta para finalizaciones abruptas
        PauseTransition pausa = new PauseTransition(Duration.seconds(3));
        pausa.setOnFinished(e -> volverAlInicio());
        pausa.play();
    }

    // ===================== UTILIDADES DE INTERFAZ =====================

    /**
     * Muestra solo un nodo específico ocultando todos los demás
     * @param nodoVisible El nodo que debe permanecer visible
     */
    private void mostrarSolo(Node nodoVisible) {
        for (Node node : contenedorPrincipal.getChildren()) {
            node.setVisible(false);
            node.setManaged(false);
        }
        nodoVisible.setVisible(true);
        nodoVisible.setManaged(true);
    }

    /**
     * Oculta los nodos especificados
     * @param nodos Nodos a ocultar
     */
    private void ocultarNodos(Node... nodos) {
        for (Node nodo : nodos) {
            nodo.setVisible(false);
            nodo.setManaged(false);
        }
    }

    /**
     * Activa y hace visibles los nodos especificados
     * @param nodos Nodos a activar
     */
    private void activarNodos(Node... nodos) {
        for (Node nodo : nodos) {
            nodo.setVisible(true);
            nodo.setManaged(true);
        }
    }

    /**
     * CORREGIDO: Habilitar respuestas cuando el servidor lo solicite
     * Configura la interfaz para permitir selección y respuesta
     */
    private void habilitarRespuestas() {
        // Limpiar estilos antes de habilitar
        limpiarEstilosRespuestas();

        // Habilitar radio buttons para selección
        opcionA.setDisable(false);
        opcionB.setDisable(false);
        opcionC.setDisable(false);
        opcionD.setDisable(false);

        // CORREGIDO: Habilitar botón responder
        botonResponder.setDisable(false);
        activarNodos(botonResponder);

        // CORREGIDO: Agregar listener para habilitar botón cuando se seleccione una opción
        opcionesGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            botonResponder.setDisable(newToggle == null);
        });
    }

    /**
     * Deshabilita todas las opciones de respuesta
     */
    private void deshabilitarRespuestas() {
        opcionA.setDisable(true);
        opcionB.setDisable(true);
        opcionC.setDisable(true);
        opcionD.setDisable(true);
        botonResponder.setDisable(true);
    }

    /**
     * Rehabilita los controles de selección de categoría y modo
     */
    private void habilitarSeleccion() {
        botonIniciarJuego.setDisable(false);
        comboCategorias.setDisable(false);
        comboModo.setDisable(false);
    }

    /**
     * Resalta visualmente la respuesta correcta
     * @param correcta Letra de la respuesta correcta (A, B, C, D)
     */
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

    /**
     * Limpia todos los estilos aplicados a las opciones de respuesta
     */
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

    /**
     * Inicia el contador de tiempo para responder la pregunta
     * Countdown de 20 segundos con alerta visual en los últimos 5 segundos
     */
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

                    // Cambiar color a rojo en los últimos 5 segundos
                    if (segundosRestantes <= 5) {
                        contadorLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                    }
                })
        );
        contadorTiempo.setCycleCount(20); // 20 segundos total
        contadorTiempo.play();
    }

    /**
     * Controla la visibilidad del indicador de carga
     * @param mostrar true para mostrar, false para ocultar
     */
    private void mostrarCarga(boolean mostrar) {
        ruedaCarga.setVisible(mostrar);
        ruedaCarga.setManaged(mostrar);
    }

    /**
     * Muestra un diálogo de alerta con mensaje informativo
     * @param mensaje Mensaje a mostrar en el diálogo
     */
    private void mostrarAlerta(String mensaje) {
        mostrarAlerta("Información", mensaje);
    }

    /**
     * Muestra un diálogo de alerta con título y mensaje personalizados
     * @param titulo Título del diálogo
     * @param mensaje Contenido del mensaje
     */
    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    /**
     * Regresa al estado inicial de la aplicación
     * Reinicia la conexión y resetea toda la interfaz
     */
    private void volverAlInicio() {
        mostrarCarga(false);
        resetearUI();
        cerrarConexion();
        conectarServidor();
    }

    /**
     * Resetea completamente la interfaz de usuario a su estado inicial
     * Limpia todos los campos, contadores y estilos aplicados
     */
    private void resetearUI() {
        // Resetear pantalla de nombre
        nombreField.setDisable(false);
        nombreField.clear();
        botonEnviarNombre.setDisable(false);

        // Resetear pantalla de selección
        habilitarSeleccion();
        comboCategorias.getItems().clear();
        comboModo.setValue("solo");

        // Resetear variables de estado
        preguntaActual = 0;
        puntosJ1 = 0;
        puntosJ2 = 0;
        esPartidaSolo = true;
        oponente = "";

        // Ocultar elementos de juego
        ocultarNodos(puntuacionTotal, oponenteLabel, feedbackLabel, puntosLabel, contadorLabel, botonCancelarJuego, botonResponder);

        // IMPORTANTE: Limpiar todos los estilos
        limpiarEstilosRespuestas();

        // Detener temporizadores activos
        if (contadorTiempo != null) {
            contadorTiempo.stop();
        }
    }

    /**
     * Configura el comportamiento al cerrar la ventana principal
     * Muestra confirmación y maneja cancelación de partida en curso
     * @param stage Ventana principal de la aplicación
     */
    public void setStage(Stage stage) {
        stage.setOnCloseRequest(event -> {
            Alert alerta = new Alert(Alert.AlertType.CONFIRMATION);
            alerta.setTitle("Confirmación");
            alerta.setHeaderText("¿Estás seguro de que quieres salir?");
            alerta.setContentText("Si estás en partida se cancelará automáticamente.");
            Optional<ButtonType> resultado = alerta.showAndWait();
            if (resultado.isPresent() && resultado.get() == ButtonType.OK) {
                enviar("cancelar"); // Notificar cancelación al servidor
                cerrarConexion();
            } else {
                event.consume(); // Cancelar el cierre de ventana
            }
        });
    }
}