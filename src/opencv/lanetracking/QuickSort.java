package opencv.lanetracking;

import org.opencv.core.Mat;

public class QuickSort {
	private static int getMiddle(Mat list, int low, int high) {
		double[] tmp = list.get(0, low);
		while (low < high) {
			while (low < high && getLineLength(high, list) < getLineLength(tmp)) {
				high--;
			}
			list.put(0, low, list.get(0, high));
			while (low < high && getLineLength(low, list) < getLineLength(tmp)) {
				low++;
			}
			list.put(0, high, list.get(0, low));
		}
		list.put(0, low, tmp);
		return low;                   
	}
	
	private static void _quickSort(Mat list, int low, int high) {
		if (low < high) {
			int middle = getMiddle(list, low, high);
			_quickSort(list, low, middle - 1);
			_quickSort(list, middle + 1, high);
		}
	}

	public static void sort(Mat list) {
		if (list.cols() > 0) {
			_quickSort(list, 0, list.cols() - 1);
		}
	}
	

    
    private static double getLineLength(int index, Mat lines) {
    	double length;
    	double[] vec = lines.get(0, index);
		double x1 = vec[0], y1 = vec[1], x2 = vec[2], y2 = vec[3];
        length = (x1-x2)*(x1-x2) + (y1-y2)*(y1-y2);
		return length;
	}
    private static double getLineLength(double[] vec) {
    	double length;
    	double x1 = vec[0], y1 = vec[1], x2 = vec[2], y2 = vec[3];
        length = (x1-x2)*(x1-x2) + (y1-y2)*(y1-y2);
		return length;
	}
    
}
