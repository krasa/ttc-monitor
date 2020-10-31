package krasa.ttcmonitor.commons;

import krasa.ttcmonitor.controller.model.EsoWatch;

public class MyException extends RuntimeException {
	private final EsoWatch esoWatch;

	public MyException(String s, EsoWatch esoWatch) {
		super((s));
		this.esoWatch = esoWatch;
	}

	public EsoWatch getEsoWatch() {
		return esoWatch;
	}
}
