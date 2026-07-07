package com.example.smartcartapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
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
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import com.example.smartcartapp.model.Product;
import com.example.smartcartapp.utils.CartManager;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.util.concurrent.ExecutionException;

public class QRScanDialogFragment extends DialogFragment {

    private PreviewView previewView;
    private BarcodeScanner scanner;
    private CartManager cartManager;
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private ProcessCameraProvider cameraProvider;
    private boolean isScanning = true;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        startCamera();
                    } else {
                        Toast.makeText(requireContext(), "카메라 권한이 필요합니다", Toast.LENGTH_SHORT).show();
                        dismiss();
                    }
                });
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_qr_scan, container, false);
        previewView = view.findViewById(R.id.previewView);
        cartManager = CartManager.getInstance();
        
        return view;
    }
    
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        return dialog;
    }

    private void startCamera() {
        if (getContext() == null || !isAdded()) {
            return;
        }
        
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());

        cameraProviderFuture.addListener(() -> {
            try {
                if (!isAdded() || getContext() == null) {
                    return;
                }
                
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                this.cameraProvider = cameraProvider;

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                ImageAnalysis analysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                if (scanner == null) {
                    scanner = BarcodeScanning.getClient();
                }

                analysis.setAnalyzer(ContextCompat.getMainExecutor(requireContext()), image -> {
                    if (!isScanning || !isAdded()) {
                        image.close();
                        return;
                    }
                    
                    try {
                        @SuppressLint("UnsafeOptInUsageError")
                        InputImage inputImage = InputImage.fromMediaImage(image.getImage(), image.getImageInfo().getRotationDegrees());

                        scanner.process(inputImage)
                                .addOnSuccessListener(barcodes -> {
                                    if (isAdded() && isScanning) {
                                        for (Barcode barcode : barcodes) {
                                            String rawValue = barcode.getRawValue();
                                            if (rawValue != null && !rawValue.isEmpty()) {
                                                processQRCode(rawValue);
                                                break;
                                            }
                                        }
                                    }
                                })
                                .addOnFailureListener(e -> Log.e("QRScanner", "인식 실패", e))
                                .addOnCompleteListener(t -> image.close());
                    } catch (Exception e) {
                        Log.e("QRScanner", "이미지 처리 오류", e);
                        image.close();
                    }
                });

                CameraSelector selector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, selector, preview, analysis);

            } catch (ExecutionException | InterruptedException e) {
                Log.e("QRScanner", "카메라 시작 오류", e);
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void processQRCode(String qrCode) {
        if (!isScanning) return;
        
        isScanning = false;
        
        // QR 코드로 상품 생성
        Product product = createProductFromQR(qrCode);
        
        // 장바구니에 상품 추가
        cartManager.addItem(product);
        
        // 장바구니 화면 즉시 업데이트
        requireActivity().runOnUiThread(() -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).refreshCartFragment();
            }
        });
        
        // 사용자 피드백
        Toast.makeText(requireContext(), 
                "QR 스캔 완료: " + product.getName() + "이(가) 장바구니에 추가되었습니다", 
                Toast.LENGTH_LONG).show();
        
        // 다이얼로그 닫기
        dismiss();
    }

    private Product createProductFromQR(String qrCode) {
        String[] productNames = {"신선한 사과", "유기농 바나나", "프리미엄 우유", "수제 식빵", "천연 치즈"};
        int[] prices = {3000, 2500, 2800, 4000, 5500};
        
        int index = Math.abs(qrCode.hashCode()) % productNames.length;
        
        return new Product(
                System.currentTimeMillis(),
                productNames[index] + " (QR: " + qrCode.substring(0, Math.min(qrCode.length(), 8)) + ")",
                prices[index],
                "식품"
        );
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
            cameraProvider = null;
        }
        if (scanner != null) {
            scanner.close();
            scanner = null;
        }
    }
}