package cam.lmb.niek;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import java.util.Date;
import java.util.HashMap;
import java.text.SimpleDateFormat;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;

import org.apache.commons.io.FileUtils;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import ij.IJ;

import org.scijava.prefs.DefaultPrefService;

import fiji.util.gui.GenericDialogPlus;


public class IOUtility {
	
	protected static final String setupFileTitle = "hzqfox-SpotCounterSettings";
	protected static final String batchSetupFileTitle = "hzqfox-BatchSpotCounterSettings";
	protected static final String environmentNodeTitle = "Environment";
	protected static final String imageNodeTitle = "Image";
	protected static final String roiNodeTitle = "CellROI";
	protected static final String detectionNodeTitle = "SpotDetection";
	
	// batch processing parameters
	//protected static String batchFilePath;
	//protected static String ext = ".czi";
	//protected static Boolean doSubDir = false;
	//protected static int minSize = 5000;
	//protected static String batchSetupPath = "         use current setup";
	//protected static boolean doZScore = true;
	//protected static String batchResultDirPath = "             same as input";
	//protected static boolean doAutoROI = false;
	//protected static boolean exportImage = true;
	//protected static boolean exportSpotROI = true;
	//protected static boolean exportSpotTable = true;
	//protected static boolean exportFilterTable = true;
	
    public static void main(String[] args) throws IOException {
        //writeFileUsingJDOM(empList, fileName);
    }
    
    
    /*
	protected static void batchProcessFile() {
		// load stored parameters	
		DefaultPrefService prefs = new DefaultPrefService();
		batchFilePath = prefs.get(String.class, "NiekSpotCounter-batch-batchFilePath", batchFilePath);
		ext = prefs.get(String.class, "NiekSpotCounter-batch-ext", ext);
		minSize = prefs.getInt(Integer.class, "NiekSpotCounter-batch-minSize", minSize);
		doZScore = prefs.getBoolean(Boolean.class, "NiekSpotCounter-batch-doZScore", doZScore);
		doAutoROI = prefs.getBoolean(Boolean.class, "NiekSpotCounter-batch-doAutoROI", doAutoROI);
		exportImage = prefs.getBoolean(Boolean.class, "NiekSpotCounter-batch-exportImage", exportImage);
		exportSpotROI = prefs.getBoolean(Boolean.class, "NiekSpotCounter-batch-exportSpotROI", exportSpotROI);
		exportSpotTable = prefs.getBoolean(Boolean.class, "NiekSpotCounter-batch-exportSpotTable", exportSpotTable);
		exportFilterTable = prefs.getBoolean(Boolean.class, "NiekSpotCounter-batch-exportFilterTable", exportFilterTable);
		
		// create dialog
		GenericDialogPlus gd = new GenericDialogPlus("Batch Process File");
		
		gd.setInsets(5, 100, 5); gd.addMessage("Select Image File or Folder");
		gd.addDirectoryOrFileField("", batchFilePath, 24);		
		
		String[] extensions = {".czi", ".tif"};
		gd.setInsets(5, -5, 5);
		gd.addChoice("file extension", extensions, ext);
		gd.addNumericField("image size >", minSize, 0, 6, "pixel (czi file only)");
		//gd.setInsets(5, 80, 5);		gd.addCheckbox("check sub-folder", doSubDir);
		
		gd.addDirectoryOrFileField("Setup File", batchSetupPath, 24);
		gd.setInsets(5, -5, 5);
		String[] relativeOrAbsoluteValue = {"use relative filter value", "use absolute filter value"};
		gd.addChoice("", relativeOrAbsoluteValue, relativeOrAbsoluteValue[doZScore?0:1]);
		gd.addDirectoryField("Result Folder", batchResultDirPath, 24);
		
		gd.setInsets(5, 80, 5); 	gd.addCheckbox("create auto ROI", doAutoROI);
		gd.setInsets(5, 80, 5); 	gd.addCheckbox("export image while processing", exportImage);
		gd.setInsets(5, 80, 5);		gd.addCheckbox("save cell detection ROI", exportSpotROI);
		gd.setInsets(5, 80, 5); 	gd.addCheckbox("save detection result", exportSpotTable);
		gd.setInsets(5, 80, 5); 	gd.addCheckbox("save filter result", exportFilterTable);
		
		gd.showDialog();
		if (gd.wasCanceled()) return;
		
		batchFilePath = gd.getNextString();
		ext = gd.getNextChoice();
		minSize = (int) gd.getNextNumber();
		batchSetupPath = gd.getNextString();
		doZScore = (gd.getNextChoiceIndex()==0);
		batchResultDirPath = gd.getNextString();
		doAutoROI = gd.getNextBoolean();
		exportImage = gd.getNextBoolean();
		exportSpotROI = gd.getNextBoolean();
		exportSpotTable = gd.getNextBoolean();
		exportFilterTable = gd.getNextBoolean();
		
		// save parameter to internal storage
		prefs.put(String.class, "NiekSpotCounter-batch-batchFilePath", batchFilePath);
		prefs.put(String.class,  "NiekSpotCounter-batch-ext", ext);
		prefs.put(Integer.class, "NiekSpotCounter-batch-minSize", minSize);
		prefs.put(Boolean.class, "NiekSpotCounter-batch-doZScore", doZScore);
		prefs.put(Boolean.class, "NiekSpotCounter-batch-doAutoROI", doAutoROI);
		prefs.put(Boolean.class, "NiekSpotCounter-batch-exportImage", exportImage);
		prefs.put(Boolean.class, "NiekSpotCounter-batch-exportSpotROI", exportSpotROI);
		prefs.put(Boolean.class, "NiekSpotCounter-batch-exportSpotTable", exportSpotTable);
		prefs.put(Boolean.class, "NiekSpotCounter-batch-exportFilterTable", exportFilterTable);
		
		// check input file exist
		File batchFile = new File(batchFilePath);
		if (!batchFile.exists()) return;
		// get file list
		List<File> fileList = getFileList(batchFile, ext);
		if (fileList==null || fileList.size()==0) return;
		// get result folder, check exist
		File resultDir = null;
		if (batchResultDirPath.equals("             same as input")) {
			resultDir = batchFile.isDirectory() ? batchFile : batchFile.getParentFile();
		} else {
			resultDir = new File(batchResultDirPath);
		}
		if (!resultDir.exists() || !resultDir.isDirectory()) return;
		// check and get setup file, save to result folder
		String setupFilePath = resultDir.getAbsolutePath() + File.separator + "batch_setup.xml";
		if (batchSetupPath.equals("         use current setup")) {
			if (DetectionUtility.sourceImage==null)
				return;
			else
				IOUtility.saveSetupToXmlFile(setupFilePath);
		} else {
			 try {
				FileUtils.copyFile(new File(batchSetupPath), new File(setupFilePath));
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
		}
		// get parameter set
		HashMap<String, Object> parameters = IOUtility.batchLoadSetup(setupFilePath);
		if (parameters==null) return;
		// process file list
		//try {
		//	processFileList(fileList, ext, minSize, parameters, resultDir, doZScore, doAutoROI,
		//			exportImage, exportSpotROI, exportSpotTable, exportFilterTable);
		//} catch (IOException | FormatException | ServiceException e) {
		//	e.printStackTrace();
		//}
	}
	*/
    
    
	protected static List<File> getFileList (File inputFile, String ext) {
		if (!inputFile.exists()) return null;
		// extend file extension
		String ext2 = ext.equals(".tif") ? ".tiff" : ext;
		// inputFile is file
		if (inputFile.isFile()) {
			List<File> list = new ArrayList<File>();
			if (inputFile.getName().endsWith(ext) || inputFile.getName().endsWith(ext2))
					list.add(inputFile);
			return list;
		}
		// inputFile is folder
		File[] files = inputFile.listFiles(new FilenameFilter() {	
			@Override
			public boolean accept(File dir, String name) {
				return (name.endsWith(ext) || name.endsWith(ext2));
			}
		});
		return Arrays.asList(files);
	}
	
    protected static void saveElementToXmlFile (Element element, String fileName) {
    	Document doc = new Document();
        doc.setRootElement(element);
        // Write JDOM document to file
        XMLOutputter xmlOutputter = new XMLOutputter(Format.getPrettyFormat());
        //output xml to console for debugging
        //xmlOutputter.output(doc, System.out);
        try {
        	xmlOutputter.output(doc, new FileOutputStream(fileName));
        } catch (IOException io) {
			System.out.println(io.getMessage());
		}
    }
    
    // environment element: ImageJ version, Java version, OS, MAC, IP, user, date, time
    protected static Element environmentInfoToElement () {
    	Date now = new Date();
    	Element rootElement = new Element(environmentNodeTitle);
    	rootElement.addContent(new Element("ImageJ").setText(getImageJVersion()));
    	rootElement.addContent(new Element("Java").setText(getJavaVersion()));
    	rootElement.addContent(new Element("OS").setText(getOS()));
    	rootElement.addContent(new Element("MAC").setText(getMAC()));
    	rootElement.addContent(new Element("IP").setText(getIP()));
    	rootElement.addContent(new Element("User").setText(getUser()));
    	rootElement.addContent(new Element("Date").setText(getDate(now)));
    	rootElement.addContent(new Element("Time").setText(getTime(now)));
    	return rootElement;
    }
    // image element: title, folder, nChannels
    protected static Element imageInfoToElement () {
    	if (DetectionUtility.sourceImage==null) return null;
    	Element rootElement = new Element(imageNodeTitle);
    	// get source image title and path
    	rootElement.addContent(new Element("title").setText(DetectionUtility.sourceImage.getTitle()));
    	DetectionUtility.sourceImage.setActivated();
    	rootElement.addContent(new Element("folder").setText(IJ.getDirectory("image")));
    	// get channel and color setup
    	rootElement.addContent(new Element("nChannels").setText(""+DetectionUtility.sourceImage.getNChannels()));
    		/*
	    	Element channelColors = new Element("colors");
	        int[] channels = DetectionUtility.channelColors;
	    	for (int i=0; i<channels.length; i++) {
	    		channelColors.addContent( new Element("C"+(i+1)).setText(DetectionUtility.colorStrings[channels[i]]) );
	    	}
	    	rootElement.addContent(channelColors);
	    	*/
    	return rootElement;
    }
    // image element (batch mode): input folder, input file list, nChannels,
    protected static Element fileListInfoToElement (List<File> fileList, HashMap<String, Object> batchSetup) {
    	Element rootElement = new Element(imageNodeTitle);
    	// get batch mode indicator
    	rootElement.addContent(new Element("mode").setText("batch"));
    	rootElement.addContent(new Element("input").setText(""+batchSetup.get("inputPath")));
    	rootElement.addContent(new Element("extension").setText(""+batchSetup.get("extension")));
    	rootElement.addContent(new Element("output").setText(""+batchSetup.get("outputPath")));
    	Element batchFiles = new Element("files");
        int nFile = fileList.size();
    	for (int i=0; i<nFile; i++) {
    		batchFiles.addContent( new Element("File_"+(i+1)).setText(fileList.get(i).getName()) );
    	}
    	rootElement.addContent(batchFiles);
    	return rootElement;
    }
    // ROI element: auto, channel, method, color
    protected static Element ROIInfoToElement () {
    	Element rootElement = new Element(roiNodeTitle);
    	rootElement.addContent(new Element("mode").setText(DetectionUtility.roiModes[DetectionUtility.autoROIMode]));
    	rootElement.addContent(new Element("channel").setText(""+DetectionUtility.autoROIChannel));
    	rootElement.addContent(new Element("method").setText(""+DetectionUtility.autoMethods[DetectionUtility.autoROIMethod]));
    	rootElement.addContent(new Element("color").setText(""+DetectionUtility.colorStrings[DetectionUtility.autoROIColorIndex]));
    	return rootElement;
    }
    
    // detection element: channel, radius, threshold, spot shape, spot color
    protected static Element detectionInfoToElement () {
    	Element rootElement = new Element(detectionNodeTitle);
    	rootElement.addContent(new Element("channel").setText(""+DetectionUtility.targetChannel));
    	rootElement.addContent(new Element("radius").setText(""+DetectionUtility.spotRadius));
    	rootElement.addContent(new Element("threshold").setText(""+DetectionUtility.qualityThreshold));
    	rootElement.addContent(new Element("tolerance").setText(""+DetectionUtility.maxTolerance));
    	rootElement.addContent(new Element("minDistance").setText(""+DetectionUtility.minDistance));
    	
    	rootElement.addContent(new Element("logShape").setText(""+DetectionUtility.spotShapes[DetectionUtility.logShape]));
    	rootElement.addContent(new Element("logColor").setText(""+DetectionUtility.colorStrings[DetectionUtility.logColorIndex]));
    	rootElement.addContent(new Element("logAlpha").setText(""+DetectionUtility.logAlpha));
    	rootElement.addContent(new Element("logVisible").setText(""+DetectionUtility.showLog));
    	
    	rootElement.addContent(new Element("maxShape").setText(""+DetectionUtility.spotShapes[DetectionUtility.maxShape]));
    	rootElement.addContent(new Element("maxColor").setText(""+DetectionUtility.colorStrings[DetectionUtility.maxColorIndex]));
    	rootElement.addContent(new Element("maxAlpha").setText(""+DetectionUtility.maxAlpha));
    	rootElement.addContent(new Element("maxVisible").setText(""+DetectionUtility.showMax));
    	
    	rootElement.addContent(new Element("jointShape").setText(""+DetectionUtility.spotShapes[DetectionUtility.jointShape]));
    	rootElement.addContent(new Element("jointColor").setText(""+DetectionUtility.colorStrings[DetectionUtility.jointColorIndex]));
    	rootElement.addContent(new Element("jointAlpha").setText(""+DetectionUtility.jointAlpha));
    	rootElement.addContent(new Element("jointVisible").setText(""+DetectionUtility.showJoint));
    	
    	return rootElement;
    }
    


  
	
    protected static void saveSetupToXmlFile (String fileName) {
    	if (DetectionUtility.sourceImage==null) return;
    	Element rootElement = new Element(setupFileTitle);
    	rootElement.addContent(environmentInfoToElement());
    	rootElement.addContent(imageInfoToElement());
    	rootElement.addContent(ROIInfoToElement());
    	rootElement.addContent(detectionInfoToElement());
        saveElementToXmlFile(rootElement, fileName);
    }
	
    protected static void loadSetupFromXmlFile(String setupFilePath, boolean applySetup) {
    	if (DetectionUtility.sourceImage==null) return;
    	File xmlFile = new File(setupFilePath);
		if (!xmlFile.exists()) return;
		
		SAXBuilder builder = new SAXBuilder();
		try {
			Document document = (Document) builder.build(xmlFile);
			// load image setup
			Element imageNode = document.getRootElement().getChild(imageNodeTitle);
			int nChannels = Integer.valueOf(imageNode.getChildText("nChannels"));
			DetectionPanel.channelSlider.setMaximum(nChannels);
			//int[] channelColors = new int[10];
				/*
				Element colors = imageNode.getChild("colors");
				for (int i=0; i<10; i++) {
					DetectionUtility.channelColors[i] = DetectionUtility.getColorIndex(colors.getChildText("C"+(i+1)));
				}
				*/
			// load ROI setup
			Element roiNode = document.getRootElement().getChild(roiNodeTitle);
			DetectionUtility.autoROIMode = DetectionUtility.getROIModeIndex(roiNode.getChildText("mode"));
			DetectionUtility.autoROIChannel = Integer.valueOf(roiNode.getChildText("channel"));
			DetectionUtility.autoROIMethod = DetectionUtility.getThresholdMethodIndex(roiNode.getChildText("method"));
			DetectionUtility.autoROIColor = DetectionUtility.getColor(roiNode.getChildText("color"));
			// load spot detectin setup
			Element detectionNode = document.getRootElement().getChild(detectionNodeTitle);
			DetectionUtility.targetChannel = Integer.valueOf(detectionNode.getChildText("channel"));
			DetectionUtility.spotRadius = Double.valueOf(detectionNode.getChildText("radius"));
			DetectionUtility.qualityThreshold = Double.valueOf(detectionNode.getChildText("threshold"));
			DetectionUtility.maxTolerance = Double.valueOf(detectionNode.getChildText("tolerance"));
			DetectionUtility.minDistance = Double.valueOf(detectionNode.getChildText("minDistance"));
			// update detection panel GUI components
			DetectionPanel.channelSlider.setValue(DetectionUtility.targetChannel);
			DetectionPanel.diameterSpinner.setValue(2*DetectionUtility.spotRadius);
			DetectionPanel.qualityThresholdSpinner.setValue(DetectionUtility.qualityThreshold);
			DetectionPanel.maxToleranceSpinner.setValue(DetectionUtility.maxTolerance);
			DetectionPanel.minDistanceSpinner.setValue(DetectionUtility.minDistance);
			// load spot properties
			DetectionUtility.logShape = DetectionUtility.getShapeIndex(detectionNode.getChildText("logShape"));
			DetectionUtility.logColor = DetectionUtility.getColor(detectionNode.getChildText("logColor"));
			DetectionUtility.logAlpha = Integer.valueOf(detectionNode.getChildText("logAlpha"));
			DetectionUtility.showLog = Boolean.valueOf(detectionNode.getChildText("logVisible"));
			DetectionUtility.maxShape = DetectionUtility.getShapeIndex(detectionNode.getChildText("maxShape"));
			DetectionUtility.maxColor = DetectionUtility.getColor(detectionNode.getChildText("maxColor"));
			DetectionUtility.maxAlpha = Integer.valueOf(detectionNode.getChildText("maxAlpha"));
			DetectionUtility.showMax = Boolean.valueOf(detectionNode.getChildText("maxVisible"));
			DetectionUtility.jointShape = DetectionUtility.getShapeIndex(detectionNode.getChildText("jointShape"));
			DetectionUtility.jointColor = DetectionUtility.getColor(detectionNode.getChildText("jointColor"));
			DetectionUtility.jointAlpha = Integer.valueOf(detectionNode.getChildText("jointAlpha"));
			DetectionUtility.showJoint = Boolean.valueOf(detectionNode.getChildText("jointVisible"));
			// update spot GUI components
			DetectionPanel.boxShowLog.setSelected(DetectionUtility.showLog);
			DetectionPanel.boxShowMax.setSelected(DetectionUtility.showMax);
			DetectionPanel.boxShowJoint.setSelected(DetectionUtility.showJoint);
			// apply setup if needed
			if (applySetup) {
				applySetup();
			}
		} catch (IOException io) {
			System.out.println(io.getMessage());
		} catch (JDOMException jdomex) {
			System.out.println(jdomex.getMessage());
		}
	}
	
    
    protected static void applySetup () {
    	if (DetectionUtility.autoROIMode==0) {
    		
    		if (DetectionUtility.sourceImage.getRawStatistics().area > 1e5) { 
    			System.out.println("File too large for single thread Star Dist. Apply setup abort."); 
    			return;
    		}
    		
    		Thread thread = new Thread(){ // need optimization, single thread operation on StarDist
    			public void run(){
    				System.out.println("StarDist Thread Running");
    				IJ.run("Smart ROI", "");
    				DetectionUtility.sourceImage.setRoi(ROIUtility.roi);
    		    	// get LoG, Max, and joint spots
    		    	DetectionUtility.updateLogSpots(false);
    		    	DetectionUtility.updateMaxSpots(false);
    		    	DetectionUtility.updateJointSpots(false);
    		    	DetectionUtility.showSpots(DetectionUtility.showLog, DetectionUtility.showMax, DetectionUtility.showJoint);
    			}
    		};
    		thread.start();
    	} else {
    		DetectionUtility.autoROIMode = 1;
    		ROIUtility.autoROI();
    		DetectionUtility.sourceImage.setRoi(ROIUtility.roi);
        	// get LoG, Max, and joint spots
        	DetectionUtility.updateLogSpots(false);
        	DetectionUtility.updateMaxSpots(false);
        	DetectionUtility.updateJointSpots(false);
        	DetectionUtility.showSpots(DetectionUtility.showLog, DetectionUtility.showMax, DetectionUtility.showJoint);
    	}
    	// update result info
    }
    
    protected static void saveBatchSetup (List<File> fileList, HashMap<String, Object> batchSetup, String fileName) {
    	if (fileList==null || fileList.size()==0 || batchSetup==null) return;
    	Element rootElement = new Element(batchSetupFileTitle);
    	rootElement.addContent(environmentInfoToElement());
    	rootElement.addContent(fileListInfoToElement(fileList, batchSetup));
    	// populate ROI element
    	Element roiElement = new Element(roiNodeTitle);
    	roiElement.addContent(new Element("mode").setText("StarDist"));
    	roiElement.addContent(new Element("channel").setText(""+batchSetup.get("roiChannel")));
    	roiElement.addContent(new Element("color").setText(""+DetectionUtility.getStringFromColor((Color) batchSetup.get("roiColor"))));
    	rootElement.addContent(roiElement);
    	// populate spot detection element
    	Element spotElement = new Element(detectionNodeTitle);
    	spotElement.addContent(new Element("channel").setText(""+batchSetup.get("spotChannel")));
    	spotElement.addContent(new Element("radius").setText(""+batchSetup.get("logRadius")));
    	spotElement.addContent(new Element("threshold").setText(""+batchSetup.get("logThreshold")));
    	spotElement.addContent(new Element("tolerance").setText(""+batchSetup.get("maxTolerance")));
    	spotElement.addContent(new Element("minDistance").setText(""+batchSetup.get("minDistance")));
    	spotElement.addContent(new Element("logColor").setText(""+DetectionUtility.getStringFromColor((Color) batchSetup.get("logColor"))));
    	spotElement.addContent(new Element("maxColor").setText(""+DetectionUtility.getStringFromColor((Color) batchSetup.get("maxColor"))));
    	spotElement.addContent(new Element("jointColor").setText(""+DetectionUtility.getStringFromColor((Color) batchSetup.get("jointColor"))));
    	rootElement.addContent(spotElement);
    	// save element to xml file
        saveElementToXmlFile(rootElement, fileName);
    }
    
    protected static HashMap<String, Object> loadBatchSetup (String setupFilePath) {
    	File xmlFile = new File(setupFilePath);
		if (!xmlFile.exists()) return null;
		
		HashMap<String, Object> parameters = new HashMap<String, Object>();
		
		SAXBuilder builder = new SAXBuilder();
		try {
			Document document = (Document) builder.build(xmlFile);
				// load image setup
				/*
				Element imageNode = document.getRootElement().getChild(imageNodeTitle);
				int nChannels = Integer.valueOf(imageNode.getChildText("nChannels"));
				parameters.put("nChannels", nChannels);
				
				Element colors = imageNode.getChild("colors");
				int[] channelColors = new int[10];
				for (int i=0; i<10; i++) {
					channelColors[i] = DetectionUtility.getColorIndex(colors.getChildText("C"+(i+1)));
				}
				parameters.put("channelColors", channelColors);
				*/
			// load ROI setup
			Element roiNode = document.getRootElement().getChild(roiNodeTitle);
			int roiChannel = Integer.valueOf(roiNode.getChildText("channel"));
			//int roiMethod = DetectionUtility.getThresholdMethodIndex(roiNode.getChildText("method"));
			Color roiColor = DetectionUtility.getColor(roiNode.getChildText("color"));
			parameters.put("roiChannel", roiChannel);
			//parameters.put("roiMethod", roiMethod);
			parameters.put("roiColor", roiColor);
			// load spot detection setup
			Element detectionNode = document.getRootElement().getChild(detectionNodeTitle);
			int spotChannel = Integer.valueOf(detectionNode.getChildText("channel"));
			double logRadius = Double.valueOf(detectionNode.getChildText("radius"));
			double logThreshold = Double.valueOf(detectionNode.getChildText("threshold"));
			double maxTolerance = Double.valueOf(detectionNode.getChildText("tolerance"));
			double minDistance = Double.valueOf(detectionNode.getChildText("minDistance"));
			Color logColor = DetectionUtility.getColor(detectionNode.getChildText("logColor"));
			Color maxColor = DetectionUtility.getColor(detectionNode.getChildText("maxColor"));
			Color jointColor = DetectionUtility.getColor(detectionNode.getChildText("jointColor"));
			parameters.put("spotChannel", spotChannel);
			parameters.put("logRadius", logRadius);
			parameters.put("logThreshold", logThreshold);
			parameters.put("maxTolerance", maxTolerance);
			parameters.put("minDistance", minDistance);
			parameters.put("logColor", logColor);
			parameters.put("maxColor", maxColor);
			parameters.put("jointColor", jointColor);	
		} catch (IOException io) {
			System.out.println(io.getMessage());
		} catch (JDOMException jdomex) {
			System.out.println(jdomex.getMessage());
		}
		return parameters;
	}
	
	/*
	 * Function group to get run environment: ImageJ version, Java version, OS, MAC, IP, user, date, time
	 */
    public static String getImageJVersion () {
    	return IJ.getVersion();
    }
    public static String getJavaVersion () {
    	return System.getProperty("java.version");
    }
	public static String getOS () {
		return System.getProperty("os.name");
	}
	public static String getMAC () {
		String macAddress = "SocketException";
		try {
			NetworkInterface network = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());	
			byte[] mac = network.getHardwareAddress();
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < mac.length; i++) {
				sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));		
			}
			macAddress = sb.toString();
		} catch (UnknownHostException | SocketException e) {
			e.printStackTrace();
		}
		return macAddress;
	}
	public static String getIP () {
		String ipAddress = "UnknownHost";
		try {
			ipAddress = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		return ipAddress;
	}
	public static String getUser () {
		return System.getProperty("user.name");
	}
	public static String getDate (Date date) { // 2020-03-28
		return new SimpleDateFormat("yyyy-MM-dd','E").format(date);
	}
	public static String getTime (Date date) { // 18:30:45:2787
		return new SimpleDateFormat("H:mm:ss.SS','zzz").format(date);
	}


	
	
	
}
