module com.example.cliente {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.cliente to javafx.fxml;
    exports com.example.cliente;
}