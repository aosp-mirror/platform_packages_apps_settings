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

package com.android.settings.development.qstile;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.RemoteException;
import android.service.quicksettings.TileService;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.internal.statusbar.IStatusBarService;
import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowPackageManager;
import org.robolectric.util.ReflectionHelpers;

import java.util.Arrays;

@RunWith(RobolectricTestRunner.class)
public class DevelopmentTilePreferenceControllerTest {

    private static final String SERVICE_INFO_NAME = "TestName";
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private IStatusBarService mStatusBarService;
    private Context mContext;
    private DevelopmentTilePreferenceController mController;
    private ShadowPackageManager mShadowPackageManager;
    private DevelopmentTilePreferenceController.OnChangeHandler mOnChangeHandler;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mShadowPackageManager = Shadows.shadowOf(mContext.getPackageManager());
        when(mScreen.getContext()).thenReturn(mContext);

        mController = new DevelopmentTilePreferenceController(mContext, "testkey");
        mOnChangeHandler = spy(new DevelopmentTilePreferenceController.OnChangeHandler(mContext));
        ReflectionHelpers.setField(mOnChangeHandler, "mStatusBarService", mStatusBarService);
    }

    @Test
    public void isAlwaysAvailable() {
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void display_hasTileService_shouldDisplay() {
        final Intent tileProbe = new Intent(TileService.ACTION_QS_TILE)
                .setPackage(mContext.getPackageName());
        final ResolveInfo info = new ResolveInfo();
        info.serviceInfo = new FakeServiceInfo();
        info.serviceInfo.name = "abc";
        info.serviceInfo.icon = R.drawable.ic_settings_24dp;
        info.serviceInfo.packageName = mContext.getPackageName();
        mShadowPackageManager.setResolveInfosForIntent(tileProbe, Arrays.asList(info));

        mController.displayPreference(mScreen);

        verify(mScreen, atLeastOnce()).addPreference(any(Preference.class));
    }

    @Test
    public void preferenceChecked_shouldAddTile() throws RemoteException {
        SwitchPreference preference = createPreference(/* defaultCheckedState = */ false);
        preference.performClick();

        ArgumentCaptor<ComponentName> argument = ArgumentCaptor.forClass(ComponentName.class);
        verify(mStatusBarService).addTile(argument.capture());
        assertThat(argument.getValue().getClassName()).isEqualTo(SERVICE_INFO_NAME);
        assertThat(argument.getValue().getPackageName()).isEqualTo(mContext.getPackageName());
    }

    @Test
    public void preferenceUnchecked_shouldRemoveTile() throws RemoteException {
        SwitchPreference preference = createPreference(/* defaultCheckedState = */ true);
        preference.performClick();

        ArgumentCaptor<ComponentName> argument = ArgumentCaptor.forClass(ComponentName.class);
        verify(mStatusBarService).remTile(argument.capture());
        assertThat(argument.getValue().getClassName()).isEqualTo(SERVICE_INFO_NAME);
        assertThat(argument.getValue().getPackageName()).isEqualTo(mContext.getPackageName());
    }

    private SwitchPreference createPreference(boolean defaultCheckedState) {
        SwitchPreference preference = new SwitchPreference(mContext);
        preference.setTitle("Test Pref");
        preference.setIcon(R.drawable.ic_settings_24dp);
        preference.setKey(SERVICE_INFO_NAME);
        preference.setChecked(defaultCheckedState);
        preference.setOnPreferenceChangeListener(mOnChangeHandler);
        return preference;
    }

    private static class FakeServiceInfo extends ServiceInfo {

        public String loadLabel(PackageManager mgr) {
            return "hi";
        }
    }
}
