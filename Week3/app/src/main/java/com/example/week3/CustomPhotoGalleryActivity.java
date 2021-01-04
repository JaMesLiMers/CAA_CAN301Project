package com.example.week3;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
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

import com.example.week3.exifTool.PhotoDetailPresenter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CustomPhotoGalleryActivity extends Activity {
    private static final String TAG = "CustomPhotoGalleryActivity";
    private GridView grdImages;
    private Button btnSelect;
    private Button btnSearch;
    private EditText strSearch;

    private ImageAdapter imageAdapter;
    private String[] all_arrPath;
    private boolean[] all_thumbnailsselection;
    private int all_ids[];
    private int all_count;
    private double all_lat_long[][];
    private String all_city[];


    /**
     * Overrides methods
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_photo_gallery);
        // 所有图片
        grdImages= (GridView) findViewById(R.id.grdImages);
        // 确定按钮
        btnSelect= (Button) findViewById(R.id.btnSelect);
        // 搜索按钮
        btnSearch= (Button) findViewById(R.id.btnSearch);
        // 搜索文字
        strSearch =(EditText)findViewById(R.id.strSearch);

        final String[] columns = { MediaStore.Images.Media.DATA, MediaStore.Images.Media._ID }; //{"_data, _id"}
        final String orderBy = MediaStore.Images.Media._ID; // "_id"

        // 获取所有的图片
        @SuppressWarnings("deprecation")
        Cursor imagecursor = managedQuery(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, columns, null, null, orderBy);
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
        // 获取所有的lat lon信息
        getAllLatLon();

        // 获取所有的City信息
        getAllCity();

        // 设定图片预览 （下面的class负责）
        // imageAdapter = new ImageAdapter(this.all_count, this.all_ids);
        // grdImages.setAdapter((ListAdapter) imageAdapter);

        // 查询完毕关闭cursor
        imagecursor.close();

        // 搜索回调函数
        btnSearch.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                // 清空结果
                grdImages.setAdapter(null);

                // 获取文字
                String strSearch_="";
                strSearch_=strSearch.getText().toString().toLowerCase();

                int count = 0;
                ArrayList ids_ = new ArrayList<Integer>();

                // 按照搜索过滤所有的图片
                for (int i = 0; i < all_count; i++) {
                    if (all_city[i].toLowerCase().equals(strSearch_)){
                        count += 1;
                        ids_.add(all_ids[i]);
                    }
                }
                // int[]
                int[] ids = ids_.stream().mapToInt(i -> (int) i).toArray();

                // 设定图片预览 （下面的class负责）
                imageAdapter = new ImageAdapter(count, ids);
                grdImages.setAdapter((ListAdapter) imageAdapter);
            }
        });

        // 确认选择回调函数
        btnSelect.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                final int len = all_thumbnailsselection.length;
                int cnt = 0;
                String selectImages = "";
                for (int i = 0; i < len; i++) {
                    if (all_thumbnailsselection[i]) {
                        cnt++;
                        selectImages = selectImages + all_arrPath[i] + "|";
                    }
                }
                if (cnt == 0) {
                    Toast.makeText(getApplicationContext(), "Please select at least one image", Toast.LENGTH_LONG).show();
                } else {

                    Log.d("SelectedImages", selectImages);
                    Intent i = new Intent();
                    i.putExtra("data", selectImages);
                    setResult(Activity.RESULT_OK, i);
                    finish();
                }
            }
        });

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
                        all_thumbnailsselection[id] = false;
                    } else {
                        cb.setChecked(true);
                        all_thumbnailsselection[id] = true;
                    }
                }
            });
            holder.imgThumb.setOnClickListener(new View.OnClickListener() {

                public void onClick(View v) {
                    int id = holder.chkImage.getId();
                    if (all_thumbnailsselection[id]) {
                        holder.chkImage.setChecked(false);
                        all_thumbnailsselection[id] = false;
                    } else {
                        holder.chkImage.setChecked(true);
                        all_thumbnailsselection[id] = true;
                    }
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