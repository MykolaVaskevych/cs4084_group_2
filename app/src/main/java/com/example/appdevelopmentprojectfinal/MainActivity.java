package com.example.appdevelopmentprojectfinal;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.appdevelopmentprojectfinal.databinding.ActivityMainBinding;

import java.sql.Time;

public class MainActivity extends AppCompatActivity {
    ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
//        We will set here the listener to send from the bottom navigation bar items (icons) to the appropriate fragment.
        binding.bottomNavigationView.setOnItemSelectedListener(item -> {
            switch(item.getItemId()){
                case R.id.market:
                    replaceFragment(new MarketFragment());
                    break;
                case R.id.home:
                    replaceFragment(new HomeFragment());
                    break;
                case R.id.timetable:
                    replaceFragment(new TimetableFragment());
                    break;
                case R.id.profile:
                    replaceFragment(new ProfileFragment());
                    break;
            }
            return true;
        });
    }
//    the process of changing of the fragment described here
    private void replaceFragment(Fragment fragment){
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.frame_layout, fragment);
        fragmentTransaction.commit();

    }
}