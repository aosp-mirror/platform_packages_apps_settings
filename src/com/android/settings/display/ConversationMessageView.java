/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.display;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.settings.R;

/**
 * The view for a single entry in a conversation. This is a simplified version of
 * com.android.messaging.ui.conversation.ConversationMessageView class.
 */
public class ConversationMessageView extends FrameLayout {
    private final boolean mIncoming;
    private final CharSequence mMessageText;
    private final CharSequence mTimestampText;
    private final CharSequence mIconText;
    private final int mIconTextColor;
    private final int mIconBackgroundColor;

    private LinearLayout mMessageBubble;
    private ViewGroup mMessageTextAndInfoView;
    private TextView mMessageTextView;
    private TextView mStatusTextView;
    private TextView mContactIconView;

    public ConversationMessageView(Context context) {
        this(context, null);
    }

    public ConversationMessageView(final Context context, final AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ConversationMessageView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public ConversationMessageView(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        final TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.ConversationMessageView);

        mIncoming = a.getBoolean(R.styleable.ConversationMessageView_incoming, true);
        mMessageText = a.getString(R.styleable.ConversationMessageView_messageText);
        mTimestampText = a.getString(R.styleable.ConversationMessageView_timestampText);
        mIconText = a.getString(R.styleable.ConversationMessageView_iconText);
        mIconTextColor = a.getColor(R.styleable.ConversationMessageView_iconTextColor, 0);
        mIconBackgroundColor = a.getColor(R.styleable.ConversationMessageView_iconBackgroundColor,
                0);
        a.recycle();
        LayoutInflater.from(context).inflate(R.layout.conversation_message_icon, this);
        LayoutInflater.from(context).inflate(R.layout.conversation_message_content, this);
    }

    @Override
    protected void onFinishInflate() {
        mMessageBubble = (LinearLayout) findViewById(R.id.message_content);
        mMessageTextAndInfoView = (ViewGroup) findViewById(R.id.message_text_and_info);
        mMessageTextView = (TextView) findViewById(R.id.message_text);
        mStatusTextView = (TextView) findViewById(R.id.message_status);
        mContactIconView = (TextView) findViewById(R.id.conversation_icon);
        updateViewContent();
    }

    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        updateViewAppearance();

        final int horizontalSpace = MeasureSpec.getSize(widthMeasureSpec);

        final int unspecifiedMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        int iconMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);

        mContactIconView.measure(iconMeasureSpec, iconMeasureSpec);
        iconMeasureSpec = MeasureSpec.makeMeasureSpec(
                Math.max(mContactIconView.getMeasuredWidth(), mContactIconView.getMeasuredHeight()),
                MeasureSpec.EXACTLY);
        mContactIconView.measure(iconMeasureSpec, iconMeasureSpec);

        final int arrowWidth =
                getResources().getDimensionPixelSize(R.dimen.message_bubble_arrow_width);

        // We need to subtract contact icon width twice from the horizontal space to get
        // the max leftover space because we want the message bubble to extend no further than the
        // starting position of the message bubble in the opposite direction.
        final int maxLeftoverSpace = horizontalSpace - mContactIconView.getMeasuredWidth() * 2
                - arrowWidth - getPaddingLeft() - getPaddingRight();
        final int messageContentWidthMeasureSpec = MeasureSpec.makeMeasureSpec(maxLeftoverSpace,
                MeasureSpec.AT_MOST);

        mMessageBubble.measure(messageContentWidthMeasureSpec, unspecifiedMeasureSpec);

        final int maxHeight = Math.max(mContactIconView.getMeasuredHeight(),
                mMessageBubble.getMeasuredHeight());
        setMeasuredDimension(horizontalSpace, maxHeight + getPaddingBottom() + getPaddingTop());
    }

    @Override
    protected void onLayout(final boolean changed, final int left, final int top, final int right,
            final int bottom) {
        final boolean isRtl = isLayoutRtl(this);

        final int iconWidth = mContactIconView.getMeasuredWidth();
        final int iconHeight = mContactIconView.getMeasuredHeight();
        final int iconTop = getPaddingTop();
        final int contentWidth = (right -left) - iconWidth - getPaddingLeft() - getPaddingRight();
        final int contentHeight = mMessageBubble.getMeasuredHeight();
        final int contentTop = iconTop;

        final int iconLeft;
        final int contentLeft;

        if (mIncoming) {
            if (isRtl) {
                iconLeft = (right - left) - getPaddingRight() - iconWidth;
                contentLeft = iconLeft - contentWidth;
            } else {
                iconLeft = getPaddingLeft();
                contentLeft = iconLeft + iconWidth;
            }
        } else {
            if (isRtl) {
                iconLeft = getPaddingLeft();
                contentLeft = iconLeft + iconWidth;
            } else {
                iconLeft = (right - left) - getPaddingRight() - iconWidth;
                contentLeft = iconLeft - contentWidth;
            }
        }

        mContactIconView.layout(iconLeft, iconTop, iconLeft + iconWidth, iconTop + iconHeight);

        mMessageBubble.layout(contentLeft, contentTop, contentLeft + contentWidth,
                contentTop + contentHeight);
    }

    private static boolean isLayoutRtl(final View view) {
        return View.LAYOUT_DIRECTION_RTL == view.getLayoutDirection();
    }

    private void updateViewContent() {
        mMessageTextView.setText(mMessageText);
        mStatusTextView.setText(mTimestampText);
        mContactIconView.setText(mIconText);

        mContactIconView.setTextColor(mIconTextColor);
        final Drawable iconBase = getContext().getDrawable(R.drawable.conversation_message_icon);
        mContactIconView
                .setBackground(getTintedDrawable(getContext(), iconBase, mIconBackgroundColor));
    }

    private void updateViewAppearance() {
        final Resources res = getResources();

        final int arrowWidth = res.getDimensionPixelOffset(
                R.dimen.message_bubble_arrow_width);
        final int messageTextLeftRightPadding = res.getDimensionPixelOffset(
                R.dimen.message_text_left_right_padding);
        final int textTopPadding = res.getDimensionPixelOffset(
                R.dimen.message_text_top_padding);
        final int textBottomPadding = res.getDimensionPixelOffset(
                R.dimen.message_text_bottom_padding);

        final int textLeftPadding, textRightPadding;

        if (mIncoming) {
            textLeftPadding = messageTextLeftRightPadding + arrowWidth;
            textRightPadding = messageTextLeftRightPadding;
        } else {
            textLeftPadding = messageTextLeftRightPadding;
            textRightPadding = messageTextLeftRightPadding + arrowWidth;
        }

        // These values do not depend on whether the message includes attachments
        final int gravity = mIncoming ? (Gravity.START | Gravity.CENTER_VERTICAL) :
                (Gravity.END | Gravity.CENTER_VERTICAL);
        final int messageTopPadding = res.getDimensionPixelSize(
                R.dimen.message_padding_default);
        final int metadataTopPadding =  res.getDimensionPixelOffset(
                R.dimen.message_metadata_top_padding);

        // Update the message text/info views
        final int bubbleDrawableResId = mIncoming ? R.drawable.msg_bubble_incoming
                : R.drawable.msg_bubble_outgoing;
        final int bubbleColorResId = mIncoming ? R.color.message_bubble_incoming
                : R.color.message_bubble_outgoing;
        final Context context = getContext();

        final Drawable textBackgroundDrawable = getTintedDrawable(context,
                context.getDrawable(bubbleDrawableResId),
                context.getColor(bubbleColorResId));
        mMessageTextAndInfoView.setBackground(textBackgroundDrawable);

        if (isLayoutRtl(this)) {
            // Need to switch right and left padding in RtL mode
            mMessageTextAndInfoView.setPadding(textRightPadding,
                    textTopPadding + metadataTopPadding,
                    textLeftPadding, textBottomPadding);
        } else {
            mMessageTextAndInfoView.setPadding(textLeftPadding,
                    textTopPadding + metadataTopPadding,
                    textRightPadding, textBottomPadding);
        }

        // Update the message row and message bubble views
        setPadding(getPaddingLeft(), messageTopPadding, getPaddingRight(), 0);
        mMessageBubble.setGravity(gravity);

        updateTextAppearance();
    }

    private void updateTextAppearance() {
        final int messageColorResId = (mIncoming ? R.color.message_text_incoming
                : R.color.message_text_outgoing);
        final int timestampColorResId = mIncoming ? R.color.timestamp_text_incoming
                : R.color.timestamp_text_outgoing;
        final int messageColor = getContext().getColor(messageColorResId);

        mMessageTextView.setTextColor(messageColor);
        mMessageTextView.setLinkTextColor(messageColor);
        mStatusTextView.setTextColor(timestampColorResId);
    }

    private static Drawable getTintedDrawable(final Context context, final Drawable drawable,
            final int color) {
        // For some reason occasionally drawables on JB has a null constant state
        final Drawable.ConstantState constantStateDrawable = drawable.getConstantState();
        final Drawable retDrawable = (constantStateDrawable != null)
                ? constantStateDrawable.newDrawable(context.getResources()).mutate()
                : drawable;
        retDrawable.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
        return retDrawable;
    }
}
