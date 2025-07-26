package com.cqs.qrmfg.util;

import com.cqs.qrmfg.dto.QuerySummaryDto;
import com.cqs.qrmfg.model.Query;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class QueryMapper {

    public QuerySummaryDto toSummaryDto(Query query) {
        if (query == null) {
            return null;
        }

        return new QuerySummaryDto(
            query.getId(),
            query.getWorkflow() != null ? query.getWorkflow().getMaterialCode() : null,
            query.getWorkflow() != null ? query.getWorkflow().getMaterialName() : null,
            query.getWorkflow() != null ? query.getWorkflow().getAssignedPlant() : null,
            query.getStepNumber(),
            query.getFieldName(),
            query.getQuestion(),
            query.getResponse(),
            query.getAssignedTeam(),
            query.getStatus(),
            query.getRaisedBy(),
            query.getResolvedBy(),
            query.getPriorityLevel(),
            query.getCreatedAt(),
            query.getResolvedAt(),
            query.getDaysOpen(),
            query.isOverdue(),
            query.isHighPriority()
        );
    }

    public List<QuerySummaryDto> toSummaryDtoList(List<Query> queries) {
        if (queries == null) {
            return null;
        }

        return queries.stream()
                .map(this::toSummaryDto)
                .collect(Collectors.toList());
    }
}