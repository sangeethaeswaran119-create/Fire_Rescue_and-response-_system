@echo off
title Smart Fire & Rescue - MySQL Database Console
cd /d "%~dp0"
echo ====================================================================
echo    SMART FIRE AND RESCUE RESPONSE SYSTEM - MYSQL 8.0 DATABASE
echo    Host: 127.0.0.1  ^|  Port: 3307  ^|  Database: smart_fire_rescue
echo ====================================================================
echo.
echo [1] Overview of Tables:
"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -P 3307 -u root -proot -e "USE smart_fire_rescue; SHOW TABLES;"
echo.
echo [2] Active Response Units:
"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -P 3307 -u root -proot -e "USE smart_fire_rescue; SELECT unit_code, unit_type, station_name, current_location, status FROM response_units;"
echo.
echo [3] Recent Mission Logs:
"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -P 3307 -u root -proot -e "USE smart_fire_rescue; SELECT id, incident_id, unit_code, start_location, target_location, travel_time_mins, status FROM mission_history ORDER BY id DESC LIMIT 5;"
echo.
echo ====================================================================
echo  Entering interactive MySQL CLI... (Type 'exit' to quit)
echo ====================================================================
"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -P 3307 -u root -proot smart_fire_rescue
