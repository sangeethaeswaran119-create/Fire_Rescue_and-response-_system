================================================================================
SMART FIRE AND RESCUE RESPONSE SYSTEM - COLLEGE PROJECT REVIEW GUIDE
Town of Sudar Emergency Operations Center (EOC)
Zero-Maven / Standard Java SE + HTML + CSS + JavaScript + MySQL JDBC
================================================================================

1. PROJECT STRUCTURE
--------------------------------------------------------------------------------
Smart_Fire_Rescue_No_Maven/
  ├── src/                       <- Java source files (OOP & Data Structures)
  │    ├── Config.java           <- MySQL connection settings & password entry
  │    ├── Incident.java         <- Domain model with urgency score calculation
  │    ├── ResponseUnit.java     <- Abstract base class for vehicles (HashSet eq)
  │    ├── FireEngine.java       <- Subclass overriding supportsRequirement()
  │    ├── RescueVan.java        <- Subclass overriding supportsRequirement()
  │    ├── FireStation.java      <- Composition of assigned response units
  │    ├── RoadNetwork.java      <- Weighted graph & Dijkstra Min-Heap algorithm
  │    ├── DispatchRecord.java   <- Mission audit & telemetry model
  │    ├── IncidentPriorityPolicy.java <- Urgency ranking strategy for PriorityQueue
  │    ├── DatabaseManager.java  <- Pure JDBC PreparedStatements & persistence
  │    ├── DispatchService.java  <- Core coordinator (PriorityQueue, Dijkstra)
  │    └── Main.java             <- JDK HttpServer (port 8080) & REST API handlers
  ├── web/                       <- Modern Emergency Dashboard (Frontend)
  │    ├── index.html            <- Responsive EOC Control Room dashboard
  │    ├── style.css             <- Dark control-room styling with telemetry
  │    └── app.js                <- Live telemetry, Dijkstra breadcrumbs, SVG map
  ├── lib/                       <- External Libraries
  │    └── mysql-connector-j.jar <- MySQL Connector/J driver (pre-installed!)
  ├── database.sql               <- MySQL database schema & 15 sample incidents
  └── README.txt                 <- This comprehensive guide


2. MYSQL DATABASE SETUP
--------------------------------------------------------------------------------
A. Check MySQL Service:
   - Your local MySQL 8.0 service is already running on port 3307!

B. Import Database Schema:
   - Open MySQL Workbench (or MySQL Command Line Client):
     Run the contents of 'database.sql'
     Or in terminal:
     mysql -P 3307 -u root -p < database.sql

C. Configure Credentials:
   - Open 'src/Config.java' in any text editor.
   - Look for line 21:
     public static final String PASSWORD = "YOUR_MYSQL_PASSWORD";
   - Replace "YOUR_MYSQL_PASSWORD" with your actual MySQL root password.
   - Save the file.

NOTE: If MySQL is not running or credentials are not yet entered, the application
will smoothly run in "In-Memory Mode" with complete functionality, while clearly
displaying the exact MySQL connection diagnostics on the dashboard and console.


3. HOW TO COMPILE AND RUN (WITHOUT MAVEN)
--------------------------------------------------------------------------------
Open terminal in the project folder:
  D:\My Documents\Downloads\Smart_Fire_Rescue_No_Maven

CRITICAL NOTE ON WINDOWS:
Even when using Git Bash, Windows Java (java.exe) requires a SEMICOLON ';'
as the classpath separator, NOT a colon ':'.
Colon ':' is only used on native Linux/Mac systems. On Windows, ':' causes:
"Error: Could not find or load main class Main".

EASIEST OPTION 1 - USING RUN SCRIPTS:
- In Git Bash:
    bash run.sh
- In Windows CMD / Double-Click:
    run.bat

OPTION 2 - RUNNING COMMANDS MANUALLY:

--- GIT BASH ---
1. Make sure you are inside the project folder:
   cd "/d/My Documents/Downloads/Smart_Fire_Rescue_No_Maven"

2. Compile:
   mkdir -p out
   javac -cp "lib/mysql-connector-j.jar" -d out src/*.java

3. Run (Notice the semicolon ';' inside quotes):
   java -cp "out;lib/mysql-connector-j.jar" Main

--- WINDOWS CMD / POWERSHELL ---
1. Move into the project folder:
   cd /d "D:\My Documents\Downloads\Smart_Fire_Rescue_No_Maven"

2. Compile:
   if not exist out mkdir out
   javac -cp "lib\mysql-connector-j.jar" -d out src\*.java

3. Run:
   java -cp "out;lib\mysql-connector-j.jar" Main


4. OPEN IN BROWSER
--------------------------------------------------------------------------------
Once the server displays "Server is running!", open your web browser at:
  http://localhost:8080


5. STEP-BY-STEP DEMO SEQUENCE FOR YOUR COLLEGE REVIEW TOMORROW
--------------------------------------------------------------------------------
Step 1: System Overview & Telemetry
  - Show the EOC dashboard at http://localhost:8080.
  - Point out the 6 summary metric cards: Total Incidents (15), Pending in Queue (15),
    Active Dispatches, Completed Missions, Available Units (4 / 4), Stations (2).
  - Point out the database status indicator in the top header.

Step 2: Java Data Structures Justification
  - PriorityQueue: Explain that incidents are prioritized by:
    Intensity (1-5) ➔ Critical Infrastructure (+25 bonus) ➔ Population Exposure (1-3) ➔ FIFO.
    Notice INC-101 (Hospital, Intensity 5, Critical=YES) is ranked at the very top!
  - Weighted Graph: Show the SVG map of Sudar town with 15 locations and positive road travel times.
  - HashSet: Explain that units store equipment capabilities in a HashSet for instant O(1) checks.

Step 3: Automated Dijkstra Dispatch
  - Click the large amber button: "⚡ DISPATCH HIGHEST-PRIORITY INCIDENT".
  - Watch the live telemetry card update immediately:
    * Assigned Unit: FE-01 (Fire Engine) from Station-A.
    * Target Incident: INC-101 (Hospital).
    * Calculated Route: Station-A ➔ Junction-1 ➔ Junction-2 ➔ Hospital.
    * Travel Time: Minimum travel time in minutes.
    * SVG Map: The exact shortest path turns glowing gold!
  - Look at the Response Units fleet panel: FE-01 status flips to BUSY.
  - Look at the Incident Queue table: Status updates to DISPATCHED.

Step 4: Register a New Incident
  - Fill the "Register Emergency Incident" form:
    * Location: "Market"
    * Incident Type: "Major Structural Fire"
    * Intensity: 5
    * Population: 3 (Dense Crowds)
    * Critical: Checked
    * Equipment: "FOAM"
  - Click "Register & Prioritize Incident".
  - Show the examiner that the new incident is enqueued and immediately ranks based on priority score.

Step 5: Resource Constraint & Equipment Compatibility Handling
  - Click "DISPATCH" again.
  - The system checks available units, verifies equipment support, and allocates FE-02.
  - When all units for a given equipment are busy, the system politely alerts:
    "All units supporting FOAM are currently BUSY. Incident remains PENDING."
  - This demonstrates robust emergency resource conflict handling.

Step 6: Mission Completion & Vehicle Release
  - On the active dispatched incident or in the fleet card, click "Complete Mission".
  - Show that:
    1. Incident status transitions to COMPLETED.
    2. Unit is released back to AVAILABLE.
    3. The mission is archived in the "Mission Dispatch History" table.

Step 7: Dynamic Road Closure Simulation (Case Study Feature)
  - Scroll down to "Road Closure Simulation" below the town map.
  - Select "Junction-1 ➔ Junction-2" and click "Toggle Road Closure".
  - The route turns red and dashed on the SVG map.
  - Trigger a dispatch to demonstrate that Dijkstra immediately recalculates an
    alternate bypass route (e.g., via Market or Highway) avoiding the closed road!

================================================================================
Good luck with your college project review!
================================================================================
