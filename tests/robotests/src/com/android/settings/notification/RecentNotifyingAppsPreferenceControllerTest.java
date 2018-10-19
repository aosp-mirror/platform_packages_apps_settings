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

package com.android.settings.notification;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.UserHandle;
import android.os.UserManager;
import android.service.notification.NotifyingApp;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import android.text.TextUtils;

import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settingslib.applications.AppUtils;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.applications.instantapps.InstantAppDataProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

import java.util.ArrayList;
import java.util.List;

@RunWith(SettingsRobolectricTestRunner.class)
public class RecentNotifyingAppsPreferenceControllerTest {

    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private PreferenceCategory mCategory;
    @Mock
    private Preference mSeeAllPref;
    @Mock
    private PreferenceCategory mDivider;
    @Mock
    private UserManager mUserManager;
    @Mock
    private ApplicationsState mAppState;
    @Mock
    private PackageManager mPackageManager;
    @Mock
    private ApplicationsState.AppEntry mAppEntry;
    @Mock
    private ApplicationInfo mApplicationInfo;
    @Mock
    private NotificationBackend mBackend;
    @Mock
    private Fragment mHost;
    @Mock
    private Activity mActivity;

    private Context mContext;
    private RecentNotifyingAppsPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        doReturn(mUserManager).when(mContext).getSystemService(Context.USER_SERVICE);
        doReturn(mPackageManager).when(mContext).getPackageManager();

        mController = new RecentNotifyingAppsPreferenceController(
                mContext, mBackend, mAppState, mHost);
        when(mScreen.findPreference(anyString())).thenReturn(mCategory);

        when(mScreen.findPreference(RecentNotifyingAppsPreferenceController.KEY_SEE_ALL))
                .thenReturn(mSeeAllPref);
        when(mScreen.findPreference(RecentNotifyingAppsPreferenceController.KEY_DIVIDER))
                .thenReturn(mDivider);
        when(mCategory.getContext()).thenReturn(mContext);
        when(mHost.getActivity()).thenReturn(mActivity);
    }

    @Test
    public void isAlwaysAvailable() {
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void doNotIndexCategory() {
        final List<String> nonIndexable = new ArrayList<>();

        mController.updateNonIndexableKeys(nonIndexable);

        assertThat(nonIndexable).containsAllOf(mController.getPreferenceKey(),
                RecentNotifyingAppsPreferenceController.KEY_DIVIDER);
    }

    @Test
    public void onDisplayAndUpdateState_shouldRefreshUi() {
        mController = spy(new RecentNotifyingAppsPreferenceController(
                mContext, null, (ApplicationsState) null, null));

        doNothing().when(mController).refreshUi(mContext);

        mController.displayPreference(mScreen);
        mController.updateState(mCategory);

        verify(mController, times(2)).refreshUi(mContext);
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void display_shouldNotShowRecents_showAppInfoPreference() {
        mController.displayPreference(mScreen);

        verify(mCategory, never()).addPreference(any(Preference.class));
        verify(mCategory).setTitle(null);
        verify(mSeeAllPref).setTitle(R.string.notifications_title);
        verify(mSeeAllPref).setIcon(null);
        verify(mDivider).setVisible(false);
    }

    @Test
    public void display_showRecents() {
        final List<NotifyingApp> apps = new ArrayList<>();
        final NotifyingApp app1 = new NotifyingApp()
                .setPackage("pkg.class")
                .setLastNotified(System.currentTimeMillis());
        final NotifyingApp app2 = new NotifyingApp()
                .setLastNotified(System.currentTimeMillis())
                .setPackage("com.android.settings");
        final NotifyingApp app3 = new NotifyingApp()
                .setLastNotified(System.currentTimeMillis() - 1000)
                .setPackage("pkg.class2");

        apps.add(app1);
        apps.add(app2);
        apps.add(app3);

        // app1, app2 are valid apps. app3 is invalid.
        when(mAppState.getEntry(app1.getPackage(), UserHandle.myUserId()))
                .thenReturn(mAppEntry);
        when(mAppState.getEntry(app2.getPackage(), UserHandle.myUserId()))
                .thenReturn(mAppEntry);
        when(mAppState.getEntry(app3.getPackage(), UserHandle.myUserId()))
                .thenReturn(null);
        when(mPackageManager.resolveActivity(any(Intent.class), anyInt())).thenReturn(
                new ResolveInfo());
        when(mBackend.getRecentApps()).thenReturn(apps);
        mAppEntry.info = mApplicationInfo;

        mController.displayPreference(mScreen);

        verify(mCategory).setTitle(R.string.recent_notifications);
        // Only add app1. app2 is skipped because of the package name, app3 skipped because
        // it's invalid app.
        verify(mCategory, times(1)).addPreference(any(Preference.class));

        verify(mSeeAllPref).setSummary(null);
        verify(mSeeAllPref).setIcon(R.drawable.ic_chevron_right_24dp);
        verify(mDivider).setVisible(true);
    }

    @Test
    public void display_showRecentsWithInstantApp() {
        // Regular app.
        final List<NotifyingApp> apps = new ArrayList<>();
        final NotifyingApp app1 = new NotifyingApp().
                setLastNotified(System.currentTimeMillis())
                .setPackage("com.foo.bar");
        apps.add(app1);

        // Instant app.
        final NotifyingApp app2 = new NotifyingApp()
                .setLastNotified(System.currentTimeMillis() + 200)
                .setPackage("com.foo.barinstant");
        apps.add(app2);

        ApplicationsState.AppEntry app1Entry = mock(ApplicationsState.AppEntry.class);
        ApplicationsState.AppEntry app2Entry = mock(ApplicationsState.AppEntry.class);
        app1Entry.info = mApplicationInfo;
        app2Entry.info = mApplicationInfo;

        when(mAppState.getEntry(app1.getPackage(), UserHandle.myUserId())).thenReturn(app1Entry);
        when(mAppState.getEntry(app2.getPackage(), UserHandle.myUserId())).thenReturn(app2Entry);

        // Only the regular app app1 should have its intent resolve.
        when(mPackageManager.resolveActivity(argThat(intentMatcher(app1.getPackage())),
                anyInt())).thenReturn(new ResolveInfo());

        when(mBackend.getRecentApps()).thenReturn(apps);

        // Make sure app2 is considered an instant app.
        ReflectionHelpers.setStaticField(AppUtils.class, "sInstantAppDataProvider",
                (InstantAppDataProvider) (ApplicationInfo info) -> {
                    if (info == app2Entry.info) {
                        return true;
                    } else {
                        return false;
                    }
                });

        mController.displayPreference(mScreen);

        ArgumentCaptor<Preference> prefCaptor = ArgumentCaptor.forClass(Preference.class);
        verify(mCategory, times(2)).addPreference(prefCaptor.capture());
        List<Preference> prefs = prefCaptor.getAllValues();
        assertThat(prefs.get(1).getKey()).isEqualTo(app1.getPackage());
        assertThat(prefs.get(0).getKey()).isEqualTo(app2.getPackage());
    }

    @Test
    public void display_hasRecentButNoneDisplayable_showAppInfo() {
        final List<NotifyingApp> apps = new ArrayList<>();
        final NotifyingApp app1 = new NotifyingApp()
                .setPackage("com.android.phone")
                .setLastNotified(System.currentTimeMillis());
        final NotifyingApp app2 = new NotifyingApp()
                .setPackage("com.android.settings")
                .setLastNotified(System.currentTimeMillis());
        apps.add(app1);
        apps.add(app2);

        // app1, app2 are not displayable
        when(mAppState.getEntry(app1.getPackage(), UserHandle.myUserId()))
                .thenReturn(mock(ApplicationsState.AppEntry.class));
        when(mAppState.getEntry(app2.getPackage(), UserHandle.myUserId()))
                .thenReturn(mock(ApplicationsState.AppEntry.class));
        when(mPackageManager.resolveActivity(any(Intent.class), anyInt())).thenReturn(
                new ResolveInfo());
        when(mBackend.getRecentApps()).thenReturn(apps);

        mController.displayPreference(mScreen);

        verify(mCategory, never()).addPreference(any(Preference.class));
        verify(mCategory).setTitle(null);
        verify(mSeeAllPref).setTitle(R.string.notifications_title);
        verify(mSeeAllPref).setIcon(null);
    }

    @Test
    public void display_showRecents_formatSummary() {
        final List<NotifyingApp> apps = new ArrayList<>();
        final NotifyingApp app1 = new NotifyingApp()
                .setLastNotified(System.currentTimeMillis())
                .setPackage("pkg.class");
        apps.add(app1);

        when(mAppState.getEntry(app1.getPackage(), UserHandle.myUserId()))
                .thenReturn(mAppEntry);
        when(mPackageManager.resolveActivity(any(Intent.class), anyInt())).thenReturn(
                new ResolveInfo());
        when(mBackend.getRecentApps()).thenReturn(apps);
        mAppEntry.info = mApplicationInfo;

        mController.displayPreference(mScreen);

        verify(mCategory).addPreference(argThat(summaryMatches("Just now")));
    }

    private static ArgumentMatcher<Preference> summaryMatches(String expected) {
        return preference -> TextUtils.equals(expected, preference.getSummary());
    }

    // Used for matching an intent with a specific package name.
    private static ArgumentMatcher<Intent> intentMatcher(String packageName) {
        return intent -> packageName.equals(intent.getPackage());
    }
}
