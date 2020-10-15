package cam.lmb.niek;

//import de.csbdresden.stardist.Opt;
import de.csbdresden.stardist.StarDist2D;
//import de.csbdresden.stardist.StarDist2DModel;

import java.awt.Color;
import java.util.concurrent.ExecutionException;

import org.scijava.Context;
import org.scijava.command.Command;
import org.scijava.command.CommandModule;
import org.scijava.command.CommandService;
import org.scijava.command.ContextCommand;
import org.scijava.convert.ConvertService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

//import ij.ImageJ;
import net.imagej.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.plugin.Duplicator;
import ij.plugin.filter.ThresholdToSelection;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;
import net.imagej.Dataset;

//import net.imagej.ui.*;

@Plugin(type = Command.class, name = "Smart ROI", menuPath = "Plugins>Fluorescent Spot Count>Smart ROI")
public class StarDistROI extends ContextCommand implements Command {

	
	
	//private ImagePlus inputImp;
	private int targetChannel;
	
	private ImagePlus labelImp;
	private Dataset labelDataset;
	
	private Roi[] labelRois;
	
	@Parameter
	protected static ImageJ ij;
	
	@Parameter
	protected static ConvertService convertService;
	
	//@Parameter 
	//protected static Dataset inputData;
	@Parameter
	protected static ImagePlus inputImp;
	
	@Parameter
	protected static UIService ui;
	
	@Parameter
	protected static CommandService command;
	
	
	public void StarDistROI () {
		this.inputImp = WindowManager.getCurrentImage();
		this.targetChannel = 1;
	}
	public void StarDistROI (ImagePlus imp) {
		this.inputImp = imp;
		this.targetChannel = 1;
	}
	public void StarDistROI (ImagePlus imp, int channel) {
		this.inputImp = imp;
		this.targetChannel = channel;
	}
	
	public Roi[] getRois () {
		return this.labelRois;
	}
	
	private void datasetToLabelImage() {
		labelImp = convertService.convert(labelDataset, ImagePlus.class);
    }
	
	//private void inputToDataset() {
	//	inputData = convertService.convert(inputImp, Dataset.class);
    //}
	
	protected static Dataset convertToDataset (final ImageJ ijInstance, ImagePlus imp) {
		if (ijInstance==null) {
			System.out.println("ij is null");
			return null;
		} else {
			//System.out.println("ij shortName: " + ijInstance.getShortName());
			//System.out.println("ij title: " + ijInstance.getTitle());
			//System.out.println("ij version: " + ijInstance.getVersion());
		}
		
		Context context = ijInstance.getContext();
		convertService = context.service(ConvertService.class);
		Dataset data = convertService.convert(imp, Dataset.class);
		return data;
	}
	
	protected static ImagePlus convertToImagePlus(final ImageJ ijInstance, Dataset dataset) {
		if (ijInstance==null) {
			System.out.println("ij is null");
			return null;
		} else {
			//System.out.println("ij shortName: " + ijInstance.getShortName());
			//System.out.println("ij title: " + ijInstance.getTitle());
			//System.out.println("ij version: " + ijInstance.getVersion());
		}
		Context context = ijInstance.getContext();
		convertService = context.service(ConvertService.class);
        ImagePlus image = convertService.convert(dataset, ImagePlus.class);
        //imagePlus = convertService.convert(dataset, ImagePlus.class);
        return image;
    }
	
	public static void main(final String... args) throws Exception {
        ij = new ImageJ();
        ij.launch(args);
        //
        ImagePlus imp = DetectionUtility.sourceImage;
		if (imp==null) return;
		int channel = DetectionUtility.autoROIChannel;
		if (channel > imp.getNChannels()) channel = 1;
		// get channel 2D image
		int xOffset = 0; int yOffset = 0;
		if (imp.getRoi()!=null) {
			xOffset = imp.getRoi().getBounds().x;
			yOffset = imp.getRoi().getBounds().y;
		}
		ImagePlus impDup = new Duplicator().run(imp, channel, channel, imp.getZ(), imp.getZ(), imp.getT(), imp.getT());
		imp.deleteRoi();
		// convert to dataset
		Dataset inputData = convertToDataset(ij, impDup);
		if (inputData==null) {
			System.out.println("ImagePlus convert to Dataset failed.");
			return;
		}
		
		CommandModule res = null;
		try {
			res = command.run(StarDist2D.class, false,
					"input", inputData,
					"modelChoice", "Versatile (fluorescent nuclei)",
					"outputType", "Label Image").get();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
			System.out.println("Exception while running StarDist2D from command plugin: StarDistROI:168.");
		}
		if (res==null) return;
		
		Dataset label = (Dataset) res.getOutput("label");
		ImagePlus labelImage = convertToImagePlus(ij, label);
		if (labelImage==null) {
			System.out.println("Dataset convert to ImagePlus failed.");
			return;
		}
		
		Roi[] rois = labelToRois(labelImage, DetectionUtility.autoROIColor, xOffset, yOffset);
		Roi starRoi = combineRois(rois);
		ROIUtility.roi = starRoi;
		DetectionUtility.sourceImage.setRoi(starRoi);
    }
	
	protected static void getRois (ImagePlus imp, int channel, Color roiColor) {
		
		new StarDistROI().run();
		//ImagePlus labelImp = getLabelImage(imp, channel);
		//Roi[] rois = labelToRois (labelImp, roiColor);
		//labelImp.close(); System.gc();
		//return combineRois(rois);
	}
	
	
	protected static Roi[] getRoisFromManager(Color color, int xOffset, int yOffset) {
		RoiManager rm = RoiManager.getInstance();
		if (rm==null) return null;
		Roi[] rois = rm.getRoisAsArray();
		for (int i=0; i<rois.length; i++) {
			rois[i] = new ShapeRoi(rois[i]);
			rois[i].setLocation(rois[i].getBounds().x + xOffset, rois[i].getBounds().y + yOffset);
			rois[i].setStrokeColor(color);
		}
		return rois;
	}
	protected static Roi combineRois (Roi[] rois) {
		if (rois==null) return null;
		if (rois.length==1) return rois[0];
		int idx = 0;
		for (idx=0; idx<rois.length; idx++) {
			if (rois[idx].isArea()) break;
		}
		Roi firstRoi = new ShapeRoi(rois[idx]);
		//System.out.println("roi idx: " + idx);
		for (int i=idx+1; i<rois.length; i++) {
			if (!rois[i].isArea()) continue;
			firstRoi = ((ShapeRoi) firstRoi).or(new ShapeRoi(rois[i]));
		}
		//firstRoi.setStrokeColor(DetectionUtility.autoROIColor);
		return firstRoi;
	}
	
	
	protected static Roi[] labelToRois (ImagePlus labelImp, Color color, int xOffset, int yOffset) {
		if (labelImp==null) return null;
		int objCount = (int) labelImp.getStatistics().max;
		if (objCount <= 0) return null;
		Roi[] rois = new Roi[objCount];
		for (int i=0; i<objCount; i++) {
			labelImp.getProcessor().setThreshold(i+1, i+1, ImageProcessor.NO_LUT_UPDATE);
			rois[i] = ThresholdToSelection.run(labelImp);
			rois[i].setLocation(rois[i].getBounds().x + xOffset, rois[i].getBounds().y + yOffset);
			rois[i].setStrokeColor(color);
		}
		return rois;
	}
	
	protected void getLabelImage (ImagePlus input, int channel) {
		//ImageJ ij = new ImageJ();
		// get channel image
		if (channel > input.getNChannels()) channel = 1;
		int xOffset = 0; int yOffset = 0;
		if (input.getRoi()!=null) {
			xOffset = input.getRoi().getBounds().x;
			yOffset = input.getRoi().getBounds().y;
		}
		ImagePlus impDup = new Duplicator().run(input, channel, channel, input.getZ(), input.getZ(), input.getT(), input.getT());
		input.deleteRoi();
		// convert to dataset
		//Dataset img = convertToDataset(impDup);
		Dataset inputData = convertToDataset(ij, impDup);
		//inputToDataset();
		if (inputData==null) {
			System.out.println("imageplus convert to dataset failed.");
			return;
		}
		// get result from StarDist 2D (as ImageJ2 command)
		CommandModule res = null;
		try {
			res = command.run(StarDist2D.class, false,
					"input", inputData,
					"modelChoice", "Versatile (fluorescent nuclei)",
					"outputType", "Label Image").get();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
			System.out.println("Exception while running StarDist2D from command plugin: StarDistROI:168.");
		}
		if (res==null) {
			System.out.println("StarDist 2D execution failed.");
			return;
		}
		// convert result dataset to imageplus
		labelDataset = (Dataset) res.getOutput("label");
		//ImagePlus labelImage = convertToImagePlus(label);
		datasetToLabelImage();
		
		if (labelImp==null) {
			System.out.println("Dataset convert to ImagePlus failed.");
			return;
		}
		
		labelRois = labelToRois(labelImp, DetectionUtility.autoROIColor, xOffset, yOffset);
		
		return;
		//if (labelImage==null) {
		//	System.out.println("label convert to imageplus failed.");
		//	return null;
		//}
		
		//return labelImage;
		//ui.show(label);
	}
	


	@Override
	public void run() {
		//if (ij==null) 
		//	final ImageJ ij = new ImageJ();
		//ij.launch(args);
		
		// fetch source image from detection panel;
		if (inputImp==null)
			inputImp = DetectionUtility.sourceImage==null ? WindowManager.getCurrentImage() : DetectionUtility.sourceImage;
		if (inputImp==null) return;
		// fetch and prepare RoiManager
		RoiManager rm = ROIUtility.prepareManager();
		// prepare ROI channel from detection panel:
		targetChannel = DetectionUtility.autoROIChannel;
		if (targetChannel > inputImp.getNChannels()) targetChannel = 1;
		// display message to resultPanel for wait
		ResultPanel.cellInfo = "      Performing StarDist 2D cell segmentation, please be patient...\n";
		ResultPanel.updateInfo();
		// get channel 2D image
		int xOffset = 0; int yOffset = 0;
		if (inputImp.getRoi()!=null) {
			xOffset = inputImp.getRoi().getBounds().x;
			yOffset = inputImp.getRoi().getBounds().y;
		}
		ImagePlus impDup = new Duplicator().run(inputImp, targetChannel, targetChannel, inputImp.getZ(), inputImp.getZ(), inputImp.getT(), inputImp.getT());
		inputImp.deleteRoi();
		// convert to dataset
		Dataset inputData = convertToDataset(ij, impDup);
		if (inputData==null) {
			System.out.println("ImagePlus convert to Dataset failed.");
			ROIUtility.restoreManager();
			return;
		}
		CommandModule res = null;
		try {
			res = command.run(StarDist2D.class, false,
					"input", inputData,
					"modelChoice", "Versatile (fluorescent nuclei)",
					"outputType", "ROI Manager").get();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
			System.out.println("Exception while running StarDist2D from command in plugin: StarDistROI.run().");
		}
		if (res==null) { ROIUtility.restoreManager(); return; }
		
		/*
		Dataset label = (Dataset) res.getOutput("label");
		ImagePlus labelImage = convertToImagePlus(ij, label);
		if (labelImage==null) {
			System.out.println("Dataset convert to ImagePlus failed.");
			restoreManager();
			return;
		}
		*/
		//labelRois = labelToRois(labelImage, DetectionUtility.autoROIColor, xOffset, yOffset);
		//Roi[] rois = labelToRois(labelImage, DetectionUtility.autoROIColor);
		labelRois = getRoisFromManager(DetectionUtility.autoROIColor, xOffset, yOffset);
		ROIUtility.rois = labelRois;
		DetectionUtility.autoROIMode = 0;
		ResultPanel.cellInfo = InfoUtility.getCellROIInfo(inputImp);
		ResultPanel.updateInfo();
		Roi starRoi = combineRois(labelRois);
		ROIUtility.roi = starRoi;
		//inputImp.deleteRoi();
		inputImp.setRoi(starRoi);
		
		// display message to resultPanel for finish
		impDup.close(); //labelImage.close();
		ROIUtility.restoreManager();
		System.gc();
		//ui.show(label);
	}


}
