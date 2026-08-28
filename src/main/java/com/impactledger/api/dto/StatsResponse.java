package com.impactledger.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatsResponse {
    private long totalTasks;
    private long completedTasks;
    private long totalPrs;
    private long totalDesignDocs;

    private Map<String, Long> byPriority;
    private Map<String, Long> byComplexity;
    private Map<String, Long> byStatus;
    private Map<String, Long> byTaskType;
    private Map<String, Long> byTechStack;

    // key = "YYYY-MM"
    private Map<String, Long> tasksCompletedByMonth;

    // day -> count, for the contribution heatmap ("YYYY-MM-DD" -> count)
    private Map<String, Long> activityHeatmap;

    private List<TaskResponse> highlightedTasks;
}
