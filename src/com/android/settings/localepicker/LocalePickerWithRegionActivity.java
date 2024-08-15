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

import static android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT;

import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.os.LocaleList;
import android.provider.Settings;
import android.util.Log;
import android.view.MenuItem;
import android.window.OnBackInvokedCallback;

import androidx.core.view.ViewCompat;

import com.android.internal.app.LocalePickerWithRegion;
import com.android.internal.app.LocaleStore;
import com.android.settings.R;
import com.android.settings.core.SettingsBaseActivity;

/** A activity to show the locale picker page. */
public class LocalePickerWithRegionActivity extends SettingsBaseActivity
        implements LocalePickerWithRegion.LocaleSelectedListener, MenuItem.OnActionExpandListener {
    private static final String TAG = LocalePickerWithRegionActivity.class.getSimpleName();
    private static final String PARENT_FRAGMENT_NAME = "localeListEditor";
    private static final String CHILD_FRAGMENT_NAME = "LocalePickerWithRegion";

    private LocalePickerWithRegion mSelector;

    private final OnBackInvokedCallback mOnBackInvokedCallback = () -> {
        handleBackPressed();
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        setTitle(R.string.add_a_language);
        LocaleList explicitLocales = null;
        if (isDeviceDemoMode()) {
            Bundle bundle = getIntent().getExtras();
            explicitLocales = bundle == null
                    ? null
                    : bundle.getParcelable(Settings.EXTRA_EXPLICIT_LOCALES, LocaleList.class);
            Log.i(TAG, "Has explicit locales : " + explicitLocales);
        }
        getOnBackInvokedDispatcher()
                .registerOnBackInvokedCallback(PRIORITY_DEFAULT, mOnBackInvokedCallback);
        mSelector = LocalePickerWithRegion.createLanguagePicker(
                this,
                LocalePickerWithRegionActivity.this,
                false /* translate only */,
                explicitLocales,
                null /* appPackageName */,
                this);

        if (getFragmentManager().findFragmentByTag(CHILD_FRAGMENT_NAME) == null) {
            getFragmentManager()
                    .beginTransaction()
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .replace(R.id.content_frame, mSelector, CHILD_FRAGMENT_NAME)
                    .addToBackStack(PARENT_FRAGMENT_NAME)
                    .commit();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(mOnBackInvokedCallback);
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

    private void handleBackPressed() {
        if (getFragmentManager().getBackStackEntryCount() > 1) {
            super.onBackPressed();
        } else {
            setResult(RESULT_CANCELED);
            finish();
        }
    }

    private boolean isDeviceDemoMode() {
        return Settings.Global.getInt(
                getContentResolver(), Settings.Global.DEVICE_DEMO_MODE, 0) == 1;
    }

    @Override
    public boolean onMenuItemActionExpand(MenuItem item) {
        // To prevent a large space on tool bar.
        mAppBarLayout.setExpanded(false /*expanded*/, false /*animate*/);
        // To prevent user can expand the collpasing tool bar view.
        ViewCompat.setNestedScrollingEnabled(mSelector.getListView(), false);
        return true;
    }

    @Override
    public boolean onMenuItemActionCollapse(MenuItem item) {
        mAppBarLayout.setExpanded(false /*expanded*/, false /*animate*/);
        ViewCompat.setNestedScrollingEnabled(mSelector.getListView(), true);
        return true;
    }
}

