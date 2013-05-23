package opencv.lanetracking;

import java.util.logging.Logger;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.KalmanFilter;

import android.R.integer;
import android.R.string;
import android.app.Dialog;
import android.nfc.Tag;
import android.os.Message;
import android.provider.MediaStore.Video;
import android.renderscript.Sampler.Value;
import android.util.Log;
import android.view.ViewDebug.IntToString;
import android.widget.ImageButton;

public class LaneTrackingProcess {
	
	private Mat                 mIntermediateMat;
    private Mat                 mRgba;
    private Mat                 mGray;
    public static final int 	INIT = -1;
    private static int 			mMode;
    private static Point 		lastLeftStart;
    private static Point 		lastLeftEnd;
    private static Point 		lastRightStart;
    private static Point 		lastRightEnd;
    private static Rect			roiRect;
    
    
    public LaneTrackingProcess(Mat rgba, int mode) {
		// TODO Auto-generated constructor stub
    	mRgba = rgba;
    	mGray = new Mat();
    	mIntermediateMat = new Mat();
    	mMode = mode;
    	
	}
	public Mat laneTrackingProcess(){
    	Imgproc.cvtColor(mRgba, mGray, Imgproc.COLOR_RGB2GRAY, 4);
    	if (mMode!=INIT) {
			
		}
    	
    	if(LaneTrackingNativeCamera.checkEqualizer){
    		Imgproc.equalizeHist(mGray, mGray);
    	}
    	
        Imgproc.Canny(mGray, mIntermediateMat, 100, 140);
        
        
        Mat lines = new Mat();
        Mat leftLines;
        Mat rightLines;
        int bigLineNum = 5;
        int threshold = 20;
        int minLineSize = 30;
        int lineGap = 15;
        int rightLineNum=0;
        int leftLineNum=0;
        Point leftStart = new Point();
        Point leftEnd = new Point();
        Point rightStart = new Point();
        Point rightEnd = new Point();
        double tanLeft = 0.0;
        double tanRight = 0.0;
        double tanTemp;
        
//        if (mMode != INIT) {
//			Mat roiMat = mIntermediateMat.submat(roiRect);
//			Imgproc.HoughLinesP(roiMat, lines, 7, Math.PI/180, threshold, minLineSize, lineGap);
//		}
//        else {
//        	Imgproc.HoughLinesP(mIntermediateMat, lines, 7, Math.PI/180, threshold, minLineSize, lineGap);
//		}
        
        Imgproc.HoughLinesP(mIntermediateMat, lines, 7, Math.PI/180, threshold, minLineSize, lineGap);
        
        int matType = lines.type();
        leftLines = new Mat(lines.rows(),lines.cols(),matType);
        rightLines = new Mat(lines.rows(),lines.cols(),matType);
        
        for (int x = 0; x < lines.cols(); x++){
              double[] vec = lines.get(0, x);
              double x1 = vec[0], 
                     y1 = vec[1],
                     x2 = vec[2],
                     y2 = vec[3];
              double tan = (y1 - y2)/(x1 - x2);
              if(tan>0){
            	  leftLines.put(0, leftLineNum, vec);
            	  leftLineNum++;
              }
              if(tan<0){
            	  rightLines.put(0, rightLineNum, vec);
           		  rightLineNum++;
              }
        }
        Log.d("mycv", "num = " + lines.cols());
        bubbleSort(leftLines, leftLineNum, BIG_FIRST, bigLineNum);
        bubbleSort(rightLines, rightLineNum, BIG_FIRST, bigLineNum);
        
        
        
        
//        QuickSort.sort(leftLines);
//        QuickSort.sort(rightLines);
        
        for(int x=0; (x < bigLineNum)&&(x<leftLineNum);x++){
        	double[] vec = leftLines.get(0, x);
            double x1 = vec[0], 
                   y1 = vec[1],
                   x2 = vec[2],
                   y2 = vec[3];
            tanTemp = (y1 - y2)/(x1 - x2);
            if((tanTemp > tanLeft)&&(tanTemp<2)){
          	  tanLeft = tanTemp;
              Point start = new Point(x1, y1);
              Point end = new Point(x2, y2);
          	  leftStart = start;
          	  leftEnd = end;
            }
        }
        for(int x=0; (x < bigLineNum)&&(x<rightLineNum); x++){
        	double[] vec = rightLines.get(0, x);
            double x1 = vec[0], 
                   y1 = vec[1],
                   x2 = vec[2],
                   y2 = vec[3];
            tanTemp = (y1 - y2)/(x1 - x2);
            if((tanTemp < tanRight)&&(tanTemp>-2)){
          	  tanRight = tanTemp;
              Point start = new Point(x1, y1);
              Point end = new Point(x2, y2);
          	  rightStart = start;
          	  rightEnd = end;
            }
        }
        

        if(!LaneTrackingNativeCamera.checkRgb){
        	Imgproc.cvtColor(mIntermediateMat, mRgba, Imgproc.COLOR_GRAY2BGRA, 4);
        }
//        if(mMode != INIT){
//        	float angleNow = Core.fastAtan2((float) (leftEnd.y-leftStart.y), (float) (leftEnd.x-leftStart.x));
//        	float angleLast = Core.fastAtan2((float) (lastLeftEnd.y-lastLeftStart.y), (float) (lastLeftEnd.x-lastLeftStart.x));
//        	if ((angleNow-angleLast)<-5 || (angleNow-angleLast)>5) {
//				leftStart = lastLeftStart;
//				leftEnd = lastLeftEnd;
//				rightStart = lastRightStart;
//				rightEnd = lastRightEnd;
//			}
//        }
//        else {
//			lastLeftStart = leftStart;
//			lastLeftEnd = leftEnd;
//			lastRightStart = rightStart;
//			lastRightEnd = rightEnd;
//		}
    	lastLeftStart = leftStart;
		lastLeftEnd = leftEnd;
		lastRightStart = rightStart;
		lastRightEnd = rightEnd;
        
        //Core.line(mRgba, leftStart, leftEnd, new Scalar(255,0,0), 5);    //Print the left lane
        //Core.line(mRgba, rightStart, rightEnd, new Scalar(0,255,0), 5);    //Print the right lane
//        Mat pointMap = mapCrossPoints(leftLines, rightLines, 1, 480, 320);
//        Imgproc.cvtColor(points, mRgba, Imgproc.COLOR_GRAY2BGRA, 4);
//        int[] vanishPoint = getVanishPoint(pointMap);
//        //********************
        
        
        double[] vec1 = {leftStart.x, leftStart.y, leftEnd.x, leftEnd.y};
        double[] vec2 = {rightStart.x, rightStart.y, rightEnd.x, rightEnd.y};
        Mat crossPoinMat = getCrossPoint(vec1, vec2);
        double[] vanishPointX = crossPoinMat.get(0, 0);
		double[] vanishPointY = crossPoinMat.get(1, 0);
		Point vanishPoint = new Point();
		vanishPoint.x = vanishPointX[0];
		vanishPoint.y = vanishPointY[0];
		roiRect = new Rect(0, (int) vanishPoint.y, mRgba.cols()-1, mRgba.rows()-(int)vanishPoint.y-1);
		Core.rectangle(mRgba, roiRect.br(), roiRect.tl(), new Scalar(255,255,255), 2);
		
		//draw vanish point
        Core.circle(mRgba, vanishPoint, 10, new Scalar(255,255,0), -10);
        //draw infinit lines
        drawInfinitLine(mRgba, leftStart, leftEnd,new Scalar(255,0,0), 3);
        drawInfinitLine(mRgba, rightStart, rightEnd, new Scalar(0,255,0), 3);
//        Core.line(mRgba, leftStart, leftEnd, new Scalar(255,0,0), 3);
//        Core.line(mRgba, rightStart, rightEnd, new Scalar(0,255,0), 3);
        lastLeftStart = leftStart;
		lastLeftEnd = leftEnd;
		lastRightStart = rightStart;
		lastRightEnd = rightEnd;
        return mRgba;
	}
	

	private static float[] formLine(Point p1, Point p2) {
		float[] line = new float[2];
		// use 2 points to get line in formation of y=kx + b
		//calculate k
		line[0] = (float) ((p1.y-p2.y)/(p1.x-p2.x));
		
		//calculate b
		line[1] = (float) (p1.y - line[0] * p1.x);
		
		return line;
	}
	private static void  drawInfinitLine(Mat image,Point p1,Point p2, Scalar color, int thickness) {
		int xMax = image.width();
		int yMax = image.height();
		Point start = new Point();
		Point end = new Point();
		start.y = (double) yMax;
		start.x = (start.y-p1.y)*(p2.x-p1.x)/(p2.y-p1.y)+p1.x;
		
		end.y = 0.0;
		end.x = (end.y-p1.y)*(p2.x-p1.x)/(p2.y-p1.y) + p1.x;
		
		Core.line(image, start, end, color, thickness);
	}
    
	private static int[] getVanishPoint(Mat pointMap) {
		double value =0;
		byte[] val = new byte[1];
		byte[] temp = new byte[1];
		int[] vec = new int[2]; 
		val[0] = (byte)0;
		vec[0] = 0;
		vec[1] = 0;
		for (int i = 0; i < pointMap.cols(); i++) {
			for (int j = 0; j < pointMap.rows(); j++) {
				//double[] temp = pointMap.get(j, i);
				pointMap.get(j, i, temp);
				if (temp[0] > val[0]) {
					val[0] = temp[0];
					vec[0] = i;
					vec[1] = j;
				}
			}
		}
		return vec;
	}
	private Mat mapCrossPoints(Mat linesL, Mat linesR, int bigLineNum, int width, int height) {
		Mat crossPointMapMat = Mat.zeros(height, width, CvType.CV_8UC1);
		for(int i = 0; i< (linesL.cols()<bigLineNum?linesL.cols():bigLineNum); i++){
			for(int j = 0; j<(linesR.cols()<bigLineNum?linesR.cols():bigLineNum); j++){
				if (linesL.cols()!=0 && linesR.cols()!=0) {
					double[] vec1 = linesL.get(0, i);
					double[] vec2 = linesR.get(0, j);
					Mat point = getCrossPoint(vec1, vec2);
					double[] x = point.get(0, 0);
					double[] y = point.get(1, 0);
					if ((int)x[0]<width && (int)y[0]<height) {
						double[] dvalue = crossPointMapMat.get((int)y[0], (int)x[0]);
						dvalue[0] ++;
						int[] ivalue = new int[1];
						ivalue[0] = (int) dvalue[0];
						crossPointMapMat.put((int)y[0], (int)x[0], dvalue);
					}
				}
				else {
					break;
				}
				
			}
		}
		return crossPointMapMat;
	}
	public  Mat getCrossPoint(double[] vec1, double[] vec2) {
		double x1 = vec1[0];
		double y1 = vec1[1];
		double x2 = vec1[2];
		double y2 = vec1[3];
		double x3 = vec2[0];
		double y3 = vec2[1];
		double x4 = vec2[2];
		double y4 = vec2[3];
		
		Mat equationA = new Mat(2,2,CvType.CV_64FC1);
		Mat equationB = new Mat(2,1,CvType.CV_64FC1);
		Mat solution = new Mat(2,1,CvType.CV_64FC1);
		equationA.put(0, 0, y1-y2);
		equationA.put(1, 0, y3-y4);
		equationA.put(0, 1, x2-x1);
		equationA.put(1, 1, x4-x3);
		equationB.put(0, 0, -(x1-x2)*y1-x1*(y2-y1));
		equationB.put(1, 0, -(x3-x4)*y3-x3*(y4-y3));
		Core.solve(equationA, equationB, solution);
		double[] x = solution.get(0, 0);
		double[] y = solution.get(1, 0);
		
		
		
		return solution;
	}

	public final static boolean SMALL_FIRST = false;
    public final static boolean BIG_FIRST = true;
    public static void bubbleSort(Mat lines, int totalNum, Boolean isBigFirst, int rankNum){
        double tempLenThis, tempLenNext;
        int num=rankNum<totalNum?rankNum:totalNum;
    	for(int x=0; x<num; x++){
    		for(int y=totalNum-1; y>x; y--){
            	double[] vecT = lines.get(0, y);
            	double[] vecN = lines.get(0, y-1);
    			double xt1 = vecT[0], yt1 = vecT[1], xt2 = vecT[2], yt2 = vecT[3];
    			double xn1 = vecN[0], yn1 = vecN[1], xn2 = vecN[2], yn2 = vecN[3];
                tempLenThis = (xt1-xt2)*(xt1-xt2) + (yt1-yt2)*(yt1-yt2);
                tempLenNext = (xn1-xn2)*(xn1-xn2) + (yn1-yn2)*(yn1-yn2);
                if ((tempLenThis>tempLenNext && isBigFirst) || (tempLenThis<tempLenNext && !isBigFirst)) {
                	lineExchange(y, y-1, lines);
				}

    		}
        }
    }
    private static void lineExchange(int index1, int index2, Mat lines) {
    	double[] vec1 = lines.get(0, index1);
    	double[] vec2 = lines.get(0, index2);
    	lines.put(0, index1, vec2);
    	lines.put(0, index2, vec1);
	}


    
}

