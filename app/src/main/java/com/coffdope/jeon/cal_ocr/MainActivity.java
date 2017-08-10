package com.coffdope.jeon.cal_ocr;

import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.Button;
import android.graphics.Bitmap;

import java.io.IOException;
import java.util.List;
import java.util.jar.Manifest;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

//todo 뷰 사이즈 조절
public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback, Camera.PreviewCallback {

    private final static int PERMISSIONS_REQUEST_CODE = 100;
    private final static String TAG = "Main";

    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private Camera mCamera;
    private Button button;
    private Detector detector;
    private ImageView mImageView;
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
//                mCamera.takePicture(null,null,null);
                mImageView.setImageBitmap(detector.cvTest());
            }
        });

        detector = new Detector(this);
    }

    @Override
    protected void onResume() {
        detector.onResume(this);
        super.onResume();
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        mCamera = Camera.open();

        Camera.Parameters params = mCamera.getParameters();
        final List<Camera.Size> listPreviewSize = params.getSupportedPreviewSizes();
        for (Camera.Size size : listPreviewSize) {
            Log.i(TAG, String.format("Supported Preview Size (%d, %d)", size.width, size.height));
        }
        Camera.Size previewSize = listPreviewSize.get(1);
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
        Camera.Parameters p = mCamera.getParameters();
//        p.setPreviewSize(i1,i2);
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

    }
}
