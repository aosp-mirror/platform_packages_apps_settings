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

package com.android.settings.privatespace;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.privatespace.PrivateSpaceMaintainer.ErrorDeletingPrivateSpace.DELETE_PS_ERROR_INTERNAL;
import static com.android.settings.privatespace.PrivateSpaceMaintainer.ErrorDeletingPrivateSpace.DELETE_PS_ERROR_NONE;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import android.content.Context;

import androidx.preference.Preference;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class DeletePrivateSpaceControllerTest {
    @Mock private PrivateSpaceMaintainer mPrivateSpaceMaintainer;
    @Mock private Context mContext;

    private Preference mPreference;
    private DeletePrivateSpaceController mDeletePrivateSpaceController;

    /** Required setup before a test. */
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = ApplicationProvider.getApplicationContext();
        final String preferenceKey = "private_space_delete";

        mPreference = new Preference(ApplicationProvider.getApplicationContext());
        mPreference.setKey(preferenceKey);

        mDeletePrivateSpaceController =
                new DeletePrivateSpaceController(
                        mContext,
                        preferenceKey,
                        new DeletePrivateSpaceController.Injector() {
                            @Override
                            PrivateSpaceMaintainer injectPrivateSpaceMaintainer(Context context) {
                                return mPrivateSpaceMaintainer;
                            }
                        });
    }

    /** Tests that the controller is always available. */
    @Test
    public void getAvailabilityStatus_returnsAvailable() {
        assertThat(mDeletePrivateSpaceController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    /** Tests that on click it attempts to delete the PS. */
    @Test
    public void handlePreferenceTreeClick_attemptsToDeletePrivateSpace() {
        doReturn(DELETE_PS_ERROR_NONE).when(mPrivateSpaceMaintainer).deletePrivateSpace();
        DeletePrivateSpaceController spy = Mockito.spy(mDeletePrivateSpaceController);
        doNothing().when(spy).showSuccessfulDeletionToast();
        spy.handlePreferenceTreeClick(mPreference);

        verify(mPrivateSpaceMaintainer).deletePrivateSpace();
    }

    /** Tests that on deletion of PS relevant toast is shown. */
    @Test
    public void handlePreferenceTreeClick_onDeletion_showsDeletedToast() {
        doReturn(DELETE_PS_ERROR_NONE).when(mPrivateSpaceMaintainer).deletePrivateSpace();
        DeletePrivateSpaceController spy = Mockito.spy(mDeletePrivateSpaceController);
        doNothing().when(spy).showSuccessfulDeletionToast();
        spy.handlePreferenceTreeClick(mPreference);

        verify(spy).showSuccessfulDeletionToast();
    }

    /** Tests that on failing to delete the PS relevant toast is shown. */
    @Test
    public void handlePreferenceTreeClick_onDeletionError_showsDeletionFailedToast() {
        doReturn(DELETE_PS_ERROR_INTERNAL).when(mPrivateSpaceMaintainer).deletePrivateSpace();
        DeletePrivateSpaceController spy = Mockito.spy(mDeletePrivateSpaceController);
        doNothing().when(spy).showDeletionInternalErrorToast();
        spy.handlePreferenceTreeClick(mPreference);

        verify(spy).showDeletionInternalErrorToast();
    }
}
