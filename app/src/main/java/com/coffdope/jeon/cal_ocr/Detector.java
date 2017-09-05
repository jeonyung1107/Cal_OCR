package com.coffdope.jeon.cal_ocr;

import org.opencv.core.Core;
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
    private Context context;
    private Camera.Size size;
    private Mat input_image,output_image,inter_image;
    private float ratio;
    private ArrayList<MatOfPoint> cnt = new ArrayList<MatOfPoint>();
    public Detector() {
        super();
    }

    public Detector(Context context, Camera.Size size){
        this.context = context;
        this.size = size;
    }
    public float getRatio(){
        return ratio;
    }
    public Camera.Size getSize(){
        return size;
    }
    /*영역 인식 메서드, */
    public ArrayList<MatOfPoint> detectPage(byte[] bytes){
        Bitmap[] result_bitmap = new Bitmap[2];
        ArrayList<MatOfPoint> result_cnt = new ArrayList<MatOfPoint>();
        ratio = (float)size.height/300;
        input_image = new Mat(size.height,size.width,Imgcodecs.CV_LOAD_IMAGE_GRAYSCALE);
        inter_image = new Mat((int)(size.height/ratio),(int)(size.width/ratio),Imgcodecs.CV_LOAD_IMAGE_GRAYSCALE);
        output_image = new Mat(inter_image.rows(),inter_image.cols(),Imgcodecs.CV_LOAD_IMAGE_GRAYSCALE);
        input_image.put(0,0,bytes);
        Imgproc.resize(input_image,inter_image,new Size(inter_image.width(),inter_image.height()));

        /*이미지 전처리*/
        Imgproc.GaussianBlur(inter_image,inter_image,new Size(5,5),8,8);
        Imgproc.Canny(inter_image,output_image,75,200,3,false);
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
        /*top 5 저장*/
        if(cnt.size()>1){
            int cnt_size = cnt.size();
            for(int i = cnt_size; i>1; i--){
                cnt.remove(i-1);
            }
        }

        /*가장 큰 영역부터 4개의 꼭지점을 가지는 contour찾는다.*/
        double arclength;
        MatOfPoint2f mat2 = new MatOfPoint2f(); //contour 결과물 저장하는 motofpoint2f
        MatOfPoint2f approx = new MatOfPoint2f();

        /*cnt에 저장되어있는 contour들 조건 탐색*/
        for(MatOfPoint c :cnt){
            mat2.fromArray(c.toArray()); //matofpoint2f 형태로 변환
            arclength = Imgproc.arcLength(mat2,true);
            Imgproc.approxPolyDP(mat2,approx,0.1*arclength,true); //단순화
            if(approx.toArray().length==4&&Imgproc.contourArea(c)>1000){
                result_cnt.add(new MatOfPoint(approx.toArray())); //선택된 contour만 추가한다.
                Log.i(TAG,"success!!");
                break;
            }
        }

        /*결과물 반환*/
        //todo 조건 충족하는 contour없는 경우 설정해서 결과 출력하도록 할것
        //todo 마지막에 어떻게 transform 할것인지 고민할 필요 있음
        return result_cnt; //contour 반환
    }
    //todo transform 구현
    public Bitmap four_point_transform(ArrayList<MatOfPoint> contour, ){

    }

    public Bitmap cnt_image(byte[] bytes,ArrayList<MatOfPoint> contour){
        if(!contour.isEmpty()){Core.multiply(contour.get(0),new Scalar(ratio,ratio),contour.get(0));}
        if(!contour.isEmpty()) {Imgproc.drawContours(input_image,contour,-1,new Scalar(255,0,0),2);} //조건 만족하는 contour이는 경우 그린다.
        Bitmap b = Bitmap.createBitmap(input_image.width(),input_image.height(), Bitmap.Config.RGB_565);
        Utils.matToBitmap(input_image,b);

        return b;
    }

}
