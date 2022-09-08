package com.example.android_facedetection;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    Button button;
    TextView textView;
    ImageView imageView;

    private static final int REQUEST_IMAGE_CAPTURE = 99;
    InputImage firebaseVision;
    FaceDetector visionFaceDetector;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        button = findViewById(R.id.btnCamera);
        textView = findViewById(R.id.text1);
        imageView = findViewById(R.id.imageView);

        FirebaseApp.initializeApp(this);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OpenFile();
                
            }
        });

    }

    private void OpenFile() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null){
            startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
        }else {
            Toast.makeText(this, "Failed", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Bundle bundle = data.getExtras();
        Bitmap bitmap = (Bitmap) bundle.get("data");
        FaceDetectionProcess(bitmap);
    }

    public void FaceDetectionProcess(Bitmap bitmap) {
        textView.setText("Processing....");
        final StringBuilder builder = new StringBuilder();
        BitmapDrawable drawable = (BitmapDrawable) imageView.getDrawable();
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        FaceDetectorOptions highAccuracyOpt = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .enableTracking().build();

        FaceDetector detector = FaceDetection.getClient(highAccuracyOpt);
        Task<List<Face>> result =
                detector.process(image)
                        .addOnSuccessListener(
                                new OnSuccessListener<List<Face>>() {
                                    @Override
                                    public void onSuccess(List<Face> faces) {
                                        // Task completed successfully
                                        if (faces.size() != 0){
                                            if (faces.size() == 1){
                                                builder.append(faces.size() + "Face Detected \n\n");
                                            }else if (faces.size() > 1){
                                                builder.append(faces.size() + "Faces Detected \n\n");
                                            }
                                        }
                                        for (Face face : faces){
                                            builder.append("1. Face Tracking ID ["+ face.getTrackingId()+"]\n");
                                            builder.append("2. Head Rotation to Right [" + String.format("%.2f", face.getHeadEulerAngleY()) + " deg. ]\n");
                                            builder.append("3. Head Titled Sideways [" + String.format("%.2f", face.getHeadEulerAngleZ()) + " deg. ]\n");
                                            if (face.getSmilingProbability() > 0){
                                                builder.append("4. Smiling Probability [" + String.format("%.2f", face.getSmilingProbability()) + "]\n");
                                            }
                                            if (face.getLeftEyeOpenProbability() > 0){
                                                builder.append("5. Left Eye Open [" + String.format("%.2f", face.getLeftEyeOpenProbability())+ "]\n");
                                            }
                                            if (face.getRightEyeOpenProbability() > 0){
                                                builder.append("6. Right Eye Open [" + String.format("%.2f", face.getRightEyeOpenProbability())+ "]\n");
                                            }
                                            builder.append("\n");
                                        }
                                        ShowDetection("Face Detection", builder, true);
                                    }
                                })
                        .addOnFailureListener(
                                new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        // Task failed with an exception
                                        StringBuilder stringBuilder = new StringBuilder();
                                        stringBuilder.append("Error");
                                        ShowDetection("Face Detection" , builder, false);
                                    }
                                });
    }

    private void ShowDetection(final String title, final StringBuilder builder, boolean success) {
        if (success == true){
            textView.setText(null);
            textView.setMovementMethod(new ScrollingMovementMethod());
            if (builder.length() != 0){
                textView.append(builder);
                if (title.substring(0, title.indexOf(' ')).equalsIgnoreCase("OCR")){
                    textView.append("\n(Hold the text to copy it!)");
                } else {
                    textView.append("(Hold the text to copy it");
                }

                textView.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipData clipData = ClipData.newPlainText(title, builder);
                        clipboardManager.setPrimaryClip(clipData);
                        return true;
                    }
                });
            }else {
                textView.append(title.substring(0, title.indexOf(' ')) + "Failed to find anything");
            }
        }else if (success == false){
            textView.setText(null);
            textView.setMovementMethod(new ScrollingMovementMethod());
            textView.append(builder);
        }
    }
}