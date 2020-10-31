package krasa.ttcmonitor.controller.model;

import com.google.gson.annotations.Expose;
import krasa.ttcmonitor.service.MetadataManager;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

public class EsoWatch implements Serializable {

	private String trait;
	private String category1;
	private String category2;
	private String category3;
	@Expose
	private String link;
	private String quality;
	private String name;

	private String levelMin;
	private String priceMax;
	private String amountMin;
	@Expose
	private boolean enabled = true;
	private LocalDateTime lastCheck;
	@Expose
	private String exclude;
	@Expose
	private String cycle = "1";

	public EsoWatch(String link, String exclude, String cycle) {
		setLink(link);
		setExclude(exclude);
		setCycle(cycle);
	}

	public EsoWatch(String link) {
		setLink(link);
	}

	public EsoWatch() {
	}

	public void setLink(String link) {
		link = link.trim();
		this.link = link;
		Map<String, String> params = new HashMap<>();
		String[] split = link.split("&");
		for (String s : split) {
			String[] split1 = s.split("=");
			if (split1.length == 2) {
				params.put(split1[0], split1[1]);
			}
		}

		this.trait = MetadataManager.traits.get(params.get("ItemTraitID"));
		this.category1 = params.get("ItemCategory1ID") != null ? MetadataManager.categories1.get(params.get("ItemCategory1ID")).name : null;
		this.category2 = params.get("ItemCategory2ID") != null ? MetadataManager.categories2.get(params.get("ItemCategory2ID")).name : null;
		this.category3 = params.get("ItemCategory3ID") != null ? MetadataManager.categories3.get(params.get("ItemCategory3ID")).name : null;
		this.quality = MetadataManager.quality.get(params.get("ItemQualityID"));
		try {
			String itemNamePattern = params.get("ItemNamePattern");
			if (itemNamePattern != null) {
				this.name = URLDecoder.decode(itemNamePattern, "UTF-8");
			}
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
		this.priceMax = params.get("PriceMax");
		this.amountMin = params.get("AmountMin");
		this.levelMin = params.get("LevelMin");
	}

	public String getExclude() {
		return exclude;
	}

	public void setExclude(String exclude) {
		this.exclude = exclude;
	}

	public String getTrait() {
		return trait;
	}

	public String getCategory1() {
		return category1;
	}

	public String getCategory2() {
		return category2;
	}

	public String getCategory3() {
		return category3;
	}

	public String getLink() {
		return link;
	}

	public String getQuality() {
		return quality;
	}

	public String getName() {
		return name;
	}

	public String getLevelMin() {
		return levelMin;
	}

	public String getPriceMax() {
		return priceMax;
	}

	public String getAmountMin() {
		return amountMin;
	}

	@Override
	public String toString() {
		return "EsoWatch{" +
			", name='" + name + '\'' +
			", priceMax='" + priceMax + '\'' +
			", trait='" + trait + '\'' +
			", quality='" + quality + '\'' +
			", category1='" + category1 + '\'' +
			", category2='" + category2 + '\'' +
			", category3='" + category3 + '\'' +
			", amountMin='" + amountMin + '\'' +
			", levelMin='" + levelMin + '\'' +
			", link='" + link + '\'' +
			'}';
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public void setLastCheck(LocalDateTime lastCheck) {
		this.lastCheck = lastCheck;
	}

	public String getLastCheck() {
		if (lastCheck == null) {
			return "";
		}
		Duration between = Duration.between(lastCheck, LocalDateTime.now());
		return between.toMinutes() + " min";
	}

	public long getLastCheckMillis() {
		if (lastCheck == null) {
			return 0;
		}
		return lastCheck.toInstant(ZoneOffset.UTC).toEpochMilli();
	}

	public boolean isExcluded(EsoItem esoItem) {
		if (exclude != null) {
			String[] split = exclude.split(";");
			for (String s : split) {
				if (esoItem.getName().contains(s)) {
					return true;
				}
			}
		}
		return false;
	}

	public void setCycle(String cycle) {
		this.cycle = cycle;
	}

	public String getCycle() {
		return cycle;
	}

	public boolean canRun(int index) {
		int i = 0;
		try {
			i = Integer.parseInt(getCycle());
		} catch (Exception e) {
			i = 1;
		}
		return index % i == 0;
	}
}
