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

package com.android.settings.dashboard.suggestions;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settingslib.SuggestionParser;
import com.android.settingslib.drawer.Tile;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class SuggestionFeatureProviderImplTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock
    private SuggestionParser mSuggestionParser;
    @Mock
    private Tile mSuggestion;

    private FakeFeatureFactory mFactory;
    private SuggestionFeatureProviderImpl mProvider;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        FakeFeatureFactory.setupForTest(mContext);
        mFactory = (FakeFeatureFactory) FakeFeatureFactory.getFactory(mContext);
        when(mContext.getApplicationContext()).thenReturn(RuntimeEnvironment.application);
        mSuggestion.intent = new Intent().setClassName("pkg", "cls");
        mSuggestion.category = "category";

        mProvider = new SuggestionFeatureProviderImpl(mContext);
    }

    @Test
    public void dismissSuggestion_noParserOrSuggestion_noop() {
        mProvider.dismissSuggestion(mContext, null, null);
        mProvider.dismissSuggestion(mContext, mSuggestionParser, null);
        mProvider.dismissSuggestion(mContext, null, mSuggestion);

        verifyZeroInteractions(mFactory.metricsFeatureProvider);
    }

    @Test
    public void getSuggestionIdentifier_samePackage_returnClassName() {
        final Tile suggestion = new Tile();
        suggestion.intent = new Intent()
                .setClassName(RuntimeEnvironment.application.getPackageName(), "123");
        assertThat(mProvider.getSuggestionIdentifier(RuntimeEnvironment.application, suggestion))
                .isEqualTo("123");
    }

    @Test
    public void getSuggestionIdentifier_differentPackage_returnPackageName() {
        final Tile suggestion = new Tile();
        suggestion.intent = new Intent()
                .setClassName(RuntimeEnvironment.application.getPackageName(), "123");
        assertThat(mProvider.getSuggestionIdentifier(mContext, suggestion))
                .isEqualTo(RuntimeEnvironment.application.getPackageName());
    }

    @Test
    public void getSuggestionIdentifier_nullComponent_shouldNotCrash() {
        final Tile suggestion = new Tile();
        suggestion.intent = new Intent();
        assertThat(mProvider.getSuggestionIdentifier(mContext, suggestion))
                .isNotEmpty();
    }

    @Test
    public void dismissSuggestion_hasMoreDismissCount_shouldNotDisableComponent() {
        when(mSuggestionParser.dismissSuggestion(any(Tile.class), anyBoolean()))
                .thenReturn(false);
        mProvider.dismissSuggestion(mContext, mSuggestionParser, mSuggestion);

        verify(mFactory.metricsFeatureProvider).action(
                eq(mContext),
                eq(MetricsProto.MetricsEvent.ACTION_SETTINGS_DISMISS_SUGGESTION),
                anyString());
        verify(mContext, never()).getPackageManager();
    }


    @Test
    public void dismissSuggestion_noContext_shouldDoNothing() {
        mProvider.dismissSuggestion(null, mSuggestionParser, mSuggestion);

        verifyZeroInteractions(mFactory.metricsFeatureProvider);
    }

    @Test
    public void dismissSuggestion_hasNoMoreDismissCount_shouldDisableComponent() {
        when(mSuggestionParser.dismissSuggestion(any(Tile.class), anyBoolean()))
                .thenReturn(true);

        mProvider.dismissSuggestion(mContext, mSuggestionParser, mSuggestion);

        verify(mFactory.metricsFeatureProvider).action(
                eq(mContext),
                eq(MetricsProto.MetricsEvent.ACTION_SETTINGS_DISMISS_SUGGESTION),
                anyString());

        verify(mContext.getPackageManager())
                .setComponentEnabledSetting(mSuggestion.intent.getComponent(),
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP);
        verify(mSuggestionParser).markCategoryDone(mSuggestion.category);
    }
}
