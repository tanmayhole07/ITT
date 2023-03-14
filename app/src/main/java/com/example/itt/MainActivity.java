package com.example.itt;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private MaterialButton inputImageBtn;
    private MaterialButton recognizedTextBtn;
    private ShapeableImageView imageIv;
    private EditText recognizedTextEt;
    private Button sendToGSBtn;

    private static final String TAG = "MAIN_TAG";

    private Uri imageUri = null;

    private static final int CAMERA_REQUEST_CODE = 100;
    private static final int STORAGE_REQUEST_CODE = 101;

    private String[] cameraPermissions;
    private String[] storagePermissions;


    private ProgressDialog progressDialog;

    private TextRecognizer textRecognizer;
    String recognizedText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cameraPermissions = new String[]{Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};

        inputImageBtn = findViewById(R.id.inputImagebtn);
        recognizedTextBtn = findViewById(R.id.recognizeTextbtn);
        imageIv = findViewById(R.id.imageIv);
        recognizedTextEt = findViewById(R.id.recognizedTextEt);
        sendToGSBtn = findViewById(R.id.sendToGSBtn);

        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please wait");
        progressDialog.setCanceledOnTouchOutside(false);

        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        inputImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showInputImageDialog();
            }
        });

        recognizedTextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (imageUri == null){
                    Toast.makeText(MainActivity.this, "Pick Image First...", Toast.LENGTH_SHORT).show();
                }else {
                    recognizeTextFromImage();
                }
            }
        });

        sendToGSBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                int count = 1;
                String  updatedText = recognizedTextEt.getText().toString();
                String[] liness = updatedText.split("\\r?\\n");
                count = liness.length;
                Toast.makeText(MainActivity.this, Arrays.toString(liness), Toast.LENGTH_SHORT).show();

                if (count == 1){
                    addToGoogleSheet(recognizedTextEt.getText()+"");
                }else {
                    for (int i = 0; i <count ; i++) {
                        addToGoogleSheet(liness[i]);
                    }
                }

            }
        });

    }

    private void recognizeTextFromImage() {
        progressDialog.setMessage("Preparing Image...");
        progressDialog.show();
        try {
            InputImage inputImage = InputImage.fromFilePath(this, imageUri);
            progressDialog.setMessage("Recognizing text");
            Task<Text> textTaskResult = textRecognizer.process(inputImage)
                    .addOnSuccessListener(new OnSuccessListener<Text>() {
                        @Override
                        public void onSuccess(Text text) {
                            progressDialog.dismiss();
                            recognizedText = text.getText();
                            recognizedTextEt.setText(recognizedText);







                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {

                            progressDialog.dismiss();
                            Toast.makeText(MainActivity.this, "Failed recognizing text "+e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        } catch (Exception e){
            progressDialog.dismiss();
            Toast.makeText(this, "Failed preparing image due to "+ e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void addToGoogleSheet(String rt) {

        String[] splitedText = rt.split("\\s+");
        if (splitedText.length == 5){
            String SubTask = splitedText[0];
            String Task = splitedText[1];
            String Category = splitedText[2];
            String StartDate = splitedText[3];
            String EndDate = splitedText[4];


            StringRequest stringRequest = new StringRequest(Request.Method.POST, "https://script.google.com/macros/s/AKfycbw5-PMIs5pE02gedvgx-O3KJCp7VHsgusd2uhSphs7MyWaxfTf8DOgzNh3ZqTJc8ol5jw/exec", new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {

                    Toast.makeText(MainActivity.this, "Data Sent Successfully ", Toast.LENGTH_SHORT).show();
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {

                }
            }){
                @Nullable
                @Override
                protected Map<String, String> getParams(){

                    Map<String, String> params = new HashMap<>();
                    params.put("action", "addStudent");
//                params.put("vdata",rt);
                    params.put("vSubTask", SubTask);
                    params.put("vTask", Task);
                    params.put("vCategory", Category);
                    params.put("vStartDate", StartDate);
                    params.put("vEndDate", EndDate);

                    return params;



                }
            };

            int socketTimeOut = 50000;
            RetryPolicy retryPolicy = new DefaultRetryPolicy(socketTimeOut, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
            stringRequest.setRetryPolicy(retryPolicy);

            RequestQueue requestQueue = Volley.newRequestQueue(this);
            requestQueue.add(stringRequest);
        }

    }



    private void showInputImageDialog() {
        PopupMenu popupMenu = new PopupMenu(this, inputImageBtn);
        popupMenu.getMenu().add(Menu.NONE, 1, 1, "CAMERA");
        popupMenu.getMenu().add(Menu.NONE, 2, 2, "GALLERY");
        popupMenu.show();

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                int id = menuItem.getItemId();
                if (id == 1){
                    if (checkCameraPermission()){
                        pickImageCamera();
                    }else {
                        requestCameraPermission();
                    }
                }else if (id == 2){

                    if (checkStoragePermission()){
                        pickImageGallery();
                    }else {
                        requestStoragePermission();
                    }
                }
                return true;
            }
        });
    }

    private void pickImageGallery(){
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        galleryActivityResultLauncher.launch(intent);
    }

    private ActivityResultLauncher<Intent> galleryActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK){

                        Intent data = result.getData();
                        imageUri = data.getData();
                        imageIv.setImageURI(imageUri);
                    }
                    else {
                        Toast.makeText(MainActivity.this, "Cancelled", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    private void pickImageCamera(){
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "Sample Title");
        values.put(MediaStore.Images.Media.DESCRIPTION,"Sample Description");
        imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        cameraActivityResultLauncher.launch(intent);
    }

    private ActivityResultLauncher<Intent> cameraActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK){

                        imageIv.setImageURI(imageUri);
                    }
                    else {

                        Toast.makeText(MainActivity.this, "Cancelled", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    private boolean checkStoragePermission(){

        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.R){
            return Environment.isExternalStorageManager();
        }
        else {
            boolean result = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);
            return result;
        }


    }

    private void requestStoragePermission(){

        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.R){
            try {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                Uri uri = Uri.fromParts("package", this.getPackageName(), null);
                intent.setData(uri);
                storageActivityResultLauncher.launch(intent);
            }catch (Exception e){
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                storageActivityResultLauncher.launch(intent);
            }
        }
        else {
            ActivityCompat.requestPermissions(this, storagePermissions, STORAGE_REQUEST_CODE);

        }

    }

    private ActivityResultLauncher<Intent> storageActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.R){
                        if (Environment.isExternalStorageManager()){
//                            createFolder();
                        }
                    }
                    else {

                    }
                }
            }
    );


    private boolean checkCameraPermission(){
        boolean cameraResult = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == (PackageManager.PERMISSION_GRANTED);
        boolean storageResult = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);

        return cameraResult && storageResult;
    }

    private void requestCameraPermission(){


            ActivityCompat.requestPermissions(this, cameraPermissions, CAMERA_REQUEST_CODE);





    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode){
            case CAMERA_REQUEST_CODE : {
                if (grantResults.length>0){
                    boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean storageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;

                    if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.R){
                        if (cameraAccepted ){
                            pickImageCamera();
                        }
                        else {
                            Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show();
                        }
                    }else {
                        if (cameraAccepted && storageAccepted){
                            pickImageCamera();
                        }
                        else {
                            Toast.makeText(this, "Camera & Storage permission are required", Toast.LENGTH_SHORT).show();
                        }
                    }


                }
                else {
                    Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT).show();
                }
            }

            break;

            case STORAGE_REQUEST_CODE : {

                if (grantResults.length>0){
                    boolean storageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    if (storageAccepted){
                        pickImageGallery();
                    }else {
                        Toast.makeText(this, "Storage permission is required", Toast.LENGTH_SHORT).show();
                    }
                }
            }
            break;
        }
    }
}