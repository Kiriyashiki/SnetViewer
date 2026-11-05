@echo off
setlocal enabledelayedexpansion

cd /d "%~dp0"

if not exist "build\libs\snet-viewer.jar" (
    echo Building SnetViewer...
    if not exist "build\classes" mkdir build\classes
    if not exist "build\libs" mkdir build\libs
    
    javac -d build\classes src\main\java\xyz\hainaut\snetviewer\model\*.java src\main\java\xyz\hainaut\snetviewer\service\*.java src\main\java\xyz\hainaut\snetviewer\ui\*.java src\main\java\xyz\hainaut\snetviewer\App.java
    if errorlevel 1 (
        echo Compilation failed.
        exit /b 1
    )
    
    if exist "src\main\resources" xcopy /E /I /Y src\main\resources build\classes >nul
    
    (
        echo Manifest-Version: 1.0
        echo Main-Class: xyz.hainaut.snetviewer.App
    ) > build\manifest.txt
    
    jar cfm build\libs\snet-viewer.jar build\manifest.txt -C build\classes .
)

echo Launching SnetViewer...
java -jar build\libs\snet-viewer.jar %*
