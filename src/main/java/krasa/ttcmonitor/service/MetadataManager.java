package krasa.ttcmonitor.service;

import krasa.ttcmonitor.commons.MyUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Service
public class MetadataManager {
	public static Map<String, Category1> categories1 = new HashMap<>();
	public static Map<String, Category2> categories2 = new HashMap<>();
	public static Map<String, Category3> categories3 = new HashMap<>();
	public static Map<String, String> traits = new HashMap<>();
	public static Map<String, String> quality = new HashMap<>();

	public static void main(String[] args) throws IOException {
		MetadataManager categoriesManager = new MetadataManager();
		categoriesManager.init();
		System.err.println(MetadataManager.categories1);
		System.err.println(MetadataManager.traits);
		System.err.println(MetadataManager.quality);
	}

	@PostConstruct
	public void init() throws IOException {
//		File file = new File("F:\\_projects\\javafx-weaver\\samples\\springboot-sample\\src\\main\\resources\\categories.html");
		String file = MyUtils.getClasspathResource("categories.html");
		Document categoriesDoc = Jsoup.parse(file);

		category1(categoriesDoc);
		category2(categoriesDoc);
		category3(categoriesDoc);
		traits(categoriesDoc);
		quality(categoriesDoc);
	}

	private void quality(Document categoriesDoc) {
		Elements select = categoriesDoc.select("select[name=ItemQualityID]");
		Elements option = select.select("option");
		Iterator<Element> options = option.iterator();
		while (options.hasNext()) {
			Element next = options.next();
			String id = next.attr("value");
			String name = next.text();
			quality.put(id, name);
		}

	}

	private void traits(Document categoriesDoc) {
		Elements select = categoriesDoc.select("select[name=ItemTraitID]");
		Elements option = select.select("option");
		Iterator<Element> options = option.iterator();
		while (options.hasNext()) {
			Element next = options.next();
			String id = next.attr("value");
			String name = next.text();
			traits.put(id, name);
		}

	}

	private void category3(Document categoriesDoc) {
		Elements select = categoriesDoc.select("select[name=ItemCategory3ID]");
		Elements option = select.select("option");
		Iterator<Element> options = option.iterator();
		while (options.hasNext()) {
			Element next = options.next();
			String id = next.attr("value");
			String cat1 = next.attr("data-associated-category1");
			String cat2 = next.attr("data-associated-category2");
			String name = next.text();
			Category3 category3 = new Category3(id, name);
			categories2.get(cat2).put(category3);
			categories3.put(id, category3);
		}

	}

	private void category2(Document categoriesDoc) {
		Elements select = categoriesDoc.select("select[name=ItemCategory2ID]");
		Elements option = select.select("option");
		Iterator<Element> options = option.iterator();
		while (options.hasNext()) {
			Element next = options.next();
			String id = next.attr("value");
			String cat1 = next.attr("data-associated-category1");
			String cat2 = next.attr("data-associated-category2");
			String name = next.text();
			Category2 category2 = new Category2(id, name);
			categories1.get(cat1).put(category2);
			categories2.put(id, category2);
		}

	}

	private void category1(Document categoriesDoc) {
		Elements select = categoriesDoc.select("select[name=ItemCategory1ID]");
		Elements option = select.select("option");
		Iterator<Element> options = option.iterator();
		while (options.hasNext()) {
			Element next = options.next();
			String id = next.attr("value");
			String cat1 = next.attr("data-associated-category1");
			String cat2 = next.attr("data-associated-category2");
			String name = next.text();
			categories1.put(id, new Category1(id, name));
		}
	}

	public static class Category1 {
		public String name;
		public String id;
		public Map<String, Category2> categories2 = new HashMap<>();

		public Category1(String id, String name) {
			this.id = id;
			this.name = name;
		}

		@Override
		public String toString() {
			return "Category1{" +
				"name='" + name + '\'' +
				", id='" + id + '\'' +
				", categories2=" + categories2 +
				'}';
		}

		public void put(Category2 category2) {
			categories2.put(category2.id, category2);
		}
	}

	public static class Category2 {
		public String name;
		public String id;
		public Map<String, Category3> categories3 = new HashMap<>();

		public Category2(String id, String name) {
			this.id = id;
			this.name = name;
		}

		@Override
		public String toString() {
			return "Category2{" +
				"name='" + name + '\'' +
				", id='" + id + '\'' +
				", categories3=" + categories3 +
				'}';
		}

		public void put(Category3 category3) {
			categories3.put(category3.id, category3);
		}
	}

	public static class Category3 {
		public String name;
		public String id;
		public Map<String, String> categories3 = new HashMap<>();

		public Category3(String id, String name) {
			this.id = id;
			this.name = name;
		}

		@Override
		public String toString() {
			return "Category3{" +
				"name='" + name + '\'' +
				", id='" + id + '\'' +
				", categories3=" + categories3 +
				'}';
		}
	}
}
