package krasa.ttcmonitor.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import javafx.collections.ObservableList;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import krasa.ttcmonitor.controller.model.EsoWatch;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

@Component
public class Storage {
	public static final Path STORAGE = Paths.get("esoWatch.json");
	public static final Path STORAGE_OLD = Paths.get("esoWatch.json.old");
	public static final Path STORAGE_TMP = Paths.get("esoWatch.json.tmp");

	public void save(ObservableList<EsoWatch> items, CheckBox torCheckBox, TextField frequency, TextField volume) {
		StorageData storageData = new StorageData();
		storageData.watches.addAll(items);
		storageData.tor = torCheckBox.isSelected();
		storageData.frequency = frequency.getText();
		storageData.volume = volume.getText();

		String s = getGson().toJson(storageData);


		try {
			if (Files.exists(STORAGE)) {
				if (s.equals(Files.readString(STORAGE))) {
					return;
				}
			}

			Files.deleteIfExists(STORAGE_TMP);
			Files.writeString(STORAGE_TMP, s.toString());
			if (Files.exists(STORAGE)) {
				Files.move(STORAGE, STORAGE_OLD, StandardCopyOption.REPLACE_EXISTING);
			}
			Files.move(STORAGE_TMP, STORAGE);

		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private Gson getGson() {
		return new GsonBuilder()
			.setPrettyPrinting()
			.excludeFieldsWithoutExposeAnnotation()
			.create();

	}

	public void load(ObservableList<EsoWatch> items, CheckBox torCheckBox, TextField frequency, TextField volume) {
		try {
			StorageData storageData;
			if (Files.exists(STORAGE)) {

				String s1 = Files.readString(STORAGE);
				storageData = getGson().fromJson(new StringReader(s1), StorageData.class);
			} else {
				storageData = new StorageData();
			}
			torCheckBox.setSelected(storageData.tor);
			frequency.setText(storageData.frequency);
			volume.setText(storageData.volume);

			List<EsoWatch> watches = storageData.watches;
			items.clear();

			for (int i = 0; i < watches.size(); i++) {
				EsoWatch watch = watches.get(i);
				watch.setLink(watch.getLink());
				items.add(watch);
//				if (!this.storageData.watches.get(i).getLink().equals(watch.getLink())) {
//					throw new IllegalStateException();
//				}
			}

		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}

	public static class StorageData {
		@Expose
		private boolean tor = true;
		@Expose
		private String frequency = "30";
		@Expose
		private String volume = "30";
		@Expose
		private List<EsoWatch> watches = new ArrayList<>();

		public boolean isTor() {
			return tor;
		}

		public List<EsoWatch> getWatches() {
			return watches;
		}
	}

}
