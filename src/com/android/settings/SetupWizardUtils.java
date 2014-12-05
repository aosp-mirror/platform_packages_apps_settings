/*
 * Copyright (C) 2014 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.settings;

import com.android.settings.widget.SetupWizardIllustration;
import com.android.setupwizard.navigationbar.SetupWizardNavBar;


import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.view.Gravity;
import android.view.Window;
import android.widget.TextView;

public class SetupWizardUtils {
    private static final String TAG = "SetupWizardUtils";

    // Extra containing the resource name of the theme to be used
    public static final String EXTRA_THEME = "theme";
    public static final String THEME_HOLO = "holo";
    public static final String THEME_HOLO_LIGHT = "holo_light";
    public static final String THEME_MATERIAL = "material";
    public static final String THEME_MATERIAL_LIGHT = "material_light";

    public static final String EXTRA_USE_IMMERSIVE_MODE = "useImmersiveMode";

    // From WizardManager (must match constants maintained there)
    public static final String ACTION_NEXT = "com.android.wizard.NEXT";
    public static final String EXTRA_SCRIPT_URI = "scriptUri";
    public static final String EXTRA_ACTION_ID = "actionId";
    public static final String EXTRA_RESULT_CODE = "com.android.setupwizard.ResultCode";
    public static final int NEXT_REQUEST = 10000;

    public static boolean isUsingWizardManager(Activity activity) {
        return activity.getIntent().hasExtra(EXTRA_SCRIPT_URI);
    }

    /**
     * Send the results of this activity to WizardManager, which will then send out the next
     * scripted activity. WizardManager does not actually return an activity result, but if we
     * invoke WizardManager without requesting a result, the framework will choose not to issue a
     * call to onActivityResult with RESULT_CANCELED when navigating backward.
     */
    public static void sendResultsToSetupWizard(Activity activity, int resultCode) {
        final Intent intent = activity.getIntent();
        final Intent nextIntent = new Intent(ACTION_NEXT);
        nextIntent.putExtra(EXTRA_SCRIPT_URI, intent.getStringExtra(EXTRA_SCRIPT_URI));
        nextIntent.putExtra(EXTRA_ACTION_ID, intent.getStringExtra(EXTRA_ACTION_ID));
        nextIntent.putExtra(EXTRA_THEME, intent.getStringExtra(EXTRA_THEME));
        nextIntent.putExtra(EXTRA_RESULT_CODE, resultCode);
        activity.startActivityForResult(nextIntent, NEXT_REQUEST);
    }

    public static int getTheme(Intent intent, int defaultResId) {
        final String themeName = intent.getStringExtra(EXTRA_THEME);
        int resid = defaultResId;
        if (THEME_HOLO_LIGHT.equalsIgnoreCase(themeName) ||
                THEME_MATERIAL_LIGHT.equalsIgnoreCase(themeName)) {
            resid = R.style.SetupWizardTheme_Light;
        } else if (THEME_HOLO.equalsIgnoreCase(themeName) ||
                THEME_MATERIAL.equalsIgnoreCase(themeName)) {
            resid = R.style.SetupWizardTheme;
        }
        return resid;
    }

    /**
     * Sets the immersive mode related flags based on the extra in the intent which started the
     * activity.
     */
    public static void setImmersiveMode(Activity activity, SetupWizardNavBar navBar) {
        final boolean useImmersiveMode =
                activity.getIntent().getBooleanExtra(EXTRA_USE_IMMERSIVE_MODE, false);
        navBar.setUseImmersiveMode(useImmersiveMode);
        if (useImmersiveMode) {
            final Window window = activity.getWindow();
            window.setNavigationBarColor(Color.TRANSPARENT);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    }

    public static TextView getHeader(Activity activity) {
        return (TextView) activity.findViewById(R.id.title);
    }

    public static void setHeaderText(Activity activity, int text) {
        getHeader(activity).setText(text);
    }

    public static void setHeaderText(Activity activity, CharSequence text) {
        getHeader(activity).setText(text);
    }

    public static void copySetupExtras(Intent fromIntent, Intent toIntent) {
        toIntent.putExtra(EXTRA_THEME, fromIntent.getStringExtra(EXTRA_THEME));
        toIntent.putExtra(EXTRA_USE_IMMERSIVE_MODE,
                fromIntent.getBooleanExtra(EXTRA_USE_IMMERSIVE_MODE, false));
    }

    public static void setIllustration(Activity activity, int asset) {
        SetupWizardIllustration illustration =
                (SetupWizardIllustration) activity.findViewById(R.id.setup_illustration);
        if (illustration != null) {
            Drawable drawable = activity.getDrawable(R.drawable.setup_illustration);
            Drawable newIllustration = activity.getDrawable(asset);
            if (drawable instanceof LayerDrawable) {
                LayerDrawable layers = (LayerDrawable) drawable;
                Drawable oldIllustration = layers.findDrawableByLayerId(R.id.illustration_image);
                if (newIllustration instanceof BitmapDrawable
                        && oldIllustration instanceof BitmapDrawable) {
                    final int gravity = ((BitmapDrawable) oldIllustration).getGravity();
                    ((BitmapDrawable) newIllustration).setGravity(gravity);
                }
                layers.setDrawableByLayerId(R.id.illustration_image, newIllustration);
                illustration.setForeground(layers);
            }
        }
    }
}
