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
import kr.andold.utils.ChromeDriverWrapper;
import kr.andold.utils.Utility;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ChromeService {
	private static final boolean fHeadless = false;
	ChromeDriverWrapper driver;

	@Getter
	private static String webdriverPath;
	@Value("${user.selenium.webdriver.chrome.driver}")
	public void setWebdriverPath(String value) {
		log.info("{} setWebdriverPath(『{}』)", Utility.indentMiddle(), value);
		webdriverPath = value;
	}

	@Getter
	private static String userDataDir;
	@Value("${user.selenium.user.data.dir}")
	public void setUserDataDir(String value) {
		log.info("{} setUserDataDir(『{}』)", Utility.indentMiddle(), value);
		userDataDir = value;
	}

	@PostConstruct
	public void postConstruct() {
		System.setProperty("webdriver.chrome.driver", getWebdriverPath());
		ChromeOptions chromeOptions = new ChromeOptions();
		chromeOptions.addArguments("--disable-blink-features=AutomationControlled");
		chromeOptions.addArguments("--disable-dev-shm-usage");
		chromeOptions.addArguments("--disable-infobars");
		if (fHeadless) {
			chromeOptions.addArguments("--headless");
		}
		chromeOptions.addArguments("--remote-allow-origins=*");
		chromeOptions.addArguments("--window-size=2048,1024");
		chromeOptions.addArguments(String.format("--user-data-dir=%s", getUserDataDir()));
		chromeOptions.setPageLoadStrategy(PageLoadStrategy.NONE);
		driver = new ChromeDriverWrapper(chromeOptions);
		java.awt.Dimension d = MainFrame.sizeByScreen(7, 10);
		driver.manage().window().setSize(new Dimension(d.width, d.height));
		java.awt.Point p = MainFrame.locationByScreen(5, 0);
		driver.manage().window().setPosition(new Point(p.x, p.y));
	}

	@PreDestroy
	public void preDestroy() {
		driver.quit();
	}

	public void crawl() {
		log.info("{}", driver);
	}


}
