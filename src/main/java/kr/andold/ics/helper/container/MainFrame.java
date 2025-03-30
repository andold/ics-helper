/*
 * @(#)MainFrame.java 2024-06-27
 *
 * Copyright 2021 andold@naver.com All rights Reserved. 
 * andold@naver.com PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package kr.andold.ics.helper.container;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;

import org.openqa.selenium.Dimension;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebDriver.Window;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import kr.andold.utils.ChromeDriverWrapper;
import kr.andold.utils.Utility;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.Toolkit;

import javax.swing.JLayeredPane;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.border.BevelBorder;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

@Slf4j
@Component
public class MainFrame extends JFrame implements ActionListener {
	private static final long serialVersionUID = 1L;
	private static final int COLUMN_SIZE = 5;

	@Getter
	private ChromeDriverWrapper driverClient;
	@Getter
	private String windowHandleClient;
	@Getter
	private Point seleniumPostion;
	@Getter
	private Dimension seleniumSize;

	//	User Interfaces
	@Getter
	private JComboBox<Object> comboBoxGoStopAuto;	//	자동/수동/대기/무시
	@Getter
	private JCheckBox checkboxSkipRest;			//	나머지 작업
	@Getter
	private JCheckBox checkboxCloseAccount;		//	해지계좌
	@Getter
	private JToggleButton toggleButtonPlayPause;	//	Playing/Pause

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

	@Getter
	private static String logPath;
	@Value("${logging.file.path}")
	public void setLogPath(String value) {
		log.info("{} setLogPath(『{}』)", Utility.indentMiddle(), value);
		logPath = value;
	}

	public MainFrame() {
		getContentPane().setLayout(new GridLayout(1, 0, 0, 0));
		JLayeredPane layeredPane = new JLayeredPane();
		getContentPane().add(layeredPane);

		initBankButtons(layeredPane);
		createMenu();
	}

	private java.awt.Dimension sizeByScreen(int w, int h) {
		java.awt.Rectangle maximumWindowBounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
		return new java.awt.Dimension(((int)maximumWindowBounds.getWidth()) * w / 12, ((int)maximumWindowBounds.getHeight()) * h / 12);
	}

	private java.awt.Point locationByScreen(int w, int h) {
		java.awt.Rectangle maximumWindowBounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
		return new java.awt.Point((int)maximumWindowBounds.getX() + ((int)maximumWindowBounds.getWidth()) * w / 12, (int)maximumWindowBounds.getY() + ((int)maximumWindowBounds.getHeight()) * h / 12);

	}

	public Dimension sizeBySelenium(int w, int h) {
		return new Dimension(seleniumSize.getWidth() * w / 12, seleniumSize.getHeight() * h / 12);
	}

	public Point positionBySelenium(int w, int h) {
		return new Point(seleniumPostion.getX() + seleniumSize.getWidth() * w / 12, seleniumPostion.getY() + seleniumSize.getHeight() * h / 12);

	}

	private void initBrowsers() {
		log.info("{} initBrowsers() - 『{}』 『{}』 『{}』", Utility.indentMiddle(), webdriverPath, userDataDir, logPath);
		int marginBottom = 1;

		setSize(sizeByScreen(COLUMN_SIZE, 2));
		setLocation(locationByScreen(0, 0));
		System.setProperty("webdriver.chrome.driver", getWebdriverPath());

		ChromeOptions clientOptions = new ChromeOptions();
		clientOptions.addArguments("--remote-allow-origins=*");
		clientOptions.addArguments("--window-size=1024,768");
		clientOptions.addArguments(String.format("--user-data-dir=%s", String.format("%s", userDataDir)));
		clientOptions.addArguments("--disable-blink-features=AutomationControlled");
		clientOptions.addArguments("--disable-dev-shm-usage");
		clientOptions.addArguments("--disable-infobars");
		clientOptions.setPageLoadStrategy(PageLoadStrategy.NONE);
		driverClient = new ChromeDriverWrapper(clientOptions);
		windowHandleClient = driverClient.getWindowHandle();
		Window windowClient = driverClient.manage().window();

		windowClient.setSize(sizeBySelenium(12 - COLUMN_SIZE, 12 - marginBottom));
		windowClient.setPosition(positionBySelenium(COLUMN_SIZE, 0));
		driverClient.manage().timeouts().implicitlyWait(Duration.ofSeconds(4));

		log.info("{} initBrowsers()", Utility.indentMiddle());
	}

	public static List<java.awt.Component> getAllComponents(Container c) {
		java.awt.Component[] comps = c.getComponents();
	    List<java.awt.Component> compList = new ArrayList<>();
	    for (java.awt.Component comp : comps) {
	        compList.add(comp);
	        if (comp instanceof Container)
	            compList.addAll(getAllComponents((Container) comp));
	    }
	    return compList;
	}

	private void initBankButtons(JLayeredPane parent) {
		java.awt.Rectangle maximumWindowBounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
		int screenResolution = Toolkit.getDefaultToolkit().getScreenResolution();
		java.awt.Dimension size = new java.awt.Dimension(((int)maximumWindowBounds.getWidth() * 4 / 12), ((int)maximumWindowBounds.getHeight() * 3 / 12 - 32));
		log.info("{} initBankButtons() - {} {} {} {} {} {} {}", Utility.indentMiddle()
			, maximumWindowBounds, screenResolution, size
			, parent.getSize(), parent.getBounds()
			, parent.getVisibleRect(), this.getPreferredSize());

		JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		statusPanel.setBorder(new BevelBorder(BevelBorder.LOWERED));
		parent.setLayout(new BorderLayout());
		parent.add(statusPanel, BorderLayout.SOUTH);
 		statusPanel.setPreferredSize(new java.awt.Dimension(getWidth(), 32));
		statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.X_AXIS));
		statusPanel.setToolTipText(Double.toString(Math.random()));
		statusPanel.setBackground(Color.BLACK);
	}

	@PostConstruct
	public void init() {
		this.setTitle("ICS Helper");
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setLocationRelativeTo(null);
		this.setVisible(true);

		initBrowsers();
	}

	@PreDestroy
	public void freeResource() {
		driverClient.quit();
	}

	private void createMenu(JMenu menu, String title, boolean enabled) {
		JMenuItem jMenuItem = new JMenuItem(title);
		jMenuItem.setEnabled(enabled);
		jMenuItem.addActionListener(this);
		menu.add(jMenuItem);
	}

	private void createMenu() {
		JMenuBar menuBar = new JMenuBar();
		JMenu menuBank = new JMenu("항목");
		menuBar.add(menuBank);
		createMenu(menuBank, "항목 1", true);
		createMenu(menuBank, "항목 2", true);
		menuBank.addSeparator();
		createMenu(menuBank, "닫기", true);

		menuBar.add(new JMenu("도움말"));

		setJMenuBar(menuBar); // 메뉴바 설정
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();
		switch (command) { // 메뉴 아이템 구분
		case "기업은행":
			break;
		case "농협은행":
			break;
		case "닫기":
			System.exit(0); // 시스템 종료
			break;
		}
	}

}
