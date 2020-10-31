package krasa.ttcmonitor.commons;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Modality;
import javafx.stage.Stage;
import krasa.ttcmonitor.application.PrimaryStageInitializer;
import krasa.ttcmonitor.application.SpringbootJavaFxApplication;
import krasa.ttcmonitor.controller.ErrorController;
import krasa.ttcmonitor.controller.MainController;
import krasa.ttcmonitor.controller.model.Sound;
import krasa.ttcmonitor.service.MonitoringService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

public class Notifications {
	private static final Logger LOG = LoggerFactory.getLogger(MonitoringService.class);

	public static synchronized void play(krasa.ttcmonitor.controller.model.Sound sound) {
		try {
			if (!SpringbootJavaFxApplication.isRunning()) {
				return;
			}

			Media hit = null;
			hit = new Media(sound.getUri());

			MediaPlayer mediaPlayer = new MediaPlayer(hit);

			MainController bean = MainController.getInstance();
			TextField volume = bean.volume;
			String text = volume.getText();
			double v = Double.parseDouble(text) / 100;

			mediaPlayer.setOnReady(() -> {
				mediaPlayer.setVolume(v);
				mediaPlayer.play();
				mediaPlayer.setOnEndOfMedia(() -> {
					mediaPlayer.dispose();
				});
			});
		} catch (Throwable e) {
			LOG.error(e.getLocalizedMessage(), e);
		}
	}

	public static void notify(Sound sound) {
		Platform.runLater(() -> {
			try {
				play(sound);
				Stage stage = PrimaryStageInitializer.getStage();
				if (!stage.isFocused()) {
					stage.toFront();
				}
			} catch (Throwable e) {
				LOG.error("", e);
			}
		});

	}

	public static void showError(Thread t, Throwable e) {
		try {
			LOG.error("", e);
			play(Sound.FAIL);
			if (Platform.isFxApplicationThread()) {
				showErrorDialog(e);
			} else {
				Platform.runLater(() -> showErrorDialog(e));
			}
		} catch (Throwable ex) {
			ex.printStackTrace();
			LOG.error("", ex);
		}
	}

	private static void showErrorDialog(Throwable e) {
		StringWriter errorMsg = new StringWriter();
		e.printStackTrace(new PrintWriter(errorMsg));
		Stage dialog = new Stage();
		dialog.initModality(Modality.APPLICATION_MODAL);

		FXMLLoader loader = new FXMLLoader(ErrorController.class.getResource("ErrorController.fxml"));
		try {
			Parent root = loader.load();
			ErrorController controller = (ErrorController) loader.getController();
			controller.setErrorText(errorMsg.toString());
			if (e instanceof MyException) {
				controller.setLink(((MyException) e).getEsoWatch().getLink());
			} else {
				controller.setLink(null);
			}
			dialog.setScene(new Scene(root, 800, 400));
			dialog.addEventHandler(KeyEvent.KEY_RELEASED, (KeyEvent event) -> {
				if (KeyCode.ESCAPE == event.getCode()) {
					dialog.close();
				}
			});
			dialog.show();
		} catch (IOException exc) {
			exc.printStackTrace();
		}
	}
}
