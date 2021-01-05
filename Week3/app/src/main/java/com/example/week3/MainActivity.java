package com.example.week3;

import android.Manifest;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.icu.text.SimpleDateFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

public class MainActivity extends AppCompatActivity
        implements ActivityCompat.OnRequestPermissionsResultCallback{
    public static final String EXTRA_MESSAGE = "com.example.myfirstapp.MESSAGE";
    public static final String TAG = "test";
    private static final int PERMISSION_REQUEST_CAMERA = 0;
    private static final int PERMISSION_REQUEST_PHOTO = 2;
    private static final int REQUEST_TAKE_PHOTO = 1;
    private static final int REQUEST_CHOOSE_PHOTO = 3;
    private static final int REQUEST_SEARCH_PHOTO = 4;
    private View mLayout;
    private File output;  // 设置拍照的图片文件
    private Uri photoUri;  // 拍摄照片的路径
    private ImageView picture;
    private Uri takephoto;
    private String currentPhotoPath;
    private long downloadID;
    DownloadManager downloadManager;
    private Uri uri = null;

    /**
     * 用于保存拍照图片的uri
     */
    private Uri mCameraUri;

    /**
     * 用于保存图片的文件路径，Android 10以下使用图片路径访问图片
     */
    private String mCameraImagePath;

    /**
     *  是否是Android 10以上手机
     */
    private boolean isAndroidQ = Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q;
    private EditText url_link;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        builder.detectFileUriExposure();

        // 初始化view， 添加listener
        super.onCreate(savedInstanceState);
        //load layout
        Log.i(TAG, "onCreate: create");
        setContentView(R.layout.activity_main);
        mLayout = findViewById(R.id.main_layout); // 主界面
//        findViewById(R.id.open_camera).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                showCameraPreview();
//            }
//        });
        findViewById(R.id.open_photo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showPhotoPreview();
            }
        });
        findViewById(R.id.open_search).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showSearchPreview();
            }
        });
        url_link =(EditText)findViewById(R.id.urltext);
        findViewById(R.id.url).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDownloadPreview();
            }
        });
        //add a button

    }
    private void showDownloadPreview() {
        // BEGIN_INCLUDE(startCamera)
        // Check if the Camera permission has been granted
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            Snackbar.make(mLayout,
                    R.string.photo_permission_available,
                    Snackbar.LENGTH_SHORT).show();
            downloadPhoto();
        } else {
            RequestPhotoPreview();
        }
    }
    private void downloadPhoto(){
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        String dir ="/storage/emulated/0/Download";
        String url = url_link.getText().toString();
        try {
            uri = writeToDisk(MainActivity.this, url,storageDir.getAbsolutePath());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private final BroadcastReceiver onDownloadComplete = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID,-1);
            if (downloadID == id){
                Uri uri = downloadManager.getUriForDownloadedFile(id);
                unregisterReceiver(onDownloadComplete);
                Log.i(TAG, "onReceive: ");
                String path = getFileRealPath(uri);
                openPhoto(path);

            }
        }
    };

    public Uri writeToDisk(Context context, @NonNull String imageUrl, @NonNull String downloadSubfolder) {
        Uri imageUri = Uri.parse(imageUrl);
        String fileName = imageUri.getLastPathSegment();
        String downloadSubpath = downloadSubfolder + fileName;
//        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
//        String imageFileName = "JPEG_" + timeStamp;
//        String downloadSubpath = downloadSubfolder + "/"+imageFileName+".jpeg";
        downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        registerReceiver(onDownloadComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

        DownloadManager.Request request = new DownloadManager.Request(imageUri);
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDescription(imageUrl);
        request.allowScanningByMediaScanner();
        request.setDestinationUri(getDownloadDestination(downloadSubpath));
        try {
            downloadID = downloadManager.enqueue(request);

        }catch (Exception e){
            e.printStackTrace();
        }
        return getDownloadDestination(downloadSubpath);
    }
    @NonNull private static Uri getDownloadDestination(String downloadSubpath) {
        File picturesFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File destinationFile = new File(picturesFolder, downloadSubpath);
        destinationFile.mkdirs();
        return Uri.fromFile(destinationFile);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        // BEGIN_INCLUDE(onRequestPermissionsResult)
        if (requestCode == PERMISSION_REQUEST_CAMERA) {
            // Request for camera permission.
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission has been granted. Start camera preview Activity.
                Snackbar.make(mLayout, R.string.camera_permission_granted,
                        Snackbar.LENGTH_SHORT)
                        .show();
                startCamera();
            } else {
                // Permission request was denied.
                Snackbar.make(mLayout, R.string.camera_permission_denied,
                        Snackbar.LENGTH_SHORT)
                        .show();
            }
        }
        else if(requestCode == PERMISSION_REQUEST_PHOTO) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission has been granted. Start camera preview Activity.
                Snackbar.make(mLayout, R.string.photo_permission_granted,
                        Snackbar.LENGTH_SHORT)
                        .show();
                launchPhoto();
            } else {
                // Permission request was denied.
                Snackbar.make(mLayout, R.string.photo_permission_denied,
                        Snackbar.LENGTH_SHORT)
                        .show();
            }
        }
        // END_INCLUDE(onRequestPermissionsResult)
    }

    private void showCameraPreview() {
        // BEGIN_INCLUDE(startCamera)
        // Check if the Camera permission has been granted
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            // Permission is already available, start camera preview
            Snackbar.make(mLayout,
                    R.string.camera_permission_available,
                    Snackbar.LENGTH_SHORT).show();
            startCamera();
        } else {
            // Permission is missing and must be requested.
            requestCameraPermission();
        }
        // END_INCLUDE(startCamera)
    }

    /**
     * Requests the {@link android.Manifest.permission#CAMERA} permission.
     * If an additional rationale should be displayed, the user has to launch the request from
     * a SnackBar that includes additional information.
     */
    private void requestCameraPermission() {
        // Permission has not been granted and must be requested.
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.CAMERA)) {
            // Provide an additional rationale to the user if the permission was not granted
            // and the user would benefit from additional context for the use of the permission.
            // Display a SnackBar with cda button to request the missing permission.
            Snackbar.make(mLayout, R.string.camera_access_required,
                    Snackbar.LENGTH_INDEFINITE).setAction(R.string.ok, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Request the permission
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.CAMERA},
                            PERMISSION_REQUEST_CAMERA);
                }
            }).show();

        } else {
            Snackbar.make(mLayout, R.string.camera_unavailable, Snackbar.LENGTH_SHORT).show();
            // Request the permission. The result will be received in onRequestPermissionResult().
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CAMERA);
        }
    }
//start camera
    private void startCamera() {
//        File file = new File(Environment.getExternalStorageDirectory(), "takePhotoDemo");
//        if (!file.exists()) {
//            // 如果文件路径不存在则直接创建一个文件夹
//            file.mkdir();
//        }
//        // 把时间作为拍摄照片的保存路径;
//        output = new File(file, System.currentTimeMillis() + ".jpq");
//        // 如果该照片已经存在就删除它,然后新创建一个
//        try {
//            if (output.exists()) {
//                output.delete();
//            }
//            output.createNewFile();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        // 隐式打开拍摄照片
//        photoUri = Uri.fromFile(output);
//        Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
//        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
//        startActivityForResult(intent, REQUEST_TAKE_PHOTO);

        final String dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)+"/pic/";
        File newdir = new File(dir);
        newdir.mkdirs();

        Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // 判断是否有相机
        if (captureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            Uri photoUri = null;
            Intent takePic = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(takePic,0);
//            if (isAndroidQ) {
//                // 适配android 10
//                photoUri = createImageUri();
//            } else {
//                try {
//                    photoFile = createImageFile();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//                if (photoFile != null) {
//                    Uri photoURI = FileProvider.getUriForFile(Objects.requireNonNull(getApplicationContext()),
//                            BuildConfig.APPLICATION_ID + ".provider", photoFile);
////                    Uri photoURI = FileProvider.getUriForFile(this,
////                            "com.example.android.fileprovider",
////                            photoFile);
//                    takephoto = photoURI;
//                    captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
//                    startActivityForResult(captureIntent, REQUEST_TAKE_PHOTO);
                }
//                if (photoFile != null) {
//
//                    mCameraImagePath = photoFile.getAbsolutePath();
//                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//                        //适配Android 7.0文件权限，通过FileProvider创建一个content类型的Uri //
//                        photoUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", photoFile);
//                    } else {
//                        photoUri = Uri.fromFile(photoFile);
//                    }
//                }
            }

//            mCameraUri = photoUri;
//            if (photoUri != null) {
//                captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
//                Log.i("aaaaaaa", "startCamera: "+photoUri);
//                captureIntent.putExtra("return-data", true);
//                takephoto = photoUri;
//               // captureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
//                startActivityForResult(captureIntent, REQUEST_TAKE_PHOTO);
//            }

    private Uri createImageUri() {
        String status = Environment.getExternalStorageState();
        // 判断是否有SD卡,优先使用SD卡存储,当没有SD卡时使用手机存储
        if (status.equals(Environment.MEDIA_MOUNTED)) {
            return getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, new ContentValues());
        } else {
            return getContentResolver().insert(MediaStore.Images.Media.INTERNAL_CONTENT_URI, new ContentValues());
        }
    }

    /**
     * 创建保存图片的文件
     * @return
     * @throws IOException
     */
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }
//    private File createImageFile() throws IOException {
//        String imageName = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
//        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
//        if (!storageDir.exists()) {
//            storageDir.mkdir();
//        }
//        File tempFile = new File(storageDir, imageName);
//        if (!Environment.MEDIA_MOUNTED.equals(EnvironmentCompat.getStorageState(tempFile))) {
//            return null;
//        }
//        return tempFile;
//    }

    private void showPhotoPreview() {
        // BEGIN_INCLUDE(startCamera)
        // Check if the Camera permission has been granted
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            // Permission is already available, start camera preview
            Snackbar.make(mLayout,
                    R.string.photo_permission_available,
                    Snackbar.LENGTH_SHORT).show();
            launchPhoto();
        } else {
            // Permission is missing and must be requested.
            RequestPhotoPreview();
        }
        // END_INCLUDE(startCamera)
    }

    private void showSearchPreview() {
        // BEGIN_INCLUDE(startCamera)
        // Check if the Camera permission has been granted
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            // Permission is already available, start camera preview
            Snackbar.make(mLayout,
                    R.string.photo_permission_available,
                    Snackbar.LENGTH_SHORT).show();
            //change func
            launchSearch();
        } else {
            // Permission is missing and must be requested.
            RequestPhotoPreview();
        }
        // END_INCLUDE(startCamera)
    }
    private void RequestPhotoPreview() {
        // Permission has not been granted and must be requested.
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            Snackbar.make(mLayout, R.string.photo_access_required,
                    Snackbar.LENGTH_INDEFINITE).setAction(R.string.ok, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Request the permission
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            PERMISSION_REQUEST_PHOTO);
                }
            }).show();

        } else {
            Snackbar.make(mLayout, R.string.photo_unavailable, Snackbar.LENGTH_SHORT).show();
            // Request the permission. The result will be received in onRequestPermissionResult().
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_PHOTO);
        }
    }

    private void launchPhoto() {
//        Intent intent = new Intent();
//        intent.setType("image/*");
//        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true);
//        intent.setAction(Intent.ACTION_GET_CONTENT);
//        startActivityForResult();
        Intent intent = new Intent(this, PhotoGallery.class);
//        startActivityForResult(intent, REQUEST_CHOOSE_PHOTO);
//        Intent intent = new Intent(Intent.ACTION_PICK);
//        intent.setType("image/*");String
        startActivityForResult(intent, REQUEST_CHOOSE_PHOTO);
    }

    private void launchSearch() {
        Intent intent = new Intent(this, CustomPhotoGalleryActivity.class);
        startActivityForResult(intent, REQUEST_SEARCH_PHOTO);
    }
    protected void openPhoto(String path){

        Intent intent = new Intent(this,Detail.class);
        intent.putExtra("path_file", path);
        startActivity(intent);
    }
    protected void openPhotoList(String[] pathList){
        Intent intent = new Intent(this,Batch.class);
        intent.putExtra("path_list", pathList);
        startActivity(intent);
    }

    private  String getFileRealPath(Uri uri){
        Cursor cursor = getContentResolver().query(uri, new String[]{MediaStore.Images.ImageColumns.DATA},null,null,null);
        int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
        cursor.moveToFirst();
        String path = cursor.getString(idx);
        cursor.close();
        return path;
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_TAKE_PHOTO:
                if (resultCode == RESULT_OK) {
                    //data = null result code = -1??
                    openPhoto(takephoto.getPath());
//                    try {//launch?
//                        Bitmap bit = BitmapFactory.decodeStream(getContentResolver().openInputStream(photoUri));//拍完照到这一页
//                        picture.setImageBitmap(bit);
//                    } catch (FileNotFoundException e) {
//                        e.printStackTrace();
//                        Log.d("tag", e.getMessage());
//                        Toast.makeText(this, "程序崩溃", Toast.LENGTH_SHORT).show();
//                    }
                } else {
                    Log.i("REQUEST_TAKE_PHOTO", "拍摄失败");
                }
                break;
            case REQUEST_CHOOSE_PHOTO:
                if (resultCode == RESULT_OK) {
                    if (resultCode == RESULT_OK) {
                        ArrayList<String> imagesPathList = new ArrayList<String>();
                        String[] imagesPath = data.getStringExtra("data").split("\\|");
//                        ArrayList<Uri> uris = new ArrayList<>();
//                        if (data.getData()!=null){
//                            uris.add(data.getData());
//                        }else if (data.getClipData()!=null){
//                            ClipData clipData = data.getClipData();
//                            for (int i = 0; i < clipData.getItemCount(); i++) {
//                                uris.add(clipData.getItemAt(i).getUri());
//                            }
//                        }

//                        for (Uri uri : uris) imagesPathList.add(getFileRealPath(uri));
                        for (int i=0;i<imagesPath.length;i++){
                            // 加入所有选定的path
                            imagesPathList.add(imagesPath[i]);
                        }
                        if (imagesPathList.size() == 1) openPhoto(imagesPathList.get(0));
                        else openPhotoList(imagesPathList.stream().toArray(String[]::new));
                    }

                } else {
                    Log.i("REQUEST_CHOOSE_PHOTO", "读取失败");
                }
                break;
            case REQUEST_SEARCH_PHOTO:
                // 请求搜索部分
                if (resultCode == RESULT_OK) {
                    ArrayList<String> imagesPathList = new ArrayList<String>();
                    String[] imagesPath = data.getStringExtra("data").split("\\|");

                    for (int i=0;i<imagesPath.length;i++){
                        // 加入所有选定的path
                        imagesPathList.add(imagesPath[i]);
                    }

                    // 如果只有一个，就编辑
                    if (imagesPathList.size() == 1) openPhoto(imagesPathList.get(0));
                    else openPhotoList(imagesPathList.stream().toArray(String[]::new));


                }else {
                    Log.i("REQUEST_SEARCH_PHOTO", "搜索失败");
                }
                break;

        }
    }



//        Thread t = new Thread(new Runnable() {
//            @Override
//            public void run() {
//                try{Thread.sleep(10000);}
//                catch (Exception e){
//                    Log.d("testlog", "onCreate: sleep overtime");
//                }
//                sendMessage();
//            }
//        });
//        t.start();
//        try{Thread.sleep(10000);}
//        catch (Exception e){
//            Log.d("testlog", "onCreate: sleep overtime");
//        }
//        sendMessage();

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy: destroy");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG, "onStart: start");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume: resume");

    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause: ");
    }
}