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

package com.android.settings.datausage;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.telephony.SubscriptionInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class CellDataPreferenceTest {

    @Mock
    private SubscriptionInfo mSubInfo;

    private Context mContext;
    private CellDataPreference mPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = ApplicationProvider.getApplicationContext();
        mPreference = new CellDataPreference(mContext, null) {
            @Override
            SubscriptionInfo getActiveSubscriptionInfo(int subId) {
                return mSubInfo;
            }
        };

        final LayoutInflater inflater = LayoutInflater.from(mContext);
        final View view = inflater.inflate(mPreference.getLayoutResource(),
                new LinearLayout(mContext), false);
    }

    @Test
    public void updateEnabled_noActiveSub_shouldDisable() {
        mSubInfo = null;

        mPreference.mOnSubscriptionsChangeListener.onChanged();

        assertThat(mPreference.isEnabled()).isFalse();
    }

    @Test
    public void updateEnabled_hasActiveSub_shouldEnable() {
        mPreference.mOnSubscriptionsChangeListener.onChanged();

        assertThat(mPreference.isEnabled()).isTrue();
    }
}
