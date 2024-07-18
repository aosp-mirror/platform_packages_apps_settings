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

import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
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

    private void setImage(ImageView img, @NonNull ZenMode mode) {
        // TODO: b/332730534 - set actual images depending on mode type (asynchronously?)
        img.setImageDrawable(new ColorDrawable(Color.GRAY));
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
