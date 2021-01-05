package com.example.week3;

import android.app.DatePickerDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.amap.api.maps2d.AMap;
import com.amap.api.maps2d.MapView;

import com.bumptech.glide.Glide;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.exifinterface.media.ExifInterface;

import com.example.week3.exifTool.*;
import com.example.week3.exifTool.Type;
import com.example.week3.exifTool.PhotoDetailPresenter;
import com.example.week3.exifTool.ViewExtensionKt;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Date;

import gdut.bsx.share2.Share2;
import gdut.bsx.share2.ShareContentType;


public class Detail extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMapClickListener, AMap.OnMapClickListener {
    private PhotoDetailPresenter pre = new PhotoDetailPresenter();

    //Calendar
    private int mYear;
    private int mMonth;
    private int mDay;

    //Map
    private GoogleMap mMap;
    private AMap aMap;
    private AlertDialog mapDialog;
    private AlertDialog aMapDialog;
    private double lon = 0;
    private double lat = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // let the VM ignore the exposure of Uri
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        builder.detectFileUriExposure();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.detail);

        //initialize the Exif information of the photo selected
        pre.initialize(getIntent());
        setImage(pre.file.getName(), pre.imageUri);
        setExifData(pre.exifTagsList);

        // Calendar
        Calendar ca = Calendar.getInstance();
        mYear = ca.get(Calendar.YEAR);
        mMonth = ca.get(Calendar.MONTH);
        mDay = ca.get(Calendar.DAY_OF_MONTH);


        // for google map
        setDialogMap();
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // for aMap
        setDialogAMap(savedInstanceState);

    }

    //tool bar
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.tool_bar_menu, menu);
        return true;
    }

    //tool bar interaction
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.toolbar_share) {
            //share single photo using the Share2 builder
            new Share2.Builder(this)
                    .setContentType(ShareContentType.IMAGE)
                    .setShareFileUri(pre.imageUri)
                    .setTitle("Share Image")
                    .build()
                    .shareBySystem();
            return true;
        } else if (item.getItemId() == R.id.toolbar_delete) {
            AlertDialog.Builder adb = new AlertDialog.Builder(this);
            adb.setTitle("Are you sure to delete all the data information? This operation could not be reverted.")
                    .setPositiveButton("Confirm", (dialog, id) -> {
                        pre.removeAllTags();
                        reloadUI();
                    })
                    .setNegativeButton("Cancel", (dialog, id) -> dialog.cancel()).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    protected void setImage(String fileName, Uri imageUri) {
        ((TextView) findViewById(R.id.text_image_info)).setText((CharSequence) (fileName + '\n'));
        TextView tv = (TextView) findViewById(R.id.text_image_info);
        Glide.with(this).load(imageUri).into((ImageView) findViewById(R.id.image_photo));
        Log.i("photo", "post it");
    }

    //recycle
    protected void setExifData(List<ExifTagsContainer> list) {
        for (final ExifTagsContainer container : list) {
            TextView textView;
            switch (container.getType()) {
                case GPS:
                    textView = findViewById(R.id.text_type_gps);
                    textView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_pin_drop_black_24dp, 0, 0, 0);
                    ((TextView) findViewById(R.id.text_property_gps)).setText(container.getOnStringProperties());
                    findViewById(R.id.gps).setOnClickListener(v -> showAlertDialogWhenItemIsPressed(container));
                    break;
                case CAMERA_PROPERTIES:
                    textView = findViewById(R.id.text_type_camera);
                    textView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_insert_photo_24px, 0, 0, 0);
                    ((TextView) findViewById(R.id.text_property_camera)).setText(container.getOnStringProperties());
                    findViewById(R.id.camera).setOnClickListener(v -> showAlertDialogWhenItemIsPressed(container));
                    break;
                case DATE:
                    textView = findViewById(R.id.text_type_date);
                    textView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_date_range_black_24dp, 0, 0, 0);
                    ((TextView) findViewById(R.id.text_property_date)).setText(container.getOnStringProperties());
                    findViewById(R.id.date).setOnClickListener(v -> showAlertDialogWhenItemIsPressed(container));
                    break;
                case DIMENSION:
                    textView = findViewById(R.id.text_type_dimension);
                    textView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_photo_size_select_actual_black_24dp, 0, 0, 0);
                    ((TextView) findViewById(R.id.text_property_dimension)).setText(container.getOnStringProperties());
                    findViewById(R.id.dimension).setOnClickListener(v -> showAlertDialogWhenItemIsPressed(container));
                    break;
                case OTHER:
                    textView = findViewById(R.id.text_type_other);
                    textView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_blur_on_black_24dp, 0, 0, 0);
                    ((TextView) findViewById(R.id.text_property_other)).setText(container.getOnStringProperties());
                    findViewById(R.id.other).setOnClickListener(v -> showAlertDialogWhenItemIsPressed(container));
                    break;
            }
        }
    }

    //the alert dialog list
    protected void showAlertDialogWhenItemIsPressed(final ExifTagsContainer item) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        ArrayList<String> optionList = new ArrayList<>();

        optionList.add(this.getResources().getString(R.string.alert_item_copy_to_clipboard));
        if (item.getType() == Type.GPS) {
            Log.i("type", "GPS");
            optionList.add(this.getResources().getString(R.string.alert_item_open_map));
            optionList.add(this.getResources().getString(R.string.alert_item_open_a_map));
            optionList.add(this.getResources().getString(R.string.alert_item_remove_gps_tags));
        } else if (item.getType() == Type.DATE) {
            Log.i("type", "DATE");
            optionList.add(this.getResources().getString(R.string.alert_item_edit));
            optionList.add(this.getResources().getString(R.string.alert_item_remove_date));
        } else if (item.getType() == Type.CAMERA_PROPERTIES) {
            Log.i("type", "CAMERA");
            optionList.add(this.getResources().getString(R.string.alert_item_edit_camera));
            optionList.add(this.getResources().getString(R.string.alert_item_remove_camera));
        }
        //add a title
        alertDialogBuilder.setTitle(this.getResources().getString(R.string.alert_select_an_action));

        alertDialogBuilder.setItems(Arrays.copyOf(optionList.toArray(), optionList.size(), String[].class), (dialog, which) -> {
            if (which == 0) {
                copyDataToClipboard(item);
            } else if (which == 1) {
                if (item.getType() == Type.GPS) openDialogMap(item); //open the google map
                else if (item.getType() == Type.DATE) editDate(item); //edit the date
                else if (item.getType() == Type.CAMERA_PROPERTIES) editCamera(item);
            } else if (which == 2) {
                if (item.getType() == Type.GPS) openDialogAMap(item); //open a map
                else removeTags(item); //remove Exif information
            } else if (which == 3) {
                removeTags(item); //remove Exif information
            }
        });
        AlertDialog dialog = alertDialogBuilder.create();
        dialog.show();
    }

    protected void copyDataToClipboard(ExifTagsContainer item) {
        ClipboardManager clipboard = (ClipboardManager) this.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(item.getType().name(), item.getOnStringProperties());
        clipboard.setPrimaryClip(clip);
        ViewExtensionKt.showSnackbar(this.findViewById(R.id.coordinator_layout), R.string.text_copied_to_clipboard_message);
    }

    protected void openDialogMap(ExifTagsContainer item) {
        mapDialog.show();
    }

    protected void openDialogAMap(ExifTagsContainer item) {
        aMapDialog.show();
    }

    protected void setDialogAMap(Bundle savedInstanceState) {//ExifTagsContainer item) {

        AlertDialog.Builder mapDialogBuilder = new AlertDialog.Builder(Detail.this);
        final View dialogView = LayoutInflater.from(Detail.this)
                .inflate(R.layout.amap_dialog, null);
        MapView mapView = (MapView) dialogView;

        mapView.onCreate(savedInstanceState);

        // on aMap ready
        aMap = mapView.getMap();
        aMap.setOnMapClickListener(this);

        refreshMapTarget(aMap);

        mapDialogBuilder.setTitle("MapDialog");
        mapDialogBuilder.setView(dialogView);
        mapDialogBuilder.setPositiveButton("确定",
                (dialog, which) -> {
                    if (aMap != null) {
                        pre.setExifGPS(lat, lon);
                        pre.setLatitude(lat);
                        pre.setLongitude(lon);
                        reloadUI();
                        refreshMapTarget(aMap);
                        if (mMap != null) {
                            refreshMapTarget(mMap);
                        }
                    }
                });
        mapDialogBuilder.setNegativeButton("关闭",
                (dialog, which) -> {
                    if (aMap != null) refreshMapTarget(aMap);
                });
        aMapDialog = mapDialogBuilder.create();
    }

    protected void setDialogMap() {//ExifTagsContainer item) {
        AlertDialog.Builder mapDialogBuilder = new AlertDialog.Builder(Detail.this);
        final View dialogView = LayoutInflater.from(Detail.this)
                .inflate(R.layout.map_dialog, null);
        mapDialogBuilder.setTitle("MapDialog");
        mapDialogBuilder.setView(dialogView);
        mapDialogBuilder.setPositiveButton("确定",
                (dialog, which) -> {
                    if (mMap != null) {
                        pre.setExifGPS(lat, lon);
                        pre.setLatitude(lat);
                        pre.setLongitude(lon);
                        reloadUI();
                        refreshMapTarget(mMap);
                        if (aMap != null) {
                            refreshMapTarget(aMap);
                        }
                    }
                });
        mapDialogBuilder.setNegativeButton("关闭",
                (dialog, which) -> {
                    if (mMap != null) refreshMapTarget(mMap);
                });

        mapDialog = mapDialogBuilder.create();
    }


    protected void editDate(ExifTagsContainer item) {
        DatePickerDialog datePickerDialog = new DatePickerDialog(Detail.this,
                (view, year, month, dayOfMonth) -> {
                    mYear = year;
                    mMonth = month;
                    mDay = dayOfMonth;
                    String dateString = mYear + ":" + (mMonth + 1) + ":" + mDay;
                    Date date = new Date();
                    SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
                    String time = formatter.format(date);
                    if (!(pre.getExifInterface().getAttribute(ExifInterface.TAG_DATETIME) == null)) {
                        String[] list = pre.getExifInterface().getAttribute(ExifInterface.TAG_DATETIME).split(" ");
                        if (list.length == 2) {
                            time = list[1];
                        }
                    }
                    String dateTime = dateString + " " + time;
                    pre.setExifDate(dateTime);
                    reloadUI();
                },
                mYear, mMonth, mDay);
        datePickerDialog.show();
    }

    protected void editCamera(ExifTagsContainer item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        // Get the layout inflater
        final LayoutInflater inflater = this.getLayoutInflater();
        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        final View view = inflater.inflate(R.layout.dialog_edit_camera, null);
        builder.setView(view)
                // Add action buttons
                .setPositiveButton(R.string.dialog_edit_camera_properties_confirm, (dialog, id) -> {
                    String camera_make = ((EditText) view.findViewById(R.id.editText_camera_make)).getText().toString();
                    String camera_model = ((EditText) view.findViewById(R.id.editText_camera_model)).getText().toString();
                    pre.setExifCamera(camera_make, camera_model);
                    reloadUI();
                })
                .setNegativeButton(R.string.dialog_edit_camera_properties_cancel, (dialog, id) -> dialog.cancel());
        AlertDialog dialog_edit_camera = builder.create();
        dialog_edit_camera.show();
    }

    protected void removeTags(ExifTagsContainer item) {
        // confirm alert dialog before remove
        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        switch (item.getType()) {
            case DATE:
                adb.setTitle("Are you sure to delete the data information?")
                        .setPositiveButton("Confirm", (dialog, id) -> {
                            pre.removeExifDate();
                            reloadUI();
                        })
                        .setNegativeButton("Cancel", (dialog, id) -> dialog.cancel()).show();
                break;
            case CAMERA_PROPERTIES:
                adb.setTitle("Are you sure to delete the data information?")
                        .setPositiveButton("Confirm", (dialog, id) -> {
                            pre.removeExifCamera();
                            reloadUI();
                        })
                        .setNegativeButton("Cancel", (dialog, id) -> dialog.cancel()).show();
                break;
            case GPS:
                adb.setTitle("Are you sure to delete the GPS information?")
                        .setPositiveButton("Confirm", (dialog, id) -> {
                            pre.removeExifGPS();
                            reloadUI();
                        })
                        .setNegativeButton("Cancel", (dialog, id) -> dialog.cancel()).show();
        }
        reloadUI();
    }



    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMapClickListener(this);
        refreshMapTarget(mMap);
    }

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

    // for refresh the UI to display the edited information
    private void reloadUI() {
        pre.initialize(getIntent());
        setImage(pre.file.getName(), pre.imageUri);
        setExifData(pre.exifTagsList);
    }

    public void refreshMapTarget(AMap a_map) {
        aMap = a_map;
        if (pre.getLatitude() != null && pre.getLongitude() != null) {
            lat = pre.getLatitude();
            lon = pre.getLongitude();
        }
        com.amap.api.maps2d.model.LatLng target = new com.amap.api.maps2d.model.LatLng(lat, lon);
        aMap.clear();
        aMap.addMarker(new com.amap.api.maps2d.model.MarkerOptions()
                .position(target)
                .title("Marker to select"));
        aMap.moveCamera(com.amap.api.maps2d.CameraUpdateFactory.newLatLng(target));
    }

    @Override
    public void onMapClick(com.amap.api.maps2d.model.LatLng point) {
        lat = point.latitude;
        lon = point.longitude;

        // Add a marker in Sydney and move the camera
        com.amap.api.maps2d.model.LatLng target = new com.amap.api.maps2d.model.LatLng(lat, lon);
        aMap.clear();
        aMap.addMarker(new com.amap.api.maps2d.model.MarkerOptions()
                .position(target)
                .title("Marker to select"));
        aMap.moveCamera(com.amap.api.maps2d.CameraUpdateFactory.newLatLng(target));
    }
}
