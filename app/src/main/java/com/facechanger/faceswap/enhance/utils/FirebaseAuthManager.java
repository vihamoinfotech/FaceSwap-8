package com.facechanger.faceswap.enhance.utils;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/**
 * Singleton manager for Firebase Email/Password Authentication
 * and Realtime Database user profile operations.
 * <p>
 * Active only when APP_EXP == 1.
 * <p>
 * Database structure:
 * <pre>
 *   users/
 *     {firebaseUid}/
 *       fullName: "John Doe"
 *       email: "john@example.com"
 *       customUserId: "FS_A1B2C3D4"
 *       getToken: "..." (from SplashDataResponse)
 *       userId: "..." (from SplashDataResponse)
 *       deviceId: "..." (from SplashDataResponse)
 *       remainingLimit: 10.0 (from SplashDataResponse)
 *       createdAt: "2026-08-17T20:16:59"
 * </pre>
 */
public final class FirebaseAuthManager {

    private static final String TAG = "FirebaseAuthManager";
    private static final String DB_USERS_NODE = "users";
    private static final String CUSTOM_ID_PREFIX = "FS_";
    private static final int CUSTOM_ID_LENGTH = 8;

    private static final String[] DB_URL_CANDIDATES = new String[]{
            "https://faceswap-3-ed44c-default-rtdb.firebaseio.com",
            "https://faceswap-3-ed44c.firebaseio.com",
            "https://faceswap-3-ed44c-default-rtdb.asia-southeast1.firebasedatabase.app",
            "https://faceswap-3-ed44c-default-rtdb.europe-west1.firebasedatabase.app"
    };

    private static volatile FirebaseAuthManager instance;

    private FirebaseAuth firebaseAuth;
    private DatabaseReference usersRef;

    private FirebaseAuthManager() {
        try {
            firebaseAuth = FirebaseAuth.getInstance();
            usersRef = getDatabaseInstance().getReference(DB_USERS_NODE);
        } catch (Exception e) {
            Log.e(TAG, "Error initializing FirebaseAuth or FirebaseDatabase in constructor", e);
        }
    }

    @NonNull
    public static FirebaseAuthManager getInstance() {
        if (instance == null) {
            synchronized (FirebaseAuthManager.class) {
                if (instance == null) {
                    instance = new FirebaseAuthManager();
                }
            }
        }
        return instance;
    }

    @Nullable
    private FirebaseAuth getAuth() {
        if (firebaseAuth == null) {
            try {
                firebaseAuth = FirebaseAuth.getInstance();
            } catch (Exception e) {
                Log.e(TAG, "Error obtaining FirebaseAuth instance", e);
            }
        }
        return firebaseAuth;
    }

    @NonNull
    private FirebaseDatabase getDatabaseInstance() {
        try {
            return FirebaseDatabase.getInstance();
        } catch (Exception e) {
            Log.w(TAG, "Default FirebaseDatabase.getInstance() failed, trying candidates: " + e.getMessage());
            for (String url : DB_URL_CANDIDATES) {
                try {
                    return FirebaseDatabase.getInstance(url);
                } catch (Exception ignored) {
                }
            }
            return FirebaseDatabase.getInstance("https://faceswap-3-ed44c-default-rtdb.firebaseio.com");
        }
    }

    @Nullable
    private DatabaseReference getUsersRef() {
        if (usersRef == null) {
            try {
                usersRef = getDatabaseInstance().getReference(DB_USERS_NODE);
            } catch (Exception e) {
                Log.e(TAG, "Error obtaining DatabaseReference instance", e);
            }
        }
        return usersRef;
    }

    // ══════════════════════════════════════════════════
    //  Auth State
    // ══════════════════════════════════════════════════

    /**
     * @return true if a Firebase user is currently signed in
     */
    public boolean isLoggedIn() {
        FirebaseAuth auth = getAuth();
        return auth != null && auth.getCurrentUser() != null;
    }

    /**
     * @return The current Firebase user, or null if not signed in
     */
    @Nullable
    public FirebaseUser getCurrentUser() {
        FirebaseAuth auth = getAuth();
        return auth != null ? auth.getCurrentUser() : null;
    }

    /**
     * @return The Firebase UID of the current user, or empty string if not signed in
     */
    @NonNull
    public String getCurrentUid() {
        FirebaseUser user = getCurrentUser();
        return user != null ? user.getUid() : "";
    }

    // ══════════════════════════════════════════════════
    //  Custom User ID Generator
    // ══════════════════════════════════════════════════

    /**
     * Generates a unique custom User ID in the format FS_XXXXXXXX
     * (8 random alphanumeric characters).
     */
    @NonNull
    public static String generateCustomUserId() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder(CUSTOM_ID_PREFIX);
        for (int i = 0; i < CUSTOM_ID_LENGTH; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    // ══════════════════════════════════════════════════
    //  Sign Up
    // ══════════════════════════════════════════════════

    /**
     * Creates a new user with Email/Password, then writes the profile to Realtime Database.
     *
     * @param email          User email
     * @param password       User password
     * @param fullName       User's full name
     * @param customUserId   Generated custom user ID (FS_XXXXXXXX)
     * @param splashToken    Token from SplashDataResponse
     * @param splashUserId   UserId from SplashDataResponse
     * @param splashDeviceId DeviceId from SplashDataResponse
     * @param remainingLimit Remaining limit from SplashDataResponse
     * @param callback       Callback for success/failure
     */
    public void signUp(@NonNull String email, @NonNull String password,
                       @NonNull String fullName, @NonNull String customUserId,
                       @NonNull String splashToken, @NonNull String splashUserId,
                       @NonNull String splashDeviceId,
                       double remainingLimit,
                       @NonNull AuthCallback callback) {

        FirebaseAuth auth = getAuth();
        if (auth == null) {
            callback.onError("Firebase Auth is not available");
            return;
        }

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null
                            && task.getResult().getUser() != null) {
                        String uid = task.getResult().getUser().getUid();
                        Log.d(TAG, "Sign up successful in Firebase Auth. UID: " + uid);

                        // Write user profile to Realtime Database
                        Map<String, Object> userData = new HashMap<>();
                        userData.put("fullName", fullName != null ? fullName : "");
                        userData.put("email", email != null ? email : "");
                        userData.put("customUserId", customUserId != null ? customUserId : "");
                        userData.put("getToken", splashToken != null ? splashToken : "");
                        userData.put("userId", splashUserId != null ? splashUserId : "");
                        userData.put("deviceId", splashDeviceId != null ? splashDeviceId : "");
                        userData.put("remainingLimit", remainingLimit);
                        userData.put("createdAt", getCurrentTimestamp());

                        saveUserToAllDatabaseCandidates(uid, userData, callback);
                    } else {
                        Exception e = task.getException();
                        Log.e(TAG, "Sign up failed", e);
                        callback.onError(getSignUpErrorMessage(e));
                    }
                });
    }

    private void saveUserToAllDatabaseCandidates(@NonNull String uid, @NonNull Map<String, Object> userData, @NonNull AuthCallback callback) {
        DatabaseReference primaryRef = getUsersRef();
        if (primaryRef != null) {
            primaryRef.child(uid).setValue(userData)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "User profile successfully saved to primary Realtime Database node: users/" + uid);
                        callback.onSuccess();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Primary DB write failed, trying fallback URLs...", e);
                        tryFallbackUrls(uid, userData, callback, 0);
                    });
        } else {
            tryFallbackUrls(uid, userData, callback, 0);
        }
    }

    private void tryFallbackUrls(@NonNull String uid, @NonNull Map<String, Object> userData, @NonNull AuthCallback callback, int index) {
        if (index >= DB_URL_CANDIDATES.length) {
            Log.e(TAG, "All Realtime Database URL candidates failed to write.");
            callback.onSuccess();
            return;
        }

        String candidateUrl = DB_URL_CANDIDATES[index];
        try {
            DatabaseReference candidateRef = FirebaseDatabase.getInstance(candidateUrl).getReference(DB_USERS_NODE);
            candidateRef.child(uid).setValue(userData)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "User profile saved using fallback candidate URL: " + candidateUrl);
                        usersRef = candidateRef;
                        callback.onSuccess();
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "Candidate " + candidateUrl + " failed: " + e.getMessage());
                        tryFallbackUrls(uid, userData, callback, index + 1);
                    });
        } catch (Exception ex) {
            Log.w(TAG, "Candidate " + candidateUrl + " threw exception: " + ex.getMessage());
            tryFallbackUrls(uid, userData, callback, index + 1);
        }
    }

    // ══════════════════════════════════════════════════
    //  Login
    // ══════════════════════════════════════════════════

    /**
     * Signs in an existing user with Email/Password.
     *
     * @param email    User email
     * @param password User password
     * @param callback Callback for success/failure
     */
    public void login(@NonNull String email, @NonNull String password,
                      @NonNull AuthCallback callback) {

        FirebaseAuth auth = getAuth();
        if (auth == null) {
            callback.onError("Firebase Auth is not available");
            return;
        }

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Login successful");
                        callback.onSuccess();
                    } else {
                        Exception e = task.getException();
                        Log.e(TAG, "Login failed", e);
                        callback.onError(getLoginErrorMessage(e));
                    }
                });
    }

    // ══════════════════════════════════════════════════
    //  Profile Operations
    // ══════════════════════════════════════════════════

    /**
     * Fetches the current user's profile from Realtime Database.
     *
     * @param callback Callback with user data map or error
     */
    public void fetchUserProfile(@NonNull ProfileCallback callback) {
        FirebaseUser user = getCurrentUser();
        if (user == null) {
            callback.onError("No user is signed in");
            return;
        }

        DatabaseReference ref = getUsersRef();
        if (ref == null) {
            callback.onError("Database reference unavailable");
            return;
        }

        ref.child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Map<String, Object> data = new HashMap<>();
                    for (DataSnapshot child : snapshot.getChildren()) {
                        data.put(child.getKey(), child.getValue());
                    }
                    Log.d(TAG, "User profile fetched: " + data.keySet());
                    callback.onSuccess(data);
                } else {
                    Log.w(TAG, "No profile data found for UID: " + user.getUid());
                    callback.onError("Profile not found");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to fetch profile", error.toException());
                callback.onError(error.getMessage());
            }
        });
    }

    /**
     * Updates the user's full name in Realtime Database.
     *
     * @param newName  The new name
     * @param callback Callback for success/failure
     */
    public void updateUserName(@NonNull String newName, @NonNull AuthCallback callback) {
        FirebaseUser user = getCurrentUser();
        if (user == null) {
            callback.onError("No user is signed in");
            return;
        }

        DatabaseReference ref = getUsersRef();
        if (ref == null) {
            callback.onError("Database reference unavailable");
            return;
        }

        ref.child(user.getUid()).child("fullName").setValue(newName)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User name updated to: " + newName);
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update user name", e);
                    callback.onError(e.getMessage());
                });
    }

    public void updateUserCoin(@NonNull Double coin) {
        FirebaseUser user = getCurrentUser();
        if (user == null) {

            return;
        }

        DatabaseReference ref = getUsersRef();
        if (ref == null) {

            return;
        }

        ref.child(user.getUid()).child("remainingLimit").setValue(coin)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User name updated to: " + coin);

                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update user name", e);

                });
    }

    /**
     * Updates splash-related data in the user's Firebase profile.
     * Called when the user logs in and splash data is available.
     *
     * @param token          Token from SplashDataResponse
     * @param userId         UserId from SplashDataResponse
     * @param deviceId       DeviceId from SplashDataResponse
     * @param remainingLimit Remaining limit from SplashDataResponse
     */
    public void updateSplashData(@NonNull String token, @NonNull String userId,
                                 @NonNull String deviceId, double remainingLimit) {
        FirebaseUser user = getCurrentUser();
        if (user == null) return;

        DatabaseReference ref = getUsersRef();
        if (ref == null) return;

        Map<String, Object> updates = new HashMap<>();
        updates.put("getToken", token);
        updates.put("userId", userId);
        updates.put("deviceId", deviceId);
        updates.put("remainingLimit", remainingLimit);

        ref.child(user.getUid()).updateChildren(updates)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Splash data updated in Firebase"))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to update splash data", e));
    }

    // ══════════════════════════════════════════════════
    //  Logout
    // ══════════════════════════════════════════════════

    /**
     * Signs out the current user from Firebase Auth.
     */
    public void logout() {
        FirebaseAuth auth = getAuth();
        if (auth != null) {
            auth.signOut();
        }
        Log.d(TAG, "User signed out");
    }

    // ══════════════════════════════════════════════════
    //  Delete Account
    // ══════════════════════════════════════════════════

    /**
     * Re-authenticates the user before a sensitive operation (like account deletion).
     *
     * @param email    User email
     * @param password User password
     * @param callback Callback for success/failure
     */
    public void reAuthenticate(@NonNull String email, @NonNull String password,
                               @NonNull AuthCallback callback) {
        FirebaseUser user = getCurrentUser();
        if (user == null) {
            callback.onError("No user is signed in");
            return;
        }

        AuthCredential credential = EmailAuthProvider.getCredential(email, password);
        user.reauthenticate(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Re-authentication successful");
                        callback.onSuccess();
                    } else {
                        Log.e(TAG, "Re-authentication failed", task.getException());
                        callback.onError("Incorrect password");
                    }
                });
    }

    /**
     * Deletes the user's data from Realtime Database and then deletes the Firebase Auth account.
     * The user MUST be re-authenticated first for this to succeed.
     *
     * @param callback Callback for success/failure
     */
    public void deleteAccount(@NonNull AuthCallback callback) {
        FirebaseUser user = getCurrentUser();
        if (user == null) {
            callback.onError("No user is signed in");
            return;
        }

        String uid = user.getUid();
        DatabaseReference ref = getUsersRef();

        if (ref != null) {
            // First, delete user data from Realtime Database
            ref.child(uid).removeValue()
                    .addOnSuccessListener(aVoid -> {
                        // Then, delete the Firebase Auth account
                        user.delete()
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        Log.d(TAG, "Account deleted successfully");
                                        callback.onSuccess();
                                    } else {
                                        Log.e(TAG, "Failed to delete auth account", task.getException());
                                        callback.onError("Failed to delete account");
                                    }
                                });
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to delete user data from database", e);
                        callback.onError("Failed to delete account data");
                    });
        } else {
            user.delete()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            callback.onSuccess();
                        } else {
                            callback.onError("Failed to delete account");
                        }
                    });
        }
    }

    // ══════════════════════════════════════════════════
    //  Error Message Helpers
    // ══════════════════════════════════════════════════

    @NonNull
    private String getSignUpErrorMessage(@Nullable Exception e) {
        if (e instanceof FirebaseAuthUserCollisionException) {
            return "EMAIL_EXISTS";
        } else if (e instanceof FirebaseAuthWeakPasswordException) {
            return "WEAK_PASSWORD";
        } else if (e instanceof FirebaseAuthInvalidCredentialsException) {
            return "INVALID_EMAIL";
        }
        return e != null ? e.getMessage() : "Unknown error";
    }

    @NonNull
    private String getLoginErrorMessage(@Nullable Exception e) {
        if (e instanceof FirebaseAuthInvalidUserException) {
            return "USER_NOT_FOUND";
        } else if (e instanceof FirebaseAuthInvalidCredentialsException) {
            return "WRONG_PASSWORD";
        }
        String msg = e != null ? e.getMessage() : "";
        if (msg != null && msg.contains("INVALID_LOGIN_CREDENTIALS")) {
            return "WRONG_PASSWORD";
        }
        if (msg != null && msg.contains("TOO_MANY_REQUESTS")) {
            return "TOO_MANY_REQUESTS";
        }
        return msg != null ? msg : "Unknown error";
    }

    @NonNull
    private String getCurrentTimestamp() {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(new Date());
    }

    // ══════════════════════════════════════════════════
    //  Callbacks
    // ══════════════════════════════════════════════════

    public interface AuthCallback {
        void onSuccess();
        void onError(@NonNull String errorMessage);
    }

    public interface ProfileCallback {
        void onSuccess(@NonNull Map<String, Object> userData);
        void onError(@NonNull String errorMessage);
    }
}
