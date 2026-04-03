package geoinfo.client;

import geoinfo.client.gui.components.MButton;
import geoinfo.client.gui.utils.Configure;
import geoinfo.client.network.ClientService;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class Client extends Application {
    private ClientService clientService;

    private BorderPane mainLayout;
    private HBox header, toolBar;
    private BorderPane content;


    @Override
    public void start(Stage stage) {
        clientService = new ClientService("localhost", 12345);
        mainLayout = new BorderPane();
        header = new HBox();
        toolBar = new HBox();
        content = new BorderPane();

        // ================== HEADER =================
        MButton title = new MButton("Geographic Information System", "/images/logo/globe.png", 22, 22);
        title.getStyleClass().add("m-button-title");
        header.setBackground(Configure.PRIMARY_BACKGROUND);
        header.setPadding(new Insets(10,20,10,20));
        header.getChildren().add(title);
        // ================ END HEADER ===============

        // ================= TOOLBAR =================
        toolBar.setBackground(Configure.SECONDARY_BACKGROUND);
        toolBar.setPadding(new Insets(10, 20, 10, 20));
        toolBar.setSpacing(10);

        // Label + ComboBox để chọn loại tìm kiếm
        Label typeLabel = new Label("Loại tìm kiếm:");
        typeLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14;");

        ComboBox<String> queryType = new ComboBox<>();
        queryType.getItems().addAll("Country", "City");
        queryType.setValue("Country");
        queryType.setPrefWidth(120);
        queryType.setStyle("-fx-font-size: 13;");

        // Label + TextField để nhập tên quốc gia/thành phố
        Label searchLabel = new Label("Tìm kiếm:");
        searchLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14;");

        TextField searchInput = new TextField();
        searchInput.setPromptText("Nhập tên quốc gia hoặc thành phố...");
        searchInput.setPrefWidth(350);
        searchInput.setStyle("-fx-font-size: 13;");

        // Button tìm kiếm
        Button searchBtn = new Button("🔍 Tìm kiếm");
        searchBtn.setStyle("-fx-font-size: 13; -fx-padding: 8px 20px; -fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold;");
        searchBtn.setCursor(javafx.scene.Cursor.HAND);

        // Status label
        Label statusLabel = new Label("Sẵn sàng");
        statusLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13;");

        toolBar.getChildren().addAll(
            typeLabel, queryType,
            new Separator(Orientation.VERTICAL),
            searchLabel, searchInput, searchBtn,
            new Separator(Orientation.VERTICAL),
            statusLabel
        );
        // =============== END TOOLBAR ===============

        // ================= CONTENT =================
        TextArea resultArea = new TextArea();
        resultArea.setWrapText(true);
        resultArea.setEditable(false);
        resultArea.setStyle("-fx-control-inner-background: #f0f0f0; -fx-text-fill: #333; -fx-font-size: 12; -fx-font-family: 'Courier New';");
        resultArea.setText("Chào mừng bạn đến với Geographic Information System!\n\n" +
                          "Hướng dẫn sử dụng:\n" +
                          "1. Chọn loại tìm kiếm: Country (Quốc gia) hoặc City (Thành phố)\n" +
                          "2. Nhập tên quốc gia/thành phố\n" +
                          "3. Nhấn 'Tìm kiếm' hoặc Enter\n\n" +
                          "Đợi kết quả sẽ hiển thị ở đây...");

        content.setTop(toolBar);
        content.setCenter(resultArea);
        // =============== END CONTENT ===============

        // =============== MAIN LAYOUT ===============
        mainLayout.setTop(header);
        mainLayout.setCenter(content);

        Scene scene = new Scene(mainLayout, 1400, 750);
        try {
            String css = getClass().getResource("/utils/Configure.css").toExternalForm();
            scene.getStylesheets().add(css);
        } catch (NullPointerException e) {
            System.out.println("CSS file not found, using default styling");
        }

        stage.setScene(scene);
        stage.setTitle("Geographic Information System");
        stage.setMinWidth(1000);
        stage.setMinHeight(600);
        stage.show();
        // ============= END MAIN LAYOUT =============

        // =============== EVENT HANDLERS ===============
        // Search button handler
        searchBtn.setOnAction(e -> performSearch(searchInput, queryType, resultArea, statusLabel));

        // Enter key handler
        searchInput.setOnKeyPressed(e -> {
            if (e.getCode().toString().equals("ENTER")) {
                performSearch(searchInput, queryType, resultArea, statusLabel);
            }
        });

    }

    private void performSearch(TextField searchInput, ComboBox<String> queryType, TextArea resultArea, Label statusLabel) {
        String input = searchInput.getText().trim();
        String type = queryType.getValue();

        if (input.isEmpty()) {
            resultArea.setText("⚠️ Vui lòng nhập tên để tìm kiếm!");
            return;
        }

        statusLabel.setText("⏳ Đang tìm kiếm...");
        resultArea.setText("Đang xử lý yêu cầu...\n");

        new Thread(() -> {
            try {
                String query = type.equalsIgnoreCase("Country")
                    ? "country:" + input
                    : "city:" + input;

                ClientService service = new ClientService("localhost", 12345);
                service.sendQuery(query, response -> {
                    javafx.application.Platform.runLater(() -> {
                        resultArea.setText(response);
                        statusLabel.setText("✓ Hoàn thành");
                    });
                });
            } catch (Exception ex) {
                javafx.application.Platform.runLater(() -> {
                    resultArea.setText("❌ Lỗi: " + ex.getMessage());
                    statusLabel.setText("✗ Lỗi kết nối");
                });
            }
        }).start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}