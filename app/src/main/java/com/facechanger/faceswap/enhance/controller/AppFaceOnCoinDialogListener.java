package com.facechanger.faceswap.enhance.controller;

/**
 * Callback interface for the Insufficient Coins dialog.
 * <p>
 * Provides three action paths:
 * <ul>
 *   <li>{@link #onBuyCoins()} — User tapped "Buy Coins" → navigate to Store</li>
 *   <li>{@link #onDismiss()} — Dialog dismissed without action</li>
 * </ul>
 */
public interface AppFaceOnCoinDialogListener {
    void onBuyCoins();
    void onDismiss();
}
