package com.example.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response for bulk member addition operation")
public class AddMembersResponse {

    @Schema(description = "Total number of members processed", example = "5")
    private int totalProcessed;

    @Schema(description = "Number of successful additions", example = "3")
    private int successCount;

    @Schema(description = "Number of failed additions", example = "2")
    private int errorCount;

    @Schema(description = "Detailed results for each member")
    private List<MemberOperationResult> results;
}