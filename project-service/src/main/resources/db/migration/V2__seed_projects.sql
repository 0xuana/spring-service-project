-- Insert sample projects
INSERT INTO project.projects (code, name, description, status, start_date, end_date) VALUES
('EMP-MGMT', 'Employee Management System', 'A comprehensive system for managing employee data and processes', 'ACTIVE', '2024-01-01', '2024-12-31'),
('API-GW', 'API Gateway Implementation', 'Implementation of centralized API gateway for microservices architecture', 'ACTIVE', '2024-02-01', '2024-06-30'),
('CONFIG-SRV', 'Config Server Setup', 'Centralized configuration management for all microservices', 'COMPLETED', '2024-01-15', '2024-03-15'),
('TEST-AUTO', 'Test Automation Framework', 'Automated testing framework for all microservices', 'PLANNED', '2024-04-01', '2024-08-31'),
('DOC-PORTAL', 'Documentation Portal', 'Centralized documentation portal for development team', 'ON_HOLD', '2024-03-01', NULL),
('PERF-OPT', 'Performance Optimization', 'System-wide performance optimization initiative', 'PLANNED', '2024-05-01', '2024-07-31');

-- Insert sample project members (using hypothetical employee IDs)
-- Note: These employee IDs should exist in the employee service
INSERT INTO project.project_members (project_id, employee_id, role, allocation_percent) VALUES
-- EMP-MGMT project members
(1, 1, 'Project Manager', 100),
(1, 2, 'Senior Developer', 80),
(1, 3, 'Frontend Developer', 90),
(1, 4, 'Backend Developer', 85),

-- API-GW project members
(2, 2, 'Lead Developer', 75),
(2, 5, 'DevOps Engineer', 60),
(2, 6, 'Senior Developer', 70),

-- CONFIG-SRV project members (completed project)
(3, 1, 'Project Lead', 50),
(3, 5, 'DevOps Engineer', 80),

-- TEST-AUTO project members
(4, 7, 'QA Lead', 100),
(4, 8, 'Test Automation Engineer', 90),
(4, 3, 'Developer', 30),

-- DOC-PORTAL project members (on hold, minimal allocation)
(5, 9, 'Technical Writer', 50),
(5, 1, 'Project Coordinator', 25),

-- PERF-OPT project members
(6, 2, 'Performance Architect', 60),
(6, 4, 'Backend Specialist', 40),
(6, 10, 'Database Specialist', 80);