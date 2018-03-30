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

package com.android.settings;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;

import static org.mockito.Mockito.doReturn;

import android.database.Cursor;
import android.net.Uri;

import com.android.settings.ApnEditor.ApnData;
import com.android.settings.ApnEditor.InvalidTypeException;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(SettingsRobolectricTestRunner.class)
public class ApnEditorTest {

    private static final int CURSOR_INTEGER_INDEX = 0;
    private static final int CURSOR_STRING_INDEX = 1;

    private static final Uri mApnUri = Uri.parse("Apn://row/1");

    @Mock
    private Cursor mCursor;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        initCursor();
    }

    private void initCursor() {
        doReturn(2).when(mCursor).getColumnCount();
        doReturn(Integer.valueOf(2)).when(mCursor).getInt(CURSOR_INTEGER_INDEX);
        doReturn("str").when(mCursor).getString(CURSOR_STRING_INDEX);
        doReturn(Cursor.FIELD_TYPE_INTEGER).when(mCursor).getType(CURSOR_INTEGER_INDEX);
        doReturn(Cursor.FIELD_TYPE_STRING).when(mCursor).getType(CURSOR_STRING_INDEX);
    }

    @Test(expected = InvalidTypeException.class)
    public void testApnData_invalidIntegerType_throwsInvalidTypeException() {
        // GIVEN a ApnData constructed from cursor
        ApnData data = new ApnData(mApnUri, mCursor);

        // WHEN get a string from an integer column
        // THEN the InvalidTypeException is threw
        data.getString(CURSOR_INTEGER_INDEX);
    }

    @Test(expected = InvalidTypeException.class)
    public void testApnData_invalidStringType_throwsInvalidTypeException() {
        // GIVEN a ApnData constructed from cursor
        ApnData data = new ApnData(mApnUri, mCursor);

        // WHEN get a integer from a string column
        // THEN the InvalidTypeException is threw
        data.getInteger(CURSOR_STRING_INDEX);
    }

    @Test
    public void testApnData_validIntegerType_returnCorrectValue() {
        // GIVEN a ApnData constructed from cursor
        ApnData data = new ApnData(mApnUri, mCursor);

        // WHEN get integer from an integer column
        int val = data.getInteger(CURSOR_INTEGER_INDEX);

        // THEN the integer is returned correctly
        assertEquals(mCursor.getInt(CURSOR_INTEGER_INDEX), val);
    }

    @Test
    public void testApnData_validStringType_returnCorrectValue() {
        // GIVEN a ApnData constructed from cursor
        ApnData data = new ApnData(mApnUri, mCursor);

        // WHEN get string from a string column
        String str = data.getString(CURSOR_STRING_INDEX);

        // THEN the integer is returned correctly
        assertEquals(mCursor.getString(CURSOR_STRING_INDEX), str);
    }

    @Test
    public void testApnData_nullValueColumn_returnNull() {
        // GIVEN a empty ApnData
        ApnData data = new ApnData(3);

        // WHEN get string value from a null column
        String str = data.getString(0);

        // THEN the null value is returned
        assertNull(str);
    }
}
