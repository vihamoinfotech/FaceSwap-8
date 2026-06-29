package com.facechanger.faceswap.enhance.controller;

/**
 * Callback interface for custom dialog actions.
 * <p>
 * Implement this in your Activity/Fragment to receive button click
 * and dismiss events from dialogs shown via {@link AppFaceAppDialogController}.
 */
public interface AppFaceOnDialogActionListener {

    /** Called when the positive/confirm button is clicked. */
    void onPositiveClick();

    /** Called when the dialog is dismissed (close button, back press, or outside tap). */
    void onDismiss();
}
