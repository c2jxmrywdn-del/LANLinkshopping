@echo off
setlocal

set "ROOT=%~dp0"
set "BACKEND=%ROOT%backend"
set "FRONTEND=%ROOT%frontend"
set "SQLFILE=%BACKEND%\src\main\resources\sql\schema.sql"
set "MYSQL="

echo ==========================================
echo   LANLinkshopping - one click launcher
echo ==========================================
echo.

REM ---------- auto-detect mysql.exe ----------
for /f "delims=" %%i in ('where mysql 2^>nul') do if not defined MYSQL set "MYSQL=%%i"
for %%R in ("C:\Program Files\MySQL" "C:\Program Files (x86)\MySQL" "D:\Program Files\MySQL" "E:\Program Files\MySQL") do (
  for /d %%V in ("%%~R\MySQL Server *") do if not defined MYSQL if exist "%%V\bin\mysql.exe" set "MYSQL=%%V\bin\mysql.exe"
)
if not defined MYSQL for /d %%V in ("C:\Program Files\MySQL\*") do if exist "%%V\bin\mysql.exe" set "MYSQL=%%V\bin\mysql.exe"
if not defined MYSQL for /d %%V in ("C:\MySQL*" "D:\MySQL*" "E:\MySQL*") do if exist "%%V\bin\mysql.exe" set "MYSQL=%%V\bin\mysql.exe"

if not defined MYSQL goto :no_mysql
echo [i] Found mysql: %MYSQL%
if not defined DBPASS set /p "DBPASS=Enter MySQL root password: "
if not defined DBPASS goto :missing_dbpass

echo [0] Checking database ...
"%MYSQL%" -uroot --password="%DBPASS%" -e "CREATE DATABASE IF NOT EXISTS lanlink_shopping;" 1>nul 2>nul
"%MYSQL%" -uroot --password="%DBPASS%" lanlink_shopping -e "SELECT 1 FROM t_user LIMIT 1;" 1>nul 2>nul
if errorlevel 1 (
    echo     First run - creating tables and dictionary data ...
    "%MYSQL¶»§q«^