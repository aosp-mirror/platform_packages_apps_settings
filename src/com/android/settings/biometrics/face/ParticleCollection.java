/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.biometrics.face;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Rect;

import com.android.settings.R;
import com.android.settings.biometrics.BiometricEnrollSidecar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Class that's used to create, maintain, and update the state of each animation particle. Particles
 * should have their colors assigned based on their index. Particles are split into primary and
 * secondary types - primary types animate twice as fast during the completion effect. The particles
 * are updated/drawn in a special order so that the overlap is correct during the final completion
 * effect.
 */
public class ParticleCollection implements BiometricEnrollSidecar.Listener {

    private static final String TAG = "AnimationController";

    private static final int NUM_PARTICLES = 12;

    public static final int STATE_STARTED = 1; // dots are rotating
    public static final int STATE_STOPPED_COLORFUL = 2; // dots are not rotating but colorful
    public static final int STATE_STOPPED_GRAY = 3; // dots are not rotating and also gray (error)
    public static final int STATE_COMPLETE = 4; // face is enrolled

    private final List<AnimationParticle> mParticleList;
    private final List<Integer> mPrimariesInProgress; // primary particles not done animating yet
    private int mState;
    private Listener mListener;

    public interface Listener {
        void onEnrolled();
    }

    private final AnimationParticle.Listener mParticleListener = new AnimationParticle.Listener() {
        @Override
        public void onRingCompleted(int index) {
            final boolean wasEmpty = mPrimariesInProgress.isEmpty();
            // We can stop the time animator once the three primary particles have finished
            for (int i = 0; i < mPrimariesInProgress.size(); i++) {
                if (mPrimariesInProgress.get(i).intValue() == index) {
                    mPrimariesInProgress.remove(i);
                    break;
                }
            }
            if (mPrimariesInProgress.isEmpty() && !wasEmpty) {
                mListener.onEnrolled();
            }
        }
    };

    public ParticleCollection(Context context, Listener listener, Rect bounds, int borderWidth) {
        mParticleList = new ArrayList<>();
        mListener = listener;

        final List<Integer> colors = new ArrayList<>();
        final Resources.Theme theme = context.getTheme();
        final Resources resources = context.getResources();
        colors.add(resources.getColor(R.color.face_anim_particle_color_1, theme));
        colors.add(resources.getColor(R.color.face_anim_particle_color_2, theme));
        colors.add(resources.getColor(R.color.face_anim_particle_color_3, theme));
        colors.add(resources.getColor(R.color.face_anim_particle_color_4, theme));

        // Primary particles expand faster during the completion animation
        mPrimariesInProgress = new ArrayList<>(Arrays.asList(0, 4, 8));

        // Order in which to draw the particles. This is so the final "completion" animation has
        // the correct behavior.
        final int[] order = {3, 7, 11, 2, 6, 10, 1, 5, 9, 0, 4, 8};

        for (int i = 0; i < NUM_PARTICLES; i++) {
            AnimationParticle particle = new AnimationParticle(context, mParticleListener, bounds,
                    borderWidth, order[i], NUM_PARTICLES, colors);
            if (mPrimariesInProgress.contains(order[i])) {
                particle.setAsPrimary();
            }
            mParticleList.add(particle);
        }

        updateState(STATE_STARTED);
    }

    public void update(long t, long dt) {
        for (int i = 0; i < mParticleList.size(); i++) {
            mParticleList.get(i).update(t, dt);
        }
    }

    public void draw(Canvas canvas) {
        for (int i = 0; i < mParticleList.size(); i++) {
            mParticleList.get(i).draw(canvas);
        }
    }

    private void updateState(int state) {
        if (mState != state) {
            for (int i = 0; i < mParticleList.size(); i++) {
                mParticleList.get(i).updateState(state);
            }
            mState = state;
        }
    }

    @Override
    public void onEnrollmentHelp(int helpMsgId, CharSequence helpString) {

    }

    @Override
    public void onEnrollmentError(int errMsgId, CharSequence errString) {

    }

    @Override
    public void onEnrollmentProgressChange(int steps, int remaining) {
        if (remaining == 0) {
            updateState(STATE_COMPLETE);
        }
    }
}
