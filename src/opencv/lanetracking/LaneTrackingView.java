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
        	capture.retrieve(mGray, Highgui.CV_CAP_ANDROID_GREY_FRAME);
            Imgproc.Canny(mGray, mIntermediateMat, 80, 100);
            Mat lines = new Mat();
//            Mat leftLines = Mat.zeros(4, 1000, CvType.CV_8UC1);
            Mat leftLines;
            Mat rightLines;
            int threshold = 8;
            int minLineSize = 20;
            int lineGap = 20;
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
            leftLines = new Mat();
            rightLines = new Mat();
            lines.copyTo(leftLines);
            lines.copyTo(rightLines);
            for (int x = 0; x < lines.cols(); x++){
                  double[] vec = lines.get(0, x);
                  double x1 = vec[0], 
                         y1 = vec[1],
                         x2 = vec[2],
                         y2 = vec[3];
                  Point start = new Point(x1, y1);
                  Point end = new Point(x2, y2);
                  double tan = (y1 - y2)/(x1 - x2);
                  tempLen = (x1-x2)*(x1-x2) + (y1-y2)*(y1-y2);
                  if(tan<0 && tempLen>leftMaxLen){
                	  
                	  leftLines.put(0, leftLineNum, vec);
                	  leftMaxLen = tempLen;
                	  leftLineNum++;
                  }
                  if(tan>0 && tempLen>rightMaxLen){
                	  rightLines.put(0, rightLineNum, vec);
               		  rightMaxLen = tempLen;
               		  rightLineNum++;
                  }
            }
            Log.d("mycv", "num = " + lines.cols());
            for(int x=1; (x < 15)&&(leftLineNum-x)>=0;x++){
            	double[] vec = leftLines.get(0, leftLineNum-x);
                double x1 = vec[0], 
                       y1 = vec[1],
                       x2 = vec[2],
                       y2 = vec[3];
                Point start = new Point(x1, y1);
                Point end = new Point(x2, y2);
                tanTemp = (y1 - y2)/(x1 - x2);
                if(tanTemp < tanLeft){
              	  tanLeft = tanTemp;
              	  leftStart = start;
              	  leftEnd = end;
                }
            }
            for(int x=1; (x < 15)&&(rightLineNum-x)>=0;x++){
            	double[] vec = rightLines.get(0, rightLineNum-x);
                double x1 = vec[0], 
                       y1 = vec[1],
                       x2 = vec[2],
                       y2 = vec[3];
                Point start = new Point(x1, y1);
                Point end = new Point(x2, y2);
                tanTemp = (y1 - y2)/(x1 - x2);
                if(tanTemp > tanRight){
              	  tanRight = tanTemp;
              	  rightStart = start;
              	  rightEnd = end;
                }
            }

            Imgproc.cvtColor(mIntermediateMat, mRgba, Imgproc.COLOR_GRAY2BGRA, 4);
            Core.line(mRgba, leftStart, leftEnd, new Scalar(255,0,0), 3);    //Print the left lane
            Core.line(mRgba, rightStart, rightEnd, new Scalar(0,255,0), 3);    //Print the right lane
            
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
