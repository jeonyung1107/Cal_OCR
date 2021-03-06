package com.coffdope.jeon.cal_ocr;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
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
import android.widget.Toast;
import android.widget.Button;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
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
    private Button button;
    private DetectorTask mDetectorTask;
    private OCR_Preview mOCR_preview;
    private Camera.Size mCameraSize;
    private OCR mOCR;
    private Mat matForTranmsform;

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

        mSurfaceView = (SurfaceView) findViewById(R.id.surfaceView1);
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(this);

        mOCR_preview = (OCR_Preview) findViewById(R.id.OCR_preview);
        mOCR_preview.setZOrderOnTop(true);
        mOCR_holder = mOCR_preview.getHolder();
        mOCR_holder.setFormat(PixelFormat.TRANSPARENT);

        mOCR = new OCR(this);

        /*autofocus on touching the screen*/
        mOCR_preview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCamera.autoFocus(new Camera.AutoFocusCallback() {
                    @Override
                    public void onAutoFocus(boolean b, Camera camera) {
                       if(b){
                           Toast.makeText(getApplicationContext(), "autoFocus",Toast.LENGTH_SHORT);
                       }
                    }
                });
            }
        });

        /*버튼 클릭 시 촬영 -> 편집 화면 Intent 콜*/
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Detector mDetector = new Detector();
                matForTranmsform=mDetector.four_point_transform(mContour.get(0), matForTranmsform);
                ArrayList<Point> pts = mDetector.findIntersections(matForTranmsform);
                matForTranmsform = mDetector.rotate(matForTranmsform);
                ArrayList<Mat> cropped = mDetector.cropImage(matForTranmsform, pts);

                // TODO: 17. 11. 6 need to make async
                String st = "";
                for(Mat s: cropped){
                    Bitmap bm = Bitmap.createBitmap(s.cols(), s.rows(), Bitmap.Config.ARGB_8888);
                    Utils.matToBitmap(s,bm);
                    st += mOCR.processImage(bm);
                }

                cal = new Calendar_activity();
                startActivity(cal.insert_event(2017,05,01,12,00,st));
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
        Toast.makeText(this, "카메라 해제", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPreviewFrame(byte[] bytes, Camera camera) {
        if(mDetectorTask == null|| mDetectorTask.getStatus() == AsyncTask.Status.FINISHED){
            mDetectorTask = new DetectorTask(this);
            mDetectorTask.execute(bytes);

            matForTranmsform = new Mat(mCameraSize.height, mCameraSize.width,CvType.CV_8UC1 );
            matForTranmsform.put(0, 0, bytes);
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
                Mat mat = new Mat(mCameraSize.height, mCameraSize.width,CvType.CV_8UC4);
                Mat mat_rot = new Mat(mCameraSize.width,mCameraSize.height, CvType.CV_8UC4);
                Mat mat_resize = new Mat(mCanvas.getHeight(), mCanvas.getWidth(), CvType.CV_8UC4);
                Bitmap cntBitmap = Bitmap.createBitmap(mCanvas.getWidth(), mCanvas.getHeight(), Bitmap.Config.ARGB_8888);

                try{
                    synchronized (mOCR_holder){
                        mCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                        Imgproc.drawContours(mat, mContour, -1, new Scalar(255, 0, 0), 5);
                        Core.rotate(mat,mat_rot,Core.ROTATE_90_CLOCKWISE);
                        Imgproc.resize(mat_rot, mat_resize, new Size(mat_resize.width(), mat_resize.height()));
                        Utils.matToBitmap(mat_resize,cntBitmap);
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
