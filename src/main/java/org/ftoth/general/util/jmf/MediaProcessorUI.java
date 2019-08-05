package org.ftoth.general.util.jmf;

import java.awt.Component;
import java.awt.Frame;
import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.MenuItem;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.media.Control;
import javax.media.Player;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import jmapps.ui.JMFrame;

import org.ftoth.general.util.jmf.MediaProcessorConfig;
import org.ftoth.general.util.jmf.MediaProcessor.PresentingTarget;


public class MediaProcessorUI extends JFrame implements ActionListener
{
	// ------------------------- constants, locals -------------------------
	private static final String MENU_TOOLS_PLUGINS = "Plugins";
	private static final String MENU_TOOL_PREFERENCES = "Preferences";
	
	private Component componentPlugins;
	private Control controlPlugins;
	private JMFRegistry jmfRegistry = null;
	
	@Override
	public void actionPerformed(ActionEvent event)
	{
		String strCmd = event.getActionCommand();
		if (strCmd.equals(MENU_TOOLS_PLUGINS)) {
			if (componentPlugins != null) {
				componentPlugins.setVisible(true);
			}
			else {
				if (controlPlugins != null && controlPlugins instanceof Component) {
					componentPlugins = (Component) controlPlugins;
					componentPlugins.setVisible(true);
					Component component = componentPlugins;
					while (component != null) {
						if (component instanceof Frame) {
							Frame frame = (Frame) component;
							JMFrame.autoPosition(frame, this);
							break;
						}
						component = component.getParent();
					}
				}
			}
		}
		else if (strCmd.equals(MENU_TOOL_PREFERENCES)) {
			if (jmfRegistry == null) {
				jmfRegistry = new JMFRegistry();
			}
			jmfRegistry.setVisible(true);
		}
	}
	
	public void initGUI(MediaProcessorConfig config)
	{
		// GUI
		setLocation(new Point(100, 100));
		setSize(500, 100);
		setVisible(true);
		invalidate();
		// app.pack();

		if (config.getPresentingTarget() == PresentingTarget.SINK && !config.isAutoStartProcessor()) {
			showMsgBox("Start processing to save data into sink.");
		}

		addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent e)
			{
				System.exit(0);
			}
		});
	}
	
	public static void showMsgBox(String msg)
	{
		JOptionPane.showMessageDialog(null, msg);
	}
	
	public void buildUI(Player processor)
	{
		// processor control panel
		Component ctrlPanel = processor.getControlPanelComponent();
		add(ctrlPanel);

		// menu
		MenuBar menu = new MenuBar();
		this.setMenuBar(menu);

		Menu menuTools = new Menu("Tools");
		menu.add(menuTools);

		MenuItem menuItemPlugins = new MenuItem(MENU_TOOLS_PLUGINS);
		menuItemPlugins.addActionListener(this);
		menuTools.add(menuItemPlugins);

		MenuItem menuItemPrefs = new MenuItem(MENU_TOOL_PREFERENCES);
		menuItemPrefs.addActionListener(this);
		menuTools.add(menuItemPrefs);

		// plugin viewer
		controlPlugins = processor.getControl("com.sun.media.JMD");
	}
	
}
