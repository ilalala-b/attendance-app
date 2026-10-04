package com.sharmila.attendance;

import com.sharmila.attendance.database.Database;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class App extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        Database.initialize();

        FXMLLoader loader = new FXMLLoader(App.class.getResource("/com/sharmila/attendance/main-view.fxml"));
        Scene scene = new Scene(loader.load(), 1280, 760);
        scene.getStylesheets().add(App.class.getResource("/css/style.css").toExternalForm());

        stage.setTitle("Attendance Management System");
        stage.setMinWidth(1050);
        stage.setMinHeight(650);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
