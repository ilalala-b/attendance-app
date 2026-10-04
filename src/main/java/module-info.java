module com.sharmila.attendance {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires org.xerial.sqlitejdbc;

    opens com.sharmila.attendance to javafx.fxml;
    opens com.sharmila.attendance.controller to javafx.fxml;

    exports com.sharmila.attendance;
    exports com.sharmila.attendance.model;
}
