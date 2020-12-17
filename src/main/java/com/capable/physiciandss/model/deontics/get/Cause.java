package com.capable.physiciandss.model.deontics.get;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class Cause {
    String message;
    Component component;
    Cause[] causes;
}
