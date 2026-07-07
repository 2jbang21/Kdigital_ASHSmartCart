package com.example.smartcartapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.common.InputImage;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CartQRScanDialogFragment extends DialogFragment {
    public interface OnCartScannedListener {
        void onCartScanned(String cartId);
    }

    private OnCartScannedListener cartScannedListener;
    private PreviewView previewView;
    private BarcodeScanner scanner;
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private ExecutorService cameraExecutor;

    public void setOnCartScannedListener(OnCartScannedListener listener) {
        this.cartScannedListener = listener;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        cameraExecutor = Executors.newSingleThreadExecutor();
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (granted) startCamera();
                    else {
                        Toast.makeText(getContext(), "카메라 권한 필요", Toast.LENGTH_SHORT).show();
                        dismiss();
                    }
                });
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_cart_qr_scan, container,false);
        previewView = view.findViewById(R.id.previewView);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        scanner = BarcodeScanning.getClient();
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else requestPermissionLauncher.launch(Manifest.permission.CAMERA);
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(requireContext());
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                ImageAnalysis analysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                analysis.setAnalyzer(cameraExecutor, image -> {
                    @SuppressLint("UnsafeOptInUsageError")
                    InputImage inputImage = InputImage.fromMediaImage(
                            image.getImage(), image.getImageInfo().getRotationDegrees());
                    scanner.process(inputImage)
                            .addOnSuccessListener(this::handleBarcodes)
                            .addOnCompleteListener(t -> image.close());
                });

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this,
                        CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis);
            } catch (ExecutionException | InterruptedException e) {
                Log.e("CartQRScanner", "카메라 시작 오류", e);
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void handleBarcodes(List<Barcode> barcodes) {
        for (Barcode bc : barcodes) {
            String raw = bc.getRawValue();
            if (raw != null && raw.matches("\\d+")) {
                // 저장
                SharedPreferences prefs = requireContext()
                        .getSharedPreferences("cart_prefs", Context.MODE_PRIVATE);
                prefs.edit().putString("cart_id", raw).apply();

                if (cartScannedListener != null) {
                    cartScannedListener.onCartScanned(raw);
                }
                dismiss();
                break;
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (scanner != null) scanner.close();
        if (cameraExecutor != null) cameraExecutor.shutdown();
    }
}