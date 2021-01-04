package com.example.week3;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.exifinterface.media.ExifInterface;

import com.amap.api.maps2d.AMap;
import com.bumptech.glide.Glide;


import com.example.week3.exifTool.ExifTagsContainer;
import com.example.week3.exifTool.PhotoDetailPresenter;
import com.example.week3.exifTool.Type;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.MapFragment;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import gdut.bsx.share2.Share2;
import gdut.bsx.share2.ShareContentType;

public class Batch extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMapClickListener {
    private static final String TAG = "CustomPhotoGalleryActivity";
    private GridView grdImages;
    private Button gps_del;
    private Button gps_edit;
    private Button camera_del;
    private Button camera_edit;
    private Button date_del;
    private Button date_edit;
    private Button all_del;
    private GoogleMap mMap;
    private AlertDialog mapDialog;
    private ArrayList<PhotoDetailPresenter> photoList = new ArrayList<>();
    private ArrayList<Uri> path = new ArrayList<>();
    private double lon = 0;
    private double lat = 0;
    private PhotoDetailPresenter pre;
    double temp_lon = 0;
    double temp_lat = 0;

    //Calendar
    private int mYear;
    private int mMonth;
    private int mDay;


    /**
     * Overrides methods
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        builder.detectFileUriExposure();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.batch);
        Intent intent = this.getIntent();
        String[] imageList = intent.getStringArrayExtra("path_list");
        ArrayList<Integer> integers = new ArrayList<>();
        int index = 0;
        for (String str : imageList) {
            PhotoDetailPresenter pre = new PhotoDetailPresenter();
            pre.initialize(str);
            photoList.add(pre);
            path.add(pre.imageUri);
            integers.add(index);
            index++;
        }
        //display
        GridView gridView = (GridView) findViewById(R.id.bacthImage);
        gridView.setAdapter(new ImageGridAdapter(this, path));
        //set pre
        pre = photoList.get(0);
//        if(pre.getLongitude() != null && pre.getLatitude()!=null) {
//            temp_lon = pre.getLongitude();
//            temp_lat = pre.getLatitude();
//        }
        setDialogMap();
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map_two);
//        mapFragment.getMapAsync(new OnMapReadyCallback() {
//            @Override
//            public void onMapReady(GoogleMap googleMap) {
//                if(temp_lon != pre.getLongitude() && temp_lat!=pre.getLatitude()) {
//                    temp_lon = pre.getLongitude();
//                    temp_lat = pre.getLatitude();
//                    for (PhotoDetailPresenter pre : photoList) {
//                        pre.setExifGPS(temp_lon, temp_lat);
//                        pre.setLatitude(temp_lon);
//                        pre.setLongitude(temp_lat);
//                    }
//                }
//            }
//        });
        mapFragment.getMapAsync(this);

        Calendar ca = Calendar.getInstance();
        mYear = ca.get(Calendar.YEAR);
        mMonth = ca.get(Calendar.MONTH);
        mDay = ca.get(Calendar.DAY_OF_MONTH);

        ((ImageView) findViewById(R.id.image_photo_gps)).setImageResource(R.drawable.ic_pin_drop_black_24dp);
        ((ImageView) findViewById(R.id.image_photo_camera)).setImageResource(R.drawable.ic_photo_camera_black_24dp);
        ((ImageView) findViewById(R.id.image_photo_date)).setImageResource(R.drawable.ic_date_range_black_24dp);

        Context context = this;
        //gps
        gps_del = findViewById(R.id.gps_delete);
        gps_edit = findViewById(R.id.gps_edit);
        gps_edit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //fine
                temp_lon = pre.getLongitude();
                temp_lat = pre.getLatitude();
                openDialogMap();

            }
        });
        gps_del.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder adb = new AlertDialog.Builder(context);
                adb.setTitle("Are you sure to delete all the GPS information?")
                        .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                removeTags(pre.exifTagsList.get(0));
                            }
                        }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                }).show();
            }
        });
        //camera
        camera_del = findViewById(R.id.camera_delete);
        camera_edit = findViewById(R.id.camera_edit);
        camera_edit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editCamera();

            }
        });
        camera_del.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder adb = new AlertDialog.Builder(context);
                adb.setTitle("Are you sure to delete all the camera properties?")
                        .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                removeTags(pre.exifTagsList.get(2));
                            }
                        }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                }).show();

            }
        });
        //date
        date_del = findViewById(R.id.date_delete);
        date_edit = findViewById(R.id.date_edit);
        date_edit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editDate();

            }
        });

        date_del.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder adb = new AlertDialog.Builder(context);
                adb.setTitle("Are you sure to delete all the date information?")
                        .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                removeTags(pre.exifTagsList.get(1));
                            }
                        }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                }).show();

            }
        });
        //all delete
        all_del = findViewById(R.id.deleteAll);
        all_del.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder adb = new AlertDialog.Builder(context);
                adb.setTitle("Are you sure to delete all the information? This operation could not be reverted.")
                        .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                for(PhotoDetailPresenter pre: photoList){
                                    pre.removeAllTags();
                                }
                            }
                        }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                }).show();

            }
        });

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.tool_bar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.toolbar_share) {
            ArrayList<Uri> uris = new ArrayList<Uri>();
            for(PhotoDetailPresenter pre: photoList){
                uris.add(pre.imageUri);
            }
            boolean multiple = uris.size() > 1;
            Intent intent = new Intent(multiple ? android.content.Intent.ACTION_SEND_MULTIPLE
                    : android.content.Intent.ACTION_SEND);

            if (multiple) {
                intent.setType("image/*");
                intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
            } else {
                intent.setType("image/*");
                intent.putExtra(Intent.EXTRA_STREAM, uris.get(0));
            }
            startActivity(Intent.createChooser(intent, "Share"));

            return true;
        }else if(item.getItemId() == R.id.toolbar_delete){

            AlertDialog.Builder adb = new AlertDialog.Builder(this);
            adb.setTitle("Are you sure to delete all the information? This operation could not be reverted.")
                    .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            for(PhotoDetailPresenter pre: photoList){
                                pre.removeAllTags();
                            }
                        }
                    }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.cancel();
                }
            }).show();

            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    protected void openDialogMap(){
        mapDialog.show();
    }
    protected void setDialogMap() {//ExifTagsContainer item) {

        AlertDialog.Builder mapDialogBuilder = new AlertDialog.Builder(Batch.this);
        final View  dialogView = LayoutInflater.from(Batch.this)
                    .inflate(R.layout.map_dialog_two, null);
        mapDialogBuilder.setTitle("MapDialog");
        mapDialogBuilder.setView(dialogView);

        mapDialogBuilder.setPositiveButton("confirm",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (mMap != null) {
                            pre.setExifGPS(lat, lon);
                            pre.setLatitude(lat);
                            pre.setLongitude(lon);
                            for (PhotoDetailPresenter pre : photoList) {
                        pre.setExifGPS(lon, lat);
                        pre.setLatitude(lat);
                        pre.setLongitude(lon);
                    }
                            refreshMapTarget(mMap);
                        }
                    }
                });
        mapDialogBuilder.setNegativeButton("close",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //...To-do nothing
                        if (mMap != null) refreshMapTarget(mMap);
                    }
                });
        mapDialog = mapDialogBuilder.create();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMapClickListener(this);
        refreshMapTarget(mMap);
//        if(temp_lon != pre.getLongitude() && temp_lat!=pre.getLatitude()) {
//                    temp_lon = pre.getLongitude();
//                    temp_lat = pre.getLatitude();
//                    for (PhotoDetailPresenter pre : photoList) {
//                        pre.setExifGPS(temp_lon, temp_lat);
//                        pre.setLatitude(temp_lon);
//                        pre.setLongitude(temp_lat);
//                    }
//                }
    }
    @Override
    public void onMapClick(LatLng point) {

        lat = point.latitude;
        lon = point.longitude;

        // Add a marker in Sydney and move the camera
        LatLng target = new LatLng(lat, lon);
        mMap.clear();
        mMap.addMarker(new MarkerOptions()
                .position(target)
                .title("Marker to select"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(target));

    }
    //for 循环之后再修改
    public void refreshMapTarget(GoogleMap googleMap) {
        mMap = googleMap;
        if (pre.getLatitude() != null && pre.getLongitude() != null) {
            lat = pre.getLatitude();
            lon = pre.getLongitude();
        }
        LatLng target = new LatLng(lat, lon);
        mMap.clear();
        mMap.addMarker(new MarkerOptions()
                .position(target)
                .title("Marker to select"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(target));
    }


    protected void editDate(){
        DatePickerDialog datePickerDialog = new DatePickerDialog(Batch.this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        mYear = year;
                        mMonth = month;
                        mDay = dayOfMonth;
                        String dateString = mYear + ":" + (mMonth + 1) + ":" + mDay;
                        Date date = new Date();
                        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
                        String time = formatter.format(date);
                        if (!(pre.getExifInterface().getAttribute(ExifInterface.TAG_DATETIME) == null)){
                            String[] list = pre.getExifInterface().getAttribute(ExifInterface.TAG_DATETIME).split(" ");
                            if (list.length == 2){
                                time = list[1];
                            }
                        }
                        String dateTime = dateString + " " + time;
                        for(PhotoDetailPresenter pre: photoList) {
                            pre.setExifDate(dateTime);
                        }
                    }
                },
                mYear, mMonth, mDay);
        datePickerDialog.show();

    }
//
    protected void editCamera(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        // Get the layout inflater
        final LayoutInflater inflater = this.getLayoutInflater();

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        final View view  = inflater.inflate(R.layout.dialog_edit_camera, null);
        builder.setView(view)
                // Add action buttons
                .setPositiveButton(R.string.dialog_edit_camera_properties_confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        String camera_make = ((EditText)view.findViewById(R.id.editText_camera_make)).getText().toString();
                        String camera_model = ((EditText)view.findViewById(R.id.editText_camera_model)).getText().toString();
                        for(PhotoDetailPresenter pre: photoList)
                            {
                                pre.setExifCamera(camera_make, camera_model);
                            }
                    }
                })
                .setNegativeButton(R.string.dialog_edit_camera_properties_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog dialog_edit_camera = builder.create();
        dialog_edit_camera.show();



    }

    protected void removeTags(ExifTagsContainer item) {
        switch (item.getType()) {
            case DATE:
                for(PhotoDetailPresenter pre:photoList){
                    pre.removeExifDate();
                }
                break;
            case CAMERA_PROPERTIES:
                for(PhotoDetailPresenter pre:photoList) {
                    pre.removeExifCamera();
                }
                break;
            case GPS:
                for(PhotoDetailPresenter pre:photoList) {
                    pre.removeExifGPS();
                }
        }
    }


    @Override
    public void onBackPressed() {
        setResult(Activity.RESULT_CANCELED);
        super.onBackPressed();
    }




    public class ImageGridAdapter extends BaseAdapter {

        private Context mContext;
        private List<Uri> mUris;

        public ImageGridAdapter(Context c, List<Uri> uris) {
            this.mUris = uris;
            this.mContext = c;
        }

        @Override
        public int getCount() {
            try {
                return path.size();
            } catch (NullPointerException e) {
                return 0;
            }
        }

        @Override
        public Object getItem(int position) {

            return path.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }


        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView imageView;
            if (convertView == null) {
                imageView = new ImageView(mContext);
                imageView.setLayoutParams(new GridView.LayoutParams(400, 400));
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                imageView.setPadding(5, 5, 5, 5);
            } else {
                imageView = (ImageView) convertView;
            }

            try {
                imageView.setImageURI(path.get(position));
            } catch (NullPointerException e) {
            }

            return imageView;
        }

    }
}
