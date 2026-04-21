package geoinfo.client.gui.pages;

import geoinfo.client.gui.components.SearchResultPane;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
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
    private static final double RESULT_MIN_HEIGHT = 120;
    private static final double RESULT_COLLAPSED_HEIGHT = 50;
    private static final double RESULT_DEFAULT_HEIGHT = 400;

    private final SearchEnginePage searchEnginePage;
    private VBox bottomContent;
    private boolean isExpanded = true;
    private double lastExpandedHeight = RESULT_DEFAULT_HEIGHT;
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

        Region resizeHandle = new Region();
        resizeHandle.getStyleClass().add("map-result-resize-handle");
        resizeHandle.setPrefHeight(7);
        resizeHandle.setCursor(Cursor.N_RESIZE);

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

        VBox panelTop = new VBox(resizeHandle, headerBox);
        BorderPane bottomPanel = new BorderPane();
        bottomPanel.setTop(panelTop);
        bottomPanel.setCenter(bottomContent);
        bottomPanel.setStyle("-fx-background-color: white; -fx-border-color: #e5e7eb; -fx-border-width: 1 0 0 0;");
        bottomPanel.setPrefHeight(lastExpandedHeight);
        bottomPanel.setMaxHeight(lastExpandedHeight);

        final double[] dragAnchorY = new double[1];
        final double[] dragStartHeight = new double[1];
        resizeHandle.setOnMousePressed(event -> {
            dragAnchorY[0] = event.getSceneY();
            dragStartHeight[0] = bottomPanel.getPrefHeight();
        });
        resizeHandle.setOnMouseDragged(event -> {
            double dragDelta = dragAnchorY[0] - event.getSceneY();
            double candidateHeight = dragStartHeight[0] + dragDelta;
            double maxHeight = Math.max(RESULT_MIN_HEIGHT + 100, this.getHeight() - 80);
            double nextHeight = Math.max(RESULT_MIN_HEIGHT, Math.min(candidateHeight, maxHeight));

            bottomPanel.setPrefHeight(nextHeight);
            bottomPanel.setMaxHeight(nextHeight);
            lastExpandedHeight = nextHeight;
            bottomContent.setVisible(true);
            bottomContent.setManaged(true);
            toggleButton.setText("▼");
            isExpanded = true;
        });

        toggleButton.setOnAction(e -> {
            if (isExpanded) {
                lastExpandedHeight = bottomPanel.getPrefHeight();
                bottomPanel.setPrefHeight(RESULT_COLLAPSED_HEIGHT);
                bottomPanel.setMaxHeight(RESULT_COLLAPSED_HEIGHT);
                bottomContent.setVisible(false);
                bottomContent.setManaged(false);
                toggleButton.setText("▲");
                isExpanded = false;
            } else {
                bottomPanel.setPrefHeight(lastExpandedHeight);
                bottomPanel.setMaxHeight(lastExpandedHeight);
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
