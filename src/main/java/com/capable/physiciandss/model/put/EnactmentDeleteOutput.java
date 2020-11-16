package com.capable.physiciandss.model.put;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EnactmentDeleteOutput {
    private String id;
    private String deleted;
}
