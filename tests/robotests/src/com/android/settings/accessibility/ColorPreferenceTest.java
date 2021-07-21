/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.accessibility;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.preference.PreferenceViewHolder;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link ColorPreference}. */
@RunWith(RobolectricTestRunner.class)
public class ColorPreferenceTest {

    private Context mContext;
    private static final int COLOR_TRANSPARENT_VALUE = 0;
    private static final int COLOR_WHITE_VALUE = 0xFFFFFFFF;
    private static final int COLOR_BLACK_VALUE = 0xFF000000;
    private static final String COLOR_TRANSPARENT = "TRANSPARENT";
    private static final String COLOR_WHITE = "WHITE";
    private static final String COLOR_BLACK = "BLACK";
    private final int[] mColorValues =
            {COLOR_TRANSPARENT_VALUE, COLOR_WHITE_VALUE, COLOR_BLACK_VALUE};
    private final String[] mColorTitles = {COLOR_TRANSPARENT, COLOR_WHITE, COLOR_BLACK};
    private View mRootView;
    private ImageView mImageView;
    private TextView mTextView;
    private ColorPreference mColorPreference;
    private PreferenceViewHolder mViewHolder;

    @Before
    public void init() {
        mContext = ApplicationProvider.getApplicationContext();
        mRootView = spy(new View(mContext));
        mViewHolder = spy(PreferenceViewHolder.createInstanceForTests(mRootView));
        mImageView = spy(new ImageView(mContext));
        mTextView = spy(new TextView(mContext));

        final AttributeSet attributeSet = Robolectric.buildAttributeSet().build();
        mColorPreference = new ColorPreference(mContext, attributeSet);
    }

    @Test
    public void setPreviewEnabled_enabled_shouldSetCustomLayout() {
        mColorPreference.setPreviewEnabled(true);

        assertThat(mColorPreference.getWidgetLayoutResource()).isEqualTo(R.layout.preference_color);
    }

    @Test
    public void setPreviewEnabled_disabled_shouldSetInvalidId() {
        mColorPreference.setPreviewEnabled(false);

        assertThat(mColorPreference.getWidgetLayoutResource()).isEqualTo(0);
    }

    @Test
    public void setTitles_titlesExist_returnTitle() {
        mColorPreference.setTitles(mColorTitles);

        assertThat(mColorPreference.getTitleAt(/* index= */ 0)).isEqualTo(mColorTitles[0]);
        assertThat(mColorPreference.getTitleAt(/* index= */ 1)).isEqualTo(mColorTitles[1]);
        assertThat(mColorPreference.getTitleAt(/* index= */ 2)).isEqualTo(mColorTitles[2]);
    }

    @Test
    public void setTitles_noTitle_returnRGBText() {
        final int testIndex = 0;
        mColorPreference.setValues(mColorValues);
        final ListDialogPreference listDialogPreference = (ListDialogPreference) mColorPreference;
        final int value = listDialogPreference.getValueAt(testIndex);
        final int r = Color.red(value);
        final int g = Color.green(value);
        final int b = Color.blue(value);
        final String rgbText = mContext.getString(R.string.color_custom, r, g, b);

        mColorPreference.setTitles(null);
        final CharSequence title = mColorPreference.getTitleAt(testIndex);

        assertThat(title).isEqualTo(rgbText);
    }

    @Test
    public void onBindViewHolder_enabled_transparent_matchBackgroundResource() {
        doReturn(mImageView).when(mViewHolder).findViewById(R.id.color_preview);
        mColorPreference.setPreviewEnabled(true);

        mColorPreference.setEnabled(true);
        mColorPreference.setTitles(mColorTitles);
        mColorPreference.setValues(mColorValues);
        mColorPreference.setValue(COLOR_TRANSPARENT_VALUE);
        mColorPreference.onBindViewHolder(mViewHolder);

        verify(mImageView).setBackgroundResource(R.drawable.transparency_tileable);
    }

    @Test
    public void onBindViewHolder_enabled_titlesExist_matchDescription() {
        doReturn(mImageView).when(mViewHolder).findViewById(R.id.color_preview);
        mColorPreference.setPreviewEnabled(true);

        mColorPreference.setEnabled(true);
        mColorPreference.setTitles(mColorTitles);
        mColorPreference.setValues(mColorValues);
        mColorPreference.setValue(COLOR_WHITE_VALUE);
        mColorPreference.onBindViewHolder(mViewHolder);

        verify(mImageView).setContentDescription(COLOR_WHITE);
    }

    @Test
    public void onBindViewHolder_disabled_matchAlpha() {
        doReturn(mImageView).when(mViewHolder).findViewById(R.id.color_preview);
        mColorPreference.setPreviewEnabled(true);
        mColorPreference.setValues(mColorValues);
        mColorPreference.setValue(COLOR_WHITE_VALUE);

        mColorPreference.setEnabled(false);
        mColorPreference.onBindViewHolder(mViewHolder);

        verify(mImageView).setAlpha(0.2f);
    }

    @Test
    public void onBindListItem_transparent_matchBackgroundResource() {
        final int colorTransparentIndex = 0;
        doReturn(mImageView).when(mRootView).findViewById(R.id.color_swatch);
        doReturn(mTextView).when(mRootView).findViewById(R.id.summary);
        mColorPreference.setTitles(mColorTitles);
        mColorPreference.setValues(mColorValues);

        mColorPreference.onBindListItem(mRootView, colorTransparentIndex);

        verify(mImageView).setBackgroundResource(R.drawable.transparency_tileable);
    }

    @Test
    public void onBindListItem_colorDrawable_matchColor() {
        final int testIndex = 0;
        final ColorDrawable colorDrawable = spy(new ColorDrawable());
        doReturn(mImageView).when(mRootView).findViewById(R.id.color_swatch);
        doReturn(colorDrawable).when(mImageView).getDrawable();
        doReturn(mTextView).when(mRootView).findViewById(R.id.summary);
        mColorPreference.setTitles(mColorTitles);
        mColorPreference.setValues(mColorValues);

        mColorPreference.onBindListItem(mRootView, testIndex);
        final int argb = mColorPreference.getValueAt(testIndex);
        final int alpha = Color.alpha(argb);

        verify(colorDrawable).setColor(alpha);
    }

    @Test
    public void onBindListItem_colorDrawable_matchSummary() {
        final int testIndex = 0;
        doReturn(mImageView).when(mRootView).findViewById(R.id.color_swatch);
        doReturn(mTextView).when(mRootView).findViewById(R.id.summary);
        mColorPreference.setTitles(mColorTitles);
        mColorPreference.setValues(mColorValues);

        mColorPreference.onBindListItem(mRootView, /* index= */ testIndex);
        final CharSequence title = mColorPreference.getTitleAt(testIndex);

        verify(mTextView).setText(title);
    }
}
