package com.group09.ComicReader;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.group09.ComicReader.databinding.ActivityMainBinding;
import com.group09.ComicReader.ui.reader.ReaderActivity;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private ActivityMainBinding binding;
    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        com.group09.ComicReader.data.ComicRepository.init(getApplicationContext());

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.fcv_main_nav_host);
        if (navHostFragment == null) {
            Log.e(TAG, "NavHostFragment is null. Skip nav setup.");
            return;
        }
        NavController navController = navHostFragment.getNavController();
        this.navController = navController;
        NavigationUI.setupWithNavController(binding.bnvMainTabs, navController);

        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            int destinationId = destination.getId();
            boolean showBottomNav = destinationId == R.id.homeFragment
                    || destinationId == R.id.searchFragment
                    || destinationId == R.id.browseFragment
                    || destinationId == R.id.rankingFragment
                    || destinationId == R.id.libraryFragment
                    || destinationId == R.id.profileFragment;
            binding.bnvMainTabs.setVisibility(showBottomNav ? View.VISIBLE : View.GONE);
        });

        handleIncomingIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIncomingIntent(intent);
    }

    private void handleIncomingIntent(Intent intent) {
        if (intent == null) {
            return;
        }

        Uri data = intent.getData();
        if (!Intent.ACTION_VIEW.equals(intent.getAction()) || data == null) {
            return;
        }

        if ("comicreader".equalsIgnoreCase(data.getScheme()) && "chapter".equalsIgnoreCase(data.getHost())) {
            Integer chapterId = parseFirstPathInt(data);
            if (chapterId == null || chapterId <= 0) {
                return;
            }

            Integer comicId = parseQueryInt(data, "comicId");
            Integer chapterNumber = parseQueryInt(data, "chapterNumber");
            startActivity(ReaderActivity.createDeepLinkIntent(
                    this,
                    comicId == null ? 0 : comicId,
                    chapterId,
                    chapterNumber == null ? 0 : chapterNumber));
            return;
        }

        if (navController != null) {
            navController.handleDeepLink(intent);
        }
    }

    private static Integer parseFirstPathInt(Uri uri) {
        if (uri == null || uri.getPathSegments() == null || uri.getPathSegments().isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(uri.getPathSegments().get(0));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static Integer parseQueryInt(Uri uri, String key) {
        if (uri == null || key == null) {
            return null;
        }
        String raw = uri.getQueryParameter(key);
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
