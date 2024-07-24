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

package com.android.settings.notification.app;

import static android.app.NotificationManager.IMPORTANCE_DEFAULT;
import static android.app.NotificationManager.IMPORTANCE_LOW;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class ImportancePreferenceTest {

    private Context mContext;

    @Before
    public void setUp() {
        Context context = spy(RuntimeEnvironment.application.getApplicationContext());
        mContext = new ContextThemeWrapper(context, R.style.Theme_Settings);
    }

    @Test
    public void createNewPreference_shouldSetLayout() {
        final ImportancePreference preference = new ImportancePreference(mContext);
        assertThat(preference.getLayoutResource()).isEqualTo(
                R.layout.notif_importance_preference);
    }

    @Test
    public void onBindViewHolder_nonConfigurable() {
        final ImportancePreference preference = new ImportancePreference(mContext);
        final LayoutInflater inflater = LayoutInflater.from(mContext);
        PreferenceViewHolder holder = PreferenceViewHolder.createInstanceForTests(
                inflater.inflate(R.layout.notif_importance_preference, null));
        Drawable unselected = mock(Drawable.class);
        Drawable selected = mock(Drawable.class);
        preference.selectedBackground = selected;
        preference.unselectedBackground = unselected;

        preference.setConfigurable(false);
        preference.setImportance(IMPORTANCE_DEFAULT);
        preference.onBindViewHolder(holder);

        assertThat(holder.itemView.findViewById(R.id.silence).isEnabled()).isFalse();
        assertThat(holder.itemView.findViewById(R.id.alert).isEnabled()).isFalse();

        assertThat(holder.itemView.findViewById(R.id.alert).getBackground()).isEqualTo(selected);
        assertThat(holder.itemView.findViewById(R.id.silence).getBackground())
                .isEqualTo(unselected);

        // other button
        preference.setImportance(IMPORTANCE_LOW);
        holder = PreferenceViewHolder.createInstanceForTests(
                inflater.inflate(R.layout.notif_importance_preference, null));
        preference.onBindViewHolder(holder);

        assertThat(holder.itemView.findViewById(R.id.alert).getBackground()).isEqualTo(unselected);
        assertThat(holder.itemView.findViewById(R.id.silence).getBackground()).isEqualTo(selected);
    }

    @Test
    public void onBindViewHolder_selectButtonAndText() {
        final ImportancePreference preference = new ImportancePreference(mContext);
        final LayoutInflater inflater = LayoutInflater.from(mContext);
        final PreferenceViewHolder holder = PreferenceViewHolder.createInstanceForTests(
                inflater.inflate(R.layout.notif_importance_preference, null));
        Drawable unselected = mock(Drawable.class);
        Drawable selected = mock(Drawable.class);
        preference.selectedBackground = selected;
        preference.unselectedBackground = unselected;

        preference.setConfigurable(true);
        preference.setImportance(IMPORTANCE_DEFAULT);

        preference.onBindViewHolder(holder);

        assertThat(holder.itemView.findViewById(R.id.alert).getBackground()).isEqualTo(selected);
        assertThat(holder.itemView.findViewById(R.id.silence).getBackground())
                .isEqualTo(unselected);
        assertThat(((TextView) holder.itemView.findViewById(R.id.alert_summary)).getText())
                .isEqualTo(mContext.getString(R.string.notification_channel_summary_default));
    }

    @Test
    public void onClick_changesUICallsListener() {
        final ImportancePreference preference = spy(new ImportancePreference(mContext));
        final LayoutInflater inflater = LayoutInflater.from(mContext);
        final PreferenceViewHolder holder = PreferenceViewHolder.createInstanceForTests(
                inflater.inflate(R.layout.notif_importance_preference, null));
        Drawable unselected = mock(Drawable.class);
        Drawable selected = mock(Drawable.class);
        preference.selectedBackground = selected;
        preference.unselectedBackground = unselected;

        preference.setConfigurable(true);
        preference.setImportance(IMPORTANCE_DEFAULT);
        preference.onBindViewHolder(holder);

        View silenceButton = holder.itemView.findViewById(R.id.silence);

        silenceButton.callOnClick();

        assertThat(holder.itemView.findViewById(R.id.alert).getBackground()).isEqualTo(unselected);
        assertThat(holder.itemView.findViewById(R.id.silence).getBackground()).isEqualTo(selected);

        verify(preference, times(1)).callChangeListener(IMPORTANCE_LOW);
    }

    @Test
    public void setImportanceSummary() {
        final ImportancePreference preference = spy(new ImportancePreference(mContext));
        final LayoutInflater inflater = LayoutInflater.from(mContext);
        final PreferenceViewHolder holder = PreferenceViewHolder.createInstanceForTests(
                inflater.inflate(R.layout.notif_importance_preference, null));

        preference.setConfigurable(true);
        preference.setImportance(IMPORTANCE_DEFAULT);
        preference.onBindViewHolder(holder);

        TextView tv = holder.itemView.findViewById(R.id.silence_summary);

        preference.setDisplayInStatusBar(true);
        preference.setDisplayOnLockscreen(true);

        preference.setImportanceSummary((ViewGroup) holder.itemView, IMPORTANCE_LOW, true);

        assertThat(tv.getText()).isEqualTo(
                mContext.getString(R.string.notification_channel_summary_low));
    }

    @Test
    public void setImportanceSummary_default() {
        final ImportancePreference preference = spy(new ImportancePreference(mContext));
        final LayoutInflater inflater = LayoutInflater.from(mContext);
        final PreferenceViewHolder holder = PreferenceViewHolder.createInstanceForTests(
                inflater.inflate(R.layout.notif_importance_preference, null));

        preference.setConfigurable(true);
        preference.setImportance(IMPORTANCE_DEFAULT);
        preference.onBindViewHolder(holder);

        TextView tv = holder.itemView.findViewById(R.id.alert_summary);

        preference.setDisplayInStatusBar(true);
        preference.setDisplayOnLockscreen(true);

        preference.setImportanceSummary((ViewGroup) holder.itemView, IMPORTANCE_DEFAULT, true);

        assertThat(tv.getText()).isEqualTo(
                mContext.getString(R.string.notification_channel_summary_default));
    }
}
