package com.example.smartcartapp;

import android.content.Context;
import android.util.Log;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class CartBinder {
    private static final String BROKER = ServerConfig.MQTT_SERVER_URL;

    public static void sendCartNumber(int cartNumber) {
        sendJson("smartcart/cart/set", "{\"cart_number\":" + cartNumber + "}");
    }

    public static void sendCartNumberToPi(String piId, int cartNumber) {
        sendJson("smartcart/pi/" + piId + "/set_cart", "{\"cart_number\":" + cartNumber + "}");
    }

    private static void sendJson(String topic, String json) {
        new Thread(() -> {
            try {
                String cid = "android-binder-" + System.currentTimeMillis();
                MqttClient c = new MqttClient(BROKER, cid, null);
                MqttConnectOptions opt = new MqttConnectOptions();
                opt.setAutomaticReconnect(false);
                opt.setCleanSession(true);
                c.connect(opt);
                MqttMessage m = new MqttMessage(json.getBytes());
                m.setQos(1);
                c.publish(topic, m);
                c.disconnect();
                c.close();
                Log.d("CartBinder","PUB " + topic + " " + json);
            } catch (Exception e) {
                Log.e("CartBinder","publish error", e);
            }
        }).start();
    }
}