module org.example {
    requires javafx.controls;
    requires javafx.fxml;
    requires junit;
    requires org.joml;

    opens org.example to javafx.fxml;
    exports org.example.ui;
}