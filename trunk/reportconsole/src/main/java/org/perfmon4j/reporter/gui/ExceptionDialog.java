package org.perfmon4j.reporter.gui;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import net.miginfocom.swing.MigLayout;

public class ExceptionDialog extends JDialog implements ActionListener {
	private static final long serialVersionUID = 1L;
    
	private final Throwable ex;
    
	private ExceptionDialog(Frame parent, Throwable ex) {
		super(parent, "Error", true);
//		this.setResizable(false);
		this.ex = ex;
		Container cp = getContentPane();
		cp.setLayout(new MigLayout());
		
		JTextArea message = new JTextArea(ex.getMessage(), 3, 75);
		message.setEditable(false);
		cp.add(message, "wrap");
		
		
		StringWriter w = new StringWriter();
		PrintWriter pw = new PrintWriter(w);
		ex.printStackTrace(pw);
		
		
		
		JTextArea stackTrace = new JTextArea(w.toString(), 25, 75);
		stackTrace.setEditable(false);
		
		JScrollPane sp = new JScrollPane(stackTrace);
		cp.add(sp, "wrap");

		JButton okButton = new JButton("Ok");
		okButton.addActionListener(this);
		
		cp.add(okButton, "center");

        pack();
        setLocationRelativeTo(parent);
	}
	
	public void actionPerformed(ActionEvent event) {
		setVisible(false);
	}

    public static void showDialog(Frame parent, Throwable th) {
    	ExceptionDialog dialog = new ExceptionDialog(parent, th);
    	dialog.setVisible(true);
    	dialog.dispose();
    }
}
