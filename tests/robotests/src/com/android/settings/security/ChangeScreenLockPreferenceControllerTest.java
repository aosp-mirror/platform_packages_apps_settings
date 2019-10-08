/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.security;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.os.UserManager;
import android.view.LayoutInflater;
import android.view.View;

import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceViewHolder;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.R;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.ShadowUtils;
import com.android.settings.widget.GearPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowUtils.class)
public class ChangeScreenLockPreferenceControllerTest {

    @Mock
    private LockPatternUtils mLockPatternUtils;
    @Mock
    private UserManager mUserManager;
    @Mock
    private DevicePolicyManager mDevicePolicyManager;
    @Mock
    private PreferenceScreen mPreferenceScreen;

    private Context mContext;
    private FakeFeatureFactory mFeatureFactory;
    private ChangeScreenLockPreferenceController mController;
    private View mGearView;
    private GearPreference mGearPreference;
    private PreferenceViewHolder mPreferenceViewHolder;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application.getApplicationContext());
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        when(mFeatureFactory.securityFeatureProvider.getLockPatternUtils(mContext))
                .thenReturn(mLockPatternUtils);
        when(mContext.getSystemService(Context.USER_SERVICE)).thenReturn(mUserManager);
        when(mContext.getSystemService(Context.DEVICE_POLICY_SERVICE))
                .thenReturn(mDevicePolicyManager);
        mController = new ChangeScreenLockPreferenceController(mContext, null  /* Host */ );
    }

    @Test
    public void testDeviceAdministrators_byDefault_shouldBeShown() {
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void testDeviceAdministrators_ifDisabled_shouldNotBeShown() {
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void updateState_notSecureDisableKeyguard_shouldNotShowGear() {
        when(mLockPatternUtils.isSecure(anyInt())).thenReturn(false);
        when(mLockPatternUtils.isLockScreenDisabled(anyInt())).thenReturn(true);
        mockGearPreferenceAndViewHolder();

        showPreference();

        assertThat(mGearView.getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void updateState_notSecureDisableKeyguard_summaryShouldShowOff() {
        when(mLockPatternUtils.isSecure(anyInt())).thenReturn(false);
        when(mLockPatternUtils.isLockScreenDisabled(anyInt())).thenReturn(true);
        mockGearPreferenceAndViewHolder();

        showPreference();

        assertThat(mGearPreference.getSummary())
                .isEqualTo(mContext.getString(R.string.unlock_set_unlock_mode_off));
    }

    @Test
    public void updateState_notSecureWithSwipeKeyguard_shouldNotShowGear() {
        when(mLockPatternUtils.isSecure(anyInt())).thenReturn(false);
        when(mLockPatternUtils.isLockScreenDisabled(anyInt())).thenReturn(false);
        mockGearPreferenceAndViewHolder();

        showPreference();

        assertThat(mGearView.getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void updateState_notSecureWithSwipeKeyguard_summaryShouldShowSwipe() {
        when(mLockPatternUtils.isSecure(anyInt())).thenReturn(false);
        when(mLockPatternUtils.isLockScreenDisabled(anyInt())).thenReturn(false);
        mockGearPreferenceAndViewHolder();

        showPreference();

        assertThat(mGearPreference.getSummary())
                .isEqualTo(mContext.getString(R.string.unlock_set_unlock_mode_none));
    }

    @Test
    public void updateState_secureWithPinKeyguard_shouldShowGear() {
        when(mLockPatternUtils.isSecure(anyInt())).thenReturn(true);
        when(mLockPatternUtils.isLockScreenDisabled(anyInt())).thenReturn(false);
        doReturn(DevicePolicyManager.PASSWORD_QUALITY_NUMERIC_COMPLEX).when(mLockPatternUtils)
                .getKeyguardStoredPasswordQuality(anyInt());
        mockGearPreferenceAndViewHolder();

        showPreference();

        assertThat(mGearView.getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Test
    public void updateState_secureWithPinKeyguard_summaryShouldShowPin() {
        when(mLockPatternUtils.isSecure(anyInt())).thenReturn(true);
        when(mLockPatternUtils.isLockScreenDisabled(anyInt())).thenReturn(false);
        doReturn(DevicePolicyManager.PASSWORD_QUALITY_NUMERIC_COMPLEX).when(mLockPatternUtils)
                .getKeyguardStoredPasswordQuality(anyInt());

        mockGearPreferenceAndViewHolder();

        showPreference();

        assertThat(mGearPreference.getSummary())
                .isEqualTo(mContext.getString(R.string.unlock_set_unlock_mode_pin));
    }

    @Test
    public void updateState_secureWithPasswordKeyguard_shouldShowGear() {
        when(mLockPatternUtils.isSecure(anyInt())).thenReturn(true);
        when(mLockPatternUtils.isLockScreenDisabled(anyInt())).thenReturn(false);
        doReturn(DevicePolicyManager.PASSWORD_QUALITY_COMPLEX).when(mLockPatternUtils)
                .getKeyguardStoredPasswordQuality(anyInt());
        mockGearPreferenceAndViewHolder();

        showPreference();

        assertThat(mGearView.getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Test
    public void updateState_secureWithPasswordKeyguard_summaryShouldShowPassword() {
        when(mLockPatternUtils.isSecure(anyInt())).thenReturn(true);
        when(mLockPatternUtils.isLockScreenDisabled(anyInt())).thenReturn(false);
        doReturn(DevicePolicyManager.PASSWORD_QUALITY_COMPLEX).when(mLockPatternUtils)
                .getKeyguardStoredPasswordQuality(anyInt());
        mockGearPreferenceAndViewHolder();

        showPreference();

        assertThat(mGearPreference.getSummary())
                .isEqualTo(mContext.getString(R.string.unlock_set_unlock_mode_password));
    }

    @Test
    public void updateState_secureWithPatternKeyguard_shouldShowGear() {
        when(mLockPatternUtils.isSecure(anyInt())).thenReturn(true);
        when(mLockPatternUtils.isLockScreenDisabled(anyInt())).thenReturn(false);
        doReturn(DevicePolicyManager.PASSWORD_QUALITY_SOMETHING).when(mLockPatternUtils)
                .getKeyguardStoredPasswordQuality(anyInt());
        mockGearPreferenceAndViewHolder();

        showPreference();

        assertThat(mGearView.getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Test
    public void updateState_secureWithPatternKeyguard_summaryShouldShowPattern() {
        when(mLockPatternUtils.isSecure(anyInt())).thenReturn(true);
        when(mLockPatternUtils.isLockScreenDisabled(anyInt())).thenReturn(false);
        doReturn(DevicePolicyManager.PASSWORD_QUALITY_SOMETHING).when(mLockPatternUtils)
                .getKeyguardStoredPasswordQuality(anyInt());
        mockGearPreferenceAndViewHolder();

        showPreference();

        assertThat(mGearPreference.getSummary())
                .isEqualTo(mContext.getString(R.string.unlock_set_unlock_mode_pattern));
    }

    private void mockGearPreferenceAndViewHolder() {
        mGearPreference = new GearPreference(mContext, null);
        mGearView = new View(mContext);
        PreferenceViewHolder viewHolder = PreferenceViewHolder.createInstanceForTests(
                LayoutInflater.from(mContext).inflate(
                        mGearPreference.getLayoutResource(), null, false));
        mPreferenceViewHolder = spy(viewHolder);
        doReturn(mGearView).when(mPreferenceViewHolder).findViewById(R.id.settings_button);
        when(mPreferenceScreen.findPreference(anyString())).thenReturn(mGearPreference);
    }

    private void showPreference() {
        mController.displayPreference(mPreferenceScreen);
        mController.updateState(mGearPreference);
        mGearPreference.onBindViewHolder(mPreferenceViewHolder);
    }
}