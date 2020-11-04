package com.capable.physiciandss.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class PlanTasks {
    private ArrayList<PlanTask> planTasks;
}
