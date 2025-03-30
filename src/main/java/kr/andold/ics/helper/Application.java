package kr.andold.ics.helper;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableAsync;

import kr.andold.ics.helper.container.MainFrame;

@EnableAsync
@SpringBootApplication
public class Application {
	public static void main(String[] args) {
		ConfigurableApplicationContext ctx = new SpringApplicationBuilder(Application.class).headless(false).run(args);
		ctx.getBean(MainFrame.class);
    }

}
