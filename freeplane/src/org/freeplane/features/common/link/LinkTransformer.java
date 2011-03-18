/*
 *  Freeplane - mind map editor
 *  Copyright (C) 2011 dimitry
 *
 *  This file author is dimitry
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
package org.freeplane.features.common.link;

import java.net.URI;

import javax.swing.Icon;

import org.freeplane.core.frame.ViewController;
import org.freeplane.core.ui.components.ObjectAndIcon;
import org.freeplane.features.common.map.MapModel;
import org.freeplane.features.common.map.ModeController;
import org.freeplane.features.common.map.NodeModel;
import org.freeplane.features.common.text.AbstractContentTransformer;
import org.freeplane.features.common.text.TextController;

/**
 * @author Dimitry Polivaev
 * Mar 3, 2011
 */
public class LinkTransformer extends AbstractContentTransformer {
	private ModeController modeController;

	public LinkTransformer(ModeController modeController, int priority) {
	    super(priority);
	    this.modeController = modeController;
    }

	public void registerListeners(ModeController modeController) {
	    final NodeUpdateChangeListener listener = new NodeUpdateChangeListener();
		modeController.getMapController().addNodeChangeListener(listener);
		modeController.getMapController().addMapChangeListener(listener);
    }

	public Object transformContent(Object content, NodeModel node, Object transformedExtension) {
		final MapModel map = node.getMap();
		return transformContent(content, map);
	}

	public Object transformContent(Object content, MapModel map) {
		if(! (content instanceof URI))
			return content;
		final String string = content.toString();
		if(! string.startsWith("#"))
			return content;
		final String nodeID=string.substring(1);
		final NodeModel target = map.getNodeForID(nodeID);
		if(target != null){
			final String shortText = TextController.getController(modeController).getShortText(target);
			final Icon icon = ViewController.localLinkIcon;
			return new ObjectAndIcon(shortText, icon);
		}
		else
			return content;
    }
}
