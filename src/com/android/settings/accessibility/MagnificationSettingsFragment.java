/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.accessibility;

import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.Context;

import com.android.settings.DialogCreatable;
import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

/** Settings page for magnification. */
@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public class MagnificationSettingsFragment extends DashboardFragment implements
        MagnificationModePreferenceController.DialogHelper {

    private static final String TAG = "MagnificationSettingsFragment";

    private DialogCreatable mDialogDelegate;


    @Override
    public int getMetricsCategory() {
        return SettingsEnums.ACCESSIBILITY_MAGNIFICATION_SETTINGS;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        use(MagnificationModePreferenceController.class).setDialogHelper(this);
    }

    @Override
    public void showDialog(int dialogId) {
        super.showDialog(dialogId);
    }

    @Override
    public void setDialogDelegate(DialogCreatable delegate) {
        mDialogDelegate = delegate;
    }

    @Override
    public int getDialogMetricsCategory(int dialogId) {
        if (mDialogDelegate != null) {
            return mDialogDelegate.getDialogMetricsCategory(dialogId);
        }
        return 0;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.accessibility_magnification_service_settings;
    }

    @Override
    public Dialog onCreateDialog(int dialogId) {
        if (mDialogDelegate != null) {
            final Dialog dialog = mDialogDelegate.onCreateDialog(dialogId);
            if (dialog != null) {
                return dialog;
            }
        }
        throw new IllegalArgumentException("Unsupported dialogId " + dialogId);
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.accessibility_magnification_service_settings);
}
