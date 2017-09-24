package com.example.digitrecognition;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final String TESS_DATA = "tessdata";
    private static final String DATA_PATH = Environment.getExternalStorageDirectory().toString() + "/DigitRecognition/";
    TextView textView;
    private TessBaseAPI tessBaseAPI;
    private static final int REQUEST_STORAGE_PERMISSION = 1;
    private static final int PICTURE_CAPTURE = 2;
    Uri outputFileUri;

    // TODO: check 3 recognition

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = (TextView) findViewById(R.id.resultTextView);

        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        prepareTessData();
    }

    public void recognise(View view) {
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_STORAGE_PERMISSION);
        } else {
            startCameraActivity();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_STORAGE_PERMISSION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startCameraActivity();
                } else {
                    Toast.makeText(this, "Can't take image!", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    private void startCameraActivity() {
        try {
            String IMAGE_PATH = Environment.getExternalStorageDirectory().toString() + "/DigitRecognition/img";
            prepareDirectory(IMAGE_PATH);

            String image_path = IMAGE_PATH + "/orc.jpg";

            outputFileUri = Uri.fromFile(new File(image_path));

            final Intent pictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            pictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);

            if (pictureIntent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(pictureIntent, PICTURE_CAPTURE);
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    private void prepareDirectory(String path) {
        File dir = new File(path);

        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                Log.e(TAG, "ERROR: Creation of directory " + path + " failed, check does Android Manifest have permission to write to external storage.");
            } else {
                Log.i(TAG, "Created directory " + path);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PICTURE_CAPTURE && resultCode == Activity.RESULT_OK) {
            doOCR();
        } else {
            Toast.makeText(this, "Уккщк ефлштп!", Toast.LENGTH_SHORT).show();
        }
    }

    private void doOCR() {
        startOCR();
    }

    private void prepareTessData() {
        prepareDirectory(DATA_PATH + TESS_DATA);

        copyTessDataFiles(TESS_DATA);
    }

    private void copyTessDataFiles(String path) {
        try {
            String fileName = "letsgodigital.traineddata";

                Log.d(TAG, "blia");

                // open file within the assets folder
                // if it is not already there copy it to the sdcard
                String pathToDataFile = DATA_PATH + path + "/" + fileName;
                if (!(new File(pathToDataFile)).exists()) {

                    InputStream in = getAssets().open(fileName);

                    OutputStream out = new FileOutputStream(pathToDataFile);

                    // Transfer bytes from in to out
                    byte[] buf = new byte[1024];
                    int len;

                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                    in.close();
                    out.close();

                    Log.d(TAG, "Copied " + fileName + "to tessdata");
                }
        } catch (IOException e) {
            Log.e(TAG, "Unable to copy files to tessdata " + e.toString());
        }
    }

    private void startOCR() {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 4;
            Bitmap bitmap = BitmapFactory.decodeFile(outputFileUri.getPath(), options);

            String result = processImage(bitmap);

            textView.setText(result);
        } catch (Exception e) {
            Log.e(TAG + " startOCR", e.getMessage());
        }
    }

    private String processImage(Bitmap bitmap) {
        try {
            tessBaseAPI = new TessBaseAPI();

        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }

        tessBaseAPI.init(DATA_PATH, "letsgodigital");
        tessBaseAPI.setImage(bitmap);
        String res = "";

        try {
            res = tessBaseAPI.getUTF8Text();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
        tessBaseAPI.end();
        return res;
    }
}
