package com.example.week3;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.TextView;
import com.bumptech.glide.Glide;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.week3.exifTool.*;
import com.example.week3.exifTool.Type;
import com.example.week3.exifTool.PhotoDetailPresenter;
import com.example.week3.exifTool.ViewExtensionKt;
import com.example.week3.interactor.mapInteractor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;

public class Detail extends AppCompatActivity {
    private RecyclerView.Adapter mAdapter;
    private RecyclerView recyclerView;
    private PhotoDetailPresenter pre = new PhotoDetailPresenter();


    //Calendar
    int mYear;
    int mMonth;
    int mDay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.detail);
       // mLayout = findViewById(R.id.main_layout); // 主界面
        pre.initialize(getIntent());
        setImage(pre.file.getName(),pre.imageUri);
        setExifData(pre.exifTagsList);
        getAddressByTriggerRequest(pre);// 检查GPS可不可以用
        // Calendar
        Calendar ca = Calendar.getInstance();
        mYear = ca.get(Calendar.YEAR);
        mMonth = ca.get(Calendar.MONTH);
        mDay = ca.get(Calendar.DAY_OF_MONTH);
    }
    protected void setImage(String fileName, Uri imageUri){
        ((TextView)findViewById(R.id.text_image_info)).setText((CharSequence)(fileName + '\n'));
        TextView tv = (TextView)findViewById(R.id.text_image_info);
        Glide.with(this).load(imageUri).into((ImageView)findViewById(R.id.image_photo));
        Log.i("photo","post it");
    }
    //recycle
    protected void setExifData(List<ExifTagsContainer> list){
        for(final ExifTagsContainer container:list){
            switch (container.getType()){
                case GPS:
                    ((ImageView)findViewById(R.id.image_photo_gps)).setImageResource(R.drawable.ic_pin_drop_black_24dp);
                    ((TextView)findViewById(R.id.text_property_gps)).setText(container.getOnStringProperties());
                    findViewById(R.id.gps).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                                showAlertDialogWhenItemIsPressed(container);
                        }
                    });
                    break;
                case CAMERA_PROPERTIES:
                    ((ImageView)findViewById(R.id.image_photo_camera)).setImageResource(R.drawable.ic_photo_camera_black_24dp);
                    ((TextView)findViewById(R.id.text_property_camera)).setText(container.getOnStringProperties());
                    findViewById(R.id.camera).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            showAlertDialogWhenItemIsPressed(container);
                        }
                    });
                    break;
                case DATE:
                    ((ImageView)findViewById(R.id.image_photo_date)).setImageResource(R.drawable.ic_date_range_black_24dp);
                    ((TextView)findViewById(R.id.text_property_date)).setText(container.getOnStringProperties());
                    findViewById(R.id.date).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            showAlertDialogWhenItemIsPressed(container);
                        }
                    });
                    break;
                case DIMENSION:
                    ((ImageView)findViewById(R.id.image_photo_dimension)).setImageResource(R.drawable.ic_photo_size_select_actual_black_24dp);
                    ((TextView)findViewById(R.id.text_property_dimension)).setText(container.getOnStringProperties());
                    findViewById(R.id.dimension).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            showAlertDialogWhenItemIsPressed(container);
                        }
                    });
                    break;
                case OTHER:
                    ((ImageView)findViewById(R.id.image_photo_other)).setImageResource(R.drawable.ic_blur_on_black_24dp);
                    ((TextView)findViewById(R.id.text_property_other)).setText(container.getOnStringProperties());
                    findViewById(R.id.other).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            showAlertDialogWhenItemIsPressed(container);
                        }
                    });
                    break;
            }


        }
//        ((RecyclerView)this.findViewById(R.id.recycler_view)).setHasFixedSize(true);
//        recyclerView = ((RecyclerView)this.findViewById(R.id.recycler_view));// recycle = null ?
//        recyclerView.setHasFixedSize(true);
//        LinearLayoutManager manager = new LinearLayoutManager(this);
//        recyclerView.setLayoutManager(manager);
//        mAdapter = new adapter(list,pre,this);
//        recyclerView.setAdapter(mAdapter);

        //重写

    }
    protected void getAddressByTriggerRequest(PhotoDetailPresenter presenter){
            if (pre.getLatitude() != null && pre.getLongitude()!= null) {
                //加一个等待
//                mapInteractor.getAddress(latitude!!, longitude!!,
//                        onResponse = {
//                                view.showAddressOnRecyclerViewItem(it)
//                               //view.hideProgressDialog()
//                        },
//                        onFailure = {
//                                view.onError(view.getContext().resources.getString(R.string.getting_address_error))
//                               // view.hideProgressDialog()
//                        });

            }
    }
    //click
    protected void showAlertDialogWhenItemIsPressed(final ExifTagsContainer item){
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        ArrayList<String> optionList = new ArrayList<>();
        TextView text = new TextView(this);


        optionList.add(this.getResources().getString(R.string.alert_item_copy_to_clipboard));
        if (item.getType() == Type.GPS) {
            Log.i("type","GPS");
            optionList.add(this.getResources().getString(R.string.alert_item_open_map));
            optionList.add(this.getResources().getString(R.string.alert_item_remove_gps_tags));
        } else if (item.getType() == Type.DATE) {
            Log.i("type","DATE");
            optionList.add(this.getResources().getString(R.string.alert_item_edit));
            optionList.add(this.getResources().getString(R.string.alert_item_remove_date));
        } else if (item.getType() == Type.CAMERA_PROPERTIES){
            Log.i("type","CAMERA");
            optionList.add(this.getResources().getString(R.string.alert_item_edit_camera));
            optionList.add(this.getResources().getString(R.string.alert_item_remove_camera));
        }
        //add a title
        alertDialogBuilder.setTitle(this.getResources().getString(R.string.alert_select_an_action));
        //
        alertDialogBuilder.setItems(Arrays.copyOf(optionList.toArray(),optionList.size(),String[].class), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(which ==0){
                    copyDataToClipboard(item);
                } else if(which ==1){
                    if(item.getType() == Type.GPS) openDialogMap(item); //打开地图
                    else if(item.getType() == Type.DATE) editDate(item); //修改日期
                } else if(which ==2){ //添加一个remove 日期选择
                    removeTags(item); //change with positive and negative action !!
                }
            }
        });
        AlertDialog dialog = alertDialogBuilder.create();
        dialog.show();
    }
    protected void copyDataToClipboard(ExifTagsContainer item){
        ClipboardManager clipboard = (ClipboardManager)this.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText((String)item.getType().name(), (String)item.getOnStringProperties());
        clipboard.setPrimaryClip(clip);
        ViewExtensionKt.showSnackbar((CoordinatorLayout)this.findViewById(R.id.coordinator_layout), R.string.text_copied_to_clipboard_message);
    }
    protected void openDialogMap(ExifTagsContainer item){

    }
    protected void editDate(ExifTagsContainer item){
        DatePickerDialog datePickerDialog = new DatePickerDialog(Detail.this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        mYear = year;
                        mMonth = month;
                        mDay = dayOfMonth;
                        String dateString = mYear + ":" + (mMonth + 1) + ":" + mDay;
                        String time = Objects.requireNonNull(pre.exifTagsList.get(1).getList().get(1).component2()).split(" ")[1];
                        if (time.equals("data")){
                            time = "";
                        }
                        String dateTime = dateString + " " + time;
                        pre.setEXIFDate(dateTime);
                        reloadUI();
                    }
                },
                mYear, mMonth, mDay);
        datePickerDialog.show();

    }
    protected void removeTags(ExifTagsContainer item){
        switch (item.getType()){
            case DATE:
                pre.setEXIFDate("No data found");
                break;
            case CAMERA_PROPERTIES:
                pre.setEXIFCamera("No data found", "No data found");
                break;
        }

        reloadUI();
    }

    private void reloadUI(){
        pre.initialize(getIntent());
        setImage(pre.file.getName(),pre.imageUri);
        setExifData(pre.exifTagsList);
    }
}
