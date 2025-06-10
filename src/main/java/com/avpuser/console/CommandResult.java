package com.avpuser.console;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
@Data
@AllArgsConstructor
public class CommandResult {
    private final int exitCode;
    private final String stdOutput;
    private final String errOutput;
}
