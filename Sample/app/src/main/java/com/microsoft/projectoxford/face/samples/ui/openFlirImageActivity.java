/*******************************************************************
 * @title FLIR THERMAL SDK
 * @file MainActivity.java
 * @Author FLIR Systems AB
 *
 * @brief Main UI of test application
 *
 * Copyright 2019:    FLIR Systems
 *******************************************************************/
package com.microsoft.projectoxford.face.samples.ui;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.flir.thermalsdk.androidsdk.BuildConfig;
import com.flir.thermalsdk.androidsdk.ThermalSdkAndroid;
import com.flir.thermalsdk.androidsdk.image.BitmapAndroid;
import com.flir.thermalsdk.image.ImageFactory;
import com.flir.thermalsdk.image.JavaImageBuffer;
import com.flir.thermalsdk.image.Point;
import com.flir.thermalsdk.image.ThermalImageFile;
import com.flir.thermalsdk.image.ThermalValue;
import com.flir.thermalsdk.image.fusion.FusionMode;
import com.flir.thermalsdk.image.measurements.MeasurementSpot;
import com.flir.thermalsdk.log.ThermalLog;
import com.microsoft.projectoxford.face.samples.R;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.microsoft.projectoxford.face.samples.helper.ImageHelper;

/**
 * This is a sample application on how to use the FLIR Thermal SDK
 * in this example we will:
 * <p>
 * 1. open a FLIR Thermal Image
 * 2. extract a image of the IR data, visual data (also known as "DC") and a image with IR and visual data mixed (known as MSX or Blending)
 * 3. extract some measurements data from the thermal image
 */
public class openFlirImageActivity extends AppCompatActivity {

    public static final String IMAGE_NAME = "flir_ir_image.jpg";
    private String thermalSDKversion;
    private static final String TAG = "openFlirImageActivity";

    //shows dc and ir image
    private ImageView dcAndIrImageView;
    //shows just ir image
    private ImageView irImageView;
    //shows just visual image
    private ImageView visualImageView;

    private TextView minImageStatValue;
    private TextView avgImageStateValue;
    private TextView maxImageStateValue;

    private TextView spotValue;

    private float x1,y1,bouunding_width,bouunding_height;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_flir_image);

        ThermalLog.LogLevel enableLoggingInDebug = BuildConfig.DEBUG ? ThermalLog.LogLevel.DEBUG : ThermalLog.LogLevel.NONE;
        //ThermalSdkAndroid.init(..) has to be initiated from an Activity and before using ANY functionality from the FLIR Thermal SDK
        ThermalSdkAndroid.init(getApplicationContext(), enableLoggingInDebug);

        setupUiViews();

        copyFlirImageToDisk();

        thermalSDKversion = ThermalSdkAndroid.getVersion();
        ThermalLog.d(TAG, "onCreate sdk version:" + thermalSDKversion);

        showSDKVersion();
    }

    @Override
    protected void onResume() {
        super.onResume();

        ThermalImageFile thermalImageFile = openIncludedImage();
        //showFusionModes(thermalImageFile, irImageView, visualImageView);
        //showImageData(thermalImageFile,avgImageStateValue);
    }


    // Flag to indicate which task is to be performed.
    private static final int REQUEST_SELECT_IMAGE = 0;

    // Called when the "Select Image" button is clicked.
    public void selectImage(View view) {
        Intent intent = new Intent(this, SelectImageActivity.class);
        startActivityForResult(intent, REQUEST_SELECT_IMAGE);
    }

    // The image selected to detect.
    private Bitmap mBitmap;

    // Called when image selection is done.
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode)
        {
            case REQUEST_SELECT_IMAGE:
                if(resultCode == RESULT_OK) {

                    // If image is selected successfully, set the image URI and bitmap.
                    Uri imageUri = data.getData();
                    mBitmap = ImageHelper.loadSizeLimitedBitmapFromUri(
                            imageUri, getContentResolver());
                    if (mBitmap != null) {
                        // Show the image on screen.
                       // ImageView imageView = (ImageView) findViewById(R.id.image);
                        irImageView.setImageBitmap(mBitmap);
                    }

                    byte[] data2 = new byte[2000];

                    try (
                            FileOutputStream outputStream = openFileOutput(IMAGE_NAME, Context.MODE_PRIVATE);
                            InputStream inputStream = getContentResolver().openInputStream(imageUri);
                    ) {
                        int result = 0;
                        while ((result = inputStream.read(data2)) > 0) {
                            outputStream.write(data2);
                            outputStream.flush();
                        }
                    } catch (IOException e) {
                        ThermalLog.e(TAG, "failed to copy FLIR image to disk, exception:" + e);
                    }


                    File directory = getFilesDir();
                    File file = new File(directory, IMAGE_NAME);
                    String absoluteFilePath = file.getAbsolutePath();

                    //use the path and open a Thermal image
                    ThermalImageFile thermalImageFile = null;
                    try {
                        thermalImageFile = (ThermalImageFile) ImageFactory.createImage(absoluteFilePath);
                    } catch (IOException e) {
                        ThermalLog.e(TAG, "failed to open IR file, exception:" + e);
                    }

                    toIrAndPhoto(thermalImageFile,irImageView,visualImageView);
                    showImageData(thermalImageFile,avgImageStateValue);


//                    // Clear the identification result.
//                    IdentificationActivity.FaceListAdapter faceListAdapter = new IdentificationActivity.FaceListAdapter(null);
//                    ListView listView = (ListView) findViewById(R.id.list_identified_faces);
//                    listView.setAdapter(faceListAdapter);
//
//                    // Clear the information panel.
//                    setInfo("");
//
//                    // Start detecting in image.
//                    detect(mBitmap);
                }
                break;
            default:
                break;
        }
    }

    private void showImageData(ThermalImageFile thermalImageFile, TextView avgImage) {
        //** Get measurements from a spot and show it
        //Get the first spot found

        //thermalImageFile.getMeasurements().addSpot(100,100);
        thermalImageFile.getMeasurements().addRectangle((int)(x1/2.645833),(int)(y1/2.645833),(int)(bouunding_width/2.645833),(int)(bouunding_height/2.645833));

        ThermalValue avgValue;
        avgValue = thermalImageFile.getMeasurements().getRectangles().get(0).getAverage().asCelsius();


        //MeasurementSpot measurementSpot = thermalImageFile.getMeasurements().getSpots().get(0);

        //Get where on the image the spot was
        //Point position = measurementSpot.getPosition();

        //Get the values for the Spot
        //ThermalValue thermalValue = measurementSpot.getValue().asCelsius();
        //String unitName = thermalValue.unit.name();
        //String stateName = thermalValue.state.name(); //state indicate whether we can trust the accuracy of the value
        //String value = String.valueOf(thermalValue.value);

        //Show the collected Spot values
        //spotValues.setText(getString(R.string.spot_value_text, position.x, position.y, stateName, value, unitName));

        //** Get image min/max/avg value
//        ThermalValue minimumValue = thermalImageFile.getStatistics().min.asCelsius();
//        ThermalValue averageValue = thermalImageFile.getStatistics().average.asCelsius();
//        ThermalValue maximumValue = thermalImageFile.getStatistics().max.asCelsius();

        //Show the collected min/max/avg values
//        minImage.setText(getString(R.string.stat_min_text, minimumValue.state.name(), String.valueOf(minimumValue.value), minimumValue.unit.name()));
//        avgImage.setText(getString(R.string.stat_avg_text, averageValue.state.name(), String.valueOf(averageValue.value), averageValue.unit.name()));
//        maxImage.setText(getString(R.string.stat_max_text, maximumValue.state.name(), String.valueOf(maximumValue.value), maximumValue.unit.name()));

        String temp;
        if(avgValue.value>30)
        {
            temp = "是活人";
        }
        else
        {
            temp = "不是活人";
        }
        DecimalFormat fnum   =   new   DecimalFormat("##0.00");
        String   dd=fnum.format(avgValue.value);
        //avgImage.setText(getString(R.string.avg_value_text,String.valueOf(avgValue.value),temp));
        avgImage.setText(getString(R.string.avg_value_text,dd,temp));
        //avgImage.setText(getString(R.string.avg_value_text,String.valueOf(avgValue.value)));
    }

    private void showFusionModes(ThermalImageFile thermalImageFile, ImageView irImageView, ImageView visualImageView) {

        {
            //** Get the IR image data from the image
            //First we need to se the correct Fusion mode in the ThermalImageFile
            thermalImageFile.getFusion().setFusionMode(FusionMode.THERMAL_ONLY);
            ThermalLog.d(TAG, "current image width:" + thermalImageFile.getWidth() + " height:" + thermalImageFile.getHeight());

            //Make a deep copy of the actual Image pixels
            JavaImageBuffer irImage = thermalImageFile.getImage();
            ThermalLog.d(TAG, "current IR image width:" + irImage.width + " height:" + irImage.height);

            //Convert the pixel into a Android bitmap
            BitmapAndroid irBitmap = BitmapAndroid.createBitmap(irImage);

            //Set the ImageView with the IR bitmap
            //irImageView.setImageBitmap(irBitmap.getBitMap());
        }
        {
            //** Get the mixed Visual and IR image
            //First we need to se the correct Fusion mode in the ThermalImageFile known as MSX
            thermalImageFile.getFusion().setFusionMode(FusionMode.MSX);
            ThermalLog.d(TAG, "current image width:" + thermalImageFile.getWidth() + " height:" + thermalImageFile.getHeight());

            //Make a deep copy of the actual Image pixels
            JavaImageBuffer image = thermalImageFile.getImage();
            ThermalLog.d(TAG, "current MSX image width:" + image.width + " height:" + image.height);

            //Convert the pixel into a Android bitmap
            BitmapAndroid bitmap = BitmapAndroid.createBitmap(image);

            //Set the ImageView with
            //dcAndIrImageView.setImageBitmap(bitmap.getBitMap());
        }
        {
            //** Get the Visual image, the visual image can be bigger then the IR and MSX image
            //First we need to se the correct Fusion mode in the ThermalImageFile known as MSX
            thermalImageFile.getFusion().setFusionMode(FusionMode.VISUAL_ONLY); //The visual image might also be known as DC image
            ThermalLog.d(TAG, "current image width:" + thermalImageFile.getWidth() + " height:" + thermalImageFile.getHeight());

            //Make a deep copy of the actual Image pixels
            JavaImageBuffer photo = thermalImageFile.getImage();
            ThermalLog.d(TAG, "current photo image width:" + photo.width + " height:" + photo.height);

            //Convert the pixel into a Android bitmap
            BitmapAndroid photoBitmap = BitmapAndroid.createBitmap(photo);


            Bitmap myBitmap=photoBitmap.getBitMap();

            FileOutputStream fOut;
            try {
                File dir = new File("/sdcard/demo/");
                if (!dir.exists()) {
                    dir.mkdir();
                }

                String tmp = "/sdcard/demo/takepicture.jpg";
                fOut = new FileOutputStream(tmp);
                myBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fOut);

                try {
                    fOut.flush();
                    fOut.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

//            // Set the ImageView with the
//            visualImageView.setImageBitmap(photoBitmap.getBitMap());

            // Create a Paint object for drawing with
            Paint myRectPaint = new Paint();
            myRectPaint.setStrokeWidth(5);
            myRectPaint.setColor(Color.RED);
            myRectPaint.setStyle(Paint.Style.STROKE);

            // Create a Canvas object for drawing on
            Bitmap tempBitmap = Bitmap.createBitmap(myBitmap.getWidth(), myBitmap.getHeight(), Bitmap.Config.RGB_565);
            Canvas tempCanvas = new Canvas(tempBitmap);
            tempCanvas.drawBitmap(myBitmap, 0, 0, null);

            // Create the Face Detector
            FaceDetector faceDetector = new
                    FaceDetector.Builder(getApplicationContext()).setTrackingEnabled(false)
                    .build();
            if(!faceDetector.isOperational()){
                //new AlertDialog.Builder(v.getContext()).setMessage("Could not set up the face detector!").show();
                return;
            }

            // Detect the Faces
            Frame frame = new Frame.Builder().setBitmap(myBitmap).build();
            SparseArray<Face> faces = faceDetector.detect(frame);

            // Draw Rectangles on the Faces
            for(int i=0; i<faces.size(); i++) {
                Face thisFace = faces.valueAt(i);
                x1 = thisFace.getPosition().x;
                y1 = thisFace.getPosition().y;
                bouunding_width = x1 + thisFace.getWidth();
                bouunding_height = y1 + thisFace.getHeight();
                tempCanvas.drawRoundRect(new RectF(x1, y1, bouunding_width, bouunding_height), 2, 2, myRectPaint);
            }
            visualImageView.setImageDrawable(new BitmapDrawable(getResources(),tempBitmap));
        }
    }

    private void toIrAndPhoto(ThermalImageFile thermalImageFile, ImageView irImageView, ImageView visualImageView) {

        {
            //** Get the IR image data from the image
            //First we need to se the correct Fusion mode in the ThermalImageFile
            thermalImageFile.getFusion().setFusionMode(FusionMode.THERMAL_ONLY);
            ThermalLog.d(TAG, "current image width:" + thermalImageFile.getWidth() + " height:" + thermalImageFile.getHeight());

            //Make a deep copy of the actual Image pixels
            JavaImageBuffer irImage = thermalImageFile.getImage();
            ThermalLog.d(TAG, "current IR image width:" + irImage.width + " height:" + irImage.height);

            //Convert the pixel into a Android bitmap
            BitmapAndroid irBitmap = BitmapAndroid.createBitmap(irImage);

            //Set the ImageView with the IR bitmap
            irImageView.setImageBitmap(irBitmap.getBitMap());
        }
        {
            //** Get the Visual image, the visual image can be bigger then the IR and MSX image
            //First we need to se the correct Fusion mode in the ThermalImageFile known as MSX
            thermalImageFile.getFusion().setFusionMode(FusionMode.VISUAL_ONLY); //The visual image might also be known as DC image
            ThermalLog.d(TAG, "current image width:" + thermalImageFile.getWidth() + " height:" + thermalImageFile.getHeight());

            //Make a deep copy of the actual Image pixels
            JavaImageBuffer photo = thermalImageFile.getImage();
            ThermalLog.d(TAG, "current photo image width:" + photo.width + " height:" + photo.height);

            //Convert the pixel into a Android bitmap
            BitmapAndroid photoBitmap = BitmapAndroid.createBitmap(photo);
            Bitmap myBitmap = photoBitmap.getBitMap();

            FileOutputStream fOut;
            try {
                File directory = getFilesDir();
                File dir = new File(directory,"demo");
                if (!dir.exists()) {
                    dir.mkdir();
                }

                fOut = new FileOutputStream(dir.getAbsolutePath() + "/takepicture.jpg");
                myBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fOut);

                try {
                    fOut.flush();
                    fOut.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            // Create a Paint object for drawing with
            Paint myRectPaint = new Paint();
            myRectPaint.setStrokeWidth(5);
            myRectPaint.setColor(Color.RED);
            myRectPaint.setStyle(Paint.Style.STROKE);

            // Create a Canvas object for drawing on
            Bitmap tempBitmap = Bitmap.createBitmap(myBitmap.getWidth(), myBitmap.getHeight(), Bitmap.Config.RGB_565);
            Canvas tempCanvas = new Canvas(tempBitmap);
            tempCanvas.drawBitmap(myBitmap, 0, 0, null);

            // Create the Face Detector
            FaceDetector faceDetector = new
                    FaceDetector.Builder(getApplicationContext()).setTrackingEnabled(false)
                    .build();
            if(!faceDetector.isOperational()){
                //new AlertDialog.Builder(v.getContext()).setMessage("Could not set up the face detector!").show();
                return;
            }

            // Detect the Faces
            Frame frame = new Frame.Builder().setBitmap(myBitmap).build();
            SparseArray<Face> faces = faceDetector.detect(frame);

            // Draw Rectangles on the Faces
            for(int i=0; i<faces.size(); i++) {
                Face thisFace = faces.valueAt(i);
                x1 = thisFace.getPosition().x;
                y1 = thisFace.getPosition().y;
                bouunding_width = x1 + thisFace.getWidth();
                bouunding_height = y1 + thisFace.getHeight();
                tempCanvas.drawRoundRect(new RectF(x1, y1, bouunding_width, bouunding_height), 2, 2, myRectPaint);
            }
            visualImageView.setImageDrawable(new BitmapDrawable(getResources(),tempBitmap));
        }
    }

    /**
     * Open the included FLIR image file
     *
     * @return a IR file or NULL if failed to open file
     */
    private ThermalImageFile openIncludedImage() {

        //Get a path to the file saved on disk
        File directory = getFilesDir();
        File file = new File(directory, IMAGE_NAME);
        String absoluteFilePath = file.getAbsolutePath();

        //use the path and open a Thermal image
        ThermalImageFile thermalImageFile = null;
        try {
            thermalImageFile = (ThermalImageFile) ImageFactory.createImage(absoluteFilePath);
        } catch (IOException e) {
            ThermalLog.e(TAG, "failed to open IR file, exception:" + e);
        }
        return thermalImageFile;
    }

    /**
     * Copy a FLIR IR image from the application to disk so we can open it with the Thermal SDK
     */
    private void copyFlirImageToDisk() {

        byte[] data = new byte[2000];

        try (
                FileOutputStream outputStream = openFileOutput(IMAGE_NAME, Context.MODE_PRIVATE);
                InputStream inputStream = getResources().openRawResource(R.raw.ir_7);
        ) {
            int result = 0;
            while ((result = inputStream.read(data)) > 0) {
                outputStream.write(data);
                outputStream.flush();
            }
        } catch (IOException e) {
            ThermalLog.e(TAG, "failed to copy FLIR image to disk, exception:" + e);
        }
    }
    public void identification(View view) {
        Intent intent = new Intent(this, IdentificationActivity.class);
        startActivity(intent);
    }

    private void showSDKVersion() {
        TextView thermalSdkTextView = findViewById(R.id.thermal_sdk_version_text_view);
        String thermalSDKtext = getString(R.string.thermal_sdk_version, thermalSDKversion);
        ThermalLog.d(TAG, "showSDKVersion() " + thermalSDKtext);
        thermalSdkTextView.setText(thermalSDKtext);
    }

    private void setupUiViews() {
        //dcAndIrImageView = (ImageView) findViewById(R.id.dc_and_ir_image);
        irImageView = (ImageView) findViewById(R.id.ir_image);
        visualImageView = (ImageView) findViewById(R.id.visual_image);

//        minImageStatValue = (TextView) findViewById(R.id.min_textview);
        avgImageStateValue = (TextView) findViewById(R.id.avg_value);
//        maxImageStateValue = (TextView) findViewById(R.id.max_textview);
//
//        spotValue = (TextView) findViewById(R.id.spot_value);
    }


}
