package com.android.settings.development;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.backup.IBackupManager;
import android.content.Context;
import android.os.RemoteException;
import android.os.UserManager;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
public class LocalBackupPasswordPreferenceControllerTest {

    @Mock
    private Preference mPreference;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private UserManager mUserManager;
    @Mock
    private IBackupManager mBackupManager;

    private Context mContext;
    private LocalBackupPasswordPreferenceController mController;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mController = spy(new LocalBackupPasswordPreferenceController(mContext));
        ReflectionHelpers.setField(mController, "mUserManager", mUserManager);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
    }

    @Test
    public void updateState_hasBackupPassword_preferenceShouldHavePasswordSetSummary()
            throws RemoteException {
        ReflectionHelpers.setField(mController, "mBackupManager", mBackupManager);
        doReturn(true).when(mController).isAdminUser();
        when(mBackupManager.hasBackupPassword()).thenReturn(true);
        mController.displayPreference(mScreen);

        mController.updateState(mPreference);

        verify(mPreference).setSummary(R.string.local_backup_password_summary_change);
    }

    @Test
    public void updateState_noBackupPassword_preferenceShouldHaveNoPasswordSetSummary()
            throws RemoteException {
        ReflectionHelpers.setField(mController, "mBackupManager", mBackupManager);
        doReturn(true).when(mController).isAdminUser();
        when(mBackupManager.hasBackupPassword()).thenReturn(false);
        mController.displayPreference(mScreen);

        mController.updateState(mPreference);

        verify(mPreference).setSummary(R.string.local_backup_password_summary_none);
    }

    @Test
    public void updateState_backupManagerIsNull_shouldDisablePreference() throws RemoteException {
        ReflectionHelpers.setField(mController, "mBackupManager", null);
        doReturn(true).when(mController).isAdminUser();
        when(mBackupManager.hasBackupPassword()).thenReturn(true);
        mController.displayPreference(mScreen);

        mController.updateState(mPreference);

        verify(mPreference).setEnabled(false);
        verify(mPreference, never()).setSummary(any());
    }

    @Test
    public void updateState_backupManagerIsNotNullAndNotAdminUser_shouldDisablePreference()
            throws RemoteException {
        ReflectionHelpers.setField(mController, "mBackupManager", mBackupManager);
        doReturn(false).when(mController).isAdminUser();
        when(mBackupManager.hasBackupPassword()).thenReturn(true);

        mController.updateState(mPreference);

        verify(mPreference).setEnabled(false);
        verify(mPreference, never()).setSummary(any());
    }

    @Test
    public void updateState_backupManagerIsNotNullAndAdminUser_shouldEnablePreference()
            throws RemoteException {
        ReflectionHelpers.setField(mController, "mBackupManager", mBackupManager);
        doReturn(true).when(mController).isAdminUser();
        when(mBackupManager.hasBackupPassword()).thenReturn(true);

        mController.updateState(mPreference);

        verify(mPreference).setEnabled(true);
        verify(mPreference, never()).setSummary(any());
    }
}
