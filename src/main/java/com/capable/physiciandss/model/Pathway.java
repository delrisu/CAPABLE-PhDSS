package com.capable.physiciandss.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class Pathway {
    private String name;
    private String caption;
    private String URI;
    private String revision;
    private String version;
    private String vcshash;
    private String category;
    private String coding;
    private String deleted;
    private String temp;
    private String mtime;
    private String id;
}
