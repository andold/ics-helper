package kr.andold.ics.helper.service;

import java.io.File;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Set;

import org.openqa.selenium.By;
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
public class ChromeService {
	public static final String DOWNLOAD_URL = "https://calendar.naver.com/";

	private static final Duration DEFAULT_TIMEOUT_DURATION = Duration.ofSeconds(4);
	private static final Duration DEFAULT_TIMEOUT_DURATION_LONG = Duration.ofMinutes(5);
	private static final Integer VCALENDAR_ID = 1028;
	private static final boolean fHeadless = false;
	ChromeDriverWrapper driver;

	@Getter
	private static String webdriverPath;
	@Value("${user.selenium.webdriver.chrome.driver:C:/apps/chromedriver-win64/chromedriver.exe}")
	public void setWebdriverPath(String value) {
		log.info("{} setWebdriverPath(『{}』)", Utility.indentMiddle(), value);
		webdriverPath = value;
	}

	@Getter
	private static String userDataDir;
	@Value("${user.selenium.user.data.dir:C:/logs/test-ics-helper/.selenium}")
	public void setUserDataDir(String value) {
		log.info("{} setUserDataDir(『{}』)", Utility.indentMiddle(), value);
		userDataDir = value;
	}

	@Getter
	private static String userUploadUrl;
	@Value("${user.upload.url:http://localhost/ics/}")
	public void setUserUploadUrl(String value) {
		log.info("{} setUserUploadUrl(『{}』)", Utility.indentMiddle(), value);
		userUploadUrl = value;
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
		log.info("{} crawl()", Utility.indentStart());
		long started = System.currentTimeMillis();

		String filename = download();
		if (filename == null || filename.isBlank()) {
			return;
		}

		int count = upload(filename);

		log.info("{} crawl() - {}", Utility.indentEnd(), count, Utility.toStringPastTimeReadable(started));
		return;
	}

	private int upload(String filename) {
		log.info("{} upload({})", Utility.indentStart(), filename, VCALENDAR_ID);

		String fullPath = String.format("%s/Downloads/%s", System.getProperty("user.home"), filename);
		try {
			Set<String> windowHandles = driver.getWindowHandles();
			String windowHandle = (String) windowHandles.toArray()[0];
			driver.switchTo().window(windowHandle);

			if (notUpload(driver)) {
				navigateUpload(driver);
			}

			driver.manage().timeouts().implicitlyWait(DEFAULT_TIMEOUT_DURATION);

			//	파일선택	/html/body/div[3]/div/div/div[2]/form/div[1]/div/input
			By BY_XPATH_FILE_SELECT = By.xpath("//form//input[contains(@type,'file')]");
			log.debug("{} upload(...) - 『{}』『{}』", Utility.indentMiddle(), "파일선택", driver.getText(BY_XPATH_FILE_SELECT, Duration.ZERO));
			driver.presenceOfElementLocated(BY_XPATH_FILE_SELECT, DEFAULT_TIMEOUT_DURATION);
			driver.sendKeys(BY_XPATH_FILE_SELECT, fullPath);
			log.debug("{} upload(...) - 『{}』『{}』", Utility.indentMiddle(), "파일선택", driver.getText(BY_XPATH_FILE_SELECT, Duration.ZERO));
			
			//	Submit	/html/body/div[3]/div/div/div[2]/form/div[3]/div/button
			By BY_XPATH_SUBMIT_BUTTON = By.xpath("//form//div/button[contains(text(),'Submit')]");
			log.debug("{} upload(...) - 『{}』『{}』", Utility.indentMiddle(), "Submit", driver.getText(BY_XPATH_SUBMIT_BUTTON, Duration.ZERO));
			driver.presenceOfElementLocated(BY_XPATH_SUBMIT_BUTTON, DEFAULT_TIMEOUT_DURATION);
			driver.clickIfExist(BY_XPATH_SUBMIT_BUTTON);
			Utility.sleep(1000);
			log.debug("{} upload(...) - 『{}』『{}』", Utility.indentMiddle(), "Submit", driver.getText(BY_XPATH_SUBMIT_BUTTON, Duration.ZERO));
			
			//	/html/body/div[3]/div/div/div[1]/div/div
			//	<div title="올린 파일 분석중" class="ms-0 me-1 align-middle spinner-grow text-warning"></div>
			By BY_XPATH_PROGRESS_UPLOADING = By.xpath("//div[contains(@title,'올린 파일 분석중')]");
			log.debug("{} upload(...) - 『{}』『{}』", Utility.indentMiddle(), "올린 파일 분석중", driver.getText(BY_XPATH_PROGRESS_UPLOADING, Duration.ZERO));
			driver.waitUntilExist(BY_XPATH_PROGRESS_UPLOADING, false, 1000 * 60);
			log.debug("{} upload(...) - 『{}』『{}』", Utility.indentMiddle(), "올린 파일 분석중", driver.getText(BY_XPATH_PROGRESS_UPLOADING, Duration.ZERO));

			//	No Create Data!	/html/body/div[3]/div/div/div[2]/div/div[1]
			//	Create #4		/html/body/div[3]/div/div/div[2]/div/div[1]/h2/button
			By BY_XPATH_CREATE_DATA = By.xpath("//div[contains(@class,'modal-body')]/div[contains(@class,'accordion')]/div[1]");
			By BY_XPATH_NO_CREATE_DATA = By.xpath("//div[contains(@class,'modal-body')]/div[contains(@class,'accordion')]/div[contains(text(),'No Create Data!')]");
			log.debug("{} upload(...) - 『{}』『{}』", Utility.indentMiddle(), "No Create Data!", driver.getText(BY_XPATH_NO_CREATE_DATA, Duration.ZERO));
			driver.waitUntilTextMatch(BY_XPATH_CREATE_DATA, "((No Create Data!)|(Create #[0-9]+))");
			log.debug("{} upload(...) - 『{}』『{}』", Utility.indentMiddle(), "No Create Data!", driver.getText(BY_XPATH_NO_CREATE_DATA, Duration.ZERO));
			if (driver.isEmpty(BY_XPATH_NO_CREATE_DATA, 0)) {
				log.info("{} 생성합니다", Utility.indentMiddle());
				//	Select All And Do Batch	/html/body/div[3]/div/div/div[2]/div/div[1]/div/div/div[1]/div[1]/button[3]
				By BY_XPATH_CREATE_ALL_BATCH_BUTTON = By.xpath("/html/body/div[3]/div/div/div[2]/div/div[1]/div/div/div[1]/div[1]/button[contains(text(),'Select All And Do Batch')]");
				log.debug("{} upload(...) - 『{}』『{}』", Utility.indentMiddle(), "Select All And Do Batch", driver.getText(BY_XPATH_CREATE_ALL_BATCH_BUTTON, Duration.ZERO));
				driver.presenceOfElementLocated(BY_XPATH_CREATE_ALL_BATCH_BUTTON, DEFAULT_TIMEOUT_DURATION);
				driver.clickIfExist(BY_XPATH_CREATE_ALL_BATCH_BUTTON);
				log.debug("{} upload(...) - 『{}』『{}』", Utility.indentMiddle(), "Select All And Do Batch", driver.getText(BY_XPATH_CREATE_ALL_BATCH_BUTTON, Duration.ZERO));
			} else {
				log.info("{} 생성할게 없습니다", Utility.indentMiddle());
			}

			//	No Update Data!	/html/body/div[3]/div/div/div[2]/div/div[3]	/html/body/div[3]/div/div/div[2]/div/div[3]/h2/button
			By BY_XPATH_UPDATE_DATA = By.xpath("//div[contains(@class,'modal-body')]/div[contains(@class,'accordion')]/div[3]");
			By BY_XPATH_NO_UPDATE_DATA = By.xpath("//div[contains(@class,'modal-body')]/div[contains(@class,'accordion')]/div[contains(text(),'No Update Data!')]");
			log.debug("{} upload(...) - 『{}』『{}』", Utility.indentMiddle(), "No Update Data!", driver.getText(BY_XPATH_UPDATE_DATA, Duration.ZERO));
			driver.waitUntilTextMatch(BY_XPATH_UPDATE_DATA, "((No Update Data!)|(Update #[0-9]+))");
			log.debug("{} upload(...) - 『{}』『{}』", Utility.indentMiddle(), "No Update Data!", driver.getText(BY_XPATH_UPDATE_DATA, Duration.ZERO));
			if (driver.isEmpty(BY_XPATH_NO_UPDATE_DATA, 0)) {
				log.info("{} 수정해야 합니다", Utility.indentMiddle());
				//	Update #1	/html/body/div[3]/div/div/div[2]/div/div[3]/h2/button
				By BY_XPATH_UPDATE_BUTTON = By.xpath("//div[contains(@class,'modal-body')]/div[contains(@class,'accordion')]/div[3]/h2/button");
				log.debug("{} upload(...) - 『{}』『{}』", Utility.indentMiddle(), "Update #1", driver.getText(BY_XPATH_UPDATE_BUTTON, Duration.ZERO));
				driver.presenceOfElementLocated(BY_XPATH_UPDATE_BUTTON, DEFAULT_TIMEOUT_DURATION);
				driver.clickIfExist(BY_XPATH_UPDATE_BUTTON);
				log.debug("{} upload(...) - 『{}』『{}』", Utility.indentMiddle(), "Update #1", driver.getText(BY_XPATH_UPDATE_BUTTON, Duration.ZERO));
				
				//	Select All And Do Batch	/html/body/div[3]/div/div/div[2]/div/div[3]/div/div/div[1]/div[1]/button[3]
				By BY_XPATH_UPDATE_ALL_BATCH_BUTTON = By.xpath("/html/body/div[3]/div/div/div[2]/div/div[3]/div/div/div[1]/div[1]/button[contains(text(),'Select All And Do Batch')]");
				log.debug("{} upload(...) - 『{}』『{}』", Utility.indentMiddle(), "Select All And Do Batch", driver.getText(BY_XPATH_UPDATE_ALL_BATCH_BUTTON, Duration.ZERO));
				driver.presenceOfElementLocated(BY_XPATH_UPDATE_ALL_BATCH_BUTTON, DEFAULT_TIMEOUT_DURATION);
				driver.clickIfExist(BY_XPATH_UPDATE_ALL_BATCH_BUTTON);
				log.debug("{} upload(...) - 『{}』『{}』", Utility.indentMiddle(), "Select All And Do Batch", driver.getText(BY_XPATH_UPDATE_ALL_BATCH_BUTTON, Duration.ZERO));
			} else {
				log.info("{} 수정할게 없습니다", Utility.indentMiddle());
			}

			log.info("{} 잠시 쉬어 갑니다", Utility.indentMiddle());
			Utility.sleep(1000 * 60);

			return 0;
		} catch (Exception e) {
			log.error("Exception:: {}", e.getLocalizedMessage(), e);
		}
		
		File file = new File(fullPath);
		String text = Utility.extractStringFromText(file);
		int count = text.length();

		log.info("{} #{} - upload({}) - 『{}』", Utility.indentEnd(), count, filename, Utility.ellipsisEscape(text, 32, 32));
		return count;
	}

	private void navigateUpload(ChromeDriverWrapper driver) {
		driver.manage().timeouts().implicitlyWait(DEFAULT_TIMEOUT_DURATION_LONG);
		driver.get(getUserUploadUrl());
		
		//	올리기	//*[@id="root"]/div/div[1]/div[3]/div/button[3]
		By BY_XPATH_UPLOAD_BUTTON = By.xpath("//*[@id='root']//div[contains(@class,'col-auto')]/div/button[contains(text(),'올리기')]");
		log.debug("{} navigateUpload(...) - 『{}』『{}』", Utility.indentMiddle(), "올리기", driver.getText(BY_XPATH_UPLOAD_BUTTON, Duration.ZERO));
		driver.presenceOfElementLocated(BY_XPATH_UPLOAD_BUTTON, DEFAULT_TIMEOUT_DURATION);
		driver.clickIfExist(BY_XPATH_UPLOAD_BUTTON);
		log.debug("{} navigateUpload(...) - 『{}』『{}』", Utility.indentMiddle(), "올리기", driver.getText(BY_XPATH_UPLOAD_BUTTON, Duration.ZERO));

		//	파일선택	/html/body/div[3]/div/div/div[2]/form/div[1]/div/input
		By BY_XPATH_FILE_SELECT = By.xpath("//form//input[contains(@type,'file')]");
		log.debug("{} navigateUpload(...) - 『{}』『{}』", Utility.indentMiddle(), "파일선택", driver.getText(BY_XPATH_FILE_SELECT, Duration.ZERO));
		driver.presenceOfElementLocated(BY_XPATH_FILE_SELECT, DEFAULT_TIMEOUT_DURATION);
		log.debug("{} navigateUpload(...) - 『{}』『{}』", Utility.indentMiddle(), "파일선택", driver.getText(BY_XPATH_FILE_SELECT, Duration.ZERO));
	}

	private boolean notUpload(ChromeDriverWrapper driver) {
		return true;
	}

	private String download() {
		log.info("{} download()", Utility.indentStart());
		long started = System.currentTimeMillis();

		try {
			if (notBackupNaver(driver)) {
				navigateBackupNaver(driver);
			}

			driver.manage().timeouts().implicitlyWait(DEFAULT_TIMEOUT_DURATION);
			
			//	내보내기(백업)	//*[@id="tabcontrol"]/ul/li[2]/a
			By BY_XPATH_TAB_EXPORT = By.xpath("//*[@id='tabcontrol']/ul/li/a[contains(text(),'내보내기')]");
			log.debug("{} download(...) - 『{}』『{}』", Utility.indentMiddle(), "내보내기(백업)", driver.getText(BY_XPATH_TAB_EXPORT, Duration.ZERO));
			driver.presenceOfElementLocated(BY_XPATH_TAB_EXPORT, DEFAULT_TIMEOUT_DURATION);
			driver.clickIfExist(BY_XPATH_TAB_EXPORT);
			log.debug("{} download(...) - 『{}』『{}』", Utility.indentMiddle(), "내보내기(백업)", driver.getText(BY_XPATH_TAB_EXPORT, Duration.ZERO));

			//	캘린더::선택활성화	//*[@id="export"]/div[1]/div[3]/div/div[1]/div
			By BY_XPATH_SELECT_BUTTON = By.xpath("//*[@id='export']//div[contains(@class,'selectbox-box')]/div[contains(@class,'selectbox-label')]");
			log.debug("{} download(...) - 『{}』『{}』", Utility.indentMiddle(), "내보내기(백업)", driver.getText(BY_XPATH_SELECT_BUTTON, Duration.ZERO));

			//	선택창		//*[@id="export"]/div[1]/div[3]/div/div[2]
			By BY_XPATH_SELECT_POPUP = By.xpath("//*[@id='export']//div[contains(@class,'_calendar_selectbox')]/div[contains(@class,'selectbox-layer')]");
			log.debug("{} download(...) - 『{}』『{}』", Utility.indentMiddle(), "선택창", driver.getText(BY_XPATH_SELECT_POPUP, Duration.ZERO));

			if (!driver.isDisplayed(BY_XPATH_SELECT_POPUP)) {
				log.debug("{} download(...) - 『{}』『{}』", Utility.indentMiddle(), "내보내기(백업)", driver.getText(BY_XPATH_SELECT_BUTTON, Duration.ZERO));
				driver.clickIfExist(BY_XPATH_SELECT_BUTTON);
				log.debug("{} download(...) - 『{}』『{}』", Utility.indentMiddle(), "내보내기(백업)", driver.getText(BY_XPATH_SELECT_BUTTON, Duration.ZERO));
			}

			//	개인 달력	//*[@id="export"]/div[1]/div[3]/div/div[2]/div/ul/li[4]
			By BY_XPATH_SELECT_PERSONAL = By.xpath("//*[@id='productIndex']//ul/li[contains(@class,'selectbox-item') AND contains(text(),'개인')]");
			log.debug("{} download(...) - 『{}』『{}』", Utility.indentMiddle(), "개인 달력", driver.getText(BY_XPATH_SELECT_PERSONAL, Duration.ZERO));
			driver.clickIfExist(BY_XPATH_SELECT_PERSONAL);
			log.debug("{} download(...) - 『{}』『{}』", Utility.indentMiddle(), "개인 달력", driver.getText(BY_XPATH_SELECT_PERSONAL, Duration.ZERO));

			//	전체 일정	//*[@id="export"]/div[1]/ul/li[1]/label	//*[@id="all_schedule"]
			By BY_XPATH_ALL_SCHEDULE = By.xpath("//*[@id='all_schedule']");
			log.debug("{} download(...) - 『{}』『{}』", Utility.indentMiddle(), "전체 일정", driver.getAttributeLast(BY_XPATH_ALL_SCHEDULE, "checked", 1, "NaN"));
			driver.clickIfExist(BY_XPATH_ALL_SCHEDULE);
			log.debug("{} download(...) - 『{}』『{}』", Utility.indentMiddle(), "전체 일정", driver.getAttributeLast(BY_XPATH_ALL_SCHEDULE, "checked", 1, "NaN"));
			
			Set<String> donwloadFiles = donwloadFiles(null);

			//	내보내기(백업)	//*[@id="footer"]/button[1]
			By BY_XPATH_BACKUP_BUTTON = By.xpath("//*[@id='footer']/button/strong[contains(text(),'내보내기(백업)')]/..");
			log.debug("{} download(...) - 『{}』『{}』", Utility.indentMiddle(), "내보내기(백업)", driver.getText(BY_XPATH_BACKUP_BUTTON, Duration.ZERO));
			driver.clickIfExist(BY_XPATH_BACKUP_BUTTON);
			log.debug("{} download(...) - 『{}』『{}』", Utility.indentMiddle(), "내보내기(백업)", driver.getText(BY_XPATH_BACKUP_BUTTON, Duration.ZERO));
			
			String filename = waitUntilDownloadComplete(donwloadFiles);
			
			//	취소	//*[@id="footer"]/button[3]
			By BY_XPATH_CANCEL = By.xpath("//*[@id='footer']/button[contains(@class,'_button_cancel')]");
			log.debug("{} download(...) - 『{}』『{}』", Utility.indentMiddle(), "취소", driver.getText(BY_XPATH_CANCEL, Duration.ZERO));
			driver.clickIfExist(BY_XPATH_CANCEL);
			log.debug("{} download(...) - 『{}』『{}』", Utility.indentMiddle(), "취소", driver.getText(BY_XPATH_CANCEL, Duration.ZERO));
			
			log.info("{} 『{}』 download() - {}", Utility.indentEnd(), filename, Utility.toStringPastTimeReadable(started));
			return filename;
		} catch (Exception e) {
			log.error("Exception:: {}", e.getLocalizedMessage(), e);
		}
		
		log.info("{} {} download() - {}", Utility.indentEnd(), "", Utility.toStringPastTimeReadable(started));
		return "";
	}

	private String waitUntilDownloadComplete(Set<String> donwloadFiles) {
		for (int cx = 0; cx < 32; cx++) {
			Set<String> neo  = donwloadFiles(donwloadFiles);
			
			for (String filename : neo) {
				if (filename.matches("Calendar_andold_[0-9\\-]+\\.ics")) {
					return filename;
				}
			}
			
			Utility.sleep(1000);
		}
		
		return "";
	}

	private Set<String> donwloadFiles(Set<String> setPrevious) {
		log.info("{} donwloadFiles({})", Utility.indentStart(), setPrevious);
		File fileLocation = new File(String.format("%s/Downloads", System.getProperty("user.home")));

		// Get the list of files in the directory
		File[] files = fileLocation.listFiles();
		if (setPrevious == null) {
			Set<String> set = new LinkedHashSet<>();
			for (File file : files) {
				set.add(file.getName());
			}

			log.info("{} {} - donwloadFiles({})", Utility.indentEnd(), set, setPrevious);
			return set;
		}

		Set<String> set = new LinkedHashSet<>();
		for (File file : files) {
			if (setPrevious.contains(file.getName())) {
				continue;
			}
			
			set.add(file.getName());
		}

		log.info("{} {} - donwloadFiles({})", Utility.indentEnd(), set, setPrevious);
		return set;
	}

	private void navigateBackupNaver(ChromeDriverWrapper driver) {
		driver.manage().timeouts().implicitlyWait(DEFAULT_TIMEOUT_DURATION_LONG);
		driver.get(DOWNLOAD_URL);
		
		//	개인	//*[@id="calendar_list_container"]/div[2]/ul/li[4]/a[2]/text()
		By BY_XPATH_FOOTER = By.xpath("//*[@id='footer']");
		driver.presenceOfElementLocated(BY_XPATH_FOOTER, DEFAULT_TIMEOUT_DURATION_LONG);
		driver.manage().timeouts().implicitlyWait(DEFAULT_TIMEOUT_DURATION);

		Set<String> windowHandles = driver.getWindowHandles();

		//	개인	//*[@id="calendar_list_container"]/div[2]/ul/li[4]/a[2]/text()
		By BY_XPATH_PERSONAL = By.xpath("//*[@id='calendar_list_container']//li[contains(@calendarid,'60828852')]/a[contains(@title,'개인')]");
		log.debug("{} navigateBackupNaver(...) - 『{}』『{}』", Utility.indentMiddle(), "개인", driver.getText(BY_XPATH_PERSONAL, Duration.ZERO));
		driver.presenceOfElementLocated(BY_XPATH_PERSONAL, DEFAULT_TIMEOUT_DURATION);
		driver.mouseHover(BY_XPATH_PERSONAL);
		log.debug("{} navigateBackupNaver(...) - 『{}』『{}』", Utility.indentMiddle(), "개인", driver.getText(BY_XPATH_PERSONAL, Duration.ZERO));

		//	메뉴 열기	//*[@id="calendar_list_container"]/div[2]/ul/li[4]/a[3]/span
		By BY_XPATH_OPEN_MENU = By.xpath("//*[@id='calendar_list_container']//li[contains(@calendarid,'60828852')]/a[contains(@class,'_open_menu show_menu')]");
		log.debug("{} navigateBackupNaver(...) - 『{}』『{}』", Utility.indentMiddle(), "메뉴 열기", driver.getText(BY_XPATH_OPEN_MENU, Duration.ZERO));
		driver.waitUntilIsDisplayed(BY_XPATH_OPEN_MENU, true, 1000 * 4);
		driver.clickIfExist(BY_XPATH_OPEN_MENU);
		log.debug("{} navigateBackupNaver(...) - 『{}』『{}』", Utility.indentMiddle(), "메뉴 열기", driver.getText(BY_XPATH_OPEN_MENU, Duration.ZERO));

		//	가져오기/내보내기(백업)	/html/body/div[5]/div/ul[1]/li[2]/a
		By BY_XPATH_BACKUP = By.xpath("//body/div//li[@class='_export_calendar']/a[contains(text(),'내보내기')]");
		log.debug("{} navigateBackupNaver(...) - 『{}』『{}』", Utility.indentMiddle(), "가져오기/내보내기(백업)", driver.getText(BY_XPATH_BACKUP, Duration.ZERO));
		driver.waitUntilIsDisplayed(BY_XPATH_BACKUP, true, 1000 * 4);
		driver.clickIfExist(BY_XPATH_BACKUP);
		log.debug("{} navigateBackupNaver(...) - 『{}』『{}』", Utility.indentMiddle(), "가져오기/내보내기(백업)", driver.getText(BY_XPATH_BACKUP, Duration.ZERO));

		//	팝업창
		String popup = neo(windowHandles, driver.getWindowHandles());
		driver.switchTo().window(popup);
		log.debug("{} navigateBackupNaver(...) - 『{}』『{}』", Utility.indentMiddle(), windowHandles, popup);
	}

	private String neo(Set<String> before, Set<String> now) {
		for (String handle : now) {
			if (before.contains(handle)) {
				continue;
			}
			
			return handle;
		}

		return "";
	}

	private boolean notBackupNaver(ChromeDriverWrapper driver2) {
		return true;
	}


}
