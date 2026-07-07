package com.example.smartcartapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.smartcartapp.ServerConfig;
import com.google.android.material.textfield.TextInputEditText;

import java.util.UUID;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import android.util.Log;

public class LoginActivity extends AppCompatActivity
        implements CartQRScanDialogFragment.OnCartScannedListener {

    private RadioGroup genderGroup;
    private TextInputEditText birthInput;
    private Button loginButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        if (prefs.getBoolean("is_logged_in", false)) {
            startMainActivity();
            return;
        }

        genderGroup = findViewById(R.id.gender_group);
        birthInput = findViewById(R.id.birth_input);
        loginButton = findViewById(R.id.login_button);

        loginButton.setOnClickListener(v -> {
            if (!validateUserInfo()) return;

            // 사용자 정보 저장 및 임의 ID 생성
            saveUserInfo();
            
            loginButton.setText("카트 QR 코드를 스캔하세요");
            loginButton.setOnClickListener(view -> {
                CartQRScanDialogFragment dialog = new CartQRScanDialogFragment();
                dialog.setOnCartScannedListener(this);
                dialog.show(getSupportFragmentManager(), "CartQRScanDialog");
            });
            Toast.makeText(this, "이제 카트 QR 코드를 스캔해주세요", Toast.LENGTH_LONG).show();
        });
    }

    private boolean validateUserInfo() {
        String birthDate = birthInput.getText().toString().trim();
        int selectedGenderId = genderGroup.getCheckedRadioButtonId();

        if (selectedGenderId == -1) {
            Toast.makeText(this, "성별을 선택해주세요", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (TextUtils.isEmpty(birthDate) || birthDate.length() < 8) {
            Toast.makeText(this, "올바른 생년월일을 입력해주세요", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    @Override
    public void onCartScanned(String cartId) {
        activateCart(cartId);
    }

    private void saveUserInfo() {
        String birthDate = birthInput.getText().toString().trim();
        int selectedGenderId = genderGroup.getCheckedRadioButtonId();
        String gender = (selectedGenderId == R.id.male) ? "M" : "F";
        
        // 고유한 사용자 ID 생성 (UUID 기반)
        String userId = "USER_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("user_id", userId);
        editor.putString("gender", gender);
        editor.putString("birth_date", birthDate);
        editor.apply();
    }
    
    private void activateCart(String cartId) {
        int cartNum = 1;
        try {
            cartNum = Integer.parseInt(cartId.replaceAll("\\D+", ""));
        } catch (Exception ignored) { }
        CartBinder.sendCartNumber(cartNum);

        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("is_logged_in", true);
        editor.putInt("cart_number", cartNum);
        editor.putString("cart_id", cartId);
        editor.apply();

        String userId = prefs.getString("user_id", "Unknown");
        String gender = prefs.getString("gender", "M");
        String birthDate = prefs.getString("birth_date", "1990-01-01");
        
        sendMqttMessage(userId, gender, birthDate, cartId);
        
        Toast.makeText(this, "카트 연동 완료 (ID: " + userId + ")", Toast.LENGTH_SHORT).show();
        startMainActivity();
    }

    private void sendMqttMessage(String userId, String gender, String birthDate, String cartId) {
        new Thread(() -> {
            try {
                Log.d("MQTT", "LoginActivity에서 MQTT 메시지 전송 시도...");
                
                MqttClient mqttClient = new MqttClient(ServerConfig.MQTT_SERVER_URL,
                    "login_" + System.currentTimeMillis(), new MemoryPersistence());
                mqttClient.connect();
                
                // 나이 계산
                int age = calculateAge(birthDate);
                String ageGroup = getAgeGroup(age);
                String genderKor = gender.equals("M") ? "남성" : "여성";
                
                String payload = String.format(
                    "{\"cmd\":\"start\",\"customer_id\":\"%s\",\"gender\":\"%s\",\"age_group\":\"%s\",\"cart_id\":\"%s\",\"age\":%d}",
                        userId, genderKor, ageGroup, cartId, age
                );
                
                MqttMessage message = new MqttMessage(payload.getBytes());
                message.setQos(1);
                mqttClient.publish("smartcart/3/control", message);
                
                Log.d("MQTT", "MQTT 메시지 전송 성공: " + payload);
                
                mqttClient.disconnect();
                mqttClient.close();
                
            } catch (Exception e) {
                Log.e("MQTT", "MQTT 메시지 전송 실패: " + e.getMessage(), e);
            }
        }).start();
    }
    
    private int calculateAge(String birthDate) {
        try {
            String[] parts = birthDate.split("-");
            int birthYear = Integer.parseInt(parts[0]);
            int currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);
            return currentYear - birthYear;
        } catch (Exception e) {
            return 25;
        }
    }
    
    private String getAgeGroup(int age) {
        if (age < 20) return "10대";
        else if (age < 30) return "20대";
        else if (age < 40) return "30대";
        else if (age < 50) return "40대";
        else if (age < 60) return "50대";
        else return "60대 이상";
    }

    private void startMainActivity() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}