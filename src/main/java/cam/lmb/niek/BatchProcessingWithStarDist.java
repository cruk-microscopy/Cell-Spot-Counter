package cam.lmb.niek;

import de.csbdresden.stardist.Opt;
import de.csbdresden.stardist.StarDist2D;
import de.csbdresden.stardist.StarDist2DModel;
import fiji.util.gui.GenericDialogPlus;

import java.awt.Color;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.scijava.Context;
import org.scijava.ItemIO;
import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.command.CommandModule;
import org.scijava.command.CommandService;
import org.scijava.convert.ConvertService;
import org.scijava.menu.MenuConstants;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.prefs.DefaultPrefService;
import org.scijava.ui.UIService;
import org.scijava.widget.Button;
import org.scijava.widget.ChoiceWidget;
import org.scijava.widget.NumberWidget;

import ij.IJ;
//import ij.ImageJ;
import net.imagej.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;
import ij.gui.Overlay;
import ij.gui.PointRoi;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.measure.Calibration;
import ij.measure.ResultsTable;
import ij.plugin.Duplicator;
import ij.plugin.HyperStackConverter;
import ij.plugin.filter.ThresholdToSelection;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;
import loci.common.services.ServiceException;
import loci.formats.ChannelSeparator;
import loci.formats.FormatException;
import loci.formats.meta.MetadataRetrieve;
import loci.formats.services.OMEXMLServiceImpl;
import loci.plugins.util.ImageProcessorReader;
import loci.plugins.util.LociPrefs;
import net.imagej.Dataset;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Pair;
import net.imglib2.util.ValuePair;
import net.imglib2.view.Views;
import ome.units.UNITS;
import net.imagej.command.*;
import net.imagej.ops.Op;
import net.imagej.ops.OpEnvironment;
import net.imagej.ui.*;

@Plugin(type = Op.class, name = "Batch Spot Counter", menuPath = "Plugins>Fluorescent Spot Count>Batch Spot Counter")
public class BatchProcessingWithStarDist implements Command, Op {

	@Parameter
	protected ImageJ ij;
	@Parameter
	protected ConvertService convertService;
	@Parameter
	protected UIService ui;
	@Parameter
	protected CommandService command;
	
	private List<File> fileList;
	private HashMap<String, Object> batchSetup;
	private String inputPath = System.getProperty("user.home");
	private String outputPath = "same as input";
	private File outputDir;
	private String extension = ".tif";
	private String setupPath = "";
	
	private boolean saveImage = false;
	private boolean saveCellROI = true;
	private boolean saveSpotROI = true;
	private boolean saveTable = true;
	
	private int roiChannel = DetectionUtility.autoROIChannel;
	private Color roiColor = DetectionUtility.autoROIColor;
	private int spotChannel = DetectionUtility.targetChannel;
	private double logRadius = DetectionUtility.spotRadius;
	private double logThreshold = DetectionUtility.qualityThreshold;
	private double maxTolerance = DetectionUtility.maxTolerance;
	private double minDistance = DetectionUtility.minDistance;
	private Color logColor = DetectionUtility.logColor;
	private Color maxColor = DetectionUtility.maxColor;
	private Color jointColor = DetectionUtility.jointColor;
	
	protected Dataset convertToDataset (ImagePlus imp) {
		Context context = ij.getContext();
		convertService = context.service(ConvertService.class);
		Dataset data = convertService.convert(imp, Dataset.class);
		return data;
	}
	
	protected ImagePlus convertToImagePlus(Dataset dataset) {
		//ConvertService convertService = null;
		//final ImageJ ij = new ImageJ();
		Context context = ij.getContext();
		convertService = context.service(ConvertService.class);
        ImagePlus image = convertService.convert(dataset, ImagePlus.class);
        //imagePlus = convertService.convert(dataset, ImagePlus.class);
        return image;
    }
	
	public void main(final String... args) throws Exception {
		run();
    }
	
	public boolean getBatchInput () {
		DefaultPrefService prefs = new DefaultPrefService();
		inputPath = prefs.get(String.class, "NiekSpotCounter-batch-inputPath", inputPath);
		extension = prefs.get(String.class, "NiekSpotCounter-batch-extension", extension);
		setupPath = prefs.get(String.class, "NiekSpotCounter-batch-setupPath", setupPath);
		saveImage = prefs.getBoolean(Boolean.class, "NiekSpotCounter-batch-saveImage", saveImage);
		saveCellROI = prefs.getBoolean(Boolean.class, "NiekSpotCounter-batch-saveCellROI", saveCellROI);
		saveSpotROI = prefs.getBoolean(Boolean.class, "NiekSpotCounter-batch-saveSpotROI", saveSpotROI);
		saveTable = prefs.getBoolean(Boolean.class, "NiekSpotCounter-batch-saveTable", saveTable);
		GenericDialogPlus gd = new GenericDialogPlus("Batch Processing: Fluorescent Spot Counter");
		gd.addDirectoryOrFileField("Input Folder", inputPath, 45);
		gd.addStringField("File Extension", extension);
		// setup file : if setup file doesn't exist, pop dialog 2 to get input
		// dialog2: cell ROI channel, spot channel, radius, threshold, tolerance, distance
		gd.setInsets(35, 0, 5);
		gd.addFileField("Setup File", setupPath, 45);
		gd.setInsets(35, 0, 5);
		gd.addDirectoryOrFileField("Result Folder", outputPath, 45);
		// save group: image, cell ROI, spot, result table (per image)
		// anyway save and display: batch table
		gd.setInsets(0, 55, 0);
		gd.addMessage("Save");
		gd.setInsets(10, 65, 0);
		final String[] labels = { "Image", "Cell ROI", "Spot", "Result Table" };
		boolean[] values = {saveImage, saveCellROI, saveSpotROI, saveTable};
		gd.addCheckboxGroup(1, 4, labels, values);
		gd.showDialog();
		if (gd.wasCanceled()) return false;
		inputPath = gd.getNextString();
		extension = gd.getNextString();
		setupPath = gd.getNextString();
		outputPath = gd.getNextString();
		saveImage = gd.getNextBoolean();
		saveCellROI = gd.getNextBoolean();
		saveSpotROI = gd.getNextBoolean();
		saveTable = gd.getNextBoolean();
		
		prefs.put(String.class, "NiekSpotCounter-batch-inputPath", inputPath);
		prefs.put(String.class, "NiekSpotCounter-batch-extension", extension);
		prefs.put(Boolean.class, "NiekSpotCounter-batch-saveImage", saveImage);
		prefs.put(Boolean.class, "NiekSpotCounter-batch-saveCellROI", saveCellROI);
		prefs.put(Boolean.class, "NiekSpotCounter-batch-saveSpotROI", saveSpotROI);
		prefs.put(Boolean.class, "NiekSpotCounter-batch-saveTable", saveTable);
		
		if (!getSetupParameter(setupPath)) return false;
		prefs.put(String.class, "NiekSpotCounter-batch-setupPath", setupPath);
		
		if (outputPath.equals("same as input")) outputPath = getFolder(inputPath);
		if (validateInputOutput()) return false;
		
		return true;
	}
	
	public boolean getSetupParameter (String setupPath) {
		if (!setupPath.endsWith(".xml")) {
			return getSetupFromUser();
		} else {
			batchSetup = IOUtility.loadBatchSetup(setupPath);
			return getSetupFromMap(batchSetup);
		}
	}
	
	
	public boolean getSetupFromMap (HashMap<String, Object> map) {
		if (map==null) return false;
		// get essential parameters
		if (map.get("roiChannel")==null) return false;
		else roiChannel = (int) map.get("roiChannel");

		if (map.get("spotChannel")==null) return false;
		else spotChannel = (int) map.get("spotChannel");
		
		if (map.get("logRadius")==null) return false;
		else logRadius = (double) map.get("logRadius");
		
		if (map.get("logThreshold")==null) return false;
		else logThreshold = (double) map.get("logThreshold");
		
		if (map.get("maxTolerance")==null) return false;
		else maxTolerance = (double) map.get("maxTolerance");
		
		if (map.get("minDistance")==null) return false;
		else minDistance = (double) map.get("minDistance");
		
		// get non-essential parameters
		if (map.get("roiColor")!=null)		roiColor = (Color) map.get("roiColor");
		if (map.get("logColor")!=null)		logColor = (Color) map.get("logColor");
		if (map.get("maxColor")!=null)		maxColor = (Color) map.get("maxColor");
		if (map.get("jointColor")!=null)	jointColor = (Color) map.get("jointColor");
		
		return true;
	}
	
	public boolean getSetupFromUser() {
		DefaultPrefService prefs = new DefaultPrefService();
		roiChannel = prefs.getInt(Integer.class, "NiekSpotCounter-batchSetup-roiChannel", roiChannel);
		spotChannel = prefs.getInt(Integer.class, "NiekSpotCounter-batchSetup-spotChannel", spotChannel);
		logRadius = prefs.getDouble(Double.class, "NiekSpotCounter-batchSetup-logRadius", logRadius);
		logThreshold = prefs.getDouble(Double.class, "NiekSpotCounter-batchSetup-logThreshold", logThreshold);
		maxTolerance = prefs.getDouble(Double.class, "NiekSpotCounter-batchSetup-maxTolerance", maxTolerance);
		minDistance = prefs.getDouble(Double.class, "NiekSpotCounter-batchSetup-minDistance", minDistance);
		GenericDialogPlus gd = new GenericDialogPlus("Batch Processing: Setup");
		gd.addNumericField("Cell ROI channel", roiChannel, 0, 6, "");
		gd.addNumericField("Spot detection channel", spotChannel, 0, 6, "");
		gd.addNumericField("Spot diameter", logRadius*2, 1, 6, "µm");
		gd.addNumericField("LoG threshold", logThreshold, 0, 6, "");
		gd.addNumericField("Max tolerance", maxTolerance, 0, 6, "");
		gd.addNumericField("Joint distance", minDistance, 1, 6, "µm");
		gd.showDialog();
		if (gd.wasCanceled()) return false;
		roiChannel = (int) gd.getNextNumber();
		spotChannel = (int) gd.getNextNumber();
		logRadius = gd.getNextNumber()/2;
		logThreshold = gd.getNextNumber();
		maxTolerance = gd.getNextNumber();
		minDistance = gd.getNextNumber();
		prefs.put(Integer.class, "NiekSpotCounter-batchSetup-roiChannel", roiChannel);
		prefs.put(Integer.class, "NiekSpotCounter-batchSetup-spotChannel", spotChannel);
		prefs.put(Double.class, "NiekSpotCounter-batchSetup-logRadius", logRadius);
		prefs.put(Double.class, "NiekSpotCounter-batchSetup-logThreshold", logThreshold);
		prefs.put(Double.class, "NiekSpotCounter-batchSetup-maxTolerance", maxTolerance);
		prefs.put(Double.class, "NiekSpotCounter-batchSetup-minDistance", minDistance);
		// put parameters into map
		batchSetup = new HashMap<String, Object>();
		batchSetup.put("roiChannel", roiChannel);
		batchSetup.put("roiColor", roiColor);
		batchSetup.put("spotChannel", spotChannel);
		batchSetup.put("logRadius", logRadius);
		batchSetup.put("logThreshold", logThreshold);
		batchSetup.put("maxTolerance", maxTolerance);
		batchSetup.put("minDistance", minDistance);
		batchSetup.put("logColor", logColor);
		batchSetup.put("maxColor", maxColor);
		batchSetup.put("jointColor", jointColor);
		// successful, return
		return true;
	}
	
	
	public boolean validateInputOutput () {
		if (outputPath==null) return false;
		outputDir = new File(outputPath);
		if (!outputDir.exists() || !outputDir.isDirectory()) return false;
		fileList = getFileList(inputPath);
		if (fileList==null || fileList.size()==0) return false;
		batchSetup.put("inputPath", inputPath);
		batchSetup.put("outputPath", outputPath);
		batchSetup.put("extension", extension);
		return true;
	}
	
	public String getFolder (String filePath) {
		File file = new File(filePath);
		if (!file.exists()) return null;
		if (file.isDirectory()) return filePath;
		if (file.isFile()) return file.getParent();
		return null;
	}
	
	public List<File> getFileList (String inputPath) {
		if (inputPath==null) return null;
		File inputFile = new File(inputPath);
		if (!inputFile.exists()) return null;
		// extend file extension
		String ext2 = extension.endsWith("tif") ? "tiff" : extension;
		// inputFile is file
		if (inputFile.isFile()) {
			List<File> list = new ArrayList<File>();
			if (inputFile.getName().endsWith(extension) || inputFile.getName().endsWith(ext2))
				list.add(inputFile);
			return list;
		}
		// inputFile is folder
		File[] files = inputFile.listFiles(new FilenameFilter() {	
			@Override
			public boolean accept(File dir, String name) {
				return (name.endsWith(extension) || name.endsWith(ext2));
			}
		});
		return Arrays.asList(files);
	}

	@Override
	public void run() {
		// get batch input, parse parameter, input, output file
		if (getBatchInput()) return;
		if (fileList==null || fileList.size()==0) return;
		// save batch setup to file
		IOUtility.saveBatchSetup (fileList, batchSetup, outputDir.getAbsolutePath()+File.separator+"batch setup.xml");
		RoiManager rm = ROIUtility.prepareManager();
		
		ResultsTable batchTable = new ResultsTable();
		batchTable.setPrecision(3);
		// process file list
		if (extension.endsWith("tif") || extension.endsWith("tiff")) {
			for (File file : fileList) {
				ImagePlus sourceImage = IJ.openImage(file.getAbsolutePath());
				if (sourceImage==null) continue;
				String imageName = sourceImage.getTitle();
				int dotIdx = imageName.lastIndexOf(".");
				imageName = imageName.substring(0, dotIdx);
				//if (saveImage) IJ.save(sourceImage, outputDir.getAbsolutePath()+File.separator+imageName+".tif");
				if (!saveCellROI && !saveSpotROI && !saveTable) continue; // continue to next image if no other result needed
				//Process imp with StarDist, try catch block need to be implemented.
					rm.reset();
					int posC = roiChannel;
					if (roiChannel>sourceImage.getNChannels()) posC = 1; // get channel
					ImagePlus impDup = new Duplicator().run(sourceImage, posC, posC, sourceImage.getZ(), sourceImage.getZ(), sourceImage.getT(), sourceImage.getT());
					Dataset img = convertToDataset(impDup); // convert to dataset
					if (img==null) { System.out.println("imageplus convert to dataset failed."); continue; }
					CommandModule res = null;
					try { res = command.run(StarDist2D.class, false,
							"input", img,
							"modelChoice", "Versatile (fluorescent nuclei)",
							"outputType", "ROI Manager").get();
					} catch (InterruptedException | ExecutionException e) {
						e.printStackTrace();
						System.out.println("Exception while running StarDist2D from command plugin: StarDistROI:168.");
						continue;
					}
					if (res==null) continue;
				//Process imp with StarDist, try catch block need to be implemented.
				int nROI = rm.getCount(); int nDigit = (""+nROI).length();
				if (nROI==0) continue;
				for (int i=0; i<nROI; i++) {
					ShapeRoi roi = new ShapeRoi(rm.getRoi(i));
					roi.setStrokeColor(roiColor);
					rm.setRoi(roi, i);
					rm.rename(i, "cell " + IJ.pad(i+1, nDigit));
				}
				if (saveCellROI) rm.runCommand("Save", outputDir.getAbsolutePath()+File.separator+imageName+" cell ROI.zip");
				Roi[] cellRois = rm.getRoisAsArray();
				
				Overlay logOverlay =  DetectionUtility.ovalToPointOverlay (
						DetectionUtility.getLogSpot (
								sourceImage, spotChannel, logRadius, logThreshold, 2, logColor), logColor);				
				Overlay maxOverlay =  DetectionUtility.getMaxOverlay (
							sourceImage, spotChannel, maxTolerance, maxColor);
				Overlay jointOverlay = DetectionUtility.getJointSpots (
							sourceImage, logOverlay, maxOverlay, minDistance, 2, jointColor);
				
				if (saveSpotROI) {
					rm.reset();
					rm.addRoi(DetectionUtility.overlayToSingleROI(logOverlay));
					rm.rename(0, "LoG spots");
					rm.addRoi(DetectionUtility.overlayToSingleROI(maxOverlay));
					rm.rename(1, "Max spots");
					rm.addRoi(DetectionUtility.overlayToSingleROI(jointOverlay));
					rm.rename(2, "Joint spots");
					rm.runCommand("Save", outputDir.getAbsolutePath()+File.separator+imageName+" spots.zip");
				}
				
				ResultsTable imageTable = ResultPanel.getResultsTable (
						sourceImage, cellRois, logOverlay, maxOverlay, jointOverlay);
				if (saveTable) imageTable.save(outputDir.getAbsolutePath()+File.separator+imageName+" result.csv");
				
				String[] columnHeadings = imageTable.getHeadings();
				for (int row=0; row<imageTable.getCounter(); row++) {
					batchTable.incrementCounter();
					batchTable.addValue("File", file.getName());
					batchTable.addValue("Image", imageName);
					for (int col=0; col<columnHeadings.length; col++) {
						String heading = columnHeadings[col];
						batchTable.addValue(heading, imageTable.getValue(heading, row));
					}
				}
				System.gc();
			}
			batchTable.save(outputDir.getAbsolutePath()+File.separator+"batch result.csv");
			batchTable.show("Batch Spot Count Result Table");
			ROIUtility.restoreManager();
			return;
		}
		

		// process bioformat file: multiple images per file
		for (File file : fileList) {
			// process bioformat file: multiple images per file
			ImageProcessorReader  r = new ImageProcessorReader(new ChannelSeparator(LociPrefs.makeImageReader()));
			OMEXMLServiceImpl OMEXMLService = new OMEXMLServiceImpl();
			
			try { r.setMetadataStore(OMEXMLService.createOMEXMLMetadata()); } 
			catch (ServiceException e2) { e2.printStackTrace(); continue;}
			try { r.setId(file.getAbsolutePath()); } 
			catch (FormatException | IOException e1) { e1.printStackTrace(); continue;}
			
			MetadataRetrieve meta = (MetadataRetrieve) r.getMetadataStore();
			int numSeries = meta.getImageCount();
			// iterate through series in Format container
			for (int id=0; id<numSeries; id++) {
				r.setSeries(id);
				// construct ImagePlus
				ImageStack stack = new ImageStack(r.getSizeX(), r.getSizeY());
				for (int n = 0; n < r.getImageCount(); n++) {
					ImageProcessor ip = null;
					
					try { ip = r.openProcessors(n)[0];} 
					catch (FormatException | IOException e) { e.printStackTrace(); continue;}
					
					stack.addSlice("" + (n + 1), ip);
				}
				// create ImagePlus using stack
				ImagePlus sourceImage = new ImagePlus("", stack);
				sourceImage = HyperStackConverter.toHyperStack(sourceImage, r.getSizeC(), r.getSizeZ(), r.getSizeT());
				Calibration cali = new Calibration();
				cali.pixelWidth = meta.getPixelsPhysicalSizeX(id).value(UNITS.MICROMETER).doubleValue();
				cali.pixelHeight = meta.getPixelsPhysicalSizeY(id).value(UNITS.MICROMETER).doubleValue();
				if (r.getSizeZ() > 1) {
					cali.pixelDepth = meta.getPixelsPhysicalSizeZ(id).value(UNITS.MICROMETER).doubleValue();
				}
				cali.setUnit("micron");
				sourceImage.setGlobalCalibration(cali);
				String imageName = meta.getImageName(id);
				sourceImage.setTitle(imageName);
				if (saveImage) IJ.save(sourceImage, outputDir.getAbsolutePath()+File.separator+imageName+".tif");
				if (!saveCellROI && !saveSpotROI && !saveTable) continue; // continue to next image if no other result needed
				//Process imp with StarDist, try catch block need to be implemented.
					rm.reset();
					int posC = roiChannel;
					if (roiChannel>sourceImage.getNChannels()) posC = 1; // get channel
					ImagePlus impDup = new Duplicator().run(sourceImage, posC, posC, sourceImage.getZ(), sourceImage.getZ(), sourceImage.getT(), sourceImage.getT());
					Dataset img = convertToDataset(impDup); // convert to dataset
					if (img==null) { System.out.println("imageplus convert to dataset failed."); continue; }
					CommandModule res = null;
					try { res = command.run(StarDist2D.class, false,
							"input", img,
							"modelChoice", "Versatile (fluorescent nuclei)",
							"outputType", "ROI Manager").get();
					} catch (InterruptedException | ExecutionException e) {
						e.printStackTrace();
						System.out.println("Exception while running StarDist2D from command plugin: StarDistROI:168.");
						continue;
					}
					if (res==null) continue;
				//Process imp with StarDist, try catch block need to be implemented.
				int nROI = rm.getCount(); int nDigit = (""+nROI).length();
				if (nROI==0) continue;
				for (int i=0; i<nROI; i++) {
					ShapeRoi roi = new ShapeRoi(rm.getRoi(i));
					roi.setStrokeColor(roiColor);
					rm.setRoi(roi, i);
					rm.rename(i, "cell " + IJ.pad(i+1, nDigit));
				}
				if (saveCellROI) rm.runCommand("Save", outputDir.getAbsolutePath()+File.separator+imageName+" cell ROI.zip");
				Roi[] cellRois = rm.getRoisAsArray();
				
				Overlay logOverlay =  DetectionUtility.ovalToPointOverlay (
						DetectionUtility.getLogSpot (
								sourceImage, spotChannel, logRadius, logThreshold, 2, logColor), logColor);				
				Overlay maxOverlay =  DetectionUtility.getMaxOverlay (
							sourceImage, spotChannel, maxTolerance, maxColor);
				Overlay jointOverlay = DetectionUtility.getJointSpots (
							sourceImage, logOverlay, maxOverlay, minDistance, 2, jointColor);
				
				if (saveSpotROI) {
					rm.reset();
					rm.addRoi(DetectionUtility.overlayToSingleROI(logOverlay));
					rm.rename(0, "LoG spots");
					rm.addRoi(DetectionUtility.overlayToSingleROI(maxOverlay));
					rm.rename(1, "Max spots");
					rm.addRoi(DetectionUtility.overlayToSingleROI(jointOverlay));
					rm.rename(2, "Joint spots");
					rm.runCommand("Save", outputDir.getAbsolutePath()+File.separator+imageName+" spots.zip");
				}
				
				ResultsTable imageTable = ResultPanel.getResultsTable (
						sourceImage, cellRois, logOverlay, maxOverlay, jointOverlay);
				if (saveTable) imageTable.save(outputDir.getAbsolutePath()+File.separator+imageName+" result.csv");
				
				String[] columnHeadings = imageTable.getHeadings();
				for (int row=0; row<imageTable.getCounter(); row++) {
					batchTable.incrementCounter();
					batchTable.addValue("File", file.getName());
					batchTable.addValue("Image", imageName);
					for (int col=0; col<columnHeadings.length; col++) {
						String heading = columnHeadings[col];
						batchTable.addValue(heading, imageTable.getValue(heading, row));
					}
				}
				System.gc();
			}
		}
		batchTable.save(outputDir.getAbsolutePath()+File.separator+"batch result.csv");
		batchTable.show("Batch Spot Count Result Table");
		ROIUtility.restoreManager();
	}



	protected static Roi[] labelToRois (ImagePlus labelImp, Color color) {
		if (labelImp==null) return null;
		int objCount = (int) labelImp.getStatistics().max;
		if (objCount <= 0) return null;
		Roi[] rois = new Roi[objCount];
		for (int i=0; i<objCount; i++) {
			labelImp.getProcessor().setThreshold(i+1, i+1, ImageProcessor.NO_LUT_UPDATE);
			rois[i] = ThresholdToSelection.run(labelImp);
			//rois[i].setLocation(rois[i].getBounds().x + xOffset, rois[i].getBounds().y + yOffset);
			rois[i].setPosition(0, 0, 0);
			rois[i].setStrokeColor(color);
		}
		return rois;
	}
	
	
	@Override
	public OpEnvironment ops() {
		return null;
	}

	@Override
	public void setEnvironment(OpEnvironment ops) {
	}
	
}
