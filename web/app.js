/**
 * Smart Fire and Rescue Response System - Control Room Dashboard Logic
 * Sudar Town Emergency Operations Center (EOC)
 * Professional Operational Console (Zero Emojis, Screenshot Layout)
 */

// 15 Town locations and layout coordinates for SVG topology map
const LOCATION_COORDINATES = {
  "Station-A":        { x: 100, y: 150, type: "station" },
  "Station-B":        { x: 780, y: 150, type: "station" },
  "Junction-1":       { x: 230, y: 180, type: "junction" },
  "Junction-2":       { x: 650, y: 180, type: "junction" },
  "Market":           { x: 300, y: 280, type: "facility" },
  "Hospital":         { x: 440, y: 190, type: "facility" },
  "Industrial-Estate":{ x: 360, y: 80,  type: "facility" },
  "River-Side":       { x: 570, y: 310, type: "facility" },
  "School":           { x: 440, y: 360, type: "facility" },
  "Bus-Stand":        { x: 190, y: 360, type: "facility" },
  "Residential-Area": { x: 300, y: 430, type: "facility" },
  "Highway":          { x: 760, y: 300, type: "facility" },
  "Warehouse":        { x: 550, y: 90,  type: "facility" },
  "Park":             { x: 670, y: 410, type: "facility" },
  "College":          { x: 450, y: 435, type: "facility" },
  "Other":            { x: 140, y: 280, type: "facility" }
};

const LOCATION_DISPLAY_NAMES = {
  "Residential-Area": "L2 - North Residential",
  "Hospital":         "L3 - City Hospital",
  "School":           "L7 - Public School",
  "Market":           "L4 - Central Market",
  "Industrial-Estate":"L5 - Industrial Estate",
  "River-Side":       "L6 - River Side",
  "Bus-Stand":        "L8 - Central Bus Stand",
  "Highway":          "L10 - Highway Corridor",
  "Warehouse":        "L11 - Logistics Warehouse",
  "Park":             "L12 - City Park",
  "College":          "L13 - City College",
  "Junction-1":       "L14 - West Junction",
  "Junction-2":       "L15 - East Junction",
  "Station-A":        "L1 - Central Fire Station (Station-A)",
  "Station-B":        "L9 - North Fire Station (Station-B)",
  "Other":            "Other / Unmapped Location (Others)"
};

let currentRoadNetwork = { locations: [], roads: [] };
let activeRoutePath = [];
let activeMissionTracker = null;
let vehicleAnimFrameId = null;

// DOM Helper
const $ = id => document.getElementById(id);

// --- Initialization ---
document.addEventListener("DOMContentLoaded", () => {
  initClock();
  initNavigation();
  initLocationChips();
  initEventListeners();

  // Load all operational data initially
  loadAllData();

  // Polling every 5 seconds for telemetry updates
  setInterval(loadLiveStatus, 5000);
  // Live mission countdown and ETA updater every 1 second (Feature C)
  setInterval(updateLiveMissionTimers, 1000);
});

// --- System Clock ---
function initClock() {
  const clockEl = $("systemClock");
  if (!clockEl) return;
  function update() {
    const now = new Date();
    clockEl.textContent = now.toLocaleTimeString('en-US', { hour12: false });
  }
  update();
  setInterval(update, 1000);
}

// --- Navigation View Switching ---
function initNavigation() {
  document.querySelectorAll(".nav-item[data-view]").forEach(btn => {
    btn.addEventListener("click", () => {
      switchView(btn.dataset.view);
    });
  });

  const heroReportBtn = $("heroReportBtn");
  if (heroReportBtn) {
    heroReportBtn.addEventListener("click", () => {
      switchView("report");
    });
  }

  const heroControlBtn = $("heroControlBtn");
  if (heroControlBtn) {
    heroControlBtn.addEventListener("click", () => {
      switchView("control");
    });
  }

  const viewAllBtn = $("viewAllIncidentsBtn");
  if (viewAllBtn) {
    viewAllBtn.addEventListener("click", () => {
      switchView("control");
    });
  }
}

function switchView(viewName) {
  // Update sidebar buttons active state
  document.querySelectorAll(".nav-item[data-view]").forEach(btn => {
    if (btn.dataset.view === viewName) {
      btn.classList.add("active");
    } else {
      btn.classList.remove("active");
    }
  });

  // Hide all views
  const views = ["viewHome", "viewReport", "viewControl", "viewUnits", "viewNetwork", "viewHistory"];
  views.forEach(vId => {
    const el = $(vId);
    if (el) el.classList.add("hidden");
  });

  // Show selected view
  const targetId = "view" + viewName.charAt(0).toUpperCase() + viewName.slice(1);
  const targetEl = $(targetId);
  if (targetEl) {
    targetEl.classList.remove("hidden");
  }

  // Refresh relevant data for this view
  if (viewName === "network") {
    loadRoadNetwork();
  } else if (viewName === "units") {
    loadUnits();
  } else if (viewName === "control") {
    loadControlRoom();
  } else if (viewName === "history") {
    loadHistory();
  } else if (viewName === "home") {
    loadIncidents();
    loadLiveStatus();
  }

  window.scrollTo({ top: 0, behavior: "smooth" });
}

// --- Interactive Location Selector (App & Map Driven, Zero <option> Dropdown) ---
function initLocationChips() {
  const container = $("locationChipsContainer");
  if (!container) return;
  container.innerHTML = "";

  const orderedLocations = [
    "Residential-Area",
    "Hospital",
    "School",
    "Market",
    "Industrial-Estate",
    "River-Side",
    "Bus-Stand",
    "Highway",
    "Warehouse",
    "Park",
    "College",
    "Junction-1",
    "Junction-2",
    "Station-A",
    "Station-B",
    "Other"
  ];

  orderedLocations.forEach(loc => {
    const btn = document.createElement("button");
    btn.type = "button";
    btn.className = "loc-chip-btn";
    btn.dataset.location = loc;
    btn.title = LOCATION_DISPLAY_NAMES[loc] || loc;

    const dot = document.createElement("span");
    dot.className = "chip-dot";
    const text = document.createElement("span");
    text.textContent = loc === "Other" ? "Other (Unmapped)" : loc;

    btn.appendChild(dot);
    btn.appendChild(text);

    btn.addEventListener("click", () => {
      selectIncidentLocation(loc);
    });

    container.appendChild(btn);
  });
}

function selectIncidentLocation(locKey) {
  const input = $("locationInput");
  const displayBox = $("selectedLocationDisplay");
  const displayName = $("selectedLocationName");
  const feedback = $("formFeedback");

  if (!locKey) {
    if (input) input.value = "";
    if (displayName) displayName.textContent = "None Selected \u2014 Click a location below or pick from Road Network map";
    if (displayBox) {
      displayBox.classList.remove("active");
      displayBox.classList.remove("error");
    }
    document.querySelectorAll(".loc-chip-btn").forEach(btn => btn.classList.remove("selected"));
    return;
  }

  if (input) input.value = locKey;
  if (displayName) {
    const label = LOCATION_DISPLAY_NAMES[locKey] ? `${locKey} (${LOCATION_DISPLAY_NAMES[locKey]})` : locKey;
    displayName.textContent = label;
  }
  if (displayBox) {
    displayBox.classList.add("active");
    displayBox.classList.remove("error");
  }
  if (feedback && feedback.textContent.includes("mandatory")) {
    feedback.textContent = "";
  }

  // Update selected class on all chips
  document.querySelectorAll(".loc-chip-btn").forEach(btn => {
    if (btn.dataset.location === locKey) {
      btn.classList.add("selected");
    } else {
      btn.classList.remove("selected");
    }
  });
}

window.selectIncidentLocation = selectIncidentLocation;

// --- Event Listeners ---
function initEventListeners() {
  const incidentForm = $("incidentForm");
  if (incidentForm) {
    incidentForm.addEventListener("submit", handleIncidentSubmit);
  }

  const pickOnMapBtn = $("pickOnMapBtn");
  if (pickOnMapBtn) {
    pickOnMapBtn.addEventListener("click", () => {
      switchView("network");
      showToast("Click any node on the town map to select it for the incident report.", "info");
    });
  }

  const dispatchBtn = $("dispatchBtn");
  if (dispatchBtn) {
    dispatchBtn.addEventListener("click", handleDispatch);
  }

  const dispatchAllBtn = $("dispatchAllBtn");
  if (dispatchAllBtn) {
    dispatchAllBtn.addEventListener("click", async () => {
      dispatchAllBtn.disabled = true;
      const feedback = $("controlDispatchFeedback");
      if (feedback) {
        feedback.textContent = "Assessing PriorityQueue and allocating compatible unit...";
        feedback.style.color = "var(--color-amber)";
      }
      try {
        const res = await fetchJson("/api/dispatch", { method: "POST" });
        if (feedback) {
          feedback.textContent = `Assigned ${res.unitCode} to ${res.location} (${res.travelTimeMinutes} mins).`;
          feedback.style.color = "var(--color-green)";
        }
        showToast(`Assigned ${res.unitCode} to ${res.location}.`, "success");
        await loadAllData();
        await loadControlRoom();
      } catch (err) {
        if (feedback) {
          feedback.textContent = err.message;
          feedback.style.color = "var(--brand-red-active)";
        }
        showToast(err.message, "error");
      } finally {
        dispatchAllBtn.disabled = false;
      }
    });
  }

  const toggleRoadBtn = $("toggleRoadBtn");
  if (toggleRoadBtn) {
    toggleRoadBtn.addEventListener("click", handleRoadToggle);
  }

  const simulateSurgeBtn = $("simulateSurgeBtn");
  if (simulateSurgeBtn) {
    simulateSurgeBtn.addEventListener("click", async () => {
      simulateSurgeBtn.disabled = true;
      try {
        const res = await fetchJson("/api/simulate/surge", { method: "POST" });
        showToast(`Simulated surge: 3 emergencies enqueued. Top priority is now ${res.newTopCode} (Score: ${res.newTopPriority}).`, "success");
        await loadAllData();
      } catch (err) {
        showToast(err.message, "error");
      } finally {
        simulateSurgeBtn.disabled = false;
      }
    });
  }

  // Live Track on Map Button (Feature A)
  const btnTrackOnMap = $("btnTrackOnMap");
  if (btnTrackOnMap) {
    btnTrackOnMap.addEventListener("click", () => {
      switchView("network");
      const mapEl = $("roadMapSvg");
      if (mapEl) mapEl.scrollIntoView({ behavior: "smooth", block: "center" });
      showToast("Live tracking response unit traversing Dijkstra shortest path.", "info");
    });
  }

  // Map Active Mission Complete Button (Feature C)
  const mapCompleteBtn = $("mapCompleteMissionBtn");
  if (mapCompleteBtn) {
    mapCompleteBtn.addEventListener("click", () => {
      if (activeMissionTracker && activeMissionTracker.incidentId) {
        handleCompleteMission(activeMissionTracker.incidentId);
      } else {
        showToast("No active dispatched mission to complete.", "info");
      }
    });
  }
}

// --- REST API Fetch Helper ---
async function fetchJson(endpoint, options = {}) {
  try {
    const res = await fetch(endpoint, options);
    const data = await res.json().catch(() => null);
    if (!res.ok) {
      const errMsg = (data && (data.error || data.message)) || `HTTP ${res.status}`;
      throw new Error(errMsg);
    }
    return data;
  } catch (err) {
    throw err;
  }
}

// --- Master Data Loader ---
async function loadAllData() {
  await Promise.allSettled([
    loadLiveStatus(),
    loadIncidents(),
    loadUnits(),
    loadHistory(),
    loadRoadNetwork()
  ]);
}

// --- System Status & Metrics ---
async function loadLiveStatus() {
  try {
    const stats = await fetchJson("/api/status");
    if (!stats) return;

    if ($("statTotalIncidents")) $("statTotalIncidents").textContent = stats.totalIncidents ?? 0;
    if ($("statPending")) $("statPending").textContent = stats.pendingIncidents ?? 0;
    if ($("statDispatched")) $("statDispatched").textContent = stats.dispatchedIncidents ?? 0;
    if ($("statAvailableUnits")) $("statAvailableUnits").textContent = `${stats.availableUnits ?? 0} / ${stats.totalUnits ?? 4}`;

    const dbBadge = $("dbStatusBadge");
    if (dbBadge) {
      if (stats.dbConnected) {
        dbBadge.className = "status-indicator connected";
        dbBadge.textContent = "Database: MySQL 8.0 (Active \u00B7 Port 3307)";
      } else {
        dbBadge.className = "status-indicator";
        dbBadge.textContent = "Database: In-Memory (MySQL Standby)";
      }
    }
  } catch (err) {
    console.error("Status check failed:", err);
  }
}

// --- Load Incident Priority Queue ---
async function loadIncidents() {
  const tbody = $("homeIncidentTbody");
  if (!tbody) return;

  try {
    const list = await fetchJson("/api/incidents");
    if (!list || list.length === 0) {
      tbody.innerHTML = `<tr><td colspan="9" class="loading-cell">No incidents registered.</td></tr>`;
      return;
    }

    tbody.innerHTML = list.map(inc => {
      const prioClass = inc.priority >= 65 ? "prio-high" : inc.priority >= 45 ? "prio-med" : "prio-low";
      const statusClass = inc.status === "PENDING" ? "status-pending" : inc.status === "DISPATCHED" ? "status-dispatched" : "status-completed";

      return `
        <tr>
          <td><span class="prio-pill ${prioClass}">${inc.priority}</span></td>
          <td><strong>${escapeHtml(inc.code || 'INC-' + inc.id)}</strong></td>
          <td>${escapeHtml(inc.location)}</td>
          <td>${escapeHtml(inc.type)}</td>
          <td>${inc.intensity} / 5</td>
          <td>${inc.critical ? '<strong style="color:var(--brand-red-active);">YES</strong>' : 'NO'}</td>
          <td>Rank ${inc.population}</td>
          <td><span class="capability-tag">${escapeHtml(inc.equipment)}</span></td>
          <td><span class="status-badge ${statusClass}">${escapeHtml(inc.status)}</span></td>
        </tr>
      `;
    }).join("");
  } catch (err) {
    tbody.innerHTML = `<tr><td colspan="9" class="loading-cell" style="color:var(--brand-red-active);">Error loading incidents: ${escapeHtml(err.message)}</td></tr>`;
  }
}

// --- Load Response Units Table (Matches media_1791740063258.png) ---
async function loadUnits() {
  const tbody = $("unitRows");
  if (!tbody) return;

  try {
    const units = await fetchJson("/api/units");
    if (!units || units.length === 0) {
      tbody.innerHTML = `<tr><td colspan="5" class="loading-cell">No response units cataloged.</td></tr>`;
      return;
    }

    tbody.innerHTML = units.map(u => {
      const isAvail = u.available;
      const statusText = isAvail ? "AVAILABLE" : "BUSY";
      const statusClass = isAvail ? "status-completed" : "status-dispatched";

      return `
        <tr>
          <td><strong>${escapeHtml(u.code)}</strong></td>
          <td>${escapeHtml(u.station)}</td>
          <td>${escapeHtml(u.location)}</td>
          <td>${escapeHtml((u.equipment || '').split(',').join(', '))}</td>
          <td><span class="status-badge ${statusClass}">${statusText}</span></td>
        </tr>
      `;
    }).join("");
  } catch (err) {
    tbody.innerHTML = `<tr><td colspan="5" class="loading-cell" style="color:var(--brand-red-active);">Error loading units: ${escapeHtml(err.message)}</td></tr>`;
  }
}

// --- Load Control Room Dashboard Queue (Matches media_1791740065384.png) ---
async function loadControlRoom() {
  const tbody = $("controlIncidentRows");
  if (!tbody) return;

  try {
    const list = await fetchJson("/api/incidents");
    if (!list || list.length === 0) {
      tbody.innerHTML = `<tr><td colspan="6" class="loading-cell">No incidents reported.</td></tr>`;
      return;
    }

    tbody.innerHTML = list.map(inc => {
      const isPending = inc.status === "PENDING";
      const isDispatched = inc.status === "DISPATCHED";
      const statusClass = isPending ? "status-pending" : isDispatched ? "status-dispatched" : "status-completed";

      let actionHtml = `<span style="color:var(--text-muted);">&mdash;</span>`;
      let statusBadgeHtml = `<span class="status-badge ${statusClass}">${escapeHtml(inc.status)}</span>`;

      if (isPending) {
        actionHtml = `<button class="btn btn-sm btn-hero-danger" onclick="handleDispatchSingle(${inc.id})">Dispatch</button>`;
      } else if (isDispatched) {
        actionHtml = `<button class="btn-complete-action" onclick="handleCompleteMission(${inc.id})">Complete</button>`;
        statusBadgeHtml = `<span class="status-badge status-dispatched"><span class="pulse-dot"></span> EN ROUTE (<span class="control-countdown-val" data-incident-id="${inc.id}">ETA</span>)</span>`;
      }

      const riskLevel = inc.priority >= 65 ? "CRITICAL" : inc.priority >= 45 ? "HIGH" : inc.priority >= 25 ? "MEDIUM" : "LOW";

      return `
        <tr>
          <td><strong>${escapeHtml(inc.code || 'INC-' + inc.id)}</strong></td>
          <td>${escapeHtml(inc.location)}</td>
          <td><span class="prio-pill ${inc.priority >= 65 ? 'prio-high' : 'prio-med'}">${riskLevel} (${inc.priority})</span></td>
          <td>${escapeHtml(inc.equipment)}</td>
          <td>${statusBadgeHtml}</td>
          <td>${actionHtml}</td>
        </tr>
      `;
    }).join("");
  } catch (err) {
    tbody.innerHTML = `<tr><td colspan="6" class="loading-cell" style="color:var(--brand-red-active);">Error loading control room: ${escapeHtml(err.message)}</td></tr>`;
  }
}

async function handleDispatchSingle(incidentId) {
  try {
    const res = await fetchJson("/api/dispatch", { method: "POST" });
    showToast(`Assigned ${res.unitCode} to ${res.location} (${res.travelTimeMinutes} mins).`, "success");

    const travelTime = res.travelTimeMinutes || 5;
    const demoDurationSec = Math.max(10, Math.round(travelTime * 2.5));
    activeRoutePath = res.path || [];
    activeMissionTracker = {
      incidentId: res.incidentId,
      incidentCode: res.incidentCode || `#${res.incidentId}`,
      unitCode: res.unitCode,
      unitType: res.unitType,
      startLocation: res.station,
      targetLocation: res.location,
      travelTimeMinutes: travelTime,
      durationSeconds: demoDurationSec,
      startTimeMs: res.dispatchedTimestamp || Date.now(),
      path: res.path || [],
      completed: false
    };

    await loadAllData();
    await loadControlRoom();
    renderSvgMap();
    updateLiveMissionTimers();
  } catch (err) {
    showToast(err.message, "error");
  }
}

window.handleDispatchSingle = handleDispatchSingle;

// --- Load Mission Audit Logs ---
async function loadHistory() {
  const tbody = $("historyTableBody");
  if (!tbody) return;

  try {
    const history = await fetchJson("/api/history");
    if (!history || history.length === 0) {
      tbody.innerHTML = `<tr><td colspan="7" class="loading-cell">No mission history logged yet.</td></tr>`;
      return;
    }

    tbody.innerHTML = history.map(rec => {
      const isDispatched = rec.status === "DISPATCHED";
      const statusBadge = isDispatched
        ? `<span class="status-badge status-dispatched"><span class="pulse-dot"></span> EN ROUTE (<span class="mission-countdown-val" data-incident-id="${rec.incidentId}">${rec.travelTimeMinutes}m ETA</span>)</span>`
        : `<span class="status-badge status-completed">COMPLETED</span>`;

      const actionBtn = isDispatched
        ? `<button class="btn-complete-action" onclick="handleCompleteMission(${rec.incidentId})">Complete Mission</button>`
        : `<span style="color:var(--text-muted);">&mdash;</span>`;

      // Auto-restore active mission tracking from history if browser was reloaded
      if (!activeMissionTracker && isDispatched) {
        const pathSteps = (rec.route || "").split(" -> ").map(s => s.trim()).filter(Boolean);
        if (pathSteps.length >= 2) {
          activeRoutePath = pathSteps;
          const travelTime = rec.travelTimeMinutes || 5;
          activeMissionTracker = {
            incidentId: rec.incidentId,
            incidentCode: rec.incidentCode || `#${rec.incidentId}`,
            unitCode: rec.unitCode,
            startLocation: rec.startLocation,
            targetLocation: rec.targetLocation,
            travelTimeMinutes: travelTime,
            durationSeconds: Math.max(10, Math.round(travelTime * 2.5)),
            startTimeMs: rec.dispatchedTimestamp || Date.now(),
            path: pathSteps,
            completed: false
          };
        }
      }

      return `
        <tr>
          <td><strong>${escapeHtml(rec.incidentCode || '#' + rec.incidentId)}</strong></td>
          <td><span class="capability-tag" style="font-weight:700;">${escapeHtml(rec.unitCode)}</span></td>
          <td>${escapeHtml(rec.startLocation)} &rarr; ${escapeHtml(rec.targetLocation)}</td>
          <td style="font-family:var(--font-mono);font-size:11.5px;">${escapeHtml(rec.route)}</td>
          <td><strong>${rec.travelTimeMinutes} mins</strong></td>
          <td>${statusBadge}</td>
          <td>${actionBtn}</td>
        </tr>
      `;
    }).join("");
  } catch (err) {
    tbody.innerHTML = `<tr><td colspan="7" class="loading-cell" style="color:var(--brand-red-active);">Error loading history: ${escapeHtml(err.message)}</td></tr>`;
  }
}

// --- Load Road Topology & SVG Map Rendering ---
async function loadRoadNetwork() {
  try {
    const data = await fetchJson("/api/roads");
    if (!data) return;
    currentRoadNetwork = data;
    renderSvgMap();
    populateRoadClosureSelect();
  } catch (err) {
    console.error("Road network load failed:", err);
  }
}

function renderSvgMap() {
  const svg = $("roadMapSvg");
  if (!svg) return;
  svg.innerHTML = "";

  const edgeGroup = document.createElementNS("http://www.w3.org/2000/svg", "g");
  const labelGroup = document.createElementNS("http://www.w3.org/2000/svg", "g");
  const nodeGroup = document.createElementNS("http://www.w3.org/2000/svg", "g");

  const activeEdges = new Set();
  for (let i = 0; i < activeRoutePath.length - 1; i++) {
    const u = activeRoutePath[i];
    const v = activeRoutePath[i + 1];
    activeEdges.add(u + "--" + v);
    activeEdges.add(v + "--" + u);
  }

  // Draw Edges
  (currentRoadNetwork.roads || []).forEach(r => {
    const c1 = LOCATION_COORDINATES[r.from];
    const c2 = LOCATION_COORDINATES[r.to];
    if (!c1 || !c2) return;

    const isActive = activeEdges.has(r.from + "--" + r.to);
    const isClosed = r.closed;

    const line = document.createElementNS("http://www.w3.org/2000/svg", "line");
    line.setAttribute("x1", c1.x);
    line.setAttribute("y1", c1.y);
    line.setAttribute("x2", c2.x);
    line.setAttribute("y2", c2.y);

    if (isClosed) {
      line.setAttribute("stroke", "#dc2626");
      line.setAttribute("stroke-width", "2");
      line.setAttribute("stroke-dasharray", "4,4");
    } else if (isActive) {
      line.setAttribute("stroke", "#d97706");
      line.setAttribute("stroke-width", "4.5");
      line.setAttribute("stroke-linecap", "round");
    } else {
      line.setAttribute("stroke", "#1e293b");
      line.setAttribute("stroke-width", "2");
    }
    edgeGroup.appendChild(line);

    // Edge Travel Time Label
    const midX = (c1.x + c2.x) / 2;
    const midY = (c1.y + c2.y) / 2;

    const text = document.createElementNS("http://www.w3.org/2000/svg", "text");
    text.setAttribute("x", midX);
    text.setAttribute("y", midY - 3);
    text.setAttribute("text-anchor", "middle");
    text.setAttribute("fill", isClosed ? "#ef4444" : isActive ? "#f59e0b" : "#64748b");
    text.setAttribute("font-size", "9.5");
    text.setAttribute("font-family", "JetBrains Mono, monospace");
    text.setAttribute("font-weight", "700");
    text.textContent = isClosed ? "CLOSED" : `${r.time}m`;
    labelGroup.appendChild(text);
  });

  // Draw Nodes (Clickable to select and report incident)
  Object.entries(LOCATION_COORDINATES).forEach(([name, c]) => {
    const isStation = c.type === "station";
    const isJunction = c.type === "junction";
    const isInRoute = activeRoutePath.includes(name);

    const nodeG = document.createElementNS("http://www.w3.org/2000/svg", "g");
    nodeG.setAttribute("class", "svg-map-node");
    nodeG.setAttribute("data-location", name);
    nodeG.style.cursor = "pointer";

    // Tooltip for user clarity
    const title = document.createElementNS("http://www.w3.org/2000/svg", "title");
    title.textContent = `Click to report emergency at ${name} (${LOCATION_DISPLAY_NAMES[name] || name})`;
    nodeG.appendChild(title);

    const circle = document.createElementNS("http://www.w3.org/2000/svg", "circle");
    circle.setAttribute("cx", c.x);
    circle.setAttribute("cy", c.y);
    circle.setAttribute("r", isStation ? 9 : 7);

    let fillColor = isStation ? "#dc2626" : isJunction ? "#2563eb" : "#059669";
    if (isInRoute) fillColor = "#d97706";

    circle.setAttribute("fill", fillColor);
    circle.setAttribute("stroke", "#090d16");
    circle.setAttribute("stroke-width", "2");
    nodeG.appendChild(circle);

    const text = document.createElementNS("http://www.w3.org/2000/svg", "text");
    text.setAttribute("x", c.x);
    text.setAttribute("y", c.y + (isStation ? 20 : 16));
    text.setAttribute("text-anchor", "middle");
    text.setAttribute("fill", isInRoute ? "#fbbf24" : isStation ? "#f87171" : "#cbd5e1");
    text.setAttribute("font-size", isStation ? "11" : "9.5");
    text.setAttribute("font-family", "Inter, sans-serif");
    text.setAttribute("font-weight", isStation || isInRoute ? "700" : "500");
    text.textContent = name;
    nodeG.appendChild(text);

    // Interactive Click-to-Report functionality directly from app map
    nodeG.addEventListener("click", () => {
      selectIncidentLocation(name);
      switchView("report");
      showToast(`Selected location: ${name} from Road Network map.`, "info");
    });

    nodeGroup.appendChild(nodeG);
  });

  svg.appendChild(edgeGroup);
  svg.appendChild(labelGroup);
  svg.appendChild(nodeGroup);

  // Live Vehicle Animation (Feature A)
  if (activeMissionTracker && activeMissionTracker.path && activeMissionTracker.path.length >= 2) {
    drawLiveVehicleMarker(svg);
  }
}

// --- Live Vehicle Movement on SVG Map (Feature A) ---
function drawLiveVehicleMarker(svg) {
  if (!activeMissionTracker || !activeMissionTracker.path || activeMissionTracker.path.length < 2) return;

  const path = activeMissionTracker.path;
  const coords = path.map(name => LOCATION_COORDINATES[name]).filter(Boolean);
  if (coords.length < 2) return;

  // Calculate cumulative segment distances
  let totalDist = 0;
  const segDistances = [];
  for (let i = 0; i < coords.length - 1; i++) {
    const dx = coords[i + 1].x - coords[i].x;
    const dy = coords[i + 1].y - coords[i].y;
    const d = Math.hypot(dx, dy);
    segDistances.push(d);
    totalDist += d;
  }
  if (totalDist === 0) return;

  // Calculate current transit progress
  const elapsedSec = (Date.now() - activeMissionTracker.startTimeMs) / 1000;
  const totalSec = activeMissionTracker.durationSeconds || 20;
  const progressRatio = Math.min(1.0, Math.max(0, elapsedSec / totalSec));
  const isArrived = progressRatio >= 1.0;

  // Find coordinate (currX, currY)
  let currX = coords[0].x;
  let currY = coords[0].y;

  if (isArrived) {
    currX = coords[coords.length - 1].x;
    currY = coords[coords.length - 1].y;
  } else {
    const targetDist = progressRatio * totalDist;
    let accumulated = 0;
    for (let i = 0; i < segDistances.length; i++) {
      if (accumulated + segDistances[i] >= targetDist) {
        const segRemain = targetDist - accumulated;
        const t = segDistances[i] > 0 ? (segRemain / segDistances[i]) : 0;
        currX = coords[i].x + (coords[i + 1].x - coords[i].x) * t;
        currY = coords[i].y + (coords[i + 1].y - coords[i].y) * t;
        break;
      }
      accumulated += segDistances[i];
    }
  }

  // Remove previous vehicle marker if exists
  const oldMarker = svg.querySelector("#liveVehicleMarker");
  if (oldMarker) oldMarker.remove();

  const vehicleG = document.createElementNS("http://www.w3.org/2000/svg", "g");
  vehicleG.setAttribute("id", "liveVehicleMarker");
  vehicleG.setAttribute("class", "vehicle-live-marker");
  vehicleG.setAttribute("transform", `translate(${currX.toFixed(1)}, ${currY.toFixed(1)})`);

  // Radar pulse beacon circle
  const ping = document.createElementNS("http://www.w3.org/2000/svg", "circle");
  ping.setAttribute("r", isArrived ? "14" : "18");
  ping.setAttribute("fill", "none");
  ping.setAttribute("stroke", isArrived ? "#10b981" : "#ef4444");
  ping.setAttribute("stroke-width", "2");
  ping.setAttribute("opacity", "0.8");
  vehicleG.appendChild(ping);

  // Core vehicle dot
  const core = document.createElementNS("http://www.w3.org/2000/svg", "circle");
  core.setAttribute("r", "10");
  core.setAttribute("fill", isArrived ? "#059669" : "#dc2626");
  core.setAttribute("stroke", "#ffffff");
  core.setAttribute("stroke-width", "2");
  vehicleG.appendChild(core);

  // Unit symbol text
  const label = document.createElementNS("http://www.w3.org/2000/svg", "text");
  label.setAttribute("y", "3.5");
  label.setAttribute("text-anchor", "middle");
  label.setAttribute("fill", "#ffffff");
  label.setAttribute("font-size", "7.5");
  label.setAttribute("font-family", "JetBrains Mono, monospace");
  label.setAttribute("font-weight", "800");
  label.textContent = (activeMissionTracker.unitCode || "VEH").substring(0, 2);
  vehicleG.appendChild(label);

  // Floating pill badge above vehicle
  const pillG = document.createElementNS("http://www.w3.org/2000/svg", "g");
  pillG.setAttribute("transform", "translate(0, -18)");

  const rect = document.createElementNS("http://www.w3.org/2000/svg", "rect");
  rect.setAttribute("x", "-32");
  rect.setAttribute("y", "-9");
  rect.setAttribute("width", "64");
  rect.setAttribute("height", "17");
  rect.setAttribute("rx", "4");
  rect.setAttribute("fill", "#0f172a");
  rect.setAttribute("stroke", isArrived ? "#10b981" : "#f59e0b");
  rect.setAttribute("stroke-width", "1.5");
  pillG.appendChild(rect);

  const pillText = document.createElementNS("http://www.w3.org/2000/svg", "text");
  pillText.setAttribute("y", "3");
  pillText.setAttribute("text-anchor", "middle");
  pillText.setAttribute("fill", "#ffffff");
  pillText.setAttribute("font-size", "8.5");
  pillText.setAttribute("font-family", "JetBrains Mono, monospace");
  pillText.setAttribute("font-weight", "700");
  pillText.textContent = isArrived ? "ON SCENE" : activeMissionTracker.unitCode;
  pillG.appendChild(pillText);

  vehicleG.appendChild(pillG);
  svg.appendChild(vehicleG);

  // Continue smooth 60fps frame updates while in transit
  if (!isArrived) {
    if (vehicleAnimFrameId) cancelAnimationFrame(vehicleAnimFrameId);
    vehicleAnimFrameId = requestAnimationFrame(() => {
      const activeSvg = $("roadMapSvg");
      if (activeSvg) drawLiveVehicleMarker(activeSvg);
    });
  }
}

// --- Live Mission Countdown & ETA Formatter (Feature C) ---
function formatMmSs(totalSec) {
  const m = Math.floor(totalSec / 60);
  const s = totalSec % 60;
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
}

function updateLiveMissionTimers() {
  if (!activeMissionTracker) {
    const banner = $("mapMissionBanner");
    if (banner) banner.classList.add("hidden");
    return;
  }

  const elapsedSec = (Date.now() - activeMissionTracker.startTimeMs) / 1000;
  const totalSec = activeMissionTracker.durationSeconds || 20;
  const remainingSec = Math.max(0, Math.ceil(totalSec - elapsedSec));
  const progressRatio = Math.min(1.0, elapsedSec / totalSec);
  const progressPct = Math.min(100, Math.round(progressRatio * 100));

  // 1. Telemetry card
  const telCd = $("telCountdown");
  const telStatus = $("telCountdownStatus");
  const telBar = $("telProgressBar");

  if (telBar) telBar.style.width = `${progressPct}%`;

  if (remainingSec > 0) {
    if (telCd) {
      telCd.textContent = formatMmSs(remainingSec);
      telCd.style.color = "#f59e0b";
    }
    if (telStatus) telStatus.textContent = `Transit En Route (${progressPct}%)`;
  } else {
    if (telCd) {
      telCd.textContent = "ARRIVED";
      telCd.style.color = "var(--color-green)";
    }
    if (telStatus) telStatus.textContent = `On Scene at ${activeMissionTracker.targetLocation}`;
  }

  // 2. Map Mission Banner
  const banner = $("mapMissionBanner");
  const bannerText = $("mapMissionBannerText");
  const bannerEta = $("mapMissionEtaText");

  if (banner) banner.classList.remove("hidden");
  if (bannerText) {
    bannerText.textContent = `ACTIVE MISSION: Unit ${activeMissionTracker.unitCode} navigating to ${activeMissionTracker.targetLocation} via ${(activeMissionTracker.path || []).join(" \u2192 ")}`;
  }
  if (bannerEta) {
    if (remainingSec > 0) {
      bannerEta.textContent = `ETA: ${formatMmSs(remainingSec)}`;
      bannerEta.style.color = "#fbbf24";
      bannerEta.style.borderColor = "rgba(251, 191, 36, 0.4)";
    } else {
      bannerEta.textContent = "ARRIVED ON SCENE";
      bannerEta.style.color = "#10b981";
      bannerEta.style.borderColor = "#10b981";
    }
  }

  // 3. Dynamic countdown in tables
  document.querySelectorAll(".mission-countdown-val").forEach(el => {
    const incId = Number(el.dataset.incidentId);
    if (activeMissionTracker && activeMissionTracker.incidentId === incId) {
      el.textContent = remainingSec > 0 ? `ETA ${formatMmSs(remainingSec)}` : "ARRIVED";
    }
  });

  document.querySelectorAll(".control-countdown-val").forEach(el => {
    const incId = Number(el.dataset.incidentId);
    if (activeMissionTracker && activeMissionTracker.incidentId === incId) {
      el.textContent = remainingSec > 0 ? `ETA ${formatMmSs(remainingSec)}` : "ON SCENE";
    }
  });
}

function populateRoadClosureSelect() {
  const sel = $("roadClosureSelect");
  if (!sel) return;
  sel.innerHTML = `<option value="">Select road segment...</option>`;
  (currentRoadNetwork.roads || []).forEach(r => {
    const opt = document.createElement("option");
    opt.value = `${r.from}|${r.to}`;
    opt.textContent = `${r.from} \u2192 ${r.to} (${r.time} mins) ${r.closed ? ' [CLOSED]' : ''}`;
    sel.appendChild(opt);
  });
}

// --- Incident Submission & Risk Assessment (Caller Observations) ---
async function handleIncidentSubmit(e) {
  e.preventDefault();
  const form = e.target;
  const submitBtn = $("submitIncidentBtn");
  const feedback = $("formFeedback");

  const locationEl = $("locationInput");
  const locationVal = locationEl ? locationEl.value.trim() : "";
  if (!locationVal) {
    const displayBox = $("selectedLocationDisplay");
    if (displayBox) {
      displayBox.classList.add("error");
      displayBox.scrollIntoView({ behavior: "smooth", block: "center" });
    }
    if (feedback) {
      feedback.textContent = "Incident Location is mandatory. Please select a location from the badges or Road Network map.";
      feedback.style.color = "var(--brand-red-active)";
    }
    showToast("Incident Location is mandatory. Please select a location.", "error");
    return;
  }

  const smokeVal = $("smokeSelect") ? $("smokeSelect").value : "Unknown";
  const trappedVal = $("trappedSelect") ? $("trappedSelect").value : "Unknown";
  const popVal = $("populationExposureSelect") ? $("populationExposureSelect").value : "Moderate";
  const critVal = $("criticalInfraSelect") ? $("criticalInfraSelect").value : "Unknown";

  // Base provisional intensity: 2
  let intensity = 2;

  // Smoke observation severity
  if (smokeVal === "Dense Toxic Fumes") {
    intensity = Math.min(5, intensity + 2);
  } else if (smokeVal === "Heavy Black Smoke") {
    intensity = Math.min(5, intensity + 1);
  }

  // Trapped persons risk modifier
  if (trappedVal === "Confirmed Multiple Trapped" || trappedVal === "High Risk / Evacuation Blocked") {
    intensity = Math.min(5, intensity + 2);
  } else if (trappedVal === "Possible (1-2 persons)") {
    intensity = Math.min(5, intensity + 1);
  }

  // Critical infrastructure determination
  const isCritical = (critVal === "Yes");

  // Population exposure ranking (1 to 3)
  let popRank = 1;
  if (popVal === "High" || trappedVal === "Confirmed Multiple Trapped") {
    popRank = 3;
  } else if (popVal === "Moderate") {
    popRank = 2;
  } else if (popVal === "Low") {
    popRank = 1;
  }

  intensity = Math.max(1, Math.min(5, intensity));

  // Automated Incident Classification and Equipment Allocation from caller observations
  let compositeType = "Structure Fire";
  let equipmentVal = "HOSE";

  if (smokeVal === "Dense Toxic Fumes") {
    compositeType = "Hazardous Material Incident";
    equipmentVal = "FOAM";
  } else if (trappedVal === "Confirmed Multiple Trapped" || trappedVal === "High Risk / Evacuation Blocked") {
    compositeType = "Rescue & Evacuation Emergency";
    equipmentVal = "LADDER";
  } else if (trappedVal === "Possible (1-2 persons)") {
    compositeType = "Search & Rescue Incident";
    equipmentVal = "CUTTER";
  } else if (isCritical) {
    compositeType = "Critical Facility Alert";
    equipmentVal = "HOSE";
  } else if (smokeVal === "Heavy Black Smoke" || smokeVal === "Light Smoke") {
    compositeType = "Fire Emergency";
    equipmentVal = "HOSE";
  } else {
    compositeType = "Emergency Incident";
    equipmentVal = "HOSE";
  }

  const postBody = new URLSearchParams({
    location: locationVal,
    type: compositeType,
    intensity: String(intensity),
    critical: isCritical ? "true" : "false",
    population: String(popRank),
    equipment: equipmentVal
  });

  submitBtn.disabled = true;
  feedback.textContent = "Calculating provisional risk score and enqueuing incident...";
  feedback.style.color = "var(--text-muted)";

  try {
    const res = await fetchJson("/api/incidents", {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: postBody.toString()
    });

    showToast(`Incident ${res.code || ''} assessed: Priority Score ${res.priority} (Provisional Intensity: ${intensity}/5, Auto-equipment: ${equipmentVal}).`, "success");
    feedback.textContent = `Incident registered successfully (Priority: ${res.priority} pts, Equipment: ${equipmentVal}).`;
    feedback.style.color = "var(--color-green)";

    form.reset();
    selectIncidentLocation(null);

    await loadAllData();

    // After 1.4s, return to Home view to observe the incident in the PriorityQueue
    setTimeout(() => {
      switchView("home");
    }, 1400);
  } catch (err) {
    showToast(err.message, "error");
    feedback.textContent = err.message;
    feedback.style.color = "var(--brand-red-active)";
  } finally {
    submitBtn.disabled = false;
  }
}

// --- Automated Dijkstra Dispatch Execution ---
async function handleDispatch() {
  const btn = $("dispatchBtn");
  const msgEl = $("dispatchStatusMsg");
  btn.disabled = true;
  msgEl.textContent = "Evaluating PriorityQueue and computing Dijkstra path...";
  msgEl.style.color = "var(--color-amber)";

  try {
    const res = await fetchJson("/api/dispatch", { method: "POST" });

    msgEl.textContent = res.message;
    msgEl.style.color = "var(--color-green)";
    showToast(`Dispatched unit ${res.unitCode} to ${res.location} (${res.travelTimeMinutes} mins).`, "success");

    // Update Telemetry Panel
    $("telemetryPlaceholder").classList.add("hidden");
    $("telemetryContent").classList.remove("hidden");

    $("telUnitCode").textContent = res.unitCode;
    $("telUnitType").textContent = res.unitType;
    $("telStation").textContent = res.station;
    $("telIncidentCode").textContent = res.incidentCode || `#${res.incidentId}`;
    $("telTargetLoc").textContent = res.location;
    $("telTravelTime").textContent = `${res.travelTimeMinutes} mins`;

    // Render Breadcrumbs
    const bcContainer = $("telRouteBreadcrumbs");
    const path = res.path || [];
    activeRoutePath = path;

    bcContainer.innerHTML = path.map((step, idx) => {
      const isFirst = idx === 0;
      const isLast = idx === path.length - 1;
      const chipClass = isFirst ? "start" : isLast ? "target" : "";
      const arrow = isLast ? "" : `<span class="route-arrow">&rarr;</span>`;
      return `<span class="route-chip ${chipClass}">${escapeHtml(step)}</span>${arrow}`;
    }).join("");

    const travelTime = res.travelTimeMinutes || 5;
    const demoDurationSec = Math.max(10, Math.round(travelTime * 2.5));
    activeMissionTracker = {
      incidentId: res.incidentId,
      incidentCode: res.incidentCode || `#${res.incidentId}`,
      unitCode: res.unitCode,
      unitType: res.unitType,
      startLocation: res.station,
      targetLocation: res.location,
      travelTimeMinutes: travelTime,
      durationSeconds: demoDurationSec,
      startTimeMs: res.dispatchedTimestamp || Date.now(),
      path: res.path || [],
      completed: false
    };

    updateLiveMissionTimers();
    await loadAllData();
    renderSvgMap();
  } catch (err) {
    msgEl.textContent = err.message;
    msgEl.style.color = "var(--brand-red-active)";
    showToast(err.message, "error");
    await loadAllData();
  } finally {
    btn.disabled = false;
  }
}

// --- Complete Mission Action ---
async function handleCompleteMission(incidentId) {
  if (!confirm(`Confirm completion for incident #${incidentId}? The assigned vehicle will be released to AVAILABLE.`)) {
    return;
  }

  try {
    const body = new URLSearchParams({ incidentId: String(incidentId) });
    const res = await fetchJson("/api/complete", {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: body.toString()
    });

    showToast(res.message, "success");
    activeRoutePath = [];
    activeMissionTracker = null;
    if (vehicleAnimFrameId) {
      cancelAnimationFrame(vehicleAnimFrameId);
      vehicleAnimFrameId = null;
    }
    const banner = $("mapMissionBanner");
    if (banner) banner.classList.add("hidden");
    const telCd = $("telCountdown");
    if (telCd) {
      telCd.textContent = "--:--";
      telCd.style.color = "#f59e0b";
    }
    const telStatus = $("telCountdownStatus");
    if (telStatus) telStatus.textContent = "Transit En Route";
    const telBar = $("telProgressBar");
    if (telBar) telBar.style.width = "0%";

    await loadAllData();
    renderSvgMap();
  } catch (err) {
    showToast(err.message, "error");
  }
}

window.handleCompleteMission = handleCompleteMission;

// --- Road Closure Simulation Handler ---
async function handleRoadToggle() {
  const sel = $("roadClosureSelect");
  const val = sel.value;
  const feedback = $("roadToggleFeedback");
  if (!val) {
    feedback.textContent = "Select a road segment first.";
    feedback.style.color = "var(--color-amber)";
    return;
  }

  const [from, to] = val.split("|");
  try {
    const body = new URLSearchParams({ from, to });
    const res = await fetchJson("/api/road/toggle", {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: body.toString()
    });

    feedback.textContent = res.message;
    feedback.style.color = res.closed ? "var(--brand-red-active)" : "var(--color-green)";
    showToast(res.message, res.closed ? "info" : "success");

    await loadRoadNetwork();
  } catch (err) {
    feedback.textContent = err.message;
    feedback.style.color = "var(--brand-red-active)";
    showToast(err.message, "error");
  }
}

// --- Toast Notifications (Zero Emojis) ---
function showToast(message, type = "info") {
  const container = $("toastContainer");
  if (!container) return;
  const toast = document.createElement("div");
  toast.className = `toast ${type}`;

  const prefix = type === "success" ? "STATUS" : type === "error" ? "ERROR" : "NOTICE";
  toast.innerHTML = `<span style="font-family:var(--font-mono);font-size:10.5px;font-weight:700;color:var(--text-dim);">[${prefix}]</span> <span>${escapeHtml(message)}</span>`;

  container.appendChild(toast);
  setTimeout(() => {
    toast.style.opacity = "0";
    toast.style.transition = "opacity 0.3s ease";
    setTimeout(() => toast.remove(), 300);
  }, 3500);
}

// --- HTML Escaping Security Helper ---
function escapeHtml(str) {
  if (str === null || str === undefined) return "";
  return String(str)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#039;");
}