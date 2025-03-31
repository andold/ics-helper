package kr.andold.ics.helper.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import kr.andold.utils.Utility;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootTest
@ContextConfiguration(
		loader = AnnotationConfigContextLoader.class
)
@PropertySource("classpath:application.properties")
public class ChromeServiceTest {
	@Autowired private ChromeService service;

	@BeforeEach
	public void before() {
		log.info(Utility.HR);
	}

	@Test
	public void testCrawl() {
		assertNotNull(service);
		service.crawl();
	}

}
