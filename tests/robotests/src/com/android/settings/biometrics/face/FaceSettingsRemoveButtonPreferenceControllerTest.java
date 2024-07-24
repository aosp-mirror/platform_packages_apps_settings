/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.biometrics.face;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.face.FaceManager;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.biometrics.face.FaceSettingsRemoveButtonPreferenceController.ConfirmRemoveDialog;
import com.android.settings.testutils.shadow.ShadowUserManager;
import com.android.settingslib.widget.LayoutPreference;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

@Ignore("b/295325503")
@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowUserManager.class})
public class FaceSettingsRemoveButtonPreferenceControllerTest {
    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    private static final String TEST_PREF_KEY = "baz";

    @Mock
    private FaceManager mFaceManager;
    @Mock
    private PackageManager mPackageManager;
    private SettingsActivity mActivity;
    private Context mContext;
    private FaceSettingsRemoveButtonPreferenceController mController;
    private LayoutPreference mPreference;

    @Before
    public void setUp() {
        mContext = spy(RuntimeEnvironment.application);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        when(mPackageManager.hasSystemFeature(PackageManager.FEATURE_FACE)).thenReturn(true);
        ShadowApplication.getInstance().setSystemService(Context.FACE_SERVICE, mFaceManager);

        mPreference = new LayoutPreference(mContext, R.layout.face_remove_button);
        mController = new FaceSettingsRemoveButtonPreferenceController(mContext, TEST_PREF_KEY);

        mActivity = spy(Robolectric.buildActivity(SettingsActivity.class).create().get());
        mController.setActivity(mActivity);
    }

    @Test
    public void testRotationConfirmRemoveDialog() {
        // mController calls onClick(), the dialog is created.
        mController.updateState(mPreference);
        assertThat(mController.mRemoving).isFalse();

        mController.onClick(
                mPreference.findViewById(R.id.security_settings_face_settings_remove_button));

        ConfirmRemoveDialog removeDialog =
                (ConfirmRemoveDialog) mActivity.getSupportFragmentManager()
                        .findFragmentByTag(ConfirmRemoveDialog.class.getName());
        assertThat(removeDialog).isNotNull();
        assertThat(mController.mRemoving).isTrue();


        // Simulate rotation, a new controller mController2 is created and updateState() is called.
        // Since the dialog hasn't been dismissed, so mController2.mRemoving should be true
        FaceSettingsRemoveButtonPreferenceController controller2 =
                new FaceSettingsRemoveButtonPreferenceController(mContext, TEST_PREF_KEY);
        controller2.setActivity(mActivity);
        assertThat(controller2.mRemoving).isFalse();
        controller2.updateState(mPreference);
        assertThat(controller2.mRemoving).isTrue();
    }
}
