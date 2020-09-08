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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.usage.IUsageStatsManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageEvents.Event;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Parcel;
import android.os.UserHandle;
import android.os.UserManager;
import android.service.notification.NotifyingApp;
import android.text.TextUtils;

import com.android.settings.R;
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
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

import java.util.ArrayList;
import java.util.List;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

@RunWith(RobolectricTestRunner.class)
public class RecentNotifyingAppsPreferenceControllerTest {

    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private PreferenceCategory mCategory;
    @Mock
    private Preference mSeeAllPref;
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
    private FragmentActivity mActivity;
    @Mock
    private IUsageStatsManager mIUsageStatsManager;

    private Context mContext;
    private RecentNotifyingAppsPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        doReturn(mUserManager).when(mContext).getSystemService(Context.USER_SERVICE);
        doReturn(mPackageManager).when(mContext).getPackageManager();
        when(mUserManager.getProfileIdsWithDisabled(0)).thenReturn(new int[] {0});

        mController = new RecentNotifyingAppsPreferenceController(
                mContext, mBackend, mIUsageStatsManager, mUserManager, mAppState, mHost);
        when(mScreen.findPreference(anyString())).thenReturn(mCategory);

        when(mScreen.findPreference(RecentNotifyingAppsPreferenceController.KEY_SEE_ALL))
                .thenReturn(mSeeAllPref);
        when(mCategory.getContext()).thenReturn(mContext);
        when(mHost.getActivity()).thenReturn(mActivity);
    }

    @Test
    public void isAlwaysAvailable() {
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void onDisplayAndUpdateState_shouldRefreshUi() {
        mController = spy(new RecentNotifyingAppsPreferenceController(
                mContext, null, mIUsageStatsManager, mUserManager, (ApplicationsState) null, null));

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
    }

    @Test
    public void display_showRecents() throws Exception {
        List<Event> events = new ArrayList<>();
        Event app = new Event();
        app.mEventType = Event.NOTIFICATION_INTERRUPTION;
        app.mPackage = "a";
        app.mTimeStamp = System.currentTimeMillis();
        events.add(app);
        Event app1 = new Event();
        app1.mEventType = Event.NOTIFICATION_INTERRUPTION;
        app1.mPackage = "com.android.settings";
        app1.mTimeStamp = System.currentTimeMillis();
        events.add(app1);
        Event app2 = new Event();
        app2.mEventType = Event.NOTIFICATION_INTERRUPTION;
        app2.mPackage = "pkg.class2";
        app2.mTimeStamp = System.currentTimeMillis() - 1000;
        events.add(app2);

        // app1, app2 are valid apps. app3 is invalid.
        when(mAppState.getEntry(app.getPackageName(), UserHandle.myUserId()))
                .thenReturn(mAppEntry);
        when(mAppState.getEntry(app1.getPackageName(), UserHandle.myUserId()))
                .thenReturn(mAppEntry);
        when(mAppState.getEntry(app2.getPackageName(), UserHandle.myUserId()))
                .thenReturn(null);
        when(mPackageManager.resolveActivity(any(Intent.class), anyInt())).thenReturn(
                new ResolveInfo());

        UsageEvents usageEvents = getUsageEvents(
                new String[] {app.getPackageName(), app1.getPackageName(), app2.getPackageName()},
                events);
        when(mIUsageStatsManager.queryEventsForUser(anyLong(), anyLong(), anyInt(), anyString()))
                .thenReturn(usageEvents);

        mAppEntry.info = mApplicationInfo;

        mController.displayPreference(mScreen);

        verify(mCategory).setTitle(R.string.recent_notifications);
        // Only add app1 & app2. app3 skipped because it's invalid app.
        verify(mCategory, times(2)).addPreference(any(Preference.class));

        verify(mSeeAllPref).setSummary(null);
        verify(mSeeAllPref).setIcon(R.drawable.ic_chevron_right_24dp);
    }

    @Test
    public void display_showRecentsWithInstantApp() throws Exception {
        List<Event> events = new ArrayList<>();
        Event app = new Event();
        app.mEventType = Event.NOTIFICATION_INTERRUPTION;
        app.mPackage = "com.foo.bar";
        app.mTimeStamp = System.currentTimeMillis();
        events.add(app);
        Event app1 = new Event();
        app1.mEventType = Event.NOTIFICATION_INTERRUPTION;
        app1.mPackage = "com.foo.barinstant";
        app1.mTimeStamp = System.currentTimeMillis() + 200;
        events.add(app1);
        UsageEvents usageEvents = getUsageEvents(
                new String[] {"com.foo.bar", "com.foo.barinstant"}, events);
        when(mIUsageStatsManager.queryEventsForUser(anyLong(), anyLong(), anyInt(), anyString()))
                .thenReturn(usageEvents);

        ApplicationsState.AppEntry app1Entry = mock(ApplicationsState.AppEntry.class);
        ApplicationsState.AppEntry app2Entry = mock(ApplicationsState.AppEntry.class);
        app1Entry.info = mApplicationInfo;
        app2Entry.info = mApplicationInfo;

        when(mAppState.getEntry(
                app.getPackageName(), UserHandle.myUserId())).thenReturn(app1Entry);
        when(mAppState.getEntry(
                app1.getPackageName(), UserHandle.myUserId())).thenReturn(app2Entry);

        // Only the regular app app1 should have its intent resolve.
        when(mPackageManager.resolveActivity(argThat(intentMatcher(app.getPackageName())),
                anyInt())).thenReturn(new ResolveInfo());

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
        assertThat(prefs.get(1).getKey()).isEqualTo(
                RecentNotifyingAppsPreferenceController.getKey(UserHandle.myUserId(),
                        app.getPackageName()));
        assertThat(prefs.get(0).getKey()).isEqualTo(
                RecentNotifyingAppsPreferenceController.getKey(UserHandle.myUserId(),
                        app1.getPackageName()));
    }

    @Test
    public void display_showRecents_formatSummary() throws Exception {
        List<Event> events = new ArrayList<>();
        Event app = new Event();
        app.mEventType = Event.NOTIFICATION_INTERRUPTION;
        app.mPackage = "pkg.class";
        app.mTimeStamp = System.currentTimeMillis();
        events.add(app);
        UsageEvents usageEvents = getUsageEvents(new String[] {"pkg.class"}, events);
        when(mIUsageStatsManager.queryEventsForUser(anyLong(), anyLong(), anyInt(), anyString()))
                .thenReturn(usageEvents);

        when(mAppState.getEntry(app.getPackageName(), UserHandle.myUserId()))
                .thenReturn(mAppEntry);
        when(mPackageManager.resolveActivity(any(Intent.class), anyInt())).thenReturn(
                new ResolveInfo());

        mAppEntry.info = mApplicationInfo;

        mController.displayPreference(mScreen);

        verify(mCategory).addPreference(argThat(summaryMatches("Just now")));
    }

    @Test
    public void reloadData() throws Exception {
        when(mUserManager.getProfileIdsWithDisabled(0)).thenReturn(new int[] {0, 10});

        mController = new RecentNotifyingAppsPreferenceController(
                mContext, mBackend, mIUsageStatsManager, mUserManager, mAppState, mHost);

        List<Event> events = new ArrayList<>();
        Event app = new Event();
        app.mEventType = Event.NOTIFICATION_INTERRUPTION;
        app.mPackage = "b";
        app.mTimeStamp = 1;
        events.add(app);
        Event app1 = new Event();
        app1.mEventType = Event.MAX_EVENT_TYPE;
        app1.mPackage = "com.foo.bar";
        app1.mTimeStamp = 10;
        events.add(app1);
        UsageEvents usageEvents = getUsageEvents(
                new String[] {"b", "com.foo.bar"}, events);
        when(mIUsageStatsManager.queryEventsForUser(anyLong(), anyLong(), eq(0), anyString()))
                .thenReturn(usageEvents);

        List<Event> events10 = new ArrayList<>();
        Event app10 = new Event();
        app10.mEventType = Event.NOTIFICATION_INTERRUPTION;
        app10.mPackage = "a";
        app10.mTimeStamp = 2;
        events10.add(app10);
        Event app10a = new Event();
        app10a.mEventType = Event.NOTIFICATION_INTERRUPTION;
        app10a.mPackage = "a";
        app10a.mTimeStamp = 20;
        events10.add(app10a);
        UsageEvents usageEvents10 = getUsageEvents(
                new String[] {"a"}, events10);
        when(mIUsageStatsManager.queryEventsForUser(anyLong(), anyLong(), eq(10), anyString()))
                .thenReturn(usageEvents10);

        mController.reloadData();

        assertThat(mController.mApps.size()).isEqualTo(2);
        boolean foundPkg0 = false;
        boolean foundPkg10 = false;
        for (NotifyingApp notifyingApp : mController.mApps) {
            if (notifyingApp.getLastNotified() == 20
                    && notifyingApp.getPackage().equals("a")
                    && notifyingApp.getUserId() == 10) {
                foundPkg10 = true;
            }
            if (notifyingApp.getLastNotified() == 1
                    && notifyingApp.getPackage().equals("b")
                    && notifyingApp.getUserId() == 0) {
                foundPkg0 = true;
            }
        }
        assertThat(foundPkg0).isTrue();
        assertThat(foundPkg10).isTrue();
    }

    private static ArgumentMatcher<Preference> summaryMatches(String expected) {
        return preference -> TextUtils.equals(expected, preference.getSummary());
    }

    // Used for matching an intent with a specific package name.
    private static ArgumentMatcher<Intent> intentMatcher(String packageName) {
        return intent -> packageName.equals(intent.getPackage());
    }

    private UsageEvents getUsageEvents(String[] pkgs, List<Event> events) {
        UsageEvents usageEvents = new UsageEvents(events, pkgs);
        Parcel parcel = Parcel.obtain();
        parcel.setDataPosition(0);
        usageEvents.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        return UsageEvents.CREATOR.createFromParcel(parcel);
    }
}
