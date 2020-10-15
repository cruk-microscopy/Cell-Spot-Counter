package cam.lmb.niek;


import java.util.ArrayList;


public class StatisticUtility {
	
	// translate between a certain value and Z-score of a Gaussian distribution
	public static double valueToZScore (double value, double[] MeanAndStdDev) {
		if (MeanAndStdDev==null || MeanAndStdDev.length!=2 || MeanAndStdDev[1]==0) return Double.NaN;
		return (value - MeanAndStdDev[0]) / MeanAndStdDev[1]; // (value - mean) / standard deviation
	}
	public static double zScoreToValue (double zScore, double[] MeanAndStdDev) {
		if (MeanAndStdDev==null || MeanAndStdDev.length!=2 || MeanAndStdDev[1]==0) return Double.NaN;
		return (zScore * MeanAndStdDev[1] + MeanAndStdDev[0]); // value = mean + (zScore*stdDev)
	}
	
	// removing outlier by iterating z-score test
	public static double[] regularizeDataByZscore (double[] data) {
		return regularizeDataByZscore(data, 1);
	}
	public static double[] regularizeDataByZscore (double[] data, int maxIter) {
		ArrayList<Integer> outliers = getOutlierStrict (data, maxIter);
		int size = data.length - outliers.size();
		if (size<=0) return null;
		double[] regularizedData = new double[size];
		int idx = 0;
		for (int i=0; i<data.length; i++) {
			if (outliers.contains(i)) continue;
			regularizedData[idx++] = data[i];
		}
		return regularizedData;
	}
	// keep getting outlier until no outlier can be found in data
	public static ArrayList<Integer> getOutlierStrict (double[] data) {
		return getOutlierStrict (data, 10);
	}
	public static ArrayList<Integer> getOutlierStrict (double[] data, int maxIteration) {
		ArrayList<Integer> outlierSum = new ArrayList<Integer>();
		int iter = 0;
		while (iter++ < maxIteration) {
			ArrayList<Integer> outlierCurrentIter = getOutlierByZscore(data, 3, outlierSum);
			if (outlierCurrentIter.size()==0) break;
			outlierSum.addAll(outlierCurrentIter);
		}
		return outlierSum;
	}
	
	// function to filter data by removing outlier recognized by 3-sigma test
	public static double[] filterDataByZscore (double[] data) {
		return filterDataByZscore(data, 3);
	}
	public static double[] filterDataByZscore (double[] data, int zScore) {
		double[] MeanStd = getMeanAndStdFast(data);
		double max = MeanStd[0] + zScore * MeanStd[1];
		double min = MeanStd[0] - zScore * MeanStd[1];
		ArrayList<Double> filteredList = new ArrayList<Double>();
		for (double value : data) {
			if (value > max || value < min) continue;
			filteredList.add(value);
		}
		int size = filteredList.size();
		if (size==0) return null;
		double[] filteredData = new double[size];
		for (int i=0; i<size; i++) {
			filteredData[i] = filteredList.get(i);
		}
		return filteredData;
	}
	
	
	// function to find outlier in data by 3-sigma test, standard deviation is not sample size corrected (for to fit Gaussian)
	public static ArrayList<Integer> getOutlierByZscore (double[] data) {
		return getOutlierByZscore(data, 3);
	}
	public static ArrayList<Integer> getOutlierByZscore (double[] data, int zScore) {
		return getOutlierByZscore(data, 3, null);
	}
	public static ArrayList<Integer> getOutlierByZscore (double[] data, int zScore, ArrayList<Integer> notInclude) {
		return getOutlierByZscore(data, 3, notInclude, false);
	}
	public static ArrayList<Integer> getOutlierByZscore (double[] data, int zScore, ArrayList<Integer> notInclude, boolean cumulative) {
		double[] MeanStd = getMeanAndStdFast(data, false, notInclude);
		double max = MeanStd[0] + zScore * MeanStd[1];
		double min = MeanStd[0] - zScore * MeanStd[1];
		ArrayList<Integer> outlierList = new ArrayList<Integer>();
		if (cumulative) outlierList.addAll(notInclude);
		for (int i=0; i<data.length; i++) {
			if (notInclude!=null && notInclude.contains(i)) continue;
			if (data[i] > max || data[i] < min) outlierList.add(i);
		}
		return outlierList;
	}
	
	// function to calculate mean and standard deviation of data in one go
	public static double[] getMeanAndStdFast (double[] data) {
		return getMeanAndStdFast(data, true);
	}
	public static double[] getMeanAndStdFast (double[] data, boolean correctSampleSize) {
		return getMeanAndStdFast(data, true, null);
	}
	public static double[] getMeanAndStdFast (double[] data, boolean correctSampleSize, ArrayList<Integer> notInclude) {
		if (data==null) return null;
		double[] subData; double N = 0;
		if (notInclude != null) {
			N = (double) (data.length - notInclude.size());
			if (N==0) return new double[]{0, 0};
			subData = new double[(int)N];
			int idx = 0;
			for (int i=0; i<data.length; i++) {
				if (notInclude.contains(i)) continue;
				subData[idx++] = data[i];
			}
		} else { 
			subData = data;
			N = (double) data.length;
		}
		if (N==0) return null;
		double sum = 0; double sum2 = 0;
		for (double value : subData) {
			sum += value;
			sum2 += value * value;
		}
		double mean = sum / N;
		if (N==1) return new double[]{mean, 0};
		double var = sum2 / N - mean * mean;
		if (correctSampleSize) var *= N / (N-1);
		return new double[]{mean, Math.sqrt(var)};
	}
	
	// function to get min, max, mean, stdDev in one go
	public static double[] getStatFast (double[] data) {
		return getStatFast(data, true);
	}
	public static double[] getStatFast (double[] data, boolean correctSampleSize) {
		if (data==null) return null;
		double N = (double) data.length;
		if (N==0) return new double[] {0, 0, 0, Double.NaN};
		if (N==1) return new double[]{data[0], data[0], data[0], 0};
		
		double min = Double.MAX_VALUE; double max = 0;
		double sum = 0; double sum2 = 0;
		for (double value : data) {
			sum += value;
			sum2 += value * value;
			min = Math.min(min, value);
			max = Math.max(max, value);
		}
		double mean = sum / N;
		double var = sum2 / N - mean * mean;
		if (correctSampleSize) var *= N / (N-1);
		return new double[]{min, max, mean, Math.sqrt(var)};
	}
	// function to get min, max, mean in one go
	public static double[] getStatFast2 (double[] data) {
		if (data==null) return null;
		double N = (double) data.length;
		if (N==0) return new double[] {0, 0, 0};
		if (N==1) return new double[]{data[0], data[0], data[0]};
		
		double min = Double.MAX_VALUE; double max = 0;
		double sum = 0;
		for (double value : data) {
			sum += value;
			min = Math.min(min, value);
			max = Math.max(max, value);
		}
		double mean = sum / N;
		return new double[]{min, max, mean};
	}	
	
}
