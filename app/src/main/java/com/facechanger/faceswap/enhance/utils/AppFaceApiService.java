package com.facechanger.faceswap.enhance.utils;

import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.PartMap;
import retrofit2.http.Url;

/**
 * Retrofit service interface for all API endpoints.
 * <p>
 * Every method returns {@code Call<ResponseBody>} so the response body
 * is consumed as a raw String — exactly matching the existing manual
 * JSON parsing via {@code org.json.JSONObject} and {@code fromJson()}.
 * <p>
 * Endpoints use {@code @Url} for dynamic URL resolution (supports both
 * full URLs and relative paths resolved against the Retrofit base URL).
 */
public interface AppFaceApiService {

    // ── JSON body endpoints ─────────────────────────────

    /** Generic POST with a JSON body (used by splash_data, templates, text-to-image, verify, etc.) */
    @POST
    Call<ResponseBody> post(@Url String url, @Body RequestBody jsonBody);

    /** Generic GET (used by history, etc.) */
    @GET
    Call<ResponseBody> get(@Url String url);

    // ── Multipart endpoints ─────────────────────────────

    /** Single-file multipart upload (used by remove-bg, upscale, enhance, etc.) */
    @Multipart
    @POST
    Call<ResponseBody> multipartSingleFile(
            @Url String url,
            @Part MultipartBody.Part file
    );

    /** Multi-file multipart upload with text fields (used by faceswap, hair, edit, virtual-try-on, etc.) */
    @Multipart
    @POST
    Call<ResponseBody> multipartUpload(
            @Url String url,
            @Part MultipartBody.Part[] files,
            @PartMap Map<String, RequestBody> fields
    );
}
