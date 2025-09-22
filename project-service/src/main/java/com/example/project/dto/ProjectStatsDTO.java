package com.example.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Project statistics")
public class ProjectStatsDTO {

    @Schema(description = "Total number of projects", example = "25")
    private long totalProjects;

    @Schema(description = "Project count by status")
    private Map<String, Long> countByStatus;

    @Schema(description = "Project count by month (for groupBy=month)")
    private Map<String, Long> countByMonth;
}