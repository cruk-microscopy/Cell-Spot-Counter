package cam.lmb.niek;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;

import org.scijava.prefs.DefaultPrefService;

import fiji.util.gui.GenericDialogPlus;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.Overlay;
import ij.gui.PointRoi;
import ij.gui.Roi;
import ij.measure.ResultsTable;
import ij.plugin.frame.RoiManager;



public class ResultPanel {
	
	protected static ImagePlus sourceImage;
	//protected static Overlay spotOverlay;
	
	protected static final String noImage = "Image not set.";
	
	//protected static JTextArea sourceInfo;
	protected static JTextArea resultInfo;
	
	protected static String imageInfo = "";
	protected static String cellInfo = "";
	protected static String spotInfo = "";

	
	protected static JButton btnShowResult;
	protected static JButton btnSaveResult;
	protected static JButton btnSaveSetup;
	protected static JButton btnLoadSetup;
	protected static JButton btnProcessFile;
	
	protected static String setupDir = System.getProperty("user.home");
	protected static String setupName = "setup.xml";
	protected static String setupFilePath = setupDir + File.separator + "setup.xml";
	protected static boolean applySetup = false;
	
	protected static File resultDir;
	protected static String resultDirPath = "";
	protected static String fileName = "";
	protected static boolean saveCellROI = false;
	protected static boolean saveSpotROI = false;
	protected static boolean saveTable = false;
	protected static String cellROIName = "cell ROI";
	protected static String spotROIName = "spots";
	protected static String tableName = "result";
	
	// GUI parameters
	protected final int lineWidth = 90;
	protected static Color panelColor = new Color(204, 229, 255);
	protected static JPanel resultPanel;
	protected final static Font textFont = new Font("Helvetica", Font.BOLD, 13);
	protected final static Color fontColor = Color.BLACK;
	protected final Color textAreaColor = new Color(204, 229 , 255);
	protected final Font panelTitleFont = new Font("Helvetica", Font.BOLD, 13);
	protected final Color panelTitleColor = Color.BLUE;
	protected final static EmptyBorder border = new EmptyBorder(new Insets(5, 5, 5, 5));
	
	protected final Dimension buttonSize = new Dimension(90, 10);
	
	protected final Dimension numberMax = new Dimension(100, 20);
	
	
	
	// add filter panel to parent panel
	protected static void addResultPanel (JPanel parentPanel) {
		// update panel color
		panelColor = GUIUtility.panelColor;
		
		// result panel for reporting result: add real measurement of mean and stdDev of each channel in the future
		resultPanel = new JPanel();
		resultPanel.setName("resultPanel");
		resultPanel.setLayout(new BoxLayout(resultPanel, BoxLayout.Y_AXIS));
		
		
		resultInfo = new JTextArea(13, 70);
		//spotCount.setMaximumSize(new Dimension(550, 800));
		//spotCount.setMinimumSize(new Dimension(550, 800));
		resultInfo.setEditable(false);
		resultInfo.setAlignmentX(Component.CENTER_ALIGNMENT);
		resultInfo.setFont(textFont);
		resultInfo.setForeground(fontColor);
		resultInfo.setBorder(border);
		resultInfo.setBackground(new Color(255, 255, 204));
		
		resultInfo.setText(noImage);
		
		resultPanel.setBackground(panelColor);
		resultPanel.add(resultInfo);
		//resultPanel.add(buttonPanel);
		addButtonPanel(resultPanel);
		configureResultPanelButtonFunctions();
		resultPanel.setBorder(border);
		//resultPanel.setBackground(panelColor);
		resultPanel.setMaximumSize(GUIUtility.panelMax);
		
		/*
		Timer timer = new Timer(550, new ActionListener() {
	        public void actionPerformed(ActionEvent evt) {
	        	
	        	sourceImage = DetectionUtility.getSource();
	        	spotOverlay = DetectionUtility.spotOverlay;
	        	if (sourceImage==null) {
	        		resultInfo.setText(noImage);
	        	} else {
	        		resultInfo.setText(InfoUtility.getImageInfo(sourceImage)+ "\n" + InfoUtility.getSpotInfo(sourceImage, spotOverlay));
	        	}
	        	resultInfo.repaint();	
	        }
	    });
		timer.start();
		*/
		
    	//updateInfo();
		// add result panel to parent panel
		parentPanel.add(resultPanel);
		parentPanel.validate();
		parentPanel.revalidate();

	}
	
	// create button panel, and add to result panel
	protected static void addButtonPanel (JPanel resultPanel) {
		// add button groups
		JPanel buttonPanel = new JPanel();
		BoxLayout buttonLayout = new BoxLayout(buttonPanel, BoxLayout.X_AXIS);
		buttonPanel.setLayout(buttonLayout);
		
		btnShowResult = new JButton("result table");
		btnSaveResult = new JButton("save result");
		btnSaveSetup = new JButton("save setup");
		btnLoadSetup = new JButton("load setup");
		
		buttonPanel.add(btnShowResult);
		buttonPanel.add(btnSaveResult);
		buttonPanel.add(btnSaveSetup);
		buttonPanel.add(btnLoadSetup);
		//btnShowTable.setPreferredSize(buttonSize);
		// add 4 buttons horizontally aligned: result table, save result, save setup, load setup
		//buttonLayout.linkSize(SwingConstants.HORIZONTAL, btnShowResult, btnSaveResult, btnSaveSetup, btnLoadSetup);
		
		
		
		buttonPanel.setBorder(border);
		buttonPanel.setBackground(panelColor);
		resultPanel.add(buttonPanel);
		buttonPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
	}
	
	// configure button functions of filter panel
	protected static void configureResultPanelButtonFunctions () {
		
		btnShowResult.addActionListener(new ActionListener() {
			@Override 
			public void actionPerformed(ActionEvent ae) { showResultTable(); }
		});
		
		btnSaveResult.addActionListener(new ActionListener() {
			@Override 
			public void actionPerformed(ActionEvent ae) { saveResults(); }
		});
		
		btnSaveSetup.addActionListener(new ActionListener() {
			@Override 
			public void actionPerformed(ActionEvent ae) { saveSetup(); }
		});
		
		btnLoadSetup.addActionListener(new ActionListener() {
			@Override 
			public void actionPerformed(ActionEvent ae) { loadSetup(); }
		});
		
	}
	
	









	protected static void showResultTable() {
		ResultsTable table = getResultsTable();
		table.show("Results");
		//table.show("Cell Fluorescent Spot Count");	
	}

	// function to save sample ROI, cell ROIs, spot ROIs into roiset.zip
	protected static void saveResults() {
		// check source image
		if (DetectionUtility.sourceImage==null) return;
		// get result saving dialog
		if (!resultSaveDialog()) return;
		if (saveCellROI) saveCellROI(resultDir, cellROIName);
		if (saveSpotROI) saveSpotROI(resultDir, spotROIName);
		if (saveTable) {
			ResultsTable table = getResultsTable();
			saveTable(resultDir, tableName, table);
		}
	}
	
	
	protected static void saveSetup() {
		// check source image
		if (DetectionUtility.sourceImage==null) return;
		// create file saving dialog
		DefaultPrefService prefs = new DefaultPrefService();
		setupDir = prefs.get(String.class, "NiekSpotCounter-setupDir", setupDir);
		setupName = prefs.get(String.class, "NiekSpotCounter-setupName", setupName);
		setupFilePath = prefs.get(String.class, "NiekSpotCounter-setupFilePath", setupFilePath);
		
		GenericDialogPlus gd = new GenericDialogPlus("Save Setup to File");
		gd.addDirectoryField("Folder", setupDir);
		gd.addStringField("File Name", setupName, 20);
		gd.showDialog();
		if (gd.wasCanceled()) return;
		setupDir = gd.getNextString();
		setupName = gd.getNextString();
		
		setupFilePath = setupDir + File.separator + setupName;
		if (!setupFilePath.endsWith(".xml")) setupFilePath += ".xml";
		
		prefs.put(String.class, "NiekSpotCounter-setupDir", setupDir);
		prefs.put(String.class, "NiekSpotCounter-setupName", setupName);
		prefs.put(String.class, "NiekSpotCounter-setupFilePath", setupFilePath);

		// save setup to xml file
		IOUtility.saveSetupToXmlFile(setupFilePath);
	}
	
	
	protected static void loadSetup() {
		// set source image, restore spot overlay from TrackMate result
		if (DetectionUtility.sourceImage==null) return;
		// create file loading dialog		
		DefaultPrefService prefs = new DefaultPrefService();
		setupFilePath = prefs.get(String.class, "NiekSpotCounter-setupFilePath", setupFilePath);
		applySetup = prefs.getBoolean(Boolean.class, "NiekSpotCounter-applySetup", applySetup);
		GenericDialogPlus gd = new GenericDialogPlus("Load Setup from File");
		gd.addFileField("File", setupFilePath);
		gd.addCheckbox("apply setup", applySetup);
		gd.showDialog();
		if (gd.wasCanceled()) return;
		setupFilePath = gd.getNextString();
		applySetup = gd.getNextBoolean();
		prefs.put(String.class, "NiekSpotCounter-setupFilePath", setupFilePath);
		prefs.put(Boolean.class, "NiekSpotCounter-applySetup", applySetup);
		// load setup from xml file
		IOUtility.loadSetupFromXmlFile(setupFilePath, applySetup);
		//GUIUtility.updateFrame();
	}
	
	
	// get ResultTable
	protected static ResultsTable getResultsTable () {
		sourceImage = DetectionUtility.sourceImage;
		Overlay logOverlay = DetectionUtility.ovalToPointOverlay(DetectionUtility.logSpotOverlay, DetectionUtility.logColor);
		Overlay maxOverlay = DetectionUtility.ovalToPointOverlay(DetectionUtility.maxSpotOverlay, DetectionUtility.maxColor);
		Overlay jointOverlay = DetectionUtility.ovalToPointOverlay(DetectionUtility.jointSpotOverlay, DetectionUtility.jointColor);
		return getResultsTable(sourceImage, ROIUtility.rois, logOverlay, maxOverlay, jointOverlay);
	}
	protected static ResultsTable getResultsTable (ImagePlus imp, Roi[] cellRois, Overlay logOverlay, Overlay maxOverlay, Overlay jointOverlay) {
		if (imp==null || cellRois==null || cellRois.length==0) return null;
		int numC = imp.getNChannels(); 
		int posZ = imp.getZ(); int posT = imp.getT();
		if (logOverlay==null && maxOverlay==null) return null;
		// extract calibration unit string
		String lengthUnit = "(pixel)"; String areaUnit = "(pixel²)";
		String unit = imp.getCalibration().getUnit().toLowerCase();
		if (unit.equals("micron") || unit.equals("µm") || unit.equals("um")) {
			lengthUnit = "(µm)"; areaUnit = "(µm²)";
		}
		//double pixelSize = sourceImage.getCalibration().pixelWidth;
		// get overlay spot into points
		Point[] logPoint = logOverlay==null ? null : ((PointRoi) logOverlay.get(0)).getContainedPoints();
		Point[] maxPoint = maxOverlay==null ? null : ((PointRoi) maxOverlay.get(0)).getContainedPoints();
		Point[] jointPoint = jointOverlay==null ? null : ((PointRoi) maxOverlay.get(0)).getContainedPoints();
		// populate result table
		int nCell = cellRois.length;
		ResultsTable cellTable = new ResultsTable();
		cellTable.setPrecision(3);
		for (int i=0; i<nCell; i++) {
			imp.setRoi(cellRois[i], false);
			cellTable.incrementCounter();
			cellTable.addValue("Cell ID", i+1);
			cellTable.addValue("X "+lengthUnit, imp.getAllStatistics().xCentroid);
			cellTable.addValue("Y "+lengthUnit, imp.getAllStatistics().yCentroid);
			cellTable.addValue("area "+areaUnit, imp.getStatistics().area);
			
			for (int c=0; c<numC; c++) {
				imp.setPositionWithoutUpdate(c+1, posZ, posT);
				cellTable.addValue("C"+(c+1)+" Mean", imp.getStatistics().mean);
				cellTable.addValue("C"+(c+1)+" StdDev", imp.getStatistics().stdDev);
			}
			
			int nLog = 0; int nMax = 0; int nJoint = 0;
			if (logPoint!=null) {
				for (int l=0; l<logPoint.length; l++) {
					if (cellRois[i].contains(logPoint[l].x, logPoint[l].y))
						nLog++;
				}
			}
			if (maxPoint!=null) {
				for (int m=0; m<maxPoint.length; m++) {
					if (cellRois[i].contains(maxPoint[m].x, maxPoint[m].y))
						nMax++;
				}
			}
			if (jointPoint!=null) {
				for (int j=0; j<jointPoint.length; j++) {
					if (cellRois[i].contains(jointPoint[j].x, jointPoint[j].y))
						nJoint++;
				}
			}
			cellTable.addValue("LoG spot count", nLog);
			cellTable.addValue("Max spot count", nMax);
			cellTable.addValue("joint spot count", nJoint);
		}
		return cellTable;	
	}
	
	
	protected static boolean resultSaveDialog () {
		// load default parameters from internal storage
		DefaultPrefService prefs = new DefaultPrefService();
		resultDirPath = prefs.get(String.class, "NiekSpotCounter-resultDirPath", resultDirPath);
		saveCellROI = prefs.getBoolean(Boolean.class, "NiekSpotCounter-saveCellROI", saveCellROI);
		cellROIName = prefs.get(String.class, "NiekSpotCounter-cellROIName", cellROIName);
		saveSpotROI = prefs.getBoolean(Boolean.class, "NiekSpotCounter-saveSpotROI", saveSpotROI);
		spotROIName = prefs.get(String.class, "NiekSpotCounter-spotROIName", spotROIName);
		saveTable = prefs.getBoolean(Boolean.class, "NiekSpotCounter-saveTable", saveTable);
		tableName = prefs.get(String.class, "NiekSpotCounter-tableName", tableName);
		// create file saving dialog
		GenericDialogPlus gd = new GenericDialogPlus("Save Results");
		// get result folder path
		gd.addDirectoryField("Result Folder", resultDirPath, 55);
		// get current image file name
		if (DetectionUtility.sourceImage!=null) fileName = DetectionUtility.sourceImage.getTitle();
		int dotIdx = fileName.lastIndexOf(".");
		if (dotIdx!=-1) fileName = fileName.substring(0, dotIdx);
		gd.addStringField("File Name", fileName, 55);
		//cell ROI, spots as ROI, result table
		gd.addCheckbox("cell ROI", saveCellROI);
		gd.addToSameRow(); gd.addStringField("", cellROIName, 13);
		gd.addCheckbox("spot ROI", saveSpotROI);
		gd.addToSameRow(); gd.addStringField("", spotROIName, 13);
		gd.addCheckbox("result table", saveTable);
		gd.addToSameRow(); gd.addStringField("", tableName, 13);
		// show dialog
		gd.showDialog();
		if (gd.wasCanceled()) return false;
		// get parameters
		resultDirPath = gd.getNextString();
		fileName = gd.getNextString();
		if (!fileName.endsWith(" ")) fileName += " ";
		resultDir = new File(resultDirPath);
		if (!resultDir.exists() || !resultDir.isDirectory()) return false;
		saveCellROI = gd.getNextBoolean();
		cellROIName = gd.getNextString();
		saveSpotROI = gd.getNextBoolean();
		spotROIName = gd.getNextString();
		saveTable = gd.getNextBoolean();
		tableName = gd.getNextString();
		// save parameters to internal storage
		prefs.put(String.class, "NiekSpotCounter-resultDirPath", resultDirPath);
		prefs.put(Boolean.class, "NiekSpotCounter-saveCellROI", saveCellROI);
		prefs.put(String.class, "NiekSpotCounter-cellROIName", cellROIName);
		prefs.put(Boolean.class, "NiekSpotCounter-saveSpotROI", saveSpotROI);
		prefs.put(String.class, "NiekSpotCounter-spotROIName", spotROIName);
		prefs.put(Boolean.class, "NiekSpotCounter-saveTable", saveTable);
		prefs.put(String.class, "NiekSpotCounter-tableName", tableName);
		return true;
	}
	
	protected static void saveCellROI(File outputDir, String cellROIName) {
		if (!outputDir.exists() || !outputDir.isDirectory()) return;
		if (cellROIName==null) return;
		if (ROIUtility.rois==null || ROIUtility.rois.length==0) return;
		if (!cellROIName.endsWith(".zip")) cellROIName += ".zip";
		Roi[] rois = ROIUtility.rois;
		RoiManager rm = ROIUtility.prepareManager();
		int nROI = rois.length;
		int nDigit = (""+nROI).length();
		for (int i=0; i<nROI; i++) {
			rm.addRoi(rois[i]);
			rm.rename(rm.getCount()-1, "cell " + IJ.pad(i+1, nDigit));
		}
		rm.runCommand("Save", outputDir.getAbsolutePath() + File.separator + fileName + cellROIName);
		ROIUtility.restoreManager();
	}
	
	protected static void saveSpotROI (File outputDir, String spotROIName) {
		if (!outputDir.exists() || !outputDir.isDirectory()) return;
		if (spotROIName==null) return;
		if (!spotROIName.endsWith(".zip")) spotROIName += ".zip";
		if (DetectionUtility.logSpotOverlay==null && DetectionUtility.maxSpotOverlay==null) return;
		RoiManager rm = ROIUtility.prepareManager();
		if (DetectionUtility.logSpotOverlay!=null) {
			rm.addRoi(DetectionUtility.overlayToSingleROI(DetectionUtility.logSpotOverlay));
			rm.rename(rm.getCount()-1, "LoG spots");
		}
		if (DetectionUtility.maxSpotOverlay!=null) {
			rm.addRoi(DetectionUtility.overlayToSingleROI(DetectionUtility.maxSpotOverlay));
			rm.rename(rm.getCount()-1, "Max spots");
		}
		if (DetectionUtility.jointSpotOverlay!=null) {
			rm.addRoi(DetectionUtility.overlayToSingleROI(DetectionUtility.jointSpotOverlay));
			rm.rename(rm.getCount()-1, "joint spots");
		}
		rm.runCommand("Save", outputDir.getAbsolutePath() + File.separator + fileName + spotROIName);
		ROIUtility.restoreManager();
	}
	
	protected static void saveTable (File outputDir, String tableName, ResultsTable table) {
		if (!outputDir.exists() || !outputDir.isDirectory()) return;
		if (tableName==null || table==null) return;
		if (!tableName.endsWith(".csv")) tableName += ".csv";
		table.save(outputDir.getAbsolutePath() + File.separator + fileName + tableName);
	}
	
	protected static void updateInfo () {
		sourceImage = DetectionUtility.getSource();
		if (sourceImage==null) {
    		resultInfo.setText(noImage);
    	} else {
    		resultInfo.setText(imageInfo + "\n" + cellInfo + "\n" + spotInfo);
    	}
		resultInfo.revalidate();
		resultInfo.repaint();
		resultPanel.revalidate();
		resultPanel.repaint();
		//GUIUtility.updateFrame();
	}

}
