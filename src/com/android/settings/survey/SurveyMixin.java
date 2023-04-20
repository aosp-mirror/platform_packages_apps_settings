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
package com.android.settings.survey;

import android.app.Activity;

import androidx.fragment.app.Fragment;

import com.android.settings.overlay.FeatureFactory;
import com.android.settings.overlay.SurveyFeatureProvider;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnResume;

/**
 * attaches extra, survey related work to the onResume method of registered observable classes
 * in settings. This allows new classes to automatically support settings provided the extend
 * one of the relevant classes in com.android.settings.lifecycle.
 */
public class SurveyMixin implements LifecycleObserver, OnResume {

    private String mName;
    private Fragment mFragment;

    /**
     * A mixin that attempts to perform survey related tasks right before onResume is called
     * in a Settings PreferenceFragment. This will allow for remote updating and creation of
     * surveys.
     *
     * @param fragment     The fragment that this mixin will be attached to.
     * @param fragmentName The simple name of the fragment.
     */
    public SurveyMixin(Fragment fragment, String fragmentName) {
        mName = fragmentName;
        mFragment = fragment;
    }

    @Override
    public void onResume() {
        Activity activity = mFragment.getActivity();

        // guard against the activity not existing yet
        if (activity != null) {
            SurveyFeatureProvider provider =
                    FeatureFactory.getFactory(activity).getSurveyFeatureProvider(activity);
            if (provider != null) {
                provider.sendActivityIfAvailable(mName);
            }
        }
    }
}
