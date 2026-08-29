package com.impactledger.api.service;

import com.impactledger.api.dto.StatsResponse;
import com.impactledger.api.dto.TaskResponse;
import com.impactledger.api.entity.Task;
import com.impactledger.api.entity.enums.Complexity;
import com.impactledger.api.entity.enums.Priority;
import com.impactledger.api.entity.enums.TaskStatus;
import com.impactledger.api.repository.TaskSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.impactledger.api.repository.TaskRepository;

@Service
@RequiredArgsConstructor
public class StatsService {

    private final TaskRepository taskRepository;
    private final TaskService taskService;

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    @Transactional(readOnly = true)
    public StatsResponse computeStats(Long companyId, LocalDate startDate, LocalDate endDate) {
        var spec = TaskSpecifications.filter(companyId, null, null, null, null, startDate, endDate, null, null, null);
        List<Task> tasks = taskRepository.findAll(spec);
        return buildStats(tasks);
    }

    @Transactional(readOnly = true)
    public StatsResponse buildStats(List<Task> tasks) {
        long total = tasks.size();
        long completed = tasks.stream().filter(t -> t.getStatus() == TaskStatus.COMPLETED).count();
        long totalPrs = tasks.stream().mapToLong(t -> t.getPrLinks() != null ? t.getPrLinks().size() : 0).sum();
        long totalDocs = tasks.stream().filter(t -> t.getDesignDocLink() != null && !t.getDesignDocLink().isBlank()).count();

        Map<String, Long> byPriority = countBy(tasks, t -> t.getPriority() != null ? t.getPriority().name() : "UNSET", Priority.class);
        Map<String, Long> byComplexity = countBy(tasks, t -> t.getComplexity() != null ? t.getComplexity().name() : "UNSET", Complexity.class);
        Map<String, Long> byStatus = countBy(tasks, t -> t.getStatus() != null ? t.getStatus().name() : "UNSET", TaskStatus.class);

        Map<String, Long> byTaskType = new LinkedHashMap<>();
        tasks.forEach(t -> {
            if (t.getTaskTypes() != null) {
                t.getTaskTypes().forEach(type -> byTaskType.merge(type, 1L, Long::sum));
            }
        });

        Map<String, Long> byTech = new LinkedHashMap<>();
        tasks.forEach(t -> {
            if (t.getTechStack() != null) {
                t.getTechStack().forEach(tech -> byTech.merge(tech, 1L, Long::sum));
            }
        });

        Map<String, Long> byMonth = new TreeMap<>();
        tasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.COMPLETED)
                .forEach(t -> {
                    LocalDate ref = t.getEndDate() != null ? t.getEndDate() : t.getStartDate();
                    if (ref != null) {
                        byMonth.merge(ref.format(MONTH_FMT), 1L, Long::sum);
                    }
                });

        Map<String, Long> heatmap = new TreeMap<>();
        tasks.forEach(t -> {
            LocalDate ref = t.getStartDate();
            if (ref != null) {
                heatmap.merge(ref.toString(), 1L, Long::sum);
            }
        });

        List<TaskResponse> highlights = tasks.stream()
                .filter(Task::isHighlight)
                .sorted(Comparator.comparing(Task::getEndDate, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .map(this::toResponseSafe)
                .collect(Collectors.toList());

        return StatsResponse.builder()
                .totalTasks(total)
                .completedTasks(completed)
                .totalPrs(totalPrs)
                .totalDesignDocs(totalDocs)
                .byPriority(byPriority)
                .byComplexity(byComplexity)
                .byStatus(byStatus)
                .byTaskType(byTaskType)
                .byTechStack(byTech)
                .tasksCompletedByMonth(byMonth)
                .activityHeatmap(heatmap)
                .highlightedTasks(highlights)
                .build();
    }

    private <E extends Enum<E>> Map<String, Long> countBy(List<Task> tasks, java.util.function.Function<Task, String> keyFn, Class<E> enumClass) {
        Map<String, Long> map = new LinkedHashMap<>();
        for (E e : enumClass.getEnumConstants()) {
            map.put(e.name(), 0L);
        }
        tasks.forEach(t -> map.merge(keyFn.apply(t), 1L, Long::sum));
        return map;
    }

    private TaskResponse toResponseSafe(Task t) {
        return taskService.getById(t.getId());
    }
}