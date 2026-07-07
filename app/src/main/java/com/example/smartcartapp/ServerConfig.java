package com.example.smartcartapp;

public class ServerConfig {
    // HTTP API 서버 주소
    public static final String HTTP_BASE_URL = "http://192.168.0.7:5000";
    public static final String HTTP_BASE_URL_WITH_SLASH = HTTP_BASE_URL + "/";
    
    // MQTT 서버 주소
    public static final String MQTT_SERVER_URL = "tcp://192.168.0.7:1883";
    public static final String MQTT_SERVER_HOST = "192.168.0.7";
    public static final int MQTT_SERVER_PORT = 1883;
}