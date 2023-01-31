/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.connecteddevice.stylus;

import android.content.Context;
import android.hardware.BatteryState;
import android.hardware.input.InputManager;
import android.os.Bundle;
import android.view.InputDevice;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnCreate;
import com.android.settingslib.core.lifecycle.events.OnDestroy;
import com.android.settingslib.widget.LayoutPreference;

import java.text.NumberFormat;

/**
 * This class adds a header for USI stylus devices with a heading, icon, and battery level.
 * As opposed to the bluetooth device headers, this USI header gets its battery values
 * from {@link InputManager} APIs, rather than the bluetooth battery levels.
 */
public class StylusUsiHeaderController extends BasePreferenceController implements
        InputManager.InputDeviceBatteryListener, LifecycleObserver, OnCreate, OnDestroy {

    private static final String KEY_STYLUS_USI_HEADER = "stylus_usi_header";
    private static final String TAG = StylusUsiHeaderController.class.getSimpleName();

    private final InputManager mInputManager;
    private final InputDevice mInputDevice;

    private LayoutPreference mHeaderPreference;


    public StylusUsiHeaderController(Context context, InputDevice inputDevice) {
        super(context, KEY_STYLUS_USI_HEADER);
        mInputDevice = inputDevice;
        mInputManager = context.getSystemService(InputManager.class);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        mHeaderPreference = screen.findPreference(getPreferenceKey());
        View view = mHeaderPreference.findViewById(R.id.entity_header);
        TextView titleView = view.findViewById(R.id.entity_header_title);
        titleView.setText(R.string.stylus_connected_devices_title);

        ImageView iconView = mHeaderPreference.findViewById(R.id.entity_header_icon);
        if (iconView != null) {
            // TODO(b/250909304): get proper icon once VisD ready
            iconView.setImageResource(R.drawable.ic_edit);
            iconView.setContentDescription("Icon for stylus");
        }
        refresh();
        super.displayPreference(screen);
    }

    @Override
    public void updateState(Preference preference) {
        refresh();
    }

    private void refresh() {
        BatteryState batteryState = mInputDevice.getBatteryState();
        View view = mHeaderPreference.findViewById(R.id.entity_header);
        TextView summaryView = view.findViewById(R.id.entity_header_summary);

        if (isValidBatteryState(batteryState)) {
            summaryView.setVisibility(View.VISIBLE);
            summaryView.setText(
                    NumberFormat.getPercentInstance().format(batteryState.getCapacity()));
        } else {
            summaryView.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * This determines if a battery state is 'stale', as indicated by the presence of
     * battery values.
     *
     * A USI battery state is valid (and present) if a USI battery value has been pulled
     * within the last 1 hour of a stylus touching/hovering on the screen. The header shows
     * battery values in this case, Conversely, a stale battery state means no USI battery
     * value has been detected within the last 1 hour. Thus, the USI stylus preference will
     * not be shown in Settings, and accordingly, the USI battery state won't surface.
     *
     * @param batteryState Latest battery state pulled from the kernel
     */
    private boolean isValidBatteryState(BatteryState batteryState) {
        return batteryState != null
                && batteryState.isPresent()
                && batteryState.getCapacity() > 0f;
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_STYLUS_USI_HEADER;
    }

    @Override
    public void onBatteryStateChanged(int deviceId, long eventTimeMillis,
            @NonNull BatteryState batteryState) {
        refresh();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mInputManager.addInputDeviceBatteryListener(mInputDevice.getId(),
                mContext.getMainExecutor(), this);
    }

    @Override
    public void onDestroy() {
        mInputManager.removeInputDeviceBatteryListener(mInputDevice.getId(),
                this);
    }
}
