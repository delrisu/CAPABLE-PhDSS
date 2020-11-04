package com.capable.physiciandss.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class ItemData {
    private String name;
    private String runtimeid;
    private String caption;
    private String description;
    private String type;
    private int value;
    private String requested;
    private String unit;
    private ArrayList<Range> range;
    private String defaultvalue;
    private String dynamic;
    private String metaprops;
}
