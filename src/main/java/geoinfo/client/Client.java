package geoinfo.client;

import geoinfo.client.network.ClientService;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class Client extends Application {

    private ClientService clientService;

    @Override
    public void start(Stage stage) {
        clientService = new ClientService("localhost", 12345);

        Text text = new Text("Chưa kết nối");
        Button btn = new Button("Connect");

        btn.setOnAction(e -> {
            text.setText("Đang kết nối...");
            new Thread(() -> {clientService.start();}).start();
        });

        VBox root = new VBox(10, text, btn);
        Scene scene = new Scene(root, 1200, 700);

        stage.setScene(scene);
        stage.setTitle("Client UI");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}