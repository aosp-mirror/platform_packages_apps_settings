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

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import static com.android.settings.accessibility.AccessibilityUtil.UserShortcutType;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextSwitcher;
import android.widget.TextView;

import androidx.annotation.AnimRes;
import androidx.annotation.ColorInt;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.util.Preconditions;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.android.settings.R;
import com.android.settings.Utils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for creating the dialog that guides users for gesture navigation for
 * accessibility services.
 */
public final class AccessibilityGestureNavigationTutorial {
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

    private AccessibilityGestureNavigationTutorial() {}

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

        if (!AccessibilityUtil.isGestureNavigateEnabled(context)) {
            updateMessageWithIcon(context, alertDialog);
        }

        return alertDialog;
    }

    static AlertDialog showGestureNavigationTutorialDialog(Context context) {
        return createDialog(context, DialogType.LAUNCH_SERVICE_BY_GESTURE_NAVIGATION);
    }

    static AlertDialog createAccessibilityTutorialDialog(Context context, int shortcutTypes) {
        return new AlertDialog.Builder(context)
                .setView(createShortcutNavigationContentView(context, shortcutTypes))
                .setNegativeButton(R.string.accessibility_tutorial_dialog_button, mOnClickListener)
                .create();
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
                VideoPlayer.create(context, AccessibilityUtil.isTouchExploreEnabled(context)
                                ? R.raw.illustration_accessibility_gesture_three_finger
                                : R.raw.illustration_accessibility_gesture_two_finger,
                        gestureTutorialVideo);
                gestureTutorialMessage.setText(AccessibilityUtil.isTouchExploreEnabled(context)
                        ? R.string.accessibility_tutorial_dialog_message_gesture_talkback
                        : R.string.accessibility_tutorial_dialog_message_gesture);
                break;
            case DialogType.GESTURE_NAVIGATION_SETTINGS:
                content = inflater.inflate(
                        R.layout.tutorial_dialog_launch_by_gesture_navigation_settings, null);
                final TextureView gestureSettingsTutorialVideo = content.findViewById(
                        R.id.gesture_tutorial_video);
                final TextView gestureSettingsTutorialMessage = content.findViewById(
                        R.id.gesture_tutorial_message);
                VideoPlayer.create(context, AccessibilityUtil.isTouchExploreEnabled(context)
                                ? R.raw.illustration_accessibility_gesture_three_finger
                                : R.raw.illustration_accessibility_gesture_two_finger,
                        gestureSettingsTutorialVideo);
                final int stringResId = AccessibilityUtil.isTouchExploreEnabled(context)
                        ? R.string.accessibility_tutorial_dialog_message_gesture_settings_talkback
                        : R.string.accessibility_tutorial_dialog_message_gesture_settings;
                gestureSettingsTutorialMessage.setText(stringResId);
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
        final ImageSpan imageSpan = new ImageSpan(icon);
        imageSpan.setContentDescription("");
        icon.setBounds(0, 0, lineHeight, lineHeight);
        spannableMessage.setSpan(
                imageSpan, indexIconStart, indexIconEnd,
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

    private static class TutorialPagerAdapter extends PagerAdapter {
        private final List<TutorialPage> mTutorialPages;
        private TutorialPagerAdapter(List<TutorialPage> tutorialPages) {
            this.mTutorialPages = tutorialPages;
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
            final View itemView = mTutorialPages.get(position).getImageView();
            container.addView(itemView);
            return itemView;
        }

        @Override
        public int getCount() {
            return mTutorialPages.size();
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object o) {
            return view == o;
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position,
                @NonNull Object object) {
            final View itemView = mTutorialPages.get(position).getImageView();
            container.removeView(itemView);
        }
    }

    private static ImageView createImageView(Context context, int imageRes) {
        final ImageView imageView = new ImageView(context);
        imageView.setImageResource(imageRes);
        imageView.setAdjustViewBounds(true);

        return imageView;
    }

    private static View createShortcutNavigationContentView(Context context, int shortcutTypes) {
        final LayoutInflater inflater = context.getSystemService(LayoutInflater.class);
        final View contentView = inflater.inflate(
                R.layout.accessibility_shortcut_tutorial_dialog, /* root= */ null);
        final List<TutorialPage> tutorialPages =
                createShortcutTutorialPages(context, shortcutTypes);
        Preconditions.checkArgument(!tutorialPages.isEmpty(),
                /* errorMessage= */ "Unexpected tutorial pages size");

        final LinearLayout indicatorContainer = contentView.findViewById(R.id.indicator_container);
        indicatorContainer.setVisibility(tutorialPages.size() > 1 ? VISIBLE : GONE);
        for (TutorialPage page : tutorialPages) {
            indicatorContainer.addView(page.getIndicatorIcon());
        }
        tutorialPages.get(/* firstIndex */ 0).getIndicatorIcon().setEnabled(true);

        final TextSwitcher title = contentView.findViewById(R.id.title);
        title.setFactory(() -> makeTitleView(context));
        title.setText(tutorialPages.get(/* firstIndex */ 0).getTitle());

        final TextSwitcher instruction = contentView.findViewById(R.id.instruction);
        instruction.setFactory(() -> makeInstructionView(context));
        instruction.setText(tutorialPages.get(/* firstIndex */ 0).getInstruction());

        final ViewPager viewPager = contentView.findViewById(R.id.view_pager);
        viewPager.setAdapter(new TutorialPagerAdapter(tutorialPages));
        viewPager.setContentDescription(context.getString(R.string.accessibility_tutorial_pager,
                /* firstPage */ 1, tutorialPages.size()));
        viewPager.setImportantForAccessibility(tutorialPages.size() > 1
                ? View.IMPORTANT_FOR_ACCESSIBILITY_YES
                : View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        viewPager.addOnPageChangeListener(
                new TutorialPageChangeListener(context, viewPager, title, instruction,
                        tutorialPages));

        return contentView;
    }

    private static View makeTitleView(Context context) {
        final String familyName =
                context.getString(
                        com.android.internal.R.string.config_headlineFontFamilyMedium);
        final TextView textView = new TextView(context);

        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, /* size= */ 20);
        textView.setTextColor(Utils.getColorAttr(context, android.R.attr.textColorPrimary));
        textView.setGravity(Gravity.CENTER);
        textView.setTypeface(Typeface.create(familyName, Typeface.NORMAL));

        return textView;
    }

    private static View makeInstructionView(Context context) {
        final TextView textView = new TextView(context);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, /* size= */ 16);
        textView.setTextColor(Utils.getColorAttr(context, android.R.attr.textColorPrimary));
        textView.setTypeface(
                Typeface.create(/* familyName= */ "sans-serif", Typeface.NORMAL));
        return textView;
    }

    private static TutorialPage createSoftwareTutorialPage(@NonNull Context context) {
        final CharSequence title = getSoftwareTitle(context);
        final ImageView image = createSoftwareImage(context);
        final CharSequence instruction = getSoftwareInstruction(context);
        final ImageView indicatorIcon =
                createImageView(context, R.drawable.ic_accessibility_page_indicator);
        indicatorIcon.setEnabled(false);

        return new TutorialPage(title, image, indicatorIcon, instruction);
    }

    private static TutorialPage createHardwareTutorialPage(@NonNull Context context) {
        final CharSequence title =
                context.getText(R.string.accessibility_tutorial_dialog_title_volume);
        final ImageView image =
                createImageView(context, R.drawable.accessibility_shortcut_type_hardware);
        final ImageView indicatorIcon =
                createImageView(context, R.drawable.ic_accessibility_page_indicator);
        final CharSequence instruction =
                context.getText(R.string.accessibility_tutorial_dialog_message_volume);
        indicatorIcon.setEnabled(false);

        return new TutorialPage(title, image, indicatorIcon, instruction);
    }

    private static TutorialPage createTripleTapTutorialPage(@NonNull Context context) {
        final CharSequence title =
                context.getText(R.string.accessibility_tutorial_dialog_title_triple);
        final ImageView image =
                createImageView(context, R.drawable.accessibility_shortcut_type_triple_tap);
        final CharSequence instruction =
                context.getText(R.string.accessibility_tutorial_dialog_message_triple);
        final ImageView indicatorIcon =
                createImageView(context, R.drawable.ic_accessibility_page_indicator);
        indicatorIcon.setEnabled(false);

        return new TutorialPage(title, image, indicatorIcon, instruction);
    }

    @VisibleForTesting
    static List<TutorialPage> createShortcutTutorialPages(@NonNull Context context,
            int shortcutTypes) {
        final List<TutorialPage> tutorialPages = new ArrayList<>();
        if ((shortcutTypes & UserShortcutType.SOFTWARE) == UserShortcutType.SOFTWARE) {
            tutorialPages.add(createSoftwareTutorialPage(context));
        }

        if ((shortcutTypes & UserShortcutType.HARDWARE) == UserShortcutType.HARDWARE) {
            tutorialPages.add(createHardwareTutorialPage(context));
        }

        if ((shortcutTypes & UserShortcutType.TRIPLETAP) == UserShortcutType.TRIPLETAP) {
            tutorialPages.add(createTripleTapTutorialPage(context));
        }

        return tutorialPages;
    }

    private static CharSequence getSoftwareTitle(Context context) {
        final boolean isGestureNavigationEnabled =
                AccessibilityUtil.isGestureNavigateEnabled(context);
        final int resId = isGestureNavigationEnabled
                ? R.string.accessibility_tutorial_dialog_title_gesture
                : R.string.accessibility_tutorial_dialog_title_button;

        return context.getText(resId);
    }

    private static ImageView createSoftwareImage(Context context) {
        int resId = R.drawable.accessibility_shortcut_type_software;
        if (AccessibilityUtil.isGestureNavigateEnabled(context)) {
            resId = AccessibilityUtil.isTouchExploreEnabled(context)
                    ? R.drawable.accessibility_shortcut_type_software_gesture_talkback
                    : R.drawable.accessibility_shortcut_type_software_gesture;
        }

        return createImageView(context, resId);
    }

    private static CharSequence getSoftwareInstruction(Context context) {
        final boolean isGestureNavigateEnabled =
                AccessibilityUtil.isGestureNavigateEnabled(context);
        final boolean isTouchExploreEnabled = AccessibilityUtil.isTouchExploreEnabled(context);
        int resId = R.string.accessibility_tutorial_dialog_message_button;
        if (isGestureNavigateEnabled) {
            resId = isTouchExploreEnabled
                    ? R.string.accessibility_tutorial_dialog_message_gesture_talkback
                    : R.string.accessibility_tutorial_dialog_message_gesture;
        }

        CharSequence text = context.getText(resId);
        if (resId == R.string.accessibility_tutorial_dialog_message_button) {
            text = getSoftwareInstructionWithIcon(context, text);
        }

        return text;
    }

    private static CharSequence getSoftwareInstructionWithIcon(Context context, CharSequence text) {
        final String message = text.toString();
        final SpannableString spannableInstruction = SpannableString.valueOf(message);
        final int indexIconStart = message.indexOf("%s");
        final int indexIconEnd = indexIconStart + 2;
        final ImageView iconView = new ImageView(context);
        iconView.setImageDrawable(context.getDrawable(R.drawable.ic_accessibility_new));
        final Drawable icon = iconView.getDrawable().mutate();
        final ImageSpan imageSpan = new ImageSpan(icon);
        imageSpan.setContentDescription("");
        icon.setBounds(/* left= */ 0, /* top= */ 0,
                icon.getIntrinsicWidth(), icon.getIntrinsicHeight());
        spannableInstruction.setSpan(imageSpan, indexIconStart, indexIconEnd,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        return spannableInstruction;
    }

    private static class TutorialPage {
        private final CharSequence mTitle;
        private final ImageView mImageView;
        private final ImageView mIndicatorIcon;
        private final CharSequence mInstruction;

        TutorialPage(CharSequence title, ImageView imageView, ImageView indicatorIcon,
                CharSequence instruction) {
            this.mTitle = title;
            this.mImageView = imageView;
            this.mIndicatorIcon = indicatorIcon;
            this.mInstruction = instruction;
        }

        public CharSequence getTitle() {
            return mTitle;
        }

        public ImageView getImageView() {
            return mImageView;
        }

        public ImageView getIndicatorIcon() {
            return mIndicatorIcon;
        }

        public CharSequence getInstruction() {
            return mInstruction;
        }
    }

    private static class TutorialPageChangeListener implements ViewPager.OnPageChangeListener {
        private int mLastTutorialPagePosition = 0;
        private final Context mContext;
        private final TextSwitcher mTitle;
        private final TextSwitcher mInstruction;
        private final List<TutorialPage> mTutorialPages;
        private final ViewPager mViewPager;

        TutorialPageChangeListener(Context context, ViewPager viewPager, ViewGroup title,
                ViewGroup instruction, List<TutorialPage> tutorialPages) {
            this.mContext = context;
            this.mViewPager = viewPager;
            this.mTitle = (TextSwitcher) title;
            this.mInstruction = (TextSwitcher) instruction;
            this.mTutorialPages = tutorialPages;
        }

        @Override
        public void onPageScrolled(int position, float positionOffset,
                int positionOffsetPixels) {
            // Do nothing.
        }

        @Override
        public void onPageSelected(int position) {
            final boolean isPreviousPosition =
                    mLastTutorialPagePosition > position;
            @AnimRes
            final int inAnimationResId = isPreviousPosition
                    ? android.R.anim.slide_in_left
                    : com.android.internal.R.anim.slide_in_right;

            @AnimRes
            final int outAnimationResId = isPreviousPosition
                    ? android.R.anim.slide_out_right
                    : com.android.internal.R.anim.slide_out_left;

            mTitle.setInAnimation(mContext, inAnimationResId);
            mTitle.setOutAnimation(mContext, outAnimationResId);
            mTitle.setText(mTutorialPages.get(position).getTitle());

            mInstruction.setInAnimation(mContext, inAnimationResId);
            mInstruction.setOutAnimation(mContext, outAnimationResId);
            mInstruction.setText(mTutorialPages.get(position).getInstruction());

            for (TutorialPage page : mTutorialPages) {
                page.getIndicatorIcon().setEnabled(false);
            }
            mTutorialPages.get(position).getIndicatorIcon().setEnabled(true);
            mLastTutorialPagePosition = position;

            final int currentPageNumber = position + 1;
            mViewPager.setContentDescription(
                    mContext.getString(R.string.accessibility_tutorial_pager,
                            currentPageNumber, mTutorialPages.size()));
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            // Do nothing.
        }
    }
}
