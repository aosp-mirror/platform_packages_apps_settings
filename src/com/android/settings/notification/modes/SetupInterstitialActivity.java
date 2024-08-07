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

import static android.graphics.drawable.GradientDrawable.LINEAR_GRADIENT;
import static android.graphics.drawable.GradientDrawable.Orientation.BL_TR;
import static android.provider.Settings.EXTRA_AUTOMATIC_ZEN_RULE_ID;

import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.FragmentActivity;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settingslib.notification.modes.ZenMode;
import com.android.settingslib.notification.modes.ZenModesBackend;

/**
 * Interstitial page for modes that are disabled, but not disabled by the user. This page
 * provides a button to enable the mode, and then goes to the mode setup page.
 */
public class SetupInterstitialActivity extends FragmentActivity {
    private static final String TAG = "ModeSetupInterstitial";
    private ZenModesBackend mBackend;

    private final ViewOutlineProvider mOutlineProvider = new ViewOutlineProvider() {
        @Override
        public void getOutline(View view, Outline outline) {
            // Provides a rounded rectangle outline whose width & height matches the View.
            float cornerRadius = getResources().getDimensionPixelSize(
                    R.dimen.zen_mode_interstitial_corner_radius);
            outline.setRoundRect(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight(),
                    cornerRadius);
        }
    };

    /**
     * Returns an intent leading to this page for the given mode and context.
     */
    public static @NonNull Intent getIntent(@NonNull Context context, @NonNull ZenMode mode) {
        return new Intent(Intent.ACTION_MAIN)
                .setClass(context, SetupInterstitialActivity.class)
                .setPackage(context.getPackageName())
                .setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
                .putExtra(EXTRA_AUTOMATIC_ZEN_RULE_ID, mode.getId());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        Utils.setupEdgeToEdge(this);
        super.onCreate(savedInstanceState);
        mBackend = ZenModesBackend.getInstance(this);
        setContentView(R.layout.mode_interstitial_layout);

        // Set up toolbar to only have a back button & no title
        Toolbar toolbar = findViewById(R.id.action_bar);
        setActionBar(toolbar);
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayShowTitleEnabled(false);
        }
    }

    @Override
    public boolean onNavigateUp() {
        // have the home button on the action bar go back
        getOnBackPressedDispatcher().onBackPressed();
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        // See if we have mode data
        final Intent intent = getIntent();
        if (intent == null) {
            Log.w(TAG, "no intent found for modes interstitial");
            finish();
        }

        String modeId = intent.getStringExtra(EXTRA_AUTOMATIC_ZEN_RULE_ID);
        if (modeId == null) {
            Log.w(TAG, "no mode id included in intent: " + intent);
            finish();
        }

        ZenMode mode = mBackend.getMode(modeId);
        if (mode == null) {
            Log.w(TAG, "mode not found for mode id: " + modeId);
            finish();
        }
        setTitle(mode.getName());

        TextView title = findViewById(R.id.mode_name_title);
        if (title != null) {
            title.setText(mode.getName());
        }

        ImageView img = findViewById(R.id.image);
        if (img != null) {
            setImage(img, mode);
        }

        Button button = findViewById(R.id.enable_mode_button);
        if (button != null) {
            setupButton(button, mode);
        }
    }

    private void setImage(@NonNull ImageView img, @NonNull ZenMode mode) {
        img.setImageDrawable(getModeDrawable(mode));
        img.setClipToOutline(true);
        img.setOutlineProvider(mOutlineProvider);

        FrameLayout frame = findViewById(R.id.image_frame);
        if (frame == null) {
            return;
        }
        if (img.getMeasuredWidth() == 0) {
            // set up to resize after the global layout occurs
            img.getViewTreeObserver().addOnGlobalLayoutListener(
                    new ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            img.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                            sizeImageToFrame(img, frame);
                        }
                    });
        } else {
            // measured already, resize it now
            sizeImageToFrame(img, frame);
        }
    }

    private Drawable getModeDrawable(@NonNull ZenMode mode) {
        // TODO: b/332730534 - set actual images depending on mode type (asynchronously?)
        GradientDrawable placeholder = new GradientDrawable();
        placeholder.setSize(40, 60);  // 4x6 rectangle, slightly taller than wide
        placeholder.setGradientType(LINEAR_GRADIENT);
        placeholder.setOrientation(BL_TR);
        placeholder.setColors(new int[]{Color.BLACK, Color.WHITE});
        return placeholder;
    }

    @VisibleForTesting
    protected void sizeImageToFrame(ImageView img, FrameLayout frame) {
        // width of the space we have available = overall size of frame - relevant padding
        int frameHeight =
                frame.getMeasuredHeight() - frame.getPaddingTop() - frame.getPaddingBottom();
        int frameWidth =
                frame.getMeasuredWidth() - frame.getPaddingLeft() - frame.getPaddingRight();

        int imgHeight = img.getDrawable().getIntrinsicHeight();
        int imgWidth = img.getDrawable().getIntrinsicWidth();

        // if any of these are 0, give up because we won't be able to do the relevant math (and
        // we probably don't have the relevant data set up)
        if (frameHeight == 0 || frameWidth == 0 || imgHeight == 0 || imgWidth == 0) {
            Log.w(TAG, "image or frame has invalid size parameters");
            return;
        }
        float frameHWRatio = ((float) frameHeight) / frameWidth;
        float imgHWRatio = ((float) imgHeight) / imgWidth;

        // fit horizontal dimension if the frame has a taller ratio (height/width) than the image;
        // otherwise, fit the vertical direction
        boolean fitHorizontal = frameHWRatio > imgHWRatio;

        ViewGroup.LayoutParams layoutParams = img.getLayoutParams();
        if (layoutParams == null) {
            Log.w(TAG, "image has null LayoutParams");
            return;
        }
        if (fitHorizontal) {
            layoutParams.width = frameWidth;
            float scaledHeight = imgHWRatio * frameWidth;
            layoutParams.height = (int) scaledHeight;
        } else {
            layoutParams.height = frameHeight;
            float scaledWidth = /* w/h ratio */ (1 / imgHWRatio) * frameHeight;
            layoutParams.width = (int) scaledWidth;
        }
        img.setLayoutParams(layoutParams);
    }

    private void setupButton(Button button, @NonNull ZenMode mode) {
        button.setText(getString(R.string.zen_mode_setup_button_label, mode.getName()));
        button.setOnClickListener(enableButtonListener(mode.getId()));
    }

    @VisibleForTesting
    View.OnClickListener enableButtonListener(String modeId) {
        return unused -> {
            // When clicked, we first reload mode info in case it has changed in the interim,
            // then enable the mode and then send the user to the mode's configuration page.
            boolean updated = enableMode(modeId);

            // Don't come back to this activity after sending the user to the modes page, if
            // they happen to go back. Forward the activity result in case we got here (indirectly)
            // from some app that is waiting for the result.
            finish();
            if (updated) {
                ZenSubSettingLauncher.forMode(this, modeId)
                        .addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT).launch();
            }
        };
    }

    // Enables the given mode after first refreshing its data from the backend. Returns true if
    // the update went through, and false if for some reason the mode wasn't found.
    private boolean enableMode(@NonNull String modeId) {
        if (mBackend == null) {
            return false;
        }

        ZenMode modeToUpdate = mBackend.getMode(modeId);
        if (modeToUpdate == null) {
            // tell the user the mode isn't found, for some reason
            Toast.makeText(this, R.string.zen_mode_rule_not_found_text, Toast.LENGTH_SHORT)
                    .show();
            return false;
        }

        modeToUpdate.getRule().setEnabled(true);
        mBackend.updateMode(modeToUpdate);
        return true;
    }
}
