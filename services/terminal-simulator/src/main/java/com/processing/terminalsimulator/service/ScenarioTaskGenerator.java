package com.processing.terminalsimulator.service;

import com.processing.common.dto.terminalsimulator.TerminalScenario;
import com.processing.common.dto.terminalsimulator.TransactionType;
import com.processing.terminalsimulator.model.PartofDay;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

@Component
public class ScenarioTaskGenerator {

    public record TransactionTask(TransactionType type, PartofDay partOfDay) {}

    public List<TransactionTask> generateTasks(TerminalScenario scenario, int count) {
        List<TransactionTask> tasks = new ArrayList<>();

        switch (scenario) {
            case mixed -> {
                addTask(tasks, TransactionType.NORMAL, 0, (int) (count * 0.7), PartofDay.DAY);
                addTask(tasks, TransactionType.HIGH_VALUE, (int) (count * 0.7), (int) (count * 0.7 + count * 0.15),
                        PartofDay.DAY);
                addTask(tasks, TransactionType.ALMOST_DAILY_LIMIT, (int) (count * 0.7 + count * 0.15),
                        (int) (count * 0.7 + count * 0.15 + count * 0.1), PartofDay.DAY);
                addTask(tasks, TransactionType.BLOCKED, (int) (count * 0.7 + count * 0.15 + count * 0.1), count,
                        PartofDay.DAY);
            }
            case declines_test -> {
                addTask(tasks, TransactionType.INVALID_PAN, 0, (int) (count * 0.2), PartofDay.DAY);
                addTask(tasks, TransactionType.BLOCKED, (int) (count * 0.2), (int) (count * 0.4), PartofDay.DAY);
                addTask(tasks, TransactionType.NO_MONEY, (int) (count * 0.4), (int) (count * 0.6), PartofDay.DAY);
                addTask(tasks, TransactionType.MORE_THAN_DAILY_LIMIT, (int) (count * 0.6), (int) (count * 0.8),
                        PartofDay.DAY);
                addTask(tasks, TransactionType.NORMAL, (int) (count * 0.8), count, PartofDay.DAY);
            }
            case night_time -> {
                addTask(tasks, TransactionType.NORMAL, 0, count / 2, PartofDay.NIGHT);
                addTask(tasks, TransactionType.HIGH_VALUE, count / 2, count, PartofDay.NIGHT);
            }
            case normal -> addTask(tasks,  TransactionType.NORMAL, 0, count, PartofDay.DAY);
            case high_value -> addTask(tasks, TransactionType.HIGH_VALUE, 0, count, PartofDay.DAY);
        }
        return tasks;
    }

    public TransactionTask generateSingleTask(TransactionType transactionType) {
        return new TransactionTask(transactionType, PartofDay.DAY);
    }

    public void addTask(List<TransactionTask> tasks, TransactionType type, int start, int end, PartofDay partOfDay) {
        for (int i = start; i < end; i++) {
            tasks.add(new TransactionTask(type, partOfDay));
        }
    }
}
