/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings;

import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.CallSuper;
import androidx.annotation.IntDef;

import com.android.settingslib.utils.ThreadUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * A headless fragment encapsulating a long-running action such as a network RPC surviving rotation.
 *
 * <p>Subclasses should implement their own state machine, updating the state on each state change
 * via {@link #setState(int, int)}. They can define their own states, however, it is suggested that
 * the pre-defined {@link @State} constants are used and customizations are implemented via
 * substates. Custom states must be outside the range of pre-defined states.
 *
 * <p>It is safe to update the state at any time, but state updates must originate from the main
 * thread.
 *
 * <p>A listener can be attached that receives state updates while it's registered. Note that state
 * change events can occur at any point in time and hence a registered listener should unregister if
 * it cannot act upon the state change (typically a non-resumed fragment).
 *
 * <p>Listeners can receive state changes for the same state/substate combination, so listeners
 * should make sure to be idempotent during state change events.
 *
 * <p>If a SidecarFragment is only relevant during the lifetime of another fragment (for example, a
 * sidecar performing a details request for a DetailsFragment), that fragment needs to become the
 * managing fragment of the sidecar.
 *
 * <h2>Managing fragment responsibilities</h2>
 *
 * <ol>
 *   <li>Instantiates the sidecar fragment when necessary, preferably in {@link #onStart}.
 *   <li>Removes the sidecar fragment when it's no longer used or when itself is removed. Removal of
 *       the managing fragment can be detected by checking {@link #isRemoving} in {@link #onStop}.
 *       <br>
 *   <li>Registers as a listener in {@link #onResume()}, unregisters in {@link #onPause()}.
 *   <li>Starts the long-running operation by calling into the sidecar.
 *   <li>Receives state updates via {@link Listener#onStateChange(SidecarFragment)} and updates the
 *       UI accordingly.
 * </ol>
 *
 * <h2>Managing fragment example</h2>
 *
 * <pre>
 *     public class MainFragment implements SidecarFragment.Listener {
 *         private static final String TAG_SOME_SIDECAR = ...;
 *         private static final String KEY_SOME_SIDECAR_STATE = ...;
 *
 *         private SomeSidecarFragment mSidecar;
 *
 *         &#064;Override
 *         public void onStart() {
 *             super.onStart();
 *             Bundle args = ...; // optional args
 *             mSidecar = SidecarFragment.get(getFragmentManager(), TAG_SOME_SIDECAR,
 *                     SidecarFragment.class, args);
 *         }
 *
 *         &#064;Override
 *         public void onResume() {
 *             mSomeSidecar.addListener(this);
 *         }
 *
 *         &#064;Override
 *         public void onPause() {
 *             mSomeSidecar.removeListener(this):
 *         }
 *     }
 * </pre>
 */
public class SidecarFragment extends Fragment {

    private static final String TAG = "SidecarFragment";

    /**
     * Get an instance of this sidecar.
     *
     * <p>Will return the existing instance if one is already present. Note that the args will not
     * be used in this situation, so args must be constant for any particular fragment manager and
     * tag.
     */
    @SuppressWarnings("unchecked")
    protected static <T extends SidecarFragment> T get(
            FragmentManager fm, String tag, Class<T> clazz, Bundle args) {
        T fragment = (T) fm.findFragmentByTag(tag);
        if (fragment == null) {
            try {
                fragment = clazz.newInstance();
            } catch (java.lang.InstantiationException e) {
                throw new InstantiationException("Unable to create fragment", e);
            } catch (IllegalAccessException e) {
                throw new IllegalArgumentException("Unable to create fragment", e);
            }
            if (args != null) {
                fragment.setArguments(args);
            }
            fm.beginTransaction().add(fragment, tag).commit();
            // No real harm in doing this here - get() should generally only be called from onCreate
            // which is on the main thread - and it allows us to start running the sidecar on this
            // instance immediately rather than having to wait until the transaction commits.
            fm.executePendingTransactions();
        }

        return fragment;
    }

    /** State definitions. @see {@link #getState} */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({State.INIT, State.RUNNING, State.SUCCESS, State.ERROR})
    public @interface State {
        /** Initial idling state. */
        int INIT = 0;

        /** The long-running operation is in progress. */
        int RUNNING = 1;

        /** The long-running operation has succeeded. */
        int SUCCESS = 2;

        /** The long-running operation has failed. */
        int ERROR = 3;
    }

    /** Substate definitions. @see {@link #getSubstate} */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
        Substate.UNUSED,
        Substate.RUNNING_BIND_SERVICE,
        Substate.RUNNING_GET_ACTIVATION_CODE,
    })
    public @interface Substate {
        // Unknown/unused substate.
        int UNUSED = 0;
        int RUNNING_BIND_SERVICE = 1;
        int RUNNING_GET_ACTIVATION_CODE = 2;

        // Future tags: 3+
    }

    /** **************************************** */
    private Set<Listener> mListeners = new CopyOnWriteArraySet<>();

    // Used to track whether onCreate has been called yet.
    private boolean mCreated;

    @State private int mState;
    @Substate private int mSubstate;

    /** A listener receiving state change events. */
    public interface Listener {

        /**
         * Called upon any state or substate change.
         *
         * <p>The new state can be queried through {@link #getState} and {@link #getSubstate}.
         *
         * <p>Called from the main thread.
         *
         * @param fragment the SidecarFragment that changed its state
         */
        void onStateChange(SidecarFragment fragment);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        mCreated = true;
        setState(State.INIT, Substate.UNUSED);
    }

    @Override
    public void onDestroy() {
        mCreated = false;
        super.onDestroy();
    }

    /**
     * Registers a listener that will receive subsequent state changes.
     *
     * <p>A {@link Listener#onStateChange(SidecarFragment)} event is fired as part of this call
     * unless {@link #onCreate} has not yet been called (which means that it's unsafe to access this
     * fragment as it has not been setup or restored completely). In that case, the future call to
     * onCreate will trigger onStateChange on registered listener.
     *
     * <p>Must be called from the main thread.
     *
     * @param listener a listener, or null for unregistering the current listener
     */
    public void addListener(Listener listener) {
        ThreadUtils.ensureMainThread();
        mListeners.add(listener);
        if (mCreated) {
            notifyListener(listener);
        }
    }

    /**
     * Removes a previously registered listener.
     *
     * @return {@code true} if the listener was removed, {@code false} if there was no such listener
     *     registered.
     */
    public boolean removeListener(Listener listener) {
        ThreadUtils.ensureMainThread();
        return mListeners.remove(listener);
    }

    /** Returns the current state. */
    @State
    public int getState() {
        return mState;
    }

    /** Returns the current substate. */
    @Substate
    public int getSubstate() {
        return mSubstate;
    }

    /**
     * Resets the sidecar to its initial state.
     *
     * <p>Implementers can override this method to perform additional reset tasks, but must call the
     * super method.
     */
    @CallSuper
    public void reset() {
        setState(State.INIT, Substate.UNUSED);
    }

    /**
     * Updates the state and substate and notifies the registered listener.
     *
     * <p>Must be called from the main thread.
     *
     * @param state the state to transition to
     * @param substate the substate to transition to
     */
    protected void setState(@State int state, @Substate int substate) {
        ThreadUtils.ensureMainThread();

        mState = state;
        mSubstate = substate;
        notifyAllListeners();
        printState();
    }

    private void notifyAllListeners() {
        for (Listener listener : mListeners) {
            notifyListener(listener);
        }
    }

    private void notifyListener(Listener listener) {
        listener.onStateChange(this);
    }

    /** Prints the state of the sidecar. */
    public void printState() {
        StringBuilder sb =
                new StringBuilder("SidecarFragment.setState(): Sidecar Class: ")
                        .append(getClass().getCanonicalName());
        sb.append(", State: ");
        switch (mState) {
            case SidecarFragment.State.INIT:
                sb.append("State.INIT");
                break;
            case SidecarFragment.State.RUNNING:
                sb.append("State.RUNNING");
                break;
            case SidecarFragment.State.SUCCESS:
                sb.append("State.SUCCESS");
                break;
            case SidecarFragment.State.ERROR:
                sb.append("State.ERROR");
                break;
            default:
                sb.append(mState);
                break;
        }
        switch (mSubstate) {
            case SidecarFragment.Substate.UNUSED:
                sb.append(", Substate.UNUSED");
                break;
            default:
                sb.append(", ").append(mSubstate);
                break;
        }

        Log.v(TAG, sb.toString());
    }

    @Override
    public String toString() {
        return String.format(
                Locale.US,
                "SidecarFragment[mState=%d, mSubstate=%d]: %s",
                mState,
                mSubstate,
                super.toString());
    }

    /** The State of the sidecar status. */
    public static final class States {
        public static final States SUCCESS = States.create(State.SUCCESS, Substate.UNUSED);
        public static final States ERROR = States.create(State.ERROR, Substate.UNUSED);

        @State public final int state;
        @Substate public final int substate;

        /** Creates a new sidecar state. */
        public static States create(@State int state, @Substate int substate) {
            return new States(state, substate);
        }

        public States(@State int state, @Substate int substate) {
            this.state = state;
            this.substate = substate;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof States)) {
                return false;
            }
            States other = (States) o;
            return this.state == other.state && this.substate == other.substate;
        }

        @Override
        public int hashCode() {
            return state * 31 + substate;
        }
    }
}
