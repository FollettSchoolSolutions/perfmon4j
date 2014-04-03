package org.perfmon4j.reporter.gui;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JMenuItem;

import javax.swing.AbstractAction;;

public abstract class ActionWithIcon extends AbstractAction {
	private static final long serialVersionUID = 1L;
	private final String toolTipText;
	private final Integer mnemonic;

	public ActionWithIcon(String text, String iconPath, String toolTipText) {
		super(text, buildIconOrNull(iconPath));
		
		this.toolTipText = toolTipText;
		this.mnemonic = null;
	}
	
	public ActionWithIcon(String text, String iconPath, String toolTipText, int mnemonic) {
		super(text, buildIconOrNull(iconPath));
		
		this.toolTipText = toolTipText;
		this.mnemonic = new Integer(mnemonic);
	}
	
	public JMenuItem buildMenuItem() {
		JMenuItem result = new JMenuItem(this);
		result.setIcon(null);
		if (mnemonic != null) {
			result.setMnemonic(mnemonic.intValue());
		}
		result.setToolTipText(toolTipText);		
		return result;
	}
	
	public JButton buildToolBarButton() {
		JButton result = new JButton(this);
		result.setToolTipText(toolTipText);
		
		return result;
	}
	
	private static Icon buildIconOrNull(String iconPath) {
		Icon result = null;
		
		try {
			result = new ImageIcon(ActionWithIcon.class.getResource(iconPath));
		} catch (Exception ex) {
			System.out.println("Error loading icon: " + iconPath);
			ex.printStackTrace();
		}
		return result;
	}
}
