package com.coffdope.jeon.cal_ocr;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.imgproc.Imgproc;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.android.Utils;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.CvType;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.content.Context;

import java.util.List;

import java.io.IOException;

/**
 * Created by jeon on 17. 8. 7.
 */
/*영역 인식과 관련된 기능을 담당하는 클래스*/
public class Detector {
    private final static String TAG = "Detector";
    Context context;
    Mat d,gray;
    MatOfPoint MP;
    public Detector(Context context){
        this.context = context;
    }
    /*영역 인식 메서드, */
    public Bitmap cvTest(){
        try {
            d = Utils.loadResource(context, R.drawable.b, Imgcodecs.CV_LOAD_IMAGE_COLOR);
        }catch (IOException i){
            Log.e(TAG,i.toString());
        }
        gray = new Mat(d.rows(),d.cols(),Imgcodecs.CV_LOAD_IMAGE_GRAYSCALE);
        Imgproc.cvtColor(d,gray, Imgproc.COLOR_RGB2GRAY,1);
        Mat cvCorner = d.clone();
        MP = new MatOfPoint();
        Imgproc.goodFeaturesToTrack(gray,MP,100,0.001,100);
        List<Point> points = MP.toList();
        for(int i=0; i<points.size(); i++){
            Imgproc.circle(cvCorner,points.get(i),50,  new Scalar(0,255,0));
            Log.i(TAG,i+"");
        }
        Bitmap b = Bitmap.createBitmap(cvCorner.width(),cvCorner.height(), Bitmap.Config.RGB_565);
        Utils.matToBitmap(cvCorner,b);
        return b;
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this.context) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i("OpenCV", "OpenCV loaded successfully");
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };


    public void onResume(Context context)
    {
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, context, mLoaderCallback);
    }


}
