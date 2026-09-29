package com.perfect.faceeditor.facechanger.io.controller;

/**
 * Callback interface for the "Server Busy" dialog.
 * <p>
 * Used with {@link SFS_RefaceApDlogController#showServerBusyDialog} to handle
 * user actions when an API call fails due to server load.
 */
public interface SFS_RefaceOnServerBusyListener {

    /** Called when the user taps "Try Again". */
    void onRetry();

    /** Called when the user taps "Go Back" or dismisses the dialog. */
    void onGoBack();
}
