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

package com.android.settings.wifi.calling;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.os.Parcelable;
import android.widget.ListAdapter;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowLooper;

@RunWith(RobolectricTestRunner.class)
public class ListWithEntrySummaryPreferenceTest {

    private Context mContext;
    private ListWithEntrySummaryPreference mPreference;

    private CharSequence[] mDefaultEntries =
            {"default_entry1", "default_entry2", "default_entry3"};
    private CharSequence[] mDefaultEntryValues = {"0", "1", "2"};
    private CharSequence[] mDefaultEntrySummaries =
            {"default_summary1", "default_summary2", "default_summary3"};

    private CharSequence[] mCustomEntries = {"custom_entry1", "custom_entry2"};
    private CharSequence[] mCustomEntryValues = {"0", "1"};
    private CharSequence[] mCustomEntrySummaries = {"custom_summary1", "custom_summary2"};

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mContext.setTheme(R.style.Theme_Settings_Home);
        ShadowLooper.pauseMainLooper();
        mPreference = new ListWithEntrySummaryPreference(mContext, null);
        mPreference.setEntries(mDefaultEntries);
        mPreference.setEntryValues(mDefaultEntryValues);
        mPreference.setEntrySummaries(mDefaultEntrySummaries);
    }

    @Test
    public void initialize_defaultEntries_shouldDisplayDefaultEntries() {
        AlertDialog dialog = showDialog(mPreference);
        ListAdapter adapter = dialog.getListView().getAdapter();

        int len = mDefaultEntries.length;
        assertThat(adapter.getCount()).isEqualTo(len);
        for (int i = 0; i < len; i++) {
            TextView title = adapter.getView(i, null, null).findViewById(R.id.title);
            TextView summary = adapter.getView(i, null, null).findViewById(R.id.summary);
            assertThat(title.getText()).isEqualTo(mDefaultEntries[i]);
            assertThat(summary.getText()).isEqualTo(mDefaultEntrySummaries[i]);
        }
    }

    @Test
    public void setEntries_customEntries_shouldUpdateEntries() {
        mPreference.setEntries(mCustomEntries);
        mPreference.setEntryValues(mCustomEntryValues);
        mPreference.setEntrySummaries(mCustomEntrySummaries);

        AlertDialog dialog = showDialog(mPreference);
        ListAdapter adapter = dialog.getListView().getAdapter();

        int len = mCustomEntries.length;
        assertThat(adapter.getCount()).isEqualTo(len);
        for (int i = 0; i < len; i++) {
            TextView title = adapter.getView(i, null, null).findViewById(R.id.title);
            TextView summary = adapter.getView(i, null, null).findViewById(R.id.summary);
            assertThat(title.getText()).isEqualTo(mCustomEntries[i]);
            assertThat(summary.getText()).isEqualTo(mCustomEntrySummaries[i]);
        }
    }

    @Test
    public void onSaveAndRestoreInstanceState_resumePreference_shouldNotChangeEntries() {
        setEntries_customEntries_shouldUpdateEntries();

        final Parcelable parcelable = mPreference.onSaveInstanceState();
        ListWithEntrySummaryPreference preference
                = new ListWithEntrySummaryPreference(mContext, null);
        preference.setEntries(mDefaultEntries);
        preference.setEntryValues(mDefaultEntryValues);
        preference.setEntrySummaries(mDefaultEntrySummaries);
        preference.onRestoreInstanceState(parcelable);

        AlertDialog dialog = showDialog(preference);
        ListAdapter adapter = dialog.getListView().getAdapter();

        int len = mCustomEntries.length;
        assertThat(adapter.getCount()).isEqualTo(len);
        for (int i = 0; i < len; i++) {
            TextView title = adapter.getView(i, null, null).findViewById(R.id.title);
            TextView summary = adapter.getView(i, null, null).findViewById(R.id.summary);
            assertThat(title.getText()).isEqualTo(mCustomEntries[i]);
            assertThat(summary.getText()).isEqualTo(mCustomEntrySummaries[i]);
        }
    }

    private AlertDialog showDialog(ListWithEntrySummaryPreference preference) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        preference.onPrepareDialogBuilder(builder, null);
        return builder.show();
    }
}
