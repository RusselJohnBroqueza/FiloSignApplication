package com.example.mystartpage;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.icu.text.ListFormatter;
import android.util.Log;
import android.widget.ImageView;

import com.example.mystartpage.FiloSignClasses;

import org.pytorch.IValue;
//import org.pytorch.LiteModuleLoader;
import org.pytorch.MemoryFormat;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ImageClassifier {
    public Context context;
    private String modelFilePath;


    public ImageClassifier(Context context) {
        this.context = context;
    }

    private String assetFilePath(Context context, String assetName) throws IOException {
        File file = new File(context.getFilesDir(), assetName);
        if (!file.exists()) {
            try (InputStream is = context.getAssets().open(assetName)) {
                try (OutputStream os = new FileOutputStream(file)) {
                    byte[] buffer = new byte[4 * 1024];
                    int read;
                    while ((read = is.read(buffer)) != -1) {
                        os.write(buffer, 0, read);
                    }
                    os.flush();
                }
            }
        }
        return file.getAbsolutePath();
    }

    public String classifyImage(Bitmap bitmap) {

        Module module = null;

        try {
            module = Module.load(assetFilePath(context, "model_FILOSign_RMSprop_0.001_100e_2130_final.pt"));
        } catch (IOException e) {
            Log.e("ImageClassifier", "Error reading assets", e);
        }

        // Preprocess the image
        final Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(bitmap,
                TensorImageUtils.TORCHVISION_NORM_MEAN_RGB, TensorImageUtils.TORCHVISION_NORM_STD_RGB);

        final Tensor outputTensor = module.forward(IValue.from(inputTensor)).toTensor();

        final float[] scores = outputTensor.getDataAsFloatArray();

        float maxScore = -Float.MAX_VALUE;
        int maxScoreIdx = -1;
        for (int i = 0; i < scores.length; i++) {
            if (scores[i] > maxScore) {
                maxScore = scores[i];
                maxScoreIdx = i;
            }
        }

        String className = FiloSignClasses.FILOSIGN_CLASSES[maxScoreIdx];
        return className;
    }
}

