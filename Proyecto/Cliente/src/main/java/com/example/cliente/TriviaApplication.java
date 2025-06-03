package com.example.cliente;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import java.io.IOException;

/**
 * Clase principal de la aplicación cliente de Trivia
 * Extiende Application de JavaFX para crear la interfaz gráfica de usuario
 * Configura la ventana principal y carga la vista FXML correspondiente
 */
public class TriviaApplication extends Application {

    /**
     * Método principal de inicialización de JavaFX
     * Se ejecuta automáticamente cuando se lanza la aplicación
     * @param stage Ventana principal proporcionada por JavaFX
     * @throws IOException Si hay problemas cargando el archivo FXML
     */
    @Override
    public void start(Stage stage) throws IOException {
        // Cargar el archivo FXML que define la interfaz de usuario
        FXMLLoader fxmlLoader = new FXMLLoader(TriviaApplication.class.getResource("trivia-view.fxml"));

        // Crear la escena con las dimensiones especificadas
        Scene scene = new Scene(fxmlLoader.load(), 950, 650);

        // Configurar propiedades de la ventana principal
        stage.setTitle("Trivia Game - Cliente TCP");
        stage.setScene(scene);


        // Intentar agregar icono de la aplicación
        try {
            stage.getIcons().add(new Image(getClass().getResourceAsStream("/images/trivia-icon.png")));
        } catch (Exception e) {
            System.out.println("No se pudo cargar el icono de la aplicación");
        }

        // Obtener referencia al controlador y configurarlo con la ventana
        TriviaController controller = fxmlLoader.getController();
        controller.setStage(stage);

        // Mostrar la ventana principal
        stage.show();
    }

    /**
     * Método main que inicia la aplicación JavaFX
     * Punto de entrada del programa cliente
     * @param args Argumentos de línea de comandos (no utilizados)
     */
    public static void main(String[] args) {
        launch(); // Lanza la aplicación JavaFX
    }
}