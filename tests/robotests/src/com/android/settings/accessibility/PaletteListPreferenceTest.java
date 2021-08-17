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

import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.preference.PreferenceViewHolder;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;

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
        mPaletteListPreference.onBindViewHolder(mPreferenceViewHolder);

        final ViewGroup viewGroup =
                mPreferenceViewHolder.itemView.findViewById(R.id.palette_view);
        final int expectedCount =
                mContext.getResources().getStringArray(R.array.setting_palette_data).length;
        assertEquals(expectedCount, viewGroup.getChildCount());
    }
}
