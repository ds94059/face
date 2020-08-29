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

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
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
import com.microsoft.projectoxford.face.FaceServiceClient;
import com.microsoft.projectoxford.face.contract.Face;
import com.microsoft.projectoxford.face.contract.IdentifyResult;
import com.microsoft.projectoxford.face.contract.LargePersonGroup;
import com.microsoft.projectoxford.face.contract.PersonGroup;
import com.microsoft.projectoxford.face.contract.TrainingStatus;
import com.microsoft.projectoxford.face.rest.ClientException;
import com.microsoft.projectoxford.face.samples.R;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.UUID;

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
//import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.microsoft.projectoxford.face.samples.helper.ImageHelper;
import com.microsoft.projectoxford.face.samples.helper.SampleApp;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.microsoft.projectoxford.face.FaceServiceClient;
//import com.microsoft.projectoxford.face.contract.Face;
import com.microsoft.projectoxford.face.contract.IdentifyResult;
import com.microsoft.projectoxford.face.contract.TrainingStatus;
import com.microsoft.projectoxford.face.samples.R;
import com.microsoft.projectoxford.face.samples.helper.ImageHelper;
import com.microsoft.projectoxford.face.samples.helper.LogHelper;
import com.microsoft.projectoxford.face.samples.helper.SampleApp;
import com.microsoft.projectoxford.face.samples.helper.StorageHelper;
import com.microsoft.projectoxford.face.samples.log.IdentificationLogActivity;
import com.microsoft.projectoxford.face.samples.persongroupmanagement.PersonGroupListActivity;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * This is a sample application on how to use the FLIR Thermal SDK
 * in this example we will:
 * <p>
 * 1. open a FLIR Thermal Image
 * 2. extract a image of the IR data, visual data (also known as "DC") and a image with IR and visual data mixed (known as MSX or Blending)
 * 3. extract some measurements data from the thermal image
 */
public class openFlirImageActivity extends AppCompatActivity {

    // Background task of face identification.
    private class IdentificationTask extends AsyncTask<UUID, String, IdentifyResult[]> {
        private boolean mSucceed = true;
        String mPersonGroupId;
        IdentificationTask(String personGroupId) {
            this.mPersonGroupId = personGroupId;
        }

        @Override
        protected IdentifyResult[] doInBackground(UUID... params) {
            String logString = "Request: Identifying faces ";
            for (UUID faceId: params) {
                logString += faceId.toString() + ", ";
            }
            logString += " in group " + mPersonGroupId;
            addLog(logString);

            // Get an instance of face service client to detect faces in image.
            FaceServiceClient faceServiceClient = SampleApp.getFaceServiceClient();
            try{
                publishProgress("Getting person group status...");

                TrainingStatus trainingStatus = faceServiceClient.getLargePersonGroupTrainingStatus(
                        this.mPersonGroupId);     /* personGroupId */
                if (trainingStatus.status != TrainingStatus.Status.Succeeded) {
                    publishProgress("Person group training status is " + trainingStatus.status);
                    mSucceed = false;
                    return null;
                }

                publishProgress("Identifying...");

                // Start identification.
                return faceServiceClient.identityInLargePersonGroup(
                        this.mPersonGroupId,   /* personGroupId */
                        params,                  /* faceIds */
                        1);  /* maxNumOfCandidatesReturned */
            }  catch (Exception e) {
                mSucceed = false;
                publishProgress(e.getMessage());
                addLog(e.getMessage());
                return null;
            }
        }

        @Override
        protected void onPreExecute() {
            //setUiBeforeBackgroundTask();
        }

        @Override
        protected void onProgressUpdate(String... values) {
            // Show the status of background detection task on screen.a
            //setUiDuringBackgroundTask(values[0]);
        }

        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        protected void onPostExecute(IdentifyResult[] result) {
            // Show the result on screen when detection is done.
            setUiAfterIdentification(result, mSucceed);
            //Log.d("response time after",java.time.LocalTime.now().toString());
        }
    }
    String mPersonGroupId;
    boolean detected;
    PersonGroupListAdapter mPersonGroupListAdapter;
    LargePersonGroup[] mLargePersonGroups;

    // Show the result on screen when detection is done.
    private void setUiAfterIdentification(IdentifyResult[] result, boolean succeed) {
        //progressDialog.dismiss();
        //setAllButtonsEnabledStatus(true);
        //setIdentifyButtonEnabledStatus(false);

        if (succeed) {
            // Set the information about the detection result.
            //setInfo("Identification is done");

            if (result != null) {
                mFaceListAdapter.setIdentificationResult(result);
                String logString = "Response: Success. ";
                for (IdentifyResult identifyResult: result) {
                    logString += "Face " + identifyResult.faceId.toString() + " is identified as "
                            + (identifyResult.candidates.size() > 0
                            ? identifyResult.candidates.get(0).personId.toString()
                            : "Unknown Person")
                            + ". ";
                }
                addLog(logString);

                // Show the detailed list of detected faces.
                ListView listView = (ListView) findViewById(R.id.list_identified_faces);
                listView.setAdapter(mFaceListAdapter);
            }
        }
    }

    // The adapter of the GridView which contains the details of the detected faces.
    class FaceListAdapter extends BaseAdapter {
        // The detected faces.
        List<com.microsoft.projectoxford.face.contract.Face> faces;

        List<IdentifyResult> mIdentifyResults;

        // The thumbnails of detected faces.
        List<Bitmap> faceThumbnails;

        // Initialize with detection result.
        FaceListAdapter(com.microsoft.projectoxford.face.contract.Face[] detectionResult) {
            faces = new ArrayList<>();
            faceThumbnails = new ArrayList<>();
            mIdentifyResults = new ArrayList<>();

            if (detectionResult != null) {
                faces = Arrays.asList(detectionResult);
                for (com.microsoft.projectoxford.face.contract.Face face: faces) {
                    try {
                        // Crop face thumbnail with five main landmarks drawn from original image.
                        faceThumbnails.add(ImageHelper.generateFaceThumbnail(
                                mBitmap, face.faceRectangle));
                    } catch (IOException e) {
                        // Show the exception when generating face thumbnail fails.
                        //setInfo(e.getMessage());
                    }
                }
            }
        }

        public void setIdentificationResult(IdentifyResult[] identifyResults) {
            mIdentifyResults = Arrays.asList(identifyResults);
        }

        @Override
        public boolean isEnabled(int position) {
            return false;
        }

        @Override
        public int getCount() {
            return faces.size();
        }

        @Override
        public Object getItem(int position) {
            return faces.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater layoutInflater =
                        (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = layoutInflater.inflate(
                        R.layout.item_face_with_description, parent, false);
            }
            convertView.setId(position);

            // Show the face thumbnail.
            ((ImageView)convertView.findViewById(R.id.face_thumbnail)).setImageBitmap(
                    faceThumbnails.get(position));

            if (mIdentifyResults.size() == faces.size()) {
                // Show the face details.
                DecimalFormat formatter = new DecimalFormat("#0.00");
                if (mIdentifyResults.get(position).candidates.size() > 0) {
                    String personId =
                            mIdentifyResults.get(position).candidates.get(0).personId.toString();
                    String personName = StorageHelper.getPersonName(
                            personId, mPersonGroupId, openFlirImageActivity.this);
                    String identity = "Person: " + personName + "\n"
                            + "Confidence: " + formatter.format(
                            mIdentifyResults.get(position).candidates.get(0).confidence);
                    ((TextView) convertView.findViewById(R.id.text_detected_face)).setText(
                            identity);
                    avgImageStateValue.setText(getString(R.string.avg_value_text,personName,avgTemp2bit,alive_text));
                } else {
                    ((TextView) convertView.findViewById(R.id.text_detected_face)).setText(
                            R.string.face_cannot_be_identified);
                    avgImageStateValue.setText(getString(R.string.avg_value_text,"Unknown person",avgTemp2bit,alive_text));
                }
            }



            return convertView;
        }
    }

    FaceListAdapter mFaceListAdapter;
    // Background task of face detection.
    private class DetectionTask extends AsyncTask<InputStream, String, com.microsoft.projectoxford.face.contract.Face[]> {
        @Override
        protected com.microsoft.projectoxford.face.contract.Face[] doInBackground(InputStream... params) {
            // Get an instance of face service client to detect faces in image.
            FaceServiceClient faceServiceClient = SampleApp.getFaceServiceClient();
            try{
                publishProgress("Detecting...");

                // Start detection.
                return faceServiceClient.detect(
                        params[0],  /* Input stream of image to detect */
                        true,       /* Whether to return face ID */
                        false,       /* Whether to return face landmarks */
                        /* Which face attributes to analyze, currently we support:
                           age,gender,headPose,smile,facialHair */
                        null);
            }  catch (Exception e) {
                publishProgress(e.getMessage());
                return null;
            }
        }

        @Override
        protected void onPreExecute() {
            //setUiBeforeBackgroundTask();
        }

        @Override
        protected void onProgressUpdate(String... values) {
            // Show the status of background detection task on screen.
            //setUiDuringBackgroundTask(values[0]);
        }

        @Override
        protected void onPostExecute(com.microsoft.projectoxford.face.contract.Face[] result) {
            //progressDialog.dismiss();

            //setAllButtonsEnabledStatus(true);

            if (result != null) {
                // Set the adapter of the ListView which contains the details of detected faces.
                mFaceListAdapter = new openFlirImageActivity.FaceListAdapter(result);
                ListView listView = (ListView) findViewById(R.id.list_identified_faces);
                listView.setAdapter(mFaceListAdapter);

                if (result.length == 0) {
                    detected = false;
                   //setInfo("No faces detected!");
                } else {
                    detected = true;
                    //setInfo("Click on the \"Identify\" button to identify the faces in image.");
                }
            } else {
                detected = false;
            }

            //refreshIdentifyButtonEnabledStatus();
            identify();
        }
    }

    private class GetLargePersonGroupTask extends AsyncTask<InputStream, String, LargePersonGroup[]> {

        GetLargePersonGroupTask() {}

        @Override
        protected LargePersonGroup[] doInBackground(InputStream... params) {
            FaceServiceClient faceServiceClient = SampleApp.getFaceServiceClient();
            try {
                LargePersonGroup[] largePersonGroups = faceServiceClient.listLargePersonGroups("",1000);
                mLargePersonGroups = largePersonGroups;
                return largePersonGroups;
            } catch (ClientException e) {
                e.printStackTrace();
                return null;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
    }

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
    String avgTemp2bit;
    String alive_text;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_flir_image);

        //initialize the personGroup
        ListView listView = (ListView) findViewById(R.id.list_person_groups_identify);
        mPersonGroupListAdapter = new openFlirImageActivity.PersonGroupListAdapter();
        listView.setAdapter(mPersonGroupListAdapter);

        new GetLargePersonGroupTask().execute();

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

        ListView listView = (ListView) findViewById(R.id.list_person_groups_identify);
        //mPersonGroupListAdapter = new openFlirImageActivity.PersonGroupListAdapter();
        //listView.setAdapter(mPersonGroupListAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                setPersonGroupSelected(position);
            }
        });

        if (mPersonGroupListAdapter.personGroupIdList.size() != 0) {
            setPersonGroupSelected(0);
        } else {
            setPersonGroupSelected(-1);
        }
        new GetLargePersonGroupTask().execute();
        //ThermalImageFile thermalImageFile = openIncludedImage();
        //showFusionModes(thermalImageFile, irImageView, visualImageView);
        //showImageData(thermalImageFile,avgImageStateValue);
    }

    public void getJson(View view) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(openFlirImageActivity.this);
        dialog.setTitle("PersonGroups: ");
        String temp = "";
        for(int i=0;i<mLargePersonGroups.length;i++)
        {
            temp += "Group "+i + " groupname:" + mLargePersonGroups[i].name + "\n";
        }
        dialog.setMessage(temp);
        dialog.show();
    }

    public void setting(View view) {
        Intent intent = new Intent(this, SettingPersonGroupActivity.class);
        startActivity(intent);
    }

    public void managePersonGroups(View view) {
        Intent intent = new Intent(this, PersonGroupListActivity.class);
        startActivity(intent);

        //refreshIdentifyButtonEnabledStatus();
    }

    void setPersonGroupSelected(int position) {
        //TextView textView = (TextView) findViewById(R.id.text_person_group_selected);
        if (position > 0) {
            String personGroupIdSelected = mPersonGroupListAdapter.personGroupIdList.get(position);
            mPersonGroupListAdapter.personGroupIdList.set(
                    position, mPersonGroupListAdapter.personGroupIdList.get(0));
            mPersonGroupListAdapter.personGroupIdList.set(0, personGroupIdSelected);
            ListView listView = (ListView) findViewById(R.id.list_person_groups_identify);
            listView.setAdapter(mPersonGroupListAdapter);
            setPersonGroupSelected(0);
        } else if (position < 0) {
            //setIdentifyButtonEnabledStatus(false);
            //textView.setTextColor(Color.RED);
            //textView.setText(R.string.no_person_group_selected_for_identification_warning);
        } else {
            mPersonGroupId = mPersonGroupListAdapter.personGroupIdList.get(0);
            String personGroupName = StorageHelper.getPersonGroupName(
                    mPersonGroupId, openFlirImageActivity.this);
            //refreshIdentifyButtonEnabledStatus();
            //textView.setTextColor(Color.BLACK);
            //textView.setText(String.format("Person group to use: %s", personGroupName));
        }
        //identify();
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




                    // Clear the identification result.
                    openFlirImageActivity.FaceListAdapter faceListAdapter = new openFlirImageActivity.FaceListAdapter(null);
                    ListView listView = (ListView) findViewById(R.id.list_identified_faces);
                    listView.setAdapter(faceListAdapter);

                    // Clear the information panel.
                    //setInfo("");

                    // Start detecting in image.
                    File file2 = new File(directory, "demo/takepicture.jpg");
                    Uri imageUri2= Uri.fromFile(file2);
                    mBitmap = ImageHelper.loadSizeLimitedBitmapFromUri(
                            imageUri2, getContentResolver());

                    avgImageStateValue.setText("");
                    detect(mBitmap);
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
        thermalImageFile.getMeasurements().addRectangle((int)((x1+bouunding_width/3)/2.645833),(int)((y1+bouunding_height-bouunding_height/3)/2.645833),(int)(bouunding_width/3/2.645833),(int)(bouunding_height/3/2.645833));

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


        if(avgValue.value>30)
        {
            alive_text = "是活人";
        }
        else
        {
            alive_text = "不是活人";
        }
        DecimalFormat fnum   =   new   DecimalFormat("##0.00");
        avgTemp2bit=fnum.format(avgValue.value);
        //avgImage.setText(getString(R.string.avg_value_text,String.valueOf(avgValue.value),temp));
        //avgImage.setText(getString(R.string.avg_value_text,avgTemp2bit,temp));
        //avgImage.setText(getString(R.string.avg_value_text,String.valueOf(avgValue.value)));
    }

//    private void showFusionModes(ThermalImageFile thermalImageFile, ImageView irImageView, ImageView visualImageView) {
//
//        {
//            //** Get the IR image data from the image
//            //First we need to se the correct Fusion mode in the ThermalImageFile
//            thermalImageFile.getFusion().setFusionMode(FusionMode.THERMAL_ONLY);
//            ThermalLog.d(TAG, "current image width:" + thermalImageFile.getWidth() + " height:" + thermalImageFile.getHeight());
//
//            //Make a deep copy of the actual Image pixels
//            JavaImageBuffer irImage = thermalImageFile.getImage();
//            ThermalLog.d(TAG, "current IR image width:" + irImage.width + " height:" + irImage.height);
//
//            //Convert the pixel into a Android bitmap
//            BitmapAndroid irBitmap = BitmapAndroid.createBitmap(irImage);
//
//            //Set the ImageView with the IR bitmap
//            //irImageView.setImageBitmap(irBitmap.getBitMap());
//        }
//        {
//            //** Get the mixed Visual and IR image
//            //First we need to se the correct Fusion mode in the ThermalImageFile known as MSX
//            thermalImageFile.getFusion().setFusionMode(FusionMode.MSX);
//            ThermalLog.d(TAG, "current image width:" + thermalImageFile.getWidth() + " height:" + thermalImageFile.getHeight());
//
//            //Make a deep copy of the actual Image pixels
//            JavaImageBuffer image = thermalImageFile.getImage();
//            ThermalLog.d(TAG, "current MSX image width:" + image.width + " height:" + image.height);
//
//            //Convert the pixel into a Android bitmap
//            BitmapAndroid bitmap = BitmapAndroid.createBitmap(image);
//
//            //Set the ImageView with
//            //dcAndIrImageView.setImageBitmap(bitmap.getBitMap());
//        }
//        {
//            //** Get the Visual image, the visual image can be bigger then the IR and MSX image
//            //First we need to se the correct Fusion mode in the ThermalImageFile known as MSX
//            thermalImageFile.getFusion().setFusionMode(FusionMode.VISUAL_ONLY); //The visual image might also be known as DC image
//            ThermalLog.d(TAG, "current image width:" + thermalImageFile.getWidth() + " height:" + thermalImageFile.getHeight());
//
//            //Make a deep copy of the actual Image pixels
//            JavaImageBuffer photo = thermalImageFile.getImage();
//            ThermalLog.d(TAG, "current photo image width:" + photo.width + " height:" + photo.height);
//
//            //Convert the pixel into a Android bitmap
//            BitmapAndroid photoBitmap = BitmapAndroid.createBitmap(photo);
//
//
//            Bitmap myBitmap=photoBitmap.getBitMap();
//
//            FileOutputStream fOut;
//            try {
//                File dir = new File("/sdcard/demo/");
//                if (!dir.exists()) {
//                    dir.mkdir();
//                }
//
//                String tmp = "/sdcard/demo/takepicture.jpg";
//                fOut = new FileOutputStream(tmp);
//                myBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fOut);
//
//                try {
//                    fOut.flush();
//                    fOut.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//
//            } catch (FileNotFoundException e) {
//                e.printStackTrace();
//            }
//
////            // Set the ImageView with the
////            visualImageView.setImageBitmap(photoBitmap.getBitMap());
//
//            // Create a Paint object for drawing with
//            Paint myRectPaint = new Paint();
//            myRectPaint.setStrokeWidth(5);
//            myRectPaint.setColor(Color.RED);
//            myRectPaint.setStyle(Paint.Style.STROKE);
//
//            // Create a Canvas object for drawing on
//            Bitmap tempBitmap = Bitmap.createBitmap(myBitmap.getWidth(), myBitmap.getHeight(), Bitmap.Config.RGB_565);
//            Canvas tempCanvas = new Canvas(tempBitmap);
//            tempCanvas.drawBitmap(myBitmap, 0, 0, null);
//
//            // Create the Face Detector
//            FaceDetector faceDetector = new
//                    FaceDetector.Builder(getApplicationContext()).setTrackingEnabled(false)
//                    .build();
//            if(!faceDetector.isOperational()){
//                //new AlertDialog.Builder(v.getContext()).setMessage("Could not set up the face detector!").show();
//                return;
//            }
//
//            // Detect the Faces
//            Frame frame = new Frame.Builder().setBitmap(myBitmap).build();
//            SparseArray<Face> faces = faceDetector.detect(frame);
//
//            // Draw Rectangles on the Faces
//            for(int i=0; i<faces.size(); i++) {
//                Face thisFace = faces.valueAt(i);
//                x1 = thisFace.getPosition().x;
//                y1 = thisFace.getPosition().y;
//                bouunding_width = x1 + thisFace.getWidth();
//                bouunding_height = y1 + thisFace.getHeight();
//                tempCanvas.drawRoundRect(new RectF(x1, y1, bouunding_width, bouunding_height), 2, 2, myRectPaint);
//            }
//            visualImageView.setImageDrawable(new BitmapDrawable(getResources(),tempBitmap));
//        }
//    }

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

            // Create a Paint object for drawing with face
            Paint faceRect = new Paint();
            faceRect.setStrokeWidth(5);
            faceRect.setColor(Color.RED);
            faceRect.setStyle(Paint.Style.STROKE);

            // Create a Paint object for drawing with face
            Paint noseMouseRect = new Paint();
            noseMouseRect.setStrokeWidth(5);
            noseMouseRect.setColor(Color.YELLOW);
            noseMouseRect.setStyle(Paint.Style.STROKE);

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
            SparseArray<com.google.android.gms.vision.face.Face> faces = faceDetector.detect(frame);

            // Draw Rectangles on the Faces
            for(int i=0; i<faces.size(); i++) {
                com.google.android.gms.vision.face.Face thisFace = faces.valueAt(i);
                x1 = thisFace.getPosition().x;
                y1 = thisFace.getPosition().y;
                bouunding_width =  thisFace.getWidth();
                bouunding_height =  thisFace.getHeight();
                tempCanvas.drawRoundRect(new RectF(x1, y1, x1+bouunding_width, y1+bouunding_height), 2, 2, faceRect);
                tempCanvas.drawRoundRect(new RectF(x1+bouunding_width/3, y1+bouunding_height-bouunding_height/3, x1+bouunding_width-bouunding_width/3, y1+bouunding_height), 2, 2, noseMouseRect);
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

    public void detect(Bitmap bitmap) {
        File directory = getFilesDir();
        File file = new File(directory, "demo/takepicture.jpg");
        Uri imageUri= Uri.fromFile(file);
        bitmap = ImageHelper.loadSizeLimitedBitmapFromUri(
                imageUri, getContentResolver());

        // Put the image into an input stream for detection.
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, output);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(output.toByteArray());

        //setAllButtonsEnabledStatus(false);

        // Start a background task to detect faces in the image.
        new openFlirImageActivity.DetectionTask().execute(inputStream);
    }

    // Called when the "Detect" button is clicked.
    //@RequiresApi(api = Build.VERSION_CODES.O)
    public void identify() {
       // Log.d("response time before",java.time.LocalTime.now().toString());

        if(alive_text=="不是活人")
            avgImageStateValue.setText("Error");
        else
        {
            // Start detection task only if the image to detect is selected.
            if (detected && mPersonGroupId != null) {
                // Start a background task to identify faces in the image.
                List<UUID> faceIds = new ArrayList<>();
                for (Face face:  mFaceListAdapter.faces) {
                    faceIds.add(face.faceId);
                }

                //setAllButtonsEnabledStatus(false);

                new openFlirImageActivity.IdentificationTask(mPersonGroupId).execute(
                        faceIds.toArray(new UUID[faceIds.size()]));
            } else {
                // Not detected or person group exists.
                //setInfo("Please select an image and create a person group first.");
            }
        }
    }

    // Add a log item.
    private void addLog(String log) {
        LogHelper.addIdentificationLog(log);
    }

    private void showSDKVersion() {
//        TextView thermalSdkTextView = findViewById(R.id.thermal_sdk_version_text_view);
//        String thermalSDKtext = getString(R.string.thermal_sdk_version, thermalSDKversion);
//        ThermalLog.d(TAG, "showSDKVersion() " + thermalSDKtext);
//        thermalSdkTextView.setText(thermalSDKtext);
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
    // The adapter of the ListView which contains the person groups.
    private class PersonGroupListAdapter extends BaseAdapter {
        List<String> personGroupIdList;

        // Initialize with detection result.
        PersonGroupListAdapter() {
            personGroupIdList = new ArrayList<>();

            Set<String> personGroupIds
                    = StorageHelper.getAllPersonGroupIds(openFlirImageActivity.this);

            for (String personGroupId: personGroupIds) {
                personGroupIdList.add(personGroupId);
                if (mPersonGroupId != null && personGroupId.equals(mPersonGroupId)) {
                    personGroupIdList.set(
                            personGroupIdList.size() - 1,
                            mPersonGroupListAdapter.personGroupIdList.get(0));
                    mPersonGroupListAdapter.personGroupIdList.set(0, personGroupId);
                }
            }
        }

        @Override
        public int getCount() {
            return personGroupIdList.size();
        }

        @Override
        public Object getItem(int position) {
            return personGroupIdList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater layoutInflater =
                        (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = layoutInflater.inflate(R.layout.item_person_group, parent, false);
            }
            convertView.setId(position);

            // set the text of the item
            String personGroupName = StorageHelper.getPersonGroupName(
                    personGroupIdList.get(position), openFlirImageActivity.this);
            int personNumberInGroup = StorageHelper.getAllPersonIds(
                    personGroupIdList.get(position), openFlirImageActivity.this).size();
            ((TextView)convertView.findViewById(R.id.text_person_group)).setText(
                    String.format(
                            "%s (Person count: %d)",
                            personGroupName,
                            personNumberInGroup));

            if (position == 0) {
                ((TextView)convertView.findViewById(R.id.text_person_group)).setTextColor(
                        Color.parseColor("#3399FF"));
            }

            return convertView;
        }
    }

}
