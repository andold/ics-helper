@SET PROJECT=ics-helper
@SET VERSION=0.0.1-SNAPSHOT

CHCP 65001

CALL gradlew.bat build -x test
%JAVA_HOME%\bin\java -jar .\build\libs\%PROJECT%-%VERSION%.jar
