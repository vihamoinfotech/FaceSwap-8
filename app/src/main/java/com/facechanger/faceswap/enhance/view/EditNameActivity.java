package com.facechanger.faceswap.enhance.view;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.facechanger.faceswap.enhance.R;
import com.facechanger.faceswap.enhance.utils.AppFaceTools;
import com.facechanger.faceswap.enhance.utils.FirebaseAuthManager;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.util.Map;

/**
 * Edit Name screen with matching Settings header and non-editable
 * user avatar initial circle icon in the header.
 */
public class EditNameActivity extends BaseAppActivity {

    public static final String EXTRA_CURRENT_NAME = "extra_current_name";
    public static final String EXTRA_CURRENT_EMAIL = "extra_current_email";

    private TextView tvHeroAvatarInitial, tvCurrentEmail;
    private EditText etFullName;
    private TextView tvNameError;
    private LinearLayout nameContainer;
    private TextView btnSave;
    private CircularProgressIndicator saveProgress;
    private ImageView btnClearName;

    private boolean isSaving = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_name);
        AppFaceTools.setStatusBarBleed(getWindow(), findViewById(R.id.root), false);

        initViews();
        setupData();
        setupListeners();
    }

    private void initViews() {
        tvHeroAvatarInitial = findViewById(R.id.tvHeroAvatarInitial);
        tvCurrentEmail = findViewById(R.id.tvCurrentEmail);
        etFullName = findViewById(R.id.etFullName);
        tvNameError = findViewById(R.id.tvNameError);
        nameContainer = findViewById(R.id.nameContainer);
        btnSave = findViewById(R.id.btnSave);
        saveProgress = findViewById(R.id.saveProgress);
        btnClearName = findViewById(R.id.btnClearName);

        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> onBackPressed());
        }
    }

    private void setupData() {
        String currentName = getIntent().getStringExtra(EXTRA_CURRENT_NAME);
        String currentEmail = getIntent().getStringExtra(EXTRA_CURRENT_EMAIL);

        if (currentName != null && !currentName.isEmpty()) {
            setAvatarInitial(currentName);
            etFullName.setText(currentName);
            etFullName.setSelection(etFullName.length());
        }

        if (currentEmail != null && !currentEmail.isEmpty()) {
            tvCurrentEmail.setText(currentEmail);
            tvCurrentEmail.setVisibility(View.VISIBLE);
        } else {
            tvCurrentEmail.setVisibility(View.GONE);
        }

        // Fetch fresh profile if intent extras were missing
        if (currentName == null || currentName.isEmpty()) {
            FirebaseAuthManager.getInstance().fetchUserProfile(new FirebaseAuthManager.ProfileCallback() {
                @Override
                public void onSuccess(@NonNull Map<String, Object> userData) {
                    runOnUiThread(() -> {
                        Object nameObj = userData.get("fullName");
                        Object emailObj = userData.get("email");
                        String name = nameObj != null ? String.valueOf(nameObj) : "User";
                        String email = emailObj != null ? String.valueOf(emailObj) : "";

                        setAvatarInitial(name);
                        etFullName.setText(name);
                        etFullName.setSelection(etFullName.length());

                        if (!email.isEmpty()) {
                            tvCurrentEmail.setText(email);
                            tvCurrentEmail.setVisibility(View.VISIBLE);
                        }
                    });
                }

                @Override
                public void onError(@NonNull String errorMessage) {
                    // Fallback to Firebase user email
                    runOnUiThread(() -> {
                        com.google.firebase.auth.FirebaseUser user =
                                FirebaseAuthManager.getInstance().getCurrentUser();
                        if (user != null && user.getEmail() != null) {
                            setAvatarInitial("U");
                            tvCurrentEmail.setText(user.getEmail());
                            tvCurrentEmail.setVisibility(View.VISIBLE);
                        }
                    });
                }
            });
        }
    }

    private void setAvatarInitial(@NonNull String name) {
        String initial = "U";
        if (!name.trim().isEmpty()) {
            initial = String.valueOf(name.trim().charAt(0)).toUpperCase();
        }
        if (tvHeroAvatarInitial != null) tvHeroAvatarInitial.setText(initial);
    }

    private void setupListeners() {
        btnSave.setOnClickListener(v -> {
            if (!isSaving) {
                attemptSave();
            }
        });

        btnClearName.setOnClickListener(v -> {
            etFullName.setText("");
            etFullName.requestFocus();
        });

        etFullName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                clearFieldError();
                btnClearName.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                // Update avatar initial in real-time
                if (s.length() > 0) {
                    setAvatarInitial(s.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });
    }

    private void attemptSave() {
        String newName = etFullName.getText().toString().trim();

        if (newName.isEmpty()) {
            showFieldError("Please enter your name");
            return;
        }

        if (newName.length() < 2) {
            showFieldError("Name must be at least 2 characters");
            return;
        }

        setSavingState(true);

        FirebaseAuthManager.getInstance().updateUserName(newName, new FirebaseAuthManager.AuthCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    setSavingState(false);
                    Toast.makeText(EditNameActivity.this,
                            "Name updated successfully",
                            Toast.LENGTH_SHORT).show();
                    finish();
                });
            }

            @Override
            public void onError(@NonNull String errorMessage) {
                runOnUiThread(() -> {
                    setSavingState(false);
                    Toast.makeText(EditNameActivity.this,
                            "Failed to update name. Please try again.",
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void setSavingState(boolean saving) {
        isSaving = saving;
        if (saving) {
            btnSave.setText("");
            btnSave.setEnabled(false);
            btnSave.setAlpha(0.6f);
            saveProgress.setVisibility(View.VISIBLE);
        } else {
            btnSave.setText("Save");
            btnSave.setEnabled(true);
            btnSave.setAlpha(1f);
            saveProgress.setVisibility(View.GONE);
        }
    }

    private void showFieldError(String message) {
        nameContainer.setBackgroundResource(R.drawable.bg_input_field_error);
        tvNameError.setText(message);
        tvNameError.setVisibility(View.VISIBLE);
    }

    private void clearFieldError() {
        nameContainer.setBackgroundResource(R.drawable.bg_input_field);
        tvNameError.setVisibility(View.GONE);
    }
}
