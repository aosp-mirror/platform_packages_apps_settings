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

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextSwitcher;
import android.widget.TextView;

import androidx.annotation.AnimRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RawRes;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;
import androidx.core.util.Preconditions;
import androidx.core.widget.TextViewCompat;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.android.server.accessibility.Flags;
import com.android.settings.R;
import com.android.settings.core.SubSettingLauncher;
import com.android.settingslib.widget.LottieColorUtils;

import com.airbnb.lottie.LottieAnimationView;
import com.airbnb.lottie.LottieDrawable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for creating the dialog that guides users for gesture navigation for
 * accessibility services.
 */
public final class AccessibilityGestureNavigationTutorial {
    private static final String TAG = "AccessibilityGestureNavigationTutorial";

    /** IntDef enum for dialog type. */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
            DialogType.LAUNCH_SERVICE_BY_ACCESSIBILITY_BUTTON,
            DialogType.LAUNCH_SERVICE_BY_ACCESSIBILITY_GESTURE,
            DialogType.GESTURE_NAVIGATION_SETTINGS,
    })

    private @interface DialogType {
        int LAUNCH_SERVICE_BY_ACCESSIBILITY_BUTTON = 0;
        int LAUNCH_SERVICE_BY_ACCESSIBILITY_GESTURE = 1;
        int GESTURE_NAVIGATION_SETTINGS = 2;
    }

    private AccessibilityGestureNavigationTutorial() {}

    private static final DialogInterface.OnClickListener mOnClickListener =
            (DialogInterface dialog, int which) -> dialog.dismiss();

    /**
     * Displays a dialog that guides users to use accessibility features with accessibility
     * gestures under system gesture navigation mode.
     */
    public static AlertDialog showGestureNavigationTutorialDialog(Context context,
            DialogInterface.OnDismissListener onDismissListener) {
        final AlertDialog alertDialog = new AlertDialog.Builder(context)
                .setView(createTutorialDialogContentView(context,
                        DialogType.GESTURE_NAVIGATION_SETTINGS))
                .setPositiveButton(R.string.accessibility_tutorial_dialog_button, mOnClickListener)
                .setOnDismissListener(onDismissListener)
                .create();

        alertDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        alertDialog.setCanceledOnTouchOutside(false);
        alertDialog.show();

        return alertDialog;
    }

    static AlertDialog showAccessibilityGestureTutorialDialog(Context context) {
        return createDialog(context, DialogType.LAUNCH_SERVICE_BY_ACCESSIBILITY_GESTURE);
    }

    static AlertDialog createAccessibilityTutorialDialog(Context context, int shortcutTypes) {
        return createAccessibilityTutorialDialog(context, shortcutTypes, mOnClickListener);
    }

    static AlertDialog createAccessibilityTutorialDialog(Context context, int shortcutTypes,
            @Nullable DialogInterface.OnClickListener actionButtonListener) {

        final int category = SettingsEnums.SWITCH_SHORTCUT_DIALOG_ACCESSIBILITY_BUTTON_SETTINGS;
        final DialogInterface.OnClickListener linkButtonListener =
                (dialog, which) -> new SubSettingLauncher(context)
                        .setDestination(AccessibilityButtonFragment.class.getName())
                        .setSourceMetricsCategory(category)
                        .launch();

        final AlertDialog alertDialog = new AlertDialog.Builder(context)
                .setPositiveButton(R.string.accessibility_tutorial_dialog_button,
                        actionButtonListener)
                .setNegativeButton(R.string.accessibility_tutorial_dialog_link_button,
                        linkButtonListener)
                .create();

        final List<TutorialPage> tutorialPages =
                createShortcutTutorialPages(context, shortcutTypes);
        Preconditions.checkArgument(!tutorialPages.isEmpty(),
                /* errorMessage= */ "Unexpected tutorial pages size");

        final TutorialPageChangeListener.OnPageSelectedCallback callback = index -> {
            final int pageType = tutorialPages.get(index).getType();
            alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE).setVisibility(
                    pageType == UserShortcutType.SOFTWARE ? VISIBLE : GONE);
        };

        alertDialog.setView(createShortcutNavigationContentView(context, tutorialPages, callback));

        // Showing first page won't invoke onPageSelectedCallback. Need to check the first tutorial
        // page type manually to set correct visibility of the link button.
        alertDialog.setOnShowListener(dialog -> {
            final int firstPageType = tutorialPages.get(0).getType();
            alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE).setVisibility(
                    firstPageType == UserShortcutType.SOFTWARE ? VISIBLE : GONE);
        });

        return alertDialog;
    }

    static AlertDialog createAccessibilityTutorialDialogForSetupWizard(Context context,
            int shortcutTypes) {
        return createAccessibilityTutorialDialogForSetupWizard(context, shortcutTypes,
                mOnClickListener);
    }

    static AlertDialog createAccessibilityTutorialDialogForSetupWizard(Context context,
            int shortcutTypes, @Nullable DialogInterface.OnClickListener actionButtonListener) {

        final AlertDialog alertDialog = new AlertDialog.Builder(context)
                .setPositiveButton(R.string.accessibility_tutorial_dialog_button,
                        actionButtonListener)
                .create();

        final List<TutorialPage> tutorialPages =
                createShortcutTutorialPages(context, shortcutTypes);
        Preconditions.checkArgument(!tutorialPages.isEmpty(),
                /* errorMessage= */ "Unexpected tutorial pages size");

        alertDialog.setView(createShortcutNavigationContentView(context, tutorialPages, null));

        return alertDialog;
    }

    /**
     * Gets a content View for a dialog to confirm that they want to enable a service.
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
            case DialogType.LAUNCH_SERVICE_BY_ACCESSIBILITY_GESTURE:
                content = inflater.inflate(
                        R.layout.tutorial_dialog_launch_service_by_gesture_navigation, null);
                setupGestureNavigationTextWithImage(context, content);
                break;
            case DialogType.GESTURE_NAVIGATION_SETTINGS:
                content = inflater.inflate(
                        R.layout.tutorial_dialog_launch_by_gesture_navigation_settings, null);
                setupGestureNavigationTextWithImage(context, content);
                break;
        }

        return content;
    }

    private static void setupGestureNavigationTextWithImage(Context context, View view) {
        final boolean isTouchExploreEnabled = AccessibilityUtil.isTouchExploreEnabled(context);

        final ImageView imageView = view.findViewById(R.id.image);
        final int gestureSettingsImageResId =
                isTouchExploreEnabled ? R.drawable.a11y_gesture_navigation_three_finger_preview
                        : R.drawable.a11y_gesture_navigation_two_finger_preview;
        imageView.setImageResource(gestureSettingsImageResId);

        final TextView textView = view.findViewById(R.id.gesture_tutorial_message);
        textView.setText(isTouchExploreEnabled
                ? R.string.accessibility_tutorial_dialog_message_gesture_settings_talkback
                : R.string.accessibility_tutorial_dialog_message_gesture_settings);
    }

    private static AlertDialog createDialog(Context context, int dialogType) {
        final AlertDialog alertDialog = new AlertDialog.Builder(context)
                .setView(createTutorialDialogContentView(context, dialogType))
                .setPositiveButton(R.string.accessibility_tutorial_dialog_button, mOnClickListener)
                .create();

        alertDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        alertDialog.setCanceledOnTouchOutside(false);
        alertDialog.show();

        return alertDialog;
    }

    private static class TutorialPagerAdapter extends PagerAdapter {
        private final List<TutorialPage> mTutorialPages;
        private TutorialPagerAdapter(List<TutorialPage> tutorialPages) {
            this.mTutorialPages = tutorialPages;
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
            final View itemView = mTutorialPages.get(position).getIllustrationView();
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
            final View itemView = mTutorialPages.get(position).getIllustrationView();
            container.removeView(itemView);
        }
    }

    private static ImageView createImageView(Context context, int imageRes) {
        final ImageView imageView = new ImageView(context);
        imageView.setImageResource(imageRes);
        imageView.setAdjustViewBounds(true);

        return imageView;
    }

    private static View createIllustrationView(Context context, @DrawableRes int imageRes) {
        final View illustrationFrame = inflateAndInitIllustrationFrame(context);
        final LottieAnimationView lottieView = illustrationFrame.findViewById(R.id.image);
        lottieView.setImageResource(imageRes);

        return illustrationFrame;
    }

    private static View createIllustrationViewWithImageRawResource(Context context,
            @RawRes int imageRawRes) {
        final View illustrationFrame = inflateAndInitIllustrationFrame(context);
        final LottieAnimationView lottieView = illustrationFrame.findViewById(R.id.image);
        lottieView.setFailureListener(
                result -> Log.w(TAG, "Invalid image raw resource id: " + imageRawRes,
                        result));
        lottieView.setAnimation(imageRawRes);
        lottieView.setRepeatCount(LottieDrawable.INFINITE);
        LottieColorUtils.applyDynamicColors(context, lottieView);
        lottieView.playAnimation();

        return illustrationFrame;
    }

    private static View inflateAndInitIllustrationFrame(Context context) {
        final LayoutInflater inflater = context.getSystemService(LayoutInflater.class);

        return inflater.inflate(R.layout.accessibility_lottie_animation_view, /* root= */ null);
    }

    private static View createShortcutNavigationContentView(Context context,
            List<TutorialPage> tutorialPages,
            TutorialPageChangeListener.OnPageSelectedCallback onPageSelectedCallback) {

        final LayoutInflater inflater = context.getSystemService(LayoutInflater.class);
        final View contentView = inflater.inflate(
                R.layout.accessibility_shortcut_tutorial_dialog, /* root= */ null);

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

        TutorialPageChangeListener listener = new TutorialPageChangeListener(context, viewPager,
                title, instruction, tutorialPages);
        listener.setOnPageSelectedCallback(onPageSelectedCallback);

        return contentView;
    }

    private static View makeTitleView(Context context) {
        final TextView textView = new TextView(context);
        // Sets the text color, size, style, hint color, and highlight color from the specified
        // TextAppearance resource.
        TextViewCompat.setTextAppearance(textView, R.style.AccessibilityDialogTitle);
        textView.setGravity(Gravity.CENTER);
        return textView;
    }

    private static View makeInstructionView(Context context) {
        final TextView textView = new TextView(context);
        TextViewCompat.setTextAppearance(textView, R.style.AccessibilityDialogDescription);
        return textView;
    }

    private static TutorialPage createSoftwareTutorialPage(@NonNull Context context) {
        final int type = UserShortcutType.SOFTWARE;
        final CharSequence title = getSoftwareTitle(context);
        final View image = createSoftwareImage(context);
        final CharSequence instruction = getSoftwareInstruction(context);
        final ImageView indicatorIcon =
                createImageView(context, R.drawable.ic_accessibility_page_indicator);
        indicatorIcon.setEnabled(false);

        return new TutorialPage(type, title, image, indicatorIcon, instruction);
    }

    private static TutorialPage createHardwareTutorialPage(@NonNull Context context) {
        final int type = UserShortcutType.HARDWARE;
        final CharSequence title =
                context.getText(R.string.accessibility_tutorial_dialog_title_volume);
        final View image =
                createIllustrationView(context, R.drawable.a11y_shortcut_type_hardware);
        final ImageView indicatorIcon =
                createImageView(context, R.drawable.ic_accessibility_page_indicator);
        final CharSequence instruction =
                context.getText(R.string.accessibility_tutorial_dialog_message_volume);
        indicatorIcon.setEnabled(false);

        return new TutorialPage(type, title, image, indicatorIcon, instruction);
    }

    private static TutorialPage createTripleTapTutorialPage(@NonNull Context context) {
        final int type = UserShortcutType.TRIPLETAP;
        final CharSequence title =
                context.getText(R.string.accessibility_tutorial_dialog_title_triple);
        final View image =
                createIllustrationViewWithImageRawResource(context,
                        R.raw.a11y_shortcut_type_triple_tap);
        final CharSequence instruction =
                context.getText(R.string.accessibility_tutorial_dialog_message_triple);
        final ImageView indicatorIcon =
                createImageView(context, R.drawable.ic_accessibility_page_indicator);
        indicatorIcon.setEnabled(false);

        return new TutorialPage(type, title, image, indicatorIcon, instruction);
    }

    private static TutorialPage createTwoFingerTripleTapTutorialPage(@NonNull Context context) {
        // TODO(b/308088945): Update tutorial string and image when UX provides them
        final int type = UserShortcutType.TWOFINGERTRIPLETAP;
        final CharSequence title =
                context.getText(R.string.accessibility_tutorial_dialog_title_two_finger_double);
        final View image =
                createIllustrationViewWithImageRawResource(context,
                        R.raw.a11y_shortcut_type_triple_tap);
        final CharSequence instruction =
                context.getText(R.string.accessibility_tutorial_dialog_message_two_finger_triple);
        final ImageView indicatorIcon =
                createImageView(context, R.drawable.ic_accessibility_page_indicator);
        indicatorIcon.setEnabled(false);

        return new TutorialPage(type, title, image, indicatorIcon, instruction);
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

        if (Flags.enableMagnificationMultipleFingerMultipleTapGesture()) {
            if ((shortcutTypes & UserShortcutType.TWOFINGERTRIPLETAP)
                    == UserShortcutType.TWOFINGERTRIPLETAP) {
                tutorialPages.add(createTwoFingerTripleTapTutorialPage(context));
            }
        }

        return tutorialPages;
    }

    private static View createSoftwareImage(Context context) {
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
        return createIllustrationView(context, resId);
    }

    private static CharSequence getSoftwareTitle(Context context) {
        int resId;
        if (AccessibilityUtil.isFloatingMenuEnabled(context)) {
            resId = R.string.accessibility_tutorial_dialog_title_button;
        } else if (AccessibilityUtil.isGestureNavigateEnabled(context)) {
            resId = R.string.accessibility_tutorial_dialog_title_gesture;
        } else {
            resId = R.string.accessibility_tutorial_dialog_title_button;
        }
        return context.getText(resId);
    }

    private static CharSequence getSoftwareInstruction(Context context) {
        final SpannableStringBuilder sb = new SpannableStringBuilder();
        if (AccessibilityUtil.isFloatingMenuEnabled(context)) {
            final int resId = R.string.accessibility_tutorial_dialog_message_floating_button;
            sb.append(context.getText(resId));
        } else if (AccessibilityUtil.isGestureNavigateEnabled(context)) {
            final int resId = AccessibilityUtil.isTouchExploreEnabled(context)
                    ? R.string.accessibility_tutorial_dialog_message_gesture_talkback
                    : R.string.accessibility_tutorial_dialog_message_gesture;
            sb.append(context.getText(resId));
        } else {
            final int resId = R.string.accessibility_tutorial_dialog_message_button;
            sb.append(getSoftwareInstructionWithIcon(context, context.getText(resId)));
        }
        return sb;
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
        private final int mType;
        private final CharSequence mTitle;
        private final View mIllustrationView;
        private final ImageView mIndicatorIcon;
        private final CharSequence mInstruction;

        TutorialPage(int type, CharSequence title, View illustrationView, ImageView indicatorIcon,
                CharSequence instruction) {
            this.mType = type;
            this.mTitle = title;
            this.mIllustrationView = illustrationView;
            this.mIndicatorIcon = indicatorIcon;
            this.mInstruction = instruction;

            setupIllustrationChildViewsGravity();
        }

        public int getType() {
            return mType;
        }

        public CharSequence getTitle() {
            return mTitle;
        }

        public View getIllustrationView() {
            return mIllustrationView;
        }

        public ImageView getIndicatorIcon() {
            return mIndicatorIcon;
        }

        public CharSequence getInstruction() {
            return mInstruction;
        }

        private void setupIllustrationChildViewsGravity() {
            final View backgroundView = mIllustrationView.findViewById(R.id.image_background);
            initViewGravity(backgroundView);

            final View lottieView = mIllustrationView.findViewById(R.id.image);
            initViewGravity(lottieView);
        }

        private void initViewGravity(@NonNull View view) {
            final FrameLayout.LayoutParams layoutParams =
                    new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT,
                            FrameLayout.LayoutParams.WRAP_CONTENT);
            layoutParams.gravity = Gravity.CENTER;

            view.setLayoutParams(layoutParams);
        }
    }

    private static class TutorialPageChangeListener implements ViewPager.OnPageChangeListener {
        private int mLastTutorialPagePosition = 0;
        private final Context mContext;
        private final TextSwitcher mTitle;
        private final TextSwitcher mInstruction;
        private final List<TutorialPage> mTutorialPages;
        private final ViewPager mViewPager;
        private OnPageSelectedCallback mOnPageSelectedCallback;

        TutorialPageChangeListener(Context context, ViewPager viewPager, ViewGroup title,
                ViewGroup instruction, List<TutorialPage> tutorialPages) {
            this.mContext = context;
            this.mViewPager = viewPager;
            this.mTitle = (TextSwitcher) title;
            this.mInstruction = (TextSwitcher) instruction;
            this.mTutorialPages = tutorialPages;
            this.mOnPageSelectedCallback = null;

            this.mViewPager.addOnPageChangeListener(this);
        }

        public void setOnPageSelectedCallback(
                OnPageSelectedCallback callback) {
            this.mOnPageSelectedCallback = callback;
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

            if (mOnPageSelectedCallback != null) {
                mOnPageSelectedCallback.onPageSelected(position);
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            // Do nothing.
        }

        /** The interface that provides a callback method after tutorial page is selected. */
        private interface OnPageSelectedCallback {

            /** The callback method after tutorial page is selected. */
            void onPageSelected(int index);
        }
    }
}
