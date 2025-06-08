package kr.andold.ics.helper.service;

import java.io.File;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.Executors;

import org.openqa.selenium.By;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import kr.andold.ics.helper.ApplicationContextProvider;
import kr.andold.utils.Utility;
import kr.andold.utils.job.STATUS;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class CrawlGoogleService {
	private static final String CONTACT_URL = "https://contacts.google.com/";
	private static final Duration DEFAULT_TIMEOUT_DURATION = Duration.ofSeconds(8);
	private static final Duration DEFAULT_TIMEOUT_DURATION_LONG = Duration.ofMinutes(1);

	@Autowired private ChromeDriverServer server;
	@Autowired private ChromeDriverClient client;

	@Getter
	private static String userContactUploadUrl;
	@Value("${user.contact.upload.url}")
	public void setUserContactUploadUrl(String value) {
		log.info("{} setUserContactUploadUrl(『{}』)", Utility.indentMiddle(), value);
		userContactUploadUrl = value;
	}

	public void crawlContact() {
		Executors.newSingleThreadExecutor().execute(new Runnable() {
			@Override
			public void run() {
				log.info("{} crawlContact()", Utility.indentStart());
				long started = System.currentTimeMillis();

				CrawlGoogleService that = (CrawlGoogleService) ApplicationContextProvider.getBean(CrawlGoogleService.class);
				that.main();

				log.info("{} crawlContact() - {}", Utility.indentEnd(), -1, Utility.toStringPastTimeReadable(started));
				return;
			}
		});
	}

	protected STATUS main() {
		log.info("{} main()", Utility.indentStart());

		String filename = download();
		if (filename == null || filename.isBlank()) {
			log.info("{} 『{}』 main()", Utility.indentEnd(), STATUS.ALEADY_DONE);
			return STATUS.ALEADY_DONE;
		}

		upload(filename);

		log.info("{} 『{}』 main()", Utility.indentEnd(), STATUS.ALEADY_DONE);
		return STATUS.ALEADY_DONE;
	}

	private int upload(String filename) {
		log.info("{} upload(『{}』)", Utility.indentStart(), filename);

		ChromeDriverWrapper driver = server.getDriver();
		driver.manage().timeouts().implicitlyWait(DEFAULT_TIMEOUT_DURATION_LONG);
		String fullPath = String.format("%s/Downloads/%s", System.getProperty("user.home"), filename);
		log.info("{} upload(『{}』) - 『{}』", Utility.indentMiddle(), filename, fullPath);
		try {
			Set<String> windowHandles = driver.getWindowHandles();
			String windowHandle = (String) windowHandles.toArray()[0];
			driver.switchTo().window(windowHandle);

			navigateUpload(driver);

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
			
			//	올린 파일 분석중
			waitUntilParsingDone(driver);

			//	No Create Data!	/html/body/div[3]/div/div/div[2]/div/div[1]
			//	Create #4		/html/body/div[3]/div/div/div[2]/div/div[1]/h2/button
			By BY_XPATH_CREATE_DATA = By.xpath("//div[contains(@class,'modal-body')]/div[contains(@class,'accordion')]/div[1]");
			By BY_XPATH_NO_CREATE_DATA = By.xpath("//div[contains(@class,'modal-body')]/div[contains(@class,'accordion')]/div[contains(text(),'No Create Data!')]");
			log.debug("{} upload(...) - 『{}』『{}』", Utility.indentMiddle(), "No Create Data!", driver.getText(BY_XPATH_CREATE_DATA, Duration.ZERO));
			driver.waitUntilTextMatch(BY_XPATH_CREATE_DATA, "((No Create Data!)|(Create #[0-9]+))");
			log.debug("{} upload(...) - 『{}』『{}』", Utility.indentMiddle(), "No Create Data!", driver.getText(BY_XPATH_CREATE_DATA, Duration.ZERO));

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
			Utility.sleep(1000 * 2);

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

	private void waitUntilParsingDone(ChromeDriverWrapper driver) {
		By BY_XPATH_NO_CREATE_DATA = By.xpath("//div[contains(@class,'modal-body')]/div[contains(@class,'accordion')]/div[contains(text(),'No Create Data!')]");
		By BY_XPATH_NO_REMOVE_DATA = By.xpath("//div[contains(@class,'modal-body')]/div[contains(@class,'accordion')]/div[contains(text(),'No Remove Data!')]");
		By BY_XPATH_NO_UPDATE_DATA = By.xpath("//div[contains(@class,'modal-body')]/div[contains(@class,'accordion')]/div[contains(text(),'No Update Data!')]");
		By BY_XPATH_NO_IDENTICAL_DATA = By.xpath("//div[contains(@class,'modal-body')]/div[contains(@class,'accordion')]/div[contains(text(),'No Identical Data!')]");
		for (int cx = 0; cx < 60; cx++) {
			Utility.sleep(1000);
			if (!driver.isDisplayed(BY_XPATH_NO_CREATE_DATA, Duration.ZERO)) {
				log.debug("{} waitUntilParsingDone(...) - 『{}』『{}』", Utility.indentMiddle(), cx, driver.getText(BY_XPATH_NO_CREATE_DATA, Duration.ZERO));
				break;
			}
			if (!driver.isDisplayed(BY_XPATH_NO_REMOVE_DATA, Duration.ZERO)) {
				log.debug("{} waitUntilParsingDone(...) - 『{}』『{}』", Utility.indentMiddle(), cx, driver.getText(BY_XPATH_NO_REMOVE_DATA, Duration.ZERO));
				break;
			}
			if (!driver.isDisplayed(BY_XPATH_NO_UPDATE_DATA, Duration.ZERO)) {
				log.debug("{} waitUntilParsingDone(...) - 『{}』『{}』", Utility.indentMiddle(), cx, driver.getText(BY_XPATH_NO_UPDATE_DATA, Duration.ZERO));
				break;
			}
			if (!driver.isDisplayed(BY_XPATH_NO_IDENTICAL_DATA, Duration.ZERO)) {
				log.debug("{} waitUntilParsingDone(...) - 『{}』『{}』", Utility.indentMiddle(), cx, driver.getText(BY_XPATH_NO_IDENTICAL_DATA, Duration.ZERO));
				break;
			}
			log.debug("{} waitUntilParsingDone(...) - 『{}』", Utility.indentMiddle(), cx);
		}
	}

	private void navigateUpload(ChromeDriverWrapper driver) {
		driver.manage().timeouts().implicitlyWait(DEFAULT_TIMEOUT_DURATION_LONG);
		driver.get(getUserContactUploadUrl());
		
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

	private String download() {
		log.info("{} download()", Utility.indentStart());

		ChromeDriverWrapper driver = client.getDriver();
		driver.manage().timeouts().implicitlyWait(DEFAULT_TIMEOUT_DURATION_LONG);
		driver.get(CONTACT_URL);
		driver.manage().timeouts().implicitlyWait(DEFAULT_TIMEOUT_DURATION);
		driver.switchTo().defaultContent();

		// 내보내기	//*[@id="yDmH0d"]/c-wiz[2]/div/div[1]/div[2]/div[3]/div/div[1]/div[1]/div[2]/div/div[6]/span[2]/button/div
		By BY_XPATH_DOWNLOAD = By.xpath("//*[@id='yDmH0d']//button[contains(@aria-label,'내보내기')]");
		log.debug("{} download() - 『{}』 『{}』", Utility.indentMiddle(), "내보내기", driver.getText(BY_XPATH_DOWNLOAD, Duration.ZERO));
		driver.elementToBeClickable(BY_XPATH_DOWNLOAD);
		driver.clickIfExist(BY_XPATH_DOWNLOAD);
		log.debug("{} download() - 『{}』 『{}』", Utility.indentMiddle(), "내보내기", driver.getText(BY_XPATH_DOWNLOAD, Duration.ZERO));

		// Android 또는 iOS용 vCard	//*[@id="yDmH0d"]/div[4]/div[2]/div/div[1]/div/div[3]/div[2]/div[3]/label
		By BY_XPATH_VCF = By.xpath("//*[@id='yDmH0d']//label[contains(text(),'vCard')]");
		log.debug("{} download() - 『{}』 『{}』", Utility.indentMiddle(), "vCard", driver.getText(BY_XPATH_VCF, Duration.ZERO));
		driver.elementToBeClickable(BY_XPATH_VCF);
		driver.clickIfExist(BY_XPATH_VCF);
		log.debug("{} download() - 『{}』 『{}』", Utility.indentMiddle(), "vCard", driver.getText(BY_XPATH_VCF, Duration.ZERO));
		
		Set<String> donwloadFiles = donwloadFiles(null);
		log.debug("{} download() - 『{}』 『{}』", Utility.indentMiddle(), "donwloadFiles", donwloadFiles);

		// 내보내기	//*[@id="yDmH0d"]/div[4]/div[2]/div/div[2]/div[2]/button/span[5]
		By BY_XPATH_POPUP_DOWNLOAD = By.xpath("//*[@id='yDmH0d']//button/span[contains(text(),'내보내기')]");
		log.debug("{} download() - 『{}』 『{}』", Utility.indentMiddle(), "내보내기", driver.getText(BY_XPATH_POPUP_DOWNLOAD, Duration.ZERO));
		driver.elementToBeClickable(BY_XPATH_POPUP_DOWNLOAD);
		driver.clickIfExist(BY_XPATH_POPUP_DOWNLOAD);
		log.debug("{} download() - 『{}』 『{}』", Utility.indentMiddle(), "내보내기", driver.getText(BY_XPATH_POPUP_DOWNLOAD, Duration.ZERO));
		
		log.debug("{} download() - 『{}』 『{}』", Utility.indentMiddle(), "current window", driver.getWindowHandle());
		String filename = waitUntilDownloadComplete(donwloadFiles);
		log.debug("{} download() - 『{}』 『{}』", Utility.indentMiddle(), "filename", filename);

		log.info("{} 『{}』 download()", Utility.indentEnd(), filename);
		return filename;
	}

	private Set<String> donwloadFiles(Set<String> setPrevious) {
		log.trace("{} donwloadFiles({})", Utility.indentStart(), setPrevious);
		File fileLocation = new File(String.format("%s/Downloads", System.getProperty("user.home")));

		// Get the list of files in the directory
		File[] files = fileLocation.listFiles();
		if (setPrevious == null) {
			Set<String> set = new LinkedHashSet<>();
			for (File file : files) {
				set.add(file.getName());
			}

			log.trace("{} {} - donwloadFiles({})", Utility.indentEnd(), set, setPrevious);
			return set;
		}

		Set<String> set = new LinkedHashSet<>();
		for (File file : files) {
			if (setPrevious.contains(file.getName())) {
				continue;
			}
			
			set.add(file.getName());
		}

		log.trace("{} {} - donwloadFiles({})", Utility.indentEnd(), set, setPrevious);
		return set;
	}

	private String waitUntilDownloadComplete(Set<String> donwloadFiles) {
		log.info("{} waitUntilDownloadComplete(『{}』)", Utility.indentStart(), donwloadFiles);
		for (int cx = 0; cx < 32; cx++) {
			Set<String> neo  = donwloadFiles(donwloadFiles);
			
			for (String filename : neo) {
				if (filename.matches("contacts( \\([0-9]+\\))?\\.vcf")) {
					log.info("{} 『{}』 waitUntilDownloadComplete(『{}』)", Utility.indentEnd(), filename, donwloadFiles);
					return filename;
				}
			}
			
			Utility.sleep(1000);
		}
		
		log.info("{} 『{}』 waitUntilDownloadComplete(『{}』)", Utility.indentEnd(), "", donwloadFiles);
		return "";
	}

}
