@echo off
where "java" >nul 2>nul
if %ERRORLEVEL%==0 (
   java -cp sample-image-solution.jar com.example.gdomo.SampleImageCardRecognizer %*
) else (
   "%JAVA_HOME%\bin\java" -cp sample-image-solution.jar com.example.gdomo.SampleImageCardRecognizer %*
)
