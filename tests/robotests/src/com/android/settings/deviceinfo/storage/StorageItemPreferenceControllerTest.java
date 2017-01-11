/*
 * Copyright (C) 2016 The Android Open Source Project
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
 * limitations under the License
 */
package com.android.settings.deviceinfo.storage;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.os.UserHandle;
import android.os.storage.VolumeInfo;
import android.provider.DocumentsContract;
import android.support.v7.preference.PreferenceScreen;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsRobolectricTestRunner;
import com.android.settings.SubSettings;
import com.android.settings.TestConfig;
import com.android.settings.applications.ManageApplications;
import com.android.settingslib.deviceinfo.StorageMeasurement;
import com.android.settingslib.deviceinfo.StorageVolumeProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.HashMap;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class StorageItemPreferenceControllerTest {
    /**
     *  In O, this will change to 1000 instead of 1024 due to the formatter properly defining a
     *  kilobyte.
     */
    private static long KILOBYTE = 1024L;

    private Context mContext;
    private VolumeInfo mVolume;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Fragment mFragment;
    @Mock
    private StorageVolumeProvider mSvp;
    private StorageItemPreferenceController mController;
    private StorageItemPreferenceAlternate mPreference;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mVolume = new VolumeInfo("id", 0, null, "id");
        mContext = RuntimeEnvironment.application;
        // Note: null is passed as the Lifecycle because we are handling it outside of the normal
        //       Settings fragment lifecycle for test purposes.
        mController = new StorageItemPreferenceController(mContext, null, mFragment, mVolume, mSvp);
        mPreference = new StorageItemPreferenceAlternate(mContext);

        // Inflate the preference and the widget.
        LayoutInflater inflater = LayoutInflater.from(mContext);
        final View view = inflater.inflate(
                mPreference.getLayoutResource(), new LinearLayout(mContext), false);
    }

    @Test
    public void testUpdateStateWithInitialState() {
        assertThat(mPreference.getSummary().toString()).isEqualTo(
                mContext.getString(R.string.memory_calculating_size));
    }

    @Test
    public void testClickPhotos() {
        mPreference.setKey("pref_photos_videos");
        mController.handlePreferenceTreeClick(mPreference);

        final ArgumentCaptor<Intent> argumentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(mFragment.getActivity()).startActivityAsUser(argumentCaptor.capture(),
                any(UserHandle.class));

        Intent intent = argumentCaptor.getValue();
        assertThat(intent.getAction()).isEqualTo(DocumentsContract.ACTION_BROWSE);
        assertThat(intent.getData()).isEqualTo(DocumentsContract.buildRootUri(
                "com.android.providers.media.documents",
                "images_root"));
    }

    @Test
    public void testClickAudio() {
        mPreference.setKey("pref_music_audio");
        mController.handlePreferenceTreeClick(mPreference);

        final ArgumentCaptor<Intent> argumentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(mFragment.getActivity()).startActivityAsUser(argumentCaptor.capture(),
                any(UserHandle.class));

        Intent intent = argumentCaptor.getValue();
        assertThat(intent.getAction()).isEqualTo(DocumentsContract.ACTION_BROWSE);
        assertThat(intent.getData()).isEqualTo(DocumentsContract.buildRootUri(
                "com.android.providers.media.documents",
                "audio_root"));
    }

    @Test
    public void testClickApps() {
        mPreference.setKey("pref_other_apps");
        mController.handlePreferenceTreeClick(mPreference);

        final ArgumentCaptor<Intent> argumentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(mFragment.getActivity()).startActivityAsUser(argumentCaptor.capture(),
                any(UserHandle.class));

        Intent intent = argumentCaptor.getValue();
        assertThat(intent.getAction()).isEqualTo(Intent.ACTION_MAIN);
        assertThat(intent.getComponent().getClassName()).isEqualTo(SubSettings.class.getName());
        assertThat(intent.getStringExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT)).isEqualTo(
                ManageApplications.class.getName());
    }

    @Test
    public void testClickFiles() {
        when(mSvp.findEmulatedForPrivate(any(VolumeInfo.class))).thenReturn(mVolume);
        mPreference.setKey("pref_files");
        mController.handlePreferenceTreeClick(mPreference);

        final ArgumentCaptor<Intent> argumentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(mFragment.getActivity()).startActivityAsUser(argumentCaptor.capture(),
                any(UserHandle.class));

        Intent intent = argumentCaptor.getValue();
        Intent browseIntent = mVolume.buildBrowseIntent();
        assertThat(intent.getAction()).isEqualTo(browseIntent.getAction());
        assertThat(intent.getData()).isEqualTo(browseIntent.getData());
    }

    @Test
    public void testMeasurementCompletedUpdatesPreferences() {
        StorageItemPreferenceAlternate audio = new StorageItemPreferenceAlternate(mContext);
        StorageItemPreferenceAlternate image = new StorageItemPreferenceAlternate(mContext);
        StorageItemPreferenceAlternate games = new StorageItemPreferenceAlternate(mContext);
        StorageItemPreferenceAlternate apps = new StorageItemPreferenceAlternate(mContext);
        StorageItemPreferenceAlternate system = new StorageItemPreferenceAlternate(mContext);
        StorageItemPreferenceAlternate files = new StorageItemPreferenceAlternate(mContext);
        PreferenceScreen screen = mock(PreferenceScreen.class);
        when(screen.findPreference(
                Mockito.eq(StorageItemPreferenceController.AUDIO_KEY))).thenReturn(audio);
        when(screen.findPreference(
                Mockito.eq(StorageItemPreferenceController.PHOTO_KEY))).thenReturn(image);
        when(screen.findPreference(
                Mockito.eq(StorageItemPreferenceController.GAME_KEY))).thenReturn(games);
        when(screen.findPreference(
                Mockito.eq(StorageItemPreferenceController.OTHER_APPS_KEY))).thenReturn(apps);
        when(screen.findPreference(
                Mockito.eq(StorageItemPreferenceController.SYSTEM_KEY))).thenReturn(system);
        when(screen.findPreference(
                Mockito.eq(StorageItemPreferenceController.FILES_KEY))).thenReturn(files);
        mController.displayPreference(screen);

        StorageMeasurement.MeasurementDetails details = new StorageMeasurement.MeasurementDetails();
        details.appsSize.put(0, KILOBYTE);
        HashMap<String, Long> mediaSizes = new HashMap<>();
        mediaSizes.put(Environment.DIRECTORY_PICTURES, KILOBYTE * 2);
        mediaSizes.put(Environment.DIRECTORY_MOVIES, KILOBYTE * 3);
        mediaSizes.put(Environment.DIRECTORY_MUSIC, KILOBYTE * 4);
        mediaSizes.put(Environment.DIRECTORY_DOWNLOADS, KILOBYTE * 5);
        details.mediaSize.put(0, mediaSizes);
        mController.setSystemSize(KILOBYTE * 6);
        mController.onDetailsChanged(details);

        assertThat(audio.getSummary().toString()).isEqualTo("4.00KB");
        assertThat(image.getSummary().toString()).isEqualTo("5.00KB");
        assertThat(games.getSummary().toString()).isEqualTo("0");
        assertThat(apps.getSummary().toString()).isEqualTo("1.00KB");
        assertThat(system.getSummary().toString()).isEqualTo("6.00KB");
        assertThat(files.getSummary().toString()).isEqualTo("5.00KB");
    }
}