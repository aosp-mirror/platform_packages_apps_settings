/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.accessibility;

import static com.android.settings.accessibility.AccessibilitySettingsForSetupWizard.SCREEN_READER_PACKAGE_NAME;
import static com.android.settings.accessibility.AccessibilitySettingsForSetupWizard.SCREEN_READER_SERVICE_NAME;
import static com.android.settings.accessibility.AccessibilitySettingsForSetupWizard.SELECT_TO_SPEAK_PACKAGE_NAME;
import static com.android.settings.accessibility.AccessibilitySettingsForSetupWizard.SELECT_TO_SPEAK_SERVICE_NAME;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.view.accessibility.AccessibilityManager;

import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settingslib.RestrictedPreference;

import com.google.android.setupcompat.template.FooterBarMixin;
import com.google.android.setupdesign.GlifPreferenceLayout;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests for {@link AccessibilitySettingsForSetupWizard}.
 */
@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        com.android.settings.testutils.shadow.ShadowFragment.class,
})
public class AccessibilitySettingsForSetupWizardTest {

    private static final ComponentName TEST_SCREEN_READER_COMPONENT_NAME = new ComponentName(
            SCREEN_READER_PACKAGE_NAME, SCREEN_READER_SERVICE_NAME);
    private static final ComponentName TEST_SELECT_TO_SPEAK_COMPONENT_NAME = new ComponentName(
            SELECT_TO_SPEAK_PACKAGE_NAME, SELECT_TO_SPEAK_SERVICE_NAME);
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private final List<AccessibilityServiceInfo> mAccessibilityServices = new ArrayList<>();
    @Rule
    public MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Mock
    private AccessibilityManager mAccessibilityManager;
    @Mock
    private FragmentActivity mActivity;
    @Mock
    private GlifPreferenceLayout mGlifLayoutView;
    @Mock
    private FooterBarMixin mFooterBarMixin;
    private AccessibilitySettingsForSetupWizard mFragment;

    @Before
    public void setUp() {
        mFragment = spy(new TestAccessibilitySettingsForSetupWizard(mContext));
        doReturn(mAccessibilityManager).when(mActivity).getSystemService(
                AccessibilityManager.class);
        when(mAccessibilityManager.getInstalledAccessibilityServiceList()).thenReturn(
                mAccessibilityServices);
        doReturn(mActivity).when(mFragment).getActivity();
        doReturn(mock(LifecycleOwner.class)).when(mFragment).getViewLifecycleOwner();
        doReturn(mFooterBarMixin).when(mGlifLayoutView).getMixin(FooterBarMixin.class);
    }

    @Test
    public void onViewCreated_verifyAction() {
        mFragment.onViewCreated(mGlifLayoutView, null);

        verify(mGlifLayoutView).setHeaderText(
                mContext.getString(R.string.vision_settings_title));
        verify(mGlifLayoutView).setDescriptionText(
                mContext.getString(R.string.vision_settings_description));
        verify(mFooterBarMixin).setPrimaryButton(any());
    }

    @Test
    public void onResume_accessibilityServiceListInstalled_returnExpectedValue() {
        addEnabledServiceInfo(TEST_SCREEN_READER_COMPONENT_NAME, true);
        addEnabledServiceInfo(TEST_SELECT_TO_SPEAK_COMPONENT_NAME, true);
        mFragment.onAttach(mContext);
        mFragment.onViewCreated(mGlifLayoutView, null);

        mFragment.onResume();

        assertRestrictedPreferenceMatch(mFragment.mScreenReaderPreference,
                TEST_SCREEN_READER_COMPONENT_NAME.getPackageName(),
                TEST_SCREEN_READER_COMPONENT_NAME.flattenToString());
        assertRestrictedPreferenceMatch(mFragment.mSelectToSpeakPreference,
                TEST_SELECT_TO_SPEAK_COMPONENT_NAME.getPackageName(),
                TEST_SELECT_TO_SPEAK_COMPONENT_NAME.flattenToString());
    }

    @Test
    public void onResume_accessibilityServiceListNotInstalled_returnNull() {
        mFragment.onAttach(mContext);
        mFragment.onViewCreated(mGlifLayoutView, null);

        mFragment.onResume();

        assertThat(mFragment.mScreenReaderPreference.getKey()).isNull();
        assertThat(mFragment.mSelectToSpeakPreference.getKey()).isNull();
    }

    private void addEnabledServiceInfo(ComponentName componentName, boolean isAccessibilityTool) {
        final AccessibilityServiceInfo a11yServiceInfo = mock(AccessibilityServiceInfo.class);
        when(a11yServiceInfo.getComponentName()).thenReturn(componentName);
        when(a11yServiceInfo.isAccessibilityTool()).thenReturn(isAccessibilityTool);
        final ResolveInfo resolveInfo = mock(ResolveInfo.class);
        when(a11yServiceInfo.getResolveInfo()).thenReturn(resolveInfo);
        resolveInfo.serviceInfo = mock(ServiceInfo.class);
        resolveInfo.serviceInfo.packageName = componentName.getPackageName();
        resolveInfo.serviceInfo.name = componentName.getClassName();
        when(resolveInfo.loadLabel(mActivity.getPackageManager())).thenReturn(
                componentName.getPackageName());
        mAccessibilityServices.add(a11yServiceInfo);
    }

    private void assertRestrictedPreferenceMatch(RestrictedPreference preference, String title,
            String key) {
        assertThat(preference.getTitle().toString()).isEqualTo(title);
        assertThat(preference.getKey()).isEqualTo(key);
        assertThat(preference.getExtras().getString(AccessibilitySettings.EXTRA_TITLE))
                .isEqualTo(title);
        assertThat(preference.getExtras().getString(AccessibilitySettings.EXTRA_PREFERENCE_KEY))
                .isEqualTo(key);
    }

    private static class TestAccessibilitySettingsForSetupWizard
            extends AccessibilitySettingsForSetupWizard {

        private final Context mContext;
        private final PreferenceManager mPreferenceManager;

        TestAccessibilitySettingsForSetupWizard(Context context) {
            super();
            mContext = context;
            mPreferenceManager = new PreferenceManager(context);
            mPreferenceManager.setPreferences(mPreferenceManager.createPreferenceScreen(context));
            mDisplayMagnificationPreference = new Preference(context);
            mScreenReaderPreference = new RestrictedPreference(context);
            mSelectToSpeakPreference = new RestrictedPreference(context);
        }

        @Override
        public int getPreferenceScreenResId() {
            return R.xml.placeholder_prefs;
        }

        @Override
        public PreferenceScreen getPreferenceScreen() {
            return mPreferenceManager.getPreferenceScreen();
        }

        @Override
        public PreferenceManager getPreferenceManager() {
            return mPreferenceManager;
        }

        @Override
        public Context getContext() {
            return mContext;
        }
    }
}
