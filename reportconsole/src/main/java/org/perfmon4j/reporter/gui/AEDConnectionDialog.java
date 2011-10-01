package org.perfmon4j.reporter.gui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.sql.SQLException;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.filechooser.FileFilter;

import net.miginfocom.swing.MigLayout;

import org.perfmon4j.reporter.App;
import org.perfmon4j.reporter.model.P4JConnection;
import org.perfmon4j.reporter.model.ReportSQLConnection;

public class AEDConnectionDialog extends JDialog implements ActionListener {
	private static final long serialVersionUID = 1L;
    private static ReportSQLConnection result = null;
    
    private final JTextField url;
    private final JTextField jdbcClass;
    private final JTextField schema;
    private final JTextField userName;
    private final JTextField password;
    private final JTextField jdbcJarFile;
    private final JButton okButton;

    private final static String DEFAULT_URL = "jdbc:jtds:sqlserver:/localhost/perfmon4j";
    private final static String DEFAULT_JDBCCLASS = "net.sourceforge.jtds.jdbc.Driver";
    private final static String DEFAULT_SCHEMA = "dbo";
    private final static String DEFAULT_USERNAME = "sa";
    private final static String DEFAULT_PASSWORD = "";
    private final static String DEFAULT_JDBCJARFILE = "c:/data/jdbcdrivers/jtds.jar";
    
    
	private AEDConnectionDialog(Frame parent) {
		super(parent, "Create Connection", true);
		this.setResizable(false);
		Container cp = getContentPane();
		cp.setLayout(new MigLayout("wrap 5"));
		
		cp.add(new JLabel("URL:"));
		cp.add(url = new JTextField(DEFAULT_URL, 40), "span 3, wrap");

		cp.add(new JLabel("JDBC Class:"));
		cp.add(jdbcClass = new JTextField(DEFAULT_JDBCCLASS, 20));
		
		cp.add(new JLabel("Schema:"));
		cp.add(schema = new JTextField(DEFAULT_SCHEMA, 15), "wrap");
		
		cp.add(new JLabel("User name:"));
		cp.add(userName = new JTextField(DEFAULT_USERNAME, 20), "wrap");

		cp.add(new JLabel("Password:"));
		cp.add(password = new JPasswordField(DEFAULT_PASSWORD, 20), "wrap");

		cp.add(new JLabel("JDBC Jar File:"));
		cp.add(jdbcJarFile = new JTextField(DEFAULT_JDBCJARFILE, 40), "span 3");
		jdbcJarFile.setEditable(false);

		JButton lookup;
		cp.add(lookup = new JButton("Lookup.."), "wrap");
		lookup.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent action) {
				JFileChooser j;
				String fileName = jdbcJarFile.getText();
				
				if (fileName.isEmpty()) {
					j = new JFileChooser();
				} else {
					j = new JFileChooser(new File(fileName));
				}
				j.setFileFilter(new FileFilter() {
					@Override
					public boolean accept(File f) {
						return f != null 
							&& (f.isDirectory()
									|| f.getName().endsWith(".jar") 
									|| f.getName().endsWith(".JAR"));
					}
					@Override
					public String getDescription() {
						return "Java JAR file";
					}
				});
				j.setFileSelectionMode(JFileChooser.FILES_ONLY);
				int returnVal = j.showOpenDialog((Component)action.getSource());
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					jdbcJarFile.setText(j.getSelectedFile().getAbsolutePath());
				}
			}
		});
		JPanel buttonPanel = new JPanel(); 
		okButton = new JButton("Ok");
		okButton.addActionListener(this);
		
		JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(this);
		
		buttonPanel.add(okButton);
		buttonPanel.add(cancelButton);
		cp.add(buttonPanel, "span 5, center");

        pack();
        setLocationRelativeTo(parent);
	}
	
	public void actionPerformed(ActionEvent event) {
		if (event.getSource().equals(okButton)) {
			result = new ReportSQLConnection(url.getText(), userName.getText(), password.getText(), schema.getText(), 
					jdbcClass.getText(), jdbcJarFile.getText());
			try {
				result.refresh();
		        setVisible(false);
			} catch (SQLException e) {
				result = null;
				ExceptionDialog.showDialog(App.getApp(), e);
			}
		} else {
			setVisible(false);
		}
	}

    public static P4JConnection showDialog(Frame parent) {
    	AEDConnectionDialog dialog = new AEDConnectionDialog(parent);
    	dialog.setVisible(true);
    	dialog.dispose();
    	
    	return result;
    }
}
