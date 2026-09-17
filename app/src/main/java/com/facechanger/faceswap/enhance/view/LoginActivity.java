package com.facechanger.faceswap.enhance.view;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.facechanger.faceswap.enhance.R;
import com.facechanger.faceswap.enhance.controller.AppFaceAppDialogController;
import com.facechanger.faceswap.enhance.controller.AppFaceOnDialogActionListener;
import com.facechanger.faceswap.enhance.model.api.AppFaceSplashDataResponse;
import com.facechanger.faceswap.enhance.utils.AppFaceApiClient;
import com.facechanger.faceswap.enhance.utils.AppFaceApiRepository;
import com.facechanger.faceswap.enhance.utils.AppFaceAppSystem;
import com.facechanger.faceswap.enhance.utils.AppFaceInstallReferrerInfo;
import com.facechanger.faceswap.enhance.utils.AppFaceSessionManager;
import com.facechanger.faceswap.enhance.utils.AppFaceStaticValue;
import com.facechanger.faceswap.enhance.utils.AppFaceTools;
import com.facechanger.faceswap.enhance.utils.FirebaseAuthManager;
import com.faceenhance.facechanger.Utils.GlobleMMKVManager;
import com.faceenhance.facechanger.controller.AdManager;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.util.Map;

/**
 * Login screen displayed when APP_EXP == 1.
 * Authenticates user via Firebase Email/Password (no OTP).
 * On success, reads profile from Firebase Realtime Database,
 * syncs getToken/userId/deviceId/remainingLimit to SessionManager,
 * and navigates to MainActivity.
 */
public class LoginActivity extends BaseAppActivity {

    private static final String TAG = "LoginActivity";

    private EditText etEmail, etPassword;
    private TextView tvEmailError, tvPasswordError, tvGeneralError;
    private LinearLayout emailContainer, passwordContainer;
    private TextView btnLogin;
    private CircularProgressIndicator loginProgress;
    private ImageView btnTogglePassword;
    private boolean isPasswordVisible = false;
    private boolean isLoading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        AppFaceTools.setStatusBarBleed(getWindow(), findViewById(R.id.root), true);

        initViews();
        setupListeners();
    }

    @Override
    public void onBackPressed() {
        // Prevent going back to splash
        finishAffinity();
    }

    private void initViews() {
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        tvEmailError = findViewById(R.id.tvEmailError);
        tvPasswordError = findViewById(R.id.tvPasswordError);
        tvGeneralError = findViewById(R.id.tvGeneralError);
        emailContainer = findViewById(R.id.emailContainer);
        passwordContainer = findViewById(R.id.passwordContainer);
        btnLogin = findViewById(R.id.btnLogin);
        loginProgress = findViewById(R.id.loginProgress);
        btnTogglePassword = findViewById(R.id.btnTogglePassword);
    }

    private void setupListeners() {
        // Login button
        btnLogin.setOnClickListener(v -> {
            if (!isLoading) {
                attemptLogin();
            }
        });

        // Sign Up link
        TextView tvSignUpLink = findViewById(R.id.tvSignUpLink);
        if (tvSignUpLink != null) {
            tvSignUpLink.setOnClickListener(v -> {
                Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
                startActivity(intent);
            });
        }

        // Password toggle
        btnTogglePassword.setOnClickListener(v -> togglePasswordVisibility());

        // Clear errors on text change
        etEmail.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                clearFieldError(emailContainer, tvEmailError);
                hideGeneralError();
            }
        });

        etPassword.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                clearFieldError(passwordContainer, tvPasswordError);
                hideGeneralError();
            }
        });
    }

    // ──────────────────────────────────────────────
    //  Validation & Login
    // ──────────────────────────────────────────────

    private void attemptLogin() {
        hideAllErrors();

        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        boolean isValid = true;

        // Email validation
        if (email.isEmpty()) {
            showFieldError(emailContainer, tvEmailError, getString(R.string.login_error_empty_email));
            isValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showFieldError(emailContainer, tvEmailError, getString(R.string.login_error_invalid_email));
            isValid = false;
        }

        // Password validation
        if (password.isEmpty()) {
            showFieldError(passwordContainer, tvPasswordError, getString(R.string.login_error_empty_password));
            isValid = false;
        } else if (password.length() < 6) {
            showFieldError(passwordContainer, tvPasswordError, getString(R.string.login_error_short_password));
            isValid = false;
        }

        if (!isValid) return;

        // Start login
        setLoadingState(true);

        FirebaseAuthManager.getInstance().login(email, password, new FirebaseAuthManager.AuthCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Login successful, fetching profile...");
                fetchProfileAndNavigate();
            }

            @Override
            public void onError(@NonNull String errorMessage) {
                runOnUiThread(() -> {
                    setLoadingState(false);
                    handleLoginError(errorMessage);
                });
            }
        });
    }

    private void getSplashData() {
        new AppFaceInstallReferrerInfo().getInstallReferrer(LoginActivity.this, new AppFaceInstallReferrerInfo.ReferrerCallback() {

            @Override
            public void onReferrerReceived(String referrer) {
                AppFaceApiRepository.fetchSplashData(LoginActivity.this, referrer, new AppFaceApiRepository.SplashDataCallback() {

                    @Override
                    public void onSuccess(@NonNull AppFaceSplashDataResponse response, String responseBody) {
                        if (response.isSuccess() && response.getData() != null) {
                            AppFaceSessionManager
                                    .getInstance().initialize(response.getData());
                            Log.d(TAG, "Session initialized — token acquired" + responseBody);

                            if (AppFaceAppSystem.isDebugMode()) {
                                GlobleMMKVManager.getInstance().putInt(AppFaceStaticValue.APP_EXP, 1);
                                GlobleMMKVManager.getInstance().putInt(AppFaceStaticValue.IS_IN_APP_AFT_SPL, 1);
                                GlobleMMKVManager.getInstance().putInt(AppFaceStaticValue.IS_PRM_PRC_SHOW, 0);
                            } else {
                                GlobleMMKVManager.getInstance().putInt(AppFaceStaticValue.APP_EXP,
                                        response.getData().getApp_exp());
                                GlobleMMKVManager.getInstance().putInt(AppFaceStaticValue.IS_IN_APP_AFT_SPL,
                                        response.getData().getIs_in_app_aft_spl());
                                GlobleMMKVManager.getInstance().putInt(AppFaceStaticValue.IS_PRM_PRC_SHOW,
                                        response.getData().getIs_prm_prc_show());
                            }

                            if (FirebaseAuthManager.getInstance().isLoggedIn()) {
                                FirebaseAuthManager.getInstance().updateUserCoin(AppFaceSessionManager.getInstance().getCurrentCredits());
                            }

                            AdManager.getInstance().onConfigApiResponse(responseBody);

                            Log.w(TAG, "APP_UPDATE: IS_LATEST" + response.getData().isLatest());

                            navigateToMain();

                        } else {

                            Log.e("dsfsd", "2");

                            GlobleMMKVManager.getInstance().putInt(AppFaceStaticValue.APP_EXP, 1);

                            Log.w(TAG, "Splash data returned isSuccess=false: " + response.getMessage());
                            showRetryDialog(R.drawable.app_transparent_close_choose, getString(R.string.dialog_api_error_title),
                                    response.getMessage() != null && !response.getMessage().isEmpty()
                                            ? response.getMessage()
                                            : getString(R.string.dialog_api_error_msg));
                        }
                    }

                    @Override
                    public void onError(@NonNull String errorMessage) {
                        Log.e(TAG, "Splash data error: " + errorMessage);
                        showRetryDialog(
                                R.drawable.app_transparent_close_choose,
                                getString(R.string.dialog_api_error_title),
                                getString(R.string.dialog_api_error_msg));
                    }
                });
            }
        });

    }

    private void showRetryDialog(int iconRes, String title, String message) {
        if (isFinishing() || isDestroyed())
            return;

        AppFaceAppDialogController.showRetryDialog(
                this,
                iconRes,
                title,
                message,
                getString(R.string.app_api_retry_button_text),
                new AppFaceOnDialogActionListener() {
                    @Override
                    public void onPositiveClick() {
                        getSplashData();
                    }

                    @Override
                    public void onDismiss() {
                        finish();
                    }
                });
    }

    private void fetchProfileAndNavigate() {
        FirebaseAuthManager.getInstance().fetchUserProfile(new FirebaseAuthManager.ProfileCallback() {
            @Override
            public void onSuccess(@NonNull Map<String, Object> userData) {
                runOnUiThread(() -> {
                    // Sync Firebase profile data to SessionManager
                    syncProfileToSession(userData);
                    getSplashData();
                });
            }

            @Override
            public void onError(@NonNull String errorMessage) {
                // Even if profile fetch fails, login was successful
                // Navigate to main anyway
                runOnUiThread(() -> getSplashData());
            }
        });
    }

    private void syncProfileToSession(@NonNull Map<String, Object> userData) {
        try {
            String token = getStringValue(userData, "getToken");
            String userId = getStringValue(userData, "userId");
            String deviceId = getStringValue(userData, "deviceId");

            GlobleMMKVManager.getInstance().putString(AppFaceStaticValue.FIREBASE_DEVICE_ID, deviceId);
            GlobleMMKVManager.getInstance().putString(AppFaceStaticValue.FIREBASE_USER_ID, userId);

            double remainingLimit = getDoubleValue(userData, "remainingLimit");

            // Update SessionManager with values from Firebase
            if (!token.isEmpty()) {
                AppFaceApiClient.getInstance().setAuthToken(token);
            }
            AppFaceSessionManager.getInstance().setCachedCredits(remainingLimit);

            Log.d(TAG, "Profile synced — token: " + (token.isEmpty() ? "empty" : "set")
                    + ", remainingLimit: " + remainingLimit);
        } catch (Exception e) {
            Log.e(TAG, "Error syncing profile to session", e);
        }
    }

    private void navigateToMain() {
        setLoadingState(false);
        Intent intent = new Intent(LoginActivity.this, AppFaceMainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // ──────────────────────────────────────────────
    //  Error Handling
    // ──────────────────────────────────────────────

    private void handleLoginError(@NonNull String error) {
        switch (error) {
            case "WRONG_PASSWORD":
                showGeneralError(getString(R.string.login_error_wrong_password));
                break;
            case "USER_NOT_FOUND":
                showGeneralError(getString(R.string.login_error_user_not_found));
                break;
            case "TOO_MANY_REQUESTS":
                showGeneralError(getString(R.string.login_error_too_many_requests));
                break;
            default:
                showGeneralError(getString(R.string.login_error_failed));
                break;
        }
    }

    // ──────────────────────────────────────────────
    //  UI Helpers
    // ──────────────────────────────────────────────

    private void setLoadingState(boolean loading) {
        isLoading = loading;
        if (loading) {
            btnLogin.setText("");
            btnLogin.setEnabled(false);
            btnLogin.setAlpha(0.6f);
            loginProgress.setVisibility(View.VISIBLE);
        } else {
            btnLogin.setText(getString(R.string.login_button));
            btnLogin.setEnabled(true);
            btnLogin.setAlpha(1f);
            loginProgress.setVisibility(View.GONE);
        }
    }

    private void togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible;
        if (isPasswordVisible) {
            etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        } else {
            etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }
        etPassword.setSelection(etPassword.length());
        btnTogglePassword.setAlpha(isPasswordVisible ? 0.8f : 0.4f);
    }

    private void showFieldError(LinearLayout container, TextView errorView, String message) {
        container.setBackgroundResource(R.drawable.bg_input_field_error);
        errorView.setText(message);
        errorView.setVisibility(View.VISIBLE);
    }

    private void clearFieldError(LinearLayout container, TextView errorView) {
        container.setBackgroundResource(R.drawable.bg_input_field);
        errorView.setVisibility(View.GONE);
    }

    private void showGeneralError(String message) {
        tvGeneralError.setText(message);
        tvGeneralError.setVisibility(View.VISIBLE);
    }

    private void hideGeneralError() {
        tvGeneralError.setVisibility(View.GONE);
    }

    private void hideAllErrors() {
        clearFieldError(emailContainer, tvEmailError);
        clearFieldError(passwordContainer, tvPasswordError);
        hideGeneralError();
    }

    // ──────────────────────────────────────────────
    //  Utility
    // ──────────────────────────────────────────────

    @NonNull
    private String getStringValue(@NonNull Map<String, Object> data, @NonNull String key) {
        Object value = data.get(key);
        return value != null ? String.valueOf(value) : "";
    }

    private double getDoubleValue(@NonNull Map<String, Object> data, @NonNull String key) {
        Object value = data.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (Exception e) {
            return 0.0;
        }
    }

    /**
     * Simplified TextWatcher that only requires onTextChanged.
     */
    static abstract class SimpleTextWatcher implements android.text.TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void afterTextChanged(android.text.Editable s) {
        }
    }
}
