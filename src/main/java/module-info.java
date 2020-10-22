module org.example {
  requires javafx.controls;
  requires javafx.fxml;
  requires org.joml;
  requires com.google.gson;
  requires org.jfxtras.styles.jmetro;

  opens org.example to javafx.fxml;
  exports org.example.view;
  exports org.example.model;
}