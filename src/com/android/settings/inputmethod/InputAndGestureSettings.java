/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.inputmethod;

import android.content.Context;
import android.os.UserHandle;
import android.provider.SearchIndexableResource;
import android.support.annotation.VisibleForTesting;

import com.android.internal.hardware.AmbientDisplayConfiguration;
import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.core.PreferenceController;
import com.android.settings.core.lifecycle.Lifecycle;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.gestures.DoubleTapPowerPreferenceController;
import com.android.settings.gestures.DoubleTapScreenPreferenceController;
import com.android.settings.gestures.DoubleTwistPreferenceController;
import com.android.settings.gestures.PickupGesturePreferenceController;
import com.android.settings.gestures.SwipeToNotificationPreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.drawer.CategoryKey;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class InputAndGestureSettings extends DashboardFragment {

    private static final String TAG = "InputAndGestureSettings";

    private AmbientDisplayConfiguration mAmbientDisplayConfig;

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.SETTINGS_INPUT_CATEGORY;
    }

    @Override
    protected String getCategoryKey() {
        return CategoryKey.CATEGORY_SYSTEM_INPUT;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.input_and_gesture;
    }

    @Override
    protected List<PreferenceController> getPreferenceControllers(Context context) {
        final Lifecycle lifecycle = getLifecycle();
        final GameControllerPreferenceController gameControllerPreferenceController
                = new GameControllerPreferenceController(context);
        getLifecycle().addObserver(gameControllerPreferenceController);

        if (mAmbientDisplayConfig == null) {
            mAmbientDisplayConfig = new AmbientDisplayConfiguration(context);
        }
        final List<PreferenceController> controllers = new ArrayList<>();
        controllers.add(gameControllerPreferenceController);
        // Gestures

        controllers.add(new SwipeToNotificationPreferenceController(context, lifecycle));
        controllers.add(new DoubleTwistPreferenceController(context, lifecycle));
        controllers.add(new DoubleTapPowerPreferenceController(context, lifecycle));
        controllers.add(new PickupGesturePreferenceController(
                context, lifecycle, mAmbientDisplayConfig, UserHandle.myUserId()));
        controllers.add(new DoubleTapScreenPreferenceController(
                context, lifecycle, mAmbientDisplayConfig, UserHandle.myUserId()));
        return controllers;
    }

    @VisibleForTesting
    void setAmbientDisplayConfig(AmbientDisplayConfiguration ambientConfig) {
        mAmbientDisplayConfig = ambientConfig;
    }

    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(
                        Context context, boolean enabled) {
                    if (!FeatureFactory.getFactory(context).getDashboardFeatureProvider(context)
                            .isEnabled()) {
                        return null;
                    }
                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.input_and_gesture;
                    return Arrays.asList(sir);
                }
            };
}
