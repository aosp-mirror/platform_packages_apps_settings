package com.android.settings.fuelgauge.anomaly;

import android.content.Context;
import android.support.v7.preference.Preference;

import com.android.settings.R;

/**
 * Preference that stores {@link Anomaly}
 */
public class AnomalyPreference extends Preference {
    private Anomaly mAnomaly;

    public AnomalyPreference(Context context, Anomaly anomaly) {
        super(context);
        mAnomaly = anomaly;

        if (anomaly != null) {
            setTitle(anomaly.displayName);
        }
    }

    public Anomaly getAnomaly() {
        return mAnomaly;
    }

}
