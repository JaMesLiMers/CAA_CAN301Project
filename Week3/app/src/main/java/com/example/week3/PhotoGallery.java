package com.example.week3;


import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.example.week3.exifTool.PhotoDetailPresenter;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PhotoGallery extends Activity {
    private static final String TAG = "PhotoGallery";
    private GridView grdImages;
    private Button edit;
    private Button btnSearch;
    private EditText strSearch;
    private View notFound;

    private ImageAdapter imageAdapter;
    private String[] all_arrPath;
    private boolean[] all_thumbnailsselection;
    private int all_ids[];
    private int all_count;
    private double all_lat_long[][];
    private String all_city[];
    private EditText searchbar;
    private DownloadManager downloadManager;
    long downloadID;
    int checked;

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
                if (searchbar!=null){
                    searchbar.setEnabled(true);
                    searchbar.setText("");
                }
            }
        }
    };
    Pattern pattern = Pattern.compile("^(https?|chrome)://[^\\s$.?#].[^\\s]*$", Pattern.CASE_INSENSITIVE);
    /**
     * Overrides methods
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.photos);
        // 所有图片
        RequestPhotoPreview();

    }

    private void init(){

        downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        grdImages= (GridView) findViewById(R.id.gridImages);
        // 确定按钮
        edit= (Button) findViewById(R.id.edit);
        notFound = (View)findViewById(R.id.not_found);
        //identify the content is url or location
        searchbar = findViewById(R.id.searchSeparate);

        searchbar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @SuppressLint("ClickableViewAccessibility")
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length()==0) {
                    searchbar.setCompoundDrawablesWithIntrinsicBounds(R.drawable.places_ic_search, 0, 0, 0);
                    search("");
                    searchbar.setOnTouchListener(null);
                }else {
                    String input = searchbar.getText().toString();
                    Matcher matcher = pattern.matcher(input);
                    if (matcher.find()){
                        searchbar.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_get_app_black_24dp,0,R.drawable.ic_highlight_off_black_24dp,0);
                    }else {
                        searchbar.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_pin_drop_white_24dp,0,R.drawable.ic_highlight_off_black_24dp,0);
                    }

                    searchbar.setOnTouchListener((v, event) -> {
                        final int DRAWABLE_LEFT = 0;
                        final int DRAWABLE_TOP = 1;
                        final int DRAWABLE_RIGHT = 2;
                        final int DRAWABLE_BOTTOM = 3;

                        if(event.getRawX() >= (searchbar.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width()))
                        {
                            searchbar.setText("");
                            searchbar.setCompoundDrawablesWithIntrinsicBounds(R.drawable.places_ic_search, 0, 0, 0);
                            search("");
                            return true;
                        }
                        return false;
                    });
                }


            }

            @Override
            public void afterTextChanged(Editable s) { }
        });
        searchbar.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                String input = searchbar.getText().toString();
                Matcher matcher = pattern.matcher(input);
                if (matcher.find()){
                    if (keyCode == KeyEvent.KEYCODE_ENTER){
                        downloadTask(input);
                        searchbar.setEnabled(false);
                    }
                }else {
                    if (keyCode == KeyEvent.KEYCODE_ENTER) search(input);
                }

            }
            return false;
        });

        edit.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                Log.i(TAG, "onClick: asdasd");
                int cnt = 0;
                for (boolean b : all_thumbnailsselection) {
                    if (b) {
                        cnt++;
                    }
                }

                if (cnt == 0) {
                    Toast.makeText(getApplicationContext(), "Please select at least one image", Toast.LENGTH_LONG).show();
                } else {
                    String[] path = new String[cnt];
                    int pointer = 0;
                    for (int i = 0; i < all_thumbnailsselection.length; i++) {
                        if (all_thumbnailsselection[i]) {
                            path[pointer]=all_arrPath[i];
                            pointer++;
                        }
                    }

                    if (path.length==1) openPhoto(path[0]);
                    if (path.length>1) openPhoto(path);
                }
            }
        });


        searchbar.setOnFocusChangeListener((v,has)->{
            if (has)found(true);
        });

        final String[] columns = { MediaStore.Images.Media.DATA, MediaStore.Images.Media._ID }; //{"_data, _id"}
        final String orderBy = MediaStore.Images.Media._ID; // "_id"


        // 获取所有的图片
        Cursor imagecursor = getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, columns, null, null, orderBy);
        int image_column_index = imagecursor.getColumnIndex(MediaStore.Images.Media._ID);
        this.all_count = imagecursor.getCount();        // 图片总数量
        this.all_arrPath = new String[this.all_count];      // 所有的图片的路径列表
        this.all_ids = new int[all_count];                       // 所有的图片的id列表
        this.all_thumbnailsselection = new boolean[this.all_count]; // 所有的图片是否被选择的部分
        this.all_lat_long = new double[this.all_count][2];
        this.all_city = new String[this.all_count];

        // 获取图片信息
        for (int i = 0; i < this.all_count; i++) {
            imagecursor.moveToPosition(i);
            // get图片id
            all_ids[i] = imagecursor.getInt(image_column_index);
            // get path位置
            int dataColumnIndex = imagecursor.getColumnIndex(MediaStore.Images.Media.DATA);
            // 设置path
            all_arrPath[i] = imagecursor.getString(dataColumnIndex);
        }
        imageAdapter = new ImageAdapter(all_ids.length, all_ids);
        grdImages.setAdapter((ListAdapter) imageAdapter);

        // 确认选择回调函数

        getAllLatLon();

        // 获取所有的City信息
        getAllCity();

        // 设定图片预览 （下面的class负责）
        // imageAdapter = new ImageAdapter(this.all_count, this.all_ids);
        // grdImages.setAdapter((ListAdapter) imageAdapter);

        // 查询完毕关闭cursor
        imagecursor.close();


    }

    private static final int PERMISSION_REQUEST_CAMERA = 0;
    private static final int PERMISSION_REQUEST_PHOTO = 2;
    private void RequestPhotoPreview() {
        // Permission has not been granted and must be requested.
        ActivityCompat.requestPermissions(PhotoGallery.this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_PHOTO);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

         if(requestCode == PERMISSION_REQUEST_PHOTO) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission has been granted. Start camera preview Activity.
                init();

            } else {
                // Permission request was denied.
                Toast.makeText(this,"permission not granted.",Toast.LENGTH_LONG).show();
                finish();
            }
        }
        // END_INCLUDE(onRequestPermissionsResult)
    }
    private void found(boolean found){
        if (found){
            notFound.setVisibility(View.GONE);
            grdImages.setVisibility(View.VISIBLE);
        }else {
            notFound.setVisibility(View.VISIBLE);
            grdImages.setVisibility(View.GONE);
        }
    }

    public boolean getAllLatLon(){
        try{
            for (int i = 0; i < this.all_count; i++) {
                PhotoDetailPresenter pre =  new PhotoDetailPresenter();
                pre.initialize(all_arrPath[i]);

                // get lat lon
                double lat = 104.1954;
                double lon = 35.8617;
                try {
                    lat = pre.getLatitude();
                    lon = pre.getLongitude();
                }catch (NullPointerException e){

                }finally {
                    all_lat_long[i][0] = lat;
                    all_lat_long[i][1] = lon;
                }

            }
            return true;
        }catch (Exception e) {
            return false;
        }
    }

    public void search(String keyword){
        grdImages.setAdapter(null);
        int count = 0;
        ArrayList ids_ = new ArrayList<Integer>();

        // 按照搜索过滤所有的图片
        for (int i = 0; i < all_count; i++) {
            if (all_city[i].toLowerCase().equals(keyword)){
                count += 1;
                ids_.add(all_ids[i]);
            }
        }
        // int[]
        int[] ids = ids_.stream().mapToInt(i -> (int) i).toArray();

        // 设定图片预览 （下面的class负责）
        found(count!=0);
        imageAdapter = new ImageAdapter(count, ids);
        grdImages.setAdapter((ListAdapter) imageAdapter);
    }

    public boolean getAllCity(){
        try{
            for (int i = 0; i < this.all_count; i++) {
                // get图片id
                double lat = all_lat_long[i][0];
                double lon = all_lat_long[i][1];

                // 这里可能要异步运行
                this.all_city[i] = getAddress(lat, lon);
            }
            return true;
        }catch (Exception e) {
            return false;
        }

    }

    public String getAddress(double latitude, double longitude) {
        String cityName = "";
        List<Address> addresses;
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

        try {
            addresses = geocoder.getFromLocation(latitude,longitude,1);
            if (geocoder.isPresent()) {
                StringBuilder stringBuilder = new StringBuilder();
                if (addresses.size()>0) {
                    Address returnAddress = addresses.get(0);

                    cityName = returnAddress.getLocality();
                    if (cityName == null){
                        cityName = "";
                    }
                    String name = returnAddress.getFeatureName();
                    String subLocality = returnAddress.getSubLocality();
                    String country = returnAddress.getCountryName();
                    String region_code = returnAddress.getCountryCode();
                    String zipcode = returnAddress.getPostalCode();
                    String state = returnAddress.getAdminArea();

                }
            } else {

            }
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            // remove "市"
            cityName = cityName.split(" ")[0];

            return cityName;
        }
    }


    @NonNull
    private static Uri getDownloadDestination(String downloadSubpath) {
        File picturesFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File destinationFile = new File(picturesFolder, downloadSubpath);
        destinationFile.mkdirs();
        return Uri.fromFile(destinationFile);
    }

    private void downloadTask(String url){
        downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        Uri uri = Uri.parse(url);
        registerReceiver(onDownloadComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

        DownloadManager.Request request = new DownloadManager.Request(uri);
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDescription(url);
        request.allowScanningByMediaScanner();
        String fileName = uri.getLastPathSegment();
        String downloadSubpath = getExternalFilesDir(Environment.DIRECTORY_PICTURES).getAbsolutePath() + fileName;
        request.setDestinationUri(getDownloadDestination(downloadSubpath));

        try {
            downloadID = downloadManager.enqueue(request);
        }catch (Exception e){
            e.printStackTrace();
        }


    }

    private  String getFileRealPath(Uri uri){
        Cursor cursor = getContentResolver().query(uri, new String[]{MediaStore.Images.ImageColumns.DATA},null,null,null);
        int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
        cursor.moveToFirst();
        String path = cursor.getString(idx);
        cursor.close();
        return path;
    }

    private void openPhoto(String path){
        Intent intent = new Intent(this,Detail.class);
        intent.putExtra("path_file", path);
        startActivity(intent);
    }
    private void openPhoto(String[] pathList){
        Intent intent = new Intent(this,Batch.class);
        intent.putExtra("path_list", pathList);
        startActivity(intent);
    }


    @Override
    public void onBackPressed() {
        setResult(Activity.RESULT_CANCELED);
        super.onBackPressed();

    }

    /**
     * Class method
     */

    /**
     * This method used to set bitmap.
     * @param iv represented ImageView
     * @param id represented id
     */

    private void setBitmap(final ImageView iv, final int id) {

        new AsyncTask<Void, Void, Bitmap>() {

            @Override
            protected Bitmap doInBackground(Void... params) {
                return MediaStore.Images.Thumbnails.getThumbnail(getApplicationContext().getContentResolver(), id, MediaStore.Images.Thumbnails.MICRO_KIND, null);
            }

            @Override
            protected void onPostExecute(Bitmap result) {
                super.onPostExecute(result);
                iv.setImageBitmap(result);
            }
        }.execute();
    }
    private void updateTheTextOnEditButton(){
        switch (checked){
            case 0:
                edit.setText(R.string.edit_notice);
                edit.setEnabled(false);
                edit.setBackgroundColor(getResources().getColor(R.color.gray,getTheme()));
                break;
            case 1:
                edit.setText(checked+" photo has been selected, click to edit");
                edit.setEnabled(true);
                edit.setBackgroundColor(getResources().getColor(R.color.colorPrimary,getTheme()));
                break;
            default:
                edit.setText(checked+" photos have been selected, click to edit");
                edit.setEnabled(true);
                edit.setBackgroundColor(getResources().getColor(R.color.colorPrimary,getTheme()));
        }

    }

    /**
     * List adapter
     * @author tasol
     */

    public class ImageAdapter extends BaseAdapter {
        private LayoutInflater mInflater;
        private int count;
        private int[] ids;

        public ImageAdapter(int count, int[] ids) {
            mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            this.count = count;
            this.ids = ids;
        }


        public int getCount() {
            return count;
        }

        public Object getItem(int position) {
            return position;
        }

        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final ViewHolder holder;
            if (convertView == null) {
                holder = new ViewHolder();
                convertView = mInflater.inflate(R.layout.custom_gallery_item, null);
                holder.imgThumb = (ImageView) convertView.findViewById(R.id.imgThumb);
                holder.chkImage = (CheckBox) convertView.findViewById(R.id.chkImage);

                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            holder.chkImage.setId(position);
            holder.imgThumb.setId(position);
            holder.chkImage.setOnClickListener(new View.OnClickListener() {

                public void onClick(View v) {
                    CheckBox cb = (CheckBox) v;
                    int id = cb.getId();
                    if (all_thumbnailsselection[id]) {
                        cb.setChecked(false);
                        checked-=1;
                        all_thumbnailsselection[id] = false;
                    } else {
                        cb.setChecked(true);
                        all_thumbnailsselection[id] = true;
                        checked+=1;
                    }
                    updateTheTextOnEditButton();

                }
            });
            holder.imgThumb.setOnClickListener(new View.OnClickListener() {

                public void onClick(View v) {
                    int id = holder.chkImage.getId();
                    if (all_thumbnailsselection[id]) {
                        holder.chkImage.setChecked(false);
                        all_thumbnailsselection[id] = false;
                        checked-=1;
                    } else {
                        holder.chkImage.setChecked(true);
                        all_thumbnailsselection[id] = true;
                        checked+=1;
                    }
                    updateTheTextOnEditButton();
                }
            });
            try {
                setBitmap(holder.imgThumb, ids[position]);
            } catch (Throwable e) {
            }
            holder.chkImage.setChecked(all_thumbnailsselection[position]);
            holder.id = position;
            return convertView;
        }
    }


    /**
     * Inner class
     * @author tasol
     */
    class ViewHolder {
        ImageView imgThumb;
        CheckBox chkImage;
        int id;
    }

}
