module com.mycompany.editortextojuanvidal {
    requires javafx.controls;
    requires javafx.fxml;

    opens com.mycompany.editortextojuanvidal to javafx.fxml;
    exports com.mycompany.editortextojuanvidal;
}
