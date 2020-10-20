package org.example.view;

import javafx.scene.control.ListCell;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class ColorListCell extends ListCell<Color> {

    @Override
    protected void updateItem(Color color, boolean empty) {
        super.updateItem(color, empty);
        setGraphic(null);
        setText(null);
        if(color!=null){
            Rectangle rectangle = new Rectangle(20, 20);
            rectangle.setFill(color);
            setGraphic(rectangle);
        }
    }
}
