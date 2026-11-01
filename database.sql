-- ====================================================================
-- Smart Fire and Rescue Response System - Sudar Town Emergency Services
-- Database Schema and Demo Seed Data
-- Compatible with MySQL 8.0+
-- ====================================================================

CREATE DATABASE IF NOT EXISTS smart_fire_rescue;
USE smart_fire_rescue;

-- 1. Stations Table
CREATE TABLE IF NOT EXISTS stations (
    id INT AUTO_INCREMENT PRIMARY KEY,
    station_code VARCHAR(30) UNIQUE NOT NULL,
    station_name VARCHAR(100) NOT NULL,
    location_name VARCHAR(100) NOT NULL
);

-- 2. Response Units Table
CREATE TABLE IF NOT EXISTS response_units (
    id INT AUTO_INCREMENT PRIMARY KEY,
    unit_code VARCHAR(30) UNIQUE NOT NULL,
    unit_type VARCHAR(50) NOT NULL,
    station_name VARCHAR(100) NOT NULL,
    current_location VARCHAR(100) NOT NULL,
    equipment VARCHAR(255) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'AVAILABLE'
);

-- 3. Incidents Table
CREATE TABLE IF NOT EXISTS incidents (
    id INT AUTO_INCREMENT PRIMARY KEY,
    incident_code VARCHAR(30) UNIQUE NOT NULL,
    location_name VARCHAR(100) NOT NULL,
    incident_type VARCHAR(100) NOT NULL,
    intensity INT NOT NULL,
    critical BOOLEAN NOT NULL DEFAULT FALSE,
    population_exposure INT NOT NULL DEFAULT 1,
    required_equipment VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 4. Mission History Table
CREATE TABLE IF NOT EXISTS mission_history (
    id INT AUTO_INCREMENT PRIMARY KEY,
    incident_id INT NOT NULL,
    unit_code VARCHAR(30) NOT NULL,
    start_location VARCHAR(100) NOT NULL,
    target_location VARCHAR(100) NOT NULL,
    route_text VARCHAR(500) NOT NULL,
    travel_time_mins INT NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL DEFAULT 'DISPATCHED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ====================================================================
-- SEED DATA (Idempotent - safe to run multiple times)
-- ====================================================================

-- 2 Fire Stations
INSERT INTO stations (station_code, station_name, location_name) VALUES
('STN-A', 'Central Fire Station', 'Station-A'),
('STN-B', 'North Fire Station', 'Station-B')
ON DUPLICATE KEY UPDATE station_name=VALUES(station_name), location_name=VALUES(location_name);

-- 4 Response Units (2 Fire Engines, 2 Rescue Vans)
INSERT INTO response_units (unit_code, unit_type, station_name, current_location, equipment, status) VALUES
('FE-01', 'Fire Engine', 'Central Fire Station', 'Station-A', 'HOSE,FOAM,LADDER,WATERPUMP', 'AVAILABLE'),
('RV-01', 'Rescue Van', 'Central Fire Station', 'Station-A', 'MEDICAL,CUTTER,RESCUEKIT', 'AVAILABLE'),
('FE-02', 'Fire Engine', 'North Fire Station', 'Station-B', 'HOSE,FOAM,WATERPUMP', 'AVAILABLE'),
('RV-02', 'Rescue Van', 'North Fire Station', 'Station-B', 'MEDICAL,CUTTER,BOAT', 'AVAILABLE')
ON DUPLICATE KEY UPDATE equipment=VALUES(equipment);

-- 15 Pre-Configured Sample Incidents across Sudar's 15 Road Locations
INSERT INTO incidents (incident_code, location_name, incident_type, intensity, critical, population_exposure, required_equipment, status) VALUES
('INC-101', 'Hospital', 'ICU Electrical Fire', 5, TRUE, 3, 'FOAM', 'PENDING'),
('INC-102', 'School', 'Chemistry Lab Explosion', 4, TRUE, 3, 'FOAM', 'PENDING'),
('INC-103', 'Industrial-Estate', 'Chemical Storage Tank Blaze', 5, TRUE, 2, 'FOAM', 'PENDING'),
('INC-104', 'Market', 'Commercial Complex Fire', 4, FALSE, 3, 'HOSE', 'PENDING'),
('INC-105', 'River-Side', 'Boat Overturn & Flash Flood Trapped', 3, FALSE, 2, 'BOAT', 'PENDING'),
('INC-106', 'Warehouse', 'Timber Yard Fire', 4, FALSE, 1, 'LADDER', 'PENDING'),
('INC-107', 'Highway', 'Multi-Vehicle Pileup with Fuel Spill', 4, TRUE, 2, 'CUTTER', 'PENDING'),
('INC-108', 'Residential-Area', 'High-Rise Apartment Balcony Fire', 3, FALSE, 3, 'LADDER', 'PENDING'),
('INC-109', 'Bus-Stand', 'Commuter Bus Engine Fire', 3, FALSE, 3, 'HOSE', 'PENDING'),
('INC-110', 'College', 'Auditorium Ceiling Collapse & Fire', 3, TRUE, 2, 'MEDICAL', 'PENDING'),
('INC-111', 'Park', 'Community Gazebo Fire & Smoke', 2, FALSE, 2, 'HOSE', 'PENDING'),
('INC-112', 'Junction-1', 'Traffic Signal Transformer Blast', 3, TRUE, 2, 'WATERPUMP', 'PENDING'),
('INC-113', 'Junction-2', 'Fuel Tanker Leak Emergency', 4, TRUE, 2, 'FOAM', 'PENDING'),
('INC-114', 'Residential-Area', 'Kitchen Gas Cylinder Leakage', 2, FALSE, 2, 'MEDICAL', 'PENDING'),
('INC-115', 'Industrial-Estate', 'Machinery Jam with Trapped Worker', 3, TRUE, 1, 'CUTTER', 'PENDING')
ON DUPLICATE KEY UPDATE incident_type=VALUES(incident_type);
