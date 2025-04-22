module com.example.ghostymodel {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.jsoup;
    requires java.desktop;

    opens com.example.ghostymodel to javafx.fxml;
    exports com.example.ghostymodel;
}
