package com.example.cliente;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;

public class TriviaApplication extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(TriviaApplication.class.getResource("trivia-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 900, 600);

        stage.setTitle("Trivia Game - Cliente TCP");
        stage.setScene(scene);
        stage.setResizable(false);

        // Agregar icono si tienes uno
        try {
            stage.getIcons().add(new Image(getClass().getResourceAsStream("/images/trivia-icon.png")));
        } catch (Exception e) {
            System.out.println("No se pudo cargar el icono de la aplicación");
        }

        // Configurar el controlador
        TriviaController controller = fxmlLoader.getController();
        controller.setStage(stage);

        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}