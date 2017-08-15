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
    Mat d,gray,thres, edged;
    MatOfPoint MP;
    public Detector(Context context){
        this.context = context;
    }
    /*영역 인식 메서드, */
    public Bitmap cvTest(){
        try {
            d = Utils.loadResource(context, R.drawable.a, Imgcodecs.CV_LOAD_IMAGE_COLOR);
        }catch (IOException i){
            Log.e(TAG,i.toString());
        }
        gray = new Mat(d.rows(),d.cols(),Imgcodecs.CV_LOAD_IMAGE_GRAYSCALE);
        thres = new Mat(gray.rows(),gray.cols(),Imgcodecs.CV_LOAD_IMAGE_GRAYSCALE);
        edged = new Mat(thres.rows(), thres.cols(), Imgcodecs.CV_LOAD_IMAGE_GRAYSCALE);

        Imgproc.cvtColor(d,gray, Imgproc.COLOR_RGB2GRAY,1);//grayscaling
        Imgproc.adaptiveThreshold(gray,thres,255,Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                Imgproc.THRESH_BINARY,5,10); //binary
        Imgproc.Canny(thres,edged,75,200,3,false);

        Mat cvResult = thres.clone();

        /*결과물 반환*/
        Bitmap b = Bitmap.createBitmap(cvResult.width(),cvResult.height(), Bitmap.Config.RGB_565);
        Utils.matToBitmap(cvResult,b);
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
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, context, mLoaderCallback);
    }


}
