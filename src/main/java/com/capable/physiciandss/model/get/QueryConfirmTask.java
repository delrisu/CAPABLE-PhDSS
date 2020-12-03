package com.capable.physiciandss.model.get;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class QueryConfirmTask {
    Precondition precondition;
    Cause[] causes;

}
