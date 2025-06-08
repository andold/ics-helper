package kr.andold.ics.helper.service;

import org.openqa.selenium.Dimension;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.Point;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import kr.andold.ics.helper.container.MainFrame;
import kr.andold.utils.Utility;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ChromeDriverClient {
	private static final boolean fHeadless = false;

	@Getter private ChromeDriverWrapper driver;

	@Getter
	private static String userSeleniumWebdriverChromeDriver;
	@Value("${user.selenium.webdriver.chrome.driver}")
	public void setUserSeleniumWebdriverChromeDriver(String value) {
		log.info("{} setUserSeleniumWebdriverChromeDriver(『{}』)", Utility.indentMiddle(), value);
		userSeleniumWebdriverChromeDriver = value;
	}

	@Getter
	private static String userSeleniumUserDataDir;
	@Value("${user.selenium.user.data.dir}")
	public void setUserSeleniumUserDataDir(String value) {
		log.info("{} setUserSeleniumUserDataDir(『{}』)", Utility.indentMiddle(), value);
		userSeleniumUserDataDir = value;
	}

	@PostConstruct
	public void postConstruct() {
		log.info("{} postConstruct()", Utility.indentStart());

		System.setProperty("webdriver.chrome.driver", getUserSeleniumWebdriverChromeDriver());
		ChromeOptions chromeOptions = new ChromeOptions();
		chromeOptions.addArguments("--disable-blink-features=AutomationControlled");
		chromeOptions.addArguments("--disable-dev-shm-usage");
		chromeOptions.addArguments("--disable-infobars");
		if (fHeadless) {
			chromeOptions.addArguments("--headless");
		}
		chromeOptions.addArguments("--remote-allow-origins=*");
		chromeOptions.addArguments("--window-size=2048,1024");
		chromeOptions.addArguments(String.format("--user-data-dir=%s-client", getUserSeleniumUserDataDir()));
		chromeOptions.setPageLoadStrategy(PageLoadStrategy.NONE);
		driver = new ChromeDriverWrapper(chromeOptions);
		java.awt.Dimension d = MainFrame.sizeByScreen(7, 9);
		driver.manage().window().setSize(new Dimension(d.width, d.height));
		java.awt.Point p = MainFrame.locationByScreen(5, 0);
		driver.manage().window().setPosition(new Point(p.x, p.y));

		log.info("{} postConstruct()", Utility.indentEnd());
	}

	@PreDestroy
	public void preDestroy() {
		driver.quit();
	}


}
