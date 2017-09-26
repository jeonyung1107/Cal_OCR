package com.coffdope.jeon.cal_ocr;

import com.googlecode.tesseract.android.TessBaseAPI;
//import com.googlecode.leptonica.android.
/**
 * Created by jeon on 17. 9. 6.
 */
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.content.Context;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

// TODO: 17. 9. 26 전처리 구현 필요
public class OCR  {
    Bitmap image; //사용되는 이미지
    private TessBaseAPI mTess; //Tess API reference
    String datapath = "" ; //언어데이터가 있는 경로
    Context mContext;

    public OCR(Context context) {
        mContext = context;

        //이미지 디코딩을 위한 초기화
        image = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.b); //샘플이미지파일
        //언어파일 경로
        datapath = mContext.getFilesDir()+ "/tesseract/";

        //트레이닝데이터가 카피되어 있는지 체크
        checkFile(new File(datapath + "tessdata/"));

        //Tesseract API
        String lang = "kor";

        //tesseract 엔진 초기화
        mTess = new TessBaseAPI();
        mTess.init(datapath, lang);
    }

    //Process an Image
    public String processImage(Bitmap bmp) {
        String OCRresult = null;
        mTess.setImage(bmp);
        OCRresult = mTess.getUTF8Text();

        return OCRresult;
    }


    //copy file to device
    private void copyFiles() {
        try{
            String filepath = datapath + "/tessdata/kor.traineddata";
            AssetManager assetManager = mContext.getAssets();
            InputStream instream = assetManager.open("tessdata/kor.traineddata");
            OutputStream outstream = new FileOutputStream(filepath);
            byte[] buffer = new byte[1024];
            int read;
            while ((read = instream.read(buffer)) != -1) {
                outstream.write(buffer, 0, read);
            }
            outstream.flush();
            outstream.close();
            instream.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //check file on the device
    private void checkFile(File dir) {
        //디렉토리가 없으면 디렉토리를 만들고 그후에 파일을 카피
        if(!dir.exists()&& dir.mkdirs()) {
            copyFiles();
        }
        //디렉토리가 있지만 파일이 없으면 파일카피 진행
        if(dir.exists()) {
            String datafilepath = datapath+ "/tessdata/kor.traineddata";
            File datafile = new File(datafilepath);
            if(!datafile.exists()) {
                copyFiles();
            }
        }
    }
}

