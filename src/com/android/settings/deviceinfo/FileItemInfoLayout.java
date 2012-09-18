// Copyright 2011 Google Inc. All Rights Reserved.

package com.android.settings.deviceinfo;

import android.content.Context;
import android.os.Environment.UserEnvironment;
import android.os.UserHandle;
import android.util.AttributeSet;
import android.view.ViewDebug;
import android.widget.CheckBox;
import android.widget.Checkable;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.settings.R;

/**
 * Handles display of a single row entry on Settings --> Storage --> Misc Files screen
 */
public class FileItemInfoLayout extends RelativeLayout implements Checkable {
    private TextView mFileNameView;
    private TextView mFileSizeView;
    private CheckBox mCheckbox;

    private static final int sLengthExternalStorageDirPrefix = new UserEnvironment(
            UserHandle.myUserId()).getExternalStorageDirectory().getAbsolutePath().length() + 1;

    public FileItemInfoLayout(Context context) {
        this(context, null);
    }

    public FileItemInfoLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FileItemInfoLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void toggle() {
        setChecked(!mCheckbox.isChecked());
    }

    /* (non-Javadoc)
     * @see android.view.View#onFinishInflate()
     */
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mFileNameView = (TextView) findViewById(R.id.misc_filename);
        mFileSizeView = (TextView) findViewById(R.id.misc_filesize);
        mCheckbox = (CheckBox) findViewById(R.id.misc_checkbox);
    }

    public void setFileName(String fileName) {
        mFileNameView.setText(fileName.substring(sLengthExternalStorageDirPrefix));
    }

    public void setFileSize(String filesize) {
        mFileSizeView.setText(filesize);
    }

    @ViewDebug.ExportedProperty
    public boolean isChecked() {
        return mCheckbox.isChecked();
    }

    public CheckBox getCheckBox() {
        return mCheckbox;
    }

    /**
     * <p>Changes the checked state of this text view.</p>
     *
     * @param checked true to check the text, false to uncheck it
     */
    public void setChecked(boolean checked) {
        mCheckbox.setChecked(checked);
    }
}