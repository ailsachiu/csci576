import java.awt.*;
import java.awt.image.*;
import java.io.*;
import javax.swing.*;
import java.util.ArrayList;


public class imageDecoder {
	static final int WIDTH = 512;
	static final int HEIGHT = 512;
	
	private static String fileName;
	private static int numCoefficients = WIDTH*HEIGHT; 	// default

	// Original image into 3 channels
	private static int[][] yChannel = new int[HEIGHT][WIDTH];
	private static int[][] crChannel = new int[HEIGHT][WIDTH];
	private static int[][] cbChannel = new int[HEIGHT][WIDTH];

	// Original image 8x8 blocks
	private static ArrayList<int[][]> yBlocks = new ArrayList<int[][]>(); 
	private static ArrayList<int[][]> crBlocks = new ArrayList<int[][]>();
	private static ArrayList<int[][]> cbBlocks = new ArrayList<int[][]>();

	// DCT transform, stores F(u,v) 8x8 blocks
	private static ArrayList<int[][]> yBlocksDCT = new ArrayList<int[][]>();
	private static ArrayList<int[][]> crBlocksDCT = new ArrayList<int[][]>(); 
	private static ArrayList<int[][]> cbBlocksDCT = new ArrayList<int[][]>(); 

	// DCT inverse image blocks
	private static ArrayList<int[][]> yBlocksIDCT = new ArrayList<int[][]>(); 
	private static ArrayList<int[][]> crBlocksIDCT = new ArrayList<int[][]>();
	private static ArrayList<int[][]> cbBlocksIDCT = new ArrayList<int[][]>();

	public static void main(String[] args) {
		if(args.length < 2) {
			System.out.println("Use: java imageDecoder [input file] [number of coefficients]");
			System.exit(0);
		}

		fileName = args[0];
		
	    BufferedImage img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);

	    try {
		    File file = new File(fileName);
		    InputStream is = new FileInputStream(file);

		    long len = file.length();
		    byte[] bytes = new byte[(int)len];
		    
		    int offset = 0;
	        int numRead = 0;
	        while (offset < bytes.length && (numRead=is.read(bytes, offset, bytes.length-offset)) >= 0) {
	            offset += numRead;
	        }

	    		
	    	int ind = 0;
			for(int y = 0; y < HEIGHT; y++){
		
				for(int x = 0; x < WIDTH; x++){
			 
					byte a = 0;
					byte r = bytes[ind];
					byte g = bytes[ind+HEIGHT*WIDTH];
					byte b = bytes[ind+HEIGHT*WIDTH*2]; 
					
					int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
					//int pix = ((a << 24) + (r << 16) + (g << 8) + b);
					img.setRGB(x,y,pix);
					ind++;
				}
			}
			
			
	    } catch (FileNotFoundException e) {
	      e.printStackTrace();
	    } catch (IOException e) {
	      e.printStackTrace();
	    }

	    
		int option = Integer.parseInt(args[1]);
		if(option == -1) {
			System.out.println("Progressive analysis");
			progressiveAnalysis(img);
			System.out.println("\nDone");
		}
		else {
		   	numCoefficients = option;

		   	BufferedImage dctImg = dctRepresentation(img,numCoefficients);
		   	BufferedImage dwtImg = dwtRepresentation(img,numCoefficients);

		    // Use a label to display the image
		    JFrame frame = new JFrame();
		    JLabel dctLabel = new JLabel(new ImageIcon(dctImg));
		    JLabel dwtLabel = new JLabel(new ImageIcon(dwtImg));
		    JPanel jpanel = new JPanel();
		    jpanel.add(dctLabel);
		    jpanel.add(dwtLabel);

		    frame.getContentPane().add(jpanel, BorderLayout.CENTER);
		    frame.pack();
		    frame.setVisible(true);
		}

   }

	public static void progressiveAnalysis(BufferedImage img) {
		JFrame frame = new JFrame();
		System.out.println("Iteration: ");
		for(int i=1; i <= 64; i++) {
			BufferedImage dctImg = dctRepresentation(img,i*4096);
	   		BufferedImage dwtImg = dwtRepresentation(img,i*4096);

		    JLabel dctLabel = new JLabel(new ImageIcon(dctImg));
		    JLabel dwtLabel = new JLabel(new ImageIcon(dwtImg));
		    JPanel jpanel = new JPanel();
		    jpanel.add(dctLabel);
		    jpanel.add(dwtLabel);

		    frame.getContentPane().add(jpanel, BorderLayout.CENTER);
		    frame.pack();
		    frame.setVisible(true);
	   		try {
	   			Thread.sleep(2000);
	   		}
	   		catch(InterruptedException e) {
	   			e.printStackTrace();
	   		}
	   		System.out.print(i + " ");
		}
	}

	public static BufferedImage dwtRepresentation(BufferedImage img, int numCoefficients) {
		convertToYCrCb(img);
	   	double[][] doubleYChannel = new double[HEIGHT][WIDTH];
	   	double[][] doubleCrChannel = new double[HEIGHT][WIDTH];
	   	double[][] doubleCbChannel = new double[HEIGHT][WIDTH];
	   	for(int j=0; j < HEIGHT; j++) {
	   		for(int i=0; i < WIDTH; i++) {
	   			doubleYChannel[j][i] = (double)yChannel[j][i];
	   			doubleCrChannel[j][i] = (double)crChannel[j][i];
	   			doubleCbChannel[j][i] = (double)cbChannel[j][i];
	   		}
	   	}
	    
	    int numIterations = 9; // Log(512) = 9
	    dwt(doubleYChannel,numIterations); 
	    dwt(doubleCrChannel,numIterations);
	    dwt(doubleCbChannel,numIterations);

	    zeroDWT(doubleYChannel,numCoefficients);
	   	zeroDWT(doubleCrChannel,numCoefficients);
	   	zeroDWT(doubleCbChannel,numCoefficients);

	   	idwt(doubleYChannel,numIterations);
	   	idwt(doubleCrChannel,numIterations);
	   	idwt(doubleCbChannel,numIterations);

		
		for(int j=0; j < HEIGHT; j++) {
	   		for(int i=0; i < WIDTH; i++) {
	   			yChannel[j][i] = (int)doubleYChannel[j][i];
	   			crChannel[j][i] = (int)doubleCrChannel[j][i];
	   			cbChannel[j][i] = (int)doubleCbChannel[j][i];
	   		}
	   	}

	   	return convertToRgbImage(yChannel,crChannel,cbChannel);
	}

	public static BufferedImage dctRepresentation(BufferedImage img, int numCoefficients) {
		convertToYCrCb(img);
	    splitIntoBlocks(yChannel,yBlocks);
	    splitIntoBlocks(crChannel,crBlocks);
	    splitIntoBlocks(cbChannel,cbBlocks);

	    dct(yBlocks,yBlocksDCT);
	    dct(crBlocks,crBlocksDCT);
	    dct(cbBlocks,cbBlocksDCT);

	    zeroDCT(yBlocksDCT,numCoefficients);
	    zeroDCT(crBlocksDCT,numCoefficients);
	    zeroDCT(cbBlocksDCT,numCoefficients);

	    idct(yBlocksDCT,yBlocksIDCT);
	    idct(crBlocksDCT,crBlocksIDCT);
	    idct(cbBlocksDCT,cbBlocksIDCT);


	    int[][] newYchannel = combineToArray(yBlocksIDCT);
	    int[][] newCrChannel = combineToArray(crBlocksIDCT);
	   	int[][] newCbChannel = combineToArray(cbBlocksIDCT);

	   	return convertToRgbImage(newYchannel,newCrChannel,newCbChannel);
	}

	public static void convertToYCrCb(BufferedImage img) {
		for(int y=0; y < HEIGHT; y++) {
			for(int x=0; x < WIDTH; x++) {
				Color c = new Color(img.getRGB(x,y));
				int[] ycrcb = rgbTOycrcb(c.getRed(), c.getGreen(), c.getBlue());

				yChannel[y][x] = ycrcb[0];
				crChannel[y][x] = ycrcb[1];
				cbChannel[y][x] = ycrcb[2];
			}
		}
	}

	public static int[] rgbTOycrcb(int r, int g, int b) {
		int y = (int)(0.299*r + 0.587*g + 0.144*b);
		int cb = (int)(-0.159*r - 0.332*g + 0.05*b);
		int cr = (int)(0.50*r - 0.419*g - 0.081*b);

		int[] ycrcb = new int[3];
		ycrcb[0] = y;
		ycrcb[1] = cr;
		ycrcb[2] = cb; 
		return ycrcb;
	}

	public static void splitIntoBlocks(int[][] array, ArrayList<int[][]> blocks) {
		blocks.clear();

		int x = 0;
		int y = 0;

		while(y < HEIGHT) {
			int[][] block = new int[8][8];
			for(int j=0; j < 8; j++) {
				for(int i=0; i < 8; i++) {
					block[j][i] = array[y+j][x+i]; 
				}
			}
			blocks.add(block);
			if(x == 504) {
				x = 0;
				y += 8;
			}
			else 
				x += 8;
		}
	}

	/**
	 * Create DCT transform F(u,v) for given blocks and store in dctBlocks
	 */
	public static void dct(ArrayList<int[][]> blocks, ArrayList<int[][]> dctBlocks) {
		if(blocks == null || blocks.isEmpty())
			return;

		dctBlocks.clear();

		double[][] cosine = new double[8][8];
		for(int u=0; u < 8; u++) {
			for(int x=0; x < 8; x++) {
				cosine[u][x] = Math.cos((2*x+1)*Math.PI*u/16.0);
			}
		}

		// DCT to transform f(x,y) to F(u,v)
		for(int[][] block : blocks) {
			int[][] dctBlock = new int[8][8];	// new block

			for(int v=0; v < 8; v++) {	
				for(int u=0; u < 8; u++) {
					double cu = 1.0;
					double cv = 1.0;
					if(v == 0)
						cv = 1.0/Math.sqrt(2);
					if(u == 0)
						cu = 1.0/Math.sqrt(2);

					double sum = 0.0;
					for(int y=0; y < 8; y++) {
						for(int x=0; x < 8; x++) {
							// Sum the (x,y) values							
							sum += block[y][x]*cosine[u][x]*cosine[v][y];
						}
					}

					dctBlock[v][u] = (int)(0.25*cu*cv*sum);
				}
			}
			dctBlocks.add(dctBlock);
		}
	}

	/**
	 * Get inverse from dctBlocks and store in blocksIDCT
	 */
	public static void idct(ArrayList<int[][]> dctBlocks, ArrayList<int[][]> blocksIDCT) {
		if(dctBlocks == null || dctBlocks.isEmpty())
			return;

		blocksIDCT.clear();

		double[][] cosine = new double[8][8];
		for(int u=0; u < 8; u++) {
			for(int x=0; x < 8; x++) {
	 			cosine[u][x] = Math.cos((2*x+1)*Math.PI*u/16.0);
			}
		}

		for(int i=0; i < dctBlocks.size(); i++) {
			int[][] block = new int[8][8];

			for(int y=0; y < 8; y++) {
				for(int x=0; x < 8; x++) {
					double sum = 0.0;
					for(int v=0; v < 8; v++) {
						for(int u=0; u < 8; u++) {
							double cu = 1.0;
							double cv = 1.0;
							if(v == 0)
								cv = 1.0/Math.sqrt(2);
							if(u == 0)
								cu = 1.0/Math.sqrt(2);

							// Sum the (u,v) values
							sum += cu*cv*dctBlocks.get(i)[v][u]*cosine[u][x]*cosine[v][y];
						}
					}
					block[y][x] = (int)(0.25*sum);

				}
			}
			blocksIDCT.add(block);
		}
	}

	/**
	 * Traverse dctBlocks; for each block, keep the first m coefficients, where m = n / 4096,  and zero out the rest
	 */
	public static void zeroDCT(ArrayList<int[][]> dctBlocks, int num) {
		if(dctBlocks.isEmpty() || dctBlocks == null)
			return;

		if(num >= WIDTH*HEIGHT)	{ // Keep all coefficients 
			return;
		}
		
		int	m = (int)(num / 4096);
		
		for(int[][] block : dctBlocks) {
			// Traverse in zig-zag
			int count = 0;
			int y = 0;
			int x = 0;

			for(int i=0; i < 64; i++) {
				if(count < m) {
					count++;
				}
				else {
					block[y][x] = 0;
				}
				if((x+y)%2 == 0) {
					// Even stripes
					if(x < 7)
						x++;
					else
						y += 2;
					if(y > 0)
						y--;
				}
				else {
					// Odd stripes
					if(y < 7)
						y++;
					else
						x +=2;
					if(x > 0)
						x--;
				}
			}
		}
	}

	/**
	 * Combine list of blocks back into a single 2D array
	 */
	public static int[][] combineToArray(ArrayList<int[][]> blocks) {
		int[][] array = new int[HEIGHT][WIDTH];
		int ax = 0;
		int ay = 0;

		for(int i=0; i < blocks.size(); i++) {
			int[][] block = blocks.get(i);

			for(int y=0; y < 8; y++) {
				for(int x=0; x < 8; x++) {
					array[ay+y][ax+x] = block[y][x];
				}
			}

			if(ax == 504) {
				ax = 0;
				ay += 8;
			}
			else
				ax += 8;
		}

		return array;
	}

	public static BufferedImage convertToRgbImage(int[][] yChan, int[][] crChan, int[][] cbChan) {
		BufferedImage img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);

		for(int y=0; y < HEIGHT; y++) {
			for(int x=0; x < WIDTH; x++) {
				int rgb = ycrcbTOrgb(yChan[y][x],crChan[y][x],cbChan[y][x]);
				img.setRGB(x,y,rgb);
			}
		}

		return img;
	}

	public static int ycrcbTOrgb(int y, int cr, int cb) {
		int a = 0;

		int r = (int)(0.871*y - 0.233*cb + 1.405*cr);
		if(r > 255)
			r = 255;
		else if(r < 0)
			r = 0;

		int g = (int)(0.221*y - 1.752*cb - 0.689*cr);
		if(g > 255)
			g = 255;
		else if(g < 0)
			g = 0;

		int b = (int)(4.236*y + 7.626*cb - 0.108*cr);
		if(b > 255)
			b = 255;
		else if(b < 0)
			b = 0;

		return ((a << 24) + (r << 16) + (g << 8) + b);
	}

	/** 
	 * Performs dwt for given array
	 */
	public static void dwt(double[] data) {
		double[] temp = new double[data.length];

		int newLength = data.length/2; 

		for(int i=0; i < newLength; i++) {
			int k = (i << 1);
			temp[i] = 0.5*(data[k] + data[k+1]);
			temp[i+newLength] = 0.5*(data[k] - data[k+1]);
		}

		for(int i=0; i < data.length; i++)	// copy it back
			data[i] = temp[i];
	}

	public static void dwt(double[][] array, int iterations) {
		int rows = array[0].length;
		int columns = array.length;

		double[] row;
		double[] col;

		for(int k=0; k < iterations; k++) {
			int lev = 1 << k;

			int levRows = rows/lev;
			int levCols = columns/lev;			

			row = new double[levCols];
			for(int i=0; i < levRows; i++) {
				for(int j=0; j < row.length; j++)
					row[j] = array[j][i];
				
				dwt(row);	// Perform dwt on the row

				for(int j=0; j < row.length; j++) {	// Copy sums and differences
					array[j][i] = row[j];
				}
			}
			col = new double[levRows];
			for(int j=0; j < levCols; j++) {
				for(int i=0; i < col.length; i++)
					col[i] = array[j][i];

				dwt(col);

				for(int i=0; i < col.length; i++)
					array[j][i] = col[i];
			}
			
		}

	}

	/**
	 * Inverse dwt on given array
	 */
	public static void idwt(double[] data) {
		double[] temp = new double[data.length];

		int mid = data.length/2;

		for(int i=0; i < mid; i++) {
			temp[2*i] = data[i] + data[i+mid];
			temp[2*i+1] = data[i] - data[i+mid];
		}

		for(int i=0; i < data.length; i++)	// copy it back
			data[i] = temp[i];
	}

	public static void idwt(double[][] array, int iterations) {
		int rows = array[0].length;
		int columns = array.length;

		double[] row;
		double[] col;
		

		for(int k=iterations-1; k >= 0; k--) {
			int lev = 1 << k;

			int levCols = columns / lev;
			int levRows = rows / lev;

			col = new double[levRows];
			for(int j=0; j < levCols; j++) {
				for(int i=0; i < col.length; i++)
					col[i] = array[j][i];

				idwt(col);

				for(int i=0; i < col.length; i++)
					array[j][i] = col[i];
			}

			row = new double[levCols];
			for(int i=0; i < levRows; i++) {
				for(int j=0; j < row.length; j++)
					row[j] = array[j][i];

				idwt(row);

				for(int j=0; j < row.length; j++)
					array[j][i] = row[j];
			}
		} 
	}

	public static void zeroDWT(double[][] array, int numCoefficients) {
		if(numCoefficients >= WIDTH*HEIGHT)
			return;

		int count = 4;
		int n = 2;

		boolean HL = true;
		boolean LH = false;
		boolean HH = false;

		while(count < WIDTH*HEIGHT) {
			int x = 0;
			int y = 0;

			if(HL)
				x = n;
			else if(LH)
				y = n;
			else {
				x = n;
				y = n;
			}

			for(int j=0; j < n; j++) {
				for(int i=0; i < n; i++) {
					if(count >= numCoefficients)
						array[y+j][x+i] = 0;
					count++;
				}
			}

			if(HL) {
				LH = true;
				HL = false;
			}

			else if(LH) {
				HH = true;
				LH = false;
			}

			else if(HH) {
				HL = true;
				HH = false;
				n *= 2;
			}
		}
	}

	
}