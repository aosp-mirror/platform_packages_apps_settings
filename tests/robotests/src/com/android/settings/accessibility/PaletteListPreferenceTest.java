/*
 * Copyright (C) 2020 The Android Open Source Project
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

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.preference.PreferenceViewHolder;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settingslib.Utils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link PaletteListPreference}. */
@RunWith(RobolectricTestRunner.class)
public final class PaletteListPreferenceTest {

    private PaletteListPreference mPaletteListPreference;
    private PreferenceViewHolder mPreferenceViewHolder;
    private final Context mContext = ApplicationProvider.getApplicationContext();

    @Before
    public void initObjects() {
        mPaletteListPreference = new PaletteListPreference(mContext, null);

        final LayoutInflater inflater = LayoutInflater.from(mContext);
        final View view =
                inflater.inflate(R.layout.daltonizer_preview, null);
        mPreferenceViewHolder = PreferenceViewHolder.createInstanceForTests(view);
    }

    @Test
    public void initPaletteView_success() {
        final int expectedCount =
                mContext.getResources().getStringArray(R.array.setting_palette_data).length;
        final ColorStateList expectedTextColor =
                Utils.getColorAttr(mContext, android.R.attr.textColorPrimary);

        mPaletteListPreference.onBindViewHolder(mPreferenceViewHolder);

        final ViewGroup viewGroup =
                mPreferenceViewHolder.itemView.findViewById(R.id.palette_view);
        final int childCount = viewGroup.getChildCount();
        assertThat(childCount).isEqualTo(expectedCount);
        for (int i = 0; i < childCount; i++) {
            final TextView textView = (TextView) viewGroup.getChildAt(i);
            assertThat(textView.getTextColors()).isEqualTo(expectedTextColor);
        }
    }
}
