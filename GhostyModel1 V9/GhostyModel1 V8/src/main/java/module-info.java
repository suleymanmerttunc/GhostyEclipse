module com.example.ghosty {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.jsoup;
	requires java.desktop;
    requires javafx.media;


    opens com.example.ghosty to javafx.fxml;
    exports com.example.ghosty;
}