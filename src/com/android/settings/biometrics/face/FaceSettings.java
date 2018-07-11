/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.biometrics.face;

import static com.android.settings.biometrics.BiometricEnrollBase.CONFIRM_REQUEST;

import android.content.Context;
import android.hardware.face.FaceManager;
import android.os.Bundle;
import android.provider.SearchIndexableResource;
import android.util.Log;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.password.ChooseLockSettingsHelper;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.search.SearchIndexable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Settings screen for face authentication.
 */
@SearchIndexable
public class FaceSettings extends DashboardFragment {

    private static final String TAG = "FaceSettings";
    private static final String KEY_LAUNCHED_CONFIRM = "key_launched_confirm";

    private boolean mLaunchedConfirm;

    public static boolean isAvailable(Context context) {
        FaceManager manager = Utils.getFaceManagerOrNull(context);
        return manager != null && manager.isHardwareDetected();
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.FACE;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.security_settings_face;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_LAUNCHED_CONFIRM, mLaunchedConfirm);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mLaunchedConfirm = savedInstanceState.getBoolean(KEY_LAUNCHED_CONFIRM, false);
        }

        if (!mLaunchedConfirm) {
            ChooseLockSettingsHelper helper = new ChooseLockSettingsHelper(getActivity(), this);
            if (!helper.launchConfirmationActivity(CONFIRM_REQUEST,
                    getString(R.string.security_settings_face_preference_title))) {
                Log.e(TAG, "Password not set");
                finish();
            }
        }
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return buildPreferenceControllers(context, getSettingsLifecycle());
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(Context context,
            Lifecycle lifecycle) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        controllers.add(new FaceSettingsVideoPreferenceController(context));
        controllers.add(new FaceSettingsImprovePreferenceController(context));
        controllers.add(new FaceSettingsUnlockPreferenceController(context));
        controllers.add(new FaceSettingsRemoveButtonPreferenceController(context));
        controllers.add(new FaceSettingsFooterPreferenceController(context));
        return controllers;
    }

    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(
                        Context context, boolean enabled) {
                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.security_settings_face;
                    return Arrays.asList(sir);
                }

                @Override
                public List<AbstractPreferenceController> createPreferenceControllers(
                        Context context) {
                    return buildPreferenceControllers(context, null /* lifecycle */);
                }

                @Override
                protected boolean isPageSearchEnabled(Context context) {
                    return isAvailable(context);
                }
            };

}
