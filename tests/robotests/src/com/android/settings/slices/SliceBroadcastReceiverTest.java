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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.app.settings.SettingsEnums;
import android.app.slice.Slice;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.Settings;
import android.provider.SettingsSlicesContract;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.search.SearchFeatureProvider;
import com.android.settings.search.SearchFeatureProviderImpl;
import com.android.settings.testutils.DatabaseTestUtils;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.FakeSliderController;
import com.android.settings.testutils.FakeToggleController;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class SliceBroadcastReceiverTest {

    private Context mContext;
    private SQLiteDatabase mDb;
    private SliceBroadcastReceiver mReceiver;
    private SearchFeatureProvider mSearchFeatureProvider;
    private FakeFeatureFactory mFakeFeatureFactory;

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
    }

    @After
    public void cleanUp() {
        DatabaseTestUtils.clearDb(mContext);
    }

    @Test
    public void onReceive_toggleChanged() {
        final String key = "key";
        final Uri uri = buildUri(key);
        mSearchFeatureProvider.getSearchIndexableResources().getProviderValues().clear();
        insertSpecialCase(key);
        final ContentResolver resolver = mock(ContentResolver.class);
        doReturn(resolver).when(mContext).getContentResolver();
        // Turn on toggle setting
        final FakeToggleController fakeToggleController = new FakeToggleController(mContext, key);
        fakeToggleController.setChecked(true);

        final Intent intent = new Intent(SettingsSliceProvider.ACTION_TOGGLE_CHANGED)
                .putExtra(SettingsSliceProvider.EXTRA_SLICE_KEY, key)
                .setData(uri);

        assertThat(fakeToggleController.isChecked()).isTrue();

        // Toggle setting
        mReceiver.onReceive(mContext, intent);

        assertThat(fakeToggleController.isChecked()).isFalse();
        verify(mFakeFeatureFactory.metricsFeatureProvider)
                .action(SettingsEnums.PAGE_UNKNOWN,
                        MetricsEvent.ACTION_SETTINGS_SLICE_CHANGED,
                        SettingsEnums.PAGE_UNKNOWN,
                        fakeToggleController.getPreferenceKey(),
                        0);
        verify(resolver).notifyChange(uri, null);
    }

    @Test
    public void toggleUpdate_synchronously_notifyChange_should_be_called() {
        // Monitor the ContentResolver
        final ContentResolver resolver = spy(mContext.getContentResolver());
        doReturn(resolver).when(mContext).getContentResolver();

        final String key = "key";
        final Uri expectedUri = buildUri(key);

        mSearchFeatureProvider.getSearchIndexableResources().getProviderValues().clear();
        insertSpecialCase(key);

        FakeToggleController fakeToggleController = new FakeToggleController(mContext, key);
        fakeToggleController.setChecked(true);
        // Set the toggle setting update synchronously.
        fakeToggleController.setAsyncUpdate(false);
        final Intent intent = new Intent(SettingsSliceProvider.ACTION_TOGGLE_CHANGED)
                .putExtra(SettingsSliceProvider.EXTRA_SLICE_KEY, key)
                .setData(expectedUri);

        assertThat(fakeToggleController.isChecked()).isTrue();

        // Toggle setting
        mReceiver.onReceive(mContext, intent);

        assertThat(fakeToggleController.isChecked()).isFalse();


        verify(resolver).notifyChange(expectedUri, null);
    }

    @Test
    public void toggleUpdate_asynchronously_notifyChange_should_not_be_called() {
        // Monitor the ContentResolver
        final ContentResolver resolver = spy(mContext.getContentResolver());
        doReturn(resolver).when(mContext).getContentResolver();

        final String key = "key";
        mSearchFeatureProvider.getSearchIndexableResources().getProviderValues().clear();
        insertSpecialCase(AsyncToggleController.class.getName(), key);

        final Intent intent = new Intent(SettingsSliceProvider.ACTION_TOGGLE_CHANGED)
                .putExtra(SettingsSliceProvider.EXTRA_SLICE_KEY, key)
                .setData(buildUri(key));

        // Toggle setting
        mReceiver.onReceive(mContext, intent);

        verify(resolver, never()).notifyChange(null, null);
    }

    @Test
    public void onReceive_sliderChanged() {
        final String key = "key";
        final Uri uri = buildUri(key);
        final ContentResolver resolver = mock(ContentResolver.class);
        doReturn(resolver).when(mContext).getContentResolver();
        final int position = FakeSliderController.MAX_VALUE - 1;
        final int oldPosition = FakeSliderController.MAX_VALUE;
        mSearchFeatureProvider.getSearchIndexableResources().getProviderValues().clear();
        insertSpecialCase(FakeSliderController.class.getName(), key);

        // Set slider setting
        FakeSliderController fakeSliderController = new FakeSliderController(mContext, key);
        fakeSliderController.setSliderPosition(oldPosition);
        // Build action
        final Intent intent = new Intent(SettingsSliceProvider.ACTION_SLIDER_CHANGED)
                .putExtra(Slice.EXTRA_RANGE_VALUE, position)
                .putExtra(SettingsSliceProvider.EXTRA_SLICE_KEY, key)
                .setData(uri);

        assertThat(fakeSliderController.getSliderPosition()).isEqualTo(oldPosition);

        // Update the setting.
        mReceiver.onReceive(mContext, intent);

        assertThat(fakeSliderController.getSliderPosition()).isEqualTo(position);
        verify(mFakeFeatureFactory.metricsFeatureProvider)
                .action(SettingsEnums.PAGE_UNKNOWN,
                        MetricsEvent.ACTION_SETTINGS_SLICE_CHANGED,
                        SettingsEnums.PAGE_UNKNOWN,
                        key,
                        position);

        verify(resolver).notifyChange(uri, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void onReceive_invalidController_throwsException() {
        final String key = "key";
        final int position = 0;
        mSearchFeatureProvider.getSearchIndexableResources().getProviderValues().clear();
        insertSpecialCase(key);

        // Build action
        final Intent intent = new Intent(SettingsSliceProvider.ACTION_SLIDER_CHANGED)
                .putExtra(Slice.EXTRA_RANGE_VALUE, position)
                .putExtra(SettingsSliceProvider.EXTRA_SLICE_KEY, key);

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
        final Uri expectedUri = buildUri(key);
        mSearchFeatureProvider.getSearchIndexableResources().getProviderValues().clear();
        insertSpecialCase(key);

        // Turn on toggle setting
        final FakeToggleController fakeToggleController = new FakeToggleController(mContext, key);
        fakeToggleController.setChecked(true);

        // Build Action
        final Intent intent = new Intent(SettingsSliceProvider.ACTION_TOGGLE_CHANGED)
                .putExtra(SettingsSliceProvider.EXTRA_SLICE_KEY, key)
                .setData(expectedUri);

        // Trigger Slice change
        mReceiver.onReceive(mContext, intent);

        // Check the value is the same and the Uri has been notified.
        assertThat(fakeToggleController.isChecked()).isTrue();
        verify(resolver).notifyChange(expectedUri, null);
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
        final Uri expectedUri = buildUri(key);

        final int position = FakeSliderController.MAX_VALUE - 1;
        final int oldPosition = FakeSliderController.MAX_VALUE;
        mSearchFeatureProvider.getSearchIndexableResources().getProviderValues().clear();
        insertSpecialCase(FakeSliderController.class.getName(), key);

        // Set slider setting
        final FakeSliderController fakeSliderController = new FakeSliderController(mContext, key);
        fakeSliderController.setSliderPosition(oldPosition);

        // Build action
        final Intent intent = new Intent(SettingsSliceProvider.ACTION_SLIDER_CHANGED)
                .putExtra(Slice.EXTRA_RANGE_VALUE, position)
                .putExtra(SettingsSliceProvider.EXTRA_SLICE_KEY, key)
                .setData(expectedUri);

        // Trigger Slice change
        mReceiver.onReceive(mContext, intent);

        // Check position is the same and the Uri has been notified.
        assertThat(fakeSliderController.getSliderPosition()).isEqualTo(oldPosition);
        verify(resolver).notifyChange(eq(expectedUri), eq(null));
    }

    private void insertSpecialCase(String key) {
        insertSpecialCase(SliceTestUtils.FAKE_CONTROLLER_NAME, key);
    }

    private void insertSpecialCase(String controllerClass, String key) {
        ContentValues values = new ContentValues();
        values.put(SlicesDatabaseHelper.IndexColumns.KEY, key);
        values.put(SlicesDatabaseHelper.IndexColumns.TITLE, SliceTestUtils.FAKE_TITLE);
        values.put(SlicesDatabaseHelper.IndexColumns.SUMMARY, SliceTestUtils.FAKE_SUMMARY);
        values.put(SlicesDatabaseHelper.IndexColumns.SCREENTITLE, SliceTestUtils.FAKE_SCREEN_TITLE);
        values.put(SlicesDatabaseHelper.IndexColumns.ICON_RESOURCE, SliceTestUtils.FAKE_ICON);
        values.put(SlicesDatabaseHelper.IndexColumns.FRAGMENT, SliceTestUtils.FAKE_FRAGMENT_NAME);
        values.put(SlicesDatabaseHelper.IndexColumns.CONTROLLER, controllerClass);
        values.put(SlicesDatabaseHelper.IndexColumns.SLICE_URI, buildUri(key).toSafeString());
        mDb.replaceOrThrow(SlicesDatabaseHelper.Tables.TABLE_SLICES_INDEX, null, values);
    }

    private static Uri buildUri(String key) {
        return new Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority(SettingsSliceProvider.SLICE_AUTHORITY)
                .appendPath(SettingsSlicesContract.PATH_SETTING_ACTION)
                .appendPath(key)
                .build();
    }

    public static class AsyncToggleController extends FakeToggleController {
        public AsyncToggleController(Context context, String preferenceKey) {
            super(context, preferenceKey);
        }

        @Override
        public boolean hasAsyncUpdate() {
            return true;
        }
    }
}