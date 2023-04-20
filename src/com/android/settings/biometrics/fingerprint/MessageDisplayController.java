/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.biometrics.fingerprint;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Handler;

import java.time.Clock;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;

/**
 * Processes message provided from the enrollment callback and filters them based
 * on the below configurable flags. This is primarily used to reduce the rate
 * at which messages come through, which in turns eliminates UI flicker.
 */
public class MessageDisplayController extends FingerprintManager.EnrollmentCallback {

    private final int mHelpMinimumDisplayTime;
    private final int mProgressMinimumDisplayTime;
    private final boolean mProgressPriorityOverHelp;
    private final boolean mPrioritizeAcquireMessages;
    private final int mCollectTime;
    @NonNull
    private final Deque<HelpMessage> mHelpMessageList;
    @NonNull
    private final Deque<ProgressMessage> mProgressMessageList;
    @NonNull
    private final Handler mHandler;
    @NonNull
    private final Clock mClock;
    @NonNull
    private final Runnable mDisplayMessageRunnable;

    @Nullable
    private ProgressMessage mLastProgressMessageDisplayed;
    private boolean mMustDisplayProgress;
    private boolean mWaitingForMessage;
    @NonNull FingerprintManager.EnrollmentCallback mEnrollmentCallback;

    private abstract static class Message {
        long mTimeStamp = 0;
        abstract void display();
    }

    private class HelpMessage extends Message {
        private final int mHelpMsgId;
        private final CharSequence mHelpString;

        HelpMessage(int helpMsgId, CharSequence helpString) {
            mHelpMsgId = helpMsgId;
            mHelpString = helpString;
            mTimeStamp = mClock.millis();
        }

        @Override
        void display() {
            mEnrollmentCallback.onEnrollmentHelp(mHelpMsgId, mHelpString);
            mHandler.postDelayed(mDisplayMessageRunnable, mHelpMinimumDisplayTime);
        }
    }

    private class ProgressMessage extends Message {
        private final int mRemaining;

        ProgressMessage(int remaining) {
            mRemaining = remaining;
            mTimeStamp = mClock.millis();
        }

        @Override
        void display() {
            mEnrollmentCallback.onEnrollmentProgress(mRemaining);
            mLastProgressMessageDisplayed = this;
            mHandler.postDelayed(mDisplayMessageRunnable, mProgressMinimumDisplayTime);
        }
    }

    /**
     * Creating a MessageDisplayController object.
     * @param handler main handler to run message queue
     * @param enrollmentCallback callback to display messages
     * @param clock real time system clock
     * @param helpMinimumDisplayTime the minimum duration (in millis) that
*        a help message needs to be displayed for
     * @param progressMinimumDisplayTime the minimum duration (in millis) that
*        a progress message needs to be displayed for
     * @param progressPriorityOverHelp if true, then progress message is displayed
*        when both help and progress message APIs have been called
     * @param prioritizeAcquireMessages if true, then displays the help message
*        which has occurred the most after the last display message
     * @param collectTime the waiting time (in millis) to collect messages when it is idle
     */
    public MessageDisplayController(@NonNull Handler handler,
            FingerprintManager.EnrollmentCallback enrollmentCallback,
            @NonNull Clock clock, int helpMinimumDisplayTime, int progressMinimumDisplayTime,
            boolean progressPriorityOverHelp, boolean prioritizeAcquireMessages,
            int collectTime) {
        mClock = clock;
        mWaitingForMessage = false;
        mHelpMessageList = new ArrayDeque<>();
        mProgressMessageList = new ArrayDeque<>();
        mHandler = handler;
        mEnrollmentCallback = enrollmentCallback;

        mHelpMinimumDisplayTime = helpMinimumDisplayTime;
        mProgressMinimumDisplayTime = progressMinimumDisplayTime;
        mProgressPriorityOverHelp = progressPriorityOverHelp;
        mPrioritizeAcquireMessages = prioritizeAcquireMessages;
        mCollectTime = collectTime;

        mDisplayMessageRunnable = () -> {
            long timeStamp = mClock.millis();
            Message messageToDisplay = getMessageToDisplay(timeStamp);

            if (messageToDisplay != null) {
                messageToDisplay.display();
            } else {
                mWaitingForMessage = true;
            }
        };

        mHandler.postDelayed(mDisplayMessageRunnable, 0);
    }

    /**
     * Adds help message to the queue to be processed later.
     *
     * @param helpMsgId message Id associated with the help message
     * @param helpString string associated with the help message
     */
    @Override
    public void onEnrollmentHelp(int helpMsgId, CharSequence helpString) {
        mHelpMessageList.add(new HelpMessage(helpMsgId, helpString));

        if (mWaitingForMessage) {
            mWaitingForMessage = false;
            mHandler.postDelayed(mDisplayMessageRunnable, mCollectTime);
        }
    }

    /**
     * Adds progress change message to the queue to be processed later.
     *
     * @param remaining remaining number of steps to complete enrollment
     */
    @Override
    public void onEnrollmentProgress(int remaining) {
        mProgressMessageList.add(new ProgressMessage(remaining));

        if (mWaitingForMessage) {
            mWaitingForMessage = false;
            mHandler.postDelayed(mDisplayMessageRunnable, mCollectTime);
        }
    }

    @Override
    public void onEnrollmentError(int errMsgId, CharSequence errString) {
        mEnrollmentCallback.onEnrollmentError(errMsgId, errString);
    }

    private Message getMessageToDisplay(long timeStamp) {
        ProgressMessage progressMessageToDisplay = getProgressMessageToDisplay(timeStamp);
        if (mMustDisplayProgress) {
            mMustDisplayProgress = false;
            if (progressMessageToDisplay != null) {
                return progressMessageToDisplay;
            }
            if (mLastProgressMessageDisplayed != null) {
                return mLastProgressMessageDisplayed;
            }
        }

        Message helpMessageToDisplay = getHelpMessageToDisplay(timeStamp);
        if (helpMessageToDisplay != null || progressMessageToDisplay != null) {
            if (mProgressPriorityOverHelp && progressMessageToDisplay != null) {
                return progressMessageToDisplay;
            } else if (helpMessageToDisplay != null) {
                if (progressMessageToDisplay != null) {
                    mMustDisplayProgress = true;
                    mLastProgressMessageDisplayed = progressMessageToDisplay;
                }
                return helpMessageToDisplay;
            } else {
                return progressMessageToDisplay;
            }
        } else {
            return null;
        }
    }

    private ProgressMessage getProgressMessageToDisplay(long timeStamp) {
        ProgressMessage finalProgressMessage = null;
        while (mProgressMessageList != null && !mProgressMessageList.isEmpty()) {
            Message message = mProgressMessageList.peekFirst();
            if (message.mTimeStamp <= timeStamp) {
                ProgressMessage progressMessage = mProgressMessageList.pollFirst();
                if (mLastProgressMessageDisplayed != null
                        && mLastProgressMessageDisplayed.mRemaining == progressMessage.mRemaining) {
                    continue;
                }
                finalProgressMessage = progressMessage;
            } else {
                break;
            }
        }

        return finalProgressMessage;
    }

    private HelpMessage getHelpMessageToDisplay(long timeStamp) {
        HashMap<CharSequence, Integer> messageCount = new HashMap<>();
        HelpMessage finalHelpMessage = null;

        while (mHelpMessageList != null && !mHelpMessageList.isEmpty()) {
            Message message = mHelpMessageList.peekFirst();
            if (message.mTimeStamp <= timeStamp) {
                finalHelpMessage = mHelpMessageList.pollFirst();
                CharSequence errString = finalHelpMessage.mHelpString;
                messageCount.put(errString, messageCount.getOrDefault(errString, 0) + 1);
            } else {
                break;
            }
        }
        if (mPrioritizeAcquireMessages) {
            finalHelpMessage = prioritizeHelpMessageByCount(messageCount);
        }

        return finalHelpMessage;
    }

    private HelpMessage prioritizeHelpMessageByCount(HashMap<CharSequence, Integer> messageCount) {
        int maxCount = 0;
        CharSequence maxCountMessage = null;

        for (CharSequence key :
                messageCount.keySet()) {
            if (maxCount < messageCount.get(key)) {
                maxCountMessage = key;
                maxCount = messageCount.get(key);
            }
        }

        return maxCountMessage != null ? new HelpMessage(0 /* errMsgId */,
                maxCountMessage) : null;
    }
}
