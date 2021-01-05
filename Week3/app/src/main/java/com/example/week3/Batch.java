package com.example.week3;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.exifinterface.media.ExifInterface;

import com.example.week3.exifTool.ExifTagsContainer;
import com.example.week3.exifTool.PhotoDetailPresenter;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;


public class Batch extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMapClickListener {
    private static final String TAG = "CustomPhotoGalleryActivity";
    private GoogleMap mMap;
    private AlertDialog mapDialog;
    private ArrayList<PhotoDetailPresenter> photoList = new ArrayList<>();
    private ArrayList<Uri> path = new ArrayList<>();
    private double lon = 0;
    private double lat = 0;
    private PhotoDetailPresenter pre;
    private final Context context = this;
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
        Log.i(TAG, "onCreate: " + pre);

        setDialogMap();
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map_two);

        mapFragment.getMapAsync(this);

        Calendar ca = Calendar.getInstance();
        mYear = ca.get(Calendar.YEAR);
        mMonth = ca.get(Calendar.MONTH);
        mDay = ca.get(Calendar.DAY_OF_MONTH);

        //GPS
        Button gps_del = findViewById(R.id.gps_delete);
        Button gps_edit = findViewById(R.id.gps_edit);
        gps_edit.setOnClickListener(view -> openDialogMap());
        gps_del.setOnClickListener(view -> {
            AlertDialog.Builder adb = new AlertDialog.Builder(context);
            adb.setTitle("Are you sure to delete all the GPS information?")
                    .setPositiveButton("Confirm", (dialog, id) -> removeTags(pre.exifTagsList.get(0)))
                    .setNegativeButton("Cancel", (dialog, id) -> dialog.cancel()).show();

        });
        //camera
        Button camera_del = findViewById(R.id.camera_delete);
        Button camera_edit = findViewById(R.id.camera_edit);
        camera_edit.setOnClickListener(view -> editCamera());
        camera_del.setOnClickListener(view -> {
            AlertDialog.Builder adb = new AlertDialog.Builder(context);
            adb.setTitle("Are you sure to delete all the camera information?")
                    .setPositiveButton("Confirm", (dialog, id) -> removeTags(pre.exifTagsList.get(2)))
                    .setNegativeButton("Cancel", (dialog, id) -> dialog.cancel()).show();

        });
        //date
        Button date_del = findViewById(R.id.date_delete);
        Button date_edit = findViewById(R.id.date_edit);
        date_edit.setOnClickListener(view -> editDate());
        date_del.setOnClickListener(view -> {
            AlertDialog.Builder adb = new AlertDialog.Builder(context);
            adb.setTitle("Are you sure to delete all the date information?")
                    .setPositiveButton("Confirm", (dialog, id) -> removeTags(pre.exifTagsList.get(1)))
                    .setNegativeButton("Cancel", (dialog, id) -> dialog.cancel()).show();

        });
        //all delete
        Button all_del = findViewById(R.id.deleteAll);
        all_del.setOnClickListener(view -> {
            AlertDialog.Builder adb = new AlertDialog.Builder(context);
            adb.setTitle("Are you sure to delete all the information?")
                    .setPositiveButton("Confirm", (dialog, id) -> {
                        for (PhotoDetailPresenter pre : photoList) {
                            pre.removeAllTags();
                        }
                    })
                    .setNegativeButton("Cancel", (dialog, id) -> dialog.cancel()).show();

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
            for (PhotoDetailPresenter pre : photoList) {
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
        } else if (item.getItemId() == R.id.toolbar_delete) {

            AlertDialog.Builder adb = new AlertDialog.Builder(this);
            adb.setTitle("Are you sure to delete all the information? This operation could not be reverted.")
                    .setPositiveButton("Confirm", (dialog, id) -> {
                        for (PhotoDetailPresenter pre : photoList) {
                            pre.removeAllTags();
                        }
                    })
                    .setNegativeButton("Cancel", (dialog, id) -> dialog.cancel()).show();

            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    protected void openDialogMap() {
        mapDialog.show();
    }

    protected void setDialogMap() {
        //ExifTagsContainer item)

        AlertDialog.Builder mapDialogBuilder = new AlertDialog.Builder(Batch.this);
        final View dialogView = LayoutInflater.from(Batch.this)
                .inflate(R.layout.map_dialog_two, null);
        mapDialogBuilder.setTitle("MapDialog");
        mapDialogBuilder.setView(dialogView);
        mapDialogBuilder.setPositiveButton("confirm",
                (dialog, which) -> {
                    if (mMap != null) {
                        pre.setExifGPS(lat, lon);
                        pre.setLatitude(lat);
                        pre.setLongitude(lon);
                        for (PhotoDetailPresenter pre : photoList) {
                            pre.setExifGPS(lat, lon);
                            pre.setLatitude(lat);
                            pre.setLongitude(lon);
                        }
                        refreshMapTarget(mMap);
                    }
                });
        mapDialogBuilder.setNegativeButton("close",
                (dialog, which) -> {
                    if (mMap != null) refreshMapTarget(mMap);
                });
        mapDialog = mapDialogBuilder.create();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMapClickListener(this);
        refreshMapTarget(mMap);
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

    //after the for loop
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


    protected void editDate() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(Batch.this,
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
                    for (PhotoDetailPresenter pre : photoList) {
                        pre.setExifDate(dateTime);
                    }
                },
                mYear, mMonth, mDay);
        datePickerDialog.show();

    }

    //
    protected void editCamera() {
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
                    for (PhotoDetailPresenter pre : photoList) {
                        pre.setExifCamera(camera_make, camera_model);
                    }
                })
                .setNegativeButton(R.string.dialog_edit_camera_properties_cancel, (dialog, id) -> dialog.cancel());
        AlertDialog dialog_edit_camera = builder.create();
        dialog_edit_camera.show();


    }

    protected void removeTags(ExifTagsContainer item) {
        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        switch (item.getType()) {
            case DATE:
                adb.setTitle("Are you sure to delete the data information?")
                        .setPositiveButton("Confirm", (dialog, id) -> {
                            for (PhotoDetailPresenter pre : photoList) {
                                pre.removeExifDate();
                            }
                        })
                        .setNegativeButton("Cancel", (dialog, id) -> dialog.cancel()).show();
                break;
            case CAMERA_PROPERTIES:
                adb.setTitle("Are you sure to delete the data information?")
                        .setPositiveButton("Confirm", (dialog, id) -> {
                            for (PhotoDetailPresenter pre : photoList) {
                                pre.removeExifCamera();
                            }
                        })
                        .setNegativeButton("Cancel", (dialog, id) -> dialog.cancel()).show();

                break;
            case GPS:
                adb.setTitle("Are you sure to delete the GPS information?")
                        .setPositiveButton("Confirm", (dialog, id) -> {
                            pre.removeExifGPS();
                            for (PhotoDetailPresenter pre : photoList) {
                                pre.removeExifGPS();
                            }
                        })
                        .setNegativeButton("Cancel", (dialog, id) -> dialog.cancel()).show();

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
