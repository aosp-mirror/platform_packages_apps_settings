/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.widget;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.widget.ImageView;

import androidx.preference.PreferenceViewHolder;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.testutils.ResourcesUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/** Unittest for MutableGearPreference */
@RunWith(AndroidJUnit4.class)
public class MutableGearPreferenceTest {
    @Mock
    private MutableGearPreference.OnGearClickListener mOnGearClickListener;

    private Context mContext = ApplicationProvider.getApplicationContext();
    private MutableGearPreference mMutableGearPreference;
    private PreferenceViewHolder mViewHolder;
    private ImageView mGearView;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mMutableGearPreference =
                new MutableGearPreference(mContext, null);
        int layoutId =
                ResourcesUtils.getResourcesId(mContext, "layout", "preference_widget_gear");
        PreferenceViewHolder holder =
                PreferenceViewHolder.createInstanceForTests(
                        LayoutInflater.from(ApplicationProvider.getApplicationContext())
                                .inflate(layoutId, null));
        mViewHolder = spy(holder);
        mGearView = spy(new ImageView(mContext, null));
        int gearId = ResourcesUtils.getResourcesId(mContext, "id", "settings_button");
        when(mViewHolder.findViewById(gearId)).thenReturn(mGearView);
    }

    @Test
    public void onBindViewHolder_gearChangeAlpha() {
        mMutableGearPreference.setGearEnabled(false);
        mMutableGearPreference.setOnGearClickListener(mOnGearClickListener);

        mMutableGearPreference.onBindViewHolder(mViewHolder);

        verify(mGearView).setImageAlpha(anyInt());
    }

    private static int getDisabledAlphaValue(Context context) {
        TypedValue value = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.disabledAlpha, value, true);
        return (int) (value.getFloat() * 255);
    }
}
