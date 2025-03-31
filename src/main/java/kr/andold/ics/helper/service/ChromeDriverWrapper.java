package kr.andold.ics.helper.service;

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeOptions;

import kr.andold.utils.Utility;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ChromeDriverWrapper extends kr.andold.utils.ChromeDriverWrapper {
	protected static final int PAUSE = 100;

	public ChromeDriverWrapper(ChromeOptions chromeOptions) {
		super(chromeOptions);
	}

	public void sendKeys(By by, String path) {
		try {
			WebElement element = super.findElement(by);
			element.sendKeys(path);
		} catch (Exception e) {
			log.error("Exception:: {}", e.getLocalizedMessage(), e);
		}
	}

	// bug fix
	public boolean isEmpty(By xpath, int milli) {
		while (milli >= 0) {
			try {
				List<WebElement> es = super.findElements(xpath);
				return es.isEmpty();
			} catch (Exception e) {
			}
			Utility.sleep(PAUSE);
			milli -= PAUSE;
		}
		return true;
	}

}
