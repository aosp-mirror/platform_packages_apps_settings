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

package com.android.settings.connecteddevice.audiosharing.audiostreams;

import static android.app.settings.SettingsEnums.AUDIO_STREAM_MAIN;

import static com.android.settings.connecteddevice.audiosharing.audiostreams.SourcePresentState.AUDIO_STREAM_SOURCE_PRESENT_STATE_SUMMARY;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.FragmentActivity;
import androidx.preference.Preference;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.ShadowFragment;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(
        shadows = {
            ShadowFragment.class,
        })
public class SourcePresentStateTest {
    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    private static final int BROADCAST_ID = 1;
    private static final String BROADCAST_TITLE = "title";
    private final Context mContext = ApplicationProvider.getApplicationContext();
    @Mock private AudioStreamPreference mPreference;
    @Mock private AudioStreamsProgressCategoryController mController;
    @Mock private AudioStreamsHelper mHelper;
    @Mock private AudioStreamsRepository mRepository;
    @Mock private AudioStreamsDashboardFragment mFragment;
    @Mock private FragmentActivity mActivity;
    private FakeFeatureFactory mFeatureFactory;
    private SourcePresentState mInstance;

    @Before
    public void setUp() {
        when(mFragment.getActivity()).thenReturn(mActivity);
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        mInstance = new SourcePresentState();
        when(mPreference.getAudioStreamBroadcastId()).thenReturn(BROADCAST_ID);
        when(mPreference.getTitle()).thenReturn(BROADCAST_TITLE);
    }

    @Test
    public void testGetInstance() {
        mInstance = SourcePresentState.getInstance();
        assertThat(mInstance).isNotNull();
        assertThat(mInstance).isInstanceOf(SourcePresentState.class);
    }

    @Test
    public void testGetSummary() {
        int summary = mInstance.getSummary();
        assertThat(summary).isEqualTo(AUDIO_STREAM_SOURCE_PRESENT_STATE_SUMMARY);
    }

    @Test
    public void testGetStateEnum() {
        AudioStreamsProgressCategoryController.AudioStreamState stateEnum =
                mInstance.getStateEnum();
        assertThat(stateEnum)
                .isEqualTo(AudioStreamsProgressCategoryController.AudioStreamState.SOURCE_PRESENT);
    }

    @Test
    public void testGetOnClickListener_startSubSettings() {
        when(mController.getFragment()).thenReturn(mFragment);
        when(mFragment.getMetricsCategory()).thenReturn(AUDIO_STREAM_MAIN);

        Preference.OnPreferenceClickListener listener = mInstance.getOnClickListener(mController);
        assertThat(listener).isNotNull();

        // mContext is not an Activity context, calling startActivity() from outside of an Activity
        // context requires the FLAG_ACTIVITY_NEW_TASK flag, create a mock to avoid this
        // AndroidRuntimeException.
        Context activityContext = mock(Context.class);
        when(mPreference.getContext()).thenReturn(activityContext);

        listener.onPreferenceClick(mPreference);

        ArgumentCaptor<Intent> argumentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(activityContext).startActivity(argumentCaptor.capture());

        Intent intent = argumentCaptor.getValue();
        assertThat(intent.getStringExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT))
                .isEqualTo(AudioStreamDetailsFragment.class.getName());
        assertThat(intent.getIntExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_TITLE_RESID, 0))
                .isEqualTo(R.string.audio_streams_detail_page_title);
        assertThat(intent.getIntExtra(MetricsFeatureProvider.EXTRA_SOURCE_METRICS_CATEGORY, 0))
                .isEqualTo(AUDIO_STREAM_MAIN);

        Bundle bundle = intent.getBundleExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS);
        assertThat(bundle).isNotNull();
        assertThat(bundle.getString(AudioStreamDetailsFragment.BROADCAST_NAME_ARG))
                .isEqualTo(BROADCAST_TITLE);
        assertThat(bundle.getInt(AudioStreamDetailsFragment.BROADCAST_ID_ARG))
                .isEqualTo(BROADCAST_ID);
    }
}
