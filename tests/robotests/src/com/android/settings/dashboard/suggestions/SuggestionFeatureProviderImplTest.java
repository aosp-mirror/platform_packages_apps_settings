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
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.provider.Settings.Secure;
import android.util.Pair;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.Settings.AmbientDisplayPickupSuggestionActivity;
import com.android.settings.Settings.AmbientDisplaySuggestionActivity;
import com.android.settings.Settings.DoubleTapPowerSuggestionActivity;
import com.android.settings.Settings.DoubleTwistSuggestionActivity;
import com.android.settings.Settings.NightDisplaySuggestionActivity;
import com.android.settings.Settings.SwipeToNotificationSuggestionActivity;
import com.android.settings.TestConfig;
import com.android.settings.gestures.DoubleTapPowerSettings;
import com.android.settings.gestures.DoubleTapScreenSettings;
import com.android.settings.gestures.PickupGestureSettings;
import com.android.settings.gestures.SwipeToNotificationSettings;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.shadow.SettingsShadowResources;
import com.android.settings.testutils.shadow.ShadowSecureSettings;
import com.android.settingslib.drawer.Tile;
import com.android.settingslib.suggestions.SuggestionParser;

import java.time.LocalDateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(
    manifest = TestConfig.MANIFEST_PATH,
    sdk = TestConfig.SDK_VERSION,
    shadows = {ShadowSecureSettings.class, SettingsShadowResources.class}
)
public class SuggestionFeatureProviderImplTest {

    private static final String DOUBLE_TWIST_SENSOR_NAME = "double_twist_sensor_name";
    private static final String DOUBLE_TWIST_SENSOR_VENDOR = "double_twist_sensor_vendor";

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock
    private SuggestionParser mSuggestionParser;
    @Mock
    private Tile mSuggestion;
    @Mock
    private ActivityManager mActivityManager;
    @Mock
    private PackageManager mPackageManager;
    @Mock
    private FingerprintManager mFingerprintManager;
    @Mock
    private SharedPreferences mSharedPreferences;
    @Captor
    private ArgumentCaptor<Pair> mTaggedDataCaptor = ArgumentCaptor.forClass(Pair.class);

    private FakeFeatureFactory mFactory;
    private SuggestionFeatureProviderImpl mProvider;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        FakeFeatureFactory.setupForTest(mContext);
        mFactory = (FakeFeatureFactory) FakeFeatureFactory.getFactory(mContext);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        // Explicit casting to object due to MockitoCast bug
        when((Object) mContext.getSystemService(FingerprintManager.class))
                .thenReturn(mFingerprintManager);
        when(mContext.getApplicationContext()).thenReturn(RuntimeEnvironment.application);
        when(mContext.getSystemService(Context.ACTIVITY_SERVICE)).thenReturn(mActivityManager);
        when(mActivityManager.isLowRamDevice()).thenReturn(false);

        mSuggestion.intent = new Intent().setClassName("pkg", "cls");
        mSuggestion.category = "category";

        mProvider = new SuggestionFeatureProviderImpl(mContext);
    }

    @Test
    public void isSuggestionCompleted_doubleTapPower_trueWhenNotAvailable() {
        SettingsShadowResources.overrideResource(
                com.android.internal.R.bool.config_cameraDoubleTapPowerGestureEnabled, false);

        assertThat(mProvider.isSuggestionCompleted(RuntimeEnvironment.application,
                new ComponentName(RuntimeEnvironment.application,
                        DoubleTapPowerSuggestionActivity.class))).isTrue();
    }

    @Test
    public void isSuggestionCompleted_doubleTapPower_falseWhenNotVisited() {
        SettingsShadowResources.overrideResource(
                com.android.internal.R.bool.config_cameraDoubleTapPowerGestureEnabled, true);
        // No stored value in shared preferences if not visited yet.

        assertThat(mProvider.isSuggestionCompleted(RuntimeEnvironment.application,
                new ComponentName(RuntimeEnvironment.application,
                        DoubleTapPowerSuggestionActivity.class))).isFalse();
    }

    @Test
    public void isSuggestionCompleted_doubleTapPower_trueWhenVisited() {
        SettingsShadowResources.overrideResource(
                com.android.internal.R.bool.config_cameraDoubleTapPowerGestureEnabled, true);
        mProvider.getSharedPrefs(RuntimeEnvironment.application).edit().putBoolean(
                DoubleTapPowerSettings.PREF_KEY_SUGGESTION_COMPLETE, true).commit();

        assertThat(mProvider.isSuggestionCompleted(RuntimeEnvironment.application,
                new ComponentName(RuntimeEnvironment.application,
                        DoubleTapPowerSuggestionActivity.class))).isTrue();
    }

    @Test
    public void isSuggestionCompleted_doubleTwist_trueWhenNotAvailable() {
        SettingsShadowResources.overrideResource(
                R.string.gesture_double_twist_sensor_name, "nonexistant name");
        SettingsShadowResources.overrideResource(
                R.string.gesture_double_twist_sensor_vendor, "nonexistant vendor");

        assertThat(mProvider.isSuggestionCompleted(RuntimeEnvironment.application,
                new ComponentName(RuntimeEnvironment.application,
                        DoubleTwistSuggestionActivity.class))).isTrue();
    }

    @Test
    public void isSuggestionCompleted_ambientDisplay_falseWhenNotVisited() {
        SettingsShadowResources.overrideResource(
                com.android.internal.R.string.config_dozeComponent, "foo");
        SettingsShadowResources.overrideResource(
                com.android.internal.R.string.config_dozeDoubleTapSensorType, "bar");
        // No stored value in shared preferences if not visited yet.

        assertThat(mProvider.isSuggestionCompleted(RuntimeEnvironment.application,
                new ComponentName(RuntimeEnvironment.application,
                        AmbientDisplaySuggestionActivity.class))).isFalse();
    }

    @Test
    public void isSuggestionCompleted_ambientDisplay_trueWhenVisited() {
        SettingsShadowResources.overrideResource(
                com.android.internal.R.string.config_dozeComponent, "foo");
        SettingsShadowResources.overrideResource(
                com.android.internal.R.string.config_dozeDoubleTapSensorType, "bar");
        mProvider.getSharedPrefs(RuntimeEnvironment.application).edit().putBoolean(
                DoubleTapScreenSettings.PREF_KEY_SUGGESTION_COMPLETE, true).commit();

        assertThat(mProvider.isSuggestionCompleted(RuntimeEnvironment.application,
                new ComponentName(RuntimeEnvironment.application,
                        AmbientDisplaySuggestionActivity.class))).isTrue();
    }

    @Test
    public void isSuggestionCompleted_ambientDisplayPickup_falseWhenNotVisited() {
        SettingsShadowResources.overrideResource(
                com.android.internal.R.string.config_dozeComponent, "foo");
        SettingsShadowResources.overrideResource(
                com.android.internal.R.bool.config_dozePulsePickup, true);
        // No stored value in shared preferences if not visited yet.

        assertThat(mProvider.isSuggestionCompleted(RuntimeEnvironment.application,
                new ComponentName(RuntimeEnvironment.application,
                        AmbientDisplaySuggestionActivity.class))).isFalse();
    }

    @Test
    public void isSuggestionCompleted_ambientDisplayPickup_trueWhenVisited() {
        SettingsShadowResources.overrideResource(
                com.android.internal.R.string.config_dozeComponent, "foo");
        SettingsShadowResources.overrideResource(
                com.android.internal.R.bool.config_dozePulsePickup, true);
        mProvider.getSharedPrefs(RuntimeEnvironment.application).edit().putBoolean(
                PickupGestureSettings.PREF_KEY_SUGGESTION_COMPLETE, true).commit();

        assertThat(mProvider.isSuggestionCompleted(RuntimeEnvironment.application,
                new ComponentName(RuntimeEnvironment.application,
                        AmbientDisplayPickupSuggestionActivity.class))).isTrue();
    }

    @Test
    public void isSuggestionCompleted_swipeToNotification_trueWhenNotHardwareNotAvailable() {
        stubFingerprintSupported(true);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(false);
        when(mContext.getResources().
                getBoolean(com.android.internal.R.bool.config_supportSystemNavigationKeys))
                .thenReturn(true);

        assertThat(mProvider.isSuggestionCompleted(mContext,
                new ComponentName(mContext, SwipeToNotificationSuggestionActivity.class))).isTrue();
    }

    @Test
    public void isSuggestionCompleted_swipeToNotification_trueWhenNotAvailable() {
        stubFingerprintSupported(true);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mContext.getResources().
                getBoolean(com.android.internal.R.bool.config_supportSystemNavigationKeys))
                .thenReturn(false);

        assertThat(mProvider.isSuggestionCompleted(mContext,
                new ComponentName(mContext, SwipeToNotificationSuggestionActivity.class))).isTrue();
    }

    @Test
    public void isSuggestionCompleted_swipeToNotification_falseWhenNotVisited() {
        stubFingerprintSupported(true);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mContext.getResources().
                getBoolean(com.android.internal.R.bool.config_supportSystemNavigationKeys))
                .thenReturn(true);
        // No stored value in shared preferences if not visited yet.

        assertThat(mProvider.isSuggestionCompleted(mContext,
                new ComponentName(mContext,
                        SwipeToNotificationSuggestionActivity.class))).isFalse();
    }

    @Test
    public void isSuggestionCompleted_swipeToNotification_trueWhenVisited() {
        stubFingerprintSupported(true);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mContext.getResources().
                getBoolean(com.android.internal.R.bool.config_supportSystemNavigationKeys))
                .thenReturn(true);
        when(mContext.getSharedPreferences(anyString(), anyInt())).thenReturn(mSharedPreferences);
        when(mSharedPreferences.getBoolean(
                SwipeToNotificationSettings.PREF_KEY_SUGGESTION_COMPLETE, false)).thenReturn(true);

        assertThat(mProvider.isSuggestionCompleted(mContext,
                new ComponentName(mContext, SwipeToNotificationSuggestionActivity.class))).isTrue();
    }

    @Test
    public void isSuggestionEnabled_isLowMemoryDevice_shouldReturnFalse() {
        when(mActivityManager.isLowRamDevice()).thenReturn(true);

        assertThat(mProvider.isSuggestionEnabled(mContext)).isFalse();
    }

    @Test
    public void isSuggestionEnabled_isNotLowMemoryDevice_shouldReturnTrue() {
        when(mActivityManager.isLowRamDevice()).thenReturn(false);

        assertThat(mProvider.isSuggestionEnabled(mContext)).isTrue();
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
    public void getSuggestionIdentifier_nullContext_shouldNotCrash() {
        final Tile suggestion = new Tile();
        suggestion.intent = new Intent()
                .setClassName(RuntimeEnvironment.application.getPackageName(), "123");
        assertThat(mProvider.getSuggestionIdentifier(null, suggestion))
                .isNotEmpty();
    }

    @Test
    public void dismissSuggestion_hasMoreDismissCount_shouldNotDisableComponent() {
        when(mSuggestionParser.dismissSuggestion(any(Tile.class)))
                .thenReturn(false);
        mProvider.dismissSuggestion(mContext, mSuggestionParser, mSuggestion);

        verify(mFactory.metricsFeatureProvider).action(
                eq(mContext),
                eq(MetricsProto.MetricsEvent.ACTION_SETTINGS_DISMISS_SUGGESTION),
                anyString(),
                mTaggedDataCaptor.capture());
        assertThat(mTaggedDataCaptor.getAllValues()).containsExactly(
                Pair.create(MetricsEvent.FIELD_SETTINGS_SMART_SUGGESTIONS_ENABLED, 0));
        verify(mContext, never()).getPackageManager();
    }

    @Test
    public void dismissSuggestion_noContext_shouldDoNothing() {
        mProvider.dismissSuggestion(null, mSuggestionParser, mSuggestion);

        verifyZeroInteractions(mFactory.metricsFeatureProvider);
    }

    @Test
    public void dismissSuggestion_hasNoMoreDismissCount_shouldDisableComponent() {
        when(mSuggestionParser.dismissSuggestion(any(Tile.class)))
                .thenReturn(true);

        mProvider.dismissSuggestion(mContext, mSuggestionParser, mSuggestion);

        verify(mFactory.metricsFeatureProvider).action(
                eq(mContext),
                eq(MetricsProto.MetricsEvent.ACTION_SETTINGS_DISMISS_SUGGESTION),
                anyString(),
                mTaggedDataCaptor.capture());
        assertThat(mTaggedDataCaptor.getAllValues()).containsExactly(
                Pair.create(MetricsEvent.FIELD_SETTINGS_SMART_SUGGESTIONS_ENABLED, 0));
        verify(mContext.getPackageManager())
                .setComponentEnabledSetting(mSuggestion.intent.getComponent(),
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP);
    }

    private void stubFingerprintSupported(boolean enabled) {
        when(mPackageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT))
                .thenReturn(enabled);
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
    public void hasUsedNightDisplay_returnsFalse_byDefault() {
        assertThat(mProvider.hasUsedNightDisplay(mContext)).isFalse();
    }

    @Test
    public void hasUsedNightDisplay_returnsTrue_ifPreviouslyActivatedAndManual() {
        Secure.putString(mContext.getContentResolver(), Secure.NIGHT_DISPLAY_LAST_ACTIVATED_TIME,
                LocalDateTime.now().toString());
        Secure.putInt(mContext.getContentResolver(), Secure.NIGHT_DISPLAY_AUTO_MODE, 1);
        assertThat(mProvider.hasUsedNightDisplay(mContext)).isTrue();
    }

    @Test
    public void nightDisplaySuggestion_isCompleted_ifPreviouslyActivated() {
        Secure.putString(mContext.getContentResolver(), Secure.NIGHT_DISPLAY_LAST_ACTIVATED_TIME,
                LocalDateTime.now().toString());
        final ComponentName componentName =
                new ComponentName(mContext, NightDisplaySuggestionActivity.class);
        assertThat(mProvider.isSuggestionCompleted(mContext, componentName)).isTrue();
    }

    @Test
    public void nightDisplaySuggestion_isCompleted_ifNonManualMode() {
        Secure.putInt(mContext.getContentResolver(), Secure.NIGHT_DISPLAY_AUTO_MODE, 1);
        final ComponentName componentName =
                new ComponentName(mContext, NightDisplaySuggestionActivity.class);
        assertThat(mProvider.isSuggestionCompleted(mContext, componentName)).isTrue();
    }

    @Test
    public void nightDisplaySuggestion_isCompleted_ifPreviouslyCleared() {
        Secure.putString(mContext.getContentResolver(), Secure.NIGHT_DISPLAY_LAST_ACTIVATED_TIME,
                null);
        Secure.putInt(mContext.getContentResolver(), Secure.NIGHT_DISPLAY_AUTO_MODE, 1);
        final ComponentName componentName =
                new ComponentName(mContext, NightDisplaySuggestionActivity.class);
        assertThat(mProvider.isSuggestionCompleted(mContext, componentName)).isTrue();
    }

    @Test
    public void nightDisplaySuggestion_isNotCompleted_byDefault() {
        final ComponentName componentName =
                new ComponentName(mContext, NightDisplaySuggestionActivity.class);
        assertThat(mProvider.isSuggestionCompleted(mContext, componentName)).isFalse();
    }
}
