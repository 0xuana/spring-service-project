package com.example.project.exception;

public class ProjectHasMembersException extends RuntimeException {
    public ProjectHasMembersException(String message) {
        super(message);
    }

    public static ProjectHasMembersException forProject(Long projectId, int memberCount) {
        return new ProjectHasMembersException("Cannot delete project " + projectId + " because it has " + memberCount + " active members. Remove all members first.");
    }
}