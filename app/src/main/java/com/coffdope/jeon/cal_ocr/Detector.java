package com.coffdope.jeon.cal_ocr;

import org.opencv.core.Core;
import org.opencv.core.CvType;
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
import java.util.Arrays;

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

//        try{
//            input_image = Utils.loadResource(context, R.drawable.a, Imgcodecs.CV_LOAD_IMAGE_COLOR);
//        }catch (IOException e){
//
//        }
//        Imgproc.resize(input_image,inter_image,new Size(inter_image.width(),inter_image.height()));
//        Imgproc.cvtColor(inter_image, inter_image, Imgproc.COLOR_BGR2GRAY);

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
        if(cnt.size()>5){
            int cnt_size = cnt.size();
            for(int i = cnt_size; i>5; i--){
                cnt.remove(i-5);
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
        if(!result_cnt.isEmpty())Core.multiply(result_cnt.get(0),new Scalar(ratio,ratio),result_cnt.get(0)); //원래 크기로 복구
        return result_cnt; //contour 반환
    }

    public Mat four_point_transform(MatOfPoint contour, Mat src){
        Point[] ordered = sortPoints(contour.toArray());

        Point tl = ordered[0];
        Point tr = ordered[1];
        Point br = ordered[2];
        Point bl = ordered[3];

        double widthA = Math.sqrt(Math.pow(br.x - bl.x, 2) + Math.pow(br.y - bl.y, 2));
        double widthB = Math.sqrt(Math.pow(tr.x - tl.x, 2) + Math.pow(tr.y - tl.y, 2));

        double dw = Math.max(widthA, widthB);
        int maxWidth = Double.valueOf(dw).intValue();

        double heightA = Math.sqrt(Math.pow(tr.x - br.x, 2) + Math.pow(tr.y - br.y, 2));
        double heightB = Math.sqrt(Math.pow(tl.x - bl.x, 2) + Math.pow(tl.y - bl.y, 2));

        double dh = Math.max(heightA, heightB);
        int maxHeight = Double.valueOf(dh).intValue();

        Mat result = new Mat(maxHeight, maxWidth, CvType.CV_8UC4);

        Mat src_mat = new Mat(4, 1, CvType.CV_32FC2);
        Mat dst_mat = new Mat(4, 1, CvType.CV_32FC2);

        src_mat.put(0, 0, tl.x, tl.y, tr.x, tr.y, br.x, br.y, bl.x, bl.y);
        dst_mat.put(0, 0, 0.0, 0.0, dw, 0.0, dw, dh, 0.0, dh);

        Mat m = Imgproc.getPerspectiveTransform(src_mat, dst_mat);
        Imgproc.warpPerspective(src, result, m, result.size());

        return  result;
    }

    private Point[] sortPoints( Point[] src ) {

        ArrayList<Point> srcPoints = new ArrayList<>(Arrays.asList(src));

        Point[] result = { null , null , null , null };

        Comparator<Point> sumComparator = new Comparator<Point>() {
            @Override
            public int compare(Point lhs, Point rhs) {
                return Double.valueOf(lhs.y + lhs.x).compareTo(rhs.y + rhs.x);
            }
        };

        Comparator<Point> diffComparator = new Comparator<Point>() {

            @Override
            public int compare(Point lhs, Point rhs) {
                return Double.valueOf(lhs.y - lhs.x).compareTo(rhs.y - rhs.x);
            }
        };

        // top-left corner = minimal sum
        result[0] = Collections.min(srcPoints, sumComparator);

        // bottom-right corner = maximal sum
        result[2] = Collections.max(srcPoints, sumComparator);

        // top-right corner = minimal diference
        result[1] = Collections.min(srcPoints, diffComparator);

        // bottom-left corner = maximal diference
        result[3] = Collections.max(srcPoints, diffComparator);
        return result;
    }

    // TODO: 17. 9. 26 findRect needs to be implemented
    public Mat findRects(Mat src){

        return new Mat();
    }
    public Bitmap cnt_image(byte[] bytes,ArrayList<MatOfPoint> contour){
        Imgproc.drawContours(input_image,contour,-1,new Scalar(255,0,0),2); //조건 만족하는 contour이는 경우 그린다.
        Bitmap b = Bitmap.createBitmap(input_image.width(),input_image.height(), Bitmap.Config.RGB_565);
        Utils.matToBitmap(input_image,b);

        return b;
    }

}
