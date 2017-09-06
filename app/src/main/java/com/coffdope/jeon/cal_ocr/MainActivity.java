package com.coffdope.jeon.cal_ocr;

import android.content.Context;
import android.content.pm.PackageManager;
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
import java.util.List;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.MatOfPoint;

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
    private SurfaceHolder mSurfaceHolder;
    private Camera mCamera;
    private Button button;
    private DetectorTask mDetectorTask;
    private ImageView mImageView;

    private ArrayList<MatOfPoint> mContour = new ArrayList<MatOfPoint>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        /*허가 받는 부분*/
        if(ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,new String[]{android.Manifest.permission.CAMERA}, PERMISSIONS_REQUEST_CODE);
        }

        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN); //풀스크린
        setContentView(R.layout.activity_main);

        button = (Button)findViewById(R.id.button1);

        mSurfaceView = (SurfaceView) findViewById(R.id.surfaceView1);
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(this);

        mImageView = (ImageView) findViewById(R.id.imageview1);

        //todo 촬영 기능 만들 것
        /*버튼 클릭 시 촬영 -> 편집 화면 Intent 콜*/
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                mImageView.setImageBitmap(b);
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
    //todo 영역 인식 후 영역 밚환 -> 반환 된 영역 그리기
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
            mDetector = new Detector(context, mCamera.getParameters().getPreviewSize());
            ArrayList<MatOfPoint> tmp = mDetector.detectPage(bytes[0]);
            if(!tmp.isEmpty()){mContour = tmp;}
            return mContour;
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(ArrayList<MatOfPoint> contour) {
            super.onPostExecute(contour);
            //todo contour 프리뷰 위에 덧씌우기
        }
    }

}
