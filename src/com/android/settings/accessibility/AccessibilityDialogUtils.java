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

package com.android.settings.accessibility;

import static com.android.settings.accessibility.ItemInfoArrayAdapter.ItemInfo;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ScrollView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

/**
 * Utility class for creating the edit dialog.
 */
public class AccessibilityDialogUtils {
    private static final String TAG = "AccessibilityDialogUtils";

    /** Denotes the dialog emuns for show dialog. */
    @Retention(RetentionPolicy.SOURCE)
    public @interface DialogEnums {
        /**
         * OPEN: Settings > Accessibility > Downloaded toggle service > Toggle use service to
         * enable service.
         */
        int ENABLE_WARNING_FROM_TOGGLE = 1002;

        /**
         * OPEN: Settings > Accessibility > Downloaded toggle service > Shortcut options
         * settings.
         */
        int ENABLE_WARNING_FROM_SHORTCUT = 1003;

        /**
         * OPEN: Settings > Accessibility > Downloaded toggle service > Shortcut toggle
         */
        int ENABLE_WARNING_FROM_SHORTCUT_TOGGLE = 1004;

        /**
         * OPEN: Settings > Accessibility > Downloaded toggle service > Toggle use service to
         * disable service.
         */
        int DISABLE_WARNING_FROM_TOGGLE = 1005;

        /**
         * OPEN: Settings > Accessibility > Magnification > Toggle user service in button
         * navigation.
         */
        int ACCESSIBILITY_BUTTON_TUTORIAL = 1006;

        /**
         * OPEN: Settings > Accessibility > Magnification > Toggle user service in gesture
         * navigation.
         */
        int GESTURE_NAVIGATION_TUTORIAL = 1007;

        /**
         * OPEN: Settings > Accessibility > Downloaded toggle service > Toggle user service > Show
         * launch tutorial.
         */
        int LAUNCH_ACCESSIBILITY_TUTORIAL = 1008;

        /**
         * OPEN: Settings > Accessibility > Display size and text > Click 'Reset settings' button.
         */
        int DIALOG_RESET_SETTINGS = 1009;
    }

    /**
     * Sets the scroll indicators for dialog view. The indicators appear while content view is
     * out of vision for vertical scrolling.
     *
     * @param view The view contains customized dialog content. Usually it is {@link ScrollView} or
     *             {@link AbsListView}
     */
    private static void setScrollIndicators(@NonNull View view) {
        view.setScrollIndicators(
                View.SCROLL_INDICATOR_TOP | View.SCROLL_INDICATOR_BOTTOM,
                View.SCROLL_INDICATOR_TOP | View.SCROLL_INDICATOR_BOTTOM);
    }

    /**
     * Creates a dialog with the given view.
     *
     * @param context A valid context
     * @param dialogTitle The title of the dialog
     * @param customView The customized view
     * @param positiveButtonText The text of the positive button
     * @param positiveListener This listener will be invoked when the positive button in the dialog
     *                         is clicked
     * @param negativeButtonText The text of the negative button
     * @param negativeListener This listener will be invoked when the negative button in the dialog
     *                         is clicked
     * @return the {@link Dialog} with the given view
     */
    public static Dialog createCustomDialog(Context context, CharSequence dialogTitle,
            View customView, CharSequence positiveButtonText,
            DialogInterface.OnClickListener positiveListener, CharSequence negativeButtonText,
            DialogInterface.OnClickListener negativeListener) {
        final AlertDialog alertDialog = new AlertDialog.Builder(context)
                .setView(customView)
                .setTitle(dialogTitle)
                .setCancelable(true)
                .setPositiveButton(positiveButtonText, positiveListener)
                .setNegativeButton(negativeButtonText, negativeListener)
                .create();
        if (customView instanceof ScrollView || customView instanceof AbsListView) {
            setScrollIndicators(customView);
        }
        return alertDialog;
    }

    /**
     * Creates a single choice {@link ListView} with given {@link ItemInfo} list.
     *
     * @param context A context.
     * @param itemInfoList A {@link ItemInfo} list.
     * @param itemListener The listener will be invoked when the item is clicked.
     */
    @NonNull
    public static ListView createSingleChoiceListView(@NonNull Context context,
            @NonNull List<? extends ItemInfo> itemInfoList,
            @Nullable AdapterView.OnItemClickListener itemListener) {
        final ListView list = new ListView(context);
        // Set an id to save its state.
        list.setId(android.R.id.list);
        list.setDivider(/* divider= */ null);
        list.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        final ItemInfoArrayAdapter
                adapter = new ItemInfoArrayAdapter(context, itemInfoList);
        list.setAdapter(adapter);
        list.setOnItemClickListener(itemListener);
        return list;
    }
}
