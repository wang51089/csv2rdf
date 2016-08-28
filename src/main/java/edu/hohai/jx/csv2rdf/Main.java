package edu.hohai.jx.csv2rdf;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Created by wjh on 2016/8/21.
 */
public class Main extends Application{
    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getClassLoader().getResource("frame.fxml"));
        primaryStage.setTitle("CSV2RDF v.1.0-SNAPSHOT");
        primaryStage.setScene(new Scene(root, 640, 400));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

}
