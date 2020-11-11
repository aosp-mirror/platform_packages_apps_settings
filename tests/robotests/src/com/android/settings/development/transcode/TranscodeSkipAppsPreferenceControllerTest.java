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

package com.android.settings.development.transcode;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.SystemProperties;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowPackageManager;

import java.util.Collections;

@RunWith(RobolectricTestRunner.class)
public class TranscodeSkipAppsPreferenceControllerTest {

    private static final int APPLICATION_UID = 1234;
    private static final String SKIP_SELECTED_APPS_PROP_KEY =
            "persist.sys.fuse.transcode_skip_uids";

    @Mock
    private PreferenceScreen mScreen;
    private Context mContext;
    private ShadowPackageManager mShadowPackageManager;
    private TranscodeSkipAppsPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = ApplicationProvider.getApplicationContext();
        mShadowPackageManager = Shadows.shadowOf(mContext.getPackageManager());
        mController = new TranscodeSkipAppsPreferenceController(mContext, "test_key");
        Preference preference = new Preference(mContext);

        when(mScreen.getContext()).thenReturn(mContext);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(preference);
    }

    @Test
    public void isAlwaysAvailable() {
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void display_hasLaunchAbleApps_shouldDisplay() {
        Intent launcherIntent = new Intent(Intent.ACTION_MAIN);
        launcherIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        final ResolveInfo resolveInfo = new FakeResolveInfo(mContext);
        ApplicationInfo applicationInfo = new ApplicationInfo();
        applicationInfo.uid = APPLICATION_UID;
        ActivityInfo activityInfo = new ActivityInfo();
        activityInfo.applicationInfo = applicationInfo;
        resolveInfo.activityInfo = activityInfo;
        mShadowPackageManager.setResolveInfosForIntent(launcherIntent,
                Collections.singletonList(resolveInfo));

        mController.displayPreference(mScreen);

        verify(mScreen, atLeastOnce()).addPreference(any(Preference.class));
    }

    @Test
    public void preferenceChecked_shouldSkipApp() {
        // First ensure that the app is not in skip list.
        SystemProperties.set(SKIP_SELECTED_APPS_PROP_KEY, String.valueOf(-1));
        SwitchPreference switchPreference = createPreference(/* defaultCheckedState = */ false);

        switchPreference.performClick();

        // Verify that the app is added to skip list.
        assertThat(SystemProperties.get(SKIP_SELECTED_APPS_PROP_KEY)).contains(
                String.valueOf(APPLICATION_UID));
    }

    @Test
    public void preferenceUnchecked_shouldNotSkipApp() {
        // First ensure that the app is in skip list.
        SystemProperties.set(SKIP_SELECTED_APPS_PROP_KEY, String.valueOf(APPLICATION_UID));
        SwitchPreference switchPreference = createPreference(/* defaultCheckedState = */ true);

        switchPreference.performClick();

        // Verify that the app is removed from skip list.
        assertThat(SystemProperties.get(SKIP_SELECTED_APPS_PROP_KEY)).doesNotContain(
                String.valueOf(APPLICATION_UID));
    }

    private SwitchPreference createPreference(boolean defaultCheckedState) {
        SwitchPreference preference = new SwitchPreference(mContext);
        preference.setTitle("Test Pref");
        preference.setIcon(R.drawable.ic_settings_24dp);
        preference.setKey(String.valueOf(APPLICATION_UID));
        preference.setChecked(defaultCheckedState);
        preference.setOnPreferenceChangeListener(mController);
        return preference;
    }

    private static class FakeResolveInfo extends ResolveInfo {

        private final Context mContext;

        FakeResolveInfo(Context context) {
            this.mContext = context;
        }

        @Override
        public CharSequence loadLabel(PackageManager pm) {
            return "TestName";
        }

        @Override
        public Drawable loadIcon(PackageManager pm) {
            return mContext.getDrawable(R.drawable.ic_settings_24dp);
        }
    }
}
