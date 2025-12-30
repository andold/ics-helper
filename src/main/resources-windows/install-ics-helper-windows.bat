@SET PROJECT=ics-helper
@SET VERSION=0.0.1-SNAPSHOT
@SET PROFILE=windows
@SET SOURCE_PATH=C:\src\github\%PROJECT%
@SET CURRENT_PATH=%~dp0
@SET CURRENT_FILENAME=%~nx0
@SET LC_ALL=ko_KR.UTF-8
@REM
@REM
@REM
ECHO %CURRENT_PATH% %CURRENT_FILENAME%
@REM
DATE /t
TIME /t
@REM
@REM
@REM
@REM
CD  %SOURCE_PATH%
git pull
git  log --pretty=format:"%%h - %%an, %%ai:%%ar : %%s" -8
@REM
@REM
@REM
@REM
CD  %CURRENT_PATH%
@ECHO copy deploy-%PROJECT%-%PROFILE%.bat
COPY /Y %SOURCE_PATH%\src\main\resources-%PROFILE%\deploy-%PROJECT%-%PROFILE%.bat
@REM
@REM
@REM
@REM
CALL deploy-%PROJECT%-%PROFILE%.bat
