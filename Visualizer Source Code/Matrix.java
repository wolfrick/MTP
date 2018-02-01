package tejas;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.util.*;

import javax.swing.JOptionPane;

import org.jfree.data.xy.XYSeries;

public class Matrix
{
	List<List<Double>> originalValues, originalCor, epochedValues, epochedCor;
	List<String> names;
	int row, epochrow,column;
	boolean valid, visible;
	String file;
	int epochs;
	
	public Matrix(String filename, int epoch)
	{
		epochs = epoch/1000;
		File fp = new File(filename);
		file = fp.getName();
		originalValues =  new ArrayList<List<Double>>();
		originalCor =  new ArrayList<List<Double>>();
		epochedValues =  new ArrayList<List<Double>>();
		epochedCor =  new ArrayList<List<Double>>();
		names = new ArrayList<String>();
		row = 0;
		epochrow = 0;
		column = 0;
		valid = true;
		visible = false;
		try 
		{
			Scanner input = new Scanner(fp);
			if(input.hasNextLine())
			{
			    String colReader = input.nextLine();
			    names = Arrays.asList(colReader.split(","));
			}
			while(input.hasNextLine())
			{
				row++;
			    String colReader = input.nextLine();
			    String[] inp = colReader.split(",");
			    List<Double> col = new ArrayList<Double>();
			    for(int i=0;i<inp.length;i++)
			    	col.add(Double.parseDouble(inp[i]));
			    originalValues.add(col);
			}
			input.close();
			column = originalValues.get(0).size();
			genCorrelation();
			genEpochs();
		}
		catch (FileNotFoundException e) 
		{
			valid = false;
		}
	}
	
	public void genCorrelation()
	{
		for(int i=0;i<column;i++)
		{
			ArrayList<Double> col = new ArrayList<Double>();
			double[] coli = getColumn(i);
			for(int j=0;j<column;j++)
				col.add(Correlation(coli,getColumn(j)));
			originalCor.add(col);
		}
	}
	
	public void genEpochedCorrelation()
	{
		for(int i=0;i<column;i++)
		{
			ArrayList<Double> col = new ArrayList<Double>();
			double[] coli = getEpochedColumn(i);
			for(int j=0;j<column;j++)
				col.add(Correlation(coli,getEpochedColumn(j)));
			epochedCor.add(col);
		}
	}
	
	public void genEpochs()
	{
		for(int i=epochs-1;i<row;i=i+epochs)
		{
			List<Double> toadd = new ArrayList<Double>();
			for(int j=0;j<column;j++)
			{
				double preval = 0;
				double curval = originalValues.get(i).get(j);
				if(i!=epochs-1)
					preval = originalValues.get(i-epochs).get(j);
				toadd.add((curval-preval)/(epochs*1000));
			}
			epochedValues.add(toadd);
			epochrow++;
		}
		genEpochedCorrelation();
	}
	
	public String[] getNames()
	{
		String[] toret = new String[column+1];
		for(int i=0;i<column;i++)
			toret[i] = names.get(i);
		toret[column] = "Choose the parameter";
		return toret;
	}
	
	public int getNumCols()
	{
		return column;
	}
	
 	public XYSeries getSeries(int index)
	{
		String name = file + " -"+names.get(index);
		XYSeries series = new XYSeries(name);
		double[] arr = getEpochedColumn(index);
		double currIn=0 ;
		series.add(0,0);
		for(int i=0;i<epochrow;i++)
		{
			currIn = (i+1)*epochs*1000;
			series.add(currIn,arr[i]);
		}
		return series;
	}

	public boolean isValid()
	{
		if(epochs > 0 && row>0)
			return valid;
		else
			return false;
	}

	public double[] getColumn(int col)
	{
		double[] value = new double[row];
		for(int i=0;i<row;i++)
			value[i]=originalValues.get(i).get(col);
		return value;
	}
	
	public double[] getLastRow()
	{
		double[] value = new double[column];
		for(int i=0;i<column;i++)
			value[i]=originalValues.get(row-1).get(i);
		return value;
	}
	
	public double[] getEpochedColumn(int col)
	{
		double[] value = new double[epochrow];
		for(int i=0;i<epochrow;i++)
			value[i]=epochedValues.get(i).get(col);
		return value;
	}
	
	public double[] getCorCol(int col)
	{
		double[] value = new double[column];
		for(int i=0;i<column;i++)
			value[i]=originalCor.get(col).get(i);
		return value;
	}
	
	public double[] getEpochCorCol(int col)
	{
		double[] value = new double[column];
		for(int i=0;i<column;i++)
			value[i]=epochedCor.get(col).get(i);
		return value;
	}
	
	public String[] getDetNames()
	{
		String[] toret = new String[3];
		toret[0] = "Minimum Value";
		toret[1] = "Maximum Value";
		toret[2] = "Average Value";
		return toret;
	}

	public double[] getDetails(int col)
	{
		double[] arr = getColumn(col);
		double min=arr[0], max=arr[0], avg=arr[0];
		for(int i=0;i<row;i++)
		{
			if(arr[i]<min)
				min=arr[i];
			else if(arr[i]>max)
				max = arr[i];
			avg+=arr[i];
		}
		avg = avg/row;
		double[] tosend = new double[3];
		tosend[0] = min;
		tosend[1] = max;
		tosend[2] = avg;
		return tosend;
	}
	
	public double[] getEpochedDetails(int col)
	{
		double[] arr = getEpochedColumn(col);
		double min=arr[0], max=arr[0], avg=arr[0];
		for(int i=0;i<epochrow;i++)
		{
			if(arr[i]<min)
				min=arr[i];
			else if(arr[i]>max)
				max = arr[i];
			avg+=arr[i];
		}
		avg = avg/epochrow;
		double[] tosend = new double[3];
		tosend[0] = min;
		tosend[1] = max;
		tosend[2] = avg;
		return tosend;
	}
	
	public void printValues()
	{
		for(int i=0;i<row;i++)
		{
			for(int j=0;j<column;j++)
				System.out.print(originalValues.get(i).get(j)+" ");
			System.out.println("");
		}
	}
	
	public void printCor()
	{
		for(int i=0;i<column;i++)
		{
			for(int j=0;j<column;j++)
				System.out.print(originalCor.get(i).get(j)+" ");
			System.out.println("");
		}
	}
	
	public double Correlation(double[] a, double[] b) 
	{
	    double sum_a = 0.0;
	    double sum_b = 0.0;
	    double s_a_a = 0.0;
	    double s_b_b = 0.0;
	    double s_a_b = 0.0;

	    int n = a.length;

	    for(int i = 0; i < n; ++i) 
	    {
	    	double x = a[i];
	    	double y = b[i];

	    	sum_a += x;
	    	sum_b += y;
	    	s_a_a += x * x;
	    	s_b_b += y * y;
	    	s_a_b += x * y;
	    }

	    double cov = n*s_a_b - sum_a * sum_b;
	    double sigmaa = Math.sqrt(n*s_a_a  -  sum_a* sum_a);
	    double sigmab = Math.sqrt(n*s_b_b  -  sum_b* sum_b);
	    if(sigmaa==0 || sigmab==0)
	    	return 0;
	    return cov / sigmaa / sigmab;
	}
	
	public void exportData(int j)
	{
		String fname = this.getName()+names.get(j)+".txt";
		try{
			FileWriter out = new FileWriter(fname);
			out.write("Details for "+names.get(j)+" feature of "+file+" benchmark.\n");
			out.write("Correlation Vector for Original Values:\n");
			double[] toadd = this.getCorCol(j);
			int size = this.getNumCols();
			for(int i=0;i<size;i++)
				out.write(names.get(i)+":"+toadd[i]+"\n");
			out.write("\n\nCorrelation Vector for Epoched Values:\n");
			toadd = this.getEpochCorCol(j);
			for(int i=0;i<size;i++)
				out.write(names.get(i)+":"+toadd[i]+"\n");
			out.write("\n\nDetails for Original Values:\n");
			toadd = this.getDetails(j);
			String[] adName = this.getDetNames();
			size = adName.length;
			for(int i=0;i<size;i++)
				out.write(adName[i]+":"+toadd[i]+"\n");
			out.write("\n\nDetails for Epoched Values:\n");
			toadd = this.getEpochedDetails(j);
			for(int i=0;i<size;i++)
				out.write(adName[i]+":"+toadd[i]+"\n");
			out.close();
		}
		catch(Exception e)
		{
			JOptionPane.showMessageDialog(null, "ERROR",
					"Sorry!!! But this file already exists!", JOptionPane.INFORMATION_MESSAGE);
		}
	}
	
	public String getName()
	{
		return this.file;
	}
	
	public int getEpochs()
	{
		return this.epochs;
	}
	
	public boolean isVisible()
	{
		return visible;
	}

	public void setVisible(boolean isvisible)
	{
		this.visible = isvisible;
	}
}