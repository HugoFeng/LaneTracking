#include <jni.h>
#include <jni.h>
#include <opencv2/core/core.hpp>
#include <opencv2/video/tracking.hpp>
#include <vector>

using namespace std;
using namespace cv;

extern "C" {
JNIEXPORT double* JNICALL Java_opencv_lanetracking_LaneTrackingProcess_useKalman(JNIEnv* env, jobject,
		CvKalman kalman, double[] measure)
{

	//2.kalman prediction
	const CvMat* prediction=cvKalmanPredict(kalman,0);
	CvPoint predict_pt=cvPoint((int)prediction->data.fl[0],(int)prediction->data.fl[1]);
	double[2] predict={(double)prediction->data.fl[0], (double)prediction->data.fl[1]};

	//3.update measurement
	measurement->data.fl[0]=(float)measure[0];
	measurement->data.fl[1]=(float)measure[1];

	//4.update
	cvKalmanCorrect( kalman, measurement );
}

JNIEXPORT CvKalman* JNICALL Java_opencv_lanetracking_LaneTrackingProcess_initKalman(JNIEnv*, jobject,
		int stateNum, int measureNum)
{
	//1.kalman filter setup
	CvKalman* kalman = cvCreateKalman( stateNum, measureNum, 0 );//state(x,y,detaX,detaY)
	//CvMat* process_noise = cvCreateMat( stateNum, 1, CV_32FC1 );
	CvMat* measurement = cvCreateMat( measureNum, 1, CV_32FC1 );//measurement(x,y)
	//CvRNG rng = cvRNG(-1);
	float A[stateNum][stateNum] ={//transition matrix
		1,0,
		0,1,
	};

	memcpy( kalman->transition_matrix->data.fl,A,sizeof(A));
	cvSetIdentity(kalman->measurement_matrix,cvRealScalar(1) );
	cvSetIdentity(kalman->process_noise_cov,cvRealScalar(1e-4));
	cvSetIdentity(kalman->measurement_noise_cov,cvRealScalar(1e-1));
	cvSetIdentity(kalman->error_cov_post,cvRealScalar(.1));
	return kalman;

}

}
