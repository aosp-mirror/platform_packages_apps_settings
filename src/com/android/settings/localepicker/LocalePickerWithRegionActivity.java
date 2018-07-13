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

package com.android.settings.localepicker;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import com.android.internal.app.LocalePickerWithRegion;
import com.android.internal.app.LocaleStore;

public class LocalePickerWithRegionActivity extends Activity
        implements LocalePickerWithRegion.LocaleSelectedListener {

    private static final String PARENT_FRAGMENT_NAME = "localeListEditor";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        final LocalePickerWithRegion selector = LocalePickerWithRegion.createLanguagePicker(
                this, LocalePickerWithRegionActivity.this, false /* translate only */);
        getFragmentManager()
                .beginTransaction()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .replace(android.R.id.content, selector)
                .addToBackStack(PARENT_FRAGMENT_NAME)
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
    public void onLocaleSelected(LocaleStore.LocaleInfo locale) {
        final Intent intent = new Intent();
        intent.putExtra(LocaleListEditor.INTENT_LOCALE_KEY, locale);
        setResult(RESULT_OK, intent);
        finish();
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
}

