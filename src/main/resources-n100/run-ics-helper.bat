@SET PROJECT=ics-helper
@SET VERSION=0.0.1-SNAPSHOT
@SET PROFILE=n100
@SET SOURCE_PATH=C:\src\github\%PROJECT%
@SET CURRENT_PATH=%~dp0
@SET CURRENT_FILENAME=%~nx0
@SET LC_ALL=ko_KR.UTF-8

CHCP 65001

%JAVA_HOME%\bin\java -jar %CURRENT_PATH%\doc_base\%PROJECT%-%VERSION%.jar
