package kr.andold.ics.helper.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import kr.andold.utils.Utility;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootTest
public class CrawlNaverServiceTest {
	@Autowired private CrawlNaverService service;

	@BeforeEach
	public void before() {
		log.info(Utility.HR);
		assertNotNull(service);
	}

	@Test
	public void testCrawl() {
		service.crawlIcs();
	}

	@Test
	public void crawlCalendar() {
		service.crawlCalendar("공용");
	}

}
