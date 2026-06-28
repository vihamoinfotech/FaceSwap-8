package com.facechanger.faceswap.enhance.utils;

/**
 * Represents a cancellable API request.
 */
public interface ApiCall {
    /**
     * Cancels the underlying network request and prevents the callback from being fired.
     */
    void cancel();

    /**
     * @return true if the call has been cancelled
     */
    boolean isCancelled();
}
