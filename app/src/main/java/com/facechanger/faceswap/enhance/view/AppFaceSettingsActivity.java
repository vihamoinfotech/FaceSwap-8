package com.facechanger.faceswap.enhance.view;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.facechanger.faceswap.enhance.utils.AppFaceAppSystem;
import com.facechanger.faceswap.enhance.utils.AppFaceLocaleHelper;
import com.facechanger.faceswap.enhance.utils.AppFaceStaticValue;
import com.facechanger.faceswap.enhance.utils.AppFaceTools;
import com.facechanger.faceswap.enhance.R;
import com.facechanger.faceswap.enhance.utils.FirebaseAuthManager;
import com.faceenhance.facechanger.Utils.GlobleMMKVManager;
import com.faceenhance.facechanger.controller.AdManager;
import com.google.firebase.auth.FirebaseUser;

import java.util.Map;

public class AppFaceSettingsActivity extends BaseAppActivity {

    private static final String TAG = "SettingsActivity";

    private View sectionAccount, accountGroup;
    private TextView tvProfileName, tvProfileEmail, tvAvatarInitial;

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(AppFaceLocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_face_activity_settings_screen);
        AppFaceTools.setStatusBarBleed(getWindow(), findViewById(R.id.root), false);

        View rootView = findViewById(android.R.id.content);
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                rootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                loadAds();
            }
        });

        findViewById(R.id.coinsGroup).setVisibility(View.GONE);
        refreshAccountProfile();

        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> onBackPressed());
        }

        findViewById(R.id.llLanguage).setOnClickListener(v -> {
            Intent intent = new Intent(this, AppFaceLanguageActivity.class);
            intent.putExtra(AppFaceLanguageActivity.EXTRA_FROM_SETTINGS, true);
            startActivity(intent);
        });

        findViewById(R.id.llFeedback).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:support@vihamo.com"));
            intent.putExtra(Intent.EXTRA_SUBJECT, "Feedback for FaceSwap");
            try {
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(this, getString(R.string.app_face_no_email_app_found_text), Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.llShareApp).setOnClickListener(v -> {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_builder_name));
            String shareMessage = "Check out this amazing app: \nhttps://play.google.com/store/apps/details?id=" + getPackageName();
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);
            startActivity(Intent.createChooser(shareIntent, "Share via"));
        });

        findViewById(R.id.llRateApp).setOnClickListener(v -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + getPackageName())));
            } catch (ActivityNotFoundException e) {
                Toast.makeText(this, getString(R.string.app_open_coming_soon_text), Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.llTerms).setOnClickListener(v -> openBrowser("https://resumebuilder-2.blogspot.com/2026/06/terms-faceswap-2.html"));

        findViewById(R.id.llPrivacy).setOnClickListener(v -> openBrowser("https://resumebuilder-2.blogspot.com/2026/06/privacy-faceswap-2.html"));

        // Coins Section
        findViewById(R.id.llBuyCoins).setOnClickListener(v -> {
            startActivity(new Intent(this, AppFacePaywallActivity.class));
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshAccountProfile();
    }

    private void openBrowser(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, getString(R.string.app_open_no_browser_found_text), Toast.LENGTH_SHORT).show();
        }
    }

    private void setupAccountSection() {
        sectionAccount = findViewById(R.id.sectionAccount);
        accountGroup = findViewById(R.id.accountGroup);
        tvProfileName = findViewById(R.id.tvProfileName);
        tvProfileEmail = findViewById(R.id.tvProfileEmail);
        tvAvatarInitial = findViewById(R.id.tvAvatarInitial);

        int APP_EXP = GlobleMMKVManager.getInstance().getInt(AppFaceStaticValue.APP_EXP, 1);

        if (APP_EXP != 1 || !FirebaseAuthManager.getInstance().isLoggedIn()) {
            // Hide account section when APP_EXP != 1 or user not logged in
            if (sectionAccount != null) sectionAccount.setVisibility(View.GONE);
            if (accountGroup != null) accountGroup.setVisibility(View.GONE);
            if (AdManager.getInstance().isPremiumUser()) {
                findViewById(R.id.coinsGroup).setVisibility(View.GONE);
            } else {
                findViewById(R.id.coinsGroup).setVisibility(View.VISIBLE);
            }
            return;
        }

        // Show account section
        if (sectionAccount != null) sectionAccount.setVisibility(View.VISIBLE);
        if (accountGroup != null) accountGroup.setVisibility(View.VISIBLE);
        findViewById(R.id.coinsGroup).setVisibility(View.GONE);

        // Edit Name
        View llEditName = findViewById(R.id.llEditName);
        if (llEditName != null) {
            llEditName.setOnClickListener(v -> openEditNameScreen());
        }

        // Logout
        View llLogout = findViewById(R.id.llLogout);
        if (llLogout != null) {
            llLogout.setOnClickListener(v -> showLogoutConfirmDialog());
        }

        // Delete Account
        View llDeleteAccount = findViewById(R.id.llDeleteAccount);
        if (llDeleteAccount != null) {
            llDeleteAccount.setOnClickListener(v -> showDeleteAccountConfirmDialog());
        }
    }

    private void openEditNameScreen() {
        Intent intent = new Intent(this, EditNameActivity.class);
        if (tvProfileName != null && tvProfileName.getText() != null) {
            intent.putExtra(EditNameActivity.EXTRA_CURRENT_NAME, tvProfileName.getText().toString());
        }
        if (tvProfileEmail != null && tvProfileEmail.getText() != null) {
            intent.putExtra(EditNameActivity.EXTRA_CURRENT_EMAIL, tvProfileEmail.getText().toString());
        }
        startActivity(intent);
    }

    private void refreshAccountProfile() {
        int APP_EXP = GlobleMMKVManager.getInstance().getInt(AppFaceStaticValue.APP_EXP, 1);
        if (APP_EXP != 1 || !FirebaseAuthManager.getInstance().isLoggedIn()){
            setupAccountSection();
            return;
        }

        FirebaseAuthManager.getInstance().fetchUserProfile(new FirebaseAuthManager.ProfileCallback() {
            @Override
            public void onSuccess(@NonNull Map<String, Object> userData) {
                runOnUiThread(() -> {
                    setupAccountSection();
                    Object nameObj = userData.get("fullName");
                    Object emailObj = userData.get("email");
                    String name = nameObj != null ? String.valueOf(nameObj) : "User";
                    String email = emailObj != null ? String.valueOf(emailObj) : "";

                    if (tvProfileName != null) tvProfileName.setText(name);
                    if (tvProfileEmail != null) tvProfileEmail.setText(email);
                    if (tvAvatarInitial != null && !name.isEmpty()) {
                        tvAvatarInitial.setText(String.valueOf(name.charAt(0)).toUpperCase());
                    }
                });
            }

            @Override
            public void onError(@NonNull String errorMessage) {
                Log.w(TAG, "Failed to load profile: " + errorMessage);
                runOnUiThread(() -> {
                    setupAccountSection();
                    // Show Firebase email as fallback
                    FirebaseUser user =
                            FirebaseAuthManager.getInstance().getCurrentUser();
                    if (user != null) {
                        if (tvProfileEmail != null) tvProfileEmail.setText(user.getEmail());
                        if (tvProfileName != null) tvProfileName.setText("User");
                        if (tvAvatarInitial != null) tvAvatarInitial.setText("U");
                    }
                });
            }
        });
    }

    // ──────────────────────────────────────────────
    //  Logout Dialog (Popup Alert Style)
    // ──────────────────────────────────────────────

    private void showLogoutConfirmDialog() {
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_logout_confirm);
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);

        setupCustomDialogWindow(dialog);

        TextView btnConfirm = dialog.findViewById(R.id.btnLogoutConfirm);
        TextView btnCancel = dialog.findViewById(R.id.btnLogoutCancel);

        btnConfirm.setOnClickListener(v -> {
            dialog.dismiss();
            FirebaseAuthManager.getInstance().logout();
            GlobleMMKVManager.getInstance().putString(AppFaceStaticValue.FIREBASE_DEVICE_ID, "");
            GlobleMMKVManager.getInstance().putString(AppFaceStaticValue.FIREBASE_USER_ID, "");
            GlobleMMKVManager.getInstance().putInt(AppFaceStaticValue.APP_EXP, 1);
            navigateToLogin();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        safeShowDialog(dialog);
    }

    // ──────────────────────────────────────────────
    //  Delete Account Dialog (Popup Alert Style)
    // ──────────────────────────────────────────────

    private void showDeleteAccountConfirmDialog() {
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_delete_account_confirm);
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);

        setupCustomDialogWindow(dialog);

        TextView btnDeleteConfirm = dialog.findViewById(R.id.btnDeleteConfirm);
        TextView btnDeleteCancel = dialog.findViewById(R.id.btnDeleteCancel);

        btnDeleteConfirm.setOnClickListener(v -> {
            dialog.dismiss();
            showReAuthDialog();
        });

        btnDeleteCancel.setOnClickListener(v -> dialog.dismiss());

        safeShowDialog(dialog);
    }

    private void setupCustomDialogWindow(@NonNull android.app.Dialog dialog) {
        android.view.Window window = dialog.getWindow();
        if (window == null) return;

        window.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        window.setLayout(
                android.view.WindowManager.LayoutParams.MATCH_PARENT,
                android.view.WindowManager.LayoutParams.WRAP_CONTENT
        );
        window.setGravity(android.view.Gravity.CENTER);

        android.view.WindowManager.LayoutParams params = window.getAttributes();
        params.dimAmount = 0.6f;
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        window.setAttributes(params);
    }

    private void safeShowDialog(@NonNull android.app.Dialog dialog) {
        if (isFinishing() || isDestroyed()) return;
        try {
            dialog.show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing dialog", e);
        }
    }

    private void showReAuthDialog() {
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_delete_reauth);
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);

        setupCustomDialogWindow(dialog);

        EditText etPassword = dialog.findViewById(R.id.etPassword);
        TextView tvPasswordError = dialog.findViewById(R.id.tvPasswordError);
        LinearLayout passwordContainer = dialog.findViewById(R.id.passwordContainer);
        ImageView btnTogglePassword = dialog.findViewById(R.id.btnTogglePassword);
        TextView btnConfirmDelete = dialog.findViewById(R.id.btnConfirmDelete);
        TextView btnCancel = dialog.findViewById(R.id.btnCancel);
        View deleteProgress = dialog.findViewById(R.id.deleteProgress);

        final boolean[] isPasswordVisible = {false};
        btnTogglePassword.setOnClickListener(v -> {
            isPasswordVisible[0] = !isPasswordVisible[0];
            if (isPasswordVisible[0]) {
                etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            } else {
                etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            }
            etPassword.setSelection(etPassword.length());
            btnTogglePassword.setAlpha(isPasswordVisible[0] ? 0.8f : 0.4f);
        });

        etPassword.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                passwordContainer.setBackgroundResource(R.drawable.bg_input_field);
                tvPasswordError.setVisibility(View.GONE);
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });

        btnConfirmDelete.setOnClickListener(v -> {
            String password = etPassword.getText().toString().trim();
            if (password.isEmpty()) {
                passwordContainer.setBackgroundResource(R.drawable.bg_input_field_error);
                tvPasswordError.setText(getString(R.string.login_error_empty_password));
                tvPasswordError.setVisibility(View.VISIBLE);
                return;
            }

            btnConfirmDelete.setText("");
            btnConfirmDelete.setEnabled(false);
            deleteProgress.setVisibility(View.VISIBLE);
            etPassword.setEnabled(false);
            btnCancel.setEnabled(false);

            FirebaseUser user = FirebaseAuthManager.getInstance().getCurrentUser();
            String email = user != null && user.getEmail() != null ? user.getEmail() : "";

            // Re-authenticate first, then delete
            FirebaseAuthManager.getInstance().reAuthenticate(email, password, new FirebaseAuthManager.AuthCallback() {
                @Override
                public void onSuccess() {
                    FirebaseAuthManager.getInstance().deleteAccount(new FirebaseAuthManager.AuthCallback() {
                        @Override
                        public void onSuccess() {
                            runOnUiThread(() -> {
                                dialog.dismiss();
                                Toast.makeText(AppFaceSettingsActivity.this,
                                        getString(R.string.settings_account_deleted),
                                        Toast.LENGTH_SHORT).show();
                                navigateToLogin();
                            });
                        }

                        @Override
                        public void onError(@NonNull String errorMessage) {
                            runOnUiThread(() -> {
                                btnConfirmDelete.setText(getString(R.string.settings_delete_account));
                                btnConfirmDelete.setEnabled(true);
                                deleteProgress.setVisibility(View.GONE);
                                etPassword.setEnabled(true);
                                btnCancel.setEnabled(true);
                                Toast.makeText(AppFaceSettingsActivity.this,
                                        getString(R.string.settings_error_delete),
                                        Toast.LENGTH_SHORT).show();
                            });
                        }
                    });
                }

                @Override
                public void onError(@NonNull String errorMessage) {
                    runOnUiThread(() -> {
                        btnConfirmDelete.setText(getString(R.string.settings_delete_account));
                        btnConfirmDelete.setEnabled(true);
                        deleteProgress.setVisibility(View.GONE);
                        etPassword.setEnabled(true);
                        btnCancel.setEnabled(true);
                        passwordContainer.setBackgroundResource(R.drawable.bg_input_field_error);
                        tvPasswordError.setText(getString(R.string.settings_error_reauth));
                        tvPasswordError.setVisibility(View.VISIBLE);
                    });
                }
            });
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        safeShowDialog(dialog);
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }



}
