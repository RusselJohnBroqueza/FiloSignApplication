package com.example.mystartpage;

import static org.opencv.core.Core.ROTATE_90_CLOCKWISE;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import android.content.Intent;


import java.io.FileOutputStream;
import java.io.IOException;



public class local extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    CameraBridgeViewBase cameraBridgeViewBase;
    BaseLoaderCallback baseLoaderCallback;
    Mat resizeImage;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 200;




    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.local);

        // Check if the camera permission is granted
        int cameraPermissionStatus = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);

        if (cameraPermissionStatus != PackageManager.PERMISSION_GRANTED) {
            // Request the camera permission
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
        } else {
            // Camera permission already granted, perform necessary operations
            initializeCamera();
        }
        setContentView(R.layout.local);

        cameraBridgeViewBase = (JavaCameraView)findViewById(R.id.CameraView);
        cameraBridgeViewBase.setVisibility(SurfaceView.VISIBLE);
        cameraBridgeViewBase.setCvCameraViewListener(this);


        baseLoaderCallback = new BaseLoaderCallback(this) { //ensures the camera operation is ongoing
            @Override
            public void onManagerConnected(int status) {
                super.onManagerConnected(status);

                switch(status){

                    case BaseLoaderCallback.SUCCESS:
                        cameraBridgeViewBase.enableView();
                        break;
                    default:
                        super.onManagerConnected(status);
                        break;
                }
            }
        };

    }


    public void saveImage(Context context, String fileName, Mat image) {//saves image
        // Convert the Mat to a Bitmap
        Bitmap bitmap = Bitmap.createBitmap(image.cols(), image.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(image, bitmap);

        ImageClassifier imageClassifier = new ImageClassifier(context);
        Log.d("imageClassifier", "BITMAP found proceeding to classification");
        String className = imageClassifier.classifyImage(bitmap);
        Log.d("imageClassifier", "Success! Image is :" + className);
        //get  classifier result

        TextView textView = findViewById(R.id.textView);
        textView.setText(className);

        ImageView pic = findViewById(R.id.icon);
        pic.setImageBitmap(bitmap);
        pic.setVisibility(View.VISIBLE);

    }

    private void initializeCamera() {
        cameraBridgeViewBase = (JavaCameraView) findViewById(R.id.CameraView);
        cameraBridgeViewBase.setVisibility(SurfaceView.VISIBLE);
        cameraBridgeViewBase.setCvCameraViewListener(this);

        //System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        baseLoaderCallback = new BaseLoaderCallback(this) {
            @Override
            public void onManagerConnected(int status) {
                super.onManagerConnected(status);

                switch (status) {

                    case BaseLoaderCallback.SUCCESS:
                        cameraBridgeViewBase.enableView();
                        break;
                    default:
                        super.onManagerConnected(status);
                        break;
                }
            }
        };
    }

    Mat rotateImage;

    public void captureOnClick(View v){

        long start = System.nanoTime();

        saveImage(this, "capture.jpg", rotateImage);

        long duration = (System.nanoTime() - start)/1000000;

        String time = Long.toString(duration);

        String mili = time + "ms";

        TextView exec_view = findViewById(R.id.textView2);

        exec_view.setText(mili);

        Log.d("Execution Time", mili);
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        resizeImage = new Mat();
        Log.d("onCameraFrame", "Size = "+ inputFrame.rgba().size());
        Imgproc.resize(inputFrame.rgba(), resizeImage, new Size(224,224));
        rotateImage = new Mat();
        Core.rotate(resizeImage, rotateImage, ROTATE_90_CLOCKWISE);
        Log.d("onCameraFrame", "Size = "+ resizeImage.size());
        return inputFrame.rgba(); //for display
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
    }

    @Override
    public void onCameraViewStopped() {
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!OpenCVLoader.initDebug()){
            Toast.makeText(getApplicationContext(),"There's a problem!", Toast.LENGTH_SHORT).show();
        }

        else
        {
            baseLoaderCallback.onManagerConnected(baseLoaderCallback.SUCCESS);
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        if(cameraBridgeViewBase!=null){

            cameraBridgeViewBase.disableView();
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraBridgeViewBase!=null){
            cameraBridgeViewBase.disableView();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Camera permission granted, perform necessary operations
                initializeCamera();
            } else {
                // Camera permission denied, handle accordingly (e.g., show a message or disable camera functionality)
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}


