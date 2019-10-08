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

package com.android.settings.development.featureflags;

import android.content.Context;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.widget.FooterPreferenceMixinCompat;

public class FeatureFlagFooterPreferenceController extends BasePreferenceController
        implements LifecycleObserver, OnStart {

    private FooterPreferenceMixinCompat mFooterMixin;

    public FeatureFlagFooterPreferenceController(Context context) {
        super(context, "feature_flag_footer_pref");
    }

    public void setFooterMixin(FooterPreferenceMixinCompat mixin) {
        mFooterMixin = mixin;
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void onStart() {
        mFooterMixin.createFooterPreference()
                .setTitle(R.string.experimental_category_title);
    }
}
