package com.example.imageedit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;
import com.theartofdev.edmodo.cropper.CropOverlayView;

public class MainActivity extends AppCompatActivity {
    EditText mEditText;
    ImageView mImageView;
    Button scanButton;
   String cameraPermisions[];
   String storagePermision[];
   Uri imageUril;
   private static final int REQUEST_CODE_FOR_CAMERA = 200;
   private static final int STORAGE_REQUEST_CODE = 400;
   private static final int GALLERY_CODE= 700;
   private static final int PICK_CAMERA_CODE = 1000;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mEditText = findViewById(R.id.scanResult);
        mImageView= findViewById(R.id.imageDisplay);
        scanButton = findViewById(R.id.scanBtn);
        cameraPermisions = new String[]{Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermision = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};

        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showImage();
            }
        });
    }

    private void showImage() {
        String [] items = {"Camera" ,"Gallery" };
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle(" Select Image to Scan");
        dialog.setItems(items, new DialogInterface.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0){
                    if (!checkCameraPermission()){
                        requestPermissions();
                    }else{
                        pickCamera();
                    }
                }
                if (which == 1){
                    if (!checkStoragePermission()){
                        requestStoragePermissions();
                    }else{
                        pickGallery();
                    }
                }
            }
            
        });
        dialog.create().show();
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this,cameraPermisions,REQUEST_CODE_FOR_CAMERA);
    }


    private void pickCamera() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE,"New Photo");
        values.put(MediaStore.Images.Media.DESCRIPTION,"Image To Text");
        imageUril = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values);
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT,imageUril);
        startActivityForResult(cameraIntent,PICK_CAMERA_CODE);

    }
    private void pickGallery() {
      Intent mIntent = new Intent(Intent.ACTION_PICK);
      mIntent.setType("image/*");
        startActivityForResult(mIntent, GALLERY_CODE);
    }
    private void requestStoragePermissions() {
        ActivityCompat.requestPermissions(this,storagePermision,STORAGE_REQUEST_CODE);
    }
    private boolean checkStoragePermission(){
     boolean result = ContextCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE)==
             (PackageManager.PERMISSION_GRANTED);
     return result;
    }
    private boolean checkCameraPermission(){
        boolean result = ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA)
                == (PackageManager.PERMISSION_GRANTED);
        boolean result1 = ContextCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == (PackageManager.PERMISSION_GRANTED);
        return result && result1 ;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case REQUEST_CODE_FOR_CAMERA:
                if (grantResults.length>0){
                    boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean writeStorageAcceptedb = grantResults[0] ==
                            PackageManager.PERMISSION_GRANTED;
                    if (cameraAccepted && writeStorageAcceptedb){
                        pickCamera();
                    }else {
                        Toast.makeText(this, "permission Denied", Toast.LENGTH_LONG).show();
                    }
                }
                break;
            case STORAGE_REQUEST_CODE:
                if (grantResults.length>0){
                    boolean writeStorageAcceptedDB = grantResults[0] ==
                            PackageManager.PERMISSION_GRANTED;
                    if (writeStorageAcceptedDB){
                        pickGallery();
                    }else {
                        Toast.makeText(this, "permission Denied", Toast.LENGTH_LONG).show();
                    }
                }
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode == RESULT_OK && requestCode == GALLERY_CODE) {
            CropImage.activity(data.getData())
                    .setGuidelines(CropImageView.Guidelines.ON)
                    .start(this);
        }
        if (resultCode == RESULT_OK && requestCode == PICK_CAMERA_CODE) {
            CropImage.activity(imageUril)
                    .setGuidelines(CropImageView.Guidelines.ON)
                    .start(this);
        }
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                Uri resultUri = result.getUri();
                mImageView.setImageURI(resultUri);
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), resultUri);
                    TextRecognizer recognizer = new TextRecognizer.Builder(getApplicationContext()).build();
                    if (!recognizer.isOperational()) {
                        Toast.makeText(this, "error", Toast.LENGTH_SHORT).show();
                    } else {
                        Frame frame = new Frame.Builder().setBitmap(bitmap).build();
                        SparseArray<TextBlock> detect = recognizer.detect(frame);
                        StringBuilder sb = new StringBuilder();

                        for (int j = 0; j < detect.size(); j++) {
                            TextBlock myItems = detect.valueAt(j);
                            sb.append(myItems.getValue());
                            sb.append("\n");
                        }
                        mEditText.append("\n" + sb.toString());
                    }

                } catch (Exception e) {
                    Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show();
                }
            }

        }
    }
}
