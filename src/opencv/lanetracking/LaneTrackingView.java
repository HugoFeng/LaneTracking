package opencv.lanetracking;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;
import org.opencv.imgproc.Imgproc;

import android.R.bool;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.SurfaceHolder;

class LaneTrackingView extends LaneTrackingViewBase {
    private static final String TAG = "LaneTracking::View";

    private Mat                 mRgba;
    private Mat                 mGray;
    private Mat                 mIntermediateMat;

    public LaneTrackingView(Context context) {
        super(context);
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.i(TAG, "called surfaceCreated");
        synchronized (this) {
            // initialize Mats before usage
            mGray = new Mat();
            mRgba = new Mat();
            mIntermediateMat = new Mat();
        }

        super.surfaceCreated(holder);
    }

    @Override
    protected Bitmap processFrame(VideoCapture capture) {
        switch (LaneTrackingNativeCamera.viewMode) {
        case LaneTrackingNativeCamera.VIEW_MODE_GRAY:
            capture.retrieve(mGray, Highgui.CV_CAP_ANDROID_GREY_FRAME);
            Imgproc.cvtColor(mGray, mRgba, Imgproc.COLOR_GRAY2RGBA, 4);
            break;
        case LaneTrackingNativeCamera.VIEW_MODE_RGBA:
            capture.retrieve(mRgba, Highgui.CV_CAP_ANDROID_COLOR_FRAME_RGBA);
            Core.putText(mRgba, "OpenCV+Android", new Point(10, 50), 3, 1, new Scalar(255, 0, 0, 255), 2);
            break;
        case LaneTrackingNativeCamera.VIEW_MODE_CANNY:
            capture.retrieve(mGray, Highgui.CV_CAP_ANDROID_GREY_FRAME);
            Imgproc.Canny(mGray, mIntermediateMat, 80, 100);
            Imgproc.cvtColor(mIntermediateMat, mRgba, Imgproc.COLOR_GRAY2BGRA, 4);
            break;
        case LaneTrackingNativeCamera.VIEW_MODE_TRACKING:
        	//capture.retrieve(mGray, Highgui.CV_CAP_ANDROID_GREY_FRAME);
        	capture.retrieve(mRgba, Highgui.CV_CAP_ANDROID_COLOR_FRAME_RGBA);
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
            double leftMaxLen = 0.0;
            double rightMaxLen = 0.0;
            double tempLen = 0.0;
            //Mat thresholdImage = new Mat(mRgba.rows() + mRgba.rows() / 2, mRgba.cols(), CvType.CV_8UC1);
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
                  Point start = new Point(x1, y1);
                  Point end = new Point(x2, y2);
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
            for(int x=0; (x < bigLineNum)&&(x<leftLineNum);x++){
            	double[] vec = leftLines.get(0, x);
                double x1 = vec[0], 
                       y1 = vec[1],
                       x2 = vec[2],
                       y2 = vec[3];
                tanTemp = (y1 - y2)/(x1 - x2);
                if(tanTemp > tanLeft){
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
                if(tanTemp < tanRight){
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
            
            Core.line(mRgba, leftStart, leftEnd, new Scalar(255,0,0), 5);    //Print the left lane
            Core.line(mRgba, rightStart, rightEnd, new Scalar(0,255,0), 5);    //Print the right lane
            
        	break;
        }

        Bitmap bmp = Bitmap.createBitmap(mRgba.cols(), mRgba.rows(), Bitmap.Config.ARGB_8888);

        try {
            Utils.matToBitmap(mRgba, bmp);
            return bmp;
        } catch(Exception e) {
            Log.e(TAG, "Utils.matToBitmap() throws an exception: " + e.getMessage());
            bmp.recycle();
            return null;
        }
    }

    public final boolean SMALL_FIRST = false;
    public final boolean BIG_FIRST = true;
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

    
    @Override
    public void run() {
        super.run();

        synchronized (this) {
            // Explicitly deallocate Mats
            if (mRgba != null)
                mRgba.release();
            if (mGray != null)
                mGray.release();
            if (mIntermediateMat != null)
                mIntermediateMat.release();

            mRgba = null;
            mGray = null;
            mIntermediateMat = null;
        }
    }
}
