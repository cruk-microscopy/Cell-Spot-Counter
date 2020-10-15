package cam.lmb.niek;


import java.text.DecimalFormat;

import ij.ImagePlus;
import ij.gui.Roi;


public class InfoUtility {

	
	protected final static int lineWidth = 80;
	//protected static ImagePlus sourceImage;
	
	/*
	 * 	Functions to configure and display strings to source info panel
	 */
	// function to generate string of image information
	public static String getImageInfo (ImagePlus imp) {
		if (imp==null)	return ("No image recognized!"); // if no image recognized, return;
		String head = "Image:\n";
		// get image title info
		String imageTitle = "   Title: " + imp.getTitle();
		imageTitle = wrapString(imageTitle, lineWidth, 6) + "\n";
		// get image size in RAM, and bit depth info
		double sizeInMBGB = imp.getSizeInBytes()/1048576; String sizeUnit = "MB";
		if (sizeInMBGB>1024) { sizeInMBGB /= 1024; sizeUnit = "GB";}
		String imageSize = "   Size: "
				+ new DecimalFormat("0.#").format(sizeInMBGB)
				+ sizeUnit + " (" + String.valueOf(imp.getBitDepth()) + " bit)";
		if (sizeUnit=="GB" && sizeInMBGB>2) {imageSize += " (image might be too large!)";}
		imageSize = wrapString(imageSize, lineWidth, 6) + "\n";
		// get image dimension info
		int[] dim = imp.getDimensions();
		String imageDimension = "   Dimension:  X: " + String.valueOf(dim[0])
		  + "  Y: " + String.valueOf(dim[1])
		  + "  Z: " + String.valueOf(dim[3])
		  + "  C: " + String.valueOf(dim[2])
		  + "  T: " + String.valueOf(dim[4]);
		imageDimension = wrapString(imageDimension, lineWidth, 6) + "\n";
		// get image calibration info
		String imageCalibration = "   pixel size: ";
		String pixelSize = new DecimalFormat("0.###").format(imp.getCalibration().pixelWidth);
		String calInfo = imp.getCalibration().getUnit().equals("pixel") ? "not calibrated" : pixelSize + " " + imp.getCalibration().getUnit();
		imageCalibration += calInfo; 
		imageCalibration = wrapString(imageCalibration, lineWidth, 6) + "\n";
		// get image ROI info
		//String imageRoi = imp.getRoi()==null ? "   ROI: NO" : "ROI: YES";
		//imageRoi = wrapString(imageRoi, lineWidth, 1) + "\n";
		// get image overlay info // omit for now
		//String imgOverlay = imp.getOverlay()==null?"Image does not have overlay.":"Image contains overlay.";
		//imgOverlay = wrapString(imgOverlay, lineWidth, 1) + "\n";
		
		return (head + imageTitle + imageSize + imageDimension + imageCalibration);
	}
	public static String getCellROIInfo (ImagePlus imp) {
		if (imp==null) return "";
		Roi[] rois = ROIUtility.rois;
		if (rois==null || rois.length==0) return "";
		String head = "Cell:\n";
		// display method
		String method = "   Method: " + DetectionUtility.roiModes[DetectionUtility.autoROIMode] + "\n";
		// get cell count
		int nCell = rois.length;
		String cellCount = "   Count: " + nCell + "\n";
		// get cell area stats and info
		//double pixeSize = imp.getCalibration().pixelWidth;
		double[] areas = new double[nCell];
		for (int i=0; i<nCell; i++) {
			imp.setRoi(rois[i], false);
			areas[i] = imp.getStatistics().area;
		}
		imp.deleteRoi();
		
		double[] stats = StatisticUtility.getStatFast2(areas);
		double minArea = stats[0];// * Math.pow(pixeSize, 2);
		double maxArea = stats[1];// * Math.pow(pixeSize, 2);
		double meanArea = stats[2];// * Math.pow(pixeSize, 2);
		// extract calibration unit string
		String areaUnit = "pixel²";
		String unit = imp.getCalibration().getUnit().toLowerCase();
		//System.out.println("debug: pixel unit: " + unit);
		if (unit.equals("micron") || unit.equals("µm") || unit.equals("um"))
			areaUnit = "µm²";
		// get sample ROI info
		String cellArea = "   Size: ";
		if (meanArea > 1e4) { minArea /= 1e6; maxArea /= 1e6; meanArea /= 1e6; areaUnit = "mm²"; }
		
		cellArea += "[ " + new DecimalFormat("0.##").format(minArea) + " -";
		cellArea += " " + new DecimalFormat("0.##").format(meanArea) + " -";
		cellArea += " " + new DecimalFormat("0.##").format(maxArea) + " ]";
		cellArea += " " + areaUnit;
		cellArea = wrapString(cellArea, lineWidth, 6) + "\n";

		return (head + method + cellCount + cellArea);		
	}
	
	// function to generate statistic information
	public static String getSpotInfo () {
		if (DetectionUtility.logSpotOverlay==null && DetectionUtility.maxSpotOverlay==null) return "";
		String head = "Spot:\n";
		// get spot overlays from DetectionUtility
		int nLogSpot = DetectionUtility.nLogSpot;
		int nMaxSpot = DetectionUtility.nMaxSpot;
		int nJointSpot = DetectionUtility.nJointSpot;
		String spotCount = "   Count: LoG:" + nLogSpot + ", Max:" + nMaxSpot + ", Joint:" + nJointSpot + "\n"; 
		// get cell ROI info
		Roi[] rois = ROIUtility.rois;
		if (rois==null || rois.length==0) return (head + spotCount);
		// extract area info and area unit string
		int nCell = rois.length;
		String logPerCell = new DecimalFormat("0.#").format((double)nLogSpot/(double)nCell);
		String maxPerCell = new DecimalFormat("0.#").format((double)nMaxSpot/(double)nCell);
		String jointPerCell = new DecimalFormat("0.#").format((double)nJointSpot/(double)nCell);
		
		String countPerCell = "   per cell: LoG:" + logPerCell + ", Max:" + maxPerCell + ", Joint:" + jointPerCell + "\n";
		
		return (head + spotCount + countPerCell);
	}

	/*
	 * function to generate channel statistics based on spots
	 * C1: Mean:[100 - 3500 - 65536]  StdDev:[100 - 3500 - 65536]
	 * C2: Mean:[100 - 3500 - 65536]  StdDev:[100 - 3500 - 65536]
	 * C3: Mean:[100 - 3500 - 65536]  StdDev:[100 - 3500 - 65536] 
	 */
	protected static String getStatInfo (double[][] dataMean, double[][] dataStdDev) {
		/*
		if (imp==null || overlay==null || overlay.size()==0) return "";
		ImagePlus impDup = imp.duplicate();
		//double pixelSize = imp.getCalibration().pixelWidth;
		double radius = DetectionUtility.spotRadius / imp.getCalibration().pixelWidth;
		//Roi[] spots = impDup.getOverlay().toArray();
		Roi[] spots = overlay.toArray();
		if (spots.length==1) {
			if (spots[0] instanceof PointRoi) {
				Point[] points = spots[0].getContainedPoints();
				spots = new Roi[points.length];
				for (int i=0; i<points.length; i++) {
					spots[i] = new OvalRoi(points[i].getX()-radius, points[i].getY()-radius, radius*2, radius*2);
					spots[i].setPosition(0, 0, 0);
				}
			}
		}
		
		int numSpots = spots.length;
		int numC = impDup.getNChannels();
		// get data
		double[][] dataMean = new double[numC][numSpots];
		double[][] dataStdDev = new double[numC][numSpots];
		
		for (int c=0; c<numC; c++) {
			impDup.setPositionWithoutUpdate(c+1, impDup.getZ(), impDup.getT());
			for (int i=0; i<numSpots; i++) {
				impDup.setRoi(spots[i], false);
				dataMean[c][i] = impDup.getRawStatistics().mean;
				dataStdDev[c][i] = impDup.getRawStatistics().stdDev;
			}
		}
		impDup.close(); IJ.run("Collect Garbage", "");
		*/
		// calculate statistics, generate stats info string // + StringUtils.rightPad(""+(int)statsMean[0], 5) + " - "
		if (dataMean==null || dataMean.length==0) return "";
		int numC = dataMean.length;
		String statInfo = "";
		for (int c=0; c<numC; c++) {
			double[] statsMean = StatisticUtility.getStatFast2(dataMean[c]);
			double[] statsStdDev = StatisticUtility.getStatFast2(dataStdDev[c]);
			String channelInfo = "C" + (c+1)
					+ ":   Mean:[ " + (int)statsMean[0] + " - " + (int)statsMean[2] + " - " + (int)statsMean[1] + " ] "
					+ "    StdDev:[ " + (int)statsStdDev[0] + " - " + (int)statsStdDev[2] + " - " + (int)statsStdDev[1] + " ]\n";
			statInfo += channelInfo;
		}
		return statInfo;
	}
	

	/*
	 * function to generate filter list information
	 * Filter 1 (DAPI): Count: 1000 -> 988, Density: 200 -> 170 cells / mm2
	 * C2: Mean:[100 - 3500 - 65536]  StdDev:[100 - 3500 - 65536]
	 * C3: Mean:[100 - 3500 - 65536]  StdDev:[100 - 3500 - 65536] 
	 */

	// function to wrap string after certain length for display in text area
	public static String wrapString(
			String inputLongString,
			int wrapLength,
			int indent
			) {
		String wrappedString = ""; String indentStr = "";
		for (int i=0; i<indent; i++)
			indentStr += " ";
		for (int i=0; i<inputLongString.length(); i++) {
			if (i!=0 && i%lineWidth==0)	wrappedString += ("\n"+indentStr);
			wrappedString += inputLongString.charAt(i);
		}
		return wrappedString;
	}
		
	
}
