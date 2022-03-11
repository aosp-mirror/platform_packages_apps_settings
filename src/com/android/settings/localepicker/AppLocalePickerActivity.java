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

package com.android.settings.localepicker;

import android.app.FragmentTransaction;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;

import com.android.internal.app.LocalePickerWithRegion;
import com.android.internal.app.LocaleStore;
import com.android.settings.R;
import com.android.settings.applications.appinfo.AppLocaleDetails;
import com.android.settings.core.SettingsBaseActivity;

/**
 * TODO(b/223503670): Add unit test for AppLocalePickerActivity.
 * A activity to show the locale picker and information page.
 */
public class AppLocalePickerActivity extends SettingsBaseActivity
        implements LocalePickerWithRegion.LocaleSelectedListener {
    private static final String TAG = AppLocalePickerActivity.class.getSimpleName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String packageName = getIntent().getData().getSchemeSpecificPart();
        if (TextUtils.isEmpty(packageName)) {
            Log.d(TAG, "There is no package name.");
            finish();
            return;
        }

        getActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.app_locale_picker);

        // Create App locale info detail part.
        AppLocaleDetails appLocaleDetails = AppLocaleDetails.newInstance(packageName);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.app_locale_detail, appLocaleDetails)
                .commit();

        // Create Locale picker part.
        final LocalePickerWithRegion selector = LocalePickerWithRegion.createLanguagePicker(
                this, AppLocalePickerActivity.this, false /* translate only */);
        // LocalePickerWithRegion use android.app.ListFragment. Thus, it can not user
        // getSupportFragmentManager() to add this into container.
        getFragmentManager()
                .beginTransaction()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .replace(R.id.app_locale_picker_with_region, selector)
                .commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            handleBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        handleBackPressed();
    }

    private void handleBackPressed() {
        if (getFragmentManager().getBackStackEntryCount() > 1) {
            super.onBackPressed();
        } else {
            setResult(RESULT_CANCELED);
            finish();
        }
    }

    @Override
    public void onLocaleSelected(LocaleStore.LocaleInfo locale) {
        // TODO When locale is selected, this shall set per app language here.
        finish();
    }
}

