@SET PROJECT=ics-helper
@SET VERSION=0.0.1-SNAPSHOT
@SET PROFILE=n100
@SET SOURCE_PATH=C:\src\github\%PROJECT%
@SET CURRENT_PATH=%~dp0
@SET CURRENT_FILENAME=%~nx0
@SET LC_ALL=ko_KR.UTF-8

ECHO %CURRENT_PATH% %CURRENT_FILENAME%

DATE /t
TIME /t

PUSHD %CURRENT_PATH%

CD  %SOURCE_PATH%
git pull
git  log --pretty=format:"%%h - %%an, %%ai:%%ar : %%s" -8

POPD

@ECHO copy deploy.bat new
COPY /Y %SOURCE_PATH%\src\main\resources-%PROFILE%\deploy.bat

CALL deploy.bat
