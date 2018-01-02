package io.hxq.editor;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.MediaStore.Images.ImageColumns;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * 主Activity入口
 *
 * @author xmuSistone
 */
@SuppressLint("SimpleDateFormat") public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_PICK_IMAGE = 1023;
    private static final int REQUEST_CODE_CAPTURE_CAMEIA = 1022;
    private RichTextEditor editor;

    private static final File PHOTO_DIR =
        new File(Environment.getExternalStorageDirectory() + "/DCIM/Camera");
    private File mCurrentPhotoFile;// 照相机拍照得到的图片

    private static final String DATA =
        "[{\"type\":\"txt\",\"txt\":\"静静的卡夫卡\"},{\"type\":\"img\",\"id\":0,\"w\":3024,\"h\":4032,\"fmt\":\"image\\/jpeg\",\"path\":\"\\/storage\\/emulated\\/0\\/Pictures\\/JPEG_20171225_152706.jpg\"},{\"type\":\"txt\",\"txt\":\"上世\"},{\"type\":\"img\",\"id\":0,\"w\":3024,\"h\":4032,\"fmt\":\"image\\/jpeg\",\"path\":\"\\/storage\\/emulated\\/0\\/Pictures\\/JPEG_20171225_152706.jpg\"},{\"type\":\"txt\",\"txt\":\"纪大家\"}]\n";

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editor = findViewById(R.id.editor);

        try {
            editor.setTale(generateTale(new JSONArray(DATA)), true);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        final View album = findViewById(R.id.album);
        final View camera = findViewById(R.id.camera);
        final View send = findViewById(R.id.send);

        OnClickListener event = new OnClickListener() {

            @Override public void onClick(View v) {
                editor.hideKeyBoard();
                if (v.getId() == album.getId()) {
                    // 打开系统相册
                    Intent intent = new Intent(Intent.ACTION_PICK);
                    intent.setType("image/*");// 相片类型
                    startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE);
                } else if (v.getId() == camera.getId()) {
                    // 打开相机
                    openCamera();
                } else if (v.getId() == send.getId()) {
                    dealData(editor.getTale());
                }
            }
        };

        album.setOnClickListener(event);
        camera.setOnClickListener(event);
        send.setOnClickListener(event);
    }

    private List<RichTextEditor.Tale> generateTale(JSONArray jsonArray) {
        List<RichTextEditor.Tale> tales = new ArrayList<>();
        try {
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject object = jsonArray.getJSONObject(i);
                RichTextEditor.Tale tale = new RichTextEditor.Tale();
                String type = object.getString("type");
                tale.type = type;
                if (RichTextEditor.Tale.Type.Text.equals(tale.type)) {
                    tale.txt = object.optString("txt", null);
                } else {
                    tale.id = object.optLong("id", 0L);
                    tale.w = object.optInt("w", 0);
                    tale.h = object.optInt("h", 0);
                    tale.fmt = object.optString("fmt", null);
                    tale.path = object.optString("path", null);
                }
                tales.add(tale);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return tales;
    }

    /**
     * 负责处理编辑数据提交等事宜，请自行实现
     */
    protected void dealData(ArrayList<RichTextEditor.Tale> editList) {
        //list to json array
        JSONArray jsonArray = new JSONArray();
        try {
            for (RichTextEditor.Tale tale : editList) {
                JSONObject json = new JSONObject();
                json.put("type", tale.type);
                if (RichTextEditor.Tale.Type.Text.equals(tale.type)) {
                    json.put("txt", tale.txt);
                } else {
                    json.put("id", tale.id);
                    json.put("w", tale.w);
                    json.put("h", tale.h);
                    json.put("fmt", tale.fmt);
                    if (!TextUtils.isEmpty(tale.path)) {
                        json.put("path", tale.path);
                    }
                }
                jsonArray.put(json);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.e("dealData", jsonArray.toString());
        Intent intent = new Intent(this, BoardActivity.class);
        intent.putParcelableArrayListExtra("Data", editList);
        startActivity(intent);
    }

    protected void openCamera() {
        try {
            // Launch camera to take photo for selected contact
            PHOTO_DIR.mkdirs();// 创建照片的存储目录
            mCurrentPhotoFile = new File(PHOTO_DIR, getPhotoFileName());// 给新照的照片文件命名
            final Intent intent = getTakePickIntent(mCurrentPhotoFile);
            startActivityForResult(intent, REQUEST_CODE_CAPTURE_CAMEIA);
        } catch (ActivityNotFoundException e) {
        }
    }

    public static Intent getTakePickIntent(File f) {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE, null);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(f));
        return intent;
    }

    /**
     * 用当前时间给取得的图片命名
     */
    private String getPhotoFileName() {
        Date date = new Date(System.currentTimeMillis());
        SimpleDateFormat dateFormat = new SimpleDateFormat("'IMG'_yyyy-MM-dd HH:mm:ss");
        return dateFormat.format(date) + ".jpg";
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) {
            return;
        }

        if (requestCode == REQUEST_CODE_PICK_IMAGE) {
            Uri uri = data.getData();
            insertBitmap(getRealFilePath(uri));
        } else if (requestCode == REQUEST_CODE_CAPTURE_CAMEIA) {
            insertBitmap(mCurrentPhotoFile.getAbsolutePath());
        }
    }

    /**
     * 添加图片到富文本剪辑器
     */
    private void insertBitmap(String imagePath) {
        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imagePath, bmOptions);
        int w = bmOptions.outWidth;
        int h = bmOptions.outHeight;
        String fmt = bmOptions.outMimeType;
        RichTextEditor.Tale tale = new RichTextEditor.Tale();
        tale.type = RichTextEditor.Tale.Type.Image;
        tale.fmt = fmt;
        tale.w = w;
        tale.h = h;
        tale.path = imagePath;
        editor.insertTale(tale);
    }

    /**
     * 根据Uri获取图片文件的绝对路径
     */
    public String getRealFilePath(final Uri uri) {
        if (null == uri) {
            return null;
        }

        final String scheme = uri.getScheme();
        String data = null;
        if (scheme == null) {
            data = uri.getPath();
        } else if (ContentResolver.SCHEME_FILE.equals(scheme)) {
            data = uri.getPath();
        } else if (ContentResolver.SCHEME_CONTENT.equals(scheme)) {
            Cursor cursor =
                getContentResolver().query(uri, new String[] { ImageColumns.DATA }, null, null,
                    null);
            if (null != cursor) {
                if (cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(ImageColumns.DATA);
                    if (index > -1) {
                        data = cursor.getString(index);
                    }
                }
                cursor.close();
            }
        }
        return data;
    }
}
