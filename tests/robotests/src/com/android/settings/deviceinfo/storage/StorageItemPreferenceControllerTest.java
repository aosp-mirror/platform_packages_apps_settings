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

import static com.android.settings.applications.manageapplications.ManageApplications.EXTRA_WORK_ID;
import static com.android.settings.utils.FileSizeFormatter.GIGABYTE_IN_BYTES;
import static com.android.settings.utils.FileSizeFormatter.KILOBYTE_IN_BYTES;
import static com.android.settings.utils.FileSizeFormatter.MEGABYTE_IN_BYTES;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;
import android.os.storage.VolumeInfo;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SubSettings;
import com.android.settings.applications.manageapplications.ManageApplications;
import com.android.settings.dashboard.profileselector.ProfileSelectFragment;
import com.android.settings.dashboard.profileselector.ProfileSelectFragment.ProfileType;
import com.android.settings.deviceinfo.StorageItemPreference;
import com.android.settings.testutils.shadow.ShadowUserManager;
import com.android.settingslib.deviceinfo.StorageVolumeProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        com.android.settings.testutils.shadow.ShadowFragment.class,
})
public class StorageItemPreferenceControllerTest {

    private Context mContext;
    private VolumeInfo mVolume;
    @Mock
    private Fragment mFragment;
    @Mock
    private StorageVolumeProvider mSvp;
    @Mock
    private FragmentActivity mActivity;
    @Mock
    private FragmentManager mFragmentManager;
    @Mock
    private FragmentTransaction mFragmentTransaction;
    private StorageItemPreferenceController mController;
    private StorageItemPreference mPreference;
    private PreferenceScreen mPreferenceScreen;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mFragment.getActivity()).thenReturn(mActivity);
        when(mFragment.getFragmentManager()).thenReturn(mFragmentManager);
        when(mFragmentManager.beginTransaction()).thenReturn(mFragmentTransaction);
        mContext = spy(RuntimeEnvironment.application.getApplicationContext());
        mVolume = spy(new VolumeInfo("id", 0, null, "id"));
        // Note: null is passed as the Lifecycle because we are handling it outside of the normal
        //       Settings fragment lifecycle for test purposes.
        mController = new StorageItemPreferenceController(mContext, mFragment, mVolume, mSvp,
                ProfileSelectFragment.ProfileType.PERSONAL);
        mPreference = new StorageItemPreference(mContext);

        // Inflate the preference and the widget.
        final LayoutInflater inflater = LayoutInflater.from(mContext);
        inflater.inflate(mPreference.getLayoutResource(), new LinearLayout(mContext), false);

        mPreferenceScreen = getPreferenceScreen();
    }

    private PreferenceScreen getPreferenceScreen() {
        final StorageItemPreference publicStorage = spy(new StorageItemPreference(mContext));
        publicStorage.setIcon(R.drawable.ic_folder_vd_theme_24);
        final StorageItemPreference images = spy(new StorageItemPreference(mContext));
        images.setIcon(R.drawable.ic_photo_library);
        final StorageItemPreference videos = spy(new StorageItemPreference(mContext));
        videos.setIcon(R.drawable.ic_local_movies);
        final StorageItemPreference audio = spy(new StorageItemPreference(mContext));
        audio.setIcon(R.drawable.ic_media_stream);
        final StorageItemPreference apps = spy(new StorageItemPreference(mContext));
        apps.setIcon(R.drawable.ic_storage_apps);
        final StorageItemPreference games = spy(new StorageItemPreference(mContext));
        games.setIcon(R.drawable.ic_videogame_vd_theme_24);
        final StorageItemPreference documentsAndOther = spy(new StorageItemPreference(mContext));
        documentsAndOther.setIcon(R.drawable.ic_folder_vd_theme_24);
        final StorageItemPreference system = spy(new StorageItemPreference(mContext));
        system.setIcon(com.android.settingslib.R.drawable.ic_system_update);
        final StorageItemPreference trash = spy(new StorageItemPreference(mContext));
        trash.setIcon(R.drawable.ic_trash_can);

        final PreferenceScreen screen = mock(PreferenceScreen.class);
        when(screen.findPreference(eq(StorageItemPreferenceController.PUBLIC_STORAGE_KEY)))
                .thenReturn(publicStorage);
        when(screen.findPreference(eq(StorageItemPreferenceController.IMAGES_KEY)))
                .thenReturn(images);
        when(screen.findPreference(eq(StorageItemPreferenceController.VIDEOS_KEY)))
                .thenReturn(videos);
        when(screen.findPreference(eq(StorageItemPreferenceController.AUDIO_KEY)))
                .thenReturn(audio);
        when(screen.findPreference(eq(StorageItemPreferenceController.APPS_KEY)))
                .thenReturn(apps);
        when(screen.findPreference(eq(StorageItemPreferenceController.GAMES_KEY)))
                .thenReturn(games);
        when(screen.findPreference(eq(StorageItemPreferenceController.DOCUMENTS_AND_OTHER_KEY)))
                .thenReturn(documentsAndOther);
        when(screen.findPreference(eq(StorageItemPreferenceController.SYSTEM_KEY)))
                .thenReturn(system);
        when(screen.findPreference(eq(StorageItemPreferenceController.TRASH_KEY)))
                .thenReturn(trash);

        return screen;
    }

    @Test
    public void launchPublicStorageIntent_nonNullBrowseIntent_settingsIntent() {
        final String fakeBrowseAction = "FAKE_BROWSE_ACTION";
        final Intent fakeBrowseIntent = new Intent(fakeBrowseAction);
        // mContext is not the activity, add FLAG_ACTIVITY_NEW_TASK to avoid AndroidRuntimeException
        // during this test.
        fakeBrowseIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        when(mVolume.buildBrowseIntent()).thenReturn(fakeBrowseIntent);
        mPreference.setKey(StorageItemPreferenceController.PUBLIC_STORAGE_KEY);
        mController.handlePreferenceTreeClick(mPreference);

        final ArgumentCaptor<Intent> argumentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(mContext).startActivityAsUser(argumentCaptor.capture(), nullable(UserHandle.class));

        final Intent intent = argumentCaptor.getValue();
        assertThat(intent.getAction()).isEqualTo(fakeBrowseAction);
    }

    @Test
    public void launchImagesIntent_resolveActionViewNull_settingsIntent() {
        mPreference.setKey(StorageItemPreferenceController.IMAGES_KEY);
        final Context mockContext = getMockContext();
        mController = new StorageItemPreferenceController(mockContext, mFragment, mVolume,
                mSvp, ProfileSelectFragment.ProfileType.PERSONAL);
        mController.handlePreferenceTreeClick(mPreference);

        final ArgumentCaptor<Intent> argumentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(mockContext).startActivityAsUser(argumentCaptor.capture(),
                nullable(UserHandle.class));

        final Intent intent = argumentCaptor.getValue();
        assertThat(intent.getAction()).isEqualTo(Intent.ACTION_VIEW);
        assertThat(intent.getData()).isEqualTo(mController.mImagesUri);
    }

    @Test
    public void launchAudioIntent_resolveActionViewNull_settingsIntent() {
        mPreference.setKey(StorageItemPreferenceController.AUDIO_KEY);
        final Context mockContext = getMockContext();
        mController = new StorageItemPreferenceController(mockContext, mFragment, mVolume,
                mSvp, ProfileSelectFragment.ProfileType.PERSONAL);
        mController.handlePreferenceTreeClick(mPreference);

        final ArgumentCaptor<Intent> argumentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(mockContext).startActivityAsUser(argumentCaptor.capture(),
                nullable(UserHandle.class));
        final Intent intent = argumentCaptor.getValue();

        assertThat(intent.getAction()).isEqualTo(Intent.ACTION_VIEW);
        assertThat(intent.getData()).isEqualTo(mController.mAudioUri);
    }

    @Test
    public void setVolume_nullVolume_hidePreferences() {
        mController.displayPreference(mPreferenceScreen);

        mController.setVolume(null);

        assertThat(mController.mPublicStoragePreference.isVisible()).isFalse();
        assertThat(mController.mImagesPreference.isVisible()).isFalse();
        assertThat(mController.mVideosPreference.isVisible()).isFalse();
        assertThat(mController.mAudioPreference.isVisible()).isFalse();
        assertThat(mController.mAppsPreference.isVisible()).isFalse();
        assertThat(mController.mGamesPreference.isVisible()).isFalse();
        assertThat(mController.mDocumentsAndOtherPreference.isVisible()).isFalse();
        assertThat(mController.mSystemPreference.isVisible()).isFalse();
        assertThat(mController.mTrashPreference.isVisible()).isFalse();
    }

    @Test
    public void launchAppsIntent_forPersonal_settingsIntent() {
        mPreference.setKey(StorageItemPreferenceController.APPS_KEY);
        mController.handlePreferenceTreeClick(mPreference);

        final ArgumentCaptor<Intent> argumentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(mFragment.getActivity()).startActivityAsUser(argumentCaptor.capture(),
                nullable(UserHandle.class));

        final Intent intent = argumentCaptor.getValue();
        assertThat(intent.getAction()).isEqualTo(Intent.ACTION_MAIN);
        assertThat(intent.getComponent().getClassName()).isEqualTo(SubSettings.class.getName());
        assertThat(intent.getStringExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT))
            .isEqualTo(ManageApplications.class.getName());
        assertThat(intent.getIntExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_TITLE_RESID, 0))
            .isEqualTo(R.string.apps_storage);
    }

    @Test
    public void launchAppsIntent_forWork_settingsIntent() {
        mController = new FakeStorageItemPreferenceController(mContext, mFragment, mVolume, mSvp,
                ProfileType.WORK);
        mPreference.setKey(StorageItemPreferenceController.APPS_KEY);
        mController.handlePreferenceTreeClick(mPreference);

        final ArgumentCaptor<Intent> argumentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(mFragment.getActivity()).startActivityAsUser(argumentCaptor.capture(),
                nullable(UserHandle.class));

        Intent intent = argumentCaptor.getValue();
        assertThat(intent.getAction()).isEqualTo(Intent.ACTION_MAIN);
        assertThat(intent.getComponent().getClassName()).isEqualTo(SubSettings.class.getName());
        assertThat(intent.getStringExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT))
                .isEqualTo(ManageApplications.class.getName());
        assertThat(intent.getIntExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_TITLE_RESID, 0))
                .isEqualTo(R.string.apps_storage);
        assertThat(
                intent.getBundleExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS)
                        .getInt(SettingsActivity.EXTRA_SHOW_FRAGMENT_TAB))
                .isEqualTo(ProfileSelectFragment.WORK_TAB);
        assertThat(
                intent.getBundleExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS)
                        .getInt(EXTRA_WORK_ID))
                .isEqualTo(0);
    }

    @Test
    public void launchDocumentsAndOtherIntent_resolveActionViewNull_settingsIntent() {
        mPreference.setKey(StorageItemPreferenceController.DOCUMENTS_AND_OTHER_KEY);
        final Context mockContext = getMockContext();
        mController = new StorageItemPreferenceController(mockContext, mFragment, mVolume,
                mSvp, ProfileSelectFragment.ProfileType.PERSONAL);
        mController.handlePreferenceTreeClick(mPreference);

        final ArgumentCaptor<Intent> argumentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(mockContext).startActivityAsUser(argumentCaptor.capture(),
                nullable(UserHandle.class));

        Intent intent = argumentCaptor.getValue();
        assertThat(intent.getAction()).isEqualTo(Intent.ACTION_VIEW);
        assertThat(intent.getData()).isEqualTo(mController.mDocumentsAndOtherUri);
    }

    @Test
    public void launchGamesIntent_settingsIntent() {
        mPreference.setKey(StorageItemPreferenceController.GAMES_KEY);
        mController.handlePreferenceTreeClick(mPreference);

        final ArgumentCaptor<Intent> argumentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(mFragment.getActivity()).startActivityAsUser(argumentCaptor.capture(),
                nullable(UserHandle.class));

        Intent intent = argumentCaptor.getValue();
        assertThat(intent.getAction()).isEqualTo(Intent.ACTION_MAIN);
        assertThat(intent.getComponent().getClassName()).isEqualTo(SubSettings.class.getName());
        assertThat(intent.getStringExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT))
            .isEqualTo(ManageApplications.class.getName());
        assertThat(intent.getIntExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_TITLE_RESID, 0))
            .isEqualTo(R.string.game_storage_settings);
    }

    @Test
    public void launchVideosIntent_resolveActionViewNull_settingsIntent() {
        mPreference.setKey(StorageItemPreferenceController.VIDEOS_KEY);
        final Context mockContext = getMockContext();
        mController = new StorageItemPreferenceController(mockContext, mFragment, mVolume,
                mSvp, ProfileSelectFragment.ProfileType.PERSONAL);
        mController.handlePreferenceTreeClick(mPreference);

        final ArgumentCaptor<Intent> argumentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(mockContext).startActivityAsUser(argumentCaptor.capture(),
                nullable(UserHandle.class));

        Intent intent = argumentCaptor.getValue();
        assertThat(intent.getAction()).isEqualTo(Intent.ACTION_VIEW);
        assertThat(intent.getData()).isEqualTo(mController.mVideosUri);
    }

    @Test
    public void testClickSystem() {
        mPreference.setKey(StorageItemPreferenceController.SYSTEM_KEY);
        assertThat(mController.handlePreferenceTreeClick(mPreference)).isTrue();

        verify(mFragment.getFragmentManager().beginTransaction())
            .add(nullable(StorageUtils.SystemInfoFragment.class), nullable(String.class));
    }

    @Test
    @Config(shadows = ShadowUserManager.class)
    public void testMeasurementCompletedUpdatesPreferences() {
        mController.displayPreference(mPreferenceScreen);

        mController.setUsedSize(MEGABYTE_IN_BYTES * 970); // There should 870MB attributed.
        final StorageAsyncLoader.StorageResult result = new StorageAsyncLoader.StorageResult();
        result.gamesSize = MEGABYTE_IN_BYTES * 80;
        result.imagesSize = MEGABYTE_IN_BYTES * 350;
        result.videosSize = GIGABYTE_IN_BYTES * 30;
        result.audioSize = MEGABYTE_IN_BYTES * 40;
        result.documentsAndOtherSize = MEGABYTE_IN_BYTES * 50;
        result.trashSize = KILOBYTE_IN_BYTES * 100;
        result.allAppsExceptGamesSize = MEGABYTE_IN_BYTES * 90;

        final SparseArray<StorageAsyncLoader.StorageResult> results = new SparseArray<>();
        results.put(0, result);
        mController.onLoadFinished(results, 0);

        assertThat(mController.mImagesPreference.getSummary().toString()).isEqualTo("350 MB");
        assertThat(mController.mVideosPreference.getSummary().toString()).isEqualTo("30 GB");
        assertThat(mController.mAudioPreference.getSummary().toString()).isEqualTo("40 MB");
        assertThat(mController.mAppsPreference.getSummary().toString()).isEqualTo("90 MB");
        assertThat(mController.mGamesPreference.getSummary().toString()).isEqualTo("80 MB");
        assertThat(mController.mDocumentsAndOtherPreference.getSummary().toString())
                .isEqualTo("50 MB");
        assertThat(mController.mTrashPreference.getSummary().toString()).isEqualTo("100 kB");
    }

    @Test
    public void settingUserIdAppliesNewIcons() {
        mController.displayPreference(mPreferenceScreen);

        mController.setUserId(new UserHandle(10));

        verify(mController.mPublicStoragePreference, times(2)).setIcon(nullable(Drawable.class));
        verify(mController.mImagesPreference, times(2)).setIcon(nullable(Drawable.class));
        verify(mController.mVideosPreference, times(2)).setIcon(nullable(Drawable.class));
        verify(mController.mAudioPreference, times(2)).setIcon(nullable(Drawable.class));
        verify(mController.mAppsPreference, times(2)).setIcon(nullable(Drawable.class));
        verify(mController.mGamesPreference, times(2)).setIcon(nullable(Drawable.class));
        verify(mController.mDocumentsAndOtherPreference, times(2))
                .setIcon(nullable(Drawable.class));
        verify(mController.mSystemPreference, times(2)).setIcon(nullable(Drawable.class));
        verify(mController.mTrashPreference, times(2)).setIcon(nullable(Drawable.class));
    }

    @Test
    public void displayPreference_dontHideFilePreferenceWhenEmulatedInternalStorageUsed() {
        when(mSvp.findEmulatedForPrivate(nullable(VolumeInfo.class))).thenReturn(mVolume);
        when(mVolume.getType()).thenReturn(VolumeInfo.TYPE_PRIVATE);
        when(mVolume.getState()).thenReturn(VolumeInfo.STATE_MOUNTED);
        when(mVolume.isMountedReadable()).thenReturn(true);

        mController.displayPreference(mPreferenceScreen);

        assertThat(mController.mDocumentsAndOtherPreference.isVisible()).isTrue();
    }

    @Test
    public void setPrivateStorageCategoryPreferencesVisibility_updateFilePreferenceToHideAfterSettingVolume_hidePreference() {
        when(mSvp.findEmulatedForPrivate(nullable(VolumeInfo.class))).thenReturn(mVolume);
        when(mVolume.getType()).thenReturn(VolumeInfo.TYPE_PRIVATE);
        when(mVolume.getState()).thenReturn(VolumeInfo.STATE_MOUNTED);
        when(mVolume.isMountedReadable()).thenReturn(true);
        mController.displayPreference(mPreferenceScreen);
        when(mSvp.findEmulatedForPrivate(nullable(VolumeInfo.class))).thenReturn(null);
        mController.setVolume(mVolume);

        mController.setPrivateStorageCategoryPreferencesVisibility(true);

        assertThat(mController.mDocumentsAndOtherPreference.isVisible()).isFalse();
    }

    @Test
    public void setVolume_updateFilePreferenceToShowAfterSettingVolume_showPreference() {
        // This will hide it initially.
        mController.displayPreference(mPreferenceScreen);
        when(mSvp.findEmulatedForPrivate(nullable(VolumeInfo.class))).thenReturn(mVolume);
        when(mVolume.getType()).thenReturn(VolumeInfo.TYPE_PRIVATE);
        when(mVolume.getState()).thenReturn(VolumeInfo.STATE_MOUNTED);
        when(mVolume.isMountedReadable()).thenReturn(true);

        // And we bring it back.
        mController.setVolume(mVolume);

        assertThat(mController.mDocumentsAndOtherPreference.isVisible()).isTrue();
    }

    @Test
    public void setVolume_publicStorage_showFilePreference() {
        // This will hide it initially.
        mController.displayPreference(mPreferenceScreen);
        when(mVolume.getType()).thenReturn(VolumeInfo.TYPE_PUBLIC);
        when(mVolume.getState()).thenReturn(VolumeInfo.STATE_MOUNTED);
        when(mVolume.isMountedReadable()).thenReturn(true);

        // And we bring it back.
        mController.setVolume(mVolume);

        assertThat(mController.mPublicStoragePreference.isVisible()).isTrue();
        assertThat(mController.mImagesPreference.isVisible()).isFalse();
        assertThat(mController.mVideosPreference.isVisible()).isFalse();
        assertThat(mController.mAudioPreference.isVisible()).isFalse();
        assertThat(mController.mAppsPreference.isVisible()).isFalse();
        assertThat(mController.mGamesPreference.isVisible()).isFalse();
        assertThat(mController.mDocumentsAndOtherPreference.isVisible()).isFalse();
        assertThat(mController.mSystemPreference.isVisible()).isFalse();
        assertThat(mController.mTrashPreference.isVisible()).isFalse();
    }

    /**
     * To verify startActivity, these test cases use mock Context because mContext is not an
     * activity context and AndroidRuntimeException throws for no FLAG_ACTIVITY_NEW_TASK.
     */
    private Context getMockContext() {
        final Resources resources = mock(Resources.class);
        final Context context = mock(Context.class);
        when(context.getResources()).thenReturn(resources);
        when(resources.getString(anyInt())).thenReturn("");
        return context;
    }

    private static class FakeStorageItemPreferenceController
            extends StorageItemPreferenceController {

        private static final int CURRENT_USER_ID = 10;

        FakeStorageItemPreferenceController(Context context, Fragment hostFragment,
                VolumeInfo volume, StorageVolumeProvider svp, @ProfileType int profileType) {
            super(context, hostFragment, volume, svp, profileType);
        }

        @Override
        int getCurrentUserId() {
            return CURRENT_USER_ID;
        }
    }
}
