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
import com.android.setupwizardlib.util.SystemBarHelper;
import com.android.setupwizardlib.util.WizardManagerHelper;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.widget.TextView;

public class SetupWizardUtils {
    private static final String TAG = "SetupWizardUtils";

    // From WizardManager (must match constants maintained there)
    public static final String EXTRA_SCRIPT_URI = "scriptUri";

    public static boolean isUsingWizardManager(Activity activity) {
        return activity.getIntent().hasExtra(EXTRA_SCRIPT_URI);
    }

    public static int getTheme(Intent intent) {
        if (WizardManagerHelper.isLightTheme(intent, true)) {
            return R.style.SetupWizardTheme_Light;
        } else {
            return R.style.SetupWizardTheme;
        }
    }

    /**
     * Sets the immersive mode related flags based on the extra in the intent which started the
     * activity.
     */
    public static void setImmersiveMode(Activity activity) {
        final boolean useImmersiveMode = activity.getIntent().getBooleanExtra(
                WizardManagerHelper.EXTRA_USE_IMMERSIVE_MODE, false);
        if (useImmersiveMode) {
            SystemBarHelper.hideSystemBars(activity.getWindow());
        }
    }

    public static void applyImmersiveFlags(final Dialog dialog) {
        SystemBarHelper.hideSystemBars(dialog);
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
        toIntent.putExtra(WizardManagerHelper.EXTRA_THEME,
                fromIntent.getStringExtra(WizardManagerHelper.EXTRA_THEME));
        toIntent.putExtra(WizardManagerHelper.EXTRA_USE_IMMERSIVE_MODE,
                fromIntent.getBooleanExtra(WizardManagerHelper.EXTRA_USE_IMMERSIVE_MODE, false));
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
