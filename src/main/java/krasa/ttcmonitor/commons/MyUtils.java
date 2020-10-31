package krasa.ttcmonitor.commons;

import javafx.scene.image.Image;
import krasa.ttcmonitor.TtcMonitorApplication;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

public class MyUtils {

	public static String getClasspathResource(String name) {
		InputStream resourceAsStream = TtcMonitorApplication.class.getResourceAsStream(name);
		try {
			return IOUtils.toString(resourceAsStream, "UTF-8");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static Image getImage(String name) {
		return new Image(Objects.requireNonNull(TtcMonitorApplication.class.getResourceAsStream(name)));
	}
}
