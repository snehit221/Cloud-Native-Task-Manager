package com.snehit.springboottaskapi.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Enumeration of possible task statuses")
public enum TaskStatus {
    @Schema(description = "Task has not started")
    TODO("To Do"),
    @Schema(description = "Task is the current action item")
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed");
    @Schema(description = "Task has been completed")

    private final String displayName;

    TaskStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

}
