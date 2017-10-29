package com.coffdope.jeon.cal_ocr;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.Button;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback, Camera.PreviewCallback {
    static {
        if(!OpenCVLoader.initDebug()) {
            Log.d("My App", "Unable to load OpenCV");
        } else {
            Log.d("My App", "OpenCV loaded");
        }
    }

    private final static int PERMISSIONS_REQUEST_CODE = 100;
    private final static String TAG = "Main";

    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder, mOCR_holder;
    private Camera mCamera;
    private Button button,button2;
    private DetectorTask mDetectorTask;
    private OCR_Preview mOCR_preview;
    private Camera.Size mCameraSize;
    private OCR mOCR;
    private int mOCR_height;
    private int mOCR_width;

    private ArrayList<MatOfPoint> mContour = new ArrayList<MatOfPoint>();
    private ArrayList<MatOfPoint> mContour2 = new ArrayList<MatOfPoint>();

    Calendar_activity cal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        /*허가 받는 부분*/
        if(ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.CAMERA}, PERMISSIONS_REQUEST_CODE);
        }

        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN); //풀스크린
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        button = (Button)findViewById(R.id.button1);
        button2 = (Button) findViewById(R.id.button2);

        mSurfaceView = (SurfaceView) findViewById(R.id.surfaceView1);
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(this);

        mOCR_preview = (OCR_Preview) findViewById(R.id.OCR_preview);
//        mOCR_preview.setRotation(90f);
        mOCR_preview.setZOrderOnTop(true);
        mOCR_holder = mOCR_preview.getHolder();
        mOCR_holder.setFormat(PixelFormat.TRANSPARENT);

//        mOCR = new OCR(this);

        //todo 촬영 기능 만들 것 촬영 시 시점 전환도 같이 하도록 하고 프리뷰 호출
        /*버튼 클릭 시 촬영 -> 편집 화면 Intent 콜*/
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            }
        });
        button2.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                cal = new Calendar_activity();
                // TODO: 17. 9. 10 이번트 등록 완전히 구현
                startActivity(cal.insert_event(2014,05,05,12,00,"test"));
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onDestroy() {
        if(mCamera!=null)
        mCamera.release();
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        if(mCamera!=null)
        mCamera.release();
        super.onPause();
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        mCamera = Camera.open();

        Camera.Parameters params = mCamera.getParameters();
        final List<Camera.Size> listPreviewSize = params.getSupportedPreviewSizes();
        for (Camera.Size size : listPreviewSize) {
            Log.i(TAG, String.format("Supported Preview Size (%d, %d)", size.width, size.height));
        }
        Camera.Size previewSize = listPreviewSize.get(0);
        params.setPreviewSize(previewSize.width, previewSize.height);

        mCamera.setDisplayOrientation(90);
        mCamera.setParameters(params);

        try {
            mCamera.setPreviewDisplay(mSurfaceHolder);
        }catch (IOException i){
            i.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        mCameraSize = mCamera.getParameters().getPreviewSize();
        mCamera.setPreviewCallback(this);
        mCamera.startPreview();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        mCamera.release();
        mCamera = null;
        Log.i(TAG,"카메라 해제");
        Toast.makeText(this, "카메라 해제", Toast.LENGTH_SHORT).show();
    }
    //todo 프리뷰에서 영역 인식 구현, surfaceview 두개 이용해서 만들 것
    @Override
    public void onPreviewFrame(byte[] bytes, Camera camera) {
        if(mDetectorTask == null|| mDetectorTask.getStatus() == AsyncTask.Status.FINISHED){
            mDetectorTask = new DetectorTask(this);
            mDetectorTask.execute(bytes);
        }else{
            Log.i(TAG,"no Back");
        }
    }

    /*mDetectorTask 백그라운드 실행시키는 Async class*/
    private class DetectorTask extends AsyncTask<byte[],Void,ArrayList<MatOfPoint>>{
        Context context;
        Detector mDetector;
        public DetectorTask(Context context){
            this.context = context;
        }
        @Override
        protected ArrayList<MatOfPoint> doInBackground(byte[]...bytes){
            mDetector = new Detector(context, mCameraSize);
            ArrayList<MatOfPoint> tmp = mDetector.detectPage(bytes[0]);
            if(!tmp.isEmpty()){
                mContour = tmp;
                mContour2 = (ArrayList<MatOfPoint>) mContour.clone();

                Canvas mCanvas = mOCR_holder.lockCanvas();
//                Mat mat = new Mat(mCanvas.getHeight(), mCanvas.getWidth(),CvType.CV_8UC1);// FIXME: 17. 10. 18 argb 타입 설정
                Mat mat = new Mat(mCameraSize.height, mCameraSize.width,CvType.CV_8UC1);// FIXME: 17. 10. 18 argb 타입 설정
                mat.put(0, 0, bytes[0]);
//                Bitmap cntBitmap = Bitmap.createBitmap(mCanvas.getWidth(), mCanvas.getHeight(), Bitmap.Config.ARGB_8888);
                Bitmap cntBitmap = Bitmap.createBitmap(mCameraSize.width, mCameraSize.height, Bitmap.Config.ARGB_8888);
                try{
                    synchronized (mOCR_holder){
//                        mCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                        // TODO: 17. 10. 18 contour rotate 시킬것, 높이 -> 너비 , 너비 -> 높이로 새로운 mat 만들어서
                        Imgproc.drawContours(mat, mContour, -1, new Scalar(255, 0, 0), 5);
                        Utils.matToBitmap(mat,cntBitmap);
                        mCanvas.drawBitmap(cntBitmap,0,0,null);
                    }
                }finally {
                    mOCR_holder.unlockCanvasAndPost(mCanvas);
                }
            }else{
                mContour = (ArrayList<MatOfPoint>) mContour2.clone();
            }

            return mContour;
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(ArrayList<MatOfPoint> contour) {
            super.onPostExecute(contour);
        }
    }
}
