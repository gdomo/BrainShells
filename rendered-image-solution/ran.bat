@echo off
where "java" >nul 2>nul
if %ERRORLEVEL%==0 (
   java -Djava.awt.headless=true -cp rendered-image-solution.jar com.example.gdomo.RenderedImageCardRecognizer %*
) else (
   "%JAVA_HOME%\bin\java" -Djava.awt.headless=true -cp rendered-image-solution.jar com.example.gdomo.RenderedImageCardRecognizer %*
)
