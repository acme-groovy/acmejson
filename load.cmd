@echo off


FOR /L %%I IN (1,1,10) DO call :doone %%I

exit /B 0


:doone
rem echo "call %1"
ping localhost -n 1 > nul
call groovy -cp ./build/classes/java/main .\src\test\groovy\groovyx\acme\json\AcmeJsonLoadTest.groovy
exit /B 0
