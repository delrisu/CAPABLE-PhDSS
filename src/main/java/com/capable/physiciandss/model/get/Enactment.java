package com.capable.physiciandss.model.get;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Enactment {
    private String patientid;
    private String pathwayid;
    private String reference;
    private String version;
    private String vcshash;
    private String groupid;
    private String ctime;
    private String mtime;
    private String atime;
    private boolean saved;
    private boolean temp;
    private String status;
    private int latestCycle;
    private String abortType;
    private String abortReason;
    private String id;
}
