package opencv.lanetracking;

import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Range;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.KalmanFilter;

import android.R.integer;
import android.R.string;
import android.animation.IntEvaluator;
import android.app.Dialog;
import android.mtp.MtpConstants;
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
    	
//    	if (mMode!=INIT) {
//			
//		}
    	
        Mat roi = mRgba.submat(
        		new Range((int) ((1-0.618)*mRgba.rows()), mRgba.rows()-1), 
        		Range.all());
        //Mat testMat = processRoi(roi);
        
        processRoi(roi);
        return mRgba;
	}
	
	
	private Point[] detectLine(Mat roiCanny, boolean isLeftLine, 
			int bigLineNum, int threshold, int minLineSize, int lineGap) {
		Mat detectedLines = new Mat();
		
		int selectedLineNum=0;
		Point start = new Point();
        Point end = new Point();
        Point[] linePt = new Point[2];
        double tan = 0.0;
        double tanTemp;
		Imgproc.HoughLinesP(roiCanny, detectedLines, 7, Math.PI/180, threshold, minLineSize, lineGap);
		Mat selectedLines = new Mat(detectedLines.rows(),detectedLines.cols(),detectedLines.type());
		Log.d("mycv", "num of detected hough lines = " + detectedLines.cols());
		for (int x = 0; x < detectedLines.cols(); x++){
            double[] vec = detectedLines.get(0, x);
            double x1 = vec[0], 
                   y1 = vec[1],
                   x2 = vec[2],
                   y2 = vec[3];
            double k = (y1 - y2)/(x1 - x2);
            if((k>0 && isLeftLine) || (k<0 && !isLeftLine)){
            	selectedLines.put(0, selectedLineNum, vec);
            	selectedLineNum++;
            }
		}
		bubbleSort(selectedLines, selectedLineNum, BIG_FIRST, bigLineNum);
        for(int x=0; (x < bigLineNum)&&(x<selectedLineNum);x++){
        	double[] vec = selectedLines.get(0, x);
            double x1 = vec[0], 
                   y1 = vec[1],
                   x2 = vec[2],
                   y2 = vec[3];
            tanTemp = (y1 - y2)/(x1 - x2);
            if(isLeftLine&&(tanTemp>tan)&&(tanTemp<2)){
          	  	tan = tanTemp;
          	  	Point startTemp = new Point(x1, y1);
          	  	Point endTemp = new Point(x2, y2);
          	  	start = startTemp;
          	  	end = endTemp;
            }
            if(!isLeftLine&&(tanTemp<tan)&&(tanTemp>-2)){
            	tan = tanTemp;
                Point startTemp = new Point(x1, y1);
                Point endTemp = new Point(x2, y2);
                start = startTemp;
            	end = endTemp;
            }
        }
        linePt[0] = start;
        linePt[1] = end;
        return linePt;
		
	}

	private static final int initRoiLeft = 0;
	private static final int initRoiRight = 1;
	
	private List<MatOfPoint> genContourList(Point start, Point end, int rows, int cols) {
		List<MatOfPoint> roiContour = new ArrayList<MatOfPoint>(1);
		MatOfPoint contour;
		double line[] = formLine(start, end);
		double k = line[0];
		double b = line[1];
		Point upperCrossPoint = new Point(-b/k, 0);
		Point lowerCrossPoint = new Point((rows-1-b)/k,rows-1);
		double upperDelta = 10;
		double lowerDelta = 60;
		contour = new MatOfPoint(
				new Point(upperCrossPoint.x-upperDelta,0), 
				new Point(upperCrossPoint.x+upperDelta,0), 
				new Point(lowerCrossPoint.x+lowerDelta,rows-1),
				new Point(lowerCrossPoint.x-lowerDelta,rows-1)
				);
		roiContour.add(contour);
		return roiContour;
	}
	
	private List<MatOfPoint> genContourList(int rows,int cols,int type) {
		List<MatOfPoint> roiContour = new ArrayList<MatOfPoint>(1);
		MatOfPoint contour;
		switch (type) {
		case initRoiRight:
			contour = new MatOfPoint(
					new Point(0.4*cols,0), 
					new Point(0.5*cols,0), 
					new Point(0.5*cols, rows-1),
					new Point(0, rows-1),
					new Point(0, 0.4*rows)
					);
			break;
		case initRoiLeft:
			contour = new MatOfPoint(
					new Point(0.5*cols,0), 
					new Point(0.6*cols,0), 
					new Point(cols-1, 0.4*rows),
					new Point(cols-1, rows-1),
					new Point(0.5*cols, rows-1)
					);
			break;
		default:
			contour = new MatOfPoint();
			break;
		}
		roiContour.add(contour);
		return roiContour;
	}
	
	private Mat genMask(Point start, Point end, int rows,int cols) {
		Mat mask = new Mat(rows, cols, CvType.CV_8UC1);
		List<MatOfPoint> roiContour = genContourList(start, end, rows, cols);
		Core.fillPoly(mask,roiContour,new Scalar(255,0,0));
		return mask;
	}
	
	private Mat genMask(int rows,int cols,int type) {
		Mat mask = new Mat(rows, cols, CvType.CV_8UC1);
		List<MatOfPoint> roiContour = genContourList(rows, cols, type);
		Core.fillPoly(mask,roiContour,new Scalar(255,0,0));
		return mask;
	}
	private Mat processRoi(Mat roiRgba) {
		Mat initMaskLeft = genMask(roiRgba.rows(), roiRgba.cols(), initRoiLeft);
		Mat initMaskRight = genMask(roiRgba.rows(), roiRgba.cols(), initRoiRight);
		Mat roiGray = new Mat();
		Mat roiCannyTemp = new Mat();
		Imgproc.cvtColor(roiRgba, roiGray, Imgproc.COLOR_RGB2GRAY, 4);
    	if(LaneTrackingNativeCamera.checkEqualizer){
    		Imgproc.equalizeHist(roiGray, roiGray);
    	}
    	Imgproc.Canny(roiGray, roiCannyTemp, 100, 140);
    	Mat roiCannyLeft = new Mat();
    	Mat roiCannyRight = new Mat();
    	roiCannyTemp.copyTo(roiCannyLeft,initMaskLeft);
    	roiCannyTemp.copyTo(roiCannyRight,initMaskRight);
    	
//      if (mMode != INIT) {
//    	  
//		}
//      else {
//    	  
//		}
		
        int bigLineNum = 5;
        int threshold = 20;
        int minLineSize = 30;
        int lineGap = 15;
        Point leftStart = new Point();
        Point leftEnd = new Point();
        Point rightStart = new Point();
        Point rightEnd = new Point();
        Point[] linePt = new Point[2];
    	lastLeftStart = leftStart;
		lastLeftEnd = leftEnd;
		lastRightStart = rightStart;
		lastRightEnd = rightEnd;
        
		linePt = detectLine(roiCannyLeft, true, bigLineNum, threshold, minLineSize, lineGap);
		leftStart = linePt[0];
		leftEnd = linePt[1];
		
		linePt = detectLine(roiCannyRight, false, bigLineNum, threshold, minLineSize, lineGap);
		rightStart = linePt[0];
		rightEnd = linePt[1];
        
        
        double[] vec1 = {leftStart.x, leftStart.y, leftEnd.x, leftEnd.y};
        double[] vec2 = {rightStart.x, rightStart.y, rightEnd.x, rightEnd.y};
        Mat crossPoinMat = getCrossPoint(vec1, vec2);
        double[] vanishPointX = crossPoinMat.get(0, 0);
		double[] vanishPointY = crossPoinMat.get(1, 0);
		Point vanishPoint = new Point();
		vanishPoint.x = vanishPointX[0];
		vanishPoint.y = vanishPointY[0];
		roiRect = new Rect(0, (int) vanishPoint.y, roiRgba.cols()-1, roiRgba.rows()-(int)vanishPoint.y-1);
		Core.rectangle(roiRgba, roiRect.br(), roiRect.tl(), new Scalar(255,255,255), 2);
		List<MatOfPoint> initRoiContourLeft = genContourList(roiRgba.rows(), roiRgba.cols(), initRoiLeft);
		List<MatOfPoint> initRoiContourRight = genContourList(roiRgba.rows(), roiRgba.cols(), initRoiRight);
		Core.polylines(roiRgba, initRoiContourLeft, true, new Scalar(255, 0, 0), 1);
		Core.polylines(roiRgba, initRoiContourRight, true, new Scalar(0, 255, 0), 1);
		
		
		//draw vanish point
        Core.circle(roiRgba, vanishPoint, 10, new Scalar(255,255,0), -10);
//        //draw infinit lines
//        drawInfinitLine(mRgba, leftStart, leftEnd,new Scalar(255,0,0), 3);
//        drawInfinitLine(mRgba, rightStart, rightEnd, new Scalar(0,255,0), 3);
        Core.line(roiRgba, leftStart, leftEnd, new Scalar(255,0,0), 3);
        Core.line(roiRgba, rightStart, rightEnd, new Scalar(0,255,0), 3);
        lastLeftStart = leftStart;
		lastLeftEnd = leftEnd;
		lastRightStart = rightStart;
		lastRightEnd = rightEnd;
        return roiRgba;
		
	}
	

	
	
	private static double[] formLine(Point p1, Point p2) {
		double[] line = new double[2];
		// use 2 points to get line in formation of y=kx + b
		//calculate k
		line[0] = (p1.y-p2.y)/(p1.x-p2.x);
		
		//calculate b
		line[1] = p1.y - line[0] * p1.x;
		
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

