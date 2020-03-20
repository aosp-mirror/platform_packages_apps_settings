/*
 * Copyright 2019 The Android Open Source Project
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

package com.android.settings.development.compat;

import static com.android.internal.compat.OverrideAllowedState.ALLOWED;
import static com.android.internal.compat.OverrideAllowedState.DISABLED_NOT_DEBUGGABLE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.compat.Compatibility.ChangeConfig;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.os.RemoteException;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.internal.compat.CompatibilityChangeConfig;
import com.android.internal.compat.CompatibilityChangeInfo;
import com.android.internal.compat.IOverrideValidator;
import com.android.internal.compat.IPlatformCompat;
import com.android.internal.compat.OverrideAllowedState;
import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RunWith(RobolectricTestRunner.class)
public class PlatformCompatDashboardTest {
    private PlatformCompatDashboard mDashboard;

    @Mock
    private IPlatformCompat mPlatformCompat;
    private PreferenceScreen mPreferenceScreen;
    @Mock
    private ApplicationInfo mApplicationInfo;
    @Mock
    private PreferenceManager mPreferenceManager;
    @Mock
    private IOverrideValidator mOverrideValidator;
    @Mock
    private PackageManager mPackageManager;

    private Context mContext;
    private CompatibilityChangeInfo[] mChanges;
    private static final String APP_NAME = "foo.bar.baz";

    @Before
    public void setUp() throws RemoteException, NameNotFoundException {
        MockitoAnnotations.initMocks(this);
        mChanges = new CompatibilityChangeInfo[5];
        mChanges[0] = new CompatibilityChangeInfo(1L, "Default_Enabled", 0, false, false, "");
        mChanges[1] = new CompatibilityChangeInfo(2L, "Default_Disabled", 0, true, false, "");
        mChanges[2] = new CompatibilityChangeInfo(3L, "Enabled_After_SDK_1_1", 1, false, false, "");
        mChanges[3] = new CompatibilityChangeInfo(4L, "Enabled_After_SDK_1_2", 1, false, false, "");
        mChanges[4] = new CompatibilityChangeInfo(5L, "Enabled_After_SDK_2", 2, false, false, "");
        when(mPlatformCompat.listUIChanges()).thenReturn(mChanges);
        when(mPlatformCompat.getOverrideValidator()).thenReturn(mOverrideValidator);
        // By default, allow any change
        when(mOverrideValidator.getOverrideAllowedState(anyLong(),anyString()))
            .thenReturn(new OverrideAllowedState(ALLOWED, -1, -1));
        mContext = spy(RuntimeEnvironment.application);
        mPreferenceManager = new PreferenceManager(mContext);
        mPreferenceScreen = mPreferenceManager.createPreferenceScreen(mContext);
        mApplicationInfo.packageName = APP_NAME;
        mDashboard = spy(new PlatformCompatDashboard());
        mDashboard.mSelectedApp = APP_NAME;
        doReturn(mApplicationInfo).when(mDashboard).getApplicationInfo();
        doReturn(mContext).when(mDashboard).getContext();
        doReturn(mPlatformCompat).when(mDashboard).getPlatformCompat();
        doReturn(mPreferenceScreen).when(mDashboard).getPreferenceScreen();
        doReturn(mPreferenceManager).when(mDashboard).getPreferenceManager();
        doReturn(mPackageManager).when(mContext).getPackageManager();
    }

    @Test
    public void getHelpResource_shouldNotHaveHelpResource() {
        assertThat(mDashboard.getHelpResource()).isEqualTo(0);
    }

    @Test
    public void getPreferenceScreenResId_shouldBePlatformCompatSettingsResId() {
        assertThat(mDashboard.getPreferenceScreenResId())
                .isEqualTo(R.xml.platform_compat_settings);
    }

    @Test
    public void createAppPreference_targetSdkEquals1_summaryReturnsAppNameAndTargetSdk() {
        mApplicationInfo.targetSdkVersion = 1;
        Drawable icon = mock(Drawable.class);
        when(mApplicationInfo.loadIcon(any(PackageManager.class))).thenReturn(icon);

        Preference appPreference = mDashboard.createAppPreference(mApplicationInfo);

        assertThat(appPreference.getSummary()).isEqualTo(mContext.getResources().getString(
                R.string.platform_compat_selected_app_summary, APP_NAME, 1));
    }

    @Test
    public void createPreferenceForChange_defaultEnabledChange_createCheckedEntry() {
        CompatibilityChangeInfo enabledChange = mChanges[0];
        CompatibilityChangeConfig config = new CompatibilityChangeConfig(
                new ChangeConfig(new HashSet<Long>(Arrays.asList(enabledChange.getId())),
                        new HashSet<Long>()));

        Preference enabledPreference = mDashboard.createPreferenceForChange(mContext, enabledChange,
                config);

        SwitchPreference enabledSwitchPreference = (SwitchPreference) enabledPreference;

        assertThat(enabledPreference.getSummary()).isEqualTo(mChanges[0].getName());
        assertThat(enabledPreference instanceof SwitchPreference).isTrue();
        assertThat(enabledSwitchPreference.isChecked()).isTrue();
        assertThat(enabledSwitchPreference.isEnabled()).isTrue();
    }

    @Test
    public void createPreferenceForChange_defaultDisabledChange_createUncheckedEntry() {
        CompatibilityChangeInfo disabledChange = mChanges[1];
        CompatibilityChangeConfig config = new CompatibilityChangeConfig(
                new ChangeConfig(new HashSet<Long>(),
                        new HashSet<Long>(Arrays.asList(disabledChange.getId()))));

        Preference disabledPreference = mDashboard.createPreferenceForChange(mContext,
                disabledChange, config);

        assertThat(disabledPreference.getSummary()).isEqualTo(mChanges[1].getName());
        SwitchPreference disabledSwitchPreference = (SwitchPreference) disabledPreference;
        assertThat(disabledSwitchPreference.isChecked()).isFalse();
        assertThat(disabledSwitchPreference.isEnabled()).isTrue();
    }

    @Test
    public void createPreferenceForChange_cannotOverride_createDisabledEntry()
                    throws RemoteException {
        CompatibilityChangeInfo enabledChange = mChanges[0];
        CompatibilityChangeConfig config = new CompatibilityChangeConfig(
                new ChangeConfig(new HashSet<Long>(Arrays.asList(enabledChange.getId())),
                        new HashSet<Long>()));
        when(mOverrideValidator.getOverrideAllowedState(anyLong(),anyString()))
            .thenReturn(new OverrideAllowedState(DISABLED_NOT_DEBUGGABLE, -1, -1));

        Preference preference = mDashboard.createPreferenceForChange(mContext, enabledChange,
                config);

        SwitchPreference switchPreference = (SwitchPreference) preference;

        assertThat(preference.getSummary()).isEqualTo(mChanges[0].getName());
        assertThat(preference instanceof SwitchPreference).isTrue();
        assertThat(switchPreference.isChecked()).isTrue();
        assertThat(switchPreference.isEnabled()).isFalse();
    }

    @Test
    public void createChangeCategoryPreference_enabledAndDisabled_hasTitleAndEntries() {
        Set<Long> enabledChanges = new HashSet<>();
        enabledChanges.add(mChanges[0].getId());
        enabledChanges.add(mChanges[1].getId());
        enabledChanges.add(mChanges[2].getId());
        Set<Long> disabledChanges = new HashSet<>();
        disabledChanges.add(mChanges[3].getId());
        disabledChanges.add(mChanges[4].getId());
        CompatibilityChangeConfig config = new CompatibilityChangeConfig(
                new ChangeConfig(enabledChanges, disabledChanges));
        List<CompatibilityChangeInfo> changesToAdd = new ArrayList<>();
        for (int i = 0; i < mChanges.length; ++i) {
            changesToAdd.add(new CompatibilityChangeInfo(mChanges[i].getId(), mChanges[i].getName(),
                    mChanges[i].getEnableAfterTargetSdk(), mChanges[i].getDisabled(),
                    mChanges[i].getLoggingOnly(), mChanges[i].getDescription()));
        }

        PreferenceCategory category = mDashboard.createChangeCategoryPreference(changesToAdd,
        config, "foo");

        assertThat(category.getTitle()).isEqualTo("foo");
        assertThat(category.getPreferenceCount()).isEqualTo(mChanges.length);
        for (int i = 0; i < mChanges.length; ++i) {
            Preference childPreference = category.getPreference(i);
            assertThat(childPreference instanceof SwitchPreference).isTrue();
        }
    }
}
