package com.cqs.qrmfg.util;

import com.cqs.qrmfg.dto.WorkflowSummaryDto;
import com.cqs.qrmfg.model.MaterialWorkflow;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class WorkflowMapper {

    public WorkflowSummaryDto toSummaryDto(MaterialWorkflow workflow) {
        if (workflow == null) {
            return null;
        }

        return new WorkflowSummaryDto(
            workflow.getId(),
            workflow.getMaterialId(),
            workflow.getMaterialName(),
            workflow.getState(),
            workflow.getAssignedPlant(),
            workflow.getInitiatedBy(),
            workflow.getDaysPending(),
            workflow.getTotalQueriesCount(),
            workflow.getOpenQueriesCount(),
            workflow.getCreatedAt(),
            workflow.getLastModified(),
            workflow.isOverdue()
        );
    }

    public List<WorkflowSummaryDto> toSummaryDtoList(List<MaterialWorkflow> workflows) {
        if (workflows == null) {
            return null;
        }

        return workflows.stream()
                .map(this::toSummaryDto)
                .collect(Collectors.toList());
    }
}