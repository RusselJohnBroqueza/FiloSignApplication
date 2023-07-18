package com.example.mystartpage;

import static org.opencv.core.Core.ROTATE_90_CLOCKWISE;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mystartpage.databinding.ServerBinding;

import org.opencv.android.BaseLoaderCallback;


import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import com.example.mystartpage.databinding.ServerBinding;
import com.google.gson.Gson;


public class server extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    CameraBridgeViewBase cameraBridgeViewBase;
    BaseLoaderCallback baseLoaderCallback;
    Mat resizeImage;

    @SuppressLint("StaticFieldLeak")
    static ServerBinding binding;
    String path;

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final MediaType MEDIA_TYPE_JPEG = MediaType.parse("image/jpg");
    private static final Gson gson = new Gson();

    private static final String FLASK_SERVER_URL = "http://192.168.219.84:5000/predict";


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ServerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

//        cameraBridgeViewBase = new JavaCameraView(this, 0);
//        cameraBridgeViewBase.setVisibility(SurfaceView.VISIBLE);

        clickListeners();

        cameraBridgeViewBase = (JavaCameraView)findViewById(R.id.ServerCameraView);
        cameraBridgeViewBase.setVisibility(SurfaceView.VISIBLE);
        cameraBridgeViewBase.setCvCameraViewListener(this);

        //System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        baseLoaderCallback = new BaseLoaderCallback(this) {
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


    private void clickListeners() {
        binding.uploadButton.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(intent, 10);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        long start_2 = System.nanoTime();

        if (requestCode == 10 && resultCode == Activity.RESULT_OK) {
            Uri uri = data.getData();
            String path = RealPathUtil.getRealPath(this, uri);
//            byte[] datum = new byte[0];
            binding.imageview2.setVisibility(View.VISIBLE);
            binding.imageview2.setImageURI(uri);
            File file = new File(path);
//            try {
//                FileOutputStream fos = new FileOutputStream(file);
//                fos.write(datum);
//                fos.close();
//                System.out.println("File created successfully!");
//            } catch (IOException e) {
//                e.printStackTrace();
//            }


//            Uri returnUri = returnIntent.getData();
//            String mimeType = getContentResolver().getType(returnUri);

            if (file.exists()) {
                Log.d("abacada","abacada");
                try {
                    uploadImage(file);
                    long duration = (System.nanoTime() - start_2)/1000000;

                    String time = Long.toString(duration);

                    String milli = time + "ms";

                    TextView exec_view = findViewById(R.id.textView4);

                    exec_view.setText(milli);

                    Log.d("Execution Time Server", milli); //for Server upload to image output only
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Error uploading image1", Toast.LENGTH_SHORT).show();
                }
            } else {
                Log.d("error-image","error-image");
                Toast.makeText(this, "File does not exist", Toast.LENGTH_SHORT).show();

            }

//            long duration = (System.nanoTime() - start_2)/1000000;
//
//            String time = Long.toString(duration);
//
//            String milli = time + "ms";
//
//            TextView exec_view = findViewById(R.id.textView4);
//
//            exec_view.setText(milli);
//
//            Log.d("Execution Time Server", milli); //for Server upload to image output only


//            public static String getMimeType(File file) {
//                String type = null;
//                final String url = file.toString();
//                final String extension = MimeTypeMap.getFileExtensionFromUrl(url);
//                if (extension != null) {
//                    type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());
//                }
//                if (type == null) {
//                    type = "image/*"; // fallback type. You might set it to */*
//                }
//                return type;
//            }
        }
    }

    private void uploadImage(File file) throws IOException {
        OkHttpClient client = new OkHttpClient();

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("makefile", file.getName(), RequestBody.create(MEDIA_TYPE_JPEG, file))
                .build();

        Request request = new Request.Builder()
                .url(FLASK_SERVER_URL)
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(server.this, "Error uploading image2", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = Objects.requireNonNull(response.body()).string();
                    ImageClassificationResult result = gson.fromJson(responseBody, ImageClassificationResult.class);
                    String classname = result.getClassname();

                    runOnUiThread(() -> {
                        // Handle the classname received from the Flask server
                        binding.outputtextView.setText(classname);
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(server.this, "Error uploading image3", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    // Helper class for deserializing the Flask server response
    private static class ImageClassificationResult {
        private String classname;

        public String getClassname() {
            return classname;
        }
    }


//    private void clickListeners() {
//        binding.uploadButton.setOnClickListener(v->{
//            Intent intent = new Intent();
//           intent.setType("image/*");
//            intent.setAction(Intent.ACTION_GET_CONTENT);
//            startActivityForResult(intent, 10);
//        });
//    }

//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        if (requestCode == 10 && resultCode == Activity.RESULT_OK) {
//            Uri uri = data.getData();
//            Context context = MainActivity.this;
//            path = RealPathUtil.getRealPath(context, uri);
//            binding.imageview.setImageURI(uri);
//          binding.textView.setText(path);
//        }
//    }


    public static void saveImage(Context context, String fileName, Mat image) {
        // Convert the Mat to a Bitmap
        Bitmap bitmap = Bitmap.createBitmap(image.cols(), image.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(image, bitmap);

        // Save the Bitmap to MediaStore
        saveBitmapToMediaStore(context, fileName, bitmap);
    }

    private static void saveBitmapToMediaStore(Context context, String fileName, Bitmap bitmap) {
        ContentResolver contentResolver = context.getContentResolver();
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures");

        // Insert the image into MediaStore
        android.net.Uri uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        try {
            // Open an output stream to write the bitmap data to the MediaStore image file
            FileOutputStream outputStream = (FileOutputStream) contentResolver.openOutputStream(uri);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            outputStream.close();

            // Notify MediaScanner to scan the new image file
            MediaScannerConnection.scanFile(context, new String[]{uri.toString()}, null, null);
        } catch (IOException e) {
            e.printStackTrace();
            // Handle the exception
        }
    }


    Mat rotateImage;

    public void captureOnClick(View v){

        long start = System.nanoTime();

        saveImage(this, "capture.jpg", rotateImage);

        long duration = (System.nanoTime() - start)/1000000;

        String time = Long.toString(duration);

        String mili = time + "ms";

        TextView exec_view = findViewById(R.id.textView3);

        exec_view.setText(mili);

        Log.d("Execution Time Capture", mili); //for click until save image only

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
            Toast.makeText(getApplicationContext(),"There's a problem, yo!", Toast.LENGTH_SHORT).show();
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
}