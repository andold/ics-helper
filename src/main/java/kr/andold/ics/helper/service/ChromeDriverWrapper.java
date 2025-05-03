package kr.andold.ics.helper.service;

import org.openqa.selenium.chrome.ChromeOptions;

public class ChromeDriverWrapper extends kr.andold.utils.ChromeDriverWrapper {
	protected static final int PAUSE = 100;

	public ChromeDriverWrapper(ChromeOptions chromeOptions) {
		super(chromeOptions);
	}

}
