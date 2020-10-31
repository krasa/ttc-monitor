package krasa.ttcmonitor.controller.model;

import krasa.ttcmonitor.TtcMonitorApplication;

import java.io.FileNotFoundException;

public enum Sound {
	OK("Carcass_12divine.mp3"),
	FAIL("Carcass_4maps.mp3");

	public String path;

	Sound(String path) {
		this.path = path;
	}

	public String getUri() throws FileNotFoundException {
		return TtcMonitorApplication.class.getResource(path).toString();
	}
}
