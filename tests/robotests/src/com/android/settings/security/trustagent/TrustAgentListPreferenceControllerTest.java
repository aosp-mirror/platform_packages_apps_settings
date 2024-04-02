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

package com.android.settings.security.trustagent;

import static com.android.settings.security.trustagent.TrustAgentListPreferenceController.PREF_KEY_SECURITY_CATEGORY;
import static com.android.settings.security.trustagent.TrustAgentListPreferenceController.PREF_KEY_TRUST_AGENT;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static java.util.Objects.requireNonNull;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceViewHolder;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.security.SecuritySettings;
import com.android.settings.security.trustagent.TrustAgentManager.TrustAgentComponentInfo;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;
import com.android.settingslib.RestrictedPreference;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.search.SearchIndexableRaw;

import com.google.common.collect.Maps;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
public class TrustAgentListPreferenceControllerTest {

    @Mock
    private TrustAgentManager mTrustAgentManager;
    @Mock
    private LockPatternUtils mLockPatternUtils;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private PreferenceCategory mCategory;
    @Mock
    private SecuritySettings mFragment;

    private Lifecycle mLifecycle;
    private LifecycleOwner mLifecycleOwner;
    private FakeFeatureFactory mFeatureFactory;
    private Activity mActivity;

    private TrustAgentListPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mActivity = Robolectric.buildActivity(Activity.class).get();
        mLifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(mLifecycleOwner);
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        when(mFeatureFactory.securityFeatureProvider.getLockPatternUtils(any(Context.class)))
                .thenReturn(mLockPatternUtils);
        when(mFeatureFactory.securityFeatureProvider.getTrustAgentManager())
                .thenReturn(mTrustAgentManager);
        when(mCategory.getKey()).thenReturn(PREF_KEY_SECURITY_CATEGORY);
        when(mCategory.getContext()).thenReturn(mActivity);
        when(mScreen.findPreference(PREF_KEY_SECURITY_CATEGORY)).thenReturn(mCategory);
        mController = new TrustAgentListPreferenceController(mActivity, mFragment, mLifecycle);
    }

    @Test
    public void testConstants() {
        assertThat(mController.isAvailable()).isTrue();
        assertThat(mController.getPreferenceKey()).isEqualTo(PREF_KEY_TRUST_AGENT);
        assertThat(mController).isInstanceOf(PreferenceControllerMixin.class);
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void isAvailable_whenNotVisible_isFalse() {
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void onResume_shouldClearOldAgents() {
        final Preference oldAgent = new Preference(mActivity);
        oldAgent.setKey(PREF_KEY_TRUST_AGENT + 0);
        when(mCategory.findPreference(PREF_KEY_TRUST_AGENT + 0))
                .thenReturn(oldAgent)
                .thenReturn(null);
        final List<TrustAgentComponentInfo> agents = new ArrayList<>();
        final TrustAgentComponentInfo agent = createTrustAgentComponentInfo(null);
        agents.add(agent);
        when(mTrustAgentManager.getActiveTrustAgents(mActivity, mLockPatternUtils))
                .thenReturn(agents);

        mController.displayPreference(mScreen);
        mController.onResume();

        verify(mCategory).removePreference(oldAgent);
    }

    @Test
    public void onResume_shouldAddNewAgents() {
        final List<TrustAgentComponentInfo> agents = new ArrayList<>();
        final TrustAgentComponentInfo agent = createTrustAgentComponentInfo(null);
        agents.add(agent);
        when(mTrustAgentManager.getActiveTrustAgents(mActivity, mLockPatternUtils))
                .thenReturn(agents);

        mController.displayPreference(mScreen);
        mController.onResume();

        verify(mCategory, atLeastOnce()).addPreference(any(Preference.class));
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void onResume_ifNotAvailable_shouldNotAddNewAgents() {
        final List<TrustAgentComponentInfo> agents = new ArrayList<>();
        final TrustAgentComponentInfo agent = createTrustAgentComponentInfo(null);
        agents.add(agent);
        when(mTrustAgentManager.getActiveTrustAgents(mActivity, mLockPatternUtils))
                .thenReturn(agents);

        mController.displayPreference(mScreen);
        mController.onResume();

        verify(mCategory, never()).addPreference(any(Preference.class));
    }

    @Test
    public void onResume_controllerShouldHasKey() {
        final List<TrustAgentComponentInfo> agents = new ArrayList<>();
        final TrustAgentComponentInfo agent = createTrustAgentComponentInfo(null);
        agents.add(agent);
        when(mTrustAgentManager.getActiveTrustAgents(mActivity, mLockPatternUtils))
                .thenReturn(agents);
        final String key = PREF_KEY_TRUST_AGENT + 0;

        mController.displayPreference(mScreen);
        mController.onResume();

        assertThat(mController.mTrustAgentsKeyList).containsExactly(key);
    }

    @Test
    public void onResume_shouldShowDisabledByAdminRestrictedPreference() {
        final List<TrustAgentComponentInfo> agents = new ArrayList<>();
        final TrustAgentComponentInfo agent = createTrustAgentComponentInfo(new EnforcedAdmin());
        final Map<String, Preference> preferences = setUpPreferenceMap();
        agents.add(agent);
        when(mTrustAgentManager.getActiveTrustAgents(mActivity, mLockPatternUtils))
                .thenReturn(agents);

        mController.displayPreference(mScreen);
        mController.onResume();

        assertThat(preferences).hasSize(1);
        Preference preference = preferences.values().iterator().next();
        assertThat(preference).isInstanceOf(RestrictedPreference.class);
        RestrictedPreference restrictedPreference = (RestrictedPreference) preference;
        assertThat(restrictedPreference.isDisabledByAdmin()).isTrue();
    }

    @Test
    public void onResume_restrictedPreferenceShouldUseAdminDisabledSummary() {
        final List<TrustAgentComponentInfo> agents = new ArrayList<>();
        final TrustAgentComponentInfo agent = createTrustAgentComponentInfo(new EnforcedAdmin());
        final Map<String, Preference> preferences = setUpPreferenceMap();
        final LayoutInflater inflater = LayoutInflater.from(mActivity);
        agents.add(agent);
        when(mTrustAgentManager.getActiveTrustAgents(mActivity, mLockPatternUtils))
                .thenReturn(agents);
        mController.displayPreference(mScreen);
        mController.onResume();
        final RestrictedPreference restrictedPreference =
                (RestrictedPreference) preferences.values().iterator().next();
        final PreferenceViewHolder viewHolder = PreferenceViewHolder.createInstanceForTests(
                inflater.inflate(restrictedPreference.getLayoutResource(), null));

        restrictedPreference.onBindViewHolder(viewHolder);

        final TextView summaryView = (TextView) requireNonNull(
                viewHolder.findViewById(android.R.id.summary));
        assertThat(summaryView.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(summaryView.getText().toString()).isEqualTo(
                mActivity.getString(
                        com.android.settingslib.R.string.disabled_by_admin_summary_text));
    }

    private Map<String, Preference> setUpPreferenceMap() {
        final Map<String, Preference> preferences = Maps.newLinkedHashMap();
        when(mCategory.addPreference(any())).thenAnswer((invocation) -> {
            Preference preference = invocation.getArgument(0);
            preferences.put(preference.getKey(), preference);
            return true;
        });
        when(mCategory.removePreference(any())).thenAnswer((invocation) -> {
            Preference preference = invocation.getArgument(0);
            return preferences.remove(preference.getKey()) != null;
        });
        return preferences;
    }

    @Test
    public void updateDynamicRawDataToIndex_shouldIndexAgents() {
        final List<TrustAgentComponentInfo> agents = new ArrayList<>();
        final TrustAgentComponentInfo agent = createTrustAgentComponentInfo(null);
        agents.add(agent);
        when(mTrustAgentManager.getActiveTrustAgents(mActivity, mLockPatternUtils))
                .thenReturn(agents);
        final List<SearchIndexableRaw> indexRaws = new ArrayList<>();

        mController.updateDynamicRawDataToIndex(indexRaws);

        assertThat(indexRaws).hasSize(1);
    }

    @NonNull
    private static TrustAgentComponentInfo createTrustAgentComponentInfo(
            @Nullable EnforcedAdmin admin) {
        final TrustAgentComponentInfo agent = new TrustAgentComponentInfo();
        agent.title = "Test_title";
        agent.summary = "test summary";
        agent.componentName = new ComponentName("pkg", "agent");
        agent.admin = admin;
        return agent;
    }
}
