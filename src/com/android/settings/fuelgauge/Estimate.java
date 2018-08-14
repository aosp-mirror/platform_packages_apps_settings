package com.android.settings.fuelgauge;

public class Estimate {

    // Value to indicate averageTimeToDischarge could not be obtained
    public static final int AVERAGE_TIME_TO_DISCHARGE_UNKNOWN = -1;

    public final long estimateMillis;
    public final boolean isBasedOnUsage;
    public final long averageDischargeTime;

    public Estimate(long estimateMillis, boolean isBasedOnUsage,
            long averageDischargeTime) {
        this.estimateMillis = estimateMillis;
        this.isBasedOnUsage = isBasedOnUsage;
        this.averageDischargeTime = averageDischargeTime;
    }
}
