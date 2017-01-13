package com.rmtd.herry.headbackground;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    ImageView mImageView;
    Button mButton;
    PopupWindow mPopupWindow;
    RelativeLayout layout;
    RelativeLayout selectPhoto, takePhoto;

    int CAMERA_REQUEST_CODE = 1;//拍照
    int GALLERY_REQUEST_CODE = 2;//相册选取
    int CROP_REQUEST_CODE = 3;//裁剪
    int width;  //  屏幕宽度
    int hight;  //屏幕高度
    boolean flag;

    //拍照图片名称
    private static final String PHOTO_FILE_NAME = "temp_photo.jpg";
    private File tempFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeChangeUtil.changeTheme(this);

        setContentView(R.layout.activity_main);
        //获取屏幕宽，高
        Display display = this.getWindowManager().getDefaultDisplay();
        width = display.getWidth();
        hight = display.getHeight();

        initview();
        createPopupwindow();


    }

    /**
     * 生成popupwindow
     */
    private void createPopupwindow() {

        View popView = getLayoutInflater().inflate(R.layout.popupwind, null);
        mPopupWindow = new PopupWindow(popView, (int) ((width) * 0.8), (int) ((hight) * 0.2));
        selectPhoto = (RelativeLayout) popView.findViewById(R.id.select_photo);
        takePhoto = (RelativeLayout) popView.findViewById(R.id.take_photo);
        mPopupWindow.setBackgroundDrawable(new ColorDrawable(Color.WHITE));
        mPopupWindow.setOutsideTouchable(true);
        mPopupWindow.setFocusable(true);
        selectPhoto.setOnClickListener(this);
        takePhoto.setOnClickListener(this);
        //PopupWindow关闭监听
        mPopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                //背景色恢复正常
                WindowManager.LayoutParams lp = getWindow().getAttributes();
                lp.alpha = 1f;
                getWindow().setAttributes(lp);
            }
        });
    }

    /**
     * 初始化ui
     */
    private void initview() {
        layout = (RelativeLayout) findViewById(R.id.activity_main);
        mImageView = (ImageView) findViewById(R.id.image_head_background);
        mButton = (Button) findViewById(R.id.btn_changetheme);
        mButton.setOnClickListener(this);
        mImageView.setOnClickListener(this);
        getBitmapFromSharedPreferences();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_REQUEST_CODE) {
            // 从相机返回的数据
            if (hasSdcard()) {
                crop(Uri.fromFile(tempFile));
            } else {
                Toast.makeText(MainActivity.this, "未找到存储卡，无法存储照片！", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == GALLERY_REQUEST_CODE) {
            // 从相册返回的数据
            if (data != null) {
                // 得到图片的全路径
                Uri uri = data.getData();
                Log.d("tag", "~~~~");
                crop(uri);
            }
        } else if (requestCode == CROP_REQUEST_CODE) {
            // 从剪切图片返回的数据
            if (data != null) {
                Bitmap bitmap = data.getParcelableExtra("data");
                // 获得图片
                mImageView.setImageBitmap(bitmap);
                //保存到SharedPreferences
                saveBitmapToSharedPreferences(bitmap);
            }
            try {
                // 将临时文件删除
                tempFile.delete();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onClick(View v) {
        //关闭PopupWindow
        if (mPopupWindow.isShowing()) {
            mPopupWindow.dismiss();
        }
        switch (v.getId()) {
            case R.id.btn_changetheme:
                //更换主题
                if (ThemeChangeUtil.isChange) {
                    ThemeChangeUtil.isChange = false;
                } else {
                    ThemeChangeUtil.isChange = true;
                }
                this.recreate();//重新创建当前Activity实例

                break;
            case R.id.image_head_background:
                //换背景 颜色变暗
                mPopupWindow.showAtLocation(layout, Gravity.CENTER, 0, 0);
                WindowManager.LayoutParams lp = getWindow().getAttributes();
                lp.alpha = 0.7f;
                getWindow().setAttributes(lp);
                break;
            case R.id.select_photo:
                //从相册选择
                Intent selectIntent = new Intent(Intent.ACTION_GET_CONTENT);
                selectIntent.setType("image/*");
                startActivityForResult(selectIntent, GALLERY_REQUEST_CODE);
                break;
            case R.id.take_photo:
                //拍照
                Intent takeIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                // 判断存储卡是否可以用，可用进行存储
                if (hasSdcard()) {
                    tempFile = new File(Environment.getExternalStorageDirectory(), PHOTO_FILE_NAME);
                    // 从文件中创建uri
                    Uri uri = Uri.fromFile(tempFile);
                    takeIntent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
                }
                // 开启一个带有返回值的Activity，请求码为PHOTO_REQUEST_CAREM
                startActivityForResult(takeIntent, CAMERA_REQUEST_CODE);
                break;
        }
    }


    /**
     * 判断sdcard是否被挂载
     */
    private boolean hasSdcard() {
        //判断sd卡是否安装好　　　media_mounted
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 剪切图片
     */
    private void crop(Uri uri) {
        // 裁剪图片意图
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setDataAndType(uri, "image/*");
        intent.putExtra("crop", "true");
        // 裁剪框的比例，1：1
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        // 裁剪后输出图片的尺寸大小
        intent.putExtra("outputX", 250);
        intent.putExtra("outputY", 250);

        intent.putExtra("outputFormat", "JPEG");// 图片格式
        intent.putExtra("noFaceDetection", true);// 取消人脸识别
        intent.putExtra("return-data", true);
        // 开启一个带有返回值的Activity，请求码为PHOTO_REQUEST_CUT
        startActivityForResult(intent, CROP_REQUEST_CODE);
    }

    /**
     * 将图片保存到SharedPreferences
     *
     * @param bitmap
     */
    private void saveBitmapToSharedPreferences(Bitmap bitmap) {
        // Bitmap bitmap=BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
        //第一步:将Bitmap压缩至字节数组输出流ByteArrayOutputStream
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 80, byteArrayOutputStream);
        //第二步:利用Base64将字节数组输出流中的数据转换成字符串String
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        String imageString = new String(Base64.encodeToString(byteArray, Base64.DEFAULT));
        //第三步:将String保持至SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("testSP", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("image", imageString);
        editor.commit();

        //上传背景
        setImgByStr(imageString, "");
    }

    /**
     * 上传背景
     *
     * @param imgStr
     * @param imgName
     */
    public void setImgByStr(String imgStr, String imgName) {
        //这里是头像接口，通过Post请求，拼接接口地址和ID，上传数据。
        String url = "http://这里写的是接口地址（具体接收格式要看后台怎么给）";
        Map<String, String> params = new HashMap<String, String>();
        params.put("id", "11111111");// 11111111
        params.put("data", imgStr);
//        OkHttp.postAsync(url, params, new OkHttp.DataCallBack() {
//            @Override
//            public void requestFailure(Request request, IOException e) {
//                Log.i("上传失败", "失败" + request.toString() + e.toString());
//            }
//            @Override
//            public void requestSuccess(String result) throws Exception {
//                Toast.makeText(MainActivity.this,"上传成功",Toast.LENGTH_SHORT).show();
//                Log.i("上传成功", result);
//            }
//        });
    }


    /**
     * 从SharedPreferences获取图片
     */
    private void getBitmapFromSharedPreferences() {
        SharedPreferences sharedPreferences = getSharedPreferences("testSP", Context.MODE_PRIVATE);
        //第一步:取出字符串形式的Bitmap
        String imageString = sharedPreferences.getString("image", "");
        //第二步:利用Base64将字符串转换为ByteArrayInputStream
        byte[] byteArray = Base64.decode(imageString, Base64.DEFAULT);
        if (byteArray.length == 0) {
            mImageView.setImageResource(R.mipmap.ic_launcher);
        } else {
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArray);

            //第三步:利用ByteArrayInputStream生成Bitmap
            Bitmap bitmap = BitmapFactory.decodeStream(byteArrayInputStream);
            mImageView.setImageBitmap(bitmap);
        }

    }
}
