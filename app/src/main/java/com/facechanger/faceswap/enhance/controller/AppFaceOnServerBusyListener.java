package com.facechanger.faceswap.enhance.controller;

/**
 * Callback interface for the "Server Busy" dialog.
 * <p>
 * Used with {@link AppFaceAppDialogController#showServerBusyDialog} to handle
 * user actions when an API call fails due to server load.
 */
public interface AppFaceOnServerBusyListener {

    /** Called when the user taps "Try Again". */
    void onRetry();

    /** Called when the user taps "Go Back" or dismisses the dialog. */
    void onGoBack();
}
