/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.widget;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import android.content.Context;
import android.view.View;
import android.widget.Button;

import androidx.preference.PreferenceViewHolder;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class CardPreferenceTest {

    private CardPreference mCardPreference;
    private PreferenceViewHolder mHolder;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        context.setTheme(R.style.Theme_Settings);
        mCardPreference = new CardPreference(context);

        View rootView = View.inflate(context, R.layout.card_preference_layout, /* parent= */ null);
        mHolder = PreferenceViewHolder.createInstanceForTests(rootView);
    }

    @Test
    public void newACardPreference_layoutResourceShouldBeCardPreferenceLayout() {
        Context context = ApplicationProvider.getApplicationContext();
        context.setTheme(R.style.SettingsPreferenceTheme);

        CardPreference cardPreference = new CardPreference(context);

        assertThat(cardPreference.getLayoutResource()).isEqualTo(R.layout.card_preference_layout);
    }

    @Test
    public void onBindViewHolder_noButtonVisible_buttonsLayoutShouldBeGone() {
        mCardPreference.onBindViewHolder(mHolder);

        assertThat(getCardPreferenceButtonsView().getVisibility()).isEqualTo(GONE);
    }

    @Test
    public void onBindViewHolder_setPrimaryButtonVisibility_buttonsLayoutShouldBeVisible() {
        mCardPreference.setPrimaryButtonVisible(true);

        mCardPreference.onBindViewHolder(mHolder);

        assertThat(getCardPreferenceButtonsView().getVisibility()).isEqualTo(VISIBLE);
    }

    @Test
    public void onBindViewHolder_setPrimaryButtonVisibility_shouldApplyToPrimaryButton() {
        mCardPreference.setPrimaryButtonVisible(true);

        mCardPreference.onBindViewHolder(mHolder);

        assertThat(getPrimaryButton().getVisibility()).isEqualTo(VISIBLE);
    }

    @Test
    public void onBindViewHolder_setSecondaryButtonVisibility_buttonsLayoutShouldBeVisible() {
        mCardPreference.setSecondaryButtonVisible(true);

        mCardPreference.onBindViewHolder(mHolder);

        assertThat(getCardPreferenceButtonsView().getVisibility()).isEqualTo(VISIBLE);
    }

    @Test
    public void onBindViewHolder_setSecondaryButtonVisibility_shouldApplyToSecondaryButton() {
        mCardPreference.setSecondaryButtonVisible(true);

        mCardPreference.onBindViewHolder(mHolder);

        assertThat(getSecondaryButton().getVisibility()).isEqualTo(VISIBLE);
    }

    @Test
    public void onBindViewHolder_setPrimaryButtonText_shouldApplyToPrimaryButton() {
        String expectedText = "primary-button";
        mCardPreference.setPrimaryButtonText(expectedText);

        mCardPreference.onBindViewHolder(mHolder);

        assertThat(getPrimaryButton().getText().toString()).isEqualTo(expectedText);
    }

    @Test
    public void onBindViewHolder_setSecondaryButtonText_shouldApplyToSecondaryButton() {
        String expectedText = "secondary-button";
        mCardPreference.setSecondaryButtonText(expectedText);

        mCardPreference.onBindViewHolder(mHolder);

        assertThat(getSecondaryButton().getText().toString()).isEqualTo(expectedText);
    }

    @Test
    public void onBindViewHolder_initialTextForPrimaryButtonShouldBeEmpty() {
        mCardPreference.onBindViewHolder(mHolder);

        assertThat(getPrimaryButton().getText().toString()).isEqualTo("");
    }

    @Test
    public void onBindViewHolder_initialTextForSecondaryButtonShouldBeEmpty() {
        mCardPreference.onBindViewHolder(mHolder);

        assertThat(getSecondaryButton().getText().toString()).isEqualTo("");
    }

    @Test
    public void performClickOnPrimaryButton_shouldCalledClickListener() {
        final boolean[] hasCalled = {false};
        View.OnClickListener clickListener = v -> hasCalled[0] = true;
        mCardPreference.setPrimaryButtonClickListener(clickListener);

        mCardPreference.onBindViewHolder(mHolder);
        getPrimaryButton().performClick();

        assertThat(hasCalled[0]).isTrue();
    }

    @Test
    public void performClickOnSecondaryButton_shouldCalledClickListener() {
        final boolean[] hasCalled = {false};
        View.OnClickListener clickListener = v -> hasCalled[0] = true;
        mCardPreference.setSecondaryButtonClickListener(clickListener);

        mCardPreference.onBindViewHolder(mHolder);
        getSecondaryButton().performClick();

        assertThat(hasCalled[0]).isTrue();
    }

    @Test
    public void onBindViewHolder_primaryButtonDefaultIsGone() {
        mCardPreference.onBindViewHolder(mHolder);

        assertThat(getPrimaryButton().getVisibility()).isEqualTo(GONE);
    }

    @Test
    public void onBindViewHolder_secondaryButtonDefaultIsGone() {
        mCardPreference.onBindViewHolder(mHolder);

        assertThat(getSecondaryButton().getVisibility()).isEqualTo(GONE);
    }

    @Test
    public void setPrimaryButtonVisibility_setTrueAfterBindViewHolder_shouldBeVisible() {
        mCardPreference.setPrimaryButtonVisible(false);
        mCardPreference.onBindViewHolder(mHolder);

        mCardPreference.setPrimaryButtonVisible(true);

        assertThat(getPrimaryButton().getVisibility()).isEqualTo(VISIBLE);
    }

    @Test
    public void setPrimaryButtonText_setAfterBindViewHolder_setOnUi() {
        String expectedText = "123456";
        mCardPreference.onBindViewHolder(mHolder);

        mCardPreference.setPrimaryButtonText(expectedText);

        assertThat(getPrimaryButton().getText().toString()).isEqualTo(expectedText);
    }

    @Test
    public void setPrimaryButtonText_setNull_shouldBeEmptyText() {
        final String emptyString = "";
        mCardPreference.setPrimaryButtonText("1234");
        mCardPreference.onBindViewHolder(mHolder);

        mCardPreference.setPrimaryButtonText(null);

        assertThat(getPrimaryButton().getText().toString()).isEqualTo(emptyString);
    }

    @Test
    public void setPrimaryButtonClickListener_setAfterOnBindViewHolder() {
        final String[] hasCalled = {""};
        String expectedClickedResult = "was called";
        View.OnClickListener clickListener = v -> hasCalled[0] = expectedClickedResult;
        mCardPreference.onBindViewHolder(mHolder);

        mCardPreference.setPrimaryButtonClickListener(clickListener);
        getPrimaryButton().performClick();

        assertThat(hasCalled[0]).isEqualTo(expectedClickedResult);
    }

    @Test
    public void setPrimaryButtonClickListener_setNull_shouldClearTheOnClickListener() {
        final String[] hasCalled = {"not called"};
        View.OnClickListener clickListener = v -> hasCalled[0] = "called once";
        mCardPreference.setPrimaryButtonClickListener(clickListener);
        mCardPreference.onBindViewHolder(mHolder);

        mCardPreference.setPrimaryButtonClickListener(null);
        getPrimaryButton().performClick();

        assertThat(hasCalled[0]).isEqualTo("not called");
    }

    @Test
    public void setSecondaryButtonVisibility_setTrueAfterBindViewHolder_shouldBeVisible() {
        mCardPreference.setSecondaryButtonVisible(false);
        mCardPreference.onBindViewHolder(mHolder);

        mCardPreference.setSecondaryButtonVisible(true);

        assertThat(getSecondaryButton().getVisibility()).isEqualTo(VISIBLE);
    }

    @Test
    public void setSecondaryButtonText_setAfterBindViewHolder_setOnUi() {
        String expectedText = "10101010";
        mCardPreference.onBindViewHolder(mHolder);

        mCardPreference.setSecondaryButtonText(expectedText);

        assertThat(getSecondaryButton().getText().toString()).isEqualTo(expectedText);
    }

    @Test
    public void setSecondaryButtonText_setNull_shouldBeEmptyText() {
        String emptyString = "";
        mCardPreference.setSecondaryButtonText("1234");
        mCardPreference.onBindViewHolder(mHolder);

        mCardPreference.setSecondaryButtonText(null);

        assertThat(getSecondaryButton().getText().toString()).isEqualTo(emptyString);
    }

    @Test
    public void setSecondaryButtonClickListener_setAfterOnBindViewHolder() {
        final String[] hasCalled = {""};
        String expectedClickedResult = "2nd was called";
        View.OnClickListener clickListener = v -> hasCalled[0] = expectedClickedResult;
        mCardPreference.onBindViewHolder(mHolder);

        mCardPreference.setSecondaryButtonClickListener(clickListener);
        getSecondaryButton().performClick();

        assertThat(hasCalled[0]).isEqualTo(expectedClickedResult);
    }

    @Test
    public void setSecondaryButtonClickListener_setNull_shouldClearTheOnClickListener() {
        final String[] hasCalled = {"not called"};
        View.OnClickListener clickListener = v -> hasCalled[0] = "called once";
        mCardPreference.setSecondaryButtonClickListener(clickListener);
        mCardPreference.onBindViewHolder(mHolder);

        mCardPreference.setSecondaryButtonClickListener(null);
        getSecondaryButton().performClick();

        assertThat(hasCalled[0]).isEqualTo("not called");
    }

    @Test
    public void
            setPrimaryButtonVisibility_onlyPrimaryButtonVisible_setGone_buttonGroupShouldBeGone() {
        mCardPreference.setPrimaryButtonVisible(true);
        mCardPreference.setSecondaryButtonVisible(false);
        mCardPreference.onBindViewHolder(mHolder);
        assertWithMessage("PreCondition: buttonsView should be Visible")
                .that(getCardPreferenceButtonsView().getVisibility())
                .isEqualTo(VISIBLE);

        mCardPreference.setPrimaryButtonVisible(false);

        assertThat(getCardPreferenceButtonsView().getVisibility()).isEqualTo(GONE);
    }

    @Test
    public void
            setSecondaryButtonVisibility_only2ndButtonVisible_setGone_buttonGroupShouldBeGone() {
        mCardPreference.setPrimaryButtonVisible(false);
        mCardPreference.setSecondaryButtonVisible(true);
        mCardPreference.onBindViewHolder(mHolder);
        assertWithMessage("PreCondition: buttonsView should be Visible")
                .that(getCardPreferenceButtonsView().getVisibility())
                .isEqualTo(VISIBLE);

        mCardPreference.setSecondaryButtonVisible(false);

        assertThat(getCardPreferenceButtonsView().getVisibility()).isEqualTo(GONE);
    }

    private View getCardPreferenceButtonsView() {
        return mHolder.findViewById(R.id.card_preference_buttons);
    }

    private Button getPrimaryButton() {
        return (Button) mHolder.findViewById(android.R.id.button1);
    }

    private Button getSecondaryButton() {
        return (Button) mHolder.findViewById(android.R.id.button2);
    }
}
