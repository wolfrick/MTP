package tejas;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.ArrayList;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeriesCollection;


public class Main 
{
	static ArrayList<Matrix> allFiles;
    static ArrayList<Integer> indices;
	static int numFiles;
	static JFrame mainFrame, splash;
	static JSplitPane formAndCards,tab1_right,tab1_firstSplit,opdet;
	static JSplitPane tab2_firstSplit;
	static int width, height;
	static JPanel options;
	static JComboBox<String> cb1,cb2;
	
	public static void initialize()
	{
		allFiles = new ArrayList<Matrix>();
	    indices = new ArrayList<Integer>();
	    numFiles = 0;
	    Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		width = (int) screen.getWidth();
		height = (int) screen.getHeight();	
	}
	
	public static void main(String[] a) 
	{
	    initialize();  
    	mainFrame = new JFrame();
    	splash = new JFrame();
    	splash.setTitle("Tejas Simulation Result Visualizer");
	    mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		mainFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
	    mainFrame.setTitle("Tejas Simulation Result Visualizer");
	    createTab2();
		createTab1();
		JTabbedPane tabs =  new JTabbedPane();
		String title = "Welcome to Tejas Visualizer";
		String toshow = "Want to see the manual first?";
	    int rc = JOptionPane.showConfirmDialog(null,
	    		toshow, title, JOptionPane.YES_NO_OPTION);
	    if(rc==0)
		{
	    	tabs.addTab("Manual",manual());
	        tabs.addTab("Tejas Visualization", tab1_firstSplit);
			tabs.addTab("Details Section", tab2_firstSplit);
			mainFrame.add(tabs, BorderLayout.CENTER);
			mainFrame.setVisible(true); 
		}
	    else
	    {
	        tabs.addTab("Tejas Visualization", tab1_firstSplit);
			tabs.addTab("Details Section", tab2_firstSplit);
			mainFrame.add(tabs, BorderLayout.CENTER);
			mainFrame.setVisible(true); 	
	    }
	}	
	
	public static JPanel manual()
	{
		JPanel panel = new JPanel(new GridLayout());
		JLabel l1 = new JLabel("This is a sample for manual page");
		JLabel l2 = new JLabel("This is a sample for manual page");
		JLabel l3 = new JLabel("This is a sample for manual page");
		JLabel l4 = new JLabel("This is a sample for manual page");
		
		panel.add(l1);
		panel.add(l2);
		panel.add(l3);
		panel.add(l4);
		
		return panel;
	}
	
	public static void createTab2()
	{
		tab2_firstSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		updateTab2();
	}
	
	public static void updateTab2()
	{
		JPanel drops = new JPanel(new GridLayout(2,6));
		final JSplitPane tab2_det = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT); 
		tab2_det.setLeftComponent(new JPanel());
		tab2_det.setRightComponent(new JPanel());		
		
		for(int i=0;i<6;i++)
			drops.add(getDummy());
		
		//Creating the drop down for benchmark
		final JComboBox<String> bench;
		String[] toadd = new String[numFiles+1];
		for(int i=0;i<numFiles;i++)
		{
			Matrix mat = allFiles.get(i);
			toadd[i] = mat.getName();
		}
		toadd[numFiles] = "Choose a Benchmark";
		bench = new JComboBox<String>(toadd);
		bench.setSelectedIndex(numFiles);
		drops.add(bench);
		
		//Creating the drop down for feature selection
		final JComboBox<String> features;
		String[] feat = null;
		if(numFiles>0)
		{
			Matrix mat = allFiles.get(0);
			feat = new String[mat.getNumCols()+1];
			for(int i=0;i<mat.getNumCols();i++)
			{
				feat[i] = mat.getNames()[i];
			}
			feat[mat.getNumCols()] = "Choose a Feature";
			features = new JComboBox<String>(feat);
			features.setSelectedIndex(mat.getNumCols());
		}
		else
		{
			feat = new String[1];
			feat[0] = "Choose a Feature";
			features = new JComboBox<String>(feat);
		}
		drops.add(getDummy());
		drops.add(features);
		drops.add(getDummy());
		
		//Creating button for showing details
		JButton show = new JButton("Show Details");
		show.addActionListener(new ActionListener() 
		{	
			public void actionPerformed(ActionEvent arg0) 
			{
				int i = bench.getSelectedIndex();
				int j = features.getSelectedIndex();
				if(i<numFiles && j<allFiles.get(0).getNumCols())
				{
					tab2_det.setLeftComponent(corrs(i,j));
					tab2_det.setRightComponent(dets(i,j));
					tab2_det.setDividerLocation(width/2);
				}
				else
				{
					JOptionPane.showMessageDialog(null, 
		                      "Please Make Proper Selections From Dropdown Lists", 
		                      "ALERT", 
		                      JOptionPane.WARNING_MESSAGE);
				}
			}
		});
		drops.add(show);
		JButton export = new JButton("Export Details");
		export.addActionListener(new ActionListener() 
		{	
			@Override
			public void actionPerformed(ActionEvent arg0) 
			{
				int i = bench.getSelectedIndex();
				int j = features.getSelectedIndex();
				if(i<numFiles && j<allFiles.get(0).getNumCols())
				{
					allFiles.get(i).exportData(j);
					JOptionPane.showMessageDialog(null, "Data Exported successfully!!",
							"Voila", JOptionPane.INFORMATION_MESSAGE);
				}
			}
		});
		drops.add(export);
		tab2_det.setDividerLocation(width/2);
		
		tab2_firstSplit.setTopComponent(drops);
		tab2_firstSplit.setBottomComponent(tab2_det);
		tab2_firstSplit.setDividerLocation(height/20);
	}
	
	public static JSplitPane corrs(int i,int j)
	{
		JSplitPane jsp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		
		//Creating Correlation Panel for Original Values
		Matrix mat = allFiles.get(i);
		String[] names = mat.getNames();
		int size = mat.getNumCols();
		double[] toadd = mat.getCorCol(j);
		JPanel panel = new JPanel(new GridLayout((size+1)*2,1));
		JLabel tab1label = new JLabel(" Correlation Vector for Original Values of: "+names[j]);
		tab1label.setHorizontalAlignment(JLabel.CENTER);
		panel.add(tab1label);
		panel.add(getDummy());
		for(int k=0;k<size;k++)
		{
			JPanel temp = new JPanel(new GridLayout(0,2));
			temp.add(new JLabel("     "+names[k]));
			temp.add(new JLabel("     :     "+toadd[k]));
			panel.add(temp);
		}
		for(int k=0;k<size;k++)
			panel.add(getDummy());
		jsp.setLeftComponent(panel);
		
		//Creating Correlation Panel for Epoched Values
		double[] newtoadd = mat.getEpochCorCol(j);
		JPanel npanel = new JPanel(new GridLayout((size+1)*2,1));
		JLabel tab2label = new JLabel("Correlation Vector for Epoched Values of: "+names[j]);
		tab2label.setHorizontalAlignment(JLabel.CENTER);
		npanel.add(tab2label);
		npanel.add(getDummy());
		for(int k=0;k<size;k++)
		{
			JPanel temp = new JPanel(new GridLayout(0,2));
			temp.add(new JLabel("     "+names[k]));
			temp.add(new JLabel(":  "+newtoadd[k]));
			npanel.add(temp);
		}
		for(int k=0;k<size;k++)
			npanel.add(getDummy());
		jsp.setBottomComponent(npanel);
		jsp.setDividerLocation(width/4);
		return jsp;
	}

	public static JSplitPane dets(int i, int j)
	{
		JSplitPane jsp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		
		//Creating Details for Feature
		Matrix mat = allFiles.get(i);
		String[] featureNames = mat.getNames();
		String[] names = mat.getDetNames();
		
		double[] toadd = mat.getDetails(j);
		int size = toadd.length;
		JPanel panel = new JPanel(new GridLayout((size+1)*5,1));
		JLabel tab3label = new JLabel("Details for Original Values of: "+featureNames[j]);
		tab3label.setHorizontalAlignment(JLabel.CENTER);
		panel.add(tab3label);
		panel.add(getDummy());
		for(int k=0;k<size;k++)
		{
			JPanel temp = new JPanel(new GridLayout(0,2));
			temp.add(new JLabel("     "+names[k]));
			temp.add(new JLabel("     :     "+toadd[k]));
			panel.add(temp);
		}
		double[] newtoadd = mat.getEpochedDetails(j);
		JLabel tab4label = new JLabel("Details for Epoched Values of: "+featureNames[j]);
		tab4label.setHorizontalAlignment(JLabel.CENTER);
		panel.add(tab4label);
		panel.add(getDummy());
		for(int k=0;k<size;k++)
		{
			JPanel temp = new JPanel(new GridLayout(0,2));
			temp.add(new JLabel("     "+names[k]));
			temp.add(new JLabel(":  "+newtoadd[k]));
			panel.add(temp);
		}

		jsp.setLeftComponent(panel);
		
		//Generating details of benchmark
		size = mat.getNumCols();
		names = mat.getNames();
		newtoadd = mat.getLastRow();
		JPanel npanel = new JPanel(new GridLayout((size+1)*2,1));
		JLabel tab2label = new JLabel("Details of Benchmark: "+mat.getName());
		tab2label.setHorizontalAlignment(JLabel.CENTER);
		npanel.add(tab2label);
		npanel.add(getDummy());
		JLabel templabel = new JLabel("Final values of all features");
		templabel.setHorizontalAlignment(JLabel.CENTER);
		npanel.add(templabel);
		
		npanel.add(getDummy());
		for(int k=0;k<size;k++)
		{
			JPanel temp = new JPanel(new GridLayout(0,2));
			temp.add(new JLabel("     "+names[k]));
			temp.add(new JLabel(":  "+newtoadd[k]));
			npanel.add(temp);
		}
		JLabel l1,l2,l3,l4;
		l1 = new JLabel(" Data Hazard Factor : "+newtoadd[size-4]/newtoadd[4]);
		l1.setHorizontalAlignment(JLabel.CENTER);
		//npanel.add(l1);
		l2 = new JLabel(" Memory Hazard Factor : "+newtoadd[size-3]/newtoadd[4]);
		l2.setHorizontalAlignment(JLabel.CENTER);
		//npanel.add(l2);
		l3 = new JLabel(" Control Hazard Factor : "+newtoadd[size-2]/newtoadd[4]);
		l3.setHorizontalAlignment(JLabel.CENTER);
		//npanel.add(l3);
		l4 = new JLabel(" Total Hazard Factor : "+newtoadd[size-1]/newtoadd[4]);
		l4.setHorizontalAlignment(JLabel.CENTER);
		//npanel.add(l4);
		
		for(int k=0;k<size-6;k++)
			npanel.add(getDummy());

		jsp.setRightComponent(npanel);
		jsp.setDividerLocation(width/4);
		return jsp;
	}
	
	public static void createTab1()
	{
		formAndCards = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		tab1_right = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		tab1_firstSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		opdet = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		options = new JPanel(new GridLayout(6,1));
		
		createOptions();
			
		formAndCards.setTopComponent(createForm());
		formAndCards.setBottomComponent(createCards());
		formAndCards.setDividerLocation(height/3);
		
		opdet.setTopComponent(options);
		opdet.setBottomComponent(new JPanel());
		opdet.setDividerLocation(height/4);
		
		tab1_right.setLeftComponent(opdet);
		tab1_right.setRightComponent(createGraph());
		tab1_right.setDividerLocation((width/5));
		  
		tab1_firstSplit.setDividerLocation((int)width/4);
		tab1_firstSplit.setLeftComponent(formAndCards);
		tab1_firstSplit.setRightComponent(tab1_right); 
	}
	
	public static void createOptions()
	{
		options.removeAll();
		indices.clear();
		
		JLabel l = new JLabel("Feature Selection Section");
		l.setFont(new Font("Serif", Font.BOLD, 20));
		l.setHorizontalAlignment(JLabel.CENTER);
		
		options.add(l);
		JLabel l1 = new JLabel("All features will be plotted against the number of cycles");
		l1.setFont(new Font("Serif", Font.PLAIN, 14));
		options.add(l1);
		JButton but = new JButton("reset");
		but.addActionListener(new ActionListener() 
		{	
			public void actionPerformed(ActionEvent arg0) 
			{
				indices.clear();
				tab1_right.setRightComponent(createGraph());
				tab1_right.setDividerLocation(width/5);
			}
		});
		final JComboBox<String> jcb;
		if(numFiles>0)
		{
			Matrix mat = allFiles.get(0);
			final int size = mat.getNumCols();
			String[] toadd = new String[size+1];
			for(int i=0;i<size;i++)
				toadd[i] = mat.getNames()[i];
			toadd[size] = "Choose Feature";
			jcb = new JComboBox<String>(toadd);
			jcb.setSelectedIndex(size);
			jcb.addActionListener(new ActionListener() 
			{
				public void actionPerformed(ActionEvent arg0) 
				{
					if(jcb.getSelectedIndex()<size && !indices.contains(jcb.getSelectedIndex()))
						indices.add(jcb.getSelectedIndex());
					tab1_right.setRightComponent(createGraph());
					tab1_right.setDividerLocation(width/5);
				}
			});
		}
		else
		{
			String[] toadd = new String[1];
			toadd[0] = "Choose Feature";
			jcb = new JComboBox<String>(toadd);
		}
		options.add(jcb);
		/*JButton show = new JButton("Show Details");
		show.addActionListener(new ActionListener() 
		{
			public void actionPerformed(ActionEvent arg0) 
			{
				opdet.setBottomComponent(createDet(jcb.getSelectedIndex()));
				opdet.setDividerLocation(height/4);
			}
		});
		options.add(show);*/
		options.add(getDummy());
		options.add(getDummy());
		options.add(but);	
		tab1_right.setLeftComponent(opdet);
		tab1_right.setRightComponent(createGraph());
		tab1_right.setDividerLocation(width/5);
	}
	
	/*
	public static JScrollPane createDet(int index)
	{
		Matrix mat = allFiles.get(0);
		String[] names = mat.getNames();
		int size = mat.getNumCols();
		double[] toadd = mat.getCorCol(index);
		JPanel panel = new JPanel(new GridLayout(size+1,3));
		panel.add(new JLabel("Correlation Vector for "));
		panel.add(new JLabel(":"));
		panel.add(new JLabel(""+names[index]));
		for(int i=0;i<size;i++)
		{
			panel.add(new JLabel(names[i]));
			panel.add(new JLabel(":"));
			panel.add(new JLabel(""+toadd[i]));
		}
		return new JScrollPane(panel);
	}
	*/
	
	public static JPanel createForm()
	{
		JPanel panel; 
		JLabel label = new JLabel("<html>Enter the absolute path of file written by Tejas that "
				+ "contains all the values of energy and instruction per cycles</html>");
		label.setBounds(width/50, 0,width/5,height/10);
		JLabel label1 = new JLabel();
		label1.setText("Filename:");
		label1.setBounds(width/50, height/8, width/20, height/30);
		final JFileChooser fileChooser = new JFileChooser();
		FileNameExtensionFilter filter = new FileNameExtensionFilter(
		        "Tejas Dump Files", "csv");
		fileChooser.setFileFilter(filter);
		final JTextField filename = new JTextField(50);
		filename.setBounds(width/10,height/8,width/10,height/30);
		filename.addFocusListener(new FocusListener() {
			
			@Override
			public void focusLost(FocusEvent arg0) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void focusGained(FocusEvent arg0) 
			{
				int returnVal = fileChooser.showOpenDialog(filename);
				 
	            if (returnVal == JFileChooser.APPROVE_OPTION) {
	                filename.setText(fileChooser.getSelectedFile().getAbsolutePath());	
	            }
	            filename.setFocusable(false);
	            filename.setFocusable(true);
			}
		});
		JLabel label2 = new JLabel();
		label2.setText("Epoch:");
		label2.setBounds(width/50, height*2/11, width/10, height/30);
		final JTextField epoch = new JTextField(15);
		epoch.setBounds(width/10,height*2/11,width/10,height/30);
		JButton add;
		add=new JButton("ADD");
		add.setBounds(width/10,height*10/43,width/10,height/30);
		add.addActionListener(new ActionListener() { 
			public void actionPerformed(ActionEvent e) { 
				try
				{
					int epochs = Integer.parseInt(epoch.getText());
					filename.setText(fileChooser.getSelectedFile().getAbsolutePath());     
					String file = filename.getText();
					addFile(file,epochs);
				}
				catch(Exception ex)
				{
					JOptionPane.showMessageDialog(null, 
		                      "Either the file you entered is corrupted or epochs is not multiple of 1000", 
		                      "ALERT", 
		                      JOptionPane.WARNING_MESSAGE);
				}
			} 
		});
		JLabel label3 = new JLabel();
		label3.setText("Note:     Epoch must be a multiple of 1000");
		label3.setBounds(width/50, height*2/7, width/5, height/30);
		panel=new JPanel(null);
		panel.add(fileChooser);
		panel.add(label);
		panel.add(label1);
		panel.add(filename);
		panel.add(label2);
		panel.add(epoch);
		panel.add(label3);
		panel.add(add);
		return panel;
	}	
	
	public static void addFile(String file, int epochs)
	{
		  Matrix mat = new Matrix(file,epochs);
		  if(mat.isValid())
		  {
			  numFiles++;
			  allFiles.add(mat);
			  formAndCards.setBottomComponent(createCards());
			  formAndCards.setDividerLocation(height/3);
		  }
		  else
		  {
			  JOptionPane.showMessageDialog(null, 
                      "Either the file you entered is corrupted or epochs is not multiple of 1000", 
                      "ALERT", 
                      JOptionPane.WARNING_MESSAGE);
		  }
	}
	
	public static JScrollPane createCards()
	{
		JPanel panel = new JPanel();
		if(numFiles<4)
			panel.setLayout(new GridLayout(10,3));
		else
			panel.setLayout(new GridLayout(0,3));
		for(int i=0;i<numFiles;i++)
		{
			final Matrix mat = allFiles.get(i);
			JTextArea label = new JTextArea();
			label.setText(mat.getName());
			label.setSize(width/13,height/30);
			label.setLineWrap(true);
			panel.add(label);
			
			final JButton show = new JButton();
			if(mat.isVisible())
				show.setText("HIDE");
			else
				show.setText("SHOW");
			show.addActionListener(new ActionListener() { 
				public void actionPerformed(ActionEvent e) { 
					mat.setVisible(!mat.isVisible());
					if(mat.isVisible())
						show.setText("HIDE");
					else
						show.setText("SHOW");
					if(indices.size()>0)
						tab1_right.setRightComponent(createGraph());
					tab1_right.setDividerLocation((width/5));
				} 
			});
			panel.add(show);
			
			JButton delete = new JButton("Delete");
			delete.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent e) 
				{
					allFiles.remove(mat);
					numFiles--;
					formAndCards.setBottomComponent(createCards());
					formAndCards.setDividerLocation(height/3);
					if(indices.size()>0)
						tab1_right.setRightComponent(createGraph());
					tab1_right.setDividerLocation((width/5));
				}
			});
			panel.add(delete);
			  
		} 
		for(int i=0;i<10-numFiles;i++)
		{
			panel.add(getDummy());
			panel.add(getDummy());
			panel.add(getDummy());
		}
		createOptions();
		updateTab2();
		return new JScrollPane(panel);
	} 
	
	/*
	public static JPanel createIPCOptions()
	{
		JPanel panel = new JPanel(new GridLayout(10,1));
		final JCheckBox compute = new JCheckBox("Compute IPC vs Number of Cycles");
		final JCheckBox memory = new JCheckBox("Memory IPC vs Number of Cycles");
		final JCheckBox branch = new JCheckBox("Branch IPC vs Number of Cycles");
		final JCheckBox sync = new JCheckBox("Synchronization IPC vs Number of Cycles");
		final JCheckBox totalipc = new JCheckBox("Total IPC vs Number of Cycles");
		JButton ipc = new JButton("Generate");
		ipc.addActionListener(new ActionListener(){

			public void actionPerformed(ActionEvent e) 
			{
				options.setBottomComponent(createEnergyOptions());
				options.setDividerLocation(height*9/20);
				indices.clear();
				if(compute.isSelected())
					indices.add(0);
				if(memory.isSelected())
					indices.add(1);
				if(branch.isSelected())
					indices.add(2);
				if(sync.isSelected())
					indices.add(3);
				if(totalipc.isSelected())
					indices.add(4);
				if(indices.size()>0)
					graph.setTopComponent(createGraph());
			}
			
		});
		panel.add(getDummy());
		panel.add(compute);
		panel.add(memory);
		panel.add(branch);
		panel.add(sync);
		panel.add(totalipc);
		panel.add(getDummy());
		panel.add(getDummy());
		panel.add(getDummy());
		panel.add(ipc);
		return panel;
	}
	
	public static JPanel createEnergyOptions()
	{
		JPanel panel = new JPanel(new GridLayout(10,1));
		final JCheckBox ICache = new JCheckBox("ICache Energy vs Number of Cycles");
		final JCheckBox ITLB = new JCheckBox("ITLB Energy vs Number of Cycles");
		final JCheckBox DCache = new JCheckBox("DCache Energy vs Number of Cycles");
		final JCheckBox DTLB = new JCheckBox("DTLB Energy vs Number of Cycles");
		final JCheckBox pipeline = new JCheckBox("Pipeline Energy vs Number of Cycles");
		final JCheckBox totalEnergy = new JCheckBox("Total Energy vs Number of Cycles");
		JButton ipc = new JButton("Generate");
		ipc.addActionListener(new ActionListener(){

			public void actionPerformed(ActionEvent e) 
			{
				options.setTopComponent(createIPCOptions());
				options.setDividerLocation(height*9/20);
				indices.clear();
				if(ICache.isSelected())
					indices.add(5);
				if(ITLB.isSelected())
					indices.add(6);
				if(DCache.isSelected())
					indices.add(7);
				if(DTLB.isSelected())
					indices.add(8);
				if(pipeline.isSelected())
					indices.add(9);
				if(totalEnergy.isSelected())
					indices.add(10);
				if(indices.size()>0)
					graph.setTopComponent(createGraph());
			}
			
		});
		panel.add(getDummy());
		panel.add(ICache);
		panel.add(ITLB);
		panel.add(DCache);
		panel.add(DTLB);
		panel.add(pipeline);
		panel.add(totalEnergy);
		panel.add(getDummy());
		panel.add(getDummy());
		panel.add(ipc);
		return panel;
	}
*/
	
	public static JPanel createGraph() 
    {
        String chartTitle = "Features vs. Cycles Plot";
        String xAxisLabel = "Number of Cycles";
        String yAxisLabel = "Values";       
        XYDataset dataset = createDataset();
     
        JFreeChart chart = ChartFactory.createXYLineChart(chartTitle,xAxisLabel, yAxisLabel, dataset, PlotOrientation.VERTICAL, true, true, false);
        return new ChartPanel(chart);
    }
	
	public static XYDataset createDataset() 
    {
        XYSeriesCollection dataset = new XYSeriesCollection();
        for(int i=0;i<numFiles;i++)
        {
        	Matrix mat = allFiles.get(i);
        	if(mat.isVisible())
        	{
        		for(int j=0;j<indices.size();j++)
        			dataset.addSeries(mat.getSeries(indices.get(j)));
        	}
        }
        return dataset;
    }
	
	public static JLabel getDummy()
	{
		return new JLabel("");
	}
}