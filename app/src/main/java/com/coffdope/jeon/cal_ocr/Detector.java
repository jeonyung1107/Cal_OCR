package com.coffdope.jeon.cal_ocr;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.android.Utils;
import org.opencv.core.Point;
import org.opencv.core.Scalar;

import android.graphics.Bitmap;
import android.hardware.Camera;
import android.util.Log;
import android.content.Context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import java.io.IOException;

/**
 * Created by jeon on 17. 8. 7.
 */
/*영역 인식과 관련된 기능을 담당하는 클래스*/
public class Detector {
    private final static String TAG = "Detector";
    private static int idx =0;
    Context context;
    Camera.Size size;
    Mat input_image,output_image;

    ArrayList<MatOfPoint> cnt = new ArrayList<MatOfPoint>();
    public Detector() {
        super();
    }

    public Detector(Context context, Camera.Size size){
        this.context = context;
        this.size = size;
    }
    /*영역 인식 메서드, */
    public Bitmap detectPage(byte[] bytes){
        input_image = new Mat(size.height,size.width,Imgcodecs.CV_LOAD_IMAGE_GRAYSCALE);
        output_image = new Mat(size.height,size.width,Imgcodecs.CV_LOAD_IMAGE_GRAYSCALE);
        input_image.put(0,0,bytes);
//        try {
//            input_image = Utils.loadResource(context, R.drawable.a, Imgcodecs.CV_LOAD_IMAGE_UNCHANGED);
//        }catch (IOException e){
//
//        }
        Imgproc.GaussianBlur(output_image,output_image,new Size(5,5),5,5);
        Imgproc.Canny(output_image,output_image,75,200,3,false);
        Log.i(TAG,"canny");

        /*contour*/
        Imgproc.findContours(output_image,cnt,new Mat(),0,2,new Point(0,0));

        /*cnt contourArea크기로 내림차순 정렬*/
        Collections.sort( cnt, new Comparator<MatOfPoint>() {
            @Override
            public int compare(MatOfPoint matOfPoint, MatOfPoint t1) {
                double a = Imgproc.contourArea(matOfPoint);
                double b = Imgproc.contourArea(t1);
                if(a> b) return -1;
                else if (a<b) return 1;
                else return 0;
            }
        });


        /*가장 큰 영역부터 4개의 꼭지점을 가지는 contour찾는다.*/
        double arclength;
        MatOfPoint2f mat2 = new MatOfPoint2f(); //contour 결과물 저장하는 motofpoint2f
        MatOfPoint2f approx = new MatOfPoint2f();
        for(MatOfPoint c :cnt){
            mat2.fromArray(c.toArray());
            arclength = Imgproc.arcLength(mat2,true);
            Imgproc.approxPolyDP(mat2,approx,0.02*arclength,true);
            if(approx.toArray().length==4){
                break;
            }
        }

        /*결과물 반환*/
        ArrayList<MatOfPoint> result_test = new ArrayList<MatOfPoint>();
        result_test.add(new MatOfPoint(approx.toArray()));
        Imgproc.drawContours(input_image,result_test,-1,new Scalar(255,0,0),2);
        Mat cvResult = input_image.clone();
        Bitmap b = Bitmap.createBitmap(cvResult.width(),cvResult.height(), Bitmap.Config.RGB_565);
        Utils.matToBitmap(cvResult,b);
        return b;
    }


}
