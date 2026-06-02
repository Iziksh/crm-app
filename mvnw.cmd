@REM ----------------------------------------------------------------------------
@REM Licensed to the Apache Software Foundation (ASF) under one
@REM or more contributor license agreements.
@REM Maven Wrapper startup batch script
@REM ----------------------------------------------------------------------------

@IF "%__MVNW_ARG0_NAME__%"=="" (SET __MVNW_ARG0_NAME__=%~nx0)
@SET __MVNW_CMD__=
@SET __MVNW_ERROR__=
@SET __MVNW_SAVE_ERRORLEVEL__=
@SET __MVNW_SAVE_CD__=%CD%

@SETLOCAL

@SET DIRNAME=%~dp0
@IF "%DIRNAME%"=="" SET DIRNAME=.
@SET APP_BASE_NAME=%~n0
@SET APP_HOME=%DIRNAME%

@SET WRAPPER_JAR="%APP_HOME%\.mvn\wrapper\maven-wrapper.jar"
@SET WRAPPER_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain

@FOR /F "usebackq tokens=1,2 delims==" %%A IN ("%APP_HOME%\.mvn\wrapper\maven-wrapper.properties") DO (
    @IF "%%A"=="distributionUrl" SET DISTRIBUTION_URL=%%B
)

@SET JAVA_HOME_CANDIDATE=%JAVA_HOME%

@IF NOT "%JAVA_HOME%"=="" GOTO init

:findJavaFromJavaHome
@SET JAVA_HOME=%JAVA_HOME_CANDIDATE%

:init
@IF "%JAVA_HOME%"=="" (
    @ECHO Error: JAVA_HOME is not set.
    @EXIT /B 1
)

@SET JAVA_EXE=%JAVA_HOME%\bin\java.exe
@IF NOT EXIST "%JAVA_EXE%" (
    @ECHO Error: JAVA_HOME is set to an invalid directory: %JAVA_HOME%
    @EXIT /B 1
)

@IF EXIST "%WRAPPER_JAR%" GOTO wrapperExistsOk

:downloadWrapper
@ECHO Downloading Maven Wrapper...
@SET DOWNLOAD_URL=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar
"%JAVA_EXE%" -jar "%WRAPPER_JAR%" 2>NUL || (
    POWERSHELL -Command "Invoke-WebRequest -Uri '%DOWNLOAD_URL%' -OutFile '%WRAPPER_JAR%'"
)

:wrapperExistsOk
@"%JAVA_EXE%" -classpath "%WRAPPER_JAR%" %WRAPPER_LAUNCHER% %*
@EXIT /B %ERRORLEVEL%
