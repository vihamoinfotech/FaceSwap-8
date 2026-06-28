package com.facechanger.faceswap.enhance.utils;

import androidx.annotation.NonNull;

import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;

/**
 * Helper for loading images that require authenticated API access.
 * <p>
 * Wraps a URL in a {@link GlideUrl} with the Bearer auth header attached.
 * Use with Glide's {@code .load()} method:
 * <pre>
 *   Glide.with(context)
 *       .load(GlideHelper.authorizedUrl(url))
 *       .into(imageView);
 * </pre>
 */
public final class GlideHelper {

    private GlideHelper() { /* non-instantiable */ }

    /**
     * Creates a {@link GlideUrl} with the current session's Bearer token
     * attached in the Authorization header.
     *
     * @param url The image URL to load
     * @return A GlideUrl ready for authenticated loading
     */
    @NonNull
    public static GlideUrl authorizedUrl(@NonNull String url) {
        String token = SessionManager.getInstance().getToken();
        LazyHeaders.Builder builder = new LazyHeaders.Builder();
        if (!token.isEmpty()) {
            builder.addHeader("Authorization", "Bearer " + token);
        }
        return new GlideUrl(url, builder.build());
    }
}
