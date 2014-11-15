package com.android.settings;

import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Checkable;

/**
 * A hybrid of AppListPreference and SwitchPreference, representing a preference which can be on or
 * off but must have a selected value when turned on.
 *
 * It is invalid to show this preference when zero valid apps are present.
 */
public class AppListSwitchPreference extends AppListPreference {
    private static final String TAG = "AppListSwitchPref";

    private Checkable mSwitch;

    public AppListSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs, 0, R.style.AppListSwitchPreference);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        mSwitch = (Checkable) view.findViewById(com.android.internal.R.id.switchWidget);
        mSwitch.setChecked(getValue() != null);
    }

    @Override
    protected void showDialog(Bundle state) {
        if (getValue() != null) {
            // Turning off the current value.
            if (callChangeListener(null)) {
                setValue(null);
            }
        } else if (getEntryValues() == null || getEntryValues().length == 0) {
            Log.e(TAG, "Attempting to show dialog with zero entries: " + getKey());
        } else if (getEntryValues().length == 1) {
            // Suppress the dialog and just toggle the preference with the only choice.
            String value = getEntryValues()[0].toString();
            if (callChangeListener(value)) {
                setValue(value);
            }
        } else {
            super.showDialog(state);
        }
    }

    @Override
    public void setValue(String value) {
        super.setValue(value);
        if (mSwitch != null) {
            mSwitch.setChecked(value != null);
        }
    }
}
