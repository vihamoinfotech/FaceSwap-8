package com.facechanger.faceswap.enhance.view;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.facechanger.faceswap.enhance.R;
import com.facechanger.faceswap.enhance.utils.AppFaceLocaleHelper;
import com.facechanger.faceswap.enhance.utils.AppFaceTools;

/**
 * Forced app update screen shown when a newer version is available on Google Play.
 * <p>
 * This screen is displayed BEFORE the splash data is fetched and has no close
 * or cancel button — the user must update to continue using the app.
 */
public class AppFaceAppUpdateActivity extends AppCompatActivity {

    public static final String EXTRA_CURRENT_VERSION = "current_version";
    public static final String EXTRA_LATEST_VERSION  = "latest_version";

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(AppFaceLocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_face_activity_app_update_screen);
        AppFaceTools.setEdgetoEdge(getWindow(), findViewById(R.id.appUpdateContent), false, true);

        String currentVersion = getIntent().getStringExtra(EXTRA_CURRENT_VERSION);
        String latestVersion  = getIntent().getStringExtra(EXTRA_LATEST_VERSION);

        // Show version info
        TextView tvVersionInfo = findViewById(R.id.tvVersionInfo);
        if (currentVersion != null && latestVersion != null) {
            tvVersionInfo.setText("Your version: " + currentVersion + "  •  Latest: " + latestVersion);
        }

        // Update button → open Play Store
        Button btnUpdateNow = findViewById(R.id.btnUpdateNow);
        btnUpdateNow.setOnClickListener(v -> openPlayStore());
    }

    /** Disable back press — forced update cannot be dismissed. */
    @Override
    public void onBackPressed() {
        // Intentionally empty — user must update
    }

    /**
     * Opens the Google Play Store listing for this app.
     * Falls back to the web browser if the Play Store app is not available.
     */
    private void openPlayStore() {
        String packageName = getPackageName();

        // Try native Play Store app first
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=" + packageName));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            // Fallback to web browser
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=" + packageName));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }
}
