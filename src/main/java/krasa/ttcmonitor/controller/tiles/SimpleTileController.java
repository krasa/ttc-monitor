package krasa.ttcmonitor.controller.tiles;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.stereotype.Component;

@FxmlView
@Component
public class SimpleTileController {

    @FXML
    public Label label;

    @FXML
    public void initialize() {
        label.setText(label.getText() + " initialized");
    }

}
