package opencv.lanetracking;

import java.util.logging.Logger;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.KalmanFilter;

import android.R.integer;
import android.R.string;
import android.app.Dialog;
import android.nfc.Tag;
import android.os.Message;
import android.util.Log;

public class LaneTrackingProcess {
	
	private Mat                 mIntermediateMat;
    private Mat                 mRgba;
    private Mat                 mGray;
    public static final int 	INIT = -1;
    private static int 			mMode;
    
//	private static final int stateNum = 2;
//	private static final int measureNum = 2;
//	private static KalmanFilter kalmanL;
//	private static KalmanFilter kalmanR;
	
//	private static Mat measurementL;
//	private static Mat measurementR;
	
//	private static double[] estimateL;
//	private static double[] estimateR;
    public LaneTrackingProcess(Mat rgba, int mode) {
		// TODO Auto-generated constructor stub
    	mRgba = rgba;
    	mGray = new Mat();
    	mIntermediateMat = new Mat();
    	mMode = mode;
//    	kalmanL = new KalmanFilter(stateNum, measureNum);
//    	kalmanR = new KalmanFilter(stateNum, measureNum);
//    	
//    	//measurementL = new Mat(new Size(1,1), CvType.CV_64FC2);
//    	measurementL = new Mat(new Size(1,measureNum),CvType.CV_32FC1);
//    	measurementR = new Mat(new Size(1,measureNum), CvType.CV_64FC1);
    	
	}
	public Mat laneTrackingProcess(){
    	Imgproc.cvtColor(mRgba, mGray, Imgproc.COLOR_RGB2GRAY, 4);
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
        
//        Mat predictionL = kalmanL.predict();
//        //predictL = predictionL.get(0, 0); 
//        float[] vec = formLine(leftStart, leftEnd);
//        measurementL.put(0, 0, vec[0]);
//        measurementL.put(0, 1, vec[1]);
//        measurementL.rows();
//        measurementL.cols();
//        Mat estL = kalmanL.correct(measurementL);
//        //estimateL = estL.get(0, 0);
//        //estimateL = estL.get(1, 0);
//        float[] e = new float[2];
//        estL.get(0, 0, e);
//        float a = e[0] ;
//        float b = e[1] ;
        
        //Log.d("est", Double.toString(estimateL[0]) + Double.toString(estimateL[1]));
        
        
//        drawInfinitLine(mRgba, new Point(0, estimateL[1]), 
//        		new Point(- estimateL[0]/estimateL[1], 0),
//        		new Scalar(255,255,255), 10);
        
        

        if(!LaneTrackingNativeCamera.checkRgb){
        	Imgproc.cvtColor(mIntermediateMat, mRgba, Imgproc.COLOR_GRAY2BGRA, 4);
        }
        
        //Core.line(mRgba, leftStart, leftEnd, new Scalar(255,0,0), 5);    //Print the left lane
        //Core.line(mRgba, rightStart, rightEnd, new Scalar(0,255,0), 5);    //Print the right lane
        
        //draw infinit lines
        drawInfinitLine(mRgba, leftStart, leftEnd,new Scalar(255,0,0), 5);
        drawInfinitLine(mRgba, rightStart, rightEnd, new Scalar(0,255,0), 5);
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
    
	//use Kalman Filter
//	public native double[] useKalman(KalmanFilter kalman, double[] measure);
//	public native KalmanFilter initKalman(int stateNum, int measureNum); 
	
	
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

