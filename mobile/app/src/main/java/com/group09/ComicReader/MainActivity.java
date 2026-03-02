package com.group09.ComicReader;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;

import com.group09.ComicReader.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        NavController navController = Navigation.findNavController(this, R.id.fcv_main_nav_host);
        NavigationUI.setupWithNavController(binding.bnvMainTabs, navController);

        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            int destinationId = destination.getId();
            boolean showBottomNav = destinationId == R.id.homeFragment
                    || destinationId == R.id.searchFragment
                    || destinationId == R.id.libraryFragment
                    || destinationId == R.id.profileFragment;
            binding.bnvMainTabs.setVisibility(showBottomNav ? View.VISIBLE : View.GONE);
        });
    }
}
