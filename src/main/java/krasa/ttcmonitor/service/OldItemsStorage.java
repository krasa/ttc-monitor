package krasa.ttcmonitor.service;

import krasa.ttcmonitor.controller.model.EsoItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static krasa.ttcmonitor.commons.Notifications.showError;

@Service
public class OldItemsStorage {
	private static final Logger LOG = LoggerFactory.getLogger(OldItemsStorage.class);

	public static final Path STORAGE = Paths.get("esoOldItems.txt");
	public static final Path STORAGE_OLD = Paths.get("esoOldItems.txt.old");
	public static final Path STORAGE_TMP = Paths.get("esoOldItems.txt.tmp");

	private Set<String> old = new HashSet<>();

	@PostConstruct
	public void init() throws IOException {
		try {
			if (Files.exists(STORAGE)) {
				List<String> strings = Files.readAllLines(STORAGE);
				for (String link : strings) {
					if (link.isBlank()) {
						continue;
					}
					old.add(link);
				}
			}
		} catch (IOException e) {
			LOG.error("", e);
		}
	}

	public synchronized boolean contains(String link) {
		return old.contains(link);
	}

	public synchronized void add(List<EsoItem> esoItems) {
		boolean added = false;
		for (EsoItem esoItem : esoItems) {
			added |= old.add(esoItem.getLink());
		}
		if (added) {
			updateStorage();
		}
	}

	private void updateStorage() {
		StringBuilder sb = new StringBuilder();
		for (String s : old) {
			sb.append(s).append("\n");
		}

		try {
			Files.deleteIfExists(STORAGE_TMP);
			Files.writeString(STORAGE_TMP, sb.toString());
			if (Files.exists(STORAGE)) {
				Files.move(STORAGE, STORAGE_OLD, StandardCopyOption.REPLACE_EXISTING);
			}
			Files.move(STORAGE_TMP, STORAGE);
		} catch (IOException e) {
			showError(Thread.currentThread(), e);
		}
	}


}