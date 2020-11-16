package com.capable.physiciandss.model.put;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DataValueOutput {
    private String name;
    private boolean success;
    private String newvalue;
    private String errorcode;
    private String errormessage;

}
