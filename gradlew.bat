@rem Gradle startup script for Windows
@if "%DEBUG%"=="" @echo off
@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal
set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%
set GRADLE_OPTS=%GRADLE_OPTS% "-Dfile.encoding=UTF-8"
set DEFAULT_JVM_OPTS="-Xmx64m" "-Xms64m"
set CLASSPATH=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar
if not defined JAVA_HOME (
    if exist "C:\Users\saeed\.jdks\jbr-17.0.14" (
        set "JAVA_HOME=C:\Users\saeed\.jdks\jbr-17.0.14"
    ) else if exist "C:\Program Files\JetBrains\IntelliJ IDEA 2026.2\jbr" (
        set "JAVA_HOME=C:\Program Files\JetBrains\IntelliJ IDEA 2026.2\jbr"
    )
)

@rem Execute Gradle
"%JAVA_HOME%\bin\java.exe" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %GRADLE_OPTS% -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*

