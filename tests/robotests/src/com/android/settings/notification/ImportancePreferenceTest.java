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

package com.android.settings.notification;

import static android.app.NotificationManager.IMPORTANCE_DEFAULT;
import static android.app.NotificationManager.IMPORTANCE_LOW;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Switch;

import com.android.settings.R;
import com.android.settingslib.RestrictedLockUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

@RunWith(RobolectricTestRunner.class)
public class ImportancePreferenceTest {

    private Context mContext;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
    }

    private GradientDrawable getBackground(ImageButton button) {
        return (GradientDrawable) ((LayerDrawable) button.getDrawable())
                .findDrawableByLayerId(R.id.back);
    }

    @Test
    public void createNewPreference_shouldSetLayout() {
        final ImportancePreference preference = new ImportancePreference(mContext);
        assertThat(preference.getLayoutResource()).isEqualTo(
                R.layout.notif_importance_preference);
    }

    @Test
    public void onBindViewHolder_hideBlockNonBlockable() {
        final ImportancePreference preference = new ImportancePreference(mContext);
        final LayoutInflater inflater = LayoutInflater.from(mContext);
        final PreferenceViewHolder holder = PreferenceViewHolder.createInstanceForTests(
                inflater.inflate(R.layout.notif_importance_preference, null));

        preference.setBlockable(false);
        preference.setConfigurable(true);
        preference.setImportance(IMPORTANCE_DEFAULT);
        preference.onBindViewHolder(holder);

        assertThat(holder.itemView.findViewById(R.id.block).getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void onBindViewHolder_hideNonSelectedNonConfigurable() {
        final ImportancePreference preference = new ImportancePreference(mContext);
        final LayoutInflater inflater = LayoutInflater.from(mContext);
        PreferenceViewHolder holder = PreferenceViewHolder.createInstanceForTests(
                inflater.inflate(R.layout.notif_importance_preference, null));

        preference.setBlockable(true);
        preference.setConfigurable(false);
        preference.setImportance(IMPORTANCE_DEFAULT);
        preference.onBindViewHolder(holder);

        assertThat(holder.itemView.findViewById(R.id.block).getVisibility()).isEqualTo(View.GONE);
        assertThat(holder.itemView.findViewById(R.id.silence).getVisibility()).isEqualTo(View.GONE);
        assertThat(holder.itemView.findViewById(R.id.alert).getVisibility())
                .isEqualTo(View.VISIBLE);

        // other button
        preference.setImportance(IMPORTANCE_LOW);
        holder = PreferenceViewHolder.createInstanceForTests(
                inflater.inflate(R.layout.notif_importance_preference, null));
        preference.onBindViewHolder(holder);

        assertThat(holder.itemView.findViewById(R.id.block).getVisibility()).isEqualTo(View.GONE);
        assertThat(holder.itemView.findViewById(R.id.silence).getVisibility())
                .isEqualTo(View.VISIBLE);
        assertThat(holder.itemView.findViewById(R.id.alert).getVisibility())
                .isEqualTo(View.GONE);
    }

    @Test
    public void onBindViewHolder_selectButton() {
        final ImportancePreference preference = new ImportancePreference(mContext);
        final LayoutInflater inflater = LayoutInflater.from(mContext);
        final PreferenceViewHolder holder = PreferenceViewHolder.createInstanceForTests(
                inflater.inflate(R.layout.notif_importance_preference, null));

        preference.setBlockable(true);
        preference.setConfigurable(true);
        preference.setImportance(IMPORTANCE_DEFAULT);

        ImageButton blockButton = (ImageButton) holder.findViewById(R.id.block_icon);
        ImageButton silenceButton = (ImageButton) holder.findViewById(R.id.silence_icon);
        ImageButton alertButton = (ImageButton) holder.findViewById(R.id.alert_icon);

        preference.onBindViewHolder(holder);

        // selected has full color background. others are transparent
        assertThat(getBackground(alertButton).getColor().getColors()[0]).isNotEqualTo(
                Color.TRANSPARENT);
        assertThat(getBackground(silenceButton).getColor().getColors()[0]).isEqualTo(
                Color.TRANSPARENT);
        assertThat(getBackground(blockButton).getColor().getColors()[0]).isEqualTo(
                Color.TRANSPARENT);
    }

    @Test
    public void onClick_changesUICallsListener() {
        final ImportancePreference preference = spy(new ImportancePreference(mContext));
        final LayoutInflater inflater = LayoutInflater.from(mContext);
        final PreferenceViewHolder holder = PreferenceViewHolder.createInstanceForTests(
                inflater.inflate(R.layout.notif_importance_preference, null));

        preference.setBlockable(true);
        preference.setConfigurable(true);
        preference.setImportance(IMPORTANCE_DEFAULT);
        preference.onBindViewHolder(holder);

        ImageButton blockButton = (ImageButton) holder.findViewById(R.id.block_icon);
        ImageButton silenceButton = (ImageButton) holder.findViewById(R.id.silence_icon);
        ImageButton alertButton = (ImageButton) holder.findViewById(R.id.alert_icon);

        silenceButton.callOnClick();

        // selected has full color background. others are transparent
        assertThat(getBackground(silenceButton).getColor().getColors()[0]).isNotEqualTo(
                Color.TRANSPARENT);
        assertThat(getBackground(alertButton).getColor().getColors()[0]).isEqualTo(
                Color.TRANSPARENT);
        assertThat(getBackground(blockButton).getColor().getColors()[0]).isEqualTo(
                Color.TRANSPARENT);

        verify(preference, times(1)).callChangeListener(IMPORTANCE_LOW);
    }

    @Test
    public void onBindViewHolder_allButtonsVisible() {
        final ImportancePreference preference = new ImportancePreference(mContext);
        final LayoutInflater inflater = LayoutInflater.from(mContext);
        final PreferenceViewHolder holder = PreferenceViewHolder.createInstanceForTests(
                inflater.inflate(R.layout.notif_importance_preference, null));

        preference.setBlockable(true);
        preference.setConfigurable(true);
        preference.onBindViewHolder(holder);

        assertThat(holder.itemView.findViewById(R.id.block).getVisibility())
                .isEqualTo(View.VISIBLE);
        assertThat(holder.itemView.findViewById(R.id.silence).getVisibility())
                .isEqualTo(View.VISIBLE);
        assertThat(holder.itemView.findViewById(R.id.alert).getVisibility())
                .isEqualTo(View.VISIBLE);
    }
}
