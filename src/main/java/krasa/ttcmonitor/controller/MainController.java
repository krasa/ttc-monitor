package krasa.ttcmonitor.controller;

import com.sun.javafx.application.HostServicesDelegate;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Callback;
import javafx.util.Duration;
import krasa.ttcmonitor.application.SpringbootJavaFxApplication;
import krasa.ttcmonitor.commons.ActionButtonTableCell;
import krasa.ttcmonitor.commons.MyException;
import krasa.ttcmonitor.commons.MyUtils;
import krasa.ttcmonitor.commons.Notifications;
import krasa.ttcmonitor.controller.model.EsoItem;
import krasa.ttcmonitor.controller.model.EsoWatch;
import krasa.ttcmonitor.controller.model.Sound;
import krasa.ttcmonitor.service.MonitoringService;
import krasa.ttcmonitor.service.OldItemsStorage;
import krasa.ttcmonitor.service.ProxyProvider;
import krasa.ttcmonitor.service.Storage;
import net.rgielen.fxweaver.core.FxControllerAndView;
import net.rgielen.fxweaver.core.FxWeaver;
import net.rgielen.fxweaver.core.FxmlView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
@FxmlView // equal to: @FxmlView("MainController.fxml")
public class MainController implements DisposableBean {
	private static final Logger LOG = LoggerFactory.getLogger(MainController.class);
	public static final DataFormat ESO_WATCH = new DataFormat("EsoWatch");
//	public static final Path STORAGE = Paths.get("esoWatch.txt");
//	public static final Path STORAGE_OLD = Paths.get("esoWatch.txt.old");
//	public static final Path STORAGE_TMP = Paths.get("esoWatch.txt.tmp");

	private final String greeting;
	private final FxWeaver fxWeaver;
	private final FxControllerAndView<EditDialogController, VBox> dialog;
	private final ThreadPoolExecutor executor;

	@FXML
	public Button startFast;
	@FXML
	public CheckBox proxyCheckBox;

	@FXML
	public TextField volume;
	@FXML
	public Button reloadTable;

	@FXML
	public Label label;
	@FXML
	public Button addButton;
	@FXML
	public Button deleteAll;
	@FXML
	public Button testNotification;
	@FXML
	public Button testError;

	@FXML
	public TextField newUrl;
	@FXML
	public TableView<EsoWatch> table1;
	@FXML
	public TableView<EsoItem> table2;
	@FXML
	public CheckBox runningCheckBox;
	@FXML
	public TextField frequency;
	@FXML
	public Label counter;
	@Autowired
	MonitoringService monitoringService;

	@Autowired
	Storage storage;

	@Autowired
	OldItemsStorage oldItemsStorage;

	@Autowired
	ProxyProvider proxyProvider;

	final Object stick = new Object();
	volatile boolean fastFetchLoop = true;

	public MainController(@Value("${spring.application.demo.greeting}") String greeting,
						  FxWeaver fxWeaver,
						  FxControllerAndView<EditDialogController, VBox> dialog) {
		this.greeting = greeting;
		this.fxWeaver = fxWeaver;
		this.dialog = dialog;
		this.executor = new ThreadPoolExecutor(1, 1,
			0L, TimeUnit.MILLISECONDS,
			new LinkedBlockingQueue<Runnable>(), new CustomizableThreadFactory("ttc"));

		executor.submit(new MonitoringTask());
	}

	volatile EsoWatch last;

	public static MainController getInstance() {
		SpringbootJavaFxApplication instance = SpringbootJavaFxApplication.getInstance();
		return instance.getContext().getBean(MainController.class);
	}


	public void nextItem() {
		if (last != null) {
			int i = table1.getItems().indexOf(last);
			if (table1.getItems().size() > i + 1) {
				last = table1.getItems().get(i + 1);
			} else {
				last = null;
			}
		}
	}

	private int getTaskDelay(boolean first, List<EsoWatch> c) {
		int taskDelay = (getFrequencyMins() * 60_000) / c.size();
		taskDelay = Math.max(1000, taskDelay);
		if (first) {
			taskDelay = 1000;
		}
		return taskDelay + new Random().nextInt(10000);
	}

	private int getFrequencyMins() {
		int i = 5;
		try {
			i = Integer.parseInt(frequency.getText());
		} catch (Throwable e) {
			LOG.error("", e);
		}
		i = Math.max(1, i);
		return i;
	}

	private void addItems(EsoWatch p, List<EsoItem> esoItems) {
		boolean added = false;
		Collections.reverse(esoItems);


		for (EsoItem esoItem : esoItems) {
			if (p.isExcluded(esoItem)) {
				continue;
			}
			if (!oldItemsStorage.contains(esoItem.getLink())) {
				table2.getItems().add(0, esoItem);
				added = true;
			}
		}
		oldItemsStorage.add(esoItems);
		if (added) {
			Notifications.notify(Sound.OK);
		}
	}

	volatile boolean run = false;

	@FXML
	public void initialize() {
		startFast.setGraphic(new ImageView(MyUtils.getImage("rerun.png")));
		startFast.setText(null);
		addButton.setOnAction(
			actionEvent -> {
				EsoWatch e = new EsoWatch(newUrl.getText());
				table1.getItems().add(e);

				updateStorage();

				if (run) {
					process(e);
				}
			}
		);

		runningCheckBox.setOnAction(
			actionEvent -> {
				fastFetchLoop = false;
				startMonitoring();
			}
		);
		startFast.setOnAction(
			actionEvent -> {
				fastFetchLoop = true;
				runningCheckBox.setSelected(true);
				startMonitoring();
			}
		);
		reloadTable.setOnAction(
			actionEvent -> {
				table1.getColumns().clear();
				table2.getColumns().clear();
				initTable1();
				initTable2();
			}
		);


		initTable1();
		initTable2();
		initRefreshers();

		deleteAll.setOnAction(
//				actionEvent -> dialog.getController().show()
			actionEvent -> table2.getItems().clear()
		);
		testNotification.setOnAction(
			actionEvent -> {
				Notifications.notify(Sound.OK);
			}
		);

		testError.setOnAction(
			actionEvent -> {
				EsoWatch esoWatch = new EsoWatch();
//					esoWatch.setLink("https://eu.tamrieltradecentre.com/pc/Trade/SearchResult?SearchType=Sell&ItemID=&ItemNamePattern=necro&IsChampionPoint=true&LevelMin=160&LevelMax=&ItemCategory1ID=1&ItemCategory2ID=2&ItemCategory3ID=13&ItemQualityID=&ItemTraitID=13&PriceMin=&PriceMax=10000");
				throw new MyException("f", esoWatch);
			}
		);

		storage.load(table1.getItems(), proxyCheckBox, frequency, volume);
		proxyCheckBox.setText("Proxy: " + proxyProvider.getProxy().toString());
	}

	public void startMonitoring() {
		if (executor.getActiveCount() == 0) {
			executor.execute(new MonitoringTask());
		}
		runningCheckBox.setBackground(Background.EMPTY);

		boolean selected = runningCheckBox.isSelected();
		if (selected) {
			run = true;
		} else {
			run = false;
		}
		synchronized (stick) {
			stick.notifyAll();
		}
	}

	private void initRefreshers() {
		final Timeline timeline = new Timeline(
			new KeyFrame(
				Duration.millis(1000),
				event -> {
					String format = null;
					LocalTime last = monitoringService.getLast();
					if (last != null) {
						format = DateTimeFormatter.ofPattern("HH:mm").format(last);
					}
					counter.setText(String.valueOf(monitoringService.getRequests()) + " Last: " + format);
				}
			)
		);
		timeline.setCycleCount(Animation.INDEFINITE);
		timeline.play();

		final Timeline refreshTable = new Timeline(
			new KeyFrame(
				Duration.millis(60000),
				event -> {
					table1.refresh();
					table2.refresh();
				}
			)
		);
		refreshTable.setCycleCount(Animation.INDEFINITE);
		refreshTable.play();
		final Timeline threadMonitor = new Timeline(
			new KeyFrame(
				Duration.millis(60000),
				event -> {
					if (executor.getActiveCount() == 0 && runningCheckBox.isSelected()) {
						runningCheckBox.setBackground(new Background(new BackgroundFill(Color.RED, CornerRadii.EMPTY, Insets.EMPTY)));
						LOG.error("No worker is active");
					}
				}
			)
		);
		threadMonitor.setCycleCount(Animation.INDEFINITE);
		threadMonitor.play();
	}

	private void initTable1() {
		table1.setOnMouseClicked(new EventHandler<MouseEvent>() { //click
			@Override
			public void handle(MouseEvent event) {
				if (event.getClickCount() == 2) { // double click
					EsoWatch selected = table1.getSelectionModel().getSelectedItem();
					if (selected != null) {
						edit(selected);
					}
				}
			}
		});
		//click
		table1.setOnKeyPressed(new EventHandler<KeyEvent>() {
			@Override
			public void handle(KeyEvent keyEvent) {
				if (keyEvent.getCode() == KeyCode.DELETE) {
					EsoWatch selected = table1.getSelectionModel().getSelectedItem();
					if (selected != null) {
						delete(selected);
					}
				}
			}
		});

		table1.setOnDragDetected(new EventHandler<MouseEvent>() { //drag
			@Override
			public void handle(MouseEvent event) {
				// drag was detected, start drag-and-drop gesture
				EsoWatch selected = table1.getSelectionModel().getSelectedItem();
				if (selected != null) {

					Dragboard db = table1.startDragAndDrop(TransferMode.ANY);
					ClipboardContent content = new ClipboardContent();
					content.putString(String.valueOf(table1.getSelectionModel().getSelectedIndex()));
					content.put(ESO_WATCH, selected);
					db.setContent(content);
					event.consume();
				}
			}
		});

		table1.setOnDragOver(new EventHandler<DragEvent>() {
			@Override
			public void handle(DragEvent event) {
				// data is dragged over the target 
				Dragboard db = event.getDragboard();
				if (event.getDragboard().hasContent(ESO_WATCH)) {
					event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
				}
				event.consume();
			}
		});

		table1.setOnDragDropped(new EventHandler<DragEvent>() {
			@Override
			public void handle(DragEvent event) {
				Dragboard db = event.getDragboard();
				boolean success = false;
				if (event.getDragboard().hasContent(ESO_WATCH)) {

					EsoWatch text = (EsoWatch) db.getContent(ESO_WATCH);
					table1.getItems().add(text);
					table1.setItems(table1.getItems());
					success = true;
				}
				event.setDropCompleted(success);
				event.consume();
			}
		});
		table1.setRowFactory(tv -> {
			TableRow<EsoWatch> row = new TableRow();
			row.setOnDragOver(evt -> {
				if (evt.getDragboard().hasContent(ESO_WATCH)) {
					evt.acceptTransferModes(TransferMode.COPY_OR_MOVE);
				}
				evt.consume();
			});
			row.setOnDragDropped(evt -> {
				Dragboard db = evt.getDragboard();
				if (db.hasContent(ESO_WATCH)) {
					EsoWatch item = (EsoWatch) db.getContent(ESO_WATCH);
					if (row.isEmpty()) {
						// row is empty (at the end -> append item)
						table1.getItems().add(item);
					} else {
						int oldIndex = Integer.parseInt(db.getString());
						EsoWatch oldEsoWatch = table1.getItems().get(oldIndex);

						// decide based on drop position whether to add the element before or after
						int offset = row.getIndex() > oldIndex ? 1 : 0;
						table1.getItems().add(row.getIndex() + offset, item);
						table1.getItems().remove(oldEsoWatch);
						evt.setDropCompleted(true);
					}
				}
				evt.consume();
			});
			return row;
		});

		TableColumn<EsoWatch, CheckBox> select = new TableColumn<>("enabled");
//		select.setMinWidth(200);
		select.setCellValueFactory((Callback<TableColumn.CellDataFeatures<EsoWatch, CheckBox>, ObservableValue<CheckBox>>) arg0 -> {
			EsoWatch esoWatch = arg0.getValue();
			CheckBox checkBox = new CheckBox();
			checkBox.selectedProperty().setValue(esoWatch.isEnabled());

			checkBox.selectedProperty().addListener((ov, old_val, new_val) -> esoWatch.setEnabled(new_val));
			return new SimpleObjectProperty<CheckBox>(checkBox);

		});
		select.setComparator((o1, o2) -> Boolean.compare(o1.isSelected(), o2.isSelected()));

		table1.getColumns().add(select);

		table1.getColumns().add(column("name", "name", 6));
		table1.getColumns().add(column("priceMax", "priceMax"));
		table1.getColumns().add(column("amountMin", "amountMin"));
		table1.getColumns().add(column("quality", "quality"));
		table1.getColumns().add(column("trait", "trait"));
		table1.getColumns().add(column("category1", "category1"));
		table1.getColumns().add(column("category2", "category2"));
		table1.getColumns().add(column("category3", "category3"));
		table1.getColumns().add(column("levelMin", "levelMin"));

		TableColumn<EsoWatch, Button> edit = new TableColumn<>();
		edit.setCellFactory(ActionButtonTableCell.forTableColumn("Edit", (EsoWatch p) -> {
			edit(p);
			return p;
		}));
		table1.getColumns().add(edit);

		TableColumn<EsoWatch, Button> deleteWtach = new TableColumn<>();
		deleteWtach.setCellFactory(ActionButtonTableCell.forTableColumn(null, MyUtils.getImage("delete.png"), (EsoWatch p) -> {
			delete(p);
			return p;
		}));

		TableColumn<EsoWatch, String> nameColumn = new TableColumn<>("cycle");
		table1.getColumns().add(nameColumn);

		TableColumn<EsoWatch, Button> cycleDecrement = new TableColumn<>();
		cycleDecrement.setStyle("-fx-pref-height: 0;");
		cycleDecrement.setCellFactory(ActionButtonTableCell.forTableColumn("-", (EsoWatch p) -> {
			int i = Integer.parseInt(p.getCycle());
			if (i > 1) {
				p.setCycle(String.valueOf(i - 1));
				table1.refresh();
			}
			return p;
		}));
		nameColumn.getColumns().add(cycleDecrement);

		TableColumn<EsoWatch, String> cycle = new TableColumn<>();
		cycle.setStyle("-fx-pref-height: 0;");
		cycle.setCellFactory(TextFieldTableCell.forTableColumn());
		cycle.setCellValueFactory(new PropertyValueFactory<>("cycle"));
		cycle.setOnEditCommit(new EventHandler<TableColumn.CellEditEvent<EsoWatch, String>>() {
			@Override
			public void handle(TableColumn.CellEditEvent<EsoWatch, String> t) {
				EsoWatch esoWatch = t.getTableView().getItems().get(t.getTablePosition().getRow());
				esoWatch.setCycle(t.getNewValue());
				table1.refresh();

			}
		});
		cycle.setEditable(true);
		nameColumn.getColumns().add(cycle);


		TableColumn<EsoWatch, Button> cycleIncrement = new TableColumn<>();
		cycleIncrement.setStyle("-fx-pref-height: 0;");
		cycleIncrement.setCellFactory(ActionButtonTableCell.forTableColumn("+", (EsoWatch p) -> {
			int i = Integer.parseInt(p.getCycle());
			p.setCycle(String.valueOf(i + 1));
			table1.refresh();
			return p;
		}));
		nameColumn.getColumns().add(cycleIncrement);


		TableColumn<EsoWatch, Button> run = new TableColumn<>();
		run.setCellFactory(ActionButtonTableCell.forTableColumn(null, MyUtils.getImage("run.png"), (EsoWatch p) -> {
			process(p);
			return p;
		}));
		table1.getColumns().add(run);

		table1.getColumns().add(column("lastCheck", "lastCheck"));


		TableColumn<EsoWatch, Button> open = new TableColumn<>();
		open.setCellFactory(ActionButtonTableCell.forTableColumn("Open", (EsoWatch p) -> {
			HostServicesDelegate hostServices = HostServicesDelegate.getInstance(SpringbootJavaFxApplication.getInstance());
			hostServices.showDocument(p.getLink());
			return p;
		}));
		table1.getColumns().add(open);

		TableColumn<EsoWatch, Button> check = new TableColumn<>();
		check.setCellFactory(ActionButtonTableCell.forTableColumn("PriceCheck", (EsoWatch p) -> {
			HostServicesDelegate hostServices = HostServicesDelegate.getInstance(SpringbootJavaFxApplication.getInstance());
			hostServices.showDocument(p.getLink().replace("=Sell&", "=PriceCheck&"));
			return p;
		}));
		table1.getColumns().add(check);

		TableColumn<EsoWatch, Button> url = new TableColumn<>();
		url.setCellFactory(ActionButtonTableCell.forTableColumn("Copy URL", (EsoWatch p) -> {
			Clipboard clipboard = Clipboard.getSystemClipboard();
			final ClipboardContent content = new ClipboardContent();
			content.putString(p.getLink());
			clipboard.setContent(content);
			return p;
		}));
		table1.getColumns().add(url);

		TableColumn<EsoWatch, Button> copy = new TableColumn<>();
		copy.setCellFactory(ActionButtonTableCell.forTableColumn("Copy Name", (EsoWatch p) -> {
			Clipboard clipboard = Clipboard.getSystemClipboard();
			final ClipboardContent content = new ClipboardContent();
			content.putString(p.getName());
			clipboard.setContent(content);
			return p;
		}));
		table1.getColumns().add(copy);

		table1.getColumns().add(deleteWtach);
	}

	private void delete(EsoWatch p) {
		table1.getItems().remove(p);
		updateStorage();
	}

	private void edit(EsoWatch p) {
		fxWeaver.loadController(EditDialogController.class).show(p);
	}

	private void process(EsoWatch p) {
		List<EsoItem> esoItems = monitoringService.process(p, proxyCheckBox.isSelected());
		if (!esoItems.isEmpty()) {
			Platform.runLater(() -> addItems(p, esoItems));
		}
		Platform.runLater(() -> table1.refresh());

	}


	private void initTable2() {
		table2.getColumns().add(column2("name", "name", 8));
		table2.getColumns().add(column2("quality", "quality"));
		table2.getColumns().add(column2("price", "price", 8));
		table2.getColumns().add(column2("location", "location", 5));
		table2.getColumns().add(column2("created", "created"));
		TableColumn<EsoItem, ?> e = column2("elapsed", "elapsed");
		e.setComparator(new Comparator() {
			@Override
			public int compare(Object o1, Object o2) {
				Duration duration = toDuration((String) o1);
				Duration duration1 = toDuration((String) o2);
				return duration.compareTo(duration1);
			}

			public Duration toDuration(String o1) {
				String inputDate = o1;
				String[] split = inputDate.split(" ");
				int hours = Integer.parseInt(split[0].replace("h", ""));
				int minutes = Integer.parseInt(split[1].replace("min", ""));
				return Duration.hours(hours).add(Duration.minutes(minutes));
			}
		});
		table2.getColumns().add(e);
		table2.getColumns().add(column2("level", "level"));
		table2.getColumns().add(column2("trait", "trait"));
		table2.getColumns().add(column2("category1", "category1"));
		table2.getColumns().add(column2("category2", "category2"));
		table2.getColumns().add(column2("category3", "category3"));

		TableColumn<EsoItem, Button> column1 = new TableColumn<>("");
		column1.setCellFactory(ActionButtonTableCell.forTableColumn("Remove", (EsoItem p) -> {
			table2.getItems().remove(p);
			return p;
		}));
		table2.getColumns().add(column1);
		TableColumn<EsoItem, Button> copy = new TableColumn<>("");
		copy.setCellFactory(ActionButtonTableCell.forTableColumn("Copy", (EsoItem p) -> {
			Clipboard clipboard = Clipboard.getSystemClipboard();
			final ClipboardContent content = new ClipboardContent();
			content.putString(p.getName());
			clipboard.setContent(content);
			return p;
		}));
		table2.getColumns().add(copy);

/*
        openSimpleDialogButton.setOnAction(
                actionEvent -> fxWeaver.loadController(DialogController.class).show()
        );
*/
	}

	private void updateStorage() {
		storage.save(table1.getItems(), proxyCheckBox, frequency, volume);
	}

	private TableColumn<EsoWatch, ?> column(String name, String name2, Integer... ratio) {
		TableColumn<EsoWatch, ?> column1 = new TableColumn<>(name);
		column1.setCellValueFactory(new PropertyValueFactory<>(name2));
		if (ratio.length == 1) {
			column1.prefWidthProperty().bind(table1.widthProperty().divide(ratio[0])); // w * 1/ratio
		}
		return column1;
	}


	private TableColumn<EsoItem, ?> column2(String name, String name2, Integer... ratio) {
		TableColumn<EsoItem, ?> column1 = new TableColumn<>(name);
		column1.setCellValueFactory(new PropertyValueFactory<>(name2));
		if (ratio.length == 1) {
			column1.prefWidthProperty().bind(table1.widthProperty().divide(ratio[0])); // w * 1/ratio
		}
		return column1;
	}


	public void update(EsoWatch old, EsoWatch newEW) {
		ObservableList<EsoWatch> items = table1.getItems();
		int i = items.indexOf(old);
		items.add(i, newEW);
		items.remove(i + 1);
		updateStorage();
	}

	volatile boolean exit;

	@Override
	public void destroy() throws Exception {
		exit = true;
		executor.shutdownNow();
		if (table1.getItems().size() > 0) { //properly started
			updateStorage();
		}
	}


	public void nextProxy(ActionEvent actionEvent) {
		proxyProvider.next();
		proxyCheckBox.setText("Proxy: " + proxyProvider.getProxy().toString());
	}

	private class MonitoringTask implements Runnable {
		int index = 0;

		@Override
		public void run() {
			while (!exit) {
				try {
					if (run) {
						index++;
						processItems();
						fastFetchLoop = false;
					}
					try {
						synchronized (stick) {
							stick.wait(1000);
						}
					} catch (InterruptedException e) {
						LOG.debug("", e);
					}
				} catch (Throwable e) {
					LOG.error("", e);
				}
			}
		}

		private void processItems() {
			EsoWatch next = null;
			try {
				List<EsoWatch> c = new ArrayList<>(table1.getItems()).stream()
					.filter(EsoWatch::isEnabled)
					.filter(esoWatch -> esoWatch.canRun(this.index) || fastFetchLoop)
					.sorted(new Comparator<EsoWatch>() {
						@Override
						public int compare(EsoWatch o1, EsoWatch o2) {
							return Long.compare(o1.getLastCheckMillis(), o2.getLastCheckMillis());
						}
					})
					.collect(Collectors.toList());
				LOG.info("Cycle: " + this.index + ", watches: " + c.size());
				if (c.size() == 0) {
					return;
				}

				int i = 0;
				if (last != null) {
					if (!fastFetchLoop && c.indexOf(last) >= 0) {
						i = c.indexOf(last);
					}
					last = null;
				}
				for (; i < c.size(); i++) {
					next = c.get(i);
					last = next;
					if (!run) {
						break;
					}
					if (next.isEnabled()) {
						process(next);
					} else {
						//					LOG.info("Skipping: " + next);
						continue;
					}

					synchronized (stick) {
						int taskDelay = getTaskDelay(fastFetchLoop && i != c.size() - 1, c);
						java.time.Duration duration = java.time.Duration.ofMillis(taskDelay);
						LOG.info("Waiting " + duration.toMinutes() + "min " + duration.toSecondsPart() + "sec");
						stick.wait(taskDelay);
					}
				}
				last = null;
			} catch (Throwable t) {
				runningCheckBox.setSelected(false);
				runningCheckBox.setBackground(new Background(new BackgroundFill(Color.RED, CornerRadii.EMPTY, Insets.EMPTY)));
				run = false;
				Notifications.showError(Thread.currentThread(), t);
			}
		}

	}
}
