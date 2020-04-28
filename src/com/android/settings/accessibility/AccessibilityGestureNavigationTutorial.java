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
 * limitations under the License
 */

package com.android.settings.accessibility;

import static android.view.WindowManagerPolicyConstants.NAV_BAR_MODE_GESTURAL;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.Window;
import android.view.accessibility.AccessibilityManager;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.IntDef;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.android.settings.R;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Utility class for creating the dialog that guides users for gesture navigation for
 * accessibility services.
 */
public class AccessibilityGestureNavigationTutorial {

    /** IntDef enum for dialog type. */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
            DialogType.LAUNCH_SERVICE_BY_ACCESSIBILITY_BUTTON,
            DialogType.LAUNCH_SERVICE_BY_GESTURE_NAVIGATION,
            DialogType.GESTURE_NAVIGATION_SETTINGS,
    })

    private @interface DialogType {
        int LAUNCH_SERVICE_BY_ACCESSIBILITY_BUTTON = 0;
        int LAUNCH_SERVICE_BY_GESTURE_NAVIGATION = 1;
        int GESTURE_NAVIGATION_SETTINGS = 2;
    }

    private static final DialogInterface.OnClickListener mOnClickListener =
            (DialogInterface dialog, int which) -> dialog.dismiss();

    public static void showGestureNavigationSettingsTutorialDialog(Context context,
            DialogInterface.OnDismissListener dismissListener) {
        final AlertDialog alertDialog = new AlertDialog.Builder(context)
                .setView(createTutorialDialogContentView(context,
                        DialogType.GESTURE_NAVIGATION_SETTINGS))
                .setNegativeButton(R.string.accessibility_tutorial_dialog_button, mOnClickListener)
                .setOnDismissListener(dismissListener)
                .create();

        alertDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        alertDialog.setCanceledOnTouchOutside(false);
        alertDialog.show();
    }

    static AlertDialog showAccessibilityButtonTutorialDialog(Context context) {
        final AlertDialog alertDialog = createDialog(context,
                DialogType.LAUNCH_SERVICE_BY_ACCESSIBILITY_BUTTON);

        if (!isGestureNavigateEnabled(context)) {
            updateMessageWithIcon(context, alertDialog);
        }

        return alertDialog;
    }

    static AlertDialog showGestureNavigationTutorialDialog(Context context) {
        return createDialog(context, DialogType.LAUNCH_SERVICE_BY_GESTURE_NAVIGATION);
    }

    /**
     * Get a content View for a dialog to confirm that they want to enable a service.
     *
     * @param context    A valid context
     * @param dialogType The type of tutorial dialog
     * @return A content view suitable for viewing
     */
    private static View createTutorialDialogContentView(Context context, int dialogType) {
        final LayoutInflater inflater = (LayoutInflater) context.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);

        View content = null;

        switch (dialogType) {
            case DialogType.LAUNCH_SERVICE_BY_ACCESSIBILITY_BUTTON:
                content = inflater.inflate(
                        R.layout.tutorial_dialog_launch_service_by_accessibility_button, null);
                break;
            case DialogType.LAUNCH_SERVICE_BY_GESTURE_NAVIGATION:
                content = inflater.inflate(
                        R.layout.tutorial_dialog_launch_service_by_gesture_navigation, null);
                final TextureView gestureTutorialVideo = content.findViewById(
                        R.id.gesture_tutorial_video);
                final TextView gestureTutorialMessage = content.findViewById(
                        R.id.gesture_tutorial_message);
                VideoPlayer.create(context, isTouchExploreOn(context)
                                ? R.raw.illustration_accessibility_gesture_three_finger
                                : R.raw.illustration_accessibility_gesture_two_finger,
                        gestureTutorialVideo);
                gestureTutorialMessage.setText(isTouchExploreOn(context)
                        ? R.string.accessibility_tutorial_dialog_message_gesture_with_talkback
                        : R.string.accessibility_tutorial_dialog_message_gesture_without_talkback);
                break;
            case DialogType.GESTURE_NAVIGATION_SETTINGS:
                content = inflater.inflate(
                        R.layout.tutorial_dialog_launch_by_gesture_navigation_settings, null);
                final TextureView gestureSettingsTutorialVideo = content.findViewById(
                        R.id.gesture_tutorial_video);
                final TextView gestureSettingsTutorialMessage = content.findViewById(
                        R.id.gesture_tutorial_message);
                VideoPlayer.create(context, isTouchExploreOn(context)
                                ? R.raw.illustration_accessibility_gesture_three_finger
                                : R.raw.illustration_accessibility_gesture_two_finger,
                        gestureSettingsTutorialVideo);
                gestureSettingsTutorialMessage.setText(isTouchExploreOn(context)
                        ?
                        R.string.accessibility_tutorial_dialog_message_gesture_settings_with_talkback
                        : R.string.accessibility_tutorial_dialog_message_gesture_settings_without_talkback);
                break;
        }

        return content;
    }

    private static AlertDialog createDialog(Context context, int dialogType) {
        final AlertDialog alertDialog = new AlertDialog.Builder(context)
                .setView(createTutorialDialogContentView(context, dialogType))
                .setNegativeButton(R.string.accessibility_tutorial_dialog_button, mOnClickListener)
                .create();

        alertDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        alertDialog.setCanceledOnTouchOutside(false);
        alertDialog.show();

        return alertDialog;
    }

    private static void updateMessageWithIcon(Context context, AlertDialog alertDialog) {
        final TextView gestureTutorialMessage = alertDialog.findViewById(
                R.id.button_tutorial_message);

        // Get the textView line height to update [icon] size. Must be called after show()
        final int lineHeight = gestureTutorialMessage.getLineHeight();
        gestureTutorialMessage.setText(getMessageStringWithIcon(context, lineHeight));
    }

    private static SpannableString getMessageStringWithIcon(Context context, int lineHeight) {
        final String messageString = context
                .getString(R.string.accessibility_tutorial_dialog_message_button);
        final SpannableString spannableMessage = SpannableString.valueOf(messageString);

        // Icon
        final int indexIconStart = messageString.indexOf("%s");
        final int indexIconEnd = indexIconStart + 2;
        final Drawable icon = context.getDrawable(R.drawable.ic_accessibility_new);
        icon.setTint(getThemeAttrColor(context, android.R.attr.textColorPrimary));
        icon.setBounds(0, 0, lineHeight, lineHeight);
        spannableMessage.setSpan(
                new ImageSpan(icon), indexIconStart, indexIconEnd,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        return spannableMessage;
    }

    /** Returns the color associated with the specified attribute in the context's theme. */
    @ColorInt
    private static int getThemeAttrColor(final Context context, final int attributeColor) {
        final int colorResId = getAttrResourceId(context, attributeColor);
        return ContextCompat.getColor(context, colorResId);
    }

    /** Returns the identifier of the resolved resource assigned to the given attribute. */
    private static int getAttrResourceId(final Context context, final int attributeColor) {
        final int[] attrs = {attributeColor};
        final TypedArray typedArray = context.obtainStyledAttributes(attrs);
        final int colorResId = typedArray.getResourceId(0, 0);
        typedArray.recycle();
        return colorResId;
    }

    private static boolean isGestureNavigateEnabled(Context context) {
        return context.getResources().getInteger(
                com.android.internal.R.integer.config_navBarInteractionMode)
                == NAV_BAR_MODE_GESTURAL;
    }

    private static boolean isTouchExploreOn(Context context) {
        return ((AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE))
                .isTouchExplorationEnabled();
    }
}