/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.search;

import static com.android.settings.SettingsActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS;
import static com.android.settings.SettingsActivity.EXTRA_SHOW_FRAGMENT_TAB;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.android.settings.SettingsActivity;
import com.android.settings.SubSettings;
import com.android.settings.overlay.FeatureFactory;

/**
 * A trampoline activity that launches setting result page.
 */
public class SearchResultTrampoline extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // First make sure caller has privilege to launch a search result page.
        FeatureFactory.getFactory(this)
                .getSearchFeatureProvider()
                .verifyLaunchSearchResultPageCaller(this, getCallingActivity());
        // Didn't crash, proceed and launch the result as a subsetting.
        final Intent intent = getIntent();

        // Hack to take EXTRA_FRAGMENT_ARG_KEY from intent and set into
        // EXTRA_SHOW_FRAGMENT_ARGUMENTS. This is necessary because intent could be from external
        // caller and args may not persisted.
        final String settingKey = intent.getStringExtra(SettingsActivity.EXTRA_FRAGMENT_ARG_KEY);
        final int tab = intent.getIntExtra(EXTRA_SHOW_FRAGMENT_TAB, 0);
        final Bundle args = new Bundle();
        args.putString(SettingsActivity.EXTRA_FRAGMENT_ARG_KEY, settingKey);
        args.putInt(EXTRA_SHOW_FRAGMENT_TAB, tab);
        intent.putExtra(EXTRA_SHOW_FRAGMENT_ARGUMENTS, args);

        // Reroute request to SubSetting.
        intent.setClass(this /* context */, SubSettings.class)
                .addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
        startActivity(intent);

        // Done.
        finish();
    }

}
