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

package com.android.settings.inputmethod;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodSubtype;

import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.testutils.shadow.ShadowInputMethodManagerWithMethodList;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowInputMethodManagerWithMethodList.class)
public class InputMethodAndSubtypePreferenceControllerTest {

    @Mock
    private PreferenceFragmentCompat mFragment;
    private Context mContext;
    private InputMethodAndSubtypePreferenceController mController;
    private PreferenceManager mPreferenceManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mPreferenceManager = new PreferenceManager(mContext);
        when(mFragment.getContext()).thenReturn(mContext);
        when(mFragment.getResources()).thenReturn(mContext.getResources());
        when(mFragment.getPreferenceManager()).thenReturn(mPreferenceManager);
        mController = new InputMethodAndSubtypePreferenceController(mContext, "pref_key");
        mController.initialize(mFragment, "");
    }

    @Test
    public void isAlwaysAvailable() {
        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.AVAILABLE);
    }

    @Test
    public void displayPreference_hasInputMethodSubType_shouldAddPreference() {
        final PreferenceManager preferenceManager = new PreferenceManager(mContext);
        final PreferenceScreen screen = preferenceManager.createPreferenceScreen(mContext);

        mController.displayPreference(screen);

        assertThat(screen.getPreferenceCount()).isEqualTo(0);

        final List<InputMethodInfo> imis = new ArrayList<>();
        imis.add(createInputMethodInfo("test", mContext));
        ShadowInputMethodManagerWithMethodList.getShadow().setInputMethodList(imis);

        mController.initialize(mFragment, "");
        mController.displayPreference(screen);

        assertThat(screen.getPreferenceCount()).isEqualTo(2);
    }

    private InputMethodInfo createInputMethodInfo(final String name, Context targetContext) {
        List<InputMethodSubtype> subtypes = new ArrayList<>();

        subtypes.add(new InputMethodSubtype.InputMethodSubtypeBuilder()
                .build());
        subtypes.add(new InputMethodSubtype.InputMethodSubtypeBuilder()
                .build());

        final ResolveInfo resolveInfo = new ResolveInfo();
        resolveInfo.serviceInfo = new ServiceInfo();
        resolveInfo.serviceInfo.packageName = "com.android.ime";
        resolveInfo.serviceInfo.name = name;
        resolveInfo.serviceInfo.applicationInfo = new ApplicationInfo();
        resolveInfo.serviceInfo.applicationInfo.enabled = true;

        return new InputMethodInfo(
                resolveInfo,
                false /* isAuxIme */,
                "SettingsActivity",
                subtypes,
                0 /* isDefaultResId */,
                true /* forceDefault */);
    }
}