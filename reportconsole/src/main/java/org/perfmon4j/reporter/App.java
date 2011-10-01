package org.perfmon4j.reporter;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeListener;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.EtchedBorder;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.perfmon4j.reporter.controller.Controller;
import org.perfmon4j.reporter.gui.AEDConnectionDialog;
import org.perfmon4j.reporter.gui.ActionWithIcon;
import org.perfmon4j.reporter.gui.CustomTreeCellRenderer;
import org.perfmon4j.reporter.model.Category;
import org.perfmon4j.reporter.model.IntervalCategory;
import org.perfmon4j.reporter.model.P4JConnection;
import org.perfmon4j.reporter.model.P4JConnectionList;
import org.perfmon4j.reporter.model.Model;
import org.perfmon4j.reporter.model.P4JTreeNode;
import org.perfmon4j.reporter.model.SetBasedComboBoxModel;

import net.miginfocom.swing.MigLayout;

public class App extends JFrame {
	private static App app;
	final JPopupMenu treePopupMenu;
	final JTree tree;
	final JTabbedPane tabbedPane;
	final JComboBox selectConnection;
	
	final ActionWithIcon exitAction = new ActionWithIcon("Exit", "/org/perfmon4j/images/exit-32x32.png", "Exit Application", KeyEvent.VK_X) {
		public void actionPerformed(ActionEvent arg0) {
        	int option = JOptionPane.showConfirmDialog(null, "Really Exit?", "Exit", JOptionPane.YES_NO_OPTION);
            if (option == JOptionPane.YES_OPTION) {
            	System.exit(0);
            }
		}
	};
	
	final ActionWithIcon newConnectionAction = new ActionWithIcon("New", "/org/perfmon4j/images/new-connection-32x32.png", "Create New Connection") {
		public void actionPerformed(ActionEvent arg0) {
			P4JConnection conn = AEDConnectionDialog.showDialog(App.getApp());
			if (conn != null) {
				Model.getModel().getConnectionList().addConnection(conn);
				tree.updateUI();
			}
		}
	};

	final ActionWithIcon closeConnectionAction = new ActionWithIcon("Close", "/org/perfmon4j/images/close-connection-32x32.png", "Close Connection") {
		public void actionPerformed(ActionEvent arg0) {
		}
	};
	
	public static App getApp() {
		return app;
	}
	
    public JPopupMenu getTreePopupMenu() {
		return treePopupMenu;
	}

	final ActionWithIcon graphAction = new ActionWithIcon("Chart", "/org/perfmon4j/images/new-connection-32x32.png", "Quick Chart") {
		public void actionPerformed(ActionEvent arg0) {
			P4JConnectionList list = Model.getModel().getConnectionList();
			IntervalCategory c = list.getCurrentIntervalCategory();
				
			
				if (c != null && c.getDatabaseID() != null) {
					TimeSeriesCollection col = new TimeSeriesCollection();
					JFreeChart chart = ChartFactory.createTimeSeriesChart(
							c.getName(), "Time", "Duration", col, true, true,false);
					TextTitle t = new TextTitle("My Chart");
					
					chart.setTitle(t);
					chart.addSubtitle(new TextTitle("My Subtitle"));
					Plot plot = chart.getPlot();
					try {
						TimeSeries series[] = c.getTimeSeries();
						
						for (int i = 0; i < series.length; i++) {
							col.addSeries(series[i]);
							
							NumberAxis axis2 = new NumberAxis(series[i].getDescription());
							((XYPlot)plot).setRangeAxis(i, axis2);
//							((XYPlot)plot).setRangeAxisLocation(1, AxisLocation.BOTTOM_OR_RIGHT);
						}
					} catch (SQLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				
//				plot.setBackgroundImage(JFreeChart.INFO.getLogo());
				
//				ChartUtilities.saveChartAsJPEG(new File("c:/mychart.jpg"), chart, 500, 500);
				ChartPanel  chartPanel  =  new  ChartPanel(chart);
				chartPanel.setPreferredSize(new  java.awt.Dimension(500,  270));
				chartPanel.setMouseZoomable(true,  false);
			
				
				String n = c.getName();
				n = n.trim();
				tabbedPane.add(n, chartPanel);
			}
		}
	};
    
    
	public static void main( String[] args ) {
        /* Use an appropriate Look and Feel */
        try {
//            UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
            UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
        } catch (UnsupportedLookAndFeelException ex) {
            ex.printStackTrace();
        } catch (IllegalAccessException ex) {
            ex.printStackTrace();
        } catch (InstantiationException ex) {
            ex.printStackTrace();
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
        }
        /* Turn off metal's use bold fonts */
        UIManager.put("swing.boldMetal", Boolean.FALSE);
   	
    	
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
            	app = new App();
            }
        });
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu file = new JMenu("File");
        file.setMnemonic(KeyEvent.VK_F);
        file.add(exitAction.buildMenuItem());
        menuBar.add(file);
        
        return menuBar;
    }
    
    
    private JPopupMenu createTreePopupMenu() {
    	JPopupMenu m = new JPopupMenu();
    	
    	m.add(newConnectionAction.buildMenuItem());
    	m.add(graphAction.buildMenuItem());
    	
    	return m;
    }
    
   
    App() {
    	this.setTitle("Perfmon4j Reporter");
    	
		setJMenuBar(createMenuBar());
		
		JPanel cp = new JPanel(new BorderLayout());
		setContentPane(cp);
		
		// Create the toolbar...
        JToolBar toolBar = new JToolBar();
        toolBar.add(exitAction.buildToolBarButton());
        toolBar.add(new JToolBar.Separator());
        toolBar.add(newConnectionAction.buildToolBarButton());
        toolBar.add(closeConnectionAction.buildToolBarButton());
        toolBar.add(new JToolBar.Separator());
        toolBar.add(selectConnection = new JComboBox(new SetBasedComboBoxModel<P4JTreeNode>()));
		cp.add(toolBar, BorderLayout.PAGE_START);
		closeConnectionAction.setEnabled(false);
		
		
		tree = new JTree(Model.getModel().getConnectionList());
		ToolTipManager.sharedInstance().registerComponent(tree);

		// Add listener to update state when connections are added/removed
		Model.getModel().getConnectionList().addListener(new NodeChangeListenerImpl(this));
		
		tree.setCellRenderer(new CustomTreeCellRenderer());
		tree.addHierarchyListener(Controller.getController().getTreeListener());
		tree.addMouseListener(Controller.getController().getTreeListener());
		tree.addTreeSelectionListener(Controller.getController().getTreeListener());
		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		tree.add(treePopupMenu = createTreePopupMenu());
	
		
		
		JSplitPane sp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
				new JScrollPane(tree), tabbedPane = new JTabbedPane());
		sp.setDividerLocation(180);
		cp.add(sp, BorderLayout.CENTER);
		
	
		
		setSize(800, 600); 
		setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setVisible(true);
	}
}
