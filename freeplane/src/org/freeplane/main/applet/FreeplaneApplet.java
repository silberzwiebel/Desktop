/*
 *  Freeplane - mind map editor
 *  Copyright (C) 2008 Joerg Mueller, Daniel Polansky, Christian Foltin, Dimitry Polivaev
 *
 *  This file is modified by Dimitry Polivaev in 2008.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.freeplane.main.applet;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.HeadlessException;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;

import javax.swing.FocusManager;
import javax.swing.JApplet;

import org.freeplane.core.controller.Controller;
import org.freeplane.core.frame.ViewController;
import org.freeplane.core.ui.ShowSelectionAsRectangleAction;
import org.freeplane.features.browsemode.BModeController;
import org.freeplane.features.common.attribute.ModelessAttributeController;
import org.freeplane.features.common.filter.FilterController;
import org.freeplane.features.common.filter.NextNodeAction;
import org.freeplane.features.common.icon.IconController;
import org.freeplane.features.common.link.LinkController;
import org.freeplane.features.common.styles.MapViewLayout;
import org.freeplane.features.common.text.TextController;
import org.freeplane.features.common.text.TextController.Direction;
import org.freeplane.features.common.time.TimeController;
import org.freeplane.features.controller.help.HelpController;
import org.freeplane.features.controller.print.PrintController;
import org.freeplane.main.browsemode.BModeControllerFactory;
import org.freeplane.view.swing.addins.nodehistory.NodeHistory;
import org.freeplane.view.swing.map.MapViewController;
import org.freeplane.view.swing.map.ViewLayoutTypeAction;

public class FreeplaneApplet extends JApplet {
	private AppletResourceController appletResourceController;
	private static PropertyChangeListener focusListener;
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private AppletViewController appletViewController;
// // 	private Controller controller;

	public FreeplaneApplet() throws HeadlessException {
	    super();
    }

	@Override
	synchronized public void destroy() {
		Controller.getCurrentController().shutdown();
	}

	@Override
	public void init() {
		synchronized (FreeplaneApplet.class){
			if(focusListener == null){
				focusListener  = new PropertyChangeListener() {
					public void propertyChange(PropertyChangeEvent evt) {
						System.out.print(evt.toString());
					}
				};
			}
			FocusManager.getCurrentKeyboardFocusManager().removePropertyChangeListener("currentFocusCycleRoot", focusListener);
			FocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener("currentFocusCycleRoot", focusListener);
			appletResourceController = new AppletResourceController(this);
			updateLookAndFeel();
			createRootPane();
			Controller controller = new Controller(appletResourceController);
			appletResourceController.init();
			Controller.setCurrentController(controller);
			final Container contentPane = getContentPane();
			contentPane.setLayout(new BorderLayout());
			appletViewController = new AppletViewController(this, controller, new MapViewController());
			controller.addAction(new ViewLayoutTypeAction(MapViewLayout.OUTLINE));
			FilterController.install();
			PrintController.install();
			HelpController.install();
			NodeHistory.install(controller);
			ModelessAttributeController.install();
			TextController.install();
			TimeController.install();
			LinkController.install();
			IconController.install();
			final BModeController browseController = BModeControllerFactory.createModeController("/xml/appletMenu.xml");
			controller.addAction(new ShowSelectionAsRectangleAction());
			controller.addAction(new NextNodeAction(Direction.FORWARD));
			controller.addAction(new NextNodeAction(Direction.BACK));
			controller.selectMode(browseController);
			appletResourceController.setPropertyByParameter(this, "browsemode_initial_map");
			appletViewController.init(controller);
			controller.getViewController().setMenubarVisible(false);
		}
	}

	@Override
	public void start() {
		appletViewController.start();
	}

	@Override
	synchronized public void stop() {
		super.stop();
	}

	private void updateLookAndFeel() {
		String lookAndFeel = "";
		appletResourceController.setPropertyByParameter(this, "lookandfeel");
		lookAndFeel = appletResourceController.getProperty("lookandfeel");
		ViewController.setLookAndFeel(lookAndFeel);
	}
}
