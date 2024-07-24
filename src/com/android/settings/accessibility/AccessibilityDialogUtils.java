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
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.icu.text.MessageFormat;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ImageSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RawRes;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.android.server.accessibility.Flags;
import com.android.settings.R;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.utils.AnnotationSpan;
import com.android.settingslib.widget.LottieColorUtils;

import com.airbnb.lottie.LottieAnimationView;
import com.airbnb.lottie.LottieDrawable;

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

        /** OPEN: Settings > Accessibility > Any toggle service > Shortcut > Settings. */
        int EDIT_SHORTCUT = 1;

        /** OPEN: Settings > Accessibility > Magnification > Shortcut > Settings. */
        int MAGNIFICATION_EDIT_SHORTCUT = 1001;

        /**
         * OPEN: Settings > Accessibility > Downloaded toggle service > Toggle use service to
         * enable service.
         */
        int ENABLE_WARNING_FROM_TOGGLE = 1002;

        /** OPEN: Settings > Accessibility > Downloaded toggle service > Shortcut checkbox. */
        int ENABLE_WARNING_FROM_SHORTCUT = 1003;

        /**
         * OPEN: Settings > Accessibility > Downloaded toggle service > Shortcut checkbox
         * toggle.
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
     * IntDef enum for dialog type that indicates different dialog for user to choose the shortcut
     * type.
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
         DialogType.EDIT_SHORTCUT_GENERIC,
         DialogType.EDIT_SHORTCUT_GENERIC_SUW,
         DialogType.EDIT_SHORTCUT_MAGNIFICATION,
         DialogType.EDIT_SHORTCUT_MAGNIFICATION_SUW,
    })

    public @interface DialogType {
        int EDIT_SHORTCUT_GENERIC = 0;
        int EDIT_SHORTCUT_GENERIC_SUW = 1;
        int EDIT_SHORTCUT_MAGNIFICATION = 2;
        int EDIT_SHORTCUT_MAGNIFICATION_SUW = 3;
    }

    /**
     * Method to show the edit shortcut dialog.
     *
     * @param context A valid context
     * @param dialogType The type of edit shortcut dialog
     * @param dialogTitle The title of edit shortcut dialog
     * @param listener The listener to determine the action of edit shortcut dialog
     * @return A edit shortcut dialog for showing
     */
    public static AlertDialog showEditShortcutDialog(Context context, int dialogType,
            CharSequence dialogTitle, DialogInterface.OnClickListener listener) {
        final AlertDialog alertDialog = createDialog(context, dialogType, dialogTitle, listener);
        alertDialog.show();
        setScrollIndicators(alertDialog);
        return alertDialog;
    }

    /**
     * Updates the shortcut content in edit shortcut dialog.
     *
     * @param context A valid context
     * @param editShortcutDialog Need to be a type of edit shortcut dialog
     * @return True if the update is successful
     */
    public static boolean updateShortcutInDialog(Context context,
            Dialog editShortcutDialog) {
        final View container = editShortcutDialog.findViewById(R.id.container_layout);
        if (container != null) {
            initSoftwareShortcut(context, container);
            initHardwareShortcut(context, container);
            return true;
        }
        return false;
    }

    private static AlertDialog createDialog(Context context, int dialogType,
            CharSequence dialogTitle, DialogInterface.OnClickListener listener) {

        final AlertDialog alertDialog = new AlertDialog.Builder(context)
                .setView(createEditDialogContentView(context, dialogType))
                .setTitle(dialogTitle)
                .setPositiveButton(R.string.save, listener)
                .setNegativeButton(R.string.cancel,
                        (DialogInterface dialog, int which) -> dialog.dismiss())
                .create();

        return alertDialog;
    }

    /**
     * Sets the scroll indicators for dialog view. The indicators appears while content view is
     * out of vision for vertical scrolling.
     */
    private static void setScrollIndicators(AlertDialog dialog) {
        final ScrollView scrollView = dialog.findViewById(R.id.container_layout);
        setScrollIndicators(scrollView);
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
     * Get a content View for the edit shortcut dialog.
     *
     * @param context A valid context
     * @param dialogType The type of edit shortcut dialog
     * @return A content view suitable for viewing
     */
    private static View createEditDialogContentView(Context context, int dialogType) {
        final LayoutInflater inflater = (LayoutInflater) context.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);

        View contentView = null;

        switch (dialogType) {
            case DialogType.EDIT_SHORTCUT_GENERIC:
                contentView = inflater.inflate(
                        R.layout.accessibility_edit_shortcut, null);
                initSoftwareShortcut(context, contentView);
                initHardwareShortcut(context, contentView);
                break;
            case DialogType.EDIT_SHORTCUT_GENERIC_SUW:
                contentView = inflater.inflate(
                        R.layout.accessibility_edit_shortcut, null);
                initSoftwareShortcutForSUW(context, contentView);
                initHardwareShortcut(context, contentView);
                break;
            case DialogType.EDIT_SHORTCUT_MAGNIFICATION:
                contentView = inflater.inflate(
                        R.layout.accessibility_edit_shortcut_magnification, null);
                initSoftwareShortcut(context, contentView);
                initHardwareShortcut(context, contentView);
                if (Flags.enableMagnificationMultipleFingerMultipleTapGesture()) {
                    initTwoFingerDoubleTapMagnificationShortcut(context, contentView);
                }
                initMagnifyShortcut(context, contentView);
                initAdvancedWidget(contentView);
                break;
            case DialogType.EDIT_SHORTCUT_MAGNIFICATION_SUW:
                contentView = inflater.inflate(
                        R.layout.accessibility_edit_shortcut_magnification, null);
                initSoftwareShortcutForSUW(context, contentView);
                initHardwareShortcut(context, contentView);
                if (Flags.enableMagnificationMultipleFingerMultipleTapGesture()) {
                    initTwoFingerDoubleTapMagnificationShortcut(context, contentView);
                }
                initMagnifyShortcut(context, contentView);
                initAdvancedWidget(contentView);
                break;
            default:
                throw new IllegalArgumentException();
        }

        return contentView;
    }

    private static void setupShortcutWidget(View view, CharSequence titleText,
            CharSequence summaryText, @DrawableRes int imageResId) {
        setupShortcutWidgetWithTitleAndSummary(view, titleText, summaryText);
        setupShortcutWidgetWithImageResource(view, imageResId);
    }

    private static void setupShortcutWidgetWithImageRawResource(Context context,
            View view, CharSequence titleText,
            CharSequence summaryText, @RawRes int imageRawResId) {
        setupShortcutWidgetWithTitleAndSummary(view, titleText, summaryText);
        setupShortcutWidgetWithImageRawResource(context, view, imageRawResId);
    }

    private static void setupShortcutWidgetWithTitleAndSummary(View view, CharSequence titleText,
            CharSequence summaryText) {
        final CheckBox checkBox = view.findViewById(R.id.checkbox);
        checkBox.setText(titleText);

        final TextView summary = view.findViewById(R.id.summary);
        if (TextUtils.isEmpty(summaryText)) {
            summary.setVisibility(View.GONE);
        } else {
            summary.setText(summaryText);
            summary.setMovementMethod(LinkMovementMethod.getInstance());
            summary.setFocusable(false);
        }
    }

    private static void setupShortcutWidgetWithImageResource(View view,
            @DrawableRes int imageResId) {
        final ImageView imageView = view.findViewById(R.id.image);
        imageView.setImageResource(imageResId);
    }

    private static void setupShortcutWidgetWithImageRawResource(Context context, View view,
            @RawRes int imageRawResId) {
        final LottieAnimationView lottieView = view.findViewById(R.id.image);
        lottieView.setFailureListener(
                result -> Log.w(TAG, "Invalid image raw resource id: " + imageRawResId,
                        result));
        lottieView.setAnimation(imageRawResId);
        lottieView.setRepeatCount(LottieDrawable.INFINITE);
        LottieColorUtils.applyDynamicColors(context, lottieView);
        lottieView.playAnimation();
    }

    private static void initSoftwareShortcutForSUW(Context context, View view) {
        final View dialogView = view.findViewById(R.id.software_shortcut);
        final CharSequence title = context.getText(
                R.string.accessibility_shortcut_edit_dialog_title_software);
        final TextView summary = dialogView.findViewById(R.id.summary);
        final int lineHeight = summary.getLineHeight();

        setupShortcutWidget(dialogView, title,
                retrieveSoftwareShortcutSummaryForSUW(context, lineHeight),
                retrieveSoftwareShortcutImageResId(context));
    }

    private static void initSoftwareShortcut(Context context, View view) {
        final View dialogView = view.findViewById(R.id.software_shortcut);
        final TextView summary = dialogView.findViewById(R.id.summary);
        final int lineHeight = summary.getLineHeight();

        setupShortcutWidget(dialogView,
                retrieveTitle(context),
                retrieveSoftwareShortcutSummary(context, lineHeight),
                retrieveSoftwareShortcutImageResId(context));
    }

    private static void initHardwareShortcut(Context context, View view) {
        final View dialogView = view.findViewById(R.id.hardware_shortcut);
        final CharSequence title = context.getText(
                R.string.accessibility_shortcut_edit_dialog_title_hardware);
        final CharSequence summary = context.getText(
                R.string.accessibility_shortcut_edit_dialog_summary_hardware);
        setupShortcutWidget(dialogView, title, summary,
                R.drawable.a11y_shortcut_type_hardware);
    }

    private static void initMagnifyShortcut(Context context, View view) {
        final View dialogView = view.findViewById(R.id.triple_tap_shortcut);
        final CharSequence title = context.getText(
                R.string.accessibility_shortcut_edit_dialog_title_triple_tap);
        String summary = context.getString(
                R.string.accessibility_shortcut_edit_dialog_summary_triple_tap);
        // Format the number '3' in the summary.
        final Object[] arguments = {3};
        summary = MessageFormat.format(summary, arguments);

        setupShortcutWidgetWithImageRawResource(context, dialogView, title, summary,
                R.raw.a11y_shortcut_type_triple_tap);
    }

    private static void initTwoFingerDoubleTapMagnificationShortcut(Context context, View view) {
        // TODO(b/306153204): Update shortcut string and image when UX provides them
        final View dialogView = view.findViewById(R.id.two_finger_triple_tap_shortcut);
        final CharSequence title = context.getText(
                R.string.accessibility_shortcut_edit_dialog_title_two_finger_double_tap);
        String summary = context.getString(
                R.string.accessibility_shortcut_edit_dialog_summary_two_finger_double_tap);
        // Format the number '2' in the summary.
        final Object[] arguments = {2};
        summary = MessageFormat.format(summary, arguments);

        setupShortcutWidgetWithImageRawResource(context, dialogView, title, summary,
                R.raw.a11y_shortcut_type_triple_tap);

        dialogView.setVisibility(View.VISIBLE);
    }

    private static void initAdvancedWidget(View view) {
        final LinearLayout advanced = view.findViewById(R.id.advanced_shortcut);
        final View tripleTap = view.findViewById(R.id.triple_tap_shortcut);
        advanced.setOnClickListener((View v) -> {
            advanced.setVisibility(View.GONE);
            tripleTap.setVisibility(View.VISIBLE);
        });
    }

    private static CharSequence retrieveSoftwareShortcutSummaryForSUW(Context context,
            int lineHeight) {
        final SpannableStringBuilder sb = new SpannableStringBuilder();
        if (!AccessibilityUtil.isFloatingMenuEnabled(context)) {
            sb.append(getSummaryStringWithIcon(context, lineHeight));
        }
        return sb;
    }

    private static CharSequence retrieveTitle(Context context) {
        int resId;
        if (AccessibilityUtil.isFloatingMenuEnabled(context)) {
            resId = R.string.accessibility_shortcut_edit_dialog_title_software;
        } else if (AccessibilityUtil.isGestureNavigateEnabled(context)) {
            resId = R.string.accessibility_shortcut_edit_dialog_title_software_by_gesture;
        } else {
            resId = R.string.accessibility_shortcut_edit_dialog_title_software;
        }
        return context.getText(resId);
    }

    private static CharSequence retrieveSoftwareShortcutSummary(Context context, int lineHeight) {
        final SpannableStringBuilder sb = new SpannableStringBuilder();
        if (AccessibilityUtil.isFloatingMenuEnabled(context)) {
            sb.append(getCustomizeAccessibilityButtonLink(context));
        } else if (AccessibilityUtil.isGestureNavigateEnabled(context)) {
            final int resId = AccessibilityUtil.isTouchExploreEnabled(context)
                    ? R.string.accessibility_shortcut_edit_dialog_summary_software_gesture_talkback
                    : R.string.accessibility_shortcut_edit_dialog_summary_software_gesture;
            sb.append(context.getText(resId));
            sb.append("\n\n");
            sb.append(getCustomizeAccessibilityButtonLink(context));
        } else {
            sb.append(getSummaryStringWithIcon(context, lineHeight));
            sb.append("\n\n");
            sb.append(getCustomizeAccessibilityButtonLink(context));
        }
        return sb;
    }

    private static int retrieveSoftwareShortcutImageResId(Context context) {
        int resId;
        if (AccessibilityUtil.isFloatingMenuEnabled(context)) {
            resId = R.drawable.a11y_shortcut_type_software_floating;
        } else if (AccessibilityUtil.isGestureNavigateEnabled(context)) {
            resId = AccessibilityUtil.isTouchExploreEnabled(context)
                    ? R.drawable.a11y_shortcut_type_software_gesture_talkback
                    : R.drawable.a11y_shortcut_type_software_gesture;
        } else {
            resId = R.drawable.a11y_shortcut_type_software;
        }
        return resId;
    }

    private static CharSequence getCustomizeAccessibilityButtonLink(Context context) {
        final View.OnClickListener linkListener = v -> new SubSettingLauncher(context)
                .setDestination(AccessibilityButtonFragment.class.getName())
                .setSourceMetricsCategory(
                        SettingsEnums.SWITCH_SHORTCUT_DIALOG_ACCESSIBILITY_BUTTON_SETTINGS)
                .launch();
        final AnnotationSpan.LinkInfo linkInfo = new AnnotationSpan.LinkInfo(
                AnnotationSpan.LinkInfo.DEFAULT_ANNOTATION, linkListener);
        return AnnotationSpan.linkify(context.getText(
                R.string.accessibility_shortcut_edit_dialog_summary_software_floating), linkInfo);
    }

    private static SpannableString getSummaryStringWithIcon(Context context, int lineHeight) {
        final String summary = context
                .getString(R.string.accessibility_shortcut_edit_dialog_summary_software);
        final SpannableString spannableMessage = SpannableString.valueOf(summary);

        // Icon
        final int indexIconStart = summary.indexOf("%s");
        final int indexIconEnd = indexIconStart + 2;
        final Drawable icon = context.getDrawable(R.drawable.ic_accessibility_new);
        final ImageSpan imageSpan = new ImageSpan(icon);
        imageSpan.setContentDescription("");
        icon.setBounds(0, 0, lineHeight, lineHeight);
        spannableMessage.setSpan(
                imageSpan, indexIconStart, indexIconEnd,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return spannableMessage;
    }

    /**
     * Returns the color associated with the specified attribute in the context's theme.
     */
    @ColorInt
    private static int getThemeAttrColor(final Context context, final int attributeColor) {
        final int colorResId = getAttrResourceId(context, attributeColor);
        return ContextCompat.getColor(context, colorResId);
    }

    /**
     * Returns the identifier of the resolved resource assigned to the given attribute.
     */
    private static int getAttrResourceId(final Context context, final int attributeColor) {
        final int[] attrs = {attributeColor};
        final TypedArray typedArray = context.obtainStyledAttributes(attrs);
        final int colorResId = typedArray.getResourceId(0, 0);
        typedArray.recycle();
        return colorResId;
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
