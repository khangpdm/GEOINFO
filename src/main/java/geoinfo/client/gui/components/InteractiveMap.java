package geoinfo.client.gui.components;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;

public class InteractiveMap extends Application {
    private Label mapTooltip;
    @Override
    public void start(Stage stage) {
        try {
            // 1. Tạo container chính
            Pane root = new Pane();

            String path = System.getProperty("user.dir") + "/src/main/resources/data/world.svg";
            // 2. Load bản đồ từ file của bạn
            // Lưu ý: Sửa lại đường dẫn file cho đúng trên máy Quân
            Group map = loadSvgMap(path);
            // 3. Thêm bản đồ vào container
            root.getChildren().add(map);
            // Khởi tạo Tooltip UI
            mapTooltip = new Label("");
            mapTooltip.setStyle(
                    "-fx-background-color: rgba(30, 30, 30, 0.85);" + // Màu nền tối, hơi trong suốt
                            "-fx-text-fill: white;" +                          // Màu chữ
                            "-fx-padding: 5px 10px;" +                        // Khoảng cách chữ với viền
                            "-fx-background-radius: 5px;" +                  // Bo góc
                            "-fx-font-weight: bold;"
            );
            mapTooltip.setMouseTransparent(true); // Quan trọng: để chuột có thể xuyên qua Tooltip bấm vào map
            mapTooltip.setVisible(false);          // Ẩn lúc đầu

            // Thêm Tooltip vào root sau cùng để nó luôn nằm trên bản đồ
            root.getChildren().add(mapTooltip);
            // 4. Kích hoạt tính năng Zoom và Pan
            enableZoomAndPan(root, map);

            Scene scene = new Scene(root, 1100, 650);
            stage.setTitle("World Map Interactive - JavaFX");
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            System.err.println("Loi load map " + e.getMessage());
            e.printStackTrace();
        }
    }
    public Group loadSvgMap(String filePath) throws Exception{
        Group mapGroup = new Group();

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new File(filePath));

        NodeList nodeList = doc.getElementsByTagName("path");
        for (int i=0; i<nodeList.getLength(); i++){
            Element element = (Element) nodeList.item(i);
            String pathData = element.getAttribute("d");
//            String countryID = element.getAttribute("id");
            String nameCountry = element.getAttribute("title");
            SVGPath svgPath = new SVGPath();
            svgPath.setContent(pathData);
            svgPath.setFill(Color.AZURE);
            svgPath.setStroke(Color.BLACK);
            svgPath.setStrokeWidth(0.2);

            svgPath.setOnMouseEntered(e->{
                svgPath.setFill(Color.GRAY);
//                mapTooltip.setText(countryID);
                mapTooltip.setText(nameCountry);
                mapTooltip.setVisible(true);
            });
            svgPath.setOnMouseMoved(e -> {
                // Lấy tọa độ chuột so với cái Pane gốc (root)
                double mouseX = e.getSceneX();
                double mouseY = e.getSceneY();

                // Đặt vị trí Tooltip hơi lệch so với con trỏ chuột một chút để không che mất
                mapTooltip.setLayoutX(mouseX + 15);
                mapTooltip.setLayoutY(mouseY + 15);
            });

            // 3. Khi chuột đi RA KHỎI quốc gia
            svgPath.setOnMouseExited(e -> {
                svgPath.setFill(Color.AZURE); // Trả lại màu nền cũ
                mapTooltip.setVisible(false); // Ẩn Tooltip
            });

            mapGroup.getChildren().add(svgPath);
        }
        return mapGroup;
    }

    private void clickMouse(String nameCountry){

    }
    public void enableZoomAndPan(Pane container, Group content) {
        // Zoom bằng con lăn chuột
        final double[] mouseAnchor = new double[2];
        final double[] translateAnchor = new double[2];

        container.setOnScroll(event -> {
            double zoomFactor = (event.getDeltaY() > 0) ? 1.1 : 0.9;

            // Tính toán để zoom tại vị trí con trỏ chuột
            content.setScaleX(content.getScaleX() * zoomFactor);
            content.setScaleY(content.getScaleY() * zoomFactor);
            event.consume();
        });

        // 2. Xử lý KÉO (Pan) - Nhấn chuột xuống
        container.setOnMousePressed(event -> {
            // Lưu vị trí chuột lúc bắt đầu nhấn
            mouseAnchor[0] = event.getSceneX();
            mouseAnchor[1] = event.getSceneY();
            // Lưu vị trí hiện tại của bản đồ
            translateAnchor[0] = content.getTranslateX();
            translateAnchor[1] = content.getTranslateY();
        });

        // 3. Xử lý KÉO (Pan) - Di chuyển chuột
        container.setOnMouseDragged(event -> {
            // Tính toán khoảng cách đã di chuyển và cập nhật vị trí mới
            content.setTranslateX(translateAnchor[0] + event.getSceneX() - mouseAnchor[0]);
            content.setTranslateY(translateAnchor[1] + event.getSceneY() - mouseAnchor[1]);
        });
    }
    public static void main(String[] args) {
        launch(args);
    }
}
