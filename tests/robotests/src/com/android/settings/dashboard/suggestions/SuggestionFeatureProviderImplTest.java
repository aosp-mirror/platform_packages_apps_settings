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

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.provider.Settings.Secure;
import android.service.settings.suggestions.Suggestion;
import android.util.Pair;

import com.android.internal.logging.nano.MetricsProto;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.Settings.NightDisplaySuggestionActivity;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.shadow.SettingsShadowResources;
import com.android.settings.testutils.shadow.ShadowSecureSettings;
import com.android.settingslib.drawer.Tile;
import com.android.settingslib.suggestions.SuggestionControllerMixin;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(shadows = ShadowSecureSettings.class)
public class SuggestionFeatureProviderImplTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock
    private SuggestionControllerMixin mSuggestionControllerMixin;
    @Mock
    private Suggestion mSuggestion;
    @Mock
    private ActivityManager mActivityManager;
    @Mock
    private PackageManager mPackageManager;
    @Mock
    private FingerprintManager mFingerprintManager;

    private FakeFeatureFactory mFactory;
    private SuggestionFeatureProviderImpl mProvider;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mFactory = FakeFeatureFactory.setupForTest();
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        // Explicit casting to object due to MockitoCast bug
        when((Object) mContext.getSystemService(FingerprintManager.class))
                .thenReturn(mFingerprintManager);
        when(mSuggestion.getId()).thenReturn("test_id");
        when(mContext.getApplicationContext()).thenReturn(RuntimeEnvironment.application);
        when(mContext.getSystemService(Context.ACTIVITY_SERVICE)).thenReturn(mActivityManager);
        when(mActivityManager.isLowRamDevice()).thenReturn(false);

        mProvider = new SuggestionFeatureProviderImpl(mContext);
    }

    @After
    public void tearDown() {
        SettingsShadowResources.reset();
    }

    @Test
    public void getSuggestionServiceComponentName_shouldReturnAndroidPackage() {
        assertThat(mProvider.getSuggestionServiceComponent().getPackageName())
                .isEqualTo("com.android.settings.intelligence");
    }

    @Test
    public void isSuggestionEnabled_isLowMemoryDevice_shouldReturnFalse() {
        when(mActivityManager.isLowRamDevice()).thenReturn(true);

        assertThat(mProvider.isSuggestionEnabled(mContext)).isFalse();
    }

    @Test
    public void isSuggestionV2Enabled_isNotLowMemoryDevice_shouldReturnTrue() {
        when(mActivityManager.isLowRamDevice()).thenReturn(false);
        assertThat(mProvider.isSuggestionEnabled(mContext)).isTrue();
    }

    @Test
    public void dismissSuggestion_noControllerOrSuggestion_noop() {
        mProvider.dismissSuggestion(mContext, null, null);
        mProvider.dismissSuggestion(mContext, mSuggestionControllerMixin, null);
        mProvider.dismissSuggestion(mContext, null, new Suggestion.Builder("id").build());

        verifyZeroInteractions(mFactory.metricsFeatureProvider);
        verifyZeroInteractions(mSuggestionControllerMixin);
    }

    @Test
    public void dismissSuggestion_noContext_shouldDoNothing() {
        mProvider.dismissSuggestion(null, mSuggestionControllerMixin, mSuggestion);

        verifyZeroInteractions(mFactory.metricsFeatureProvider);
    }

    @Test
    public void dismissSuggestion_shouldLogAndDismiss() {
        mProvider.dismissSuggestion(mContext, mSuggestionControllerMixin, mSuggestion);

        verify(mFactory.metricsFeatureProvider).action(
                eq(mContext),
                eq(MetricsProto.MetricsEvent.ACTION_SETTINGS_DISMISS_SUGGESTION),
                anyString());
        verify(mSuggestionControllerMixin).dismissSuggestion(mSuggestion);
    }

    @Test
    public void filterExclusiveSuggestions_shouldOnlyKeepFirst3() {
        final List<Tile> suggestions = new ArrayList<>();
        suggestions.add(new Tile());
        suggestions.add(new Tile());
        suggestions.add(new Tile());
        suggestions.add(new Tile());
        suggestions.add(new Tile());
        suggestions.add(new Tile());
        suggestions.add(new Tile());

        mProvider.filterExclusiveSuggestions(suggestions);

        assertThat(suggestions).hasSize(3);
    }

    @Test
    public void testGetSmartSuggestionEnabledTaggedData_disabled() {
        assertThat(mProvider.getLoggingTaggedData(mContext)).asList().containsExactly(
                Pair.create(MetricsEvent.FIELD_SETTINGS_SMART_SUGGESTIONS_ENABLED, 0));
    }

    @Test
    public void testGetSmartSuggestionEnabledTaggedData_enabled() {
        final SuggestionFeatureProvider provider = spy(mProvider);
        when(provider.isSmartSuggestionEnabled(any(Context.class))).thenReturn(true);

        assertThat(provider.getLoggingTaggedData(mContext)).asList().containsExactly(
                Pair.create(MetricsEvent.FIELD_SETTINGS_SMART_SUGGESTIONS_ENABLED, 1));
    }
}
