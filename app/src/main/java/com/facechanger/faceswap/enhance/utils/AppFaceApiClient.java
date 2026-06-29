package com.facechanger.faceswap.enhance.utils;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;

/**
 * Lightweight API client for making HTTP requests — powered by Retrofit + OkHttp.
 * <p>
 * Uses OkHttp for efficient connection pooling, HTTP/2, transparent GZIP,
 * and automatic retries. Retrofit handles endpoint routing and multipart
 * body construction. Results are delivered on the main thread via {@link ApiCallback}.
 * <p>
 * Usage:
 * <pre>
 *   ApiClient.getInstance().get("https://api.example.com/data",
 *       new ApiClient.ApiCallback() {
 *           public void onSuccess(String response) { /* parse JSON *\/ }
 *           public void onError(int code, String message) { /* handle error *\/ }
 *       });
 *
 *   ApiClient.getInstance().post("https://api.example.com/action",
 *       "{\"key\":\"value\"}",
 *       new ApiClient.ApiCallback() { ... });
 * </pre>
 */
public final class AppFaceApiClient {

    private static final String TAG = "ApiClient";
    private static final int CONNECT_TIMEOUT_MS = 15_000;
    private static final int READ_TIMEOUT_MS = 15_000;

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private static volatile AppFaceApiClient instance;

    private final Handler mainHandler;

    /** Auth token injected into every request via the interceptor. */
    private volatile String authToken = "";

    /** Current base URL for Retrofit. */
    private String baseUrl = "https://localhost/"; // Placeholder until setBaseUrl is called

    /** Core OkHttp client with connection pool, timeouts, and interceptors. */
    private OkHttpClient okHttpClient;

    /** Retrofit instance built from the current base URL. */
    private Retrofit retrofit;

    /** Retrofit-generated API service. */
    private AppFaceApiService apiService;

    /** Callback interface for API responses — always delivered on the main thread. */
    public interface ApiCallback {
        void onSuccess(@NonNull String responseBody);
        void onError(int statusCode, @NonNull String errorMessage);
    }

    private AppFaceApiClient() {
        mainHandler = new Handler(Looper.getMainLooper());
        buildClient();
    }

    /** Thread-safe singleton accessor. */
    public static AppFaceApiClient getInstance() {
        if (instance == null) {
            synchronized (AppFaceApiClient.class) {
                if (instance == null) {
                    instance = new AppFaceApiClient();
                }
            }
        }
        return instance;
    }

    // ──────────────────────────────────────────────
    // OkHttp + Retrofit setup
    // ──────────────────────────────────────────────

    /** Builds (or rebuilds) the OkHttpClient, Retrofit instance, and ApiService. */
    private void buildClient() {
        // ── Auth Interceptor ──
        Interceptor authInterceptor = chain -> {
            Request original = chain.request();
            Request.Builder builder = original.newBuilder();
            String token = authToken; // volatile read once
            if (token != null && !token.isEmpty()) {
                builder.header("Authorization", "Bearer " + token);
            }
            return chain.proceed(builder.build());
        };

        // ── Logging Interceptor ──
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(message ->
                Log.d(TAG, message));
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        // ── OkHttpClient ──
        okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(CONNECT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .readTimeout(READ_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .writeTimeout(READ_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .addInterceptor(authInterceptor)
                .addInterceptor(loggingInterceptor)
                .retryOnConnectionFailure(true)
                .build();

        // ── Retrofit ──
        retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(ScalarsConverterFactory.create())
                .build();

        apiService = retrofit.create(AppFaceApiService.class);
    }

    /**
     * Sets the base URL for all requests.
     * E.g. "https://api.example.com/v1"
     */
    public void setBaseUrl(@NonNull String baseUrl) {
        // Ensure trailing slash (Retrofit requires it)
        String url = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
        this.baseUrl = url;
        buildClient(); // Rebuild Retrofit with the new base URL
    }

    /** @return current base URL */
    @NonNull
    public String getBaseUrl() {
        // Return without trailing slash for external consistency
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    /**
     * Sets the global authorization token to be sent in the 'Authorization: Bearer <token>' header.
     * @param token Authentication token. Set to null or empty to clear it.
     */
    public void setAuthToken(@Nullable String token) {
        this.authToken = token == null ? "" : token;
    }

    // ──────────────────────────────────────────────
    // GET
    // ──────────────────────────────────────────────

    /**
     * Performs an HTTP GET request on a background thread.
     *
     * @param endpoint Full URL, or path appended to baseUrl (if baseUrl is set).
     * @param callback Result callback (nullable — fire-and-forget if null).
     * @return ApiCall object to cancel the request
     */
    public AppFaceApiCall get(@NonNull String endpoint, @Nullable ApiCallback callback) {
        AppFaceDefaultApiCall apiCall = new AppFaceDefaultApiCall();

        String fullUrl = resolveUrl(endpoint);
        Call<ResponseBody> call = apiService.get(fullUrl);
        apiCall.setCall(call);

        enqueueCall(call, apiCall, callback);
        return apiCall;
    }

    // ──────────────────────────────────────────────
    // POST
    // ──────────────────────────────────────────────

    /**
     * Performs an HTTP POST request on a background thread.
     *
     * @param endpoint Full URL, or path appended to baseUrl.
     * @param jsonBody JSON string body to send.
     * @param callback Result callback (nullable).
     * @return ApiCall object to cancel the request
     */
    public AppFaceApiCall post(@NonNull String endpoint, @NonNull String jsonBody,
                               @Nullable ApiCallback callback) {
        AppFaceDefaultApiCall apiCall = new AppFaceDefaultApiCall();

        String fullUrl = resolveUrl(endpoint);
        RequestBody body = RequestBody.create(jsonBody, JSON);
        Call<ResponseBody> call = apiService.post(fullUrl, body);
        apiCall.setCall(call);

        enqueueCall(call, apiCall, callback);
        return apiCall;
    }

    // ──────────────────────────────────────────────
    // MULTIPART POST (IMAGE UPLOAD)
    // ──────────────────────────────────────────────

    /**
     * Performs an HTTP POST request with multipart/form-data for image upload on a background thread.
     *
     * @param endpoint Full URL, or path appended to baseUrl.
     * @param imageFile The file to upload.
     * @param fileParameterName The parameter name for the file (e.g., "image", "file", "avatar").
     * @param callback Result callback (nullable).
     * @return ApiCall object to cancel the request
     */
    public AppFaceApiCall multipartImageUpload(@NonNull String endpoint, @NonNull File imageFile, @NonNull String fileParameterName,
                                               @Nullable ApiCallback callback) {
        AppFaceDefaultApiCall apiCall = new AppFaceDefaultApiCall();

        String fullUrl = resolveUrl(endpoint);
        MediaType mediaType = MediaType.parse(guessMimeType(imageFile.getName()));
        RequestBody fileBody = RequestBody.create(imageFile, mediaType);
        MultipartBody.Part filePart = MultipartBody.Part.createFormData(
                fileParameterName, imageFile.getName(), fileBody);

        Call<ResponseBody> call = apiService.multipartSingleFile(fullUrl, filePart);
        apiCall.setCall(call);

        enqueueCall(call, apiCall, callback);
        return apiCall;
    }

    // ──────────────────────────────────────────────
    // MULTIPART POST (MULTIPLE FILES + FIELDS)
    // ──────────────────────────────────────────────

    /**
     * Performs an HTTP POST with multipart/form-data supporting multiple files and text fields.
     * <p>
     * Used for endpoints like {@code /api/Image/faceswap/basic} which require
     * two image files ({@code sourceImage}, {@code targetImage}) plus form fields ({@code enhance}).
     *
     * @param endpoint      Full URL or path appended to baseUrl.
     * @param fileParams    Map of parameter name → File (e.g. "sourceImage" → file).
     * @param textParams    Map of parameter name → String value (e.g. "enhance" → "true"). Nullable.
     * @param timeoutMs     Read timeout in milliseconds (use higher values for image processing).
     * @param callback      Result callback (nullable).
     * @return ApiCall object to cancel the request
     */
    public AppFaceApiCall multipartUpload(@NonNull String endpoint,
                                          @NonNull java.util.Map<String, File> fileParams,
                                          @Nullable java.util.Map<String, String> textParams,
                                          int timeoutMs,
                                          @Nullable ApiCallback callback) {
        AppFaceDefaultApiCall apiCall = new AppFaceDefaultApiCall();

        String fullUrl = resolveUrl(endpoint);

        // ── Build file parts ──
        MultipartBody.Part[] fileParts = new MultipartBody.Part[fileParams.size()];
        int i = 0;
        for (Map.Entry<String, File> entry : fileParams.entrySet()) {
            File file = entry.getValue();
            MediaType mediaType = MediaType.parse(guessMimeType(file.getName()));
            RequestBody fileBody = RequestBody.create(file, mediaType);
            fileParts[i++] = MultipartBody.Part.createFormData(
                    entry.getKey(), file.getName(), fileBody);
        }

        // ── Build text field parts ──
        java.util.Map<String, RequestBody> fieldParts = new java.util.HashMap<>();
        if (textParams != null) {
            for (Map.Entry<String, String> entry : textParams.entrySet()) {
                fieldParts.put(entry.getKey(),
                        RequestBody.create(entry.getValue(), MediaType.parse("text/plain")));
            }
        }

        // ── Use custom timeout for long-running image operations ──
        Call<ResponseBody> call;
        if (timeoutMs > 0 && timeoutMs != READ_TIMEOUT_MS) {
            // Clone OkHttpClient with custom read timeout (reuses connection pool)
            OkHttpClient customClient = okHttpClient.newBuilder()
                    .readTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                    .build();
            Retrofit customRetrofit = retrofit.newBuilder()
                    .client(customClient)
                    .build();
            AppFaceApiService customService = customRetrofit.create(AppFaceApiService.class);
            call = customService.multipartUpload(fullUrl, fileParts, fieldParts);
        } else {
            call = apiService.multipartUpload(fullUrl, fileParts, fieldParts);
        }

        apiCall.setCall(call);
        enqueueCall(call, apiCall, callback);
        return apiCall;
    }

    // ──────────────────────────────────────────────
    // Internal helpers
    // ──────────────────────────────────────────────

    /** Guesses MIME type from file extension. */
    private String guessMimeType(String fileName) {
        if (fileName == null) return "application/octet-stream";
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".gif")) return "image/gif";
        return "image/jpeg"; // Default for .jpg, .jpeg, and unknown
    }

    /** Resolves endpoint to full URL — prepends baseUrl if endpoint is a relative path. */
    private String resolveUrl(String endpoint) {
        if (endpoint.startsWith("http://") || endpoint.startsWith("https://")) {
            return endpoint;
        }
        // Remove leading slash since baseUrl already has trailing slash
        String path = endpoint.startsWith("/") ? endpoint.substring(1) : endpoint;
        return baseUrl + path;
    }

    /**
     * Enqueues a Retrofit call and delivers the result on the main thread
     * via the existing {@link ApiCallback} contract.
     * <p>
     * Preserves exact same behavior as the old HttpURLConnection implementation:
     * <ul>
     *   <li>2xx → {@code onSuccess(responseBody)}</li>
     *   <li>Non-2xx → {@code onError(statusCode, errorBody)}</li>
     *   <li>Network exception → {@code onError(-1, exceptionMessage)}</li>
     *   <li>Cancelled requests → callback is NOT fired</li>
     * </ul>
     */
    private void enqueueCall(@NonNull Call<ResponseBody> call,
                             @NonNull AppFaceDefaultApiCall apiCall,
                             @Nullable ApiCallback callback) {
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> c, @NonNull Response<ResponseBody> response) {
                if (apiCall.isCancelled()) return;

                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String body = response.body().string();
                        deliverSuccess(callback, body);
                    } catch (IOException e) {
                        Log.e(TAG, "Failed to read response body", e);
                        deliverError(callback, response.code(), e.getMessage());
                    }
                } else {
                    // Non-2xx response — read error body
                    String errorBody = "Unknown error";
                    try {
                        ResponseBody errBody = response.errorBody();
                        if (errBody != null) {
                            errorBody = errBody.string();
                        }
                    } catch (IOException e) {
                        errorBody = "Unable to read error response";
                    }
                    deliverError(callback, response.code(), errorBody);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> c, @NonNull Throwable t) {
                if (apiCall.isCancelled()) return;
                Log.e(TAG, "Request failed: " + c.request().url(), t);
                deliverError(callback, -1, t.getMessage());
            }
        });
    }

    private void deliverSuccess(@Nullable ApiCallback callback, String body) {
        if (callback == null) return;
        mainHandler.post(() -> callback.onSuccess(body));
    }

    private void deliverError(@Nullable ApiCallback callback, int code, String message) {
        if (callback == null) return;
        String safeMessage = message != null ? message : "Unknown error";
        mainHandler.post(() -> callback.onError(code, safeMessage));
    }
}
