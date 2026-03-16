package com.group09.ComicReader;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.group09.ComicReader.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        NavHostFragment navHostFragment =
                (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.fcv_main_nav_host);
        if (navHostFragment == null) {
            Log.e(TAG, "NavHostFragment is null. Skip nav setup.");
            return;
        }
        NavController navController = navHostFragment.getNavController();
        NavigationUI.setupWithNavController(binding.bnvMainTabs, navController);

        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            int destinationId = destination.getId();
            boolean showBottomNav = destinationId == R.id.homeFragment
                    || destinationId == R.id.searchFragment
                    || destinationId == R.id.rankingFragment
                    || destinationId == R.id.libraryFragment
                    || destinationId == R.id.profileFragment;
            binding.bnvMainTabs.setVisibility(showBottomNav ? View.VISIBLE : View.GONE);
        });
    }
}
