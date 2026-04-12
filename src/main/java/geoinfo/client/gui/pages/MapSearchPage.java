package geoinfo.client.gui.pages;

import geoinfo.client.gui.components.SearchResultPane;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;

public class MapSearchPage extends BorderPane {
    private final SearchEnginePage searchEnginePage;
    private VBox bottomContent;
    private boolean isExpanded = true;
    private Label mapTooltip;
    private Group map;
    private Pane mapContainer;

    public MapSearchPage(SearchEnginePage searchEnginePage) {
        this.searchEnginePage = searchEnginePage;
        initComponents();
        buildLayout();
    }

    private void initComponents() {
        try {
            String path = System.getProperty("user.dir") + "/src/main/resources/data/world.svg";
            map = loadSvgMap(path);

            mapTooltip = new Label("");
            mapTooltip.getStyleClass().add("map-tooltip-style");
            mapTooltip.setMouseTransparent(true);
            mapTooltip.setVisible(false);

        } catch (Exception e) {
            System.err.println("Error load map: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void buildLayout() {
        mapContainer = new Pane();
        mapContainer.getChildren().add(map);
        mapContainer.getChildren().add(mapTooltip);
        mapContainer.setStyle("-fx-background-color: white;");

        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(mapContainer.widthProperty());
        clip.heightProperty().bind(mapContainer.heightProperty());
        mapContainer.setClip(clip);
        enableZoomAndPan(mapContainer, map);

        this.setCenter(mapContainer);
    }
    public Group loadSvgMap(String filePath) throws Exception {
        Group mapGroup = new Group();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new File(filePath));

        NodeList nodeList = doc.getElementsByTagName("path");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element element = (Element) nodeList.item(i);
            String pathData = element.getAttribute("d");
            String nameCountry = element.getAttribute("title");
            SVGPath svgPath = new SVGPath();
            svgPath.setContent(pathData);
            svgPath.setFill(Color.AZURE);
            svgPath.setStroke(Color.BLACK);
            svgPath.setStrokeWidth(0.2);

            svgPath.setOnMouseEntered(event -> {
                svgPath.setFill(Color.GRAY);
                mapTooltip.setText(nameCountry);
                mapTooltip.setVisible(true);
            });
            svgPath.setOnMouseMoved(event -> {
                Point2D localMouse = mapContainer.sceneToLocal(event.getSceneX(), event.getSceneY());
                mapTooltip.setLayoutX(localMouse.getX() + 15);
                mapTooltip.setLayoutY(localMouse.getY() + 15);
            });
            svgPath.setOnMouseExited(event -> {
                svgPath.setFill(Color.AZURE);
                mapTooltip.setVisible(false);
            });
            svgPath.setOnMouseClicked(event -> {
                event.consume();
                if (event.getButton() == javafx.scene.input.MouseButton.SECONDARY) {
                    showSearchResult(nameCountry);
                }
            });
            mapGroup.getChildren().add(svgPath);
        }
        return mapGroup;
    }

    public void enableZoomAndPan(Pane container, Group content) {
        final double[] mouseAnchor = new double[2];
        final double[] translateAnchor = new double[2];

        container.setOnScroll(event -> {
            double zoomFactor = event.getDeltaY() > 0 ? 1.1 : 0.9;
            double oldScale = content.getScaleX();
            double newScale = oldScale * zoomFactor;

            if (newScale >= 0.7 && newScale <= 80.0) {
                content.setScaleX(newScale);
                content.setScaleY(newScale);

                if (zoomFactor < 1.0) {
                    var bounds = content.getBoundsInParent();
                    double limitX = bounds.getWidth() / 2;
                    double limitY = bounds.getHeight() / 2;

                    if (Math.abs(content.getTranslateX()) > limitX) {
                        content.setTranslateX(Math.signum(content.getTranslateX()) * limitX);
                    }
                    if (Math.abs(content.getTranslateY()) > limitY) {
                        content.setTranslateY(Math.signum(content.getTranslateY()) * limitY);
                    }
                }
            }
            event.consume();
        });

        container.setOnMousePressed(event -> {
            if (event.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                mouseAnchor[0] = event.getSceneX();
                mouseAnchor[1] = event.getSceneY();
                translateAnchor[0] = content.getTranslateX();
                translateAnchor[1] = content.getTranslateY();
            }
        });

        container.setOnMouseDragged(event -> {
            if (event.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                double deltaX = event.getSceneX() - mouseAnchor[0];
                double deltaY = event.getSceneY() - mouseAnchor[1];

                double newTranslateX = translateAnchor[0] + deltaX;
                double newTranslateY = translateAnchor[1] + deltaY;

                var bounds = content.getBoundsInParent();
                double mapWidth = bounds.getWidth();
                double mapHeight = bounds.getHeight();

                double limitX = mapWidth / 2;
                double limitY = mapHeight / 2;

                if (Math.abs(newTranslateX) > limitX) {
                    newTranslateX = Math.signum(newTranslateX) * limitX;
                }
                if (Math.abs(newTranslateY) > limitY) {
                    newTranslateY = Math.signum(newTranslateY) * limitY;
                }

                content.setTranslateX(newTranslateX);
                content.setTranslateY(newTranslateY);
            }
        });
    }



    private void showSearchResult(String countryName) {
        this.setBottom(null);

        SearchResultPane resultPane = searchEnginePage.createResultPane();

        HBox headerBox = new HBox(10);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        headerBox.setPadding(new Insets(0, 16, 0, 16));
        headerBox.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #e2e8f0; -fx-border-width: 0 0 1 0;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button toggleButton = new Button("▼");
        toggleButton.getStyleClass().add("toggle-and-close-button");
        Button closeButton = new Button("✕");
        closeButton.getStyleClass().add("toggle-and-close-button");

        headerBox.getChildren().addAll(spacer, toggleButton, closeButton);

        bottomContent = new VBox();
        bottomContent.getChildren().add(resultPane);
        bottomContent.setPadding(new Insets(10));

        BorderPane bottomPanel = new BorderPane();
        bottomPanel.setTop(headerBox);
        bottomPanel.setCenter(bottomContent);
        bottomPanel.setStyle("-fx-background-color: white; -fx-border-color: #e5e7eb; -fx-border-width: 1 0 0 0;");
        bottomPanel.setPrefHeight(400);
        bottomPanel.setMaxHeight(400);

        toggleButton.setOnAction(e -> {
            if (isExpanded) {
                bottomPanel.setPrefHeight(50);
                bottomPanel.setMaxHeight(50);
                bottomContent.setVisible(false);
                bottomContent.setManaged(false);
                toggleButton.setText("▲");
                isExpanded = false;
            } else {
                bottomPanel.setPrefHeight(400);
                bottomPanel.setMaxHeight(400);
                bottomContent.setVisible(true);
                bottomContent.setManaged(true);
                toggleButton.setText("▼");
                isExpanded = true;
            }
        });

        closeButton.setOnAction(e -> {
            this.setBottom(null);
            isExpanded = true;
        });

        this.setBottom(bottomPanel);

        resultPane.search("country:" + countryName, "Searching for " + countryName + "...");

    }
}
