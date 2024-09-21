/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.notification.modes;

import static android.app.NotificationManager.META_DATA_AUTOMATIC_RULE_TYPE;
import static android.app.NotificationManager.META_DATA_RULE_INSTANCE_LIMIT;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Flags;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ComponentInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import com.android.settings.notification.modes.ZenModesListAddModePreferenceController.ModeType;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.MoreExecutors;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.List;
import java.util.function.Function;

@RunWith(RobolectricTestRunner.class)
@EnableFlags(Flags.FLAG_MODES_UI)
public class ZenModesListAddModePreferenceControllerTest {

    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private Context mContext;
    private ZenModesListAddModePreferenceController mController;

    @Mock private ZenModesListAddModePreferenceController.OnAddModeListener mListener;
    @Mock private ZenServiceListing mZenServiceListing;
    @Mock private ConfigurationActivityHelper mConfigurationActivityHelper;
    @Mock private NotificationManager mNm;
    @Mock private PackageManager mPm;

    @Captor private ArgumentCaptor<List<ModeType>> mListenerCaptor;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.getApplication();
        Function<ApplicationInfo, Drawable> appIconRetriever = appInfo -> new ColorDrawable();

        mController = new ZenModesListAddModePreferenceController(mContext, "add_mode", mListener,
                mZenServiceListing, mConfigurationActivityHelper, mNm, mPm, appIconRetriever,
                MoreExecutors.newDirectExecutorService(), MoreExecutors.directExecutor());

        when(mConfigurationActivityHelper.getConfigurationActivityFromApprovedComponent(any()))
                .thenAnswer((Answer<ComponentName>) invocationOnMock -> {
                    // By default, assume the ComponentInfo is also the configurationActivity.
                    ComponentInfo ci = invocationOnMock.getArgument(0);
                    return ci != null ? ci.getComponentName() : null;
                });
    }

    @Test
    public void onClickAddMode_noAppProviders_onlyOptionIsCustom() {
        when(mZenServiceListing.loadApprovedComponents()).thenReturn(ImmutableSet.of());

        mController.onClickAddMode();

        verify(mListener).onAvailableModeTypesForAdd(mListenerCaptor.capture());
        List<ModeType> options = mListenerCaptor.getValue();
        assertThat(options).hasSize(1);
        assertThat(options.get(0).name()).isEqualTo("Custom");
        assertThat(options.get(0).summary()).isNull();
        assertThat(options.get(0).icon()).isNotNull();
        assertThat(options.get(0).creationActivityIntent()).isNull();
    }

    @Test
    public void onClickAddMode_someAppProviders_includedInOptions() {
        ImmutableSet<ComponentInfo> approvedComponents = ImmutableSet.of(
                newComponentInfoWithValidMetadata("pkg1"),
                newComponentInfoWithValidMetadata("pkg2"));
        when(mZenServiceListing.loadApprovedComponents()).thenReturn(approvedComponents);

        mController.onClickAddMode();

        verify(mListener).onAvailableModeTypesForAdd(mListenerCaptor.capture());
        List<ModeType> options = mListenerCaptor.getValue();
        assertThat(options).hasSize(3);

        assertThat(options.get(1).name()).isEqualTo("Rule by pkg1");
        assertThat(options.get(1).summary()).isEqualTo("A package called pkg1");
        assertThat(options.get(1).icon()).isNotNull();
        assertThat(options.get(1).creationActivityIntent()).isNotNull();
        assertThat(options.get(1).creationActivityIntent().getComponent()).isEqualTo(
                new ComponentName("pkg1", "pkg1.activity"));

        assertThat(options.get(0).name()).isEqualTo("Custom");
        assertThat(options.get(2).name()).isEqualTo("Rule by pkg2");
    }

    @Test
    public void onClickAddMode_someAppProviders_optionsAreSorted() {
        ImmutableSet<ComponentInfo> approvedComponents = ImmutableSet.of(
                newComponentInfoWithValidMetadata("pkg_Z"),
                newComponentInfoWithValidMetadata("pkg_A"),
                newComponentInfoWithValidMetadata("pkg_F"),
                newComponentInfoWithValidMetadata("pkg_C"));
        when(mZenServiceListing.loadApprovedComponents()).thenReturn(approvedComponents);

        mController.onClickAddMode();

        verify(mListener).onAvailableModeTypesForAdd(mListenerCaptor.capture());
        List<ModeType> options = mListenerCaptor.getValue();
        assertThat(options).hasSize(5);
        assertThat(options.stream().map(o -> o.name()).toList())
                .containsExactly("Custom", "Rule by pkg_A", "Rule by pkg_C", "Rule by pkg_F",
                        "Rule by pkg_Z")
                .inOrder();
    }

    @Test
    public void onClickAddMode_appProviderWithMissingMetadata_notAnOption() {
        ComponentInfo componentWithoutRuleType = newComponentInfoWithValidMetadata("pkg1");
        componentWithoutRuleType.metaData.remove(META_DATA_AUTOMATIC_RULE_TYPE);
        ImmutableSet<ComponentInfo> approvedComponents = ImmutableSet.of(
                componentWithoutRuleType, newComponentInfoWithValidMetadata("pkg2"));
        when(mZenServiceListing.loadApprovedComponents()).thenReturn(approvedComponents);

        mController.onClickAddMode();

        verify(mListener).onAvailableModeTypesForAdd(mListenerCaptor.capture());
        List<ModeType> options = mListenerCaptor.getValue();
        assertThat(options).hasSize(2);
        assertThat(options.get(0).name()).isEqualTo("Custom");
        assertThat(options.get(1).name()).isEqualTo("Rule by pkg2");
    }

    @Test
    public void onClickAddMode_appProviderWithRuleLimitExceeded_notAnOption() {
        ComponentInfo componentWithLimitThreeRules = newComponentInfoWithValidMetadata("pkg1");
        componentWithLimitThreeRules.metaData.putInt(META_DATA_RULE_INSTANCE_LIMIT, 3);
        ImmutableSet<ComponentInfo> approvedComponents = ImmutableSet.of(
                componentWithLimitThreeRules, newComponentInfoWithValidMetadata("pkg2"));
        when(mZenServiceListing.loadApprovedComponents()).thenReturn(approvedComponents);
        when(mNm.getRuleInstanceCount(any())).thenReturn(3); // Already 3 created rules.

        mController.onClickAddMode();

        verify(mListener).onAvailableModeTypesForAdd(mListenerCaptor.capture());
        List<ModeType> options = mListenerCaptor.getValue();
        assertThat(options).hasSize(2);
        assertThat(options.get(0).name()).isEqualTo("Custom");
        assertThat(options.get(1).name()).isEqualTo("Rule by pkg2");
        verify(mNm).getRuleInstanceCount(eq(componentWithLimitThreeRules.getComponentName()));
    }

    @Test
    public void onClickAddMode_appProviderWithoutConfigurationActivity_notAnOption() {
        ComponentInfo componentWithoutConfigActivity = newComponentInfoWithValidMetadata("pkg2");
        ImmutableSet<ComponentInfo> approvedComponents = ImmutableSet.of(
                newComponentInfoWithValidMetadata("pkg1"), componentWithoutConfigActivity);
        when(mZenServiceListing.loadApprovedComponents()).thenReturn(approvedComponents);
        when(mConfigurationActivityHelper.getConfigurationActivityFromApprovedComponent(any()))
                .thenAnswer((Answer<ComponentName>) invocationOnMock -> {
                    ComponentInfo ci = invocationOnMock.getArgument(0);
                    if (ci == componentWithoutConfigActivity) {
                        return null;
                    } else {
                        return ci.getComponentName();
                    }
                });

        mController.onClickAddMode();

        verify(mListener).onAvailableModeTypesForAdd(mListenerCaptor.capture());
        List<ModeType> options = mListenerCaptor.getValue();
        assertThat(options).hasSize(2);
        assertThat(options.get(0).name()).isEqualTo("Custom");
        assertThat(options.get(1).name()).isEqualTo("Rule by pkg1");
    }

    private ComponentInfo newComponentInfoWithValidMetadata(String pkg) {
        ComponentInfo ci = new ActivityInfo();

        ci.applicationInfo = mock(ApplicationInfo.class);
        when(ci.applicationInfo.loadLabel(any())).thenReturn("A package called " + pkg);
        when(ci.applicationInfo.loadUnbadgedIcon(any())).thenReturn(new ColorDrawable());
        ci.packageName = pkg;
        ci.name = pkg + ".activity";
        ci.metaData = new Bundle();
        ci.metaData.putString(META_DATA_AUTOMATIC_RULE_TYPE, "Rule by " + pkg);

        return ci;
    }
}
