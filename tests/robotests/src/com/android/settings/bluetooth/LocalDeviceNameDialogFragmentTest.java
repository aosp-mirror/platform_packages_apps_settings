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

package com.android.settings.bluetooth;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;

import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.shadow.SettingsShadowResourcesImpl;
import com.android.settings.testutils.shadow.ShadowAlertDialogCompat;
import com.android.settingslib.bluetooth.LocalBluetoothManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.androidx.fragment.FragmentController;
import org.robolectric.util.ReflectionHelpers;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(shadows = {ShadowAlertDialogCompat.class, SettingsShadowResourcesImpl.class})
public class LocalDeviceNameDialogFragmentTest {

    @Mock
    private InputMethodManager mInputMethodManager;

    private Context mContext;
    private LocalDeviceNameDialogFragment mFragment;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        doReturn(mInputMethodManager).when(mContext).getSystemService(Context.INPUT_METHOD_SERVICE);

        mFragment = spy(LocalDeviceNameDialogFragment.newInstance());
        when(mFragment.getContext()).thenReturn(mContext);
    }

    @After
    public void tearDown() {
        ReflectionHelpers.setStaticField(LocalBluetoothManager.class, "sInstance", null);
    }

    @Test
    public void diaglogTriggersShowSoftInput() {
        FragmentController.setupFragment(mFragment, FragmentActivity.class, 0 /* containerViewId */,
                null /* bundle */);
        AlertDialog dialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(dialog).isNotNull();
        View view = dialog.findViewById(R.id.edittext);
        verify(mInputMethodManager).showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
    }
}
