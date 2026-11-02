@echo off
color 0A
title Sudar Town Emergency Operations - Live MySQL Database View
echo ===============================================================================
echo            SMART FIRE AND RESCUE RESPONSE SYSTEM - LIVE MYSQL DATABASE
echo                     Database: smart_fire_rescue  ^|  Port: 3307
echo ===============================================================================
echo.
echo [1] TABLE LIST:
"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -h 127.0.0.1 -P 3307 -u root -proot smart_fire_rescue -t -e "SHOW TABLES;"

echo.
echo [2] RECENT INCIDENTS (incidents table):
"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -h 127.0.0.1 -P 3307 -u root -proot smart_fire_rescue -t -e "SELECT id, incident_code, location_name, incident_type, intensity, critical, status FROM incidents ORDER BY id DESC LIMIT 5;"

echo.
echo [3] RESPONSE UNITS FLEET (response_units table):
"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -h 127.0.0.1 -P 3307 -u root -proot smart_fire_rescue -t -e "SELECT unit_code, station_name, current_location, status, equipment FROM response_units;"

echo.
echo [4] AUDIT TELEMETRY (mission_history table):
"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -h 127.0.0.1 -P 3307 -u root -proot smart_fire_rescue -t -e "SELECT id, incident_id, unit_code, start_location, target_location, route_text, travel_time_mins, status FROM mission_history ORDER BY id DESC LIMIT 5;"

echo.
echo ===============================================================================
echo Interactive MySQL prompt ready. Type SQL queries or type exit to quit.
echo ===============================================================================
"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -h 127.0.0.1 -P 3307 -u root -proot smart_fire_rescue
pause
