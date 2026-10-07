# CLAUDE.md

이 파일은 이 저장소에서 작업하는 Claude Code(claude.ai/code)를 위한 안내서입니다.

## 언어 규칙 (항상 적용)

1. 사용자에게 하는 모든 응답은 한국어로 작성한다.
2. 새로 생성하는 모든 문서는 한국어로 작성한다. (코드 식별자와 기술 용어는 원문 그대로 둔다.)

## 프로젝트 개요

Windows용 데스크톱 도우미(Spring Boot 3 + Swing, Java 21)로, Selenium으로 실제 Chrome 창을 조작해 개인 데이터를 사용자 소유의 웹앱으로 동기화한다.

- **일정: 네이버** — 네이버 캘린더(`개인`, `집안일`, `공용`)를 `.ics`로 내보낸 뒤, 각 파일을 `user.upload.url`의 ICS 웹앱에 업로드하고 중복제거 흐름을 실행한다.
- **연락처: 구글** — 구글 연락처를 vCard로 내보낸 뒤, `user.contact.upload.url`의 연락처 웹앱에 업로드한다.

API 연동은 없다. 모든 동작이 XPath 셀렉터(상당수가 한글 버튼 텍스트를 포함)를 이용한 브라우저 자동화이므로, 네이버/구글이나 업로드 대상 페이지의 UI가 바뀌면 흐름이 깨진다.

## 빌드 / 실행 / 테스트

Gradle 빌드 시 GitHub Packages(`maven.pkg.github.com/andold/utils`)에서 `kr.andold:utils`를 받기 위해 `mavenUser` / `mavenPassword` 속성(예: `~/.gradle/gradle.properties`)이 필요하다.

```sh
./gradlew build -x test                      # 빌드 (로컬/테스트 리소스 사용)
./gradlew build -Pprofile=windows -x test    # src/main/resources-windows 리소스로 빌드
java -jar build/libs/ics-helper-0.0.1-SNAPSHOT.jar   # 또는 run.bat / run.sh (빌드 + 실행)
./gradlew test --tests kr.andold.ics.helper.service.CrawlNaverServiceTest.crawlCalendar
```

- `-Pprofile=<이름>`을 주면 리소스 경로가 `src/main/resources` + `src/main/resources-<이름>`으로 바뀐다. 주지 않으면 `src/test/resources` + `src/test/resources-local`을 사용한다(`localhost` 테스트 엔드포인트, 로그/데이터는 `C:/logs/test-ics-helper`, `C:/data/test-ics-helper`). `src/test/resources-n100`은 다른 머신용 설정이다.
- 테스트는 `@SpringBootTest` 통합 실행이다. 실제 Chrome 창을 띄우고 실제 네이버/업로드 사이트에 접속하다. `chromedriver`는 Selenium Manager가 설치된 Chrome 버전에 맞춰 자동으로 받는다(첫 실행 시 인터넷 필요, `~/.cache/selenium`에 캐시). `user.selenium.webdriver.chrome.driver`를 지정하면 그 경로를 강제로 쓰므로, Chrome 자동 업데이트 후 버전 불일치(issue #7)가 다시 생길 수 있다. 단위 테스트가 아니며 일반 빌드에서는 건너뛴다.
- 배포: `src/main/resources-windows/install-ics-helper-windows.bat`이 `C:\src\github\ics-helper`를 pull한 뒤 `deploy-ics-helper-windows.bat`을 호출하고, 이 스크립트가 `-Pprofile=windows`로 빌드해 jar를 `doc_base`에 복사한다. 실행은 `run-ics-helper.bat`으로 한다.

## 아키텍처

- `Application`은 Spring을 non-headless로 시작하고 `MainFrame`(Swing `JFrame` `@Component`)을 가져온다. 타임존은 `Asia/Seoul`로 고정된다. `MainFrame.actionPerformed`의 메뉴 항목이 크롤 서비스로 분기하며, 서비스는 UI가 멈추지 않도록 새 single-thread executor에서 작업을 실행한다.
- **Chrome 인스턴스는 시작 시 생성되는 Spring 싱글톤이다** (`@PostConstruct`에서 생성, `@PreDestroy`에서 quit). 따라서 앱을 실행하면 즉시 여러 개의 Chrome 창이 열린다.
  - `CrawlNaverService`는 자체 드라이버(`--user-data-dir=<user.selenium.user.data.dir>`)를 가지며 다운로드와 업로드 모두에 사용한다.
  - `CrawlGoogleService`는 `ChromeDriverClient`(다운로드, 프로필 디렉터리 접미사 `-client`)와 `ChromeDriverServer`(업로드, 접미사 `-server`)를 사용한다.
  - 영속 프로필 디렉터리에 네이버/구글 로그인 상태가 유지된다. 최초 1회는 해당 창에서 사용자가 직접 로그인한다.
- 각 브라우저의 창 크기/위치는 `MainFrame.sizeByScreen` / `locationByScreen`으로 화면 비율(12등분 그리드)에 따라 계산한다.
- 다운로드 완료는 내보내기 전후의 `~/Downloads` 목록을 비교해 감지한다(예: 네이버 파일은 `Calendar_andold_[0-9-]+.ics` 패턴). 업로드는 해당 파일 경로를 대상 페이지의 file input에 넣은 뒤, 결과 패널(`No Create Data!`, `No Update Data!`, `Remove #N` 등)을 폴링하고 "Select All And Do Batch"를 클릭한다.
- `ChromeDriverWrapper`는 외부 라이브러리 `kr.andold.utils.ChromeDriverWrapper`의 얇은 서브클래스로, `getText`, `clickIfExist`, `presenceOfElementLocated`, `waitUntilExist`, `waitUntilTextMatch` 같은 헬퍼를 제공받는다. 같은 라이브러리의 `Utility.indentStart/Middle/End`가 메서드 진입/중간/종료 로그 규칙이다.
- 사용자 정의 설정 키(`user.*`)는 `src/main/resources/META-INF/additional-spring-configuration-metadata.json`에 정의되어 있으며, 서비스는 static 필드에 setter `@Value`로 주입받는다.
