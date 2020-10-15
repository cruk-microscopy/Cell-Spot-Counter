package cam.lmb.niek;


import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.util.ArrayList;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;

import ij.plugin.frame.PlugInFrame;


public class GUIUtility {

	// parameters for GUI
	protected static PlugInFrame pf;
	protected static String frameName = "Fluorescent Spot Counter";
	protected static int frameWidth = 600;
	protected static int frameHeight = 500;
	
	protected static JPanel contentPanel;
	
	protected final int lineWidth = 60;
	protected static Color panelColor = new Color(204, 229, 255);
	protected static JPanel parentPanel;
	protected final Font textFont = new Font("Helvetica", Font.BOLD, 14);
	protected final Color fontColor = Color.BLACK;
	protected final Font errorFont = new Font("Helvetica", Font.BOLD, 14);
	protected final Color errorFontColor = Color.RED;
	protected final Color textAreaColor = new Color(204, 229 , 255);
	protected final Font panelTitleFont = new Font("Helvetica", Font.BOLD, 13);
	protected final Color panelTitleColor = Color.BLUE;
	protected final static EmptyBorder border = new EmptyBorder(new Insets(5, 5, 5, 5));
	protected final Dimension textAreaMax = new Dimension(260, 150);
	protected final Dimension tablePreferred = new Dimension(260, 100);
	protected final Dimension tableMax = new Dimension(260, 150);
	protected final Dimension panelTitleMax = new Dimension(500, 30);
	protected final static Dimension panelMax = new Dimension(620, 300);
	protected final Dimension panelMin = new Dimension(280, 200);
	protected final static Dimension buttonSize = new Dimension(120, 10);
	
	//protected Dimension buttonSize = new Dimension(120, 50);
	
	
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
	
	
	
	
	protected static void updateFrameByUser(String name) {	// make the frame gaylly for Hung-Chang
		String userName = System.getProperty("user.name");
		if (userName.equals(name)) {
			panelColor = new Color(255, 204, 229);
			frameName = "\u5b8f\u660c\u5144\u7684(\u57fa\u4f6c\u7c89)\u63d2\u4ef6";
		}
	}
	
	protected static void createPluginFrame (JPanel contentPanel) {
		createPluginFrame(frameName, contentPanel);
	}
	protected static void createPluginFrame (String frameName, JPanel contentPanel) {
		pf = new PlugInFrame(frameName);
		pf.setLayout(new BoxLayout(pf, BoxLayout.Y_AXIS));
		// a white background parent panel, to add component from top to bottom
		parentPanel = new JPanel();
		parentPanel.setBorder(border);
		parentPanel.setBackground(pf.getBackground());
		parentPanel.setLayout(new BoxLayout(parentPanel, BoxLayout.Y_AXIS));
		
		//addSubPanel()
		addPanel(parentPanel, contentPanel, true);
		
		//f.add(parentPanel, BorderLayout.NORTH);
		pf.add(parentPanel);
		pf.pack();
		pf.setSize(frameWidth, frameHeight);
		pf.setMinimumSize(panelMax);
		pf.setVisible(true);
		pf.setLocationRelativeTo(null);
		//pf.setResizable(false);
		//GUI.center(pf);
		return;
	}
	
	protected static JPanel createContentPanel () {
		updateFrameByUser("chen02");
		// create and configure the content panel
		contentPanel = new JPanel();
		contentPanel.setBorder(border);
		contentPanel.setBackground(panelColor);
		contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
		return contentPanel;
	}
	
	
	protected static void updateFrame() {
		if (contentPanel==null || pf==null) return;
		contentPanel.revalidate();
		contentPanel.repaint();
		parentPanel.revalidate();
		parentPanel.repaint();
		
		//Dimension parentSize = parentPanel.getSize();
		//Dimension frameSize = new Dimension(parentSize.width+10, parentSize.height+40);
		//pf.setSize(frameSize);
	}
	
	public static void addPanel(JPanel parentPanel, JPanel contentPanel, boolean vertical) {
		// configure the content panel
		contentPanel.setBorder(border);
		contentPanel.setBackground(panelColor);
		if (vertical)
			contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
		else
			contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.X_AXIS));
		parentPanel.add(contentPanel);
		contentPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
	}
	
	
	
	
	
	protected static void arrangePanel (JPanel panel) {// ROI panel, detection panel, filter panel ...
		panel.setBorder(border);
		panel.setBackground(panelColor);
		panel.setMaximumSize(panelMax);
		panel.validate();
		panel.revalidate();
	}
	
	//protected static void addButtonFunction (JButton button, ActionListener listener) {
	//	button.addActionListener( listener );
	//}
	
	protected static JPanel createButtonPanel (ArrayList<JButton> buttons, boolean vertical) {
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
		if (vertical) buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
		for (JButton button : buttons) {
			button.setPreferredSize(buttonSize);
			buttonPanel.add(button);	
		}
		buttonPanel.setBorder(border);
		buttonPanel.setBackground(panelColor);
		buttonPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
		return buttonPanel;
	}
	
	
	
}
