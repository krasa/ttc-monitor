package krasa.ttcmonitor.controller;

import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import krasa.ttcmonitor.TtcMonitorApplication;
import krasa.ttcmonitor.controller.model.EsoWatch;
import net.rgielen.fxweaver.core.FxWeaver;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.beans.factory.InjectionPoint;
import org.springframework.stereotype.Component;

@Component
@FxmlView("EditDialog.fxml")
public class EditDialogController {

    @FXML
    public TextArea link;
    @FXML
    public TextField exclude;
    @FXML
    public TextField cycle;

    private Stage stage;

    @FXML
    private Button okButton;
    @FXML
    private Button closeButton;
    @FXML
    private VBox dialog;

    private EsoWatch p;
    private MainController mainController;

    /**
     * This injection is powered by
     * {@link TtcMonitorApplication#controllerAndView(FxWeaver, InjectionPoint)}
     * <p/>
     * Your IDE might get confused, but it works :)
     */
    public EditDialogController(MainController mainController) {
        this.mainController = mainController;
    }

    @FXML
    public void initialize() {
        this.stage = new Stage();
        stage.setScene(new Scene(dialog));
        stage.addEventHandler(KeyEvent.KEY_RELEASED, (KeyEvent event) -> {
            if (KeyCode.ESCAPE == event.getCode()) {
                stage.close();
            }
        });
        okButton.setOnAction(
            actionEvent -> {
                mainController.update(p, new EsoWatch(link.getText(), exclude.getText(), cycle.getText()));
                stage.close();
            }
        );
        closeButton.setOnAction(
            actionEvent -> stage.close()
        );
    }

    public void show(EsoWatch p) {
        this.p = p;
        link.setText(this.p.getLink());
        exclude.setText(this.p.getExclude());
        cycle.setText(this.p.getCycle());
        stage.show();
    }

}
