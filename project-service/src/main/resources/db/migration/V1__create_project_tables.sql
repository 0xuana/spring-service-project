-- Create project schema if it doesn't exist
CREATE SCHEMA IF NOT EXISTS project;

-- Create projects table
CREATE TABLE project.projects (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create project_members table
CREATE TABLE project.project_members (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL,
    employee_id BIGINT NOT NULL,
    role VARCHAR(60) NOT NULL,
    allocation_percent INTEGER NOT NULL CHECK (allocation_percent >= 0 AND allocation_percent <= 100),
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (project_id) REFERENCES project.projects(id) ON DELETE CASCADE,
    UNIQUE (project_id, employee_id)
);

-- Create indexes for better performance
CREATE INDEX idx_projects_status ON project.projects(status);
CREATE INDEX idx_projects_start_date ON project.projects(start_date);
CREATE INDEX idx_projects_end_date ON project.projects(end_date);
CREATE INDEX idx_projects_code ON project.projects(code);
CREATE INDEX idx_project_members_project_id ON project.project_members(project_id);
CREATE INDEX idx_project_members_employee_id ON project.project_members(employee_id);
CREATE INDEX idx_project_members_role ON project.project_members(role);

-- Add constraint to ensure end_date >= start_date
ALTER TABLE project.projects ADD CONSTRAINT chk_project_dates
    CHECK (end_date IS NULL OR end_date >= start_date);

-- Create function to update updated_at timestamp
CREATE OR REPLACE FUNCTION project.update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create trigger to automatically update updated_at
CREATE TRIGGER update_projects_updated_at
    BEFORE UPDATE ON project.projects
    FOR EACH ROW
    EXECUTE FUNCTION project.update_updated_at_column();