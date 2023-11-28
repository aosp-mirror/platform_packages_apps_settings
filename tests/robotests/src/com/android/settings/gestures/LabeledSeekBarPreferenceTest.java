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

package com.android.settings.gestures;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settings.testutils.shadow.ShadowInteractionJankMonitor;
import com.android.settings.testutils.shadow.ShadowUserManager;
import com.android.settings.widget.LabeledSeekBarPreference;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

/**
 * Tests for {@link LabeledSeekBarPreference}.
 */
@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        ShadowUserManager.class,
        ShadowInteractionJankMonitor.class,
})
public class LabeledSeekBarPreferenceTest {

    private Context mContext;
    private PreferenceViewHolder mViewHolder;
    private SeekBar mSeekBar;
    private TextView mSummary;
    private ViewGroup mIconStartFrame;
    private ViewGroup mIconEndFrame;
    private View mLabelFrame;
    private LabeledSeekBarPreference mSeekBarPreference;

    @Mock
    private Preference.OnPreferenceChangeListener mListener;

    @Mock
    private SeekBar.OnSeekBarChangeListener mSeekBarChangeListener;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = Mockito.spy(RuntimeEnvironment.application);
        mSeekBarPreference = new LabeledSeekBarPreference(mContext, null);
        LayoutInflater inflater = LayoutInflater.from(mContext);
        final View view =
                inflater.inflate(mSeekBarPreference.getLayoutResource(),
                        new LinearLayout(mContext), false);
        mViewHolder = PreferenceViewHolder.createInstanceForTests(view);
        mSeekBar = (SeekBar) mViewHolder.findViewById(com.android.internal.R.id.seekbar);
        mSummary = (TextView) mViewHolder.findViewById(android.R.id.summary);
        mIconStartFrame = (ViewGroup) mViewHolder.findViewById(R.id.icon_start_frame);
        mIconEndFrame = (ViewGroup) mViewHolder.findViewById(R.id.icon_end_frame);
        mLabelFrame = mViewHolder.findViewById(R.id.label_frame);
    }

    @Test
    public void seekBarPreferenceOnStopTrackingTouch_callsListener() {
        mSeekBar.setProgress(2);

        mSeekBarPreference.setOnPreferenceChangeStopListener(mListener);
        mSeekBarPreference.onStopTrackingTouch(mSeekBar);

        verify(mListener, times(1)).onPreferenceChange(mSeekBarPreference, 2);
    }

    @Test
    public void seekBarPreferenceSummarySet_returnsValue() {
        final String summary = "this is a summary";
        mSeekBarPreference.setSummary(summary);
        mSeekBarPreference.onBindViewHolder(mViewHolder);

        assertThat(mSeekBarPreference.getSummary()).isEqualTo(summary);
        assertThat(mSummary.getText()).isEqualTo(summary);
    }

    @Test
    public void seekBarPreferenceSummaryNull_hidesView() {
        mSeekBarPreference.setSummary(null);
        mSeekBarPreference.onBindViewHolder(mViewHolder);

        assertThat(mSummary.getText()).isEqualTo("");
        assertThat(mSummary.getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void setTextAttributes_textStart_textEnd_labelFrameVisible() {
        final AttributeSet attributeSet = Robolectric.buildAttributeSet()
                .addAttribute(R.attr.textStart, "@string/screen_zoom_make_smaller_desc")
                .addAttribute(R.attr.textEnd, "@string/screen_zoom_make_larger_desc")
                .build();
        final LabeledSeekBarPreference seekBarPreference =
                new LabeledSeekBarPreference(mContext, attributeSet);

        seekBarPreference.onBindViewHolder(mViewHolder);

        assertThat(mLabelFrame.getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Test
    public void notSetTextAttributes_labelFrameGone() {
        final AttributeSet attributeSet = Robolectric.buildAttributeSet()
                .build();
        final LabeledSeekBarPreference seekBarPreference =
                new LabeledSeekBarPreference(mContext, attributeSet);

        seekBarPreference.onBindViewHolder(mViewHolder);

        assertThat(mLabelFrame.getVisibility()).isEqualTo(View.GONE);
    }

    @Ignore("b/313594999")
    @Test
    public void setIconAttributes_iconVisible() {
        final AttributeSet attributeSet = Robolectric.buildAttributeSet()
                .addAttribute(R.attr.iconStart, "@drawable/ic_remove_24dp")
                .addAttribute(R.attr.iconEnd, "@drawable/ic_add_24dp")
                .build();
        final LabeledSeekBarPreference seekBarPreference =
                new LabeledSeekBarPreference(mContext, attributeSet);

        seekBarPreference.onBindViewHolder(mViewHolder);

        assertThat(mIconStartFrame.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(mIconEndFrame.getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Test
    public void notSetIconAttributes_iconGone() {
        final AttributeSet attributeSet = Robolectric.buildAttributeSet()
                .build();
        final LabeledSeekBarPreference seekBarPreference =
                new LabeledSeekBarPreference(mContext, attributeSet);

        seekBarPreference.onBindViewHolder(mViewHolder);

        assertThat(mIconStartFrame.getVisibility()).isEqualTo(View.GONE);
        assertThat(mIconEndFrame.getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void setSeekBarListener_success() {
        mSeekBarPreference.setOnSeekBarChangeListener(mSeekBarChangeListener);
        mSeekBarPreference.onStartTrackingTouch(mSeekBar);
        mSeekBarPreference.onProgressChanged(mSeekBar, /* progress= */ 0,
                /* fromUser= */ false);
        mSeekBarPreference.onStopTrackingTouch(mSeekBar);

        verify(mSeekBarChangeListener).onStartTrackingTouch(any(SeekBar.class));
        verify(mSeekBarChangeListener).onProgressChanged(any(SeekBar.class), anyInt(),
                anyBoolean());
        verify(mSeekBarChangeListener).onStopTrackingTouch(any(SeekBar.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void setContentDescriptionWithoutIcon_throwException() {
        final AttributeSet attributeSet = Robolectric.buildAttributeSet()
                .addAttribute(R.attr.iconStartContentDescription,
                        "@string/screen_zoom_make_smaller_desc")
                .addAttribute(R.attr.iconEndContentDescription,
                        "@string/screen_zoom_make_larger_desc")
                .build();

        new LabeledSeekBarPreference(mContext, attributeSet);
    }

    @Ignore("b/313594999")
    @Test
    public void setContentDescriptionWithIcon_success() {
        final String startDescription =
                mContext.getResources().getString(R.string.screen_zoom_make_smaller_desc);
        final String endDescription =
                mContext.getResources().getString(R.string.screen_zoom_make_larger_desc);
        final AttributeSet attributeSet = Robolectric.buildAttributeSet()
                .addAttribute(R.attr.iconStart, "@drawable/ic_remove_24dp")
                .addAttribute(R.attr.iconEnd, "@drawable/ic_add_24dp")
                .addAttribute(R.attr.iconStartContentDescription,
                        "@string/screen_zoom_make_smaller_desc")
                .addAttribute(R.attr.iconEndContentDescription,
                        "@string/screen_zoom_make_larger_desc")
                .build();
        final LabeledSeekBarPreference seekBarPreference =
                new LabeledSeekBarPreference(mContext, attributeSet);

        seekBarPreference.onBindViewHolder(mViewHolder);

        assertThat(mIconStartFrame.getContentDescription().toString().contentEquals(
                startDescription)).isTrue();
        assertThat(mIconEndFrame.getContentDescription().toString().contentEquals(
                endDescription)).isTrue();
    }

    @Test
    public void notSetContentDescriptionAttributes_noDescription() {
        final AttributeSet attributeSet = Robolectric.buildAttributeSet()
                .build();
        final LabeledSeekBarPreference seekBarPreference =
                new LabeledSeekBarPreference(mContext, attributeSet);

        seekBarPreference.onBindViewHolder(mViewHolder);

        assertThat(mIconStartFrame.getContentDescription()).isNull();
        assertThat(mIconEndFrame.getContentDescription()).isNull();
    }
}
