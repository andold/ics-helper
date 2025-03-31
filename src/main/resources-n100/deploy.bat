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

ECHO "clean"
CALL gradlew.bat clean -Pprofile=%PROFILE% -x test

ECHO "build"
CD %SOURCE_PATH%
git clean -f
CALL gradle.bat build -Pprofile=%PROFILE% -x test

POPD
CD  doc_base
@ECHO delete files
DEL /F /S /Q * > nul
@ECHO deploy new files
COPY /Y %SOURCE_PATH%\build\libs\%PROJECT%-%VERSION%.jar .

CD  ..
@ECHO copy this file from new
ECHO %CURRENT_FILENAME%
COPY /Y %SOURCE_PATH%\src\main\resources-%PROFILE%\install-%PROJECT%-%PROFILE%.bat
COPY /Y %SOURCE_PATH%\src\main\resources-%PROFILE%\run-%PROJECT%.bat
