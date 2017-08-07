package com.coffdope.jeon.cal_ocr;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
/**
 * Created by jeon on 17. 8. 7.
 */
/*영역 인식과 관련된 기능을 담당하는 클래스*/
public class Detector {

    public Detector(){
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

    }
    /*영역 인식 메서드, */
    /*public boolean catchArea(){

    }*/
}
