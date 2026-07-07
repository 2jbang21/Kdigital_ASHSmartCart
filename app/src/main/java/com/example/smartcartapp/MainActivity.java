package com.example.smartcartapp;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.smartcartapp.ui.cart.CartFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.appcompat.app.AlertDialog;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // (선택) 전체화면/내비 숨김
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );

        // WindowInsets 처리
        View root = findViewById(R.id.main);
        if (root != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                return insets;
            });
        }

        // 하단 네비게이션 처리
        BottomNavigationView navView = findViewById(R.id.bottom_navigation);
        NavHostFragment navHostFragment =
                (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        if (navView != null && navHostFragment != null) {
            AppBarConfiguration appBarConfiguration =
                    new AppBarConfiguration.Builder(R.id.navigation_home, R.id.navigation_cart).build();
            NavController navController = navHostFragment.getNavController();
            NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
            NavigationUI.setupWithNavController(navView, navController);
        }

        MqttCartClient.get().start(getApplicationContext());
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        MqttCartClient.get().stop();
    }

    // 옵션 메뉴(있으면 사용)
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            logout();
            return true;
        } else if (item.getItemId() == R.id.action_user_info) {
            showUserInfoDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showUserInfoDialog() {
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String userId = prefs.getString("user_id", "미설정");
        String gender = prefs.getString("gender", "미설정");
        String birthDate = prefs.getString("birth_date", "미설정");
        String cartId = prefs.getString("cart_id", "미설정");
        
        String userInfo = "사용자 ID: " + userId + "\n" +
                         "성별: " + (gender.equals("M") ? "남성" : "여성") + "\n" +
                         "생년월일: " + birthDate + "\n" +
                         "카트 번호: " + cartId;
        
        new AlertDialog.Builder(this)
                .setTitle("사용자 정보")
                .setMessage(userInfo)
                .setPositiveButton("확인", null)
                .show();
    }

    private void logout() {
        SharedPreferences sp = getSharedPreferences("user_prefs", MODE_PRIVATE);
        sp.edit().putBoolean("is_logged_in", false).apply();
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    public void refreshCartFragment() {
        NavHostFragment host =
                (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        if (host != null) {
            for (androidx.fragment.app.Fragment f : host.getChildFragmentManager().getFragments()) {
                if (f instanceof CartFragment) {
                    ((CartFragment) f).refreshCartList();
                    break;
                }
            }
        }
    }
}
