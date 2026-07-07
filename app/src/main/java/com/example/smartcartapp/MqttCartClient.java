package com.example.smartcartapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.example.smartcartapp.ServerConfig;
import com.example.smartcartapp.api.RetrofitClient;
import com.example.smartcartapp.model.Product;
import com.example.smartcartapp.utils.CartManager;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.POST;

public class MqttCartClient {
    private static MqttCartClient I;
    public static MqttCartClient get(){ if(I==null) I=new MqttCartClient(); return I; }

    private final Handler ui = new Handler(Looper.getMainLooper());
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private MqttClient client;
    public String broker = ServerConfig.MQTT_SERVER_URL;

    private final ConcurrentHashMap<String, Long> lastSeen = new ConcurrentHashMap<>();
    private static final long DEDUPE_MS = 1500;

    interface ScanApi { @POST("/api/scan") Call<ScanResp> scan(@Body Map<String,Object> body); }
    public static class ScanResp { public boolean ok; public Integer product_id; public String barcode,name,category,error; public Double price; public Integer qty; }

    private int resolveCartNumber(SharedPreferences sp){
        int n = sp.getInt("cart_number", -1);
        if (n > 0) return n;
        String cid = sp.getString("cart_id", null);
        if (cid != null) {
            String digits = cid.replaceAll("\\D+","");
            if (!digits.isEmpty()) {
                try { return Integer.parseInt(digits); } catch (Exception ignore){}
            }
        }
        return 1;
    }

    public void start(Context appCtx){
        SharedPreferences sp = appCtx.getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        int cartNum = resolveCartNumber(sp);
        String topic = "smartcart/cart/" + cartNum + "/scan";
        String clientId = "android-cart-" + cartNum + "-" + System.currentTimeMillis();

        io.execute(() -> {
            try{
                client = new MqttClient(broker, clientId, null);
                MqttConnectOptions opt = new MqttConnectOptions();
                opt.setAutomaticReconnect(true);
                opt.setCleanSession(true);
                opt.setKeepAliveInterval(20);

                client.setCallback(new MqttCallback() {
                    @Override public void connectionLost(Throwable cause){ Log.e("MQTT","lost", cause); }
                    @Override public void messageArrived(String t, MqttMessage msg){
                        String json = new String(msg.getPayload());
                        String barcode = json.replaceAll(".*\"barcode\"\\s*:\\s*\"([^\"]+)\".*", "$1");
                        int qty = 1;
                        try { qty = Integer.parseInt(json.replaceAll(".*\"qty\"\\s*:\\s*(\\d+).*", "$1")); } catch(Exception ignore){}

                        long now = System.currentTimeMillis();
                        Long prev = lastSeen.get(barcode);
                        if (prev != null && (now - prev) < DEDUPE_MS) { Log.d("MQTT","dup ignored: "+barcode); return; }
                        lastSeen.put(barcode, now);

                        fetchAndAdd(appCtx, barcode, Math.max(1, qty));
                    }
                    @Override public void deliveryComplete(IMqttDeliveryToken token){}
                });

                client.connect(opt);
                client.subscribe(topic, 1);
                Log.d("MQTT","connected, sub "+topic);

                try {
                    String j = "{\"cart_number\":"+cartNum+"}";
                    client.publish("smartcart/cart/set", new MqttMessage(j.getBytes()));
                    Log.d("MQTT","sent cart_number broadcast: "+j);
                } catch(Exception ignore){}

            } catch (Exception e){
                Log.e("MQTT","connect/subscribe failed", e);
            }
        });
    }

    public void stop(){
        io.execute(() -> {
            try{ if(client!=null && client.isConnected()) client.disconnect(); }catch(Exception ignore){}
            try{ if(client!=null) client.close(); }catch(Exception ignore){}
            client=null;
        });
    }

    private void fetchAndAdd(Context ctx, String barcode, int qty){
        retrofit2.Retrofit retrofit = new retrofit2.Retrofit.Builder()
                .baseUrl(ServerConfig.HTTP_BASE_URL_WITH_SLASH)
                .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
                .build();
        ScanApi api = retrofit.create(ScanApi.class);
        Map<String,Object> body = new HashMap<>();
        body.put("barcode", barcode);
        body.put("qty", qty);

        api.scan(body).enqueue(new Callback<ScanResp>() {
            @Override public void onResponse(Call<ScanResp> call, Response<ScanResp> resp){
                if(!resp.isSuccessful() || resp.body()==null){ toast(ctx,"상품 조회 실패("+resp.code()+")"); return; }
                ScanResp r = resp.body();
                if(!r.ok){ toast(ctx,"미등록 바코드: "+barcode); return; }

                int id = (r.product_id!=null)? r.product_id : barcode.hashCode();
                int price = (r.price!=null)? (int)Math.round(r.price) : 0;
                String name = (r.name!=null)? r.name : "상품";
                String category = (r.category!=null)? r.category : "기타";

                Product p = new Product(id, name, price, category);
                for(int k=0;k<Math.max(1,qty);k++) CartManager.getInstance().addItem(p);
                toast(ctx, name+" 담김 (x"+qty+")");
            }
            @Override public void onFailure(Call<ScanResp> call, Throwable t){ toast(ctx,"네트워크 오류: "+t.getMessage()); }
        });
    }

    private void toast(Context ctx, String s){
        ui.post(() -> android.widget.Toast.makeText(ctx, s, android.widget.Toast.LENGTH_SHORT).show());
    }
}