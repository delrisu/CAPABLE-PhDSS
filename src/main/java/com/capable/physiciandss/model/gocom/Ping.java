package com.capable.physiciandss.model.gocom;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.lang.ref.Reference;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Ping {
    Reference medicationRequestReference;
}
