package com.facechanger.faceswap.enhance.utils;

import retrofit2.Call;

/**
 * Default implementation of {@link AppFaceApiCall} backed by a Retrofit {@link Call}.
 * <p>
 * Thread-safe cancellation — {@code Call.cancel()} immediately interrupts
 * the underlying OkHttp request without spawning extra threads.
 */
public class AppFaceDefaultApiCall implements AppFaceApiCall {

    private volatile Call<?> call;
    private volatile boolean cancelled = false;

    // Package-private constructor
    AppFaceDefaultApiCall() {}

    /**
     * Binds the Retrofit call so it can be cancelled later.
     * If {@link #cancel()} was already called before binding, cancels immediately.
     */
    void setCall(Call<?> call) {
        this.call = call;
        if (cancelled && call != null) {
            call.cancel();
        }
    }

    @Override
    public void cancel() {
        cancelled = true;
        if (call != null) {
            call.cancel();
        }
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }
}
