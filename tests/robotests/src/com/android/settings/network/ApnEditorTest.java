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

package com.android.settings.network;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.fragment.app.FragmentActivity;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.SwitchPreference;

import com.android.settings.R;
import com.android.settings.network.ApnEditor.ApnData;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class ApnEditorTest {

    private static final Object[] APN_DATA = {
            0, /* ID */
            "apn_name" /* apn name */,
            "apn.com" /* apn */,
            "" /* proxy */,
            "" /* port */,
            "" /* username */,
            "" /* server */,
            "" /* password */,
            "" /* MMSC */,
            "123" /* MCC */,
            "456" /* MNC */,
            "123456" /* operator numeric */,
            "" /* MMS proxy */,
            "" /* MMS port */,
            0 /* Authentication type */,
            "default,supl,ia" /* APN type */,
            "IPv6" /* APN protocol */,
            1 /* APN enable/disable */,
            0 /* Bearer */,
            0 /* Bearer BITMASK*/,
            "IPv4" /* APN roaming protocol */,
            "None" /* MVNO type */,
            "", /* MVNO value */
    };

    private static final int CURSOR_INTEGER_INDEX = 0;
    private static final int CURSOR_STRING_INDEX = 1;

    private static final Uri mApnUri = Uri.parse("Apn://row/1");

    @Mock
    private Cursor mCursor;

    @Captor
    private ArgumentCaptor<Uri> mUriCaptor;

    private ApnEditor mApnEditorUT;
    private FragmentActivity mActivity;
    private Resources mResources;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mActivity = spy(Robolectric.setupActivity(FragmentActivity.class));
        mResources = mActivity.getResources();
        mApnEditorUT = spy(new ApnEditor());

        doReturn(mActivity).when(mApnEditorUT).getActivity();
        doReturn(mResources).when(mApnEditorUT).getResources();
        doNothing().when(mApnEditorUT).finish();
        doNothing().when(mApnEditorUT).showError();
        when(mApnEditorUT.getContext()).thenReturn(RuntimeEnvironment.application);

        setMockPreference(mActivity);
        mApnEditorUT.mApnData = new FakeApnData(APN_DATA);
        mApnEditorUT.sNotSet = "Not Set";
    }

    @Test
    public void testApnEditor_doesNotUseManagedQuery() {
        mApnEditorUT.getApnDataFromUri(Mockito.mock(Uri.class));

        verify(mActivity, never()).managedQuery(
                any(Uri.class),
                any(String[].class),
                any(String.class),
                any(String.class));

        verify(mActivity, never()).managedQuery(
                any(Uri.class),
                any(String[].class),
                any(String.class),
                any(String[].class),
                any(String.class));
    }

    @Test
    public void testSetStringValue_valueChanged_shouldSetValue() {
        // GIVEN an APN value which is different than the APN value in database
        final String apnKey = "apn";
        final String apnValue = "testing.com";
        final ContentValues cv = new ContentValues();

        // WHEN try to check and set the apn value
        final boolean isDiff = mApnEditorUT.setStringValueAndCheckIfDiff(
                cv, apnKey, apnValue, false /* assumeDiff */, ApnEditor.APN_INDEX);

        // THEN the APN value is different than the one in database, and it has been stored in the
        // given ContentValues
        assertThat(isDiff).isTrue();
        assertThat(apnValue).isEqualTo(cv.getAsString(apnKey));
    }

    @Test
    public void testSetStringValue_valueNotChanged_shouldNotSetValue() {
        // GIVEN an APN value which is same as the APN value in database
        final String apnKey = "apn";
        final String apnValue = (String) APN_DATA[ApnEditor.APN_INDEX];
        final ContentValues cv = new ContentValues();

        // WHEN try to check and set the apn value
        final boolean isDiff = mApnEditorUT.setStringValueAndCheckIfDiff(
                cv, apnKey, apnValue, false /* assumeDiff */, ApnEditor.APN_INDEX);

        // THEN the APN value is same as the one in database, and the new APN value is not stored
        // in the given ContentValues
        assertThat(isDiff).isFalse();
        assertThat(cv.get(apnKey)).isNull();
    }

    @Test
    public void testSetStringValue_nullValue_shouldNotSetValue_shouldNotSetValue() {
        // GIVEN a null APN value
        final String apnKey = "apn";
        final String apnValue = null;
        final ContentValues cv = new ContentValues();

        // WHEN try to check and set the apn value
        final boolean isDiff = mApnEditorUT.setStringValueAndCheckIfDiff(
                cv, apnKey, apnValue, false /* assumeDiff */, ApnEditor.APN_INDEX);

        // THEN the APN value is different than the one in database, but the null value is not
        // stored in the given ContentValues
        assertThat(isDiff).isTrue();
        assertThat(cv.get(apnKey)).isNull();
    }

    @Test
    public void testSetIntValue_valueChanged_shouldSetValue() {
        // GIVEN a value indicated whether the apn is enabled, and it's different than the value in
        // the database
        final String apnEnabledKey = "apn_enabled";
        final int apnEnabledValue = 0;
        final ContentValues cv = new ContentValues();

        // WHEN try to check and set the apn enabled
        final boolean isDiff = mApnEditorUT.setIntValueAndCheckIfDiff(
                cv,
                apnEnabledKey,
                apnEnabledValue,
                false /* assumeDiff */,
                ApnEditor.CARRIER_ENABLED_INDEX);

        // THEN the apn enabled field is different than the one in database, and it has been stored
        // in the given ContentValues
        assertThat(isDiff).isTrue();
        assertThat(cv.getAsInteger(apnEnabledKey)).isEqualTo(apnEnabledValue);
    }

    @Test
    public void testSetIntValue_valueNotChanged_shouldNotSetValue() {
        // GIVEN a value indicated whether the apn is enabled, and it's same as the one in the
        // database
        final String apnEnabledKey = "apn_enabled";
        final int apnEnabledValue = (int) APN_DATA[ApnEditor.CARRIER_ENABLED_INDEX];
        final ContentValues cv = new ContentValues();

        // WHEN try to check and set the apn enabled
        final boolean isDiff = mApnEditorUT.setIntValueAndCheckIfDiff(
                cv,
                apnEnabledKey,
                apnEnabledValue,
                false /* assumeDiff */,
                ApnEditor.CARRIER_ENABLED_INDEX);

        // THEN the apn enabled field is same as the one in the database, and the filed is not
        // stored in the given ContentValues
        assertThat(isDiff).isFalse();
        assertThat(cv.get(apnEnabledKey)).isNull();
    }

    @Test
    public void testValidateApnData_validData_shouldReturnNull() {
        // GIVEN a valid apn data
        mApnEditorUT.mApnData = new FakeApnData(APN_DATA);
        mApnEditorUT.fillUI(true /* firstTime */);

        // WHEN validate the apn data
        final String errMsg = mApnEditorUT.validateApnData();

        // THEN the error message should be null
        assertThat(errMsg).isNull();
    }

    @Test
    public void testValidateApn_apnNameNotSet_shouldReturnErrorMessage() {
        // GIVEN a apn data without the apn name
        final FakeApnData apnData = new FakeApnData(APN_DATA);
        apnData.mData[ApnEditor.NAME_INDEX] = "";
        mApnEditorUT.mApnData = apnData;
        mApnEditorUT.fillUI(true /* firstTime */);

        // THEN validate the apn data
        final String errMsg = mApnEditorUT.validateApnData();

        // THEN the error message indicated the apn name not set is returned
        assertThat(errMsg).isEqualTo(mResources.getString(R.string.error_name_empty));
    }

    @Test
    public void testValidateApnData_apnNotSet_shouldReturnErrorMessage() {
        // GIVEN a apn data without the apn
        final FakeApnData apnData = new FakeApnData(APN_DATA);
        apnData.mData[ApnEditor.APN_INDEX] = "";
        mApnEditorUT.mApnData = apnData;
        mApnEditorUT.fillUI(true /* firstTime */);

        // THEN validate the apn data
        final String errMsg = mApnEditorUT.validateApnData();

        // THEN the error message indicated the apn not set is returned
        assertThat(errMsg).isEqualTo(mResources.getString(R.string.error_apn_empty));
    }

    @Test
    public void testValidateApnData_mccInvalid_shouldReturnErrorMessage() {
        // GIVEN a apn data with invalid mcc
        final FakeApnData apnData = new FakeApnData(APN_DATA);
        // The length of the mcc should be 3
        apnData.mData[ApnEditor.MCC_INDEX] = "1324";
        mApnEditorUT.mApnData = apnData;
        mApnEditorUT.fillUI(true /* firstTime */);

        // WHEN validate the apn data
        final String errMsg = mApnEditorUT.validateApnData();

        // THEN the error message indicated the mcc invalid is returned
        assertThat(errMsg).isEqualTo(mResources.getString(R.string.error_mcc_not3));
    }

    @Test
    public void testValidateApnData_mncInvalid_shouldReturnErrorMessage() {
        // GIVEN an apn data with invalid mnc
        final FakeApnData apnData = new FakeApnData(APN_DATA);
        // The length of the mnc should be 2 or 3
        apnData.mData[ApnEditor.MNC_INDEX] = "1324";
        mApnEditorUT.mApnData = apnData;
        mApnEditorUT.fillUI(true /* firstTime */);

        // WHEN validate the apn data
        final String errMsg = mApnEditorUT.validateApnData();

        // THEN the error message indicated the mnc invalid is returned
        assertThat(errMsg).isEqualTo(mResources.getString(R.string.error_mnc_not23));
    }

    @Test
    public void testSaveApnData_pressBackButtonWithValidApnData_shouldSaveApnData() {
        // GIVEN a valid apn data
        mApnEditorUT.mApnData = new FakeApnData(APN_DATA);
        mApnEditorUT.fillUI(true /* firstTime */);

        // WHEN press the back button
        final KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK);
        mApnEditorUT.onKey(new View(mActivity), KeyEvent.KEYCODE_BACK, event);

        // THEN the apn data is saved and the apn editor is closed
        verify(mApnEditorUT).validateAndSaveApnData();
        verify(mApnEditorUT).finish();
    }

    @Test
    public void testSaveApnData_pressSaveButtonWithValidApnData_shouldSaveApnData() {
        // GIVEN a valid apn data
        mApnEditorUT.mApnData = new FakeApnData(APN_DATA);
        mApnEditorUT.fillUI(true /* firstTime */);

        // WHEN press the save button
        MenuItem item = Mockito.mock(MenuItem.class);
        // Menu.FIRST + 1 indicated the SAVE option in ApnEditor
        doReturn(Menu.FIRST + 1).when(item).getItemId();
        mApnEditorUT.onOptionsItemSelected(item);

        // THEN the apn data is saved and the apn editor is closed
        verify(mApnEditorUT).validateAndSaveApnData();
        verify(mApnEditorUT).finish();
    }

    @Test
    public void testSaveApnData_apnDataInvalid_shouldNotSaveApnData() {
        // GIVEN an invalid apn data
        final FakeApnData apnData = new FakeApnData(APN_DATA);
        // The valid apn data should contains a non-empty apn name
        apnData.mData[ApnEditor.NAME_INDEX] = "";
        mApnEditorUT.mApnData = apnData;
        mApnEditorUT.fillUI(true /* firstTime */);

        // WHEN press the save button
        final MenuItem item = Mockito.mock(MenuItem.class);
        // Menu.FIRST + 1 indicated the SAVE option in ApnEditor
        doReturn(Menu.FIRST + 1).when(item).getItemId();
        mApnEditorUT.onOptionsItemSelected(item);

        // THEN the error dialog is shown
        verify(mApnEditorUT).validateAndSaveApnData();
        verify(mApnEditorUT).showError();
    }

    @Test
    public void testDeleteApnData_shouldDeleteData() {
        // GIVEN a valid apn data correspond a row in database
        final Uri apnUri = Uri.parse("content://telephony/carriers/1");
        mApnEditorUT.mApnData = new FakeApnData(APN_DATA, apnUri);
        mApnEditorUT.fillUI(true /* firstTime */);
        ContentResolver mockContentResolver = Mockito.mock(ContentResolver.class);
        doReturn(mockContentResolver).when(mActivity).getContentResolver();

        // WHEN press the save button
        final MenuItem item = Mockito.mock(MenuItem.class);
        // Menu.FIRST indicated the DELETE option in ApnEditor
        doReturn(Menu.FIRST).when(item).getItemId();
        mApnEditorUT.onOptionsItemSelected(item);

        // THEN the apn data is deleted and the apn editor is closed
        verify(mockContentResolver).delete(mUriCaptor.capture(), any(), any());
        assertThat(apnUri).isEqualTo(mUriCaptor.getValue());
        verify(mApnEditorUT).finish();
    }

    @Test(expected = ClassCastException.class)
    public void testApnData_invalidIntegerType_throwsInvalidTypeException() {
        // GIVEN a ApnData constructed from cursor
        initCursor();
        final ApnData data = new ApnData(mApnUri, mCursor);

        // WHEN get a string from an integer column
        // THEN the InvalidTypeException is threw
        data.getString(CURSOR_INTEGER_INDEX);
    }

    @Test(expected = ClassCastException.class)
    public void testApnData_invalidStringType_throwsInvalidTypeException() {
        // GIVEN a ApnData constructed from cursor
        initCursor();
        final ApnData data = new ApnData(mApnUri, mCursor);

        // WHEN get a integer from a string column
        // THEN the InvalidTypeException is threw
        data.getInteger(CURSOR_STRING_INDEX);
    }

    @Test
    public void testApnData_validIntegerType_returnCorrectValue() {
        // GIVEN a ApnData constructed from cursor
        initCursor();
        final ApnData data = new ApnData(mApnUri, mCursor);

        // WHEN get integer from an integer column
        final int val = data.getInteger(CURSOR_INTEGER_INDEX);

        // THEN the integer is returned correctly
        assertThat(val).isEqualTo(mCursor.getInt(CURSOR_INTEGER_INDEX));
    }

    @Test
    public void testApnData_validStringType_returnCorrectValue() {
        // GIVEN a ApnData constructed from cursor
        initCursor();
        final ApnData data = new ApnData(mApnUri, mCursor);

        // WHEN get string from a string column
        final String str = data.getString(CURSOR_STRING_INDEX);

        // THEN the integer is returned correctly
        assertThat(str).isEqualTo(mCursor.getString(CURSOR_STRING_INDEX));
    }

    @Test
    public void testApnData_nullValueColumn_returnNull() {
        // GIVEN a empty ApnData
        final ApnData data = new ApnData(3);

        // WHEN get string value from a null column
        final String str = data.getString(0);

        // THEN the null value is returned
        assertThat(str).isNull();
    }

    @Test
    public void formatInteger_shouldParseString() {
        assertThat(ApnEditor.formatInteger("42")).isEqualTo("42");
        assertThat(ApnEditor.formatInteger("01")).isEqualTo("01");
        assertThat(ApnEditor.formatInteger("001")).isEqualTo("001");
    }

    @Test
    public void formatInteger_shouldIgnoreNonIntegers() {
        assertThat(ApnEditor.formatInteger("not an int")).isEqualTo("not an int");
    }

    @Test
    public void onCreate_noAction_shouldFinishAndNoCrash() {
        doNothing().when(mApnEditorUT).addPreferencesFromResource(anyInt());

        mApnEditorUT.onCreate(null);

        verify(mApnEditorUT).finish();
    }

    @Test
    public void getUserEnteredApnType_emptyApnType_shouldReturnDefault() {
        // case 1
        // GIVEN read only APN types with DUN
        String[] readOnlyApnTypes = {"dun"};
        mApnEditorUT.mReadOnlyApnTypes = readOnlyApnTypes;
        // GIVEN read specificApnTypeForEmptyInput with DEFAULT,DUN
        String[] defaultApnTypes = {"default", "dun"};
        mApnEditorUT.mDefaultApnTypes = defaultApnTypes;

        // Input empty in TYPE
        final FakeApnData apnData = new FakeApnData(APN_DATA);
        apnData.mData[ApnEditor.TYPE_INDEX] = "";
        mApnEditorUT.mApnData = apnData;
        mApnEditorUT.fillUI(true /* firstTime */);

        // THEN APN type should be default
        assertThat(mApnEditorUT.getUserEnteredApnType()).isEqualTo("default");

        // case 2
        // GIVEN read only APN types with DUN
        String[] readOnlyApnTypesCase2 = {"dun"};
        mApnEditorUT.mReadOnlyApnTypes = readOnlyApnTypesCase2;
        // GIVEN read specificApnTypeForEmptyInput with DEFAULT
        String[] defaultApnTypesCase2 = {"default"};
        mApnEditorUT.mDefaultApnTypes = defaultApnTypesCase2;

        // Input empty in TYPE
        final FakeApnData apnDataCase2 = new FakeApnData(APN_DATA);
        apnDataCase2.mData[ApnEditor.TYPE_INDEX] = "";
        mApnEditorUT.mApnData = apnDataCase2;
        mApnEditorUT.fillUI(true /* firstTime */);

        // THEN APN type should be default
        assertThat(mApnEditorUT.getUserEnteredApnType()).isEqualTo("default");
    }

    private void initCursor() {
        doReturn(2).when(mCursor).getColumnCount();
        doReturn(2).when(mCursor).getInt(CURSOR_INTEGER_INDEX);
        doReturn("str").when(mCursor).getString(CURSOR_STRING_INDEX);
        doReturn(Cursor.FIELD_TYPE_INTEGER).when(mCursor).getType(CURSOR_INTEGER_INDEX);
        doReturn(Cursor.FIELD_TYPE_STRING).when(mCursor).getType(CURSOR_STRING_INDEX);
    }

    private void setMockPreference(Context context) {
        mApnEditorUT.mName = new EditTextPreference(context);
        mApnEditorUT.mApn = new EditTextPreference(context);
        mApnEditorUT.mProxy = new EditTextPreference(context);
        mApnEditorUT.mPort = new EditTextPreference(context);
        mApnEditorUT.mUser = new EditTextPreference(context);
        mApnEditorUT.mServer = new EditTextPreference(context);
        mApnEditorUT.mPassword = new EditTextPreference(context);
        mApnEditorUT.mMmsc = new EditTextPreference(context);
        mApnEditorUT.mMcc = new EditTextPreference(context);
        mApnEditorUT.mMnc = new EditTextPreference(context);
        mApnEditorUT.mMmsProxy = new EditTextPreference(context);
        mApnEditorUT.mMmsPort = new EditTextPreference(context);
        mApnEditorUT.mAuthType = new ListPreference(context);
        mApnEditorUT.mApnType = new EditTextPreference(context);
        mApnEditorUT.mProtocol = new ListPreference(context);
        mApnEditorUT.mRoamingProtocol = new ListPreference(context);
        mApnEditorUT.mCarrierEnabled = new SwitchPreference(context);
        mApnEditorUT.mBearerMulti = new MultiSelectListPreference(context);
        mApnEditorUT.mMvnoType = new ListPreference(context);
        mApnEditorUT.mMvnoMatchData = new EditTextPreference(context);
    }

    private final class FakeApnData extends ApnData {
        FakeApnData(Object[] data) {
            super(data.length);
            System.arraycopy(data, 0, mData, 0, data.length);
        }

        FakeApnData(Object[] data, Uri uri) {
            this(data);
            mUri = uri;
        }
    }
}
