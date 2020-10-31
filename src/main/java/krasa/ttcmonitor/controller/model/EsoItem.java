package krasa.ttcmonitor.controller.model;

import java.time.Duration;
import java.time.LocalDateTime;

public class EsoItem {

	private String trait;
	private String category1;
	private String category2;
	private String category3;
	private String link;
	private String quality;
	private String name;

	private String level;
	private String price;

	private String location;
	private String created;
	private LocalDateTime from;
	;

	public String getElapsed() {
		Duration between = Duration.between(from, LocalDateTime.now());
		return between.toHours() + "h " + between.toMinutesPart() + "min";
	}

	public void setElapsed(String elapsed) {
		int elapsedInt = Integer.parseInt(elapsed);
		from = LocalDateTime.now().minusMinutes(elapsedInt);
	}

	public String getTrait() {
		return trait;
	}

	public void setTrait(String trait) {
		this.trait = trait;
	}

	public String getCategory1() {
		return category1;
	}

	public void setCategory1(String category1) {
		this.category1 = category1;
	}

	public String getCategory2() {
		return category2;
	}

	public void setCategory2(String category2) {
		this.category2 = category2;
	}

	public String getCategory3() {
		return category3;
	}

	public void setCategory3(String category3) {
		this.category3 = category3;
	}

	public String getLink() {
		return link;
	}

	public void setLink(String link) {
		this.link = link;
	}

	public String getQuality() {
		return quality;
	}

	public void setQuality(String quality) {
		this.quality = quality.replace("item-quality-", "");
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getLevel() {
		return level;
	}

	public void setLevel(String level) {
		this.level = level;
	}

	public String getPrice() {
		return price;
	}

	public void setPrice(String price) {
		this.price = price;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public String getCreated() {
		return created;
	}

	public void setCreated(String created) {
		this.created = created;
	}

}
