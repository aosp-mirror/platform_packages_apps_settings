/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.testutils;

import java.util.PriorityQueue;
import java.util.Timer;
import java.util.TimerTask;

/**
 * A fake {@link Timer} that doesn't create a TimerThread which is hard to manage in test.
 */
public class FakeTimer extends Timer {
    private final PriorityQueue<ScheduledTimerTask> mQueue = new PriorityQueue<>();

    public FakeTimer() {
    }

    @Override
    public void cancel() {
        mQueue.clear();
    }

    @Override
    public void schedule(TimerTask task, long delay) {
        mQueue.offer(new ScheduledTimerTask(System.currentTimeMillis() + delay, task));
    }

    /**
     * Runs the first task in the queue if there's any.
     */
    public void runOneTask() {
        if (mQueue.size() > 0) {
            mQueue.poll().mTask.run();
        }
    }

    /**
     * Runs all the queued tasks in order.
     */
    public void runAllTasks() {
        while (mQueue.size() > 0) {
            mQueue.poll().mTask.run();
        }
    }

    /**
     * Returns number of pending tasks in the timer
     */
    public int numOfPendingTasks() {
        return mQueue.size();
    }

    private static class ScheduledTimerTask implements Comparable<ScheduledTimerTask> {
        final long mTimeToRunInMillisSeconds;
        final TimerTask mTask;

        ScheduledTimerTask(long timeToRunInMilliSeconds, TimerTask task) {
            this.mTimeToRunInMillisSeconds = timeToRunInMilliSeconds;
            this.mTask = task;
        }

        @Override
        public int compareTo(ScheduledTimerTask other) {
            return Long.compare(this.mTimeToRunInMillisSeconds, other.mTimeToRunInMillisSeconds);
        }
    }
}
