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
import Jama.Matrix;

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

    /*constructors*/
    public Detector() {
        super();
    }
    public Detector(Context context, Camera.Size size){
        this.context = context;
        this.size = size;
    }
    public Camera.Size getSize(){
        return size;
    }

    /*영역 인식 메서드,
    * byte배열로 주어진 이미지에서 윤곽선을 찾아 반환한다.
    * 이미지는 연산 속도를 위해 축소되어 처리된다.
    * */
    public ArrayList<MatOfPoint> detectPage(byte[] bytes){

        Mat input_image,output_image,inter_image;
        ArrayList<MatOfPoint> result_cnt = new ArrayList<MatOfPoint>();
        ArrayList<MatOfPoint> cnt = new ArrayList<MatOfPoint>();
        float ratio;

        ratio = (float)size.height/300;

        input_image = new Mat(size.height,size.width,CvType.CV_8UC1);
        inter_image = new Mat((int)(size.height/ratio),(int)(size.width/ratio),CvType.CV_8UC1);
        output_image = new Mat(inter_image.rows(),inter_image.cols(),CvType.CV_8UC1);
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
        if(!result_cnt.isEmpty()) {
            Core.multiply(result_cnt.get(0), new Scalar(ratio, ratio), result_cnt.get(0)); //원래 크기로 복구
        }

        return result_cnt; //contour 반환
    }

    /*
    * 윤곽선과 이미지를 받아 perspective transform을 수행하는 함수
    * */
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
    /*
    * 주어진 이미지에서 격자로 이루어진 사각형을 찾는다.
    * houghtransform을 이용한다.
    * */
    public Mat findRects(Mat src){
        Mat input,inter,hough, thres;
        input = src.clone();
        inter = new Mat(input.size(), CvType.CV_8UC1);
        thres = new Mat(input.size(), CvType.CV_8UC1);
        hough = new Mat();

        Imgproc.GaussianBlur(input, inter, new Size(5, 5),8,8);
        Imgproc.adaptiveThreshold(inter, thres,255,Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY,21,10);
        Imgproc.HoughLines(thres,hough,2,Math.PI/180,2500);

        /*ArrayList for intersect parameters*/
        ArrayList<ArrayList<Double>> intersect = new ArrayList<ArrayList<Double>>();
        ArrayList<Point> intersetionPoints = new ArrayList<Point>();

        double pos_hori=0;
        double pos_vert=0;

        for(int i=0; i<hough.rows(); ++i){
            double data[] = hough.get(i, 0);
            double rho = data[0];
            double theta = data[1];

            double cos = Math.cos(theta);
            double sin = Math.sin(theta);
            double x0 = cos * rho;
            double y0 = sin * rho;

            Point pt1 = new Point(x0 + 1000 * (-sin), y0 + 1000 * (cos));
            Point pt2 = new Point(x0 - 1000 * (-sin), y0 - 1000 * (cos));
            Imgproc.line(input, pt1, pt2, new Scalar(0, 0, 255), 1);

            if(sin >0.5){
               if(rho-pos_hori>10){
                   pos_hori=rho;
                   ArrayList<Double> tmp = new ArrayList<Double>();
                   tmp.add(rho);
                   tmp.add(theta);
                   tmp.add(-1d);
                   intersect.add(tmp);
               }
            }else {
                if(rho-pos_vert>10){
                    pos_vert=rho;
                    ArrayList<Double> tmp = new ArrayList<Double>();
                    tmp.add(rho);
                    tmp.add(theta);
                    tmp.add(1d);
                    intersect.add(tmp);
                }
            }
        }
        MTB(input);

        /*get intersection points*/
        for(int i=0; i<intersect.size();++i){
           if(intersect.get(i).get(2)<0){
               for(int j=0; j<intersect.size();++j){
                  if(intersect.get(j).get(2)>0){
                      double theta_point_1 = intersect.get(i).get(1);
                      double theta_point_2 = intersect.get(j).get(1);

                      double rho_point_1 = intersect.get(i).get(0);
                      double rho_point_2 = intersect.get(j).get(0);

                      double[][] cossin = {{Math.cos(theta_point_1), Math.sin(theta_point_1)}, {Math.cos(theta_point_2), Math.sin(theta_point_2)}};
                      double[][] rhos = {{rho_point_1}, {rho_point_2}};

                      Matrix cosSin = new Matrix(cossin);
                      Matrix rho_mat = new Matrix(rhos);

                      Matrix x = cosSin.solve(rho_mat);

                      intersetionPoints.add(new Point(x.get(0, 0), x.get(1, 0)));
                  }
               }
           }
        }
        for(Point i:intersetionPoints){
            Imgproc.circle(input,i,10,new Scalar(0,0,225));
        }
        MTB(input);
        // TODO: 17. 11. 4  점들 x,y 기준으로 정렬 필요
        return input;
    }

    /*
    * mat data to size matcing bitmap
    * */
    public Bitmap MTB(Mat src){
        Bitmap result = Bitmap.createBitmap(src.cols(), src.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(src,result);
        return result;
    }
}
