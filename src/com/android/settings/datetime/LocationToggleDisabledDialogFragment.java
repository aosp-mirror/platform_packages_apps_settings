/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.datetime;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;

import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;

/**
 * Dialog shown when user tries to enable GeoTZ with Location toggle disabled.
 */
public class LocationToggleDisabledDialogFragment extends InstrumentedDialogFragment {

    public LocationToggleDisabledDialogFragment() {}

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.location_time_zone_detection_location_is_off_dialog_title)
                .setIcon(R.drawable.ic_warning_24dp)
                .setMessage(R.string.location_time_zone_detection_location_is_off_dialog_message)
                .setPositiveButton(
                        R.string.location_time_zone_detection_location_is_off_dialog_ok_button,
                        (dialog, which) -> {
                            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            getContext().startActivity(intent);
                        })
                .setNegativeButton(
                        R.string.location_time_zone_detection_location_is_off_dialog_cancel_button,
                        (dialog, which) -> {})
                .create();
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DIALOG_DATE_TIME_ENABLE_GEOTZ_WITH_DISABLED_LOCATION;
    }
}
