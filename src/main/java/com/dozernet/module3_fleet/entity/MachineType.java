package com.dozernet.module3_fleet.entity;

/**
 * Categories of JCB / construction machine available on the platform.
 */
public enum MachineType {
    BACKHOE_LOADER("Backhoe Loader"),
    EXCAVATOR("Excavator"),
    WHEEL_LOADER("Wheel Loader"),
    SKID_STEER("Skid Steer Loader"),
    TELEHANDLER("Telehandler"),
    COMPACTOR("Compactor / Roller"),
    BULLDOZER("Bulldozer");

    private final String displayName;

    MachineType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
