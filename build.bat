@echo off
setlocal
where mvn >nul 2>nul
if errorlevel 1 (
  echo [HookForge] Khong tim thay Maven. Hay cai Maven 3.9+ va them vao PATH.
  exit /b 1
)
where java >nul 2>nul
if errorlevel 1 (
  echo [HookForge] Khong tim thay Java. Hay cai JDK 21+ va them vao PATH.
  exit /b 1
)
echo [HookForge] Building Maven reactor...
mvn clean package
if errorlevel 1 exit /b %errorlevel%
echo.
echo Build thanh cong. JAR: hookforge-paper	arget\HookForge-0.1.0.jar
endlocal
