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
 *
 */

package com.android.settings.slices;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.app.slice.Slice;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.Settings;
import android.provider.SettingsSlicesContract;
import android.util.Pair;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.testutils.FakeIndexProvider;
import com.android.settings.search.SearchFeatureProvider;
import com.android.settings.search.SearchFeatureProviderImpl;
import com.android.settings.testutils.DatabaseTestUtils;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.FakeSliderController;
import com.android.settings.testutils.FakeToggleController;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RuntimeEnvironment;

@RunWith(SettingsRobolectricTestRunner.class)
public class SliceBroadcastReceiverTest {

    private final String fakeTitle = "title";
    private final String fakeSummary = "summary";
    private final String fakeScreenTitle = "screen_title";
    private final int fakeIcon = 1234;
    private final String fakeFragmentClassName = FakeIndexProvider.class.getName();
    private final Class fakeControllerName = FakeToggleController.class;

    private Context mContext;
    private SQLiteDatabase mDb;
    private SliceBroadcastReceiver mReceiver;
    private SearchFeatureProvider mSearchFeatureProvider;
    private FakeFeatureFactory mFakeFeatureFactory;
    private ArgumentCaptor<Pair<Integer, Object>> mLoggingNameArgumentCatpor;
    private ArgumentCaptor<Pair<Integer, Object>> mLoggingValueArgumentCatpor;

    @Before
    public void setUp() {
        mContext = spy(RuntimeEnvironment.application);
        mDb = SlicesDatabaseHelper.getInstance(mContext).getWritableDatabase();
        mReceiver = new SliceBroadcastReceiver();
        SlicesDatabaseHelper helper = SlicesDatabaseHelper.getInstance(mContext);
        helper.setIndexedState();
        mSearchFeatureProvider = new SearchFeatureProviderImpl();
        mFakeFeatureFactory = FakeFeatureFactory.setupForTest();
        mFakeFeatureFactory.searchFeatureProvider = mSearchFeatureProvider;
        mLoggingNameArgumentCatpor = ArgumentCaptor.forClass(Pair.class);
        mLoggingValueArgumentCatpor = ArgumentCaptor.forClass(Pair.class);
    }

    @After
    public void cleanUp() {
        DatabaseTestUtils.clearDb(mContext);
    }

    @Test
    public void onReceive_toggleChanged() {
        final String key = "key";
        final Uri uri = new Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority(SettingsSliceProvider.SLICE_AUTHORITY)
                .appendPath(SettingsSlicesContract.PATH_SETTING_ACTION)
                .appendPath(key)
                .build();
        mSearchFeatureProvider.getSearchIndexableResources().getProviderValues().clear();
        insertSpecialCase(key);
        final ContentResolver resolver = mock(ContentResolver.class);
        doReturn(resolver).when(mContext).getContentResolver();
        // Turn on toggle setting
        FakeToggleController fakeToggleController = new FakeToggleController(mContext, key);
        fakeToggleController.setChecked(true);
        Intent intent = new Intent(SettingsSliceProvider.ACTION_TOGGLE_CHANGED);
        intent.putExtra(SettingsSliceProvider.EXTRA_SLICE_KEY, key);

        assertThat(fakeToggleController.isChecked()).isTrue();

        // Toggle setting
        mReceiver.onReceive(mContext, intent);

        assertThat(fakeToggleController.isChecked()).isFalse();
        verify(mFakeFeatureFactory.metricsFeatureProvider)
                .action(eq(mContext),
                        eq(MetricsEvent.ACTION_SETTINGS_SLICE_CHANGED),
                        mLoggingNameArgumentCatpor.capture(),
                        mLoggingValueArgumentCatpor.capture());

        final Pair<Integer, Object> namePair = mLoggingNameArgumentCatpor.getValue();
        final Pair<Integer, Object> valuePair = mLoggingValueArgumentCatpor.getValue();
        assertThat(namePair.first).isEqualTo(MetricsEvent.FIELD_SETTINGS_PREFERENCE_CHANGE_NAME);
        assertThat(namePair.second).isEqualTo(fakeToggleController.getPreferenceKey());

        verify(resolver).notifyChange(uri, null);
        assertThat(valuePair.first)
                .isEqualTo(MetricsEvent.FIELD_SETTINGS_PREFERENCE_CHANGE_INT_VALUE);
        assertThat(valuePair.second).isEqualTo(0);
    }

    @Test
    public void toggleUpdate_synchronously_notifyChange_should_be_called() {
        // Monitor the ContentResolver
        final ContentResolver resolver = spy(mContext.getContentResolver());
        doReturn(resolver).when(mContext).getContentResolver();

        final String key = "key";
        mSearchFeatureProvider.getSearchIndexableResources().getProviderValues().clear();
        insertSpecialCase(key);

        FakeToggleController fakeToggleController = new FakeToggleController(mContext, key);
        fakeToggleController.setChecked(true);
        // Set the toggle setting update synchronously.
        fakeToggleController.setAsyncUpdate(false);
        Intent intent = new Intent(SettingsSliceProvider.ACTION_TOGGLE_CHANGED);
        intent.putExtra(SettingsSliceProvider.EXTRA_SLICE_KEY, key);

        assertThat(fakeToggleController.isChecked()).isTrue();

        // Toggle setting
        mReceiver.onReceive(mContext, intent);

        assertThat(fakeToggleController.isChecked()).isFalse();

        final Uri expectedUri = new Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority(SettingsSliceProvider.SLICE_AUTHORITY)
                .appendPath(SettingsSlicesContract.PATH_SETTING_ACTION)
                .appendPath(key)
                .build();
        verify(resolver).notifyChange(eq(expectedUri), eq(null));
    }

    @Test
    public void toggleUpdate_asynchronously_notifyChange_should_not_be_called() {
        // Monitor the ContentResolver
        final ContentResolver resolver = spy(mContext.getContentResolver());
        doReturn(resolver).when(mContext).getContentResolver();

        final String key = "key";
        mSearchFeatureProvider.getSearchIndexableResources().getProviderValues().clear();
        insertSpecialCase(key);

        FakeToggleController fakeToggleController = new FakeToggleController(mContext, key);
        fakeToggleController.setChecked(true);
        // Set the toggle setting update asynchronously.
        fakeToggleController.setAsyncUpdate(true);
        Intent intent = new Intent(SettingsSliceProvider.ACTION_TOGGLE_CHANGED);
        intent.putExtra(SettingsSliceProvider.EXTRA_SLICE_KEY, key);

        assertThat(fakeToggleController.isChecked()).isTrue();

        // Toggle setting
        mReceiver.onReceive(mContext, intent);

        verify(resolver, never()).notifyChange(null, null);
    }

    @Test
    public void onReceive_sliderChanged() {
        final String key = "key";
        final Uri uri = new Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority(SettingsSliceProvider.SLICE_AUTHORITY)
                .appendPath(SettingsSlicesContract.PATH_SETTING_ACTION)
                .appendPath(key)
                .build();
        final ContentResolver resolver = mock(ContentResolver.class);
        doReturn(resolver).when(mContext).getContentResolver();
        final int position = FakeSliderController.MAX_STEPS - 1;
        final int oldPosition = FakeSliderController.MAX_STEPS;
        mSearchFeatureProvider.getSearchIndexableResources().getProviderValues().clear();
        insertSpecialCase(FakeSliderController.class, key);

        // Set slider setting
        FakeSliderController fakeSliderController = new FakeSliderController(mContext, key);
        fakeSliderController.setSliderPosition(oldPosition);
        // Build action
        Intent intent = new Intent(SettingsSliceProvider.ACTION_SLIDER_CHANGED);
        intent.putExtra(Slice.EXTRA_RANGE_VALUE, position);
        intent.putExtra(SettingsSliceProvider.EXTRA_SLICE_KEY, key);

        assertThat(fakeSliderController.getSliderPosition()).isEqualTo(oldPosition);

        // Update the setting.
        mReceiver.onReceive(mContext, intent);

        assertThat(fakeSliderController.getSliderPosition()).isEqualTo(position);
        verify(mFakeFeatureFactory.metricsFeatureProvider)
                .action(eq(mContext),
                        eq(MetricsEvent.ACTION_SETTINGS_SLICE_CHANGED),
                        mLoggingNameArgumentCatpor.capture(),
                        mLoggingValueArgumentCatpor.capture());

        final Pair<Integer, Object> namePair = mLoggingNameArgumentCatpor.getValue();
        final Pair<Integer, Object> valuePair = mLoggingValueArgumentCatpor.getValue();
        assertThat(namePair.first).isEqualTo(MetricsEvent.FIELD_SETTINGS_PREFERENCE_CHANGE_NAME);
        assertThat(namePair.second).isEqualTo(key);

        verify(resolver).notifyChange(uri, null);
        assertThat(valuePair.first)
                .isEqualTo(MetricsEvent.FIELD_SETTINGS_PREFERENCE_CHANGE_INT_VALUE);
        assertThat(valuePair.second).isEqualTo(position);
    }

    @Test(expected = IllegalArgumentException.class)
    public void onReceive_invalidController_throwsException() {
        final String key = "key";
        final int position = 0;
        mSearchFeatureProvider.getSearchIndexableResources().getProviderValues().clear();
        insertSpecialCase(FakeToggleController.class, key);

        // Build action
        Intent intent = new Intent(SettingsSliceProvider.ACTION_SLIDER_CHANGED);
        intent.putExtra(Slice.EXTRA_RANGE_VALUE, position);
        intent.putExtra(SettingsSliceProvider.EXTRA_SLICE_KEY, key);

        // Trigger the exception.
        mReceiver.onReceive(mContext, intent);
    }

    @Test(expected = IllegalArgumentException.class)
    public void sliderOnReceive_noKey_throwsException() {
        // Build action
        final Intent intent = new Intent(SettingsSliceProvider.ACTION_SLIDER_CHANGED)
                .putExtra(Slice.EXTRA_RANGE_VALUE, 0);

        // Trigger the exception.
        mReceiver.onReceive(mContext, intent);
    }

    @Test(expected = IllegalStateException.class)
    public void toggleOnReceive_noExtra_illegalStateException() {
        final Intent intent = new Intent(SettingsSliceProvider.ACTION_TOGGLE_CHANGED);
        mReceiver.onReceive(mContext, intent);
    }

    @Test(expected = IllegalStateException.class)
    public void toggleOnReceive_emptyKey_throwsIllegalStateException() {
        final Intent intent = new Intent(SettingsSliceProvider.ACTION_TOGGLE_CHANGED)
                .putExtra(SettingsSliceProvider.EXTRA_SLICE_KEY, (String) null);
        mReceiver.onReceive(mContext, intent);
    }

    @Test
    public void toggleUpdate_unavailableUriNotified() {
        // Monitor the ContentResolver
        final ContentResolver resolver = spy(mContext.getContentResolver());
        doReturn(resolver).when(mContext).getContentResolver();

        // Disable Setting
        Settings.Global.putInt(mContext.getContentResolver(),
                FakeToggleController.AVAILABILITY_KEY,
                BasePreferenceController.UNSUPPORTED_ON_DEVICE);

        // Insert Fake Toggle into Database
        final String key = "key";
        mSearchFeatureProvider.getSearchIndexableResources().getProviderValues().clear();
        insertSpecialCase(FakeToggleController.class, key);

        // Turn on toggle setting
        final FakeToggleController fakeToggleController = new FakeToggleController(mContext, key);
        fakeToggleController.setChecked(true);

        // Build Action
        final Intent intent = new Intent(SettingsSliceProvider.ACTION_TOGGLE_CHANGED);
        intent.putExtra(SettingsSliceProvider.EXTRA_SLICE_KEY, key);

        // Trigger Slice change
        mReceiver.onReceive(mContext, intent);

        // Check the value is the same and the Uri has been notified.
        assertThat(fakeToggleController.isChecked()).isTrue();
        final Uri expectedUri = new Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority(SettingsSliceProvider.SLICE_AUTHORITY)
                .appendPath(SettingsSlicesContract.PATH_SETTING_ACTION)
                .appendPath(key)
                .build();
        verify(resolver).notifyChange(eq(expectedUri), eq(null));
    }

    @Test
    public void sliderUpdate_unavailableUriNotified() {
        // Monitor the ContentResolver
        final ContentResolver resolver = spy(mContext.getContentResolver());
        doReturn(resolver).when(mContext).getContentResolver();

        // Disable Setting
        Settings.Global.putInt(mContext.getContentResolver(),
                FakeSliderController.AVAILABILITY_KEY,
                BasePreferenceController.UNSUPPORTED_ON_DEVICE);

        // Insert Fake Slider into Database
        final String key = "key";
        final int position = FakeSliderController.MAX_STEPS - 1;
        final int oldPosition = FakeSliderController.MAX_STEPS;
        mSearchFeatureProvider.getSearchIndexableResources().getProviderValues().clear();
        insertSpecialCase(FakeSliderController.class, key);

        // Set slider setting
        final FakeSliderController fakeSliderController = new FakeSliderController(mContext, key);
        fakeSliderController.setSliderPosition(oldPosition);

        // Build action
        final Intent intent = new Intent(SettingsSliceProvider.ACTION_SLIDER_CHANGED);
        intent.putExtra(Slice.EXTRA_RANGE_VALUE, position);
        intent.putExtra(SettingsSliceProvider.EXTRA_SLICE_KEY, key);

        // Trigger Slice change
        mReceiver.onReceive(mContext, intent);

        // Check position is the same and the Uri has been notified.
        assertThat(fakeSliderController.getSliderPosition()).isEqualTo(oldPosition);
        final Uri expectedUri = new Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority(SettingsSliceProvider.SLICE_AUTHORITY)
                .appendPath(SettingsSlicesContract.PATH_SETTING_ACTION)
                .appendPath(key)
                .build();
        verify(resolver).notifyChange(eq(expectedUri), eq(null));
    }

    private void insertSpecialCase(String key) {
        insertSpecialCase(fakeControllerName, key);
    }

    private void insertSpecialCase(Class controllerClass, String key) {
        ContentValues values = new ContentValues();
        values.put(SlicesDatabaseHelper.IndexColumns.KEY, key);
        values.put(SlicesDatabaseHelper.IndexColumns.TITLE, fakeTitle);
        values.put(SlicesDatabaseHelper.IndexColumns.SUMMARY, fakeSummary);
        values.put(SlicesDatabaseHelper.IndexColumns.SCREENTITLE, fakeScreenTitle);
        values.put(SlicesDatabaseHelper.IndexColumns.ICON_RESOURCE, fakeIcon);
        values.put(SlicesDatabaseHelper.IndexColumns.FRAGMENT, fakeFragmentClassName);
        values.put(SlicesDatabaseHelper.IndexColumns.CONTROLLER, controllerClass.getName());
        mDb.replaceOrThrow(SlicesDatabaseHelper.Tables.TABLE_SLICES_INDEX, null, values);
    }
}