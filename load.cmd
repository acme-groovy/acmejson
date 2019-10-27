@echo off

set PARM=%1

FOR /L %%I IN (1,1,3) DO call :doone %%I

exit /B 0


:doone
rem echo "call %1"
ping localhost -n 1 > nul
call groovy -cp "%USERPROFILE%/.groovy/grapes/com.google.code.gson/gson/jars/gson-2.8.5.jar;./build/classes/java/main;./build/classes/groovy/test"  .\src\load\AcmeJsonLoadTest.groovy %PARM%
exit /B 0
