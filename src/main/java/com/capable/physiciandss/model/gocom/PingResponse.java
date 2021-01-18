package com.capable.physiciandss.model.gocom;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Model wiadomości odbieranej od serwisu GoCom, zawiera informację o tym, czy serwis rozwiązał jakieś konflikty
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PingResponse {
    boolean ifResolvedConflict;
}
