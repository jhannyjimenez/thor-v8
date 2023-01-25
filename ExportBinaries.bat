@echo off
echo STARTING TO EXPORT. DO NOT CLOSE. THE COMMAND PROMPT WILL CLOSE ITSELF WHEN ALL PROCESSES ARE FINISHED
echo ----------------------------------------
echo [43mStarting to create Jar[0m
Rem create the jar which contains THOR and all its dependencies
call gradle shadowJar
echo ----------------------------------------
echo [42mFinished Jar creation[0m
Rem read the version and get the name of the fat jar
set /p version=<version.txt
set file_name=THOR-%version%-all.jar
Rem create a bin and jars directory if there isn't one already
mkdir bin
mkdir bin\jars
Rem move the jar from its default destination to the jars folder
move /y build\libs\%file_name% bin\jars
Rem create a temporary folder and make a copy of the jar file into this folder
mkdir temporary
copy bin\jars\%filename% temporary
copy version.txt temporary
Rem create the THOR App Image
echo ----------------------------------------
echo [43mStarting to create app image[0m
call jpackage --type app-image --name THOR-winx64 --input temporary --main-jar %file_name% --app-version %version% --vendor "MITRE" --icon src\main\resources\icon.ico --verbose
Rem get the name of the installer and delete it if one already exist in the installers folder
echo ----------------------------------------
echo [42mFinished app image creation[0m
set installer_name=THOR-winx64-installer-%version%.msi
del installers\%installer_name%
echo ----------------------------------------
echo [43mStarting to create installer[0m
Rem create the THOR installer and put it in the installers folder
call jpackage --type msi --app-image THOR-winx64 --name THOR-winx64-installer --vendor "MITRE" --win-menu --win-per-user-install --win-shortcut --win-upgrade-uuid "10f6c0d6-cadb-11eb-b8bc-0242ac130003" --dest bin\installers --app-version %version% --verbose
echo ----------------------------------------
echo [42mFinished installer creation[0m
Rem all the folders and files that are no longer needed
rmdir /s /q temporary
rmdir /s /q THOR-winx64
echo ----------------------------------------
echo [42ALL OPERATIONS ARE FINISHED[0m
pause