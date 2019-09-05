/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.wifi;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.preference.PreferenceViewHolder;

import com.android.settings.LinkifyUtils;
import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class LinkablePreferenceTest {

    private static final String TITLE = "Title";

    private Context mContext = RuntimeEnvironment.application;

    private LinkablePreference mPreference;
    private PreferenceViewHolder mHolder;
    private View mView;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mPreference = new LinkablePreference(mContext);
        final CharSequence linkableDescription = mContext.getText(R.string.wifi_scan_notify_text);
        final LinkifyUtils.OnClickListener clickListener = () -> {/* Do nothing */ };
        mPreference.setText(TITLE, linkableDescription, clickListener);

        LayoutInflater inflater = LayoutInflater.from(mContext);
        mView =
            inflater.inflate(mPreference.getLayoutResource(), new LinearLayout(mContext), false);
        mHolder = PreferenceViewHolder.createInstanceForTests(mView);

        mPreference.onBindViewHolder(mHolder);
    }

    @Test
    public void prefWithLinkShouldHaveAccessibilityMovementMethodSet() {
        TextView textView = mView.findViewById(android.R.id.title);
        assertThat(textView).isNotNull();
        assertThat(textView.getMovementMethod()).isNotNull();
    }
}
