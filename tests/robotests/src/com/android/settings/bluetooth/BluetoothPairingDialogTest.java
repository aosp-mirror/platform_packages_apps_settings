/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.android.settings.bluetooth;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Dialog;
import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;

import com.android.settings.R;
import com.android.settings.testutils.shadow.ShadowAlertDialogCompat;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.androidx.fragment.FragmentController;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowAlertDialogCompat.class)
public class BluetoothPairingDialogTest {

    private static final String FILLER = "text that goes in a view";
    private static final String FAKE_DEVICE_NAME = "Fake Bluetooth Device";

    @Mock
    private BluetoothPairingController controller;
    @Mock
    private BluetoothPairingDialog dialogActivity;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        doNothing().when(dialogActivity).dismiss();
    }

    @Test
    public void dialogUpdatesControllerWithUserInput() {
        // set the correct dialog type
        when(controller.getDialogType()).thenReturn(BluetoothPairingController.USER_ENTRY_DIALOG);

        // we don't care about these for this test
        when(controller.getDeviceVariantMessageId())
                .thenReturn(BluetoothPairingController.INVALID_DIALOG_TYPE);
        when(controller.getDeviceVariantMessageHintId())
                .thenReturn(BluetoothPairingController.INVALID_DIALOG_TYPE);

        // build fragment
        BluetoothPairingDialogFragment frag = makeFragment();

        // test that controller is updated on text change
        frag.afterTextChanged(new SpannableStringBuilder(FILLER));
        verify(controller, times(1)).updateUserInput(any());
    }

    @Test
    public void dialogEnablesSubmitButtonOnValidationFromController() {
        // set the correct dialog type
        when(controller.getDialogType()).thenReturn(BluetoothPairingController.USER_ENTRY_DIALOG);

        // we don't care about these for this test
        when(controller.getDeviceVariantMessageId())
                .thenReturn(BluetoothPairingController.INVALID_DIALOG_TYPE);
        when(controller.getDeviceVariantMessageHintId())
                .thenReturn(BluetoothPairingController.INVALID_DIALOG_TYPE);

        // force the controller to say that any passkey is valid
        when(controller.isPasskeyValid(any())).thenReturn(true);

        // build fragment
        BluetoothPairingDialogFragment frag = makeFragment();

        // test that the positive button is enabled when passkey is valid
        frag.afterTextChanged(new SpannableStringBuilder(FILLER));
        View button = frag.getmDialog().getButton(AlertDialog.BUTTON_POSITIVE);
        assertThat(button).isNotNull();
        assertThat(button.getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Test
    public void dialogDoesNotAskForPairCodeOnConsentVariant() {
        // set the dialog variant to confirmation/consent
        when(controller.getDialogType()).thenReturn(BluetoothPairingController.CONFIRMATION_DIALOG);

        // build the fragment
        BluetoothPairingDialogFragment frag = makeFragment();

        // check that the input field used by the entry dialog fragment does not exist
        View view = frag.getmDialog().findViewById(R.id.text);
        assertThat(view).isNull();
    }

    @Ignore
    @Test
    public void dialogAsksForPairCodeOnUserEntryVariant() {
        // set the dialog variant to user entry
        when(controller.getDialogType()).thenReturn(BluetoothPairingController.USER_ENTRY_DIALOG);

        // we don't care about these for this test
        when(controller.getDeviceVariantMessageId())
                .thenReturn(BluetoothPairingController.INVALID_DIALOG_TYPE);
        when(controller.getDeviceVariantMessageHintId())
                .thenReturn(BluetoothPairingController.INVALID_DIALOG_TYPE);

        Context context = spy(RuntimeEnvironment.application);
        InputMethodManager imm = mock(InputMethodManager.class);
        doReturn(imm).when(context).getSystemService(Context.INPUT_METHOD_SERVICE);

        // build the fragment
        BluetoothPairingDialogFragment frag = spy(new BluetoothPairingDialogFragment());
        when(frag.getContext()).thenReturn(context);
        setupFragment(frag);
        AlertDialog alertDialog = frag.getmDialog();

        // check that the pin/passkey input field is visible to the user
        View view = alertDialog.findViewById(R.id.text);
        assertThat(view.getVisibility()).isEqualTo(View.VISIBLE);

        // check that showSoftInput was called to make input method appear when the dialog was shown
        assertThat(view.isFocused()).isTrue();
        // TODO(b/73892004): Figure out why this is failing.
        // assertThat(imm.isActive()).isTrue();
        verify(imm).showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
    }

    @Test
    public void dialogDisplaysPairCodeOnDisplayPasskeyVariant() {
        // set the dialog variant to display passkey
        when(controller.getDialogType())
                .thenReturn(BluetoothPairingController.DISPLAY_PASSKEY_DIALOG);

        // ensure that the controller returns good values to indicate a passkey needs to be shown
        when(controller.isDisplayPairingKeyVariant()).thenReturn(true);
        when(controller.hasPairingContent()).thenReturn(true);
        when(controller.getPairingContent()).thenReturn(FILLER);

        // build the fragment
        BluetoothPairingDialogFragment frag = makeFragment();

        // get the relevant views
        View messagePairing = frag.getmDialog().findViewById(R.id.pairing_code_message);
        TextView pairingViewContent = frag.getmDialog().findViewById(R.id.pairing_subhead);
        View pairingViewCaption = frag.getmDialog().findViewById(R.id.pairing_caption);

        // check that the relevant views are visible and that the passkey is shown
        assertThat(messagePairing.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(pairingViewCaption.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(pairingViewContent.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(TextUtils.equals(FILLER, pairingViewContent.getText())).isTrue();
    }

    @Test(expected = IllegalStateException.class)
    public void dialogThrowsExceptionIfNoControllerSet() {
        // instantiate a fragment
        BluetoothPairingDialogFragment frag = new BluetoothPairingDialogFragment();

        // this should throw an error
        FragmentController.setupFragment(frag, FragmentActivity.class, 0 /* containerViewId */,
                null /* bundle */);
        fail("Starting the fragment with no controller set should have thrown an exception.");
    }

    @Test
    public void dialogCallsHookOnPositiveButtonPress() {
        // set the dialog variant to confirmation/consent
        when(controller.getDialogType()).thenReturn(BluetoothPairingController.CONFIRMATION_DIALOG);

        // we don't care what this does, just that it is called
        doNothing().when(controller).onDialogPositiveClick(any());

        // build the fragment
        BluetoothPairingDialogFragment frag = makeFragment();

        // click the button and verify that the controller hook was called
        frag.onClick(frag.getmDialog(), AlertDialog.BUTTON_POSITIVE);
        verify(controller, times(1)).onDialogPositiveClick(any());
    }

    @Test
    public void dialogCallsHookOnNegativeButtonPress() {
        // set the dialog variant to confirmation/consent
        when(controller.getDialogType()).thenReturn(BluetoothPairingController.CONFIRMATION_DIALOG);

        // we don't care what this does, just that it is called
        doNothing().when(controller).onDialogNegativeClick(any());

        // build the fragment
        BluetoothPairingDialogFragment frag = makeFragment();

        // click the button and verify that the controller hook was called
        frag.onClick(frag.getmDialog(), AlertDialog.BUTTON_NEGATIVE);
        verify(controller, times(1)).onDialogNegativeClick(any());
    }

    @Test(expected = IllegalStateException.class)
    public void dialogDoesNotAllowSwappingController() {
        // instantiate a fragment
        BluetoothPairingDialogFragment frag = new BluetoothPairingDialogFragment();
        frag.setPairingController(controller);

        // this should throw an error
        frag.setPairingController(controller);
        fail("Setting the controller multiple times should throw an exception.");
    }

    @Test(expected = IllegalStateException.class)
    public void dialogDoesNotAllowSwappingActivity() {
        // instantiate a fragment
        BluetoothPairingDialogFragment frag = new BluetoothPairingDialogFragment();
        frag.setPairingDialogActivity(dialogActivity);

        // this should throw an error
        frag.setPairingDialogActivity(dialogActivity);
        fail("Setting the dialog activity multiple times should throw an exception.");
    }

    @Test
    public void dialogPositiveButtonDisabledWhenUserInputInvalid() {
        // set the correct dialog type
        when(controller.getDialogType()).thenReturn(BluetoothPairingController.USER_ENTRY_DIALOG);

        // we don't care about these for this test
        when(controller.getDeviceVariantMessageId())
                .thenReturn(BluetoothPairingController.INVALID_DIALOG_TYPE);
        when(controller.getDeviceVariantMessageHintId())
                .thenReturn(BluetoothPairingController.INVALID_DIALOG_TYPE);

        // force the controller to say that any passkey is valid
        when(controller.isPasskeyValid(any())).thenReturn(false);

        // build fragment
        BluetoothPairingDialogFragment frag = makeFragment();

        // test that the positive button is enabled when passkey is valid
        frag.afterTextChanged(new SpannableStringBuilder(FILLER));
        View button = frag.getmDialog().getButton(AlertDialog.BUTTON_POSITIVE);
        assertThat(button).isNotNull();
        assertThat(button.isEnabled()).isFalse();
    }

    @Test
    public void contactSharingToggle_conditionIsReady_showsUi() {
        // set the dialog variant to confirmation/consent
        when(controller.getDialogType()).thenReturn(BluetoothPairingController.CONFIRMATION_DIALOG);
        // set a fake device name and pretend the profile has not been set up for it
        when(controller.getDeviceName()).thenReturn(FAKE_DEVICE_NAME);
        when(controller.isContactSharingVisible()).thenReturn(true);

        // build the fragment
        BluetoothPairingDialogFragment frag = makeFragment();

        // verify that the toggle is visible
        View sharingToggle =
                frag.getmDialog().findViewById(R.id.phonebook_sharing);
        assertThat(sharingToggle.getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Test
    public void contactSharingToggle_conditionIsNotReady_doesNotShowUi() {
        // set the dialog variant to confirmation/consent
        when(controller.getDialogType()).thenReturn(BluetoothPairingController.CONFIRMATION_DIALOG);
        // set a fake device name and pretend the profile has been set up for it
        when(controller.getDeviceName()).thenReturn(FAKE_DEVICE_NAME);
        when(controller.isContactSharingVisible()).thenReturn(false);

        // build the fragment
        BluetoothPairingDialogFragment frag = makeFragment();

        // verify that the toggle is gone
        View sharingToggle =
                frag.getmDialog().findViewById(R.id.phonebook_sharing);
        assertThat(sharingToggle.getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void dialogShowsMessageOnPinEntryView() {
        // set the correct dialog type
        when(controller.getDialogType()).thenReturn(BluetoothPairingController.USER_ENTRY_DIALOG);

        // Set the message id to something specific to verify later
        when(controller.getDeviceVariantMessageId()).thenReturn(R.string.cancel);
        when(controller.getDeviceVariantMessageHintId())
                .thenReturn(BluetoothPairingController.INVALID_DIALOG_TYPE);

        // build the fragment
        BluetoothPairingDialogFragment frag = makeFragment();

        // verify message is what we expect it to be and is visible
        TextView message = frag.getmDialog().findViewById(R.id.message_below_pin);
        assertThat(message.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(TextUtils.equals(frag.getString(R.string.cancel), message.getText())).isTrue();
    }

    @Test
    public void dialogShowsMessageHintOnPinEntryView() {
        // set the correct dialog type
        when(controller.getDialogType()).thenReturn(BluetoothPairingController.USER_ENTRY_DIALOG);

        // Set the message id hint to something specific to verify later
        when(controller.getDeviceVariantMessageHintId()).thenReturn(R.string.cancel);
        when(controller.getDeviceVariantMessageId())
                .thenReturn(BluetoothPairingController.INVALID_DIALOG_TYPE);

        // build the fragment
        BluetoothPairingDialogFragment frag = makeFragment();

        // verify message is what we expect it to be and is visible
        TextView hint = frag.getmDialog().findViewById(R.id.pin_values_hint);
        assertThat(hint.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(TextUtils.equals(frag.getString(R.string.cancel), hint.getText())).isTrue();
    }

    @Test
    public void dialogHidesMessageAndHintWhenNotProvidedOnPinEntryView() {
        // set the correct dialog type
        when(controller.getDialogType()).thenReturn(BluetoothPairingController.USER_ENTRY_DIALOG);

        // Set the id's to what is returned when it is not provided
        when(controller.getDeviceVariantMessageHintId())
                .thenReturn(BluetoothPairingController.INVALID_DIALOG_TYPE);
        when(controller.getDeviceVariantMessageId())
                .thenReturn(BluetoothPairingController.INVALID_DIALOG_TYPE);

        // build the fragment
        BluetoothPairingDialogFragment frag = makeFragment();

        // verify message is what we expect it to be and is visible
        TextView hint = frag.getmDialog().findViewById(R.id.pin_values_hint);
        assertThat(hint.getVisibility()).isEqualTo(View.GONE);
        TextView message = frag.getmDialog().findViewById(R.id.message_below_pin);
        assertThat(message.getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void pairingDialogDismissedOnPositiveClick() {
        // set the dialog variant to confirmation/consent
        when(controller.getDialogType()).thenReturn(BluetoothPairingController.CONFIRMATION_DIALOG);

        // we don't care what this does, just that it is called
        doNothing().when(controller).onDialogPositiveClick(any());

        // build the fragment
        BluetoothPairingDialogFragment frag = makeFragment();

        // click the button and verify that the controller hook was called
        frag.onClick(frag.getmDialog(), AlertDialog.BUTTON_POSITIVE);

        verify(controller, times(1)).onDialogPositiveClick(any());
        verify(dialogActivity, times(1)).dismiss();
    }

    @Test
    public void pairingDialogDismissedOnNegativeClick() {
        // set the dialog variant to confirmation/consent
        when(controller.getDialogType()).thenReturn(BluetoothPairingController.CONFIRMATION_DIALOG);

        // we don't care what this does, just that it is called
        doNothing().when(controller).onDialogNegativeClick(any());

        // build the fragment
        BluetoothPairingDialogFragment frag = makeFragment();

        // click the button and verify that the controller hook was called
        frag.onClick(frag.getmDialog(), AlertDialog.BUTTON_NEGATIVE);

        verify(controller, times(1)).onDialogNegativeClick(any());
        verify(dialogActivity, times(1)).dismiss();
    }

    @Ignore
    @Test
    public void rotateDialog_nullPinText_okButtonEnabled() {
        userEntryDialogExistingTextTest(null);
    }

    @Ignore
    @Test
    public void rotateDialog_emptyPinText_okButtonEnabled() {
        userEntryDialogExistingTextTest("");
    }

    @Ignore
    @Test
    public void rotateDialog_nonEmptyPinText_okButtonEnabled() {
        userEntryDialogExistingTextTest("test");
    }

    @Test
    public void groupPairing_setMemberDevice_showsMessageHint() {
        // set the correct dialog type
        when(controller.getDialogType()).thenReturn(BluetoothPairingController.CONFIRMATION_DIALOG);
        when(controller.isCoordinatedSetMemberDevice()).thenReturn(true);

        // build the fragment
        BluetoothPairingDialogFragment frag = makeFragment();

        // verify message is what we expect it to be and is visible
        TextView message = frag.getmDialog().findViewById(R.id.pairing_group_message);
        assertThat(message.getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Test
    public void groupPairing_nonSetMemberDevice_hidesMessageHint() {
        // set the correct dialog type
        when(controller.getDialogType()).thenReturn(BluetoothPairingController.CONFIRMATION_DIALOG);
        when(controller.isCoordinatedSetMemberDevice()).thenReturn(false);

        // build the fragment
        BluetoothPairingDialogFragment frag = makeFragment();

        // verify message is what we expect it to be and is visible
        TextView message = frag.getmDialog().findViewById(R.id.pairing_group_message);
        assertThat(message.getVisibility()).isEqualTo(View.GONE);
    }

    // Runs a test simulating the user entry dialog type in a situation like device rotation, where
    // the dialog fragment gets created and we already have some existing text entered into the
    // pin field.
    private void userEntryDialogExistingTextTest(CharSequence existingText) {
        when(controller.getDialogType()).thenReturn(BluetoothPairingController.USER_ENTRY_DIALOG);
        when(controller.getDeviceVariantMessageHintId())
                .thenReturn(BluetoothPairingController.INVALID_DIALOG_TYPE);
        when(controller.getDeviceVariantMessageId())
                .thenReturn(BluetoothPairingController.INVALID_DIALOG_TYPE);

        BluetoothPairingDialogFragment fragment = spy(new BluetoothPairingDialogFragment());
        when(fragment.getPairingViewText()).thenReturn(existingText);
        setupFragment(fragment);
        AlertDialog dialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(dialog).isNotNull();
        boolean expected = !TextUtils.isEmpty(existingText);
        assertThat(dialog.getButton(Dialog.BUTTON_POSITIVE).isEnabled()).isEqualTo(expected);
    }

    private void setupFragment(BluetoothPairingDialogFragment frag) {
        assertThat(frag.isPairingControllerSet()).isFalse();
        frag.setPairingController(controller);
        assertThat(frag.isPairingDialogActivitySet()).isFalse();
        frag.setPairingDialogActivity(dialogActivity);
        FragmentController.setupFragment(frag, FragmentActivity.class, 0 /* containerViewId */,
                null /* bundle */);
        assertThat(frag.getmDialog()).isNotNull();
        assertThat(frag.isPairingControllerSet()).isTrue();
        assertThat(frag.isPairingDialogActivitySet()).isTrue();
    }

    private BluetoothPairingDialogFragment makeFragment() {
        BluetoothPairingDialogFragment frag = new BluetoothPairingDialogFragment();
        setupFragment(frag);
        return frag;
    }
}
