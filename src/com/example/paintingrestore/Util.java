package com.example.paintingrestore;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;

/*
 * This class implements JAVA interface to C++ functions
 */
public class Util {
	public static Point getPointOnOrig(Mat H, Point P) {
		Mat temp = new Mat(1, 2, CvType.CV_64FC1);
		temp.put(0,0,P.x); temp.put(0,1,P.y);
		mapPoint(H.getNativeObjAddr(), temp.getNativeObjAddr());
		P.x = temp.get(0, 0)[0]; P.y = temp.get(0, 1)[0];
		return P;
	}
	public static native void mapPoint(long addrH, long addrP);
}
