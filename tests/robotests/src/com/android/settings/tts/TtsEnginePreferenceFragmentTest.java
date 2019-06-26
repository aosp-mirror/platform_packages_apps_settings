/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.tts;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TtsEngines;

import com.android.settings.testutils.shadow.ShadowTtsEngines;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowPackageManager;
import org.robolectric.shadows.androidx.fragment.FragmentController;

@RunWith(RobolectricTestRunner.class)
public class TtsEnginePreferenceFragmentTest {

    private Context mContext;
    private TtsEnginePreferenceFragment mTtsEnginePreferenceFragment;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;

        final ResolveInfo info = new ResolveInfo();
        final ServiceInfo serviceInfo = spy(new ServiceInfo());
        serviceInfo.packageName = mContext.getPackageName();
        serviceInfo.name = mContext.getClass().getName();
        info.serviceInfo = serviceInfo;
        doReturn("title").when(serviceInfo).loadLabel(any(PackageManager.class));
        doReturn(1).when(serviceInfo).getIconResource();

        final ShadowPackageManager spm = Shadow.extract(mContext.getPackageManager());
        spm.addResolveInfoForIntent(
                new Intent(TextToSpeech.Engine.INTENT_ACTION_TTS_SERVICE), info);
    }

    @After
    public void tearDown() {
        ShadowTtsEngines.reset();
    }

    @Test
    public void getCandidates_AddEngines_returnCorrectEngines() {
        mTtsEnginePreferenceFragment = FragmentController.of(new TtsEnginePreferenceFragment(),
                new Bundle())
                .create()
                .get();

        assertThat(mTtsEnginePreferenceFragment.getCandidates().size()).isEqualTo(1);
    }

    @Test
    @Config(shadows = {ShadowTtsEngines.class})
    public void getDefaultKey_validKey_returnCorrectKey() {
        final String TEST_ENGINE = "test_engine";
        final TtsEngines engine = mock(TtsEngines.class);
        ShadowTtsEngines.setInstance(engine);
        mTtsEnginePreferenceFragment = FragmentController.of(new TtsEnginePreferenceFragment(),
                new Bundle())
                .create()
                .get();
        when(engine.getDefaultEngine()).thenReturn(TEST_ENGINE);

        assertThat(mTtsEnginePreferenceFragment.getDefaultKey()).isEqualTo(TEST_ENGINE);
    }

    @Test
    @Config(shadows = {ShadowTtsEngines.class})
    public void setDefaultKey_validKey_callingTtsEngineFunction() {
        final TtsEngines engine = mock(TtsEngines.class);
        ShadowTtsEngines.setInstance(engine);
        mTtsEnginePreferenceFragment = FragmentController.of(new TtsEnginePreferenceFragment(),
                new Bundle())
                .create()
                .get();

        mTtsEnginePreferenceFragment.setDefaultKey(mContext.getPackageName());

        verify(engine).isEngineInstalled(mContext.getPackageName());
    }

    @Test
    public void setDefaultKey_validKey_updateCheckedState() {
        mTtsEnginePreferenceFragment = spy(FragmentController.of(new TtsEnginePreferenceFragment(),
                new Bundle())
                .create()
                .get());

        mTtsEnginePreferenceFragment.setDefaultKey(mContext.getPackageName());

        verify(mTtsEnginePreferenceFragment).updateCheckedState(mContext.getPackageName());
    }
}
