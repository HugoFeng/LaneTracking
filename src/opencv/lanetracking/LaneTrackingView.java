package opencv.lanetracking;

import java.text.DecimalFormat;

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
import android.text.format.Time;
import android.util.Log;
import android.view.SurfaceHolder;

class LaneTrackingView extends LaneTrackingViewBase {
    private static final String TAG = "LaneTracking::View";

    private Mat                 mRgba;
    private Mat                 mGray;
    private Mat                 mIntermediateMat;
    private long 				mLastTime;
    private int					mTimeCounter=0;
    private float				mFrameRate = 0;
    private static boolean		isFirstLaunch = true;

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
        	if(LaneTrackingNativeCamera.checkEqualizer){
        		Imgproc.equalizeHist(mGray, mGray);
        	}
            Imgproc.cvtColor(mGray, mRgba, Imgproc.COLOR_GRAY2RGBA, 4);
            break;
        case LaneTrackingNativeCamera.VIEW_MODE_RGBA:
            capture.retrieve(mRgba, Highgui.CV_CAP_ANDROID_COLOR_FRAME_RGBA);
            Core.putText(mRgba, "OpenCV+Android", new Point(5, 290), 3, 0.8, new Scalar(255, 0, 0, 255), 2);
            break;
        case LaneTrackingNativeCamera.VIEW_MODE_CANNY:
            capture.retrieve(mGray, Highgui.CV_CAP_ANDROID_GREY_FRAME);
        	if(LaneTrackingNativeCamera.checkEqualizer){
        		Imgproc.equalizeHist(mGray, mGray);
        	}
            Imgproc.Canny(mGray, mIntermediateMat, 80, 100);
            Imgproc.cvtColor(mIntermediateMat, mRgba, Imgproc.COLOR_GRAY2BGRA, 4);
            break;
        case LaneTrackingNativeCamera.VIEW_MODE_TRACKING:
        	//capture.retrieve(mGray, Highgui.CV_CAP_ANDROID_GREY_FRAME);
        	capture.retrieve(mRgba, Highgui.CV_CAP_ANDROID_COLOR_FRAME_RGBA);
        	LaneTrackingProcess ltp;
        	if (isFirstLaunch) {
        		ltp = new LaneTrackingProcess(mRgba, LaneTrackingProcess.INIT);
        		mRgba = ltp.laneTrackingProcess();
        		isFirstLaunch = false;
			} else {
				ltp = new LaneTrackingProcess(mRgba, 1);
				mRgba = ltp.laneTrackingProcess();
			}
//        	ltp = new LaneTrackingProcess(mRgba, 1);
//        	mRgba = ltp.laneTrackingProcess();
        	break;
        }
        
        // calculate and print frame rate on mRgba 
        long currentTime = System.currentTimeMillis();
        
        if (0 == mTimeCounter++)
        {
        	mLastTime = currentTime;
        }
        if (mTimeCounter==10) {
			mTimeCounter=0;
			long timeDelta = currentTime - mLastTime;
			mFrameRate = 10000.f/timeDelta ;
		}
        Core.putText(mRgba, "FrameRate: " + new DecimalFormat("###,###,###.##").format(mFrameRate ) ,
        		new Point(5, 315), 0, 0.5, new Scalar(255, 0, 0, 255), 1);
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
