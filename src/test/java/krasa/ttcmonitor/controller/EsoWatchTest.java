package krasa.ttcmonitor.controller;

import krasa.ttcmonitor.controller.model.EsoWatch;
import krasa.ttcmonitor.service.MetadataManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EsoWatchTest {
	@BeforeEach
	void setUp() throws IOException {
		new MetadataManager().init();

	}

	@Test
	void name() {
		String link = "https://eu.tamrieltradecentre.com/pc/Trade/SearchResult?ItemID=&SearchType=Sell&ItemNamePattern=willpower&ItemCategory1ID=1&ItemCategory2ID=6&ItemCategory3ID=34&ItemTraitID=17&ItemQualityID=3&IsChampionPoint=false&LevelMin=160&LevelMax=&MasterWritVoucherMin=&MasterWritVoucherMax=&AmountMin=&AmountMax=&PriceMin=&PriceMax=1000";
		EsoWatch esoWatch = new EsoWatch(link);
		assertEquals(null, esoWatch.getAmountMin());
		assertEquals("Apparel", esoWatch.getCategory1());
		assertEquals("Accessory", esoWatch.getCategory2());
		assertEquals("Neck", esoWatch.getCategory3());
		assertEquals(link, esoWatch.getLink());
		assertEquals("willpower", esoWatch.getName());
		assertEquals("1000", esoWatch.getPriceMax());
		assertEquals("Epic", esoWatch.getQuality());
		assertEquals("Arcane", esoWatch.getTrait());
		assertEquals("160", esoWatch.getLevelMin());

	}
}