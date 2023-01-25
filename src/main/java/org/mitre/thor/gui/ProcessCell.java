package org.mitre.thor.gui;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;

/**
 * List Cell which is used to represent a process in the processes pane.
 */
class ProcessCell extends ListCell<Integer> {
    HBox hbox = new HBox();
    Label label = new Label("(empty)");
    Pane pane = new Pane();
    Button button = new Button("OPEN");
    Integer lastItem;
    public int processID;

    //Set up the cell with the button and label
    public ProcessCell(Controller controller) {
        super();
        hbox.setAlignment(Pos.CENTER);
        hbox.getChildren().addAll(label, pane, button);
        label.setAlignment(Pos.BOTTOM_CENTER);
        label.setTextFill(Paint.valueOf("#D4D4D3"));
        label.setFont(Font.font("Century Gothic", 18));
        HBox.setHgrow(pane, Priority.ALWAYS);
        button.setPrefWidth(160);
        button.setPrefHeight(25);
        button.getStyleClass().add("main-button");
        button.setOnAction((action) -> {
            controller.selectProcess(processID);
            controller.transitionToPane(controller.loadingPane);
        });
    }

    @Override
    protected void updateItem(Integer item, boolean empty) {
        super.updateItem(item, empty);
        setText(null);
        setStyle("-fx-background-color: #005B94");
        if (empty) {
            lastItem = null;
            setGraphic(null);
        } else {
            lastItem = item;
            processID = item;
            label.setText("Process " + item);
            setGraphic(hbox);
        }
    }
}
