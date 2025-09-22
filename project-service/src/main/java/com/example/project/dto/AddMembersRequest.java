package com.example.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to add multiple members to a project")
public class AddMembersRequest {

    @NotEmpty(message = "At least one member must be specified")
    @Valid
    @Schema(description = "List of members to add")
    private List<AddMemberRequest> members;
}