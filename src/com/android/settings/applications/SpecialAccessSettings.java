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

package com.android.settings.applications;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Bundle;
import android.provider.SearchIndexableResource;
import android.support.v7.preference.Preference;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settingslib.core.AbstractPreferenceController;
import java.util.ArrayList;
import java.util.List;

public class SpecialAccessSettings extends DashboardFragment {

    private static final String TAG = "SpecialAccessSettings";

    private static final String[] DISABLED_FEATURES_LOW_RAM =
            new String[]{"notification_access", "zen_access", "enabled_vr_listeners",
                    "picture_in_picture"};

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.special_access;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        if (ActivityManager.isLowRamDeviceStatic()) {
            for (String disabledFeature : DISABLED_FEATURES_LOW_RAM) {
                Preference pref = findPreference(disabledFeature);
                if (pref != null) {
                    removePreference(disabledFeature);
                }
            }
        }
    }

    @Override
    protected List<AbstractPreferenceController> getPreferenceControllers(Context context) {
        return null;
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.SPECIAL_ACCESS;
    }

    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                        boolean enabled) {
                    final ArrayList<SearchIndexableResource> result = new ArrayList<>();

                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.special_access;
                    result.add(sir);
                    return result;
                }
            };
}
