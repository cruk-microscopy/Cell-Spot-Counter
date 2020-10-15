package cam.lmb.niek;

import java.awt.Color;
import java.awt.Component;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.plugin.Duplicator;
import ij.plugin.RoiScaler;
import ij.plugin.filter.ThresholdToSelection;
import ij.plugin.frame.RoiManager;
import ij.process.AutoThresholder;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;


public class ROIUtility {

	protected static ImagePlus imp = null;
	protected static Roi roi = null;
	protected static Roi[] rois = null;
	//protected static final String[] methods = {"StarDist", "conventional"};
	//protected static int roiMode = 0;
	
	protected static EmptyBorder border = new EmptyBorder(new Insets(5, 5, 5, 5));
	protected static Color panelColor = new Color(204, 229, 255);
	
	protected static JButton btnAutoRoi;
	protected static JButton btnRoiSave;
	protected static JButton btnRoiHide;
	protected static JButton btnToManager;
	
	// ROI Manager parameters
	protected static boolean rmOpen = false; 
	protected static Roi[] oriROIs = null;
	
	protected static void addROIPanel(JPanel parentPanel) {
		
		//IJ.run("Add Shortcut... ", "shortcut=q command=[Smart ROI]");
		
		panelColor = GUIUtility.panelColor;
		imp = WindowManager.getCurrentImage();

		// create and configure the content panel
		JPanel contentPanel = new JPanel();
		contentPanel.setBorder(border);
		contentPanel.setBackground(panelColor);
		contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.X_AXIS));
		
		//JPanel buttonPanel = new JPanel();
		btnAutoRoi = new JButton("auto ROI");
		btnRoiSave = new JButton("save ROI");
		btnRoiHide = new JButton("show/hide ROI");
		btnToManager = new JButton("to Manager");
		// configure button functions
		configurbuttonFunctions();
				
		contentPanel.add(btnAutoRoi);
		contentPanel.add(btnRoiSave);
		contentPanel.add(btnRoiHide);
		contentPanel.add(btnToManager);
		contentPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
		
		parentPanel.add(contentPanel);
		parentPanel.validate();
		parentPanel.revalidate();

	}
	
	protected static void configurbuttonFunctions () {
		// configure auto ROI button function
		btnAutoRoi.addActionListener(new ActionListener() { 
		  @Override 
		  public void actionPerformed(ActionEvent ae) { autoROI(); }
		});
		// configure save ROI button function
		btnRoiSave.addActionListener(new ActionListener() { 
		  @Override 
		  public void actionPerformed(ActionEvent ae) { saveROI(); }
		});
		// configure show/hide ROI button function
		btnRoiHide.addActionListener(new ActionListener() { 
		  @Override 
		  public void actionPerformed(ActionEvent ae) { showHideROI(); }
		});
		// configure ROI to ROI Manager button function
		btnToManager.addActionListener(new ActionListener() { 
		  @Override 
		  public void actionPerformed(ActionEvent ae) { roiToManager(); }
		});
	}
	
	protected static void autoROI () {
		imp = DetectionUtility.sourceImage;
		if (imp==null) return;
		switch (DetectionUtility.autoROIMode) {
			case 0:	// StarDist ROI
				Thread thread = new Thread(){
	    			public void run(){
	    				System.out.println("StarDist Thread Running");
	    				IJ.run("Smart ROI", ""); imp.setRoi(roi);
	    			}};
	    		thread.start();
				break;
			case 1:	// cell ROI
				roi = getCellROIs(imp, 
					 DetectionUtility.autoROIChannel, 
					 DetectionUtility.autoMethods[DetectionUtility.autoROIMethod], 
					 DetectionUtility.autoROIColor,
					 DetectionUtility.autoROItoManager);
				imp.setRoi(roi);
				break;
			case 2: // sample ROI
				roi = getSampleROI(imp, 
					 DetectionUtility.autoROIChannel, 
					 DetectionUtility.autoMethods[DetectionUtility.autoROIMethod], 
					 DetectionUtility.autoROIColor);
				imp.setRoi(roi);
				break;
		}
	}
	protected static void saveROI () {
		 imp = DetectionUtility.sourceImage;
		 if (imp==null) return;
		 roi = imp.getRoi();
	}
	protected static void showHideROI () {
		 imp = DetectionUtility.sourceImage;
		 if (imp==null) return;
		 if (imp.getRoi()==null) imp.setRoi(roi);
		 else imp.deleteRoi();
	}
	protected static void roiToManager () {
		if (roi==null) return;
		RoiManager rm = RoiManager.getInstance();
		if (rm==null) rm = new RoiManager();
		else {
			boolean hasEntry = rm.getCount()>0;
			boolean refresh = true;
			if (hasEntry)
				refresh = IJ.showMessageWithCancel("ROI Manager has entries!", "Overwrite ROIs in ROI Manager?");
			if (refresh) rm.reset();
			else return;
		}
		
		//if (roi!=null) {
		//	rm.addRoi(roi);
		//	rm.rename(rm.getCount()-1, "sample ROI");
		//}
		
		if (rois!=null) {
			int nROI = rois.length;
			int nDigit = (""+nROI).length();
			for (int i=0; i<rois.length; i++) {
				rm.addRoi(rois[i]);
				rm.rename(rm.getCount()-1, "cell " + IJ.pad(i+1, nDigit));
			}
		}
	}
	
	
	// put multiple ROIs into one ROI
	protected static Roi combineRois (Roi[] rois) {
		if (rois==null) return null;
		if (rois.length==1) return rois[0];
		int idx = 0;
		for (idx=0; idx<rois.length; idx++) {
			if (rois[idx].isArea()) break;
		}
		Roi firstRoi = new ShapeRoi(rois[idx]);
		//System.out.println("debug first area roi idx: " + idx);
		for (int i=idx+1; i<rois.length; i++) {
			if (!rois[i].isArea()) continue;
			firstRoi = ((ShapeRoi) firstRoi).or(new ShapeRoi(rois[i]));
		}
		firstRoi.setPosition(rois[idx].getCPosition(), rois[idx].getZPosition(), rois[idx].getTPosition());
		firstRoi.setStrokeColor(rois[idx].getStrokeColor());
		return firstRoi;
	}
	
	// get cell ROIs, and combine them into a single ROI
	protected static Roi getCellROIs (ImagePlus impOri, int channel, String method, Color roiColor, boolean toManager) {
		int[] dims = impOri.getDimensions();
		if (dims[2]<channel) return null;
		
		int posC = channel;
		int posZ = impOri.getZ();
		int posT = impOri.getT();
		
		Roi oriRoi = impOri.getRoi(); impOri.deleteRoi();
		ImagePlus imp = new Duplicator().run(impOri, posC, posC, posZ, posZ, posT, posT);
		impOri.setRoi(oriRoi);
		
		IJ.run(imp, "8-bit", "");
		imp.getProcessor().setAutoThreshold(method, true, ImageProcessor.NO_LUT_UPDATE);
		ByteProcessor bp = imp.createThresholdMask();
		ImagePlus mask = new ImagePlus("mask", bp);
		IJ.run(mask, "Median...", "radius=2.5");
		IJ.run(mask, "Watershed", "");
		//IJ.run(mask, "Fill Holes", "");
		//mask.getProcessor().setAutoThreshold(AutoThresholder.Method.Otsu, true, ImageProcessor.NO_LUT_UPDATE);
		IJ.run(mask, "Analyze Particles...", "  show=Overlay exclude");
		Overlay overlay = mask.getOverlay();
		if (overlay==null || overlay.size()==0) return null;
		rois = overlay.toArray();
		ResultPanel.cellInfo = InfoUtility.getCellROIInfo(imp);
		ResultPanel.updateInfo();
		// set color of ROI
		for (Roi roi : rois) {
			roi.setPosition(0, 0, 0);
			roi.setStrokeColor(roiColor);	
		}
		// add to ROI Manager if needed
		if (toManager) {
			RoiManager rm = RoiManager.getInstance();
			if (rm==null) rm = new RoiManager();
			else rm.reset();
			
			for (Roi roi : rois) {
				rm.addRoi(roi);;
			}
		}
		// close temp image, release memory
		imp.close(); mask.close();
		System.gc();
		// combine cell ROIs into 1
		roi = combineRois(rois);		
		return roi;
	}
	
	// get sample ROI
	protected static Roi getSampleROI(ImagePlus impOri, int channel, String method, Color roiColor) {
		int[] dims = impOri.getDimensions();
		if (dims[2]<channel) return null;
		
		int width =dims[0]; int height = dims[1];
		boolean resize = true;
		double xScale = Math.ceil((double)width/(double)1000);
		double yScale = Math.ceil((double)height/(double)1000);
		if (xScale==1 && yScale==1) resize = false;
		
		int posC = channel;
		int posZ = impOri.getZ();
		int posT = impOri.getT();

		Roi oriRoi = impOri.getRoi(); impOri.deleteRoi();
		ImagePlus imp = new Duplicator().run(impOri, posC, posC, posZ, posZ, posT, posT);
		impOri.setRoi(oriRoi);
		
		int widthDownSized = width/(int)xScale;
		int heightDownSized = height/(int)yScale;
		
		if (resize)
			IJ.run(imp, "Size...", "width=["+ widthDownSized +"] height=["+ heightDownSized +"] depth=1 average interpolation=Bilinear");
		IJ.run(imp, "8-bit", "");
		imp.getProcessor().setAutoThreshold(method, true, ImageProcessor.NO_LUT_UPDATE);
		ByteProcessor bp = imp.createThresholdMask();
		ImagePlus mask = new ImagePlus("mask", bp);
		IJ.run(mask, "Median...", "radius=10");
		IJ.run(mask, "Fill Holes", "");
		mask.getProcessor().setAutoThreshold(AutoThresholder.Method.Otsu, true, ImageProcessor.NO_LUT_UPDATE);
		Roi maskRoi = ThresholdToSelection.run(mask);
		if (maskRoi==null) return null;
		Roi newRoi = maskRoi;
		if (resize)
			newRoi = RoiScaler.scale(maskRoi, xScale, yScale, false);
		newRoi.setPosition(0, 0, 0);
		newRoi.setStrokeColor(roiColor);

		imp.close(); mask.close();
		System.gc();
		
		return newRoi;
	}
	
	// trim ROI border, to get rid of border spot error in TrackMate spot detection
	protected static Roi trimBorder (Roi roi, ImagePlus imp, double borderSize) { // might be slow!!!
		if (roi==null || imp==null || borderSize==0) return roi;
		int posC = roi.getCPosition();
		int posZ = roi.getZPosition();
		int posT = roi.getTPosition();
		Color color = roi.getStrokeColor();
		int width = imp.getWidth(); int height = imp.getHeight();
		ShapeRoi imageBound = new ShapeRoi(new Roi(borderSize, borderSize, width-2*borderSize, height-2*borderSize));
		roi = new ShapeRoi(roi);
		roi = ((ShapeRoi) roi).and(imageBound);
		roi.setPosition(posC, posZ, posT);
		roi.setStrokeColor(color);
		return roi;
	}
	
	// compute rough area associated with ROI
	protected static int getArea (Roi roi) {
		if (roi==null || !roi.isArea()) return 0;
		return roi.size();
	}
	
	// function groups to handle ROI Manager
	protected static RoiManager prepareManager () {
		RoiManager rm = RoiManager.getInstance();
		if (rm==null) {
			rmOpen = false;
			rm = new RoiManager();
		} else {
			rmOpen = true;
			oriROIs = rm.getRoisAsArray();
			rm.reset();
		}
		rm.setVisible(false);
		return rm;
	}
	protected static void restoreManager () {
		RoiManager rm = RoiManager.getInstance();
		if (rm==null) {
			if (rmOpen) {
				rm = new RoiManager();
				for (Roi roi : oriROIs) {
					rm.addRoi(roi);
				}
				rm.setVisible(true);
			}
		} else {
			if (rmOpen) {
				rm.reset();
				for (Roi roi : oriROIs) {
					rm.addRoi(roi);
				}
				rm.setVisible(true);
			} else { rm.close(); }
		}
		return;
	}

	
}
