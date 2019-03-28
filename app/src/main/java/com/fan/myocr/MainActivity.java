package com.fan.myocr;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static android.content.ContentValues.TAG;

public class MainActivity extends AppCompatActivity {

    String  sdCardRoot = Environment.getExternalStorageDirectory().getAbsolutePath();
    Button btn_openCamera;
    Button btn_findText;
    Button btn_getTxt;
    OcrView ocrView;
    TextView txt;
    TextView txtget;
    double scale =1.0;
    Bitmap bmp=null;
    TessBaseAPI mTess;

    String sdPath;
    String picPath;
    String  TXT_get = null;
    ThreadPoolExecutor threadPoolExecutor;
    String filename;
    int ScreenHeight ;
    int ScreenWidth ;
    public  static  int REQUST_ORIGINAL = 101;

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    final Context context1 = this;
    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
//            if(msg.what==0)
            txtget.setText(GetTextFromRect());
//            if (msg.what==1)
//                getImageFromCamera(context1);

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btn_openCamera =   (Button)findViewById(R.id.btn_openCamera);
        btn_findText = (Button)findViewById(R.id.btn_findtext) ;
        btn_getTxt = (Button)findViewById(R.id.btn_gettext);
        txt  = (TextView)findViewById(R.id.textView);
        ocrView = new OcrView(this);
        txtget = (TextView)findViewById(R.id.txt_get);
        threadPoolExecutor = new ThreadPoolExecutor(3,6,2, TimeUnit.SECONDS,new LinkedBlockingDeque<Runnable>(128));
        sdPath = Environment.getExternalStorageDirectory().getPath()+ File.separator+"Android/data/"+getPackageName()+"/files";
        picPath = sdPath +"/" + "temp.png";
        filename =sdPath+"/test/tessdata/chi_sim.traineddata";

        btn_openCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getImageFromCamera();
            }
        });
        btn_findText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ocrView.SelectRect();
            }
        });
        btn_getTxt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                txtget.setText("Recognising....Please wait");
                Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        TXT_get = GetTextFromRect();
                        handler.sendEmptyMessage(0);
                    }
                };
                threadPoolExecutor.execute(r);

            }
        });

        SharedPreferences sp = getSharedPreferences("ocr_test", Context.MODE_PRIVATE);
        int int_runtimes = sp.getInt("run",0);
        if(int_runtimes == 0)
        {
            try
            {
                copyBigDataToSD(filename);
                sp.edit().putInt("run",1).commit();
                txt.setText("Finished loading");
            } catch (IOException e)
            {
                e.printStackTrace();
                txt.setText("Wrong");
            }
        }
        RelativeLayout.LayoutParams p = new  RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        addContentView(ocrView,p);
        Runnable runnable  = new Runnable() {
            @Override
            public void run() {
                initTessBaseData();
            }
        };
        threadPoolExecutor.execute(runnable);
        WindowManager wm = this.getWindowManager();
        ScreenHeight = wm.getDefaultDisplay().getHeight();
        ScreenWidth = wm.getDefaultDisplay().getWidth();
    }
        private  void initTessBaseData() {
            mTess = new TessBaseAPI();
            String  datapath = sdPath + "/test/";
            String language = "chi_sim";
            File dir = new File(datapath + "tessdata/");
            if(!dir.exists())
                dir.mkdirs();
            mTess.init(datapath,language);
        }

        private void copyBigDataToSD(String strFileName) throws IOException
        {
            InputStream myInput;
            getExternalFilesDir(null).getAbsolutePath();
            File dir = new File(sdPath+File.separator+"test/tessdata/");
            dir.mkdirs();
            File filea = new File (sdPath +File.separator+"test/tessdata/chi_sim.traineddata");

            filea.createNewFile();

            OutputStream myOutput = new FileOutputStream(strFileName);
            myInput =  this.getAssets().open("chi_sim.traineddata");
            byte[] buffer = new byte[1024];
            int length = myInput.read(buffer);
            while (length>0)
            {
                myOutput.write(buffer,0,length);
                length = myInput.read(buffer);
            }
            myOutput.flush();
            myInput.close();
            myOutput.close();

        }

        public boolean isFileExist(String fileName,String path)
        {
            File file  = new File(sdCardRoot +path + File.separator +fileName);
            boolean f = file.exists();
            return  file.exists();
        }

        @Override
        protected void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);

            if (resultCode == Activity.RESULT_OK && requestCode == REQUST_ORIGINAL){
                FileInputStream fis = null;
                try{
                    Log.e("sdpath2",picPath);
                    fis = new FileInputStream(picPath);
                    bmp=null;
                    bmp = FixImageOrientation(picPath);
                    ImageView im_camera = (ImageView)findViewById(R.id.img_camera);
                    im_camera.setImageBitmap(bmp);
                }catch (Exception e){
                    e.printStackTrace();
                }finally {
                    try {
                        fis.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            else {
                 Toast.makeText(this,"No photo is taken",Toast.LENGTH_SHORT).show();
            }
        }

        protected  void getImageFromCamera(){
            Intent getImageByCamera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            Uri uri;
            if(Build.VERSION.SDK_INT>=24)
            {
                File g= new File(picPath);
                try {
                    g.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                uri = FileProvider.getUriForFile(this,"Fan",g);
            }else{
                uri = Uri.fromFile(new File(picPath));
            }

            getImageByCamera.putExtra(MediaStore.EXTRA_OUTPUT,uri);
            startActivityForResult(getImageByCamera,REQUST_ORIGINAL);
        }
        public Bitmap FixImageOrientation(String imagePath) throws  IOException{

            if (imagePath==null||imagePath.equals(""))
                return null;
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 2;
            Bitmap bitmap = BitmapFactory.decodeFile(imagePath,options);
            int rotate = 0;

            ExifInterface exif = new ExifInterface(imagePath);
            int imageOrientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,ExifInterface.ORIENTATION_NORMAL);

            switch (imageOrientation)
            {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotate = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotate = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotate = 270;
                    break;
                default:
                    break;
            }
            int w = bitmap.getWidth();
            int h = bitmap.getHeight();
            Matrix mtx = new Matrix();
            mtx.preRotate(rotate);
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, w, h, mtx, false);
            bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);


            return  bitmap;

        }

        public String   GetTextFromRect(){
            mTess.clear();
            if(bmp==null) return null;
            mTess.setImage(bmp);
            scale =(double) bmp.getWidth()/(double) ScreenWidth;
            mTess.setRectangle((int)(scale*ocrView.rect.left),(int)(scale*ocrView.rect.top),(int)(scale*ocrView.rect.width()),(int)(scale*ocrView.rect.height()));

            String result = mTess.getUTF8Text();
            return result;
        }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
}
