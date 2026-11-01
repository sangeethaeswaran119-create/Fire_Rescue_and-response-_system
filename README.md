# Smart Fire and Rescue Response System (Sudar Town EOC)

A high-performance, real-time emergency dispatch and response system built with **pure standard Java SE** (JDK built-in `HttpServer`), **native HTML/CSS/JavaScript**, and **MySQL JDBC**.

Designed with **zero third-party frameworks** (no Spring Boot, no Maven, no external npm packages) to showcase core **Data Structures and Algorithms (DSA)** in a production-ready emergency control-room architecture.

---

## Key Features

1. **Prioritized Incident Queue (Max-Heap):**
   - Automatically orders pending emergencies using `PriorityQueue<Incident>` with multi-factor heuristic scoring based on physical caller observations:
     $$\text{Priority Score} = (\text{Intensity} \times 10) + (\text{Critical} ? 25 : 0) + (\text{Population} \times 5)$$
   - Deterministic FIFO tie-breaking using an `AtomicInteger` arrival sequence.
   - **Emergency Surge Stress Test:** 1-click simulation demonstrating live $O(k \log N)$ heap bubbling when multiple high-severity calls arrive concurrently.

2. **Shortest-Path Dispatch Engine (Dijkstra's Algorithm):**
   - Computes the minimum-travel-time route from the nearest eligible response unit to the incident location in $O((V + E) \log V)$ time using a Min-Heap.
   - Dynamic road closure sandbox allowing instant blockage of road segments and live detour recalculation without hardcoded paths.

3. **Live Vehicle Navigation & Animated Map (SVG):**
   - Renders an interactive topology map of Sudar Town's 16 sectors and 22 road connections.
   - Animates dispatched fire engines and rescue vans along the exact road waypoints in real time at 60 FPS.
   - Interactive Click-to-Report: Clicking any node directly on the town map initiates an incident report for that sector.

4. **Live Mission Countdown & Transit Telemetry:**
   - Real-time ticking ETA timer and dynamic progress bar (`0%` to `100%`) tracking units in transit.
   - Visual transition: `PENDING` $\rightarrow$ `DISPATCHED (EN ROUTE)` $\rightarrow$ `ARRIVED ON SCENE` $\rightarrow$ `COMPLETED`.

5. **Observation-Driven Caller Reporting:**
   - No confusing technical dropdowns (e.g., "Building Type" or "Incident Type").
   - Classifies incidents and allocates equipment (`FOAM`, `LADDER`, `CUTTER`, `HOSE`) directly from physical observations (smoke density, trapped persons, critical facilities).

6. **Dual Persistence Architecture:**
   - Persistent MySQL 8.0 JDBC storage using prepared statements for stations, fleet units, incidents, and audit trails.
   - Graceful thread-safe in-memory fallback if the MySQL database is offline.

---

## Data Structures & Algorithms (DSA) Summary

| Component | Data Structure / Algorithm | Time Complexity | Purpose |
| :--- | :--- | :--- | :--- |
| **Emergency Priority Queue** | `PriorityQueue<Incident>` (Max-Heap) | $O(\log N)$ enqueue / poll | Urgency prioritization |
| **Route Planner** | Dijkstra's Algorithm + Min-Heap | $O((V + E) \log V)$ | Shortest response route |
| **Town Road Network** | `Map<String, Map<String, Integer>>` (Adjacency List) | $O(1)$ edge access | Sparse graph representation |
| **Fleet Capability Matching** | `HashSet<String>` | $O(1)$ set membership | Equipment compatibility |
| **Primary-Key Incident Index** | `LinkedHashMap<Integer, Incident>` | $O(1)$ lookup / mutation | Direct record access |
| **Path Reconstruction** | `LinkedList<String>` (`addFirst`) | $O(P)$ | Backwards path traversal |
| **Tie-Breaking Monotonic Clock** | `AtomicInteger` | $O(1)$ lock-free | Fair FIFO order |

---

## Project Structure

```text
Smart_Fire_Rescue_No_Maven/
  ├── src/                       <- Pure Java SE Source Files
  │    ├── Config.java           <- MySQL connection settings & port config
  │    ├── Incident.java         <- Incident domain model with heuristic priority
  │    ├── ResponseUnit.java     <- Abstract base class for vehicles (HashSet eq)
  │    ├── FireEngine.java       <- Subclass overriding supportsRequirement()
  │    ├── RescueVan.java        <- Subclass overriding supportsRequirement()
  │    ├── FireStation.java      <- Composition of response units
  │    ├── RoadNetwork.java      <- Weighted graph & Dijkstra Min-Heap algorithm
  │    ├── DispatchRecord.java   <- Mission telemetry & history model
  │    ├── IncidentPriorityPolicy.java <- Urgency ranking comparator for PriorityQueue
  │    ├── DatabaseManager.java  <- Pure JDBC PreparedStatements & persistence
  │    ├── DispatchService.java  <- Core coordinator (PriorityQueue, Dijkstra)
  │    └── Main.java             <- Built-in JDK HttpServer (port 8080) & REST API
  ├── web/                       <- Frontend Dashboard
  │    ├── index.html            <- EOC Control Room layout (Zero Emojis)
  │    ├── style.css             <- Professional console styling
  │    └── app.js                <- Live telemetry, vehicle animation, SVG map
  ├── lib/                       <- External Dependencies
  │    └── mysql-connector-j.jar <- MySQL Connector/J driver (pre-packaged)
  ├── database.sql               <- MySQL schema & default town data
  ├── run.bat                    <- 1-click Windows runner script
  ├── run.sh                     <- Linux / macOS runner script
  └── README.md                  <- Project documentation
```

---

## How to Run

### Prerequisites
- **Java SE Development Kit (JDK) 17 or higher** (`javac` and `java` in PATH).
- **MySQL 8.0** (optional; the application runs with in-memory fallback if MySQL is not running).

### Option 1: Windows (1-Click)
Double-click `run.bat` or open Command Prompt:
```cmd
cd "D:\My Documents\Downloads\Smart_Fire_Rescue_No_Maven"
run.bat
```

### Option 2: Command Line (Windows / Linux / macOS)
1. **Compile:**
   ```bash
   javac -encoding UTF-8 -cp "lib/mysql-connector-j.jar" -d out src/*.java
   ```
2. **Run:**
   - **Windows:**
     ```cmd
     java -cp "out;lib\mysql-connector-j.jar" Main
     ```
   - **Linux / macOS:**
     ```bash
     java -cp "out:lib/mysql-connector-j.jar" Main
     ```
3. Open your browser at:
   ```text
   http://localhost:8080
   ```

---

## MySQL Database Setup (Optional)

1. Ensure MySQL is running (default port `3307` or `3306`).
2. Import the database schema:
   ```cmd
   mysql -u root -p < database.sql
   ```
3. Update your credentials in `src/Config.java`:
   ```java
   public static final String URL = "jdbc:mysql://localhost:3307/smart_fire_rescue";
   public static final String USER = "root";
   public static final String PASSWORD = "YOUR_PASSWORD";
   ```
