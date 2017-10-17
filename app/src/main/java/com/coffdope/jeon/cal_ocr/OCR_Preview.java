package com.coffdope.jeon.cal_ocr;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;

import java.util.ArrayList;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.*;
import org.opencv.imgcodecs.Imgcodecs;


/**
 * Created by jeon on 17. 10. 17.
 */

public class OCR_Preview extends SurfaceView implements SurfaceHolder.Callback{
    private SurfaceHolder mHolder;
    private Context c;
    private Canvas mCanvas;
    private Bitmap cntBitmap;
    private Mat mat;

    public OCR_Preview(Context c,AttributeSet attrs){
        super(c,attrs);
        this.c = c;
        mHolder = getHolder();
        mHolder.addCallback(this);

    }
    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        cntBitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        mat = new Mat(getHeight(), getWidth(), Imgcodecs.CV_LOAD_IMAGE_COLOR);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

    }

    public void drawCnt(ArrayList<MatOfPoint> cnt){
        mCanvas = mHolder.lockCanvas();
        try{
            synchronized (mHolder) {
                Imgproc.drawContours(mat, cnt, -1, new Scalar(255, 0, 0), 5);
                Utils.matToBitmap(mat,cntBitmap);
                mCanvas.drawBitmap(cntBitmap, 0, 0, null);
            }
        }finally {
            mHolder.unlockCanvasAndPost(mCanvas);
        }
    }
}
