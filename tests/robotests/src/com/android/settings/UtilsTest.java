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

package com.android.settings;

import static android.hardware.biometrics.SensorProperties.STRENGTH_CONVENIENCE;
import static android.hardware.biometrics.SensorProperties.STRENGTH_STRONG;
import static android.hardware.biometrics.SensorProperties.STRENGTH_WEAK;

import static com.android.settings.Utils.SETTINGS_PACKAGE_NAME;
import static com.android.settings.password.ConfirmDeviceCredentialActivity.BIOMETRIC_PROMPT_AUTHENTICATORS;
import static com.android.settings.password.ConfirmDeviceCredentialActivity.BIOMETRIC_PROMPT_HIDE_BACKGROUND;
import static com.android.settings.password.ConfirmDeviceCredentialActivity.BIOMETRIC_PROMPT_NEGATIVE_BUTTON_TEXT;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.ActionBar;
import android.app.KeyguardManager;
import android.app.admin.DevicePolicyManager;
import android.app.admin.DevicePolicyResourcesManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.VectorDrawable;
import android.hardware.biometrics.BiometricManager;
import android.hardware.biometrics.Flags;
import android.hardware.face.FaceManager;
import android.hardware.face.FaceSensorProperties;
import android.hardware.face.FaceSensorPropertiesInternal;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.storage.DiskInfo;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.util.IconDrawableFactory;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.core.graphics.drawable.IconCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.password.ChooseLockSettingsHelper;
import com.android.settings.password.ConfirmDeviceCredentialActivity;
import com.android.settings.testutils.shadow.ShadowLockPatternUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowBinder;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowLockPatternUtils.class)
public class UtilsTest {

    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private static final String PACKAGE_NAME = "com.android.app";
    private static final int USER_ID = 1;

    @Mock
    private WifiManager wifiManager;
    @Mock
    private Network network;
    @Mock
    private ConnectivityManager connectivityManager;
    @Mock
    private DevicePolicyManager mDevicePolicyManager;
    @Mock
    private DevicePolicyResourcesManager mDevicePolicyResourcesManager;
    @Mock
    private UserManager mMockUserManager;
    @Mock
    private PackageManager mPackageManager;
    @Mock
    private IconDrawableFactory mIconDrawableFactory;
    @Mock
    private ApplicationInfo mApplicationInfo;
    @Mock
    private BiometricManager mBiometricManager;
    @Mock
    private Fragment mFragment;

    private Context mContext;
    private UserManager mUserManager;
    private static final int FLAG_SYSTEM = 0x00000000;
    private static final int FLAG_MAIN = 0x00004000;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(RuntimeEnvironment.application);
        mUserManager = (UserManager) mContext.getSystemService(Context.USER_SERVICE);
        when(mContext.getSystemService(WifiManager.class)).thenReturn(wifiManager);
        when(mContext.getSystemService(Context.CONNECTIVITY_SERVICE))
                .thenReturn(connectivityManager);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        when(mContext.getSystemService(BiometricManager.class)).thenReturn(mBiometricManager);
    }

    @After
    public void tearDown() {
        ShadowLockPatternUtils.reset();
    }

    @Test
    public void getWifiIpAddresses_succeeds() throws Exception {
        when(wifiManager.getCurrentNetwork()).thenReturn(network);
        LinkAddress address = new LinkAddress(InetAddress.getByName("127.0.0.1"), 0);
        LinkProperties lp = new LinkProperties();
        lp.addLinkAddress(address);
        when(connectivityManager.getLinkProperties(network)).thenReturn(lp);

        assertThat(Utils.getWifiIpAddresses(mContext)).isEqualTo("127.0.0.1");
    }

    @Test
    public void getWifiIpAddresses_nullLinkProperties() {
        when(wifiManager.getCurrentNetwork()).thenReturn(network);
        // Explicitly set the return value to null for readability sake.
        when(connectivityManager.getLinkProperties(network)).thenReturn(null);

        assertThat(Utils.getWifiIpAddresses(mContext)).isNull();
    }

    @Test
    public void getWifiIpAddresses_nullNetwork() {
        // Explicitly set the return value to null for readability sake.
        when(wifiManager.getCurrentNetwork()).thenReturn(null);

        assertThat(Utils.getWifiIpAddresses(mContext)).isNull();
    }

    @Test
    public void initializeVolumeDoesntBreakOnNullVolume() {
        VolumeInfo info = new VolumeInfo("id", 0, new DiskInfo("id", 0), "");
        StorageManager storageManager = mock(StorageManager.class, RETURNS_DEEP_STUBS);
        when(storageManager.findVolumeById(anyString())).thenReturn(info);

        Utils.maybeInitializeVolume(storageManager, new Bundle());
    }

    @Test
    public void isProfileOrDeviceOwner_deviceOwnerApp_returnTrue() {
        when(mDevicePolicyManager.isDeviceOwnerAppOnAnyUser(PACKAGE_NAME)).thenReturn(true);

        assertThat(
            Utils.isProfileOrDeviceOwner(mMockUserManager, mDevicePolicyManager, PACKAGE_NAME))
                .isTrue();
    }

    @Test
    public void isProfileOrDeviceOwner_profileOwnerApp_returnTrue() {
        final List<UserInfo> userInfos = new ArrayList<>();
        userInfos.add(new UserInfo());

        when(mMockUserManager.getUsers()).thenReturn(userInfos);
        when(mDevicePolicyManager.getProfileOwnerAsUser(userInfos.get(0).id))
            .thenReturn(new ComponentName(PACKAGE_NAME, ""));

        assertThat(
            Utils.isProfileOrDeviceOwner(mMockUserManager, mDevicePolicyManager, PACKAGE_NAME))
                .isTrue();
    }

    @Test
    public void setEditTextCursorPosition_shouldGetExpectedEditTextLenght() {
        final EditText editText = new EditText(mContext);
        final CharSequence text = "test";
        editText.setText(text, TextView.BufferType.EDITABLE);
        final int length = editText.getText().length();
        Utils.setEditTextCursorPosition(editText);

        assertThat(editText.getSelectionEnd()).isEqualTo(length);
    }

    @Test
    public void createIconWithDrawable_BitmapDrawable() {
        final Bitmap bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        final BitmapDrawable drawable = new BitmapDrawable(mContext.getResources(), bitmap);

        final IconCompat icon = Utils.createIconWithDrawable(drawable);

        assertThat(icon.getBitmap()).isNotNull();
    }

    @Test
    public void createIconWithDrawable_ColorDrawable() {
        final ColorDrawable drawable = new ColorDrawable(Color.BLACK);

        final IconCompat icon = Utils.createIconWithDrawable(drawable);

        assertThat(icon.getBitmap()).isNotNull();
    }

    @Test
    public void createIconWithDrawable_VectorDrawable() {
        final VectorDrawable drawable = VectorDrawable.create(mContext.getResources(),
                R.drawable.ic_settings_accent);

        final IconCompat icon = Utils.createIconWithDrawable(drawable);

        assertThat(icon.getBitmap()).isNotNull();
    }

    @Test
    public void getBadgedIcon_usePackageNameAndUserId()
        throws PackageManager.NameNotFoundException {
        doReturn(mApplicationInfo).when(mPackageManager).getApplicationInfoAsUser(
                PACKAGE_NAME, PackageManager.GET_META_DATA, USER_ID);

        Utils.getBadgedIcon(mIconDrawableFactory, mPackageManager, PACKAGE_NAME, USER_ID);

        // Verify that it uses the correct user id
        verify(mPackageManager).getApplicationInfoAsUser(eq(PACKAGE_NAME), anyInt(), eq(USER_ID));
        verify(mIconDrawableFactory).getBadgedIcon(mApplicationInfo, USER_ID);
    }

    @Test
    public void isPackageEnabled_appEnabled_returnTrue()
            throws PackageManager.NameNotFoundException{
        mApplicationInfo.enabled = true;
        when(mPackageManager.getApplicationInfo(PACKAGE_NAME, 0)).thenReturn(mApplicationInfo);

        assertThat(Utils.isPackageEnabled(mContext, PACKAGE_NAME)).isTrue();
    }

    @Test
    public void isPackageEnabled_appDisabled_returnTrue()
            throws PackageManager.NameNotFoundException{
        mApplicationInfo.enabled = false;
        when(mPackageManager.getApplicationInfo(PACKAGE_NAME, 0)).thenReturn(mApplicationInfo);

        assertThat(Utils.isPackageEnabled(mContext, PACKAGE_NAME)).isFalse();
    }

    @Test
    public void isPackageEnabled_noApp_returnFalse() {
        assertThat(Utils.isPackageEnabled(mContext, PACKAGE_NAME)).isFalse();
    }

    @Test
    public void setActionBarShadowAnimation_nullParameters_shouldNotCrash() {
        // no crash here
        Utils.setActionBarShadowAnimation(null, null, null);
    }

    @Test
    public void setActionBarShadowAnimation_shouldSetElevationToZero() {
        final FragmentActivity activity = Robolectric.setupActivity(FragmentActivity.class);
        final ActionBar actionBar = activity.getActionBar();

        Utils.setActionBarShadowAnimation(activity, activity.getLifecycle(),
                new ScrollView(mContext));

        assertThat(actionBar.getElevation()).isEqualTo(0.f);
    }

    @Test
    public void isSettingsIntelligence_IsSI_returnTrue() {
        final String siPackageName = mContext.getString(
                R.string.config_settingsintelligence_package_name);
        ShadowBinder.setCallingUid(USER_ID);
        when(mPackageManager.getPackagesForUid(USER_ID)).thenReturn(new String[]{siPackageName});

        assertThat(Utils.isSettingsIntelligence(mContext)).isTrue();
    }

    @Test
    public void isSettingsIntelligence_IsNotSI_returnFalse() {
        ShadowBinder.setCallingUid(USER_ID);
        when(mPackageManager.getPackagesForUid(USER_ID)).thenReturn(new String[]{PACKAGE_NAME});

        assertThat(Utils.isSettingsIntelligence(mContext)).isFalse();
    }

    @Test
    public void canCurrentUserDream_isMainUser_returnTrue() {
        Context mockContext = mock(Context.class);
        UserManager mockUserManager = mock(UserManager.class);

        when(mockContext.getSystemService(UserManager.class)).thenReturn(mockUserManager);

        // mock MainUser
        UserHandle mainUser = new UserHandle(10);
        when(mockUserManager.getMainUser()).thenReturn(mainUser);
        when(mockUserManager.isUserForeground()).thenReturn(true);

        when(mockContext.createContextAsUser(mainUser, 0)).thenReturn(mockContext);

        assertThat(Utils.canCurrentUserDream(mockContext)).isTrue();
    }

    @Test
    public void canCurrentUserDream_nullMainUser_returnFalse() {
        Context mockContext = mock(Context.class);
        UserManager mockUserManager = mock(UserManager.class);

        when(mockContext.getSystemService(UserManager.class)).thenReturn(mockUserManager);
        when(mockUserManager.getMainUser()).thenReturn(null);

        assertThat(Utils.canCurrentUserDream(mockContext)).isFalse();
    }

    @Test
    public void canCurrentUserDream_notMainUser_returnFalse() {
        Context mockContext = mock(Context.class);
        UserManager mockUserManager = mock(UserManager.class);

        when(mockContext.getSystemService(UserManager.class)).thenReturn(mockUserManager);
        when(mockUserManager.isUserForeground()).thenReturn(false);

        assertThat(Utils.canCurrentUserDream(mockContext)).isFalse();
    }

    @Test
    public void checkUserOwnsFrpCredential_userOwnsFrpCredential_returnUserId() {
        ShadowLockPatternUtils.setUserOwnsFrpCredential(true);

        assertThat(Utils.checkUserOwnsFrpCredential(mContext, 123)).isEqualTo(123);
    }

    @Test
    public void checkUserOwnsFrpCredential_userNotOwnsFrpCredential_returnUserId() {
        ShadowLockPatternUtils.setUserOwnsFrpCredential(false);

        assertThrows(
                SecurityException.class,
                () -> Utils.checkUserOwnsFrpCredential(mContext, 123));
    }

    @Test
    public void getConfirmCredentialStringForUser_Pin_shouldReturnCorrectString() {
        setUpForConfirmCredentialString(false /* isEffectiveUserManagedProfile */);

        when(mContext.getString(R.string.lockpassword_confirm_your_pin_generic))
                .thenReturn("PIN");

        String confirmCredentialString = Utils.getConfirmCredentialStringForUser(mContext,
                USER_ID, LockPatternUtils.CREDENTIAL_TYPE_PIN);

        assertThat(confirmCredentialString).isEqualTo("PIN");
    }

    @Test
    public void getConfirmCredentialStringForUser_Pattern_shouldReturnCorrectString() {
        setUpForConfirmCredentialString(false /* isEffectiveUserManagedProfile */);

        when(mContext.getString(R.string.lockpassword_confirm_your_pattern_generic))
                .thenReturn("PATTERN");

        String confirmCredentialString = Utils.getConfirmCredentialStringForUser(mContext,
                USER_ID, LockPatternUtils.CREDENTIAL_TYPE_PATTERN);

        assertThat(confirmCredentialString).isEqualTo("PATTERN");
    }

    @Test
    public void getConfirmCredentialStringForUser_Password_shouldReturnCorrectString() {
        setUpForConfirmCredentialString(false /* isEffectiveUserManagedProfile */);

        when(mContext.getString(R.string.lockpassword_confirm_your_password_generic))
                .thenReturn("PASSWORD");

        String confirmCredentialString = Utils.getConfirmCredentialStringForUser(mContext,
                USER_ID, LockPatternUtils.CREDENTIAL_TYPE_PASSWORD);

        assertThat(confirmCredentialString).isEqualTo("PASSWORD");
    }

    @Test
    public void getConfirmCredentialStringForUser_workPin_shouldReturnNull() {
        setUpForConfirmCredentialString(true /* isEffectiveUserManagedProfile */);

        String confirmCredentialString = Utils.getConfirmCredentialStringForUser(mContext,
                USER_ID, LockPatternUtils.CREDENTIAL_TYPE_PIN);

        assertNull(confirmCredentialString);
    }

    @Test
    public void getConfirmCredentialStringForUser_workPattern_shouldReturnNull() {
        setUpForConfirmCredentialString(true /* isEffectiveUserManagedProfile */);

        String confirmCredentialString = Utils.getConfirmCredentialStringForUser(mContext,
                USER_ID, LockPatternUtils.CREDENTIAL_TYPE_PATTERN);

        assertNull(confirmCredentialString);
    }

    @Test
    public void getConfirmCredentialStringForUser_workPassword_shouldReturnNull() {
        setUpForConfirmCredentialString(true /* isEffectiveUserManagedProfile */);

        String confirmCredentialString = Utils.getConfirmCredentialStringForUser(mContext,
                USER_ID, LockPatternUtils.CREDENTIAL_TYPE_PASSWORD);

        assertNull(confirmCredentialString);
    }

    @Test
    public void getConfirmCredentialStringForUser_credentialTypeNone_shouldReturnNull() {
        setUpForConfirmCredentialString(false /* isEffectiveUserManagedProfile */);

        String confirmCredentialString = Utils.getConfirmCredentialStringForUser(mContext,
                USER_ID, LockPatternUtils.CREDENTIAL_TYPE_NONE);

        assertNull(confirmCredentialString);
    }

    @Test
    public void isFaceNotConvenienceBiometric_faceStrengthStrong_shouldReturnTrue() {
        FaceManager mockFaceManager = mock(FaceManager.class);
        when(mContext.getSystemService(Context.FACE_SERVICE)).thenReturn(mockFaceManager);
        doReturn(true).when(mPackageManager).hasSystemFeature(anyString());
        List<FaceSensorPropertiesInternal> props = List.of(new FaceSensorPropertiesInternal(
                0 /* id */,
                STRENGTH_STRONG,
                1 /* maxTemplatesAllowed */,
                new ArrayList<>() /* componentInfo */,
                FaceSensorProperties.TYPE_UNKNOWN,
                true /* supportsFaceDetection */,
                true /* supportsSelfIllumination */,
                false /* resetLockoutRequiresChallenge */));
        doReturn(props).when(mockFaceManager).getSensorPropertiesInternal();

        assertThat(Utils.isFaceNotConvenienceBiometric(mContext)).isTrue();
    }

    @Test
    public void isFaceNotConvenienceBiometric_faceStrengthWeak_shouldReturnTrue() {
        FaceManager mockFaceManager = mock(FaceManager.class);
        when(mContext.getSystemService(Context.FACE_SERVICE)).thenReturn(mockFaceManager);
        doReturn(true).when(mPackageManager).hasSystemFeature(anyString());
        List<FaceSensorPropertiesInternal> props = List.of(new FaceSensorPropertiesInternal(
                0 /* id */,
                STRENGTH_WEAK,
                1 /* maxTemplatesAllowed */,
                new ArrayList<>() /* componentInfo */,
                FaceSensorProperties.TYPE_UNKNOWN,
                true /* supportsFaceDetection */,
                true /* supportsSelfIllumination */,
                false /* resetLockoutRequiresChallenge */));
        doReturn(props).when(mockFaceManager).getSensorPropertiesInternal();

        assertThat(Utils.isFaceNotConvenienceBiometric(mContext)).isTrue();
    }

    @Test
    public void isFaceNotConvenienceBiometric_faceStrengthConvenience_shouldReturnFalse() {
        FaceManager mockFaceManager = mock(FaceManager.class);
        when(mContext.getSystemService(Context.FACE_SERVICE)).thenReturn(mockFaceManager);
        doReturn(true).when(mPackageManager).hasSystemFeature(anyString());
        List<FaceSensorPropertiesInternal> props = List.of(new FaceSensorPropertiesInternal(
                0 /* id */,
                STRENGTH_CONVENIENCE,
                1 /* maxTemplatesAllowed */,
                new ArrayList<>() /* componentInfo */,
                FaceSensorProperties.TYPE_UNKNOWN,
                true /* supportsFaceDetection */,
                true /* supportsSelfIllumination */,
                false /* resetLockoutRequiresChallenge */));
        doReturn(props).when(mockFaceManager).getSensorPropertiesInternal();

        assertThat(Utils.isFaceNotConvenienceBiometric(mContext)).isFalse();
    }

    @Test
    public void isFaceNotConvenienceBiometric_faceManagerNull_shouldReturnFalse() {
        when(mContext.getSystemService(Context.FACE_SERVICE)).thenReturn(null);
        assertThat(Utils.isFaceNotConvenienceBiometric(mContext)).isFalse();
    }

    @Test
    @EnableFlags(Flags.FLAG_MANDATORY_BIOMETRICS)
    public void testRequestBiometricAuthentication_biometricManagerNull_shouldReturnNotActive() {
        when(mContext.getSystemService(BiometricManager.class)).thenReturn(null);
        assertThat(Utils.requestBiometricAuthenticationForMandatoryBiometrics(mContext,
                false /* biometricsAuthenticationRequested */, USER_ID)).isEqualTo(
                        Utils.BiometricStatus.NOT_ACTIVE);
    }

    @Test
    @EnableFlags(Flags.FLAG_MANDATORY_BIOMETRICS)
    public void testRequestBiometricAuthentication_biometricManagerReturnsSuccess_shouldReturnOk() {
        when(mBiometricManager.canAuthenticate(USER_ID,
                BiometricManager.Authenticators.IDENTITY_CHECK))
                .thenReturn(BiometricManager.BIOMETRIC_SUCCESS);
        final Utils.BiometricStatus requestBiometricAuthenticationForMandatoryBiometrics =
                Utils.requestBiometricAuthenticationForMandatoryBiometrics(mContext,
                        false /* biometricsAuthenticationRequested */, USER_ID);
        assertThat(requestBiometricAuthenticationForMandatoryBiometrics).isEqualTo(
                Utils.BiometricStatus.OK);
    }

    @Test
    @EnableFlags(Flags.FLAG_MANDATORY_BIOMETRICS)
    public void testRequestBiometricAuthentication_biometricManagerReturnsError_shouldReturnError() {
        when(mBiometricManager.canAuthenticate(anyInt(),
                eq(BiometricManager.Authenticators.IDENTITY_CHECK)))
                .thenReturn(BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE);
        assertThat(Utils.requestBiometricAuthenticationForMandatoryBiometrics(mContext,
                false /* biometricsAuthenticationRequested */, USER_ID)).isEqualTo(
                        Utils.BiometricStatus.ERROR);
    }

    @Test
    @EnableFlags(Flags.FLAG_MANDATORY_BIOMETRICS)
    public void testRequestBiometricAuthentication_biometricManagerReturnsSuccessForDifferentUser_shouldReturnError() {
        when(mContext.getSystemService(UserManager.class)).thenReturn(mMockUserManager);
        when(mMockUserManager.getCredentialOwnerProfile(USER_ID)).thenReturn(USER_ID);
        when(mBiometricManager.canAuthenticate(anyInt(),
                eq(BiometricManager.Authenticators.IDENTITY_CHECK)))
                .thenReturn(BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE);
        when(mBiometricManager.canAuthenticate(0 /* userId */,
                BiometricManager.Authenticators.IDENTITY_CHECK))
                .thenReturn(BiometricManager.BIOMETRIC_SUCCESS);
        assertThat(Utils.requestBiometricAuthenticationForMandatoryBiometrics(mContext,
                false /* biometricsAuthenticationRequested */, USER_ID)).isEqualTo(
                        Utils.BiometricStatus.ERROR);
    }

    @Test
    @EnableFlags(Flags.FLAG_MANDATORY_BIOMETRICS)
    public void testLaunchBiometricPrompt_checkIntentValues() {
        when(mFragment.getContext()).thenReturn(mContext);
        when(mContext.getSystemService(UserManager.class)).thenReturn(mMockUserManager);
        when(mMockUserManager.getCredentialOwnerProfile(USER_ID)).thenReturn(USER_ID);

        final int requestCode = 1;
        final ArgumentCaptor<Intent> intentArgumentCaptor = ArgumentCaptor.forClass(Intent.class);
        Utils.launchBiometricPromptForMandatoryBiometrics(mFragment, requestCode, USER_ID,
                false /* hideBackground */);

        verify(mFragment).startActivityForResult(intentArgumentCaptor.capture(), eq(requestCode));

        final Intent intent = intentArgumentCaptor.getValue();

        assertThat(intent.getExtra(BIOMETRIC_PROMPT_AUTHENTICATORS)).isEqualTo(
                BiometricManager.Authenticators.IDENTITY_CHECK);
        assertThat(intent.getExtra(BIOMETRIC_PROMPT_NEGATIVE_BUTTON_TEXT)).isNotNull();
        assertThat(intent.getExtra(KeyguardManager.EXTRA_DESCRIPTION)).isNotNull();
        assertThat(intent.getBooleanExtra(ChooseLockSettingsHelper.EXTRA_KEY_ALLOW_ANY_USER, false))
                .isTrue();
        assertThat(intent.getBooleanExtra(BIOMETRIC_PROMPT_HIDE_BACKGROUND, true))
                .isFalse();
        assertThat(intent.getIntExtra(Intent.EXTRA_USER_ID, 0)).isEqualTo(USER_ID);
        assertThat(intent.getComponent().getPackageName()).isEqualTo(SETTINGS_PACKAGE_NAME);
        assertThat(intent.getComponent().getClassName()).isEqualTo(
                ConfirmDeviceCredentialActivity.InternalActivity.class.getName());
    }

    private void setUpForConfirmCredentialString(boolean isEffectiveUserManagedProfile) {
        when(mContext.getSystemService(Context.USER_SERVICE)).thenReturn(mMockUserManager);
        when(mMockUserManager.getCredentialOwnerProfile(USER_ID)).thenReturn(USER_ID);
        when(mMockUserManager.isManagedProfile(USER_ID)).thenReturn(isEffectiveUserManagedProfile);
    }
}
