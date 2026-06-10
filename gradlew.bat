@echo off
setlocal

where gradle >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    gradle %*
    exit /b %ERRORLEVEL%
)

echo Java and Gradle not found in PATH. Install Java 17 and Gradle, or add gradle.exe to PATH.
exit /b 1

endlocal
