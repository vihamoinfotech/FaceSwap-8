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
import com.facechanger.faceswap.enhance.utils.AppFaceApiClient;
import com.facechanger.faceswap.enhance.utils.AppFaceSessionManager;
import com.facechanger.faceswap.enhance.utils.AppFaceStaticValue;
import com.facechanger.faceswap.enhance.utils.AppFaceTools;
import com.facechanger.faceswap.enhance.utils.FirebaseAuthManager;
import com.faceenhance.facechanger.Utils.GlobleMMKVManager;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.util.Map;

/**
 * Signup screen displayed when APP_EXP == 1.
 * Creates a new user via Firebase Email/Password Auth (no OTP).
 * Generates a custom UserID (FS_XXXXXXXX).
 * Saves user profile + splash data (getToken, userId, deviceId, remainingLimit)
 * to Firebase Realtime Database.
 */
public class SignupActivity extends BaseAppActivity {

    private static final String TAG = "SignupActivity";

    private EditText etFullName, etEmail, etPassword, etConfirmPassword;
    private TextView tvNameError, tvEmailError, tvPasswordError, tvConfirmPasswordError, tvGeneralError;
    private LinearLayout nameContainer, emailContainer, passwordContainer, confirmPasswordContainer;
    private TextView btnSignUp, tvUserId;
    private CircularProgressIndicator signupProgress;
    private ImageView btnTogglePassword, btnToggleConfirmPassword;

    private boolean isPasswordVisible = false;
    private boolean isConfirmPasswordVisible = false;
    private boolean isLoading = false;

    private String generatedUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);
        AppFaceTools.setStatusBarBleed(getWindow(), findViewById(R.id.root), true);

        // Generate custom User ID
        generatedUserId = FirebaseAuthManager.generateCustomUserId();

        initViews();
        setupListeners();

        // Display the generated User ID
        tvUserId.setText(generatedUserId);
    }

    private void initViews() {
        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);

        tvNameError = findViewById(R.id.tvNameError);
        tvEmailError = findViewById(R.id.tvEmailError);
        tvPasswordError = findViewById(R.id.tvPasswordError);
        tvConfirmPasswordError = findViewById(R.id.tvConfirmPasswordError);
        tvGeneralError = findViewById(R.id.tvGeneralError);

        nameContainer = findViewById(R.id.nameContainer);
        emailContainer = findViewById(R.id.emailContainer);
        passwordContainer = findViewById(R.id.passwordContainer);
        confirmPasswordContainer = findViewById(R.id.confirmPasswordContainer);

        btnSignUp = findViewById(R.id.btnSignUp);
        signupProgress = findViewById(R.id.signupProgress);
        tvUserId = findViewById(R.id.tvUserId);

        btnTogglePassword = findViewById(R.id.btnTogglePassword);
        btnToggleConfirmPassword = findViewById(R.id.btnToggleConfirmPassword);
    }

    private void setupListeners() {
        // Back button
        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // Sign Up button
        btnSignUp.setOnClickListener(v -> {
            if (!isLoading) {
                attemptSignUp();
            }
        });

        // Login link
        TextView tvLoginLink = findViewById(R.id.tvLoginLink);
        if (tvLoginLink != null) {
            tvLoginLink.setOnClickListener(v -> {
                finish();
            });
        }

        // Password toggles
        btnTogglePassword.setOnClickListener(v -> {
            isPasswordVisible = !isPasswordVisible;
            togglePassword(etPassword, btnTogglePassword, isPasswordVisible);
        });

        btnToggleConfirmPassword.setOnClickListener(v -> {
            isConfirmPasswordVisible = !isConfirmPasswordVisible;
            togglePassword(etConfirmPassword, btnToggleConfirmPassword, isConfirmPasswordVisible);
        });

        // Clear errors on text change
        etFullName.addTextChangedListener(new LoginActivity.SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                clearFieldError(nameContainer, tvNameError);
                hideGeneralError();
            }
        });

        etEmail.addTextChangedListener(new LoginActivity.SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                clearFieldError(emailContainer, tvEmailError);
                hideGeneralError();
            }
        });

        etPassword.addTextChangedListener(new LoginActivity.SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                clearFieldError(passwordContainer, tvPasswordError);
                hideGeneralError();
            }
        });

        etConfirmPassword.addTextChangedListener(new LoginActivity.SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                clearFieldError(confirmPasswordContainer, tvConfirmPasswordError);
                hideGeneralError();
            }
        });
    }

    // ──────────────────────────────────────────────
    //  Validation & Signup
    // ──────────────────────────────────────────────

    private void attemptSignUp() {
        hideAllErrors();

        String fullName = etFullName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        boolean isValid = true;

        // Name validation
        if (fullName.isEmpty()) {
            showFieldError(nameContainer, tvNameError, getString(R.string.signup_error_empty_name));
            isValid = false;
        } else if (fullName.length() < 2) {
            showFieldError(nameContainer, tvNameError, getString(R.string.signup_error_short_name));
            isValid = false;
        }

        // Email validation
        if (email.isEmpty()) {
            showFieldError(emailContainer, tvEmailError, getString(R.string.signup_error_empty_email));
            isValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showFieldError(emailContainer, tvEmailError, getString(R.string.signup_error_invalid_email));
            isValid = false;
        }

        // Password validation
        if (password.isEmpty()) {
            showFieldError(passwordContainer, tvPasswordError, getString(R.string.signup_error_empty_password));
            isValid = false;
        } else if (password.length() < 6) {
            showFieldError(passwordContainer, tvPasswordError, getString(R.string.signup_error_short_password));
            isValid = false;
        }

        // Confirm password validation
        if (confirmPassword.isEmpty()) {
            showFieldError(confirmPasswordContainer, tvConfirmPasswordError, getString(R.string.signup_error_empty_confirm));
            isValid = false;
        } else if (!confirmPassword.equals(password)) {
            showFieldError(confirmPasswordContainer, tvConfirmPasswordError, getString(R.string.signup_error_password_mismatch));
            isValid = false;
        }

        if (!isValid) return;

        // Get splash data from SessionManager
        String splashToken = AppFaceSessionManager.getInstance().getToken();
        String splashUserId = AppFaceSessionManager.getInstance().getUserId();
        String splashDeviceId = AppFaceSessionManager.getInstance().getDeviceId();
        double remainingLimit = AppFaceSessionManager.getInstance().getCurrentCredits();

        // Start signup
        setLoadingState(true);

        FirebaseAuthManager.getInstance().signUp(
                email, password, fullName, generatedUserId,
                splashToken, splashUserId, splashDeviceId, remainingLimit,
                new FirebaseAuthManager.AuthCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "Signup successful");
                        runOnUiThread(() -> fetchProfileAndNavigate());
                    }

                    @Override
                    public void onError(@NonNull String errorMessage) {
                        runOnUiThread(() -> {
                            setLoadingState(false);
                            handleSignUpError(errorMessage);
                        });
                    }
                }
        );
    }


    private void fetchProfileAndNavigate() {
        FirebaseAuthManager.getInstance().fetchUserProfile(new FirebaseAuthManager.ProfileCallback() {
            @Override
            public void onSuccess(@NonNull Map<String, Object> userData) {
                runOnUiThread(() -> {
                    // Sync Firebase profile data to SessionManager
                    syncProfileToSession(userData);
                    navigateToMain();
                });
            }

            @Override
            public void onError(@NonNull String errorMessage) {
                // Even if profile fetch fails, login was successful
                // Navigate to main anyway
                runOnUiThread(() -> navigateToMain());
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

            if (!token.isEmpty()) {
                AppFaceApiClient.getInstance().setAuthToken(token);
            }

            AppFaceSessionManager.getInstance().setCachedCredits(remainingLimit);

            navigateToMain();

            Log.d(TAG, "Profile synced — token: " + (token.isEmpty() ? "empty" : "set")
                    + ", remainingLimit: " + remainingLimit);
        } catch (Exception e) {
            Log.e(TAG, "Error syncing profile to session", e);
        }
    }

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

    private void navigateToMain() {
        setLoadingState(false);
        Intent intent = new Intent(SignupActivity.this, AppFaceMainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // ──────────────────────────────────────────────
    //  Error Handling
    // ──────────────────────────────────────────────

    private void handleSignUpError(@NonNull String error) {
        switch (error) {
            case "EMAIL_EXISTS":
                showFieldError(emailContainer, tvEmailError, getString(R.string.signup_error_email_exists));
                break;
            case "WEAK_PASSWORD":
                showFieldError(passwordContainer, tvPasswordError, getString(R.string.signup_error_weak_password));
                break;
            case "INVALID_EMAIL":
                showFieldError(emailContainer, tvEmailError, getString(R.string.signup_error_invalid_email));
                break;
            default:
                showGeneralError(getString(R.string.signup_error_failed));
                break;
        }
    }

    // ──────────────────────────────────────────────
    //  UI Helpers
    // ──────────────────────────────────────────────

    private void setLoadingState(boolean loading) {
        isLoading = loading;
        if (loading) {
            btnSignUp.setText("");
            btnSignUp.setEnabled(false);
            btnSignUp.setAlpha(0.6f);
            signupProgress.setVisibility(View.VISIBLE);
        } else {
            btnSignUp.setText(getString(R.string.signup_button));
            btnSignUp.setEnabled(true);
            btnSignUp.setAlpha(1f);
            signupProgress.setVisibility(View.GONE);
        }
    }

    private void togglePassword(EditText editText, ImageView toggleBtn, boolean visible) {
        if (visible) {
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        } else {
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }
        editText.setSelection(editText.length());
        toggleBtn.setAlpha(visible ? 0.8f : 0.4f);
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
        clearFieldError(nameContainer, tvNameError);
        clearFieldError(emailContainer, tvEmailError);
        clearFieldError(passwordContainer, tvPasswordError);
        clearFieldError(confirmPasswordContainer, tvConfirmPasswordError);
        hideGeneralError();
    }
}
