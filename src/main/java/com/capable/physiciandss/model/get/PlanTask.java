package com.capable.physiciandss.model.get;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class PlanTask {
    private String name;
    private int runtimeid;
    private String caption;
    private String description;
    private JsonNode metaprops;
    private String type;
    private String state;
    private String context;
    private boolean canconfirm;
    private boolean optional;
    private boolean autonomous;
    private long parentid;
    private String in_progresstime;
    private String completedstime;
    private String discardedtime;
    private String procedure;
}
