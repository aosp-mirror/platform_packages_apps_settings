/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.android.settings.display;

import android.content.Context;
import android.content.res.Configuration;
import android.support.v7.preference.DropDownPreference;
import android.support.v7.preference.Preference;

import com.android.internal.logging.nano.MetricsProto;
import com.android.internal.view.RotationPolicy;
import com.android.settings.R;
import com.android.settings.core.PreferenceController;
import com.android.settings.core.instrumentation.MetricsFeatureProvider;
import com.android.settings.overlay.FeatureFactory;

public class AutoRotatePreferenceController extends PreferenceController implements
        Preference.OnPreferenceChangeListener {

    private static final String KEY_AUTO_ROTATE = "auto_rotate";
    private final MetricsFeatureProvider mMetricsFeatureProvider;

    public AutoRotatePreferenceController(Context context) {
        super(context);
        mMetricsFeatureProvider = FeatureFactory.getFactory(context).getMetricsFeatureProvider();
    }

    @Override
    public String getPreferenceKey() {
        return KEY_AUTO_ROTATE;
    }

    @Override
    public void updateState(Preference preference) {
        final DropDownPreference rotatePreference = (DropDownPreference) preference;
        final int rotateLockedResourceId;
        preference.setSummary("%s");
        // The following block sets the string used when rotation is locked.
        // If the device locks specifically to portrait or landscape (rather than current
        // rotation), then we use a different string to include this information.
        if (allowAllRotations()) {
            rotateLockedResourceId = R.string.display_auto_rotate_stay_in_current;
        } else {
            if (RotationPolicy.getRotationLockOrientation(mContext)
                    == Configuration.ORIENTATION_PORTRAIT) {
                rotateLockedResourceId = R.string.display_auto_rotate_stay_in_portrait;
            } else {
                rotateLockedResourceId = R.string.display_auto_rotate_stay_in_landscape;
            }
        }
        rotatePreference.setEntries(new CharSequence[]{
                mContext.getString(R.string.display_auto_rotate_rotate),
                mContext.getString(rotateLockedResourceId),
        });
        rotatePreference.setEntryValues(new CharSequence[]{"0", "1"});
        rotatePreference.setValueIndex(RotationPolicy.isRotationLocked(mContext) ?
                1 : 0);
    }

    @Override
    public boolean isAvailable() {
        return RotationPolicy.isRotationLockToggleVisible(mContext);
    }

    private boolean allowAllRotations() {
        return mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_allowAllRotations);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean locked = Integer.parseInt((String) newValue) != 0;
        mMetricsFeatureProvider.action(mContext, MetricsProto.MetricsEvent.ACTION_ROTATION_LOCK,
                locked);
        RotationPolicy.setRotationLock(mContext, locked);
        return true;
    }
}
