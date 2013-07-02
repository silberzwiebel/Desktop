package org.freeplane.core.ui.ribbon;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.freeplane.core.resources.ResourceController;
import org.freeplane.core.ui.components.UITools;
import org.freeplane.core.ui.ribbon.StructureTree.StructurePath;
import org.freeplane.core.ui.ribbon.special.EdgeStyleContributorFactory;
import org.freeplane.core.ui.ribbon.special.FilterConditionsContributorFactory;
import org.freeplane.core.ui.ribbon.special.FontStyleContributorFactory;
import org.freeplane.core.ui.ribbon.special.ViewSettingsContributorFactory;
import org.freeplane.core.ui.ribbon.special.ZoomContributorFactory;
import org.freeplane.core.util.LogUtils;
import org.freeplane.core.util.TextUtils;
import org.freeplane.features.mode.ModeController;
import org.pushingpixels.flamingo.api.common.icon.ImageWrapperResizableIcon;
import org.pushingpixels.flamingo.api.common.icon.ResizableIcon;
import org.pushingpixels.flamingo.api.ribbon.JRibbon;
import org.pushingpixels.flamingo.internal.ui.ribbon.appmenu.JRibbonApplicationMenuButton;


public class RibbonBuilder {
	
	private final HashMap<String, IRibbonContributorFactory> contributorFactories = new HashMap<String, IRibbonContributorFactory>();
	
	final StructureTree structure;
	private final RootContributor rootContributor;
	private final RibbonStructureReader reader;
	private final JRibbon ribbon;
	private final ModeController mode;

	private final RibbonAcceleratorManager accelManager;

	private boolean enabled = true;

	private RibbonMapChangeAdapter changeAdapter;
	
	public RibbonBuilder(ModeController mode, JRibbon ribbon) {
		structure = new StructureTree();
		this.rootContributor = new RootContributor(ribbon);
		this.ribbon = ribbon;
		this.mode = mode;
		reader = new RibbonStructureReader(this);
		accelManager = new RibbonAcceleratorManager(this);
		registerContributorFactory("separator", new RibbonSeparatorContributorFactory());
		registerContributorFactory("ribbon_menu", new RibbonMenuContributorFactory());
		registerContributorFactory("ribbon_taskbar", new RibbonTaskbarContributorFactory());
		registerContributorFactory("primary_entry", new RibbonMenuPrimaryContributorFactory());
		registerContributorFactory("entry_group", new RibbonMenuSecondaryGroupContributorFactory());
		registerContributorFactory("footer_entry", new RibbonMenuFooterContributorFactory());
		registerContributorFactory("ribbon_task", new RibbonTaskContributorFactory());
		registerContributorFactory("ribbon_band", new RibbonBandContributorFactory());
		registerContributorFactory("ribbon_action", new RibbonActionContributorFactory(this));
		registerContributorFactory("fontStyleContributor", new FontStyleContributorFactory());
		registerContributorFactory("edgeStyleContributor", new EdgeStyleContributorFactory());		
		registerContributorFactory("ribbon_flowband", new FlowRibbonBandContributorFactory());
		
		registerContributorFactory("zoomContributor", new ZoomContributorFactory());		
		registerContributorFactory("viewSettingsContributor", new ViewSettingsContributorFactory());
		registerContributorFactory("filterConditionsContributor", new FilterConditionsContributorFactory());
		
		updateApplicationMenuButton(ribbon);
	}

	public void updateApplicationMenuButton(JRibbon ribbon) {
		for(Component comp : ribbon.getComponents()) {
			if(comp instanceof JRibbonApplicationMenuButton) {
				String appName = ResourceController.getResourceController().getProperty("ApplicationName", "Freeplane");
				URL location = ResourceController.getResourceController().getResource("/images/"+appName.trim()+"_app_menu_128.png");
				if (location != null) {
					ResizableIcon icon = ImageWrapperResizableIcon.getIcon(location, new Dimension(32, 32));
					((JRibbonApplicationMenuButton) comp).setIcon(icon);
					((JRibbonApplicationMenuButton) comp).setBackground(Color.blue);
				}
			}
		}
	}
	
	public void add(ARibbonContributor contributor, StructurePath path, int position) {
		if(contributor == null || path == null) {
			throw new IllegalArgumentException("NULL");
		}
		synchronized (structure) {
			structure.insert(path, contributor, position);
		}
	}
	
	public void registerContributorFactory(String key, IRibbonContributorFactory factory) {
		synchronized (contributorFactories) {
			this.contributorFactories.put(key, factory);
		}

	}
	
	public IRibbonContributorFactory getContributorFactory(String key) {
		return this.contributorFactories.get(key);
	}
	
	public void buildRibbon() {
		Window f = SwingUtilities.getWindowAncestor(ribbon);
		if(!isEnabled()) {
			return;
		}
		getMapChangeAdapter().clear();
		synchronized (structure) {
			final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
			Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
			try {
				rootContributor.contribute(new RibbonBuildContext(this), null);
			}
			finally {
				Thread.currentThread().setContextClassLoader(contextClassLoader);
			}
		}
		f.setMinimumSize(new Dimension(640,240));
		f.pack();
		
	}
	
	public boolean isEnabled() {
		return enabled ;
	}
	
	public void setEnabled(boolean b) {
		enabled = b;
	}
	
	public void updateRibbon(URL xmlResource) {
		//final URL xmlSource = ResourceController.getResourceController().getResource(xmlResource);
		if (xmlResource != null) {
			final boolean isUserDefined = xmlResource.getProtocol().equalsIgnoreCase("file");
			try{
				reader.loadStructure(xmlResource);
				try {
					getAcceleratorManager().loadAcceleratorPresets(new FileInputStream(getAcceleratorManager().getPresetsFile()));
				}
				catch (IOException ex) {
					LogUtils.info("not accelerator presets found: "+ex);
				}
			}
			catch (RuntimeException e){
				if(isUserDefined){
					LogUtils.warn(e);
					String myMessage = TextUtils.format("ribbon_error", xmlResource.getPath(), e.getMessage());
					UITools.backOtherWindows();
					JOptionPane.showMessageDialog(UITools.getFrame(), myMessage, "Freeplane", JOptionPane.ERROR_MESSAGE);
					System.exit(-1);
				}
				throw e;
			}
		}
	}

	public boolean containsPath(StructurePath path) {
		synchronized (structure) {
			return structure.contains(path);
		}		
	}
	
	public ModeController getMode() {
		return mode;
	}

	public RibbonAcceleratorManager getAcceleratorManager() {
		return accelManager;
	}
	
	public RibbonMapChangeAdapter getMapChangeAdapter() {
		if(changeAdapter == null) {
			changeAdapter = new RibbonMapChangeAdapter();
		}
		return changeAdapter;
	}

}