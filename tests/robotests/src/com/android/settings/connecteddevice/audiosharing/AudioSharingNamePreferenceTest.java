/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.connecteddevice.audiosharing;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.preference.PreferenceViewHolder;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.connecteddevice.audiosharing.audiostreams.AudioStreamsQrCodeFragment;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class AudioSharingNamePreferenceTest {
    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    private Context mContext;
    private AudioSharingNamePreference mPreference;

    @Before
    public void setup() {
        mContext = ApplicationProvider.getApplicationContext();
        mPreference = spy(new AudioSharingNamePreference(mContext, null));
    }

    @Test
    public void initialize_correctLayout() {
        assertThat(mPreference.getLayoutResource())
                .isEqualTo(
                        com.android.settingslib.widget.preference.twotarget.R.layout
                                .preference_two_target);
        assertThat(mPreference.getWidgetLayoutResource())
                .isEqualTo(R.layout.preference_widget_qrcode);
    }

    @Test
    public void onBindViewHolder_correctLayout_noQrCodeButton() {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(mPreference.getLayoutResource(), null);
        LinearLayout widgetView = view.findViewById(android.R.id.widget_frame);
        assertThat(widgetView).isNotNull();
        inflater.inflate(mPreference.getWidgetLayoutResource(), widgetView, true);

        var holder = PreferenceViewHolder.createInstanceForTests(view);
        mPreference.setShowQrCodeIcon(false);
        mPreference.onBindViewHolder(holder);

        ImageButton shareButton = (ImageButton) holder.findViewById(R.id.button_icon);
        View divider =
                holder.findViewById(
                        com.android.settingslib.widget.preference.twotarget.R.id
                                .two_target_divider);

        assertThat(shareButton).isNotNull();
        assertThat(shareButton.getVisibility()).isEqualTo(View.GONE);
        assertThat(shareButton.hasOnClickListeners()).isFalse();
        assertThat(divider).isNotNull();
        assertThat(divider.getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void onBindViewHolder_correctLayout_showQrCodeButton() {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(mPreference.getLayoutResource(), null);
        LinearLayout widgetView = view.findViewById(android.R.id.widget_frame);
        assertThat(widgetView).isNotNull();
        inflater.inflate(mPreference.getWidgetLayoutResource(), widgetView, true);

        var holder = PreferenceViewHolder.createInstanceForTests(view);
        mPreference.setShowQrCodeIcon(true);
        mPreference.onBindViewHolder(holder);

        ImageButton shareButton = (ImageButton) holder.findViewById(R.id.button_icon);
        View divider =
                holder.findViewById(
                        com.android.settingslib.widget.preference.twotarget.R.id
                                .two_target_divider);

        assertThat(shareButton).isNotNull();
        assertThat(shareButton.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(shareButton.getDrawable()).isNotNull();
        assertThat(shareButton.hasOnClickListeners()).isTrue();
        assertThat(shareButton.getContentDescription()).isNotNull();
        assertThat(divider).isNotNull();
        assertThat(divider.getVisibility()).isEqualTo(View.VISIBLE);

        // mContext is not an Activity context, calling startActivity() from outside of an Activity
        // context requires the FLAG_ACTIVITY_NEW_TASK flag, create a mock to avoid this
        // AndroidRuntimeException.
        Context activityContext = mock(Context.class);
        when(mPreference.getContext()).thenReturn(activityContext);
        shareButton.callOnClick();

        ArgumentCaptor<Intent> argumentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(activityContext).startActivity(argumentCaptor.capture());

        Intent intent = argumentCaptor.getValue();
        assertThat(intent.getStringExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT))
                .isEqualTo(AudioStreamsQrCodeFragment.class.getName());
        assertThat(intent.getIntExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_TITLE_RESID, 0))
                .isEqualTo(R.string.audio_streams_qr_code_page_title);
        assertThat(intent.getIntExtra(MetricsFeatureProvider.EXTRA_SOURCE_METRICS_CATEGORY, 0))
                .isEqualTo(SettingsEnums.AUDIO_SHARING_SETTINGS);
    }
}
