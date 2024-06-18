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

package com.android.settings.accessibility.shortcuts;

import static com.android.internal.accessibility.common.ShortcutConstants.SERVICES_SEPARATOR;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.os.UserHandle;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.provider.Settings;
import android.text.SpannableStringBuilder;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.Flags;

import androidx.fragment.app.FragmentActivity;

import com.android.internal.accessibility.common.ShortcutConstants;
import com.android.internal.accessibility.util.ShortcutUtils;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SubSettings;
import com.android.settings.accessibility.AccessibilityButtonFragment;
import com.android.settings.accessibility.FloatingMenuSizePreferenceController;
import com.android.settings.testutils.AccessibilityTestUtils;
import com.android.settings.utils.AnnotationSpan;
import com.android.settingslib.accessibility.AccessibilityUtils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;

import java.util.List;
import java.util.Set;

/**
 * Tests for {@link SoftwareShortcutOptionPreferenceController}
 */
@RunWith(RobolectricTestRunner.class)
public class SoftwareShortcutOptionPreferenceControllerTest {
    private static final String PREF_KEY = "prefKey";
    private static final String TARGET_MAGNIFICATION =
            "com.android.server.accessibility.MagnificationController";
    private static final ComponentName TARGET_ALWAYS_ON_A11Y_SERVICE =
            new ComponentName("FakePackage", "AlwaysOnA11yService");
    private static final ComponentName TARGET_STANDARD_A11Y_SERVICE =
            new ComponentName("FakePackage", "StandardA11yService");
    private static final String SOFTWARE_SHORTCUT_SETTING_NAME =
            Settings.Secure.ACCESSIBILITY_BUTTON_TARGETS;
    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    private Context mContext;
    private AccessibilityManager mAccessibilityManager;
    private TestSoftwareShortcutOptionPreferenceController mController;

    @Before
    public void setUp() {
        mContext = spy(Robolectric.buildActivity(FragmentActivity.class).get());
        mAccessibilityManager = AccessibilityTestUtils.setupMockAccessibilityManager(mContext);

        AccessibilityServiceInfo mAlwaysOnServiceInfo =
                AccessibilityTestUtils.createAccessibilityServiceInfo(
                        mContext, TARGET_ALWAYS_ON_A11Y_SERVICE, /* isAlwaysOnService= */ true);
        AccessibilityServiceInfo mStandardServiceInfo =
                AccessibilityTestUtils.createAccessibilityServiceInfo(
                        mContext, TARGET_STANDARD_A11Y_SERVICE, /* isAlwaysOnService= */ false);
        AccessibilityManager am = mock(AccessibilityManager.class);
        when(mContext.getSystemService(Context.ACCESSIBILITY_SERVICE)).thenReturn(am);
        when(am.getInstalledAccessibilityServiceList()).thenReturn(
                List.of(mAlwaysOnServiceInfo, mStandardServiceInfo));

        mController = new TestSoftwareShortcutOptionPreferenceController(mContext, PREF_KEY);
        mController.setShortcutTargets(Set.of(TARGET_MAGNIFICATION));
    }

    @Test
    public void isChecked_allTargetsHasShortcutConfigured_returnTrue() {
        Settings.Secure.putString(
                mContext.getContentResolver(), SOFTWARE_SHORTCUT_SETTING_NAME,
                String.join(String.valueOf(SERVICES_SEPARATOR),
                        TARGET_MAGNIFICATION,
                        TARGET_STANDARD_A11Y_SERVICE.flattenToString(),
                        TARGET_ALWAYS_ON_A11Y_SERVICE.flattenToString())
        );
        mController.setShortcutTargets(
                Set.of(TARGET_MAGNIFICATION,
                        TARGET_ALWAYS_ON_A11Y_SERVICE.flattenToString(),
                        TARGET_STANDARD_A11Y_SERVICE.flattenToString()));

        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void isChecked_someTargetsHasShortcutConfigured_returnFalse() {
        Settings.Secure.putString(
                mContext.getContentResolver(), SOFTWARE_SHORTCUT_SETTING_NAME,
                String.join(String.valueOf(SERVICES_SEPARATOR),
                        TARGET_MAGNIFICATION,
                        TARGET_STANDARD_A11Y_SERVICE.flattenToString())
        );
        mController.setShortcutTargets(
                Set.of(TARGET_MAGNIFICATION,
                        TARGET_ALWAYS_ON_A11Y_SERVICE.flattenToString(),
                        TARGET_STANDARD_A11Y_SERVICE.flattenToString()));

        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void isChecked_noTargetsHasShortcutConfigured_returnFalse() {
        Settings.Secure.putString(
                mContext.getContentResolver(), SOFTWARE_SHORTCUT_SETTING_NAME, "");
        mController.setShortcutTargets(
                Set.of(TARGET_MAGNIFICATION,
                        TARGET_ALWAYS_ON_A11Y_SERVICE.flattenToString(),
                        TARGET_STANDARD_A11Y_SERVICE.flattenToString()));

        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void getCustomizedAccessibilityButtonLink_verifyText() {
        String expected =
                mContext.getString(
                        R.string.accessibility_shortcut_edit_dialog_summary_software_floating);

        CharSequence spannable = mController.getCustomizeAccessibilityButtonLink();

        assertThat(spannable.toString()).isEqualTo(expected);
    }

    @Test
    public void getCustomizedAccessibilityButtonLink_verifyClickAction() {
        String expected =
                mContext.getString(
                        R.string.accessibility_shortcut_edit_dialog_summary_software_floating);

        CharSequence spannable = mController.getCustomizeAccessibilityButtonLink();

        assertThat(spannable).isInstanceOf(SpannableStringBuilder.class);
        AnnotationSpan[] spans = ((SpannableStringBuilder) spannable).getSpans(
                0, expected.length(), AnnotationSpan.class);
        spans[0].onClick(new View(mContext));
        assertLaunchSettingsPage(AccessibilityButtonFragment.class.getName());
    }

    @Test
    @DisableFlags(Flags.FLAG_A11Y_QS_SHORTCUT)
    public void enableShortcutForTargets_enableShortcut_shortcutTurnedOn() {
        String target = TARGET_ALWAYS_ON_A11Y_SERVICE.flattenToString();
        mController.setShortcutTargets(Set.of(target));
        assertThat(ShortcutUtils.isComponentIdExistingInSettings(
                mContext, ShortcutConstants.UserShortcutType.SOFTWARE, target
        )).isFalse();

        mController.enableShortcutForTargets(true);

        assertThat(ShortcutUtils.isComponentIdExistingInSettings(
                mContext, ShortcutConstants.UserShortcutType.SOFTWARE, target
        )).isTrue();
    }

    @Test
    @EnableFlags(Flags.FLAG_A11Y_QS_SHORTCUT)
    public void enableShortcutForTargets_enableShortcut_callA11yManager() {
        String target = TARGET_ALWAYS_ON_A11Y_SERVICE.flattenToString();
        mController.setShortcutTargets(Set.of(target));
        assertThat(ShortcutUtils.isComponentIdExistingInSettings(
                mContext, ShortcutConstants.UserShortcutType.SOFTWARE, target
        )).isFalse();

        mController.enableShortcutForTargets(true);

        verify(mAccessibilityManager).enableShortcutsForTargets(
                /* enable= */ true,
                ShortcutConstants.UserShortcutType.SOFTWARE,
                Set.of(target),
                UserHandle.myUserId()
        );
        verifyNoMoreInteractions(mAccessibilityManager);
    }

    @Test
    @DisableFlags(Flags.FLAG_A11Y_QS_SHORTCUT)
    public void enableShortcutForTargets_disableShortcut_shortcutTurnedOff() {
        String target = TARGET_ALWAYS_ON_A11Y_SERVICE.flattenToString();
        ShortcutUtils.optInValueToSettings(
                mContext, ShortcutConstants.UserShortcutType.SOFTWARE, target);
        assertThat(ShortcutUtils.isComponentIdExistingInSettings(
                mContext, ShortcutConstants.UserShortcutType.SOFTWARE, target
        )).isTrue();
        mController.setShortcutTargets(Set.of(target));

        mController.enableShortcutForTargets(false);

        assertThat(ShortcutUtils.isComponentIdExistingInSettings(
                mContext, ShortcutConstants.UserShortcutType.SOFTWARE, target
        )).isFalse();
    }

    @Test
    @EnableFlags(Flags.FLAG_A11Y_QS_SHORTCUT)
    public void enableShortcutForTargets_disableShortcut_callA11yManager() {
        String target = TARGET_ALWAYS_ON_A11Y_SERVICE.flattenToString();
        ShortcutUtils.optInValueToSettings(
                mContext, ShortcutConstants.UserShortcutType.SOFTWARE, target);
        assertThat(ShortcutUtils.isComponentIdExistingInSettings(
                mContext, ShortcutConstants.UserShortcutType.SOFTWARE, target
        )).isTrue();
        mController.setShortcutTargets(Set.of(target));

        mController.enableShortcutForTargets(false);

        verify(mAccessibilityManager).enableShortcutsForTargets(
                /* enable= */ false,
                ShortcutConstants.UserShortcutType.SOFTWARE,
                Set.of(target),
                UserHandle.myUserId()
        );
        verifyNoMoreInteractions(mAccessibilityManager);
    }

    @Test
    @DisableFlags(Flags.FLAG_A11Y_QS_SHORTCUT)
    public void enableShortcutForTargets_enableShortcutWithMagnification_menuSizeIncreased() {
        mController.setShortcutTargets(Set.of(TARGET_MAGNIFICATION));

        mController.enableShortcutForTargets(true);

        assertThat(
                Settings.Secure.getInt(
                        mContext.getContentResolver(),
                        Settings.Secure.ACCESSIBILITY_FLOATING_MENU_SIZE,
                        FloatingMenuSizePreferenceController.Size.UNKNOWN))
                .isEqualTo(FloatingMenuSizePreferenceController.Size.LARGE);
    }

    @Test
    @DisableFlags(Flags.FLAG_A11Y_QS_SHORTCUT)
    public void enableShortcutForTargets_enableShortcutWithMagnification_userConfigureSmallMenuSize_menuSizeNotChanged() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_FLOATING_MENU_SIZE,
                FloatingMenuSizePreferenceController.Size.SMALL);
        mController.setShortcutTargets(Set.of(TARGET_MAGNIFICATION));

        mController.enableShortcutForTargets(true);

        assertThat(
                Settings.Secure.getInt(
                        mContext.getContentResolver(),
                        Settings.Secure.ACCESSIBILITY_FLOATING_MENU_SIZE,
                        FloatingMenuSizePreferenceController.Size.UNKNOWN))
                .isEqualTo(FloatingMenuSizePreferenceController.Size.SMALL);
    }

    @Test
    @DisableFlags(Flags.FLAG_A11Y_QS_SHORTCUT)
    public void enableShortcutForTargets_enableAlwaysOnServiceShortcut_turnsOnAlwaysOnService() {
        mController.setShortcutTargets(
                Set.of(TARGET_ALWAYS_ON_A11Y_SERVICE.flattenToString()));

        mController.enableShortcutForTargets(true);

        assertThat(AccessibilityUtils.getEnabledServicesFromSettings(mContext))
                .contains(TARGET_ALWAYS_ON_A11Y_SERVICE);
    }

    @Test
    @DisableFlags(Flags.FLAG_A11Y_QS_SHORTCUT)
    public void enableShortcutForTargets_disableAlwaysOnServiceShortcut_turnsOffAlwaysOnService() {
        mController.setShortcutTargets(
                Set.of(TARGET_ALWAYS_ON_A11Y_SERVICE.flattenToString()));

        mController.enableShortcutForTargets(false);

        assertThat(AccessibilityUtils.getEnabledServicesFromSettings(mContext))
                .doesNotContain(TARGET_ALWAYS_ON_A11Y_SERVICE);
    }

    @Test
    @DisableFlags(Flags.FLAG_A11Y_QS_SHORTCUT)
    public void enableShortcutForTargets_enableStandardServiceShortcut_wontTurnOnService() {
        mController.setShortcutTargets(
                Set.of(TARGET_STANDARD_A11Y_SERVICE.flattenToString()));

        mController.enableShortcutForTargets(true);

        assertThat(AccessibilityUtils.getEnabledServicesFromSettings(mContext))
                .doesNotContain(TARGET_STANDARD_A11Y_SERVICE);
    }

    @Test
    @DisableFlags(Flags.FLAG_A11Y_QS_SHORTCUT)
    public void enableShortcutForTargets_disableStandardServiceShortcutWithServiceOn_wontTurnOffService() {
        mController.setShortcutTargets(
                Set.of(TARGET_STANDARD_A11Y_SERVICE.flattenToString()));
        AccessibilityUtils.setAccessibilityServiceState(
                mContext, TARGET_STANDARD_A11Y_SERVICE, /* enabled= */ true);

        mController.enableShortcutForTargets(false);

        assertThat(AccessibilityUtils.getEnabledServicesFromSettings(mContext))
                .contains(TARGET_STANDARD_A11Y_SERVICE);
    }

    private void assertLaunchSettingsPage(String page) {
        ContextWrapper applicationContext = (Application) mContext.getApplicationContext();
        final Intent intent = Shadows.shadowOf(applicationContext).getNextStartedActivity();
        assertThat(intent).isNotNull();
        assertThat(intent.getAction()).isEqualTo(Intent.ACTION_MAIN);
        assertThat(intent.getComponent()).isEqualTo(
                new ComponentName(applicationContext, SubSettings.class));
        assertThat(intent.getExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT)).isEqualTo(page);
    }

    private static class TestSoftwareShortcutOptionPreferenceController
            extends SoftwareShortcutOptionPreferenceController {

        TestSoftwareShortcutOptionPreferenceController(
                Context context, String preferenceKey) {
            super(context, preferenceKey);
        }

        @Override
        protected boolean isShortcutAvailable() {
            return true;
        }
    }
}
