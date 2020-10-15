package cam.lmb.niek;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;

import fiji.plugin.trackmate.SpotCollection;
import ij.ImagePlus;
import ij.plugin.PlugIn;
import ij.plugin.frame.PlugInFrame;


public class SpotCounter implements PlugIn {

	// parameters for source image
	protected ImagePlus sourceImage = null;
	protected int targetChannel;
	protected int numC;
	protected int numZ;
	protected int numT;
	protected int posC;
	protected int posZ;
	protected int posT;
	
	protected double spotRadius = 4.0d;
	protected double qualityThreshold = 40;
	protected SpotCollection tmSpots;
	
	// parameters for GUI
	protected PlugInFrame pf;
	
	protected String frameName = "Cell Counter";
	protected final int lineWidth = 60;
	protected Color panelColor = new Color(204, 229, 255);
	protected final Font textFont = new Font("Helvetica", Font.BOLD, 14);
	protected final Color fontColor = Color.BLACK;
	protected final Font errorFont = new Font("Helvetica", Font.BOLD, 14);
	protected final Color errorFontColor = Color.RED;
	protected final Color textAreaColor = new Color(204, 229 , 255);
	protected final Font panelTitleFont = new Font("Helvetica", Font.BOLD, 13);
	protected final Color panelTitleColor = Color.BLUE;
	protected final EmptyBorder border = new EmptyBorder(new Insets(5, 5, 5, 5));
	protected final Dimension textAreaMax = new Dimension(260, 150);
	protected final Dimension tablePreferred = new Dimension(260, 100);
	protected final Dimension tableMax = new Dimension(260, 150);
	protected final Dimension panelTitleMax = new Dimension(500, 30);
	protected final Dimension panelMax = new Dimension(600, 400);
	protected final Dimension panelMin = new Dimension(280, 200);
	protected final Dimension buttonSize = new Dimension(90, 10);
	
	protected final Dimension numberMax = new Dimension(100, 20);
	
	protected JButton btnManualTrack;
	protected JButton btnAutoTrack;
	protected JButton btnCfgAuto;
	
	protected JButton btnChangeRoiShape;
	protected JButton btnDelBefore;
	protected JButton btnDelAfter;
	
	protected JButton btnViewPlot;
	protected JButton btnViewTable;
	
	protected JButton btnFindGap;
	
	protected JTextArea sourceInfo;
	protected JTextArea spotCount;


	protected void createFrame() {

		//sourceImage = WindowManager.getCurrentImage();

		// create and configure the content panel
		JPanel contentPanel = GUIUtility.createContentPanel();

		DetectionPanel.addDetectionPanel(contentPanel);
		
		
		// result panel for reporting result: add real measurement of mean and stdDev of each channel in the future

		
		//FilterPanel.addFilterPanel(contentPanel);
		
		//contentPanel.add(resultPanel);
		
		ResultPanel.addResultPanel(contentPanel);
		
		contentPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
		
		GUIUtility.createPluginFrame(contentPanel);
	}
	
	
	@Override
	public void run(String arg) {
		//prepareSourceImage();
		
		createFrame();
	}
		
}
