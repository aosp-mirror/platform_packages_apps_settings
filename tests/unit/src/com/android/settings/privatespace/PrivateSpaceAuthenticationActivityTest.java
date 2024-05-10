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

package com.android.settings.privatespace;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.os.Flags;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class PrivateSpaceAuthenticationActivityTest {
    @Rule
    public final CheckFlagsRule mCheckFlagsRule =
            DeviceFlagsValueProvider.createCheckFlagsRule();
    @Mock private PrivateSpaceMaintainer mPrivateSpaceMaintainer;
    @Mock private Context mContext;
    private PrivateSpaceAuthenticationActivity mPrivateSpaceAuthenticationActivity;
    private Intent mDefaultIntent;

    /** Required setup before a test. */
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = ApplicationProvider.getApplicationContext();
        mDefaultIntent = new Intent();
        mDefaultIntent.setClass(InstrumentationRegistry.getInstrumentation().getTargetContext(),
                PrivateSpaceAuthenticationActivity.class);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            try {
                mPrivateSpaceAuthenticationActivity =
                        spy((PrivateSpaceAuthenticationActivity) InstrumentationRegistry
                                .getInstrumentation().newActivity(
                                        getClass().getClassLoader(),
                                        PrivateSpaceAuthenticationActivity.class.getName(),
                                        mDefaultIntent));
            } catch (Exception e) {
                throw new RuntimeException(e); // nothing to do
            }
        });
        doNothing().when(mPrivateSpaceAuthenticationActivity).startActivity(any(Intent.class));
        PrivateSpaceAuthenticationActivity.Injector injector =
                new PrivateSpaceAuthenticationActivity.Injector() {
                    @Override
                    PrivateSpaceMaintainer injectPrivateSpaceMaintainer(Context context) {
                        return mPrivateSpaceMaintainer;
                    }
                };
        mPrivateSpaceAuthenticationActivity.setPrivateSpaceMaintainer(injector);
    }

    /** Tests that when Private does not exist setup flow is started. */
    //TODO(b/307729746) Plan to add more tests for complete setup flow
    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ALLOW_PRIVATE_PROFILE)
    public void whenPrivateProfileDoesNotExist_triggersSetupFlow() {
        when(mPrivateSpaceMaintainer.doesPrivateSpaceExist()).thenReturn(false);

        final ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        mPrivateSpaceAuthenticationActivity.onLockAuthentication(mContext);
        verify(mPrivateSpaceAuthenticationActivity).startActivity(intentCaptor.capture());
        assertThat(intentCaptor.getValue().getComponent().getClassName())
                .isEqualTo(PrivateSpaceSetupActivity.class.getName());
    }
}
