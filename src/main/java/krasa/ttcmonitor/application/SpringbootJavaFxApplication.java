package krasa.ttcmonitor.application;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import krasa.ttcmonitor.TtcMonitorApplication;
import krasa.ttcmonitor.commons.Notifications;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericApplicationContext;

public class SpringbootJavaFxApplication extends Application {
	private static SpringbootJavaFxApplication instance;

	private ConfigurableApplicationContext context;

	public static boolean isRunning() {
		SpringbootJavaFxApplication instance = getInstance();
		if (instance == null) {
			return false;
		}
		ConfigurableApplicationContext context = instance.getContext();
		if (context == null) {
			return false;
		}
		return context.isRunning();
	}

	@Override
	public void init() throws Exception {
		Thread.setDefaultUncaughtExceptionHandler(Notifications::showError);
		ApplicationContextInitializer<GenericApplicationContext> initializer =
			context -> {
				context.registerBean(Application.class, () -> SpringbootJavaFxApplication.this);
				context.registerBean(Parameters.class, this::getParameters); // for demonstration, not really needed
			};
		this.context = new SpringApplicationBuilder()
			.sources(TtcMonitorApplication.class)
			.initializers(initializer)
			.run(getParameters().getRaw().toArray(new String[0]));
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		context.publishEvent(new StageReadyEvent(primaryStage));
	}

	@Override
	public void stop() throws Exception {
		this.context.close();
		Platform.exit();
	}

	public SpringbootJavaFxApplication() {
		instance = this;
	}

	public static SpringbootJavaFxApplication getInstance() {
		return instance;
	}

	public ConfigurableApplicationContext getContext() {
		return context;
	}
}