/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.notification.modes;

import static android.provider.Settings.EXTRA_AUTOMATIC_ZEN_RULE_ID;

import static com.android.settings.SettingsActivity.EXTRA_SHOW_FRAGMENT;
import static com.android.settings.SettingsActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.test.core.app.ActivityScenario;

import com.android.settingslib.notification.modes.TestModeBuilder;
import com.android.settingslib.notification.modes.ZenMode;
import com.android.settingslib.notification.modes.ZenModesBackend;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class SetupInterstitialActivityTest {
    private static final String MODE_ID = "modeId";

    @Mock
    private ZenModesBackend mBackend;

    @Mock
    private ImageView mImage;

    @Mock
    private Drawable mDrawable;

    @Mock
    private FrameLayout mFrame;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        // set global backend instance so that when the interstitial activity launches, it'll get
        // this mock backend
        ZenModesBackend.setInstance(mBackend);

        when(mBackend.getMode(MODE_ID)).thenReturn(new TestModeBuilder().build());
        when(mImage.getDrawable()).thenReturn(mDrawable);
        when(mImage.getLayoutParams()).thenReturn(new ViewGroup.LayoutParams(0, 0));
    }

    @Test
    public void enableButton_enablesModeAndRedirectsToModePage() {
        ZenMode mode = new TestModeBuilder().setId(MODE_ID).setEnabled(false).build();
        when(mBackend.getMode(MODE_ID)).thenReturn(mode);

        // Set up scenario with this mode information
        ActivityScenario<SetupInterstitialActivity> scenario =
                ActivityScenario.launch(new Intent(Intent.ACTION_MAIN)
                        .setClass(RuntimeEnvironment.getApplication(),
                                SetupInterstitialActivity.class)
                        .putExtra(EXTRA_AUTOMATIC_ZEN_RULE_ID, MODE_ID));
        scenario.onActivity(activity -> {
            View.OnClickListener listener = activity.enableButtonListener(MODE_ID);

            // simulate button press even though we don't actually have a button
            listener.onClick(null);

            // verify that the backend got a request to enable the mode
            ArgumentCaptor<ZenMode> captor = ArgumentCaptor.forClass(ZenMode.class);
            verify(mBackend).updateMode(captor.capture());
            ZenMode updatedMode = captor.getValue();
            assertThat(updatedMode.getId()).isEqualTo(MODE_ID);
            assertThat(updatedMode.isEnabled()).isTrue();

            // confirm that the next activity is the mode page
            Intent openModePageIntent = shadowOf(activity).getNextStartedActivity();
            assertThat(openModePageIntent.getStringExtra(EXTRA_SHOW_FRAGMENT))
                    .isEqualTo(ZenModeFragment.class.getName());
            Bundle fragmentArgs = openModePageIntent.getBundleExtra(EXTRA_SHOW_FRAGMENT_ARGUMENTS);
            assertThat(fragmentArgs).isNotNull();
            assertThat(fragmentArgs.getString(EXTRA_AUTOMATIC_ZEN_RULE_ID)).isEqualTo(MODE_ID);
        });
        scenario.close();
    }

    @Test
    public void setImageToFrame_sizeZero() {
        ActivityScenario<SetupInterstitialActivity> scenario =
                ActivityScenario.launch(new Intent(Intent.ACTION_MAIN)
                        .setClass(RuntimeEnvironment.getApplication(),
                                SetupInterstitialActivity.class)
                        .putExtra(EXTRA_AUTOMATIC_ZEN_RULE_ID, MODE_ID));
        scenario.onActivity(activity -> {
            // when either the image or the frame has a size 0, we do nothing
            when(mDrawable.getIntrinsicWidth()).thenReturn(0);
            when(mDrawable.getIntrinsicHeight()).thenReturn(25);
            when(mFrame.getMeasuredWidth()).thenReturn(40);
            when(mFrame.getMeasuredHeight()).thenReturn(50);

            activity.sizeImageToFrame(mImage, mFrame);
            verify(mImage, never()).setLayoutParams(any());
        });
        scenario.close();
    }

    @Test
    public void setImageToFrame_imageLargerThanFrame() {
        ActivityScenario<SetupInterstitialActivity> scenario =
                ActivityScenario.launch(new Intent(Intent.ACTION_MAIN)
                        .setClass(RuntimeEnvironment.getApplication(),
                                SetupInterstitialActivity.class)
                        .putExtra(EXTRA_AUTOMATIC_ZEN_RULE_ID, MODE_ID));
        scenario.onActivity(activity -> {
            // image: 900(w)x1500(h); frame: 600(w)x500(h)
            // image expected to be scaled down to match the height of the frame -> 300(w)x500(h)
            when(mDrawable.getIntrinsicWidth()).thenReturn(900);
            when(mDrawable.getIntrinsicHeight()).thenReturn(1500);
            when(mFrame.getMeasuredWidth()).thenReturn(600);
            when(mFrame.getMeasuredHeight()).thenReturn(500);

            ArgumentCaptor<ViewGroup.LayoutParams> captor = ArgumentCaptor.forClass(
                    ViewGroup.LayoutParams.class);
            activity.sizeImageToFrame(mImage, mFrame);
            verify(mImage).setLayoutParams(captor.capture());
            ViewGroup.LayoutParams out = captor.getValue();
            assertThat(out.width).isEqualTo(300);
            assertThat(out.height).isEqualTo(500);
        });
        scenario.close();
    }

    @Test
    public void setImageToFrame_imageSmallerThanFrame() {
        ActivityScenario<SetupInterstitialActivity> scenario =
                ActivityScenario.launch(new Intent(Intent.ACTION_MAIN)
                        .setClass(RuntimeEnvironment.getApplication(),
                                SetupInterstitialActivity.class)
                        .putExtra(EXTRA_AUTOMATIC_ZEN_RULE_ID, MODE_ID));
        scenario.onActivity(activity -> {
            // image: 300(w)x200(h); frame: 900(w)x1200(h)
            // image expected to be scaled up to match the width of the frame -> 900(w)x600(h)
            when(mDrawable.getIntrinsicWidth()).thenReturn(300);
            when(mDrawable.getIntrinsicHeight()).thenReturn(200);
            when(mFrame.getMeasuredWidth()).thenReturn(900);
            when(mFrame.getMeasuredHeight()).thenReturn(1200);

            ArgumentCaptor<ViewGroup.LayoutParams> captor = ArgumentCaptor.forClass(
                    ViewGroup.LayoutParams.class);
            activity.sizeImageToFrame(mImage, mFrame);
            verify(mImage).setLayoutParams(captor.capture());
            ViewGroup.LayoutParams out = captor.getValue();
            assertThat(out.width).isEqualTo(900);
            assertThat(out.height).isEqualTo(600);
        });
        scenario.close();
    }

    @Test
    public void setImageToFrame_horizontalImageNarrowerThanFrame() {
        ActivityScenario<SetupInterstitialActivity> scenario =
                ActivityScenario.launch(new Intent(Intent.ACTION_MAIN)
                        .setClass(RuntimeEnvironment.getApplication(),
                                SetupInterstitialActivity.class)
                        .putExtra(EXTRA_AUTOMATIC_ZEN_RULE_ID, MODE_ID));
        scenario.onActivity(activity -> {
            // image: 600(w)x400(h); frame: 1000(w)x100(h)
            // both image and frame are wider than tall, but frame is much narrower
            // so should fit image to height of frame -> 150(w)x100(h)
            when(mDrawable.getIntrinsicWidth()).thenReturn(600);
            when(mDrawable.getIntrinsicHeight()).thenReturn(400);
            when(mFrame.getMeasuredWidth()).thenReturn(1000);
            when(mFrame.getMeasuredHeight()).thenReturn(100);

            ArgumentCaptor<ViewGroup.LayoutParams> captor = ArgumentCaptor.forClass(
                    ViewGroup.LayoutParams.class);
            activity.sizeImageToFrame(mImage, mFrame);
            verify(mImage).setLayoutParams(captor.capture());
            ViewGroup.LayoutParams out = captor.getValue();
            assertThat(out.width).isEqualTo(150);
            assertThat(out.height).isEqualTo(100);
        });
        scenario.close();
    }

    @Test
    public void setImageToFrame_accountsForPadding() {
        ActivityScenario<SetupInterstitialActivity> scenario =
                ActivityScenario.launch(new Intent(Intent.ACTION_MAIN)
                        .setClass(RuntimeEnvironment.getApplication(),
                                SetupInterstitialActivity.class)
                        .putExtra(EXTRA_AUTOMATIC_ZEN_RULE_ID, MODE_ID));
        scenario.onActivity(activity -> {
            // image: 200(w)x300(h); frame: 1000(w)x1000(h), 50 top/bottom padding, 100 l/r padding
            // effective size of frame is therefore 800(w)x900(h)
            // scale image to the height of the effective frame -> 600(w)x900(h)
            when(mDrawable.getIntrinsicWidth()).thenReturn(200);
            when(mDrawable.getIntrinsicHeight()).thenReturn(300);
            when(mFrame.getMeasuredWidth()).thenReturn(1000);
            when(mFrame.getMeasuredHeight()).thenReturn(1000);
            when(mFrame.getPaddingTop()).thenReturn(50);
            when(mFrame.getPaddingBottom()).thenReturn(50);
            when(mFrame.getPaddingLeft()).thenReturn(100);
            when(mFrame.getPaddingRight()).thenReturn(100);

            ArgumentCaptor<ViewGroup.LayoutParams> captor = ArgumentCaptor.forClass(
                    ViewGroup.LayoutParams.class);
            activity.sizeImageToFrame(mImage, mFrame);
            verify(mImage).setLayoutParams(captor.capture());
            ViewGroup.LayoutParams out = captor.getValue();
            assertThat(out.width).isEqualTo(600);
            assertThat(out.height).isEqualTo(900);
        });
        scenario.close();
    }
}
