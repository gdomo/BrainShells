@echo off
where "java" >nul 2>nul
if %ERRORLEVEL%==0 (
   java -cp key-pixel-solution.jar com.example.gdomo.KeyPixelCardRecognizer %*
) else (
   "%JAVA_HOME%\bin\java" -cp key-pixel-solution.jar com.example.gdomo.KeyPixelCardRecognizer %*
)
