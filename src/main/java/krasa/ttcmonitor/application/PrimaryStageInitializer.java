package krasa.ttcmonitor.application;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import krasa.ttcmonitor.TtcMonitorApplication;
import krasa.ttcmonitor.commons.MyUtils;
import krasa.ttcmonitor.controller.MainController;
import net.rgielen.fxweaver.core.FxWeaver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class PrimaryStageInitializer implements ApplicationListener<StageReadyEvent> {

	private final FxWeaver fxWeaver;
	private static Stage stage;

	public static Stage getStage() {
		return stage;
	}

	@Autowired
	public PrimaryStageInitializer(FxWeaver fxWeaver) {
		this.fxWeaver = fxWeaver;
	}

	@Override
	public void onApplicationEvent(StageReadyEvent event) {
		stage = event.stage;
		Parent root = fxWeaver.loadView(MainController.class);
		stage.getIcons().add(MyUtils.getImage("Client.png"));
		Scene scene = new Scene(root, 1800, 800);

		String styleSheetURL = TtcMonitorApplication.class.getResource("dark.css").toString();

		// enable style
		scene.getStylesheets().add(styleSheetURL);
//		
//		// disable style
//		scene.getStylesheets().remove(styleSheetURL);;

		stage.setTitle("TTC monitoring");
		stage.setScene(scene);
		stage.show();
	}

}
