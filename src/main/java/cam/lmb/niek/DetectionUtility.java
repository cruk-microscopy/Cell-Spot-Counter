package cam.lmb.niek;

import java.awt.Color;
import java.awt.Point;
import java.awt.Polygon;
import java.util.Iterator;
import java.util.Map;

import javax.swing.JPanel;

import org.scijava.prefs.DefaultPrefService;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.detection.LogDetectorFactory;

import ij.ImagePlus;
import ij.CompositeImage;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.OvalRoi;
import ij.gui.Overlay;
import ij.gui.PointRoi;
import ij.gui.Roi;
import ij.plugin.filter.MaximumFinder;
import ij.process.ImageProcessor;
import ij.process.LUT;


public class DetectionUtility {
	
	protected static ImagePlus sourceImage;
	//protected static Overlay spotOverlay;
	
	protected static final String[] autoMethods = {"Huang", "Intermodes", "IsoData", "Li", "MaxEntropy", "Mean", "MinError(I)", "Minimum",
			"Moments", "Otsu", "Percentile", "RenyiEntropy", "Shanbhag", "Triangle", "Yen"};
	
	// color parameters (default custom color: yellow)
	protected static int red = 255; protected static int green = 255; protected static int blue = 0; protected static int alpha = 255;
	protected static Color customColor = new Color(red, green, blue, alpha);
	protected static final Color[] colors = {Color.RED, Color.GREEN, Color.BLUE, Color.MAGENTA, Color.CYAN, Color.YELLOW, Color.GRAY, Color.WHITE, Color.BLACK, customColor};
	protected static final String[] colorStrings = {"RED", "GREEN", "BLUE", "MAGENTA", "CYAN", "YELLOW", "GRAY", "WHITE", "BLACK", "custom"};
	protected static String[] channelStrings = {"C1"};
	
	// ROI paramters
	protected static final String [] roiModes = {"smart", "conventional", "sample"};
	protected static int autoROIMode = 0;
	protected static int autoROIChannel = 1;
	protected static int autoROIMethod = 9;
	protected static int autoROIColorIndex = 5;
	protected static Color autoROIColor = colors[autoROIColorIndex];
	protected static boolean autoROItoManager;
	//protected static int[] channelColors = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9}; // default color setting: cyan, green, red	
	
	protected static final String[] spotShapes = {"ring", "circle", "point"};
	
	// LoG spot properties
	protected static int logShape = 2;
	protected static int logColorIndex = 0;
	protected static Color logColor = colors[logColorIndex];
	protected static int logAlpha = 255;
	// Max spot properties
	protected static int maxShape = 2;
	protected static int maxColorIndex = 2;
	protected static Color maxColor = colors[maxColorIndex];
	protected static int maxAlpha = 255;
	// joint spot properties
	protected static int jointShape = 2;
	protected static int jointColorIndex = 3;
	protected static Color jointColor = colors[jointColorIndex];
	protected static int jointAlpha = 255;
	
	//protected static double spotAlpha = 1.0;
	
	
	// spot detection parameters
	protected static int targetChannel = 3;
	protected static double spotRadius = 0.5d; // radius in calibration unit (e.g.: micron)
	protected static double qualityThreshold = 300;
	protected static double maxTolerance = 250;
	protected static double minDistance = spotRadius*2; // default mininum distance of joint spot is 1 diameter
	
	
	//protected static SpotCollection tmSpots;
	
	// spot display parameters
	protected static Overlay logSpotOverlay;
	protected static Overlay maxSpotOverlay;
	protected static Overlay jointSpotOverlay;
	protected static boolean showLog = false;
	protected static boolean showMax = false;
	protected static boolean showJoint = false;
		
	// spot result
	protected static int nLogSpot;
	protected static int nMaxSpot;
	protected static int nJointSpot;
	
	protected static JPanel detectionPanel;
	
	
	
	protected static void loadParameters () {
		// update number of channels based on the current source image
		int numC = 1;
		if (sourceImage!=null) numC = sourceImage.getNChannels();
		channelStrings = getChannelStrings(numC);
		targetChannel = Math.max(targetChannel, numC);
		// make use of scijava parameter persistence storage
		DefaultPrefService prefs = new DefaultPrefService();
		targetChannel = prefs.getInt(Integer.class, "NiekSpotCounter-targetChannel", targetChannel);
		spotRadius = prefs.getDouble(Double.class, "NiekSpotCounter-spotRadius", spotRadius);
		qualityThreshold = prefs.getDouble(Double.class, "NiekSpotCounter-qualityThreshold", qualityThreshold);
		maxTolerance = prefs.getDouble(Double.class, "NiekSpotCounter-maxTolerance", maxTolerance);
		minDistance = prefs.getDouble(Double.class, "NiekSpotCounter-minDistance", minDistance);
		
		autoROIMode = prefs.getInt(Integer.class, "NiekSpotCounter-autoROIMode", autoROIMode);
		autoROIChannel = prefs.getInt(Integer.class, "NiekSpotCounter-autoROIChannel", autoROIChannel);
		autoROIMethod = prefs.getInt(Integer.class, "NiekSpotCounter-autoROIMethod", autoROIMethod);
		autoROIColorIndex = prefs.getInt(Integer.class, "NiekSpotCounter-autoROIColor", autoROIColorIndex);
		
			//for (int c=0; c<numC; c++) {
			//	channelColors[c] = prefs.getInt(Integer.class, "NiekSpotCounter-channelColors"+c, channelColors[c]);
			//}
		
		logShape = prefs.getInt(Integer.class, "NiekSpotCounter-logShape", logShape);
		logColorIndex = prefs.getInt(Integer.class, "NiekSpotCounter-logColorIndex", logColorIndex);
		logAlpha = prefs.getInt(Integer.class, "NiekSpotCounter-logAlpha", logAlpha);
		
		maxShape = prefs.getInt(Integer.class, "NiekSpotCounter-maxShape", maxShape);
		maxColorIndex = prefs.getInt(Integer.class, "NiekSpotCounter-maxColorIndex", maxColorIndex);
		maxAlpha = prefs.getInt(Integer.class, "NiekSpotCounter-maxAlpha", maxAlpha);
		
		jointShape = prefs.getInt(Integer.class, "NiekSpotCounter-jointShape", jointShape);
		jointColorIndex = prefs.getInt(Integer.class, "NiekSpotCounter-jointColorIndex", jointColorIndex);
		jointAlpha = prefs.getInt(Integer.class, "NiekSpotCounter-jointAlpha", jointAlpha);
		
		if (autoROIColorIndex!=9) autoROIColor = colors[autoROIColorIndex];
		if (logColorIndex!=9) logColor = new Color(colors[logColorIndex].getRed(), colors[logColorIndex].getGreen(), colors[logColorIndex].getBlue(), logAlpha);
		if (maxColorIndex!=9) maxColor = new Color(colors[maxColorIndex].getRed(), colors[maxColorIndex].getGreen(), colors[maxColorIndex].getBlue(), maxAlpha);
		if (jointColorIndex!=9) jointColor = new Color(colors[jointColorIndex].getRed(), colors[jointColorIndex].getGreen(), colors[jointColorIndex].getBlue(), jointAlpha);
		
	}
	protected static void saveParameters () {
		// update number of channels based on the current source image
		int numC = 1;
		if (sourceImage!=null) numC = sourceImage.getNChannels();
		// make use of scijava parameter persistence storage
		DefaultPrefService prefs = new DefaultPrefService();
		prefs.put(Integer.class, "NiekSpotCounter-targetChannel", targetChannel);
		prefs.put(Double.class, "NiekSpotCounter-spotRadius", spotRadius);
		prefs.put(Double.class, "NiekSpotCounter-qualityThreshold", qualityThreshold);
		prefs.put(Double.class, "NiekSpotCounter-maxTolerance", maxTolerance);
		prefs.put(Double.class, "NiekSpotCounter-minDistance", minDistance);
		
		prefs.put(Integer.class, "NiekSpotCounter-autoROIMode", autoROIMode);
		prefs.put(Integer.class, "NiekSpotCounter-autoROIChannel", autoROIChannel);
		prefs.put(Integer.class, "NiekSpotCounter-autoROIMethod", autoROIMethod);
		prefs.put(Integer.class, "NiekSpotCounter-autoROIColor", autoROIColorIndex);
		
			//for (int c=0; c<numC; c++) {
			//	prefs.put(Integer.class, "NiekSpotCounter-channelColors"+c, channelColors[c]);
			//}
		
		prefs.put(Integer.class, "NiekSpotCounter-logShape", logShape);
		prefs.put(Integer.class, "NiekSpotCounter-logColorIndex", logColorIndex);
		prefs.put(Integer.class, "NiekSpotCounter-logAlpha", logAlpha);
		
		prefs.put(Integer.class, "NiekSpotCounter-maxShape", maxShape);
		prefs.put(Integer.class, "NiekSpotCounter-maxColorIndex", maxColorIndex);
		prefs.put(Integer.class, "NiekSpotCounter-maxAlpha", maxAlpha);
		
		prefs.put(Integer.class, "NiekSpotCounter-jointShape", jointShape);
		prefs.put(Integer.class, "NiekSpotCounter-jointColorIndex", jointColorIndex);
		prefs.put(Integer.class, "NiekSpotCounter-jointAlpha", jointAlpha);
	}
	
	protected static void setSource(ImagePlus imp) {
		if (imp!=null) sourceImage = imp;
	}
	protected static ImagePlus getSource() {
		return sourceImage;
	}
	protected static String[] getChannelStrings (int nChannels) {
		String[] channelStrings = new String[nChannels];
		for (int c=0; c<nChannels; c++) {
			channelStrings[c] = "C" + (c+1);
		}
		return channelStrings;
	}
	protected static void setChannel(int channel) {
		if (sourceImage==null) return;
		if (channel>0 && channel<=sourceImage.getNChannels()) targetChannel = channel;
	}
	protected static void setRadius (double radius) {
		spotRadius = radius;
	}
	protected static void setQualityThreshold(double threshold) {
		qualityThreshold = threshold;
	}
	protected static void setMaxTolerance(double tolerance) {
		maxTolerance = tolerance;
	}
	protected static void setMinDistance(double distance) {
		minDistance = distance;
	}
	protected static void refreshSource () {
		setSource(WindowManager.getCurrentImage());
		//displaySpots();
		//update GUI accordingly
		ResultPanel.imageInfo = InfoUtility.getImageInfo(sourceImage);
		ResultPanel.updateInfo();
	}
	
		/*
		protected static void prepareSource () { // change source image channel color and 
			if (sourceImage == null || channelColors.length==0) return;
			if (!sourceImage.isComposite()) {
				sourceImage.setLut(LUT.createLutFromColor(colors[channelColors[0]]));
			} else {
				int numC = sourceImage.getNChannels();
				int numColor = channelColors.length;
				for (int c=0; c<numC; c++) {
					if (c < numColor)
						((CompositeImage)sourceImage).setChannelLut(LUT.createLutFromColor(colors[channelColors[c]]), c+1);
				}
				((CompositeImage)sourceImage).updateAndDraw();
			}
		}
		*/
	
	protected static void showHideOverlay () {
		if (sourceImage == null) return;
		sourceImage.setHideOverlay(!sourceImage.getHideOverlay());
	}
	public static void configSourceDisplay() {
		// load saved parameters
		loadParameters();
		// update number of channels based on the current source image
		int numC = 1;
		if (sourceImage!=null) numC = sourceImage.getNChannels();
		channelStrings = getChannelStrings(numC);
		// create setup dialog
		GenericDialog gd = new GenericDialog("Setup");
		// auto ROI setup
		gd.addMessage("ROI");
		gd.addChoice("mode", roiModes, roiModes[autoROIMode]);
		gd.addChoice("channel", channelStrings, channelStrings[Math.min(numC, autoROIChannel)-1]);
		gd.addToSameRow();
		gd.addChoice("method", autoMethods, autoMethods[autoROIMethod]);
		gd.addToSameRow();
		gd.addChoice("color", colorStrings, colorStrings[autoROIColorIndex]);
		// source image channel color setup
			//gd.addMessage("Image Channel Colors");
			//for (int c=0; c<numC; c++) {
			//	if (c!=0) gd.addToSameRow();
			//	gd.addChoice(channelStrings[c], colorStrings, colorStrings[channelColors[c]]);
			//}
		// spot shape and color setup
		gd.addMessage("Spot Detecion");
		gd.addChoice("LoG shape", spotShapes, spotShapes[logShape]);
		gd.addToSameRow();
		gd.addChoice(" color", colorStrings, colorStrings[logColorIndex]);
		gd.addToSameRow();
		gd.addSlider(" transparency ", 0, 255, 255-logAlpha);
		
		gd.addChoice("Max shape", spotShapes, spotShapes[maxShape]);
		gd.addToSameRow();
		gd.addChoice(" color", colorStrings, colorStrings[maxColorIndex]);
		gd.addToSameRow();
		gd.addSlider(" transparency ", 0, 255, 255-maxAlpha);
		
		gd.addChoice("Joint shape", spotShapes, spotShapes[jointShape]);
		gd.addToSameRow();
		gd.addChoice(" color", colorStrings, colorStrings[jointColorIndex]);
		gd.addToSameRow();
		gd.addSlider(" transparency ", 0, 255, 255-jointAlpha);
		
		//gd.addNumericField("transparency", 100*(1-spotAlpha), 0, 3, "%");
		// show dialog
		gd.showDialog();
		if (gd.wasCanceled()) return;
		
		// get parameters
		boolean roiChanged = false;
		int newROIMode = gd.getNextChoiceIndex();
		int newROIChannel = gd.getNextChoiceIndex()+1;
		int newROIMethod = gd.getNextChoiceIndex();
		int newROIColorIndex = gd.getNextChoiceIndex();
		if (autoROIMode != newROIMode) { autoROIMode = newROIMode; roiChanged=true; }
		if (autoROIChannel != newROIChannel) { autoROIChannel = newROIChannel; roiChanged=true; }
		if (autoROIMethod != newROIMethod) { autoROIMethod = newROIMethod; roiChanged=true; }
		if (autoROIColorIndex != newROIColorIndex) { autoROIColorIndex = newROIColorIndex; roiChanged=true; }
		
			//for (int c=0; c<numC; c++) {
			//	channelColors[c] = gd.getNextChoiceIndex();		
			//}
		
		boolean logChanged = false;
		int newShape = gd.getNextChoiceIndex();
		int newIndex = gd.getNextChoiceIndex();
		int newAlpha = (int) (255 - gd.getNextNumber());
		if (logShape != newShape) { logShape = newShape; logChanged=true; }
		if (logColorIndex != newIndex) { logColorIndex = newIndex; logChanged=true; }
		if (logAlpha != newAlpha) { logAlpha = newAlpha; logChanged=true; }
		
		boolean maxChanged = false;
		newShape = gd.getNextChoiceIndex();
		newIndex = gd.getNextChoiceIndex();
		newAlpha = (int) (255 - gd.getNextNumber());
		if (maxShape != newShape) { maxShape = newShape; maxChanged=true; }
		if (maxColorIndex != newIndex) { maxColorIndex = newIndex; maxChanged=true; }
		if (maxAlpha != newAlpha) { maxAlpha = newAlpha; maxChanged=true; }
		
		boolean jointChanged = (logChanged || maxChanged);
		newShape = gd.getNextChoiceIndex();
		newIndex = gd.getNextChoiceIndex();
		newAlpha = (int) (255 - gd.getNextNumber());
		if (jointShape != newShape) { jointShape = newShape; jointChanged=true; }
		if (jointColorIndex != newIndex) { jointColorIndex = newIndex; jointChanged=true; }
		if (jointAlpha != newAlpha) { jointAlpha = newAlpha; jointChanged=true; }
		
		if (autoROIColorIndex==9) autoROIColor = getCustomColor("ROI");
		else autoROIColor = colors[autoROIColorIndex];
		
		if (logColorIndex==9) logColor = getCustomColor("LoG Spot");
		else logColor = new Color(colors[logColorIndex].getRed(), colors[logColorIndex].getGreen(), colors[logColorIndex].getBlue(), logAlpha);
		
		if (maxColorIndex==9) maxColor = getCustomColor("Max Spot");
		else maxColor = new Color(colors[maxColorIndex].getRed(), colors[maxColorIndex].getGreen(), colors[maxColorIndex].getBlue(), maxAlpha);
		
		if (jointColorIndex==9) jointColor = getCustomColor("Joint Spot");
		else jointColor = new Color(colors[jointColorIndex].getRed(), colors[jointColorIndex].getGreen(), colors[jointColorIndex].getBlue(), jointAlpha);
		
		// save parameters to internal storage
		saveParameters();
		
		//ROIUtility.roi.setStrokeColor(colors[autoROIColor]);
		if (sourceImage!=null) {
			if (ROIUtility.roi!=null && roiChanged) {
				//ROIUtility.autoROI();
			}
			if (logChanged) updateLogSpots(true);
			if (maxChanged) updateMaxSpots(true);
			if (jointChanged) updateJointSpots(true);
		}
	}
	
	// function groups to display spot overlay: LoG, Max, Joint
	protected static void showLoGSpot (boolean visible) {
		showLog = visible;
		showSpots(showLog, showMax, showJoint);
	}
	protected static void showMaxSpot (boolean visible) {
		showMax = visible;
		showSpots(showLog, showMax, showJoint);
	}
	protected static void showJointSpot (boolean visible) {
		showJoint = visible;
		showSpots(showLog, showMax, showJoint);
	}
	protected static void showSpots (boolean showLog, boolean showMax, boolean showJoint) {
		if (sourceImage==null) return;
		Overlay newOverlay = new Overlay();
		if (showLog && logSpotOverlay!=null) {
			for (Roi roi : logSpotOverlay.toArray()) {
				newOverlay.add(roi);
			}
		}
		if (showMax && maxSpotOverlay!=null) {
			for (Roi roi : maxSpotOverlay.toArray()) {
				newOverlay.add(roi);
			}
		}
		if (showJoint && jointSpotOverlay!=null) {
			for (Roi roi : jointSpotOverlay.toArray()) {
				newOverlay.add(roi);
			}
		}
		sourceImage.setOverlay(newOverlay);
	}
	// function groups to modify spot and spot overlay: LoG, Max, Joint
	protected static void updateLogSpots (boolean display) {
		if (sourceImage==null) return;
		Overlay overlay = getLogSpot (sourceImage, targetChannel, spotRadius, qualityThreshold);
		nLogSpot = overlay==null ? 0 : overlay.size();
		ResultPanel.spotInfo = InfoUtility.getSpotInfo();
		ResultPanel.updateInfo();
		if (logShape==2) // change to point
			overlay = ovalToPointOverlay(overlay, logColor);
		logSpotOverlay = overlay;
		if (display) showSpots(showLog, showMax, showJoint);
	}
	protected static void updateMaxSpots (boolean display) {
		if (sourceImage==null) return;
		PointRoi pointRoi = getMaxSpots (sourceImage, targetChannel, maxTolerance);		
		nMaxSpot = pointRoi==null ? 0 : pointRoi.size();
		ResultPanel.spotInfo = InfoUtility.getSpotInfo();
		ResultPanel.updateInfo();
		Overlay overlay = new Overlay(pointRoi);
		if (maxShape!=2) // ring / circle
			overlay = pointToOvalOverlay(overlay, maxShape==0, maxColor);
		maxSpotOverlay = overlay;
		if (display) showSpots(showLog, showMax, showJoint);
	}
	protected static void updateJointSpots (boolean display) {
		if (sourceImage==null) return;
		//Overlay overlay1 = getLogSpot (sourceImage, targetChannel, spotRadius, qualityThreshold);
		//Overlay overlay2 = new Overlay(getMaxSpots (sourceImage, targetChannel, maxTolerance));
		//jointSpotOverlay = getJointSpots (sourceImage, overlay1, overlay2, minDistance);
		updateLogSpots(false); updateMaxSpots(false);
		if (logSpotOverlay==null || maxSpotOverlay==null) return;
		jointSpotOverlay = getJointSpots (sourceImage, logSpotOverlay, maxSpotOverlay, minDistance);
		ResultPanel.spotInfo = InfoUtility.getSpotInfo();
		ResultPanel.updateInfo();
		if (display) showSpots(showLog, showMax, showJoint);
	}
	
	// function to get LoG spot from TrackMate, as overlay spots
	protected static Overlay getLogSpot (ImagePlus imp, int channel, double radius, double threshold) {
		return getLogSpot (imp, channel, radius, threshold, logShape, logColor);
	}
	protected static Overlay getLogSpot (ImagePlus imp, int channel, double radius, double threshold, int shape, Color color) {
		if (imp==null) return null;
		Roi r = imp.getRoi();
		if (r!=null) {
			Roi newRoi = ROIUtility.trimBorder(r, imp, 1);
			imp.setRoi(newRoi);
		}
		// configure the Trackmate detector settings
		Settings tmSettings = new Settings();
		tmSettings.detectorFactory = new LogDetectorFactory();
		Map<String, Object> map = tmSettings.detectorFactory.getDefaultSettings();
		map.put("RADIUS", radius);
		map.put("DO_MEDIAN_FILTERING", false);
		map.put("DO_SUBPIXEL_LOCALIZATION", true);
		map.put("TARGET_CHANNEL", channel);
		map.put("THRESHOLD", threshold);
		tmSettings.detectorSettings = map;
		tmSettings.setFrom(imp);
		// detect spots using Trackmate
		Model model = new Model();
		TrackMate trackmate = new TrackMate(model, tmSettings);
		// Check input, Find spots
		boolean ok = trackmate.checkInput();
		ok = trackmate.execDetection();
		if (ok == false) System.out.println(trackmate.getErrorMessage());
		else saveParameters();
		SpotCollection tmSpots = model.getSpots();
		return tmSpotToOverlay(imp.getCalibration().pixelWidth, tmSpots, shape==0, color);
	}
	// function(s) to get local maxima as overlay spots
	
	protected static Overlay getMaxOverlay (ImagePlus imp, int channel, double tolerance, Color color) {
		PointRoi pointRoi = getMaxSpots (imp, channel, tolerance, color);		
		Overlay overlay = new Overlay(pointRoi);
		return overlay;
	}
	protected static PointRoi getMaxSpots (ImagePlus imp, int channel, double tolerance) {
		return getMaxSpots (imp, channel, tolerance, maxColor);
	}
	protected static PointRoi getMaxSpots (ImagePlus imp, int channel, double tolerance, Color color) {
		return getMaxSpots (imp, channel, tolerance, true, color);
	}
	protected static PointRoi getMaxSpots (ImagePlus imp, int channel, double tolerance, boolean excludeOnEdges, Color color) {
		if (imp==null) return null;
		if (channel > imp.getNChannels()) channel = 1;
		int posC = imp.getC();
		imp.setC(channel);
		Roi roi = imp.getRoi();
		//System.out.println("debug getMaxPoints, imp roi: " + (imp.getRoi()!=null));
		Polygon polygon = new MaximumFinder().getMaxima(imp.getProcessor(), tolerance, excludeOnEdges);
		imp.setC(posC);
		if (polygon==null || polygon.npoints==0) return null;
		int nPoints = polygon.npoints;
		PointRoi pointRoi = new PointRoi();
		for (int i=0; i<nPoints; i++) {
			pointRoi.addPoint(polygon.xpoints[i], polygon.ypoints[i]);
		}
		if (roi!=null && roi.isArea()) pointRoi = pointRoi.containedPoints(roi);
		/*
		for (int i=0; i<nPoints; i++) {
			if (roi!=null && !roi.contains(polygon.xpoints[i], polygon.ypoints[i])) continue;
			pointRoi.addPoint(polygon.xpoints[i], polygon.ypoints[i]);
		}
		*/
		pointRoi.setStrokeColor(color);
		return pointRoi;
	}
	// function to get Joint Spot from LoG and Max spot overlay
	protected static Overlay getJointSpots (ImagePlus imp, double minDist) {
		return getJointSpots (imp, logSpotOverlay, maxSpotOverlay, minDist);
	}
	protected static Overlay getJointSpots (ImagePlus imp, Overlay spotOverlay1, Overlay spotOverlay2, double minDist) {
		return getJointSpots (imp, spotOverlay1, spotOverlay2, minDist, jointShape, jointColor);
	}
	protected static Overlay getJointSpots (ImagePlus imp, Overlay spotOverlay1, Overlay spotOverlay2, double minDist, int shape) {
		return getJointSpots (imp, spotOverlay1, spotOverlay2, minDist, shape, jointColor);
	}
	protected static Overlay getJointSpots (ImagePlus imp, Overlay spotOverlay1, Overlay spotOverlay2, double minDist, int shape, Color color) {
		if (imp==null || spotOverlay1==null || spotOverlay2==null) return null;
		double d = minDist / imp.getCalibration().pixelWidth;
		double d2 = Math.pow(d, 2);
		Overlay jointOverlay = new Overlay();
		Overlay pointOverlay1 = ovalToPointOverlay (spotOverlay1.duplicate(), color);
		Overlay pointOverlay2 = ovalToPointOverlay (spotOverlay2.duplicate(), color);
		if(pointOverlay1==null || pointOverlay2==null) return null;
		PointRoi jointPoint = new PointRoi();
		Roi pointRoi1 = pointOverlay1.toArray()[0];
		Roi pointRoi2 = pointOverlay2.toArray()[0];
		Point[] points1 = ((PointRoi) pointRoi1).getContainedPoints();
		Point[] points2 = ((PointRoi) pointRoi2).getContainedPoints();
		
		//double start = System.currentTimeMillis();
		for (int i=0; i<points1.length; i++) {
			boolean pairFound = false;
			double x1 = points1[i].getX(); double y1 = points1[i].getY();
			double x2 = 0; double y2 = 0;
			double xxyy = Double.MAX_VALUE;
			int pairIdx  = 0;
			for (int j=0; j<points2.length; j++) {
				//if (pairFound) break;
				
				x2 = points2[j].getX(); double xx = 0;
				if (Math.abs(x2-x1) > d) continue;
				else xx = Math.pow(x2-x1, 2);
				
				y2 = points2[j].getY(); double yy = 0;
				if (Math.abs(y2-y1) > d) continue;
				else yy = Math.pow(y2-y1, 2);
				
				if (xx+yy > d2) continue;
				
				pairFound = true;
				if (xx+yy < xxyy) {
					pairIdx = j; xxyy = xx+yy;
				}
				
			}
			if (!pairFound) continue;
			jointPoint.addPoint((x1+points2[pairIdx].getX())/(double)2, (y1+points2[pairIdx].getY())/(double)2);
		}
		//double duration = (System.currentTimeMillis() - start) / 1000;
		//System.out.println("points1: " + points1.length + ", points2: " + points2.length + ", duration: " + duration + " sec");
		//System.out.println("joint compute duration: " + duration + " sec");
		jointPoint.setPosition(0, 0, 0);
		jointPoint.setStrokeColor(color);
		nJointSpot = jointPoint.size();
		jointOverlay.add(jointPoint);
		
		if (shape!=2) 
			jointOverlay = pointToOvalOverlay(jointOverlay, shape==0, color);
		
		return jointOverlay;
		
	}
	
	
	// change TrackMate Spot (in SpotCollection) to oval ROI into overlay
	protected static Overlay tmSpotToOverlay (double pixelSize, SpotCollection tmSpots, boolean ringShape, Color color) {
		if (pixelSize==0 || tmSpots==null || tmSpots.getNSpots(false)==0) return null;
		Iterator<Spot> spotIterator = tmSpots.iterator(false);
		Overlay newOverlay = new Overlay();
		//PointRoi pointRoi = new PointRoi();
		while (spotIterator.hasNext()) {
			Spot spot = spotIterator.next();
			// get spot position in pixel coordinates
			double x = spot.getDoublePosition( 0 );// / pixelSize;
			double y = spot.getDoublePosition( 1 );// / pixelSize;
			double d = spotRadius*2;
			Roi spotRoi = new OvalRoi((x-d/2)/pixelSize,(y-d/2)/pixelSize, d/pixelSize, d/pixelSize);
			spotRoi.setPosition(0, 0, 0);
			spotRoi.setStrokeColor(color);
			if(!ringShape) spotRoi.setFillColor(color);
			newOverlay.add(spotRoi);
		}
		return newOverlay;
	}
	// function group to change shape of spot overlay
	protected static Overlay pointToOvalOverlay (Overlay overlay, boolean ringShape) {
		return pointToOvalOverlay (overlay, ringShape, logColor);
	}
	protected static Overlay pointToOvalOverlay (Overlay overlay, boolean ringShape, Color color) {
		if (overlay==null || overlay.size()==0) return overlay;
		Roi[] rois = overlay.toArray();
 		if (rois.length!=1) {	// check length (multi-point overlay length == 1
 			for (Roi roi : rois) {
 				if (roi.getType() == Roi.OVAL)
 					roi.setStrokeColor(color);
 				else
 					overlay.remove(roi);
 			}
 			return overlay;
 		}
 		
 		Point[] points = ((PointRoi) rois[0]).getContainedPoints();
 		if (points.length==1) return overlay;	// check if single point in the overlay
 		// create new overlay with oval ROIs
 		double r = spotRadius / sourceImage.getCalibration().pixelWidth;
 		
 		Overlay newOverlay = new Overlay();
		for (int i=0; i<points.length; i++) {
			Roi roi = new OvalRoi(points[i].getX()-r, points[i].getY()-r, r*2, r*2);
			roi.setPosition(0, 0, 0);
			roi.setStrokeColor(color);
			if (!ringShape) roi.setFillColor(color);
			newOverlay.add(roi);
		}
 		return newOverlay;
	}
	//protected static Overlay ovalToPointOverlay (Overlay overlay) {
	//	return ovalToPointOverlay (overlay, logColor);
	//}
	protected static Overlay ovalToPointOverlay (Overlay overlay, Color color) {
		if (overlay==null || overlay.size()==0) return null;
		Roi[] rois = overlay.toArray();
		if (rois.length==1) {
			if (rois[0].getType() == Roi.POINT) {
				rois[0].setStrokeColor(color);
				return overlay;
			}
		}
		PointRoi pointRoi = new PointRoi();
		for (int i=0; i<rois.length; i++) {
			if (rois[i].getType() != Roi.OVAL) continue;
			double[] centre = rois[i].getContourCentroid();
			pointRoi.addPoint(centre[0], centre[1]);
		}
		pointRoi.setPosition(0, 0, 0);
		pointRoi.setStrokeColor(color);
		Overlay newOverlay = new Overlay();
		newOverlay.add(pointRoi);
 		return newOverlay;
	}
	// spot overlay to single ROI
	protected static Roi overlayToSingleROI (Overlay spotOverlay) {
		if (spotOverlay==null || spotOverlay.size()==0) return null;
		Roi[] rois = spotOverlay.toArray();
		return ROIUtility.combineRois(rois);
	}
	// function group to get and set Color
	protected static Color getColor (String colorName) { // get Color from name
		for (int i=0; i<colorStrings.length; i++) {
			if (colorStrings[i].equals(colorName.toUpperCase()))
				return colors[i];
		}
		return null;
	}
	protected static String getStringFromColor (Color color) { // get name from Color
		for (int i=0; i<colors.length; i++) {
			if (colors[i].equals(color))
				return colorStrings[i];
		}
		return null;
	}
	protected static int getColorIndex (String colorName) {	// get color index from name
		for (int i=0; i<colorStrings.length; i++) {
			if (colorStrings[i].equals(colorName.toUpperCase()))
				return i;
			if (colorName.equals(colorStrings[9])) return 9;
		}
		return -1;
	}
	protected static Color getCustomColor (String colorComponent) {
		DefaultPrefService prefs = new DefaultPrefService();
		red = prefs.getInt(Integer.class, "NiekSpotCounter-"+colorComponent+"-customRed", red);
		green = prefs.getInt(Integer.class, "NiekSpotCounter-"+colorComponent+"-customGreen", green);
		blue = prefs.getInt(Integer.class, "NiekSpotCounter-"+colorComponent+"-customBlue", blue);
		alpha = prefs.getInt(Integer.class, "NiekSpotCounter-"+colorComponent+"-customAlpha", alpha);
		GenericDialog gd = new GenericDialog(colorComponent + " Custom Color");
		gd.addSlider("Red", 0, 255, red);
		gd.addSlider("Green", 0, 255, green);
		gd.addSlider("Blue", 0, 255, blue);
		gd.addSlider("Transparency", 0, 255, 255-alpha);
		gd.showDialog();
		if (gd.wasCanceled()) return customColor;
		red = (int) gd.getNextNumber();
		green = (int) gd.getNextNumber();
		blue = (int) gd.getNextNumber();
		alpha = 255 - (int) gd.getNextNumber();
		prefs.put(Integer.class, "NiekSpotCounter-"+colorComponent+"-customRed", red);
		prefs.put(Integer.class, "NiekSpotCounter-"+colorComponent+"-customGreen", green);
		prefs.put(Integer.class, "NiekSpotCounter-"+colorComponent+"-customBlue", blue);
		prefs.put(Integer.class, "NiekSpotCounter-"+colorComponent+"-customAlpha", alpha);
		customColor = new Color(red, green, blue, alpha);
		return customColor;
	}
	// get index of certain Thresholding method, -1 if not found
	protected static int getThresholdMethodIndex (String method) {
		for (int i=0; i<autoMethods.length; i++) {
			if (autoMethods[i].equals(method)) {
				return i;
			}
		}
		return -1;
	}
	// get index of spot shape, -1 if not found
	protected static int getShapeIndex (String shape) {
		for (int i=0; i<spotShapes.length; i++) {
			if (spotShapes[i].equals(shape)) {
				return i;
			}
		}
		return -1;
	}
	// get index of ROI method, -1 if not found
	protected static int getROIModeIndex (String mode) {
		for (int i=0; i<roiModes.length; i++) {
			if (roiModes[i].equals(mode)) {
				return i;
			}
		}
		return -1;
	}
}
