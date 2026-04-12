package geoinfo.client.gui.components;

import javafx.geometry.Insets;
import javafx.geometry.Pos;

import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.awt.Desktop;
import java.net.URI;
import java.util.List;

public class MTable extends VBox {

    public record RowData(String field, String value) {
    }

    public MTable() {
        setSpacing(0);
        setFillWidth(true);
        setStyle("-fx-background-color: white;");
    }

    public void setRows(List<RowData> rows) {
        getChildren().clear();

        if (rows == null || rows.isEmpty()) {
            return;
        }

        for (int index = 0; index < rows.size(); index++) {
            getChildren().add(createRow(rows.get(index), index == rows.size() - 1));
        }
    }

    private HBox createRow(RowData row, boolean isLastRow) {
        Label fieldLabel = new Label(safeText(row.field()));
        fieldLabel.setWrapText(true);
        fieldLabel.setMaxWidth(Double.MAX_VALUE);
        fieldLabel.getStyleClass().add("field-label-style");

        String text = safeText(row.value());
        TextField valueField = new TextField(text);
        valueField.setPadding(new Insets(0));
        valueField.setEditable(false);
        valueField.setFocusTraversable(false);

        if (isUrl(text)) {
            valueField.getStyleClass().add("value-field-is-url-style");
        } else {
            valueField.getStyleClass().add("value-field-normal-style");
        }
        addTextFieldSupport(valueField, text);

        HBox rowBox = new HBox(18, fieldLabel, valueField);
        rowBox.setAlignment(Pos.TOP_LEFT);
        rowBox.setPadding(new Insets(12, 0, 12, 0));
        rowBox.setFillHeight(true);
        rowBox.setStyle(
                "-fx-background-color: white;" +
                (isLastRow ? "" : "-fx-border-color: transparent transparent #e5e7eb transparent;")
        );

        fieldLabel.setPrefWidth(220);
        fieldLabel.setMinWidth(220);
        HBox.setHgrow(valueField, Priority.ALWAYS);

        return rowBox;
    }

    private void addTextFieldSupport(TextField textField, String text) {
        if (isUrl(text)) {
            textField.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.PRIMARY && event.isControlDown()) {
                    openUrl(text);
                    event.consume();
                }
            });
        }
    }

    private boolean isUrl(String text) {
        if (text == null || text.isBlank()) return false;
        String lowerText = text.toLowerCase().trim();
        return lowerText.startsWith("http://") || lowerText.startsWith("https://") || lowerText.startsWith("www.");
    }

    private void openUrl(String url) {
        try {
            if (!url.startsWith("http")) {
                url = "https://" + url;
            }
            Desktop.getDesktop().browse(new URI(url));
        } catch (Exception e) {
            System.err.println("Cannot open link: " + url);
        }
    }

    private void copyToClipboard(String text) {
        ClipboardContent content = new ClipboardContent();
        content.putString(text);
        Clipboard.getSystemClipboard().setContent(content);
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }
}
