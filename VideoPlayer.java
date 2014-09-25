import java.awt.*;
import java.awt.image.*;
import java.io.*;
import javax.swing.*;
import java.util.ArrayList;
import java.lang.Math;

public class VideoPlayer {
	// Standard video dimensions
	static final int WIDTH = 352;
	static final int HEIGHT = 288;

	public ArrayList<BufferedImage> ov;	// Stores original video
	public ArrayList<BufferedImage> nv;	// Stores output video

	private String fileName;				// Name of the input video file
	private double width_scaling_factor;	// Scaling factor for width
	private double height_scaling_factor;	// Scaling factor for height
	private int output_frame_rate;			// Output frame rate
	private int isAntialiased;				// 1 = anti-aliasing turned on; 0 = off
	private int option;						// 0 = default, 1 = analysis of aspect ratio

	public static void main(String[] args) {
	
		if(args.length < 6) {
			System.out.print("Use: VideoPlayer filename width_scale_factor height_scale_factor frame_rate anti-aliasing\n");
			System.exit(0);
		}

		String fileName = args[0];									
		double width_scaling_factor = Double.parseDouble(args[1]);		
		double height_scaling_factor = Double.parseDouble(args[2]);	
		int output_frame_rate = Integer.parseInt(args[3]);			
   		int isAntialiased = Integer.parseInt(args[4]);			
   		int option = Integer.parseInt(args[5]);						

   		// Instantiate VideoPlayer
   		VideoPlayer vp = new VideoPlayer(fileName, width_scaling_factor, height_scaling_factor, output_frame_rate, isAntialiased, option);
   		
   		// Load input video
   		vp.load();

   		if(option == 0) {		// Default implementation
   			vp.resize();
   		}
   		else if(option == 1) {	// Nonlinear mapping
   			vp.nonlinear();
   		}
   		

   		// Use a label to display the image
	    JFrame frame = new JFrame();
	    JLabel label = new JLabel(new ImageIcon(vp.nv.get(0)));
	    frame.getContentPane().add(label, BorderLayout.CENTER);
	    frame.pack();
	    frame.setVisible(true);
	
	    // Set frame rate
	    for(int i=1; i < vp.nv.size(); i++) {
	  	    label.setIcon(new ImageIcon(vp.nv.get(i)));
	    	try {
	    		Thread.sleep(1000/vp.getOutput_frame_rate()); // 10000/30 should be default
	    	} catch(InterruptedException e) {
	    		e.printStackTrace();
	    	}
	    }    

	} // End of main

	/**
	 *	Constructor
	 */
	public VideoPlayer(String fileName, double width_scaling_factor, double height_scaling_factor, int output_frame_rate, int isAntialiased, int option) {
		// Initialize variables
		this.fileName = fileName;							
		this.width_scaling_factor = width_scaling_factor;		
		this.height_scaling_factor = height_scaling_factor;	
		this.output_frame_rate = output_frame_rate;			
   		this.isAntialiased = isAntialiased;				
   		this.option = option;			

   		ov = new ArrayList<BufferedImage>();
		nv = new ArrayList<BufferedImage>();
	} 

	/**
	 *	Loads the video file into an ArrayList<BufferedImage>
	 */
	public void load() {
		System.out.println("Loading input video");
		int width = WIDTH;
		int height = HEIGHT;

		// Read the input video into the ov ArrayList
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
	    	while(ind+height*width*2 < len) {    
	    		BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);	
				for(int y = 0; y < height; y++){
			
					for(int x = 0; x < width; x++){
				 
						byte a = 0;
						byte r = bytes[ind];
						byte g = bytes[ind+height*width];
						byte b = bytes[ind+height*width*2]; 
						ind++;			// Skip to the next R
						int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
						//int pix = ((a << 24) + (r << 16) + (g << 8) + b);
						img.setRGB(x,y,pix);
					}
				}
				ind += height*width*2;	// Skip to the next frame
				ov.add(img);
			}

   		} catch (FileNotFoundException e) {
   			e.printStackTrace();
   		} catch (IOException e) {
   			e.printStackTrace();
   		}
	}

	/**
	 * Default implementation
	 * Resize the video and anti-alias if ON 
	 */
	public void resize() {
		System.out.println("Resizing");
		if(isAntialiased == 1)
			System.out.println("Anti-aliasing");

		// Resize the original video
   		for(int i=0; i < ov.size(); i++) {
   			BufferedImage img = ov.get(i);	// Original frame
   			int new_width = (int)(img.getWidth()*width_scaling_factor);
   			int new_height = (int)(img.getHeight()*height_scaling_factor);
   			BufferedImage new_img = new BufferedImage(new_width,new_height,BufferedImage.TYPE_INT_RGB); // New frame

   			for(int y=0; y < img.getHeight(); y++) {
	   			for(int x=0; x < img.getWidth(); x++) {
	   				double x_orig = x*width_scaling_factor;
	   				int xf = (int)(Math.floor(x_orig));

	   				double y_orig = y*height_scaling_factor;
	   				int yf = (int)(Math.floor(y_orig));

	   				int rgb = img.getRGB(x,y);
	   				
	   				// Check if anti-aliasing is ON
	   				if(isAntialiased == 1) {
	   					rgb = avgRGB(x,y,img);
	   				}

	   				new_img.setRGB(xf,yf,rgb);			// (xf, yf)

	   				if((xf+1) < new_width)
						new_img.setRGB((xf+1),yf,rgb);	// (xf+1, yf)

	   				if((yf+1) < new_height)
						new_img.setRGB(xf,(yf+1),rgb);	// (xf, yf+1)

					if((xf+1) < new_width && (yf+1) < new_height)
						new_img.setRGB(xf+1,yf+1,rgb);	// (xf+1, yf+1)
	   			}
   			}

   			nv.add(new_img);	// Add to output 
   		}
	}

	/**
	 * Returns int RGB average of 3x3 where pixel (x,y) is in the center for a given BufferedImage img
	 * Check image boundaries
	 */
	public int avgRGB(int x, int y, BufferedImage img) {
		Color c = new Color(img.getRGB(x,y));
		int rsum = 0;
		int	gsum = 0;
		int	bsum = 0;
	   	int count = 0;

	   	int w = img.getWidth();
	   	int h = img.getHeight();
		
		for(int j=-1; j < 2; j++) {
			for(int i=-1; i < 2; i++) {
				if(x+i > -1 && x+i < w && y+j > -1 && y+j < h) {
					c = new Color(img.getRGB(x+i,y+j));
					rsum += c.getRed();
					gsum += c.getGreen();
					bsum += c.getBlue();
					count++;
				}
			}
		}

		int a = 0;
		int r = rsum/count;
		int g = gsum/count;
		int b = bsum/count;

		return ((a << 24) + (r << 16) + (g << 8) + b);
	}

	/**
	 * Nonlinear mapping implementation
	 */
	public void nonlinear() {
		System.out.println("Nonlinear mapping");

		int frame_width = (int)(WIDTH*width_scaling_factor);
   		int frame_height = (int)(HEIGHT*height_scaling_factor);
   		System.out.println("Frame dimensions = " + frame_width + "x" + frame_height);
   		System.out.println("Scaling factors, wsf = " + width_scaling_factor + ", hsf = " + height_scaling_factor);

   		System.out.println("Scaling input, keeping aspect ratio, to new frame");
   		// Dimensions for same aspect ratio in frame
   		int new_width = WIDTH;
   		int new_height = HEIGHT;
   		// Scaling factors for same aspect ratio in frame	
   		double new_wsf = 1.0;
   		double new_hsf = 1.0;

   		// Scale width if necessary
   		if(frame_width != WIDTH) {
   			new_width = frame_width;
   			new_height = (new_width*HEIGHT) / WIDTH; 	// Get proportional height with ratio
   			new_wsf = (double)new_width / (double)WIDTH;
   			new_hsf = (double)new_height / (double)HEIGHT;
   		}

   		// Scale height if necessary
   		if(new_height > frame_height) {
   			new_height = frame_height;
   			new_width = (new_height*WIDTH) / HEIGHT;	// Get proportional width with ratio
   			new_wsf = (double)new_width / (double)WIDTH;
   			new_hsf = (double)new_height / (double)HEIGHT;
   		}
   		System.out.println("Video dimensions = " + new_width + "x" + new_height);
   		System.out.println("New wsf = " + new_wsf + ", new hsf = " + new_hsf);

   		for(int i=0; i < ov.size(); i++) {
   			BufferedImage img = ov.get(i);	// Original frame
   			BufferedImage new_img = new BufferedImage(frame_width,frame_height,BufferedImage.TYPE_INT_RGB); // New frame
   			for(int y=0; y < img.getHeight(); y++) {
   				for(int x=0; x < img.getWidth(); x++) {
   					double x_orig = (double)frame_width/2.0 - (double)new_width/2.0 + x*new_wsf;
	   				//double x_orig = (double)WIDTH/2.0 + x - (double)new_width/2.0 + x;
	   				int xf = (int)(Math.floor(x_orig));	// Coordinate in new image

	   				double y_orig = (double)frame_height/2.0 - (double)new_height/2.0 + y*new_hsf;
	   				int yf = (int)(Math.floor(y_orig));	// Coordinate in new image

	   				int rgb = img.getRGB(x,y);
	   				
	   				// Check if anti-aliasing is ON
	   				if(isAntialiased == 1) {
	   					rgb = avgRGB(x,y,img);
	   				}

	   				new_img.setRGB(xf,yf,rgb);		// (xf, yf)

	   				if((xf+1) < new_width)
						new_img.setRGB((xf+1),yf,rgb);	// (xf+1, yf)

	   				if((yf+1) < new_height)
						new_img.setRGB(xf,(yf+1),rgb);	// (xf, yf+1)

					if((xf+1) < new_width && (yf+1) < new_height)
						new_img.setRGB(xf+1,yf+1,rgb);	// (xf+1, yf+1)
   				}
   			}
   			nv.add(new_img);
   		}		

/*
   		int focus = 0.8;					// % of how much to focus
		double r = focus*img.getHeight()/2;	// set radius to smallest constraint - in original, it's height
   		int cx = img.getWidth()/2;			// x center of original image
   		int cy = img.getHeight()/2;			// y center of original image

   		double nr = focus*new_height/2;		// proportional radius
   		int ncx = new_width/2;				// x center of new image
   		int ncy = new_height/2;				// y center of new image

		for(int i=0; i < ov.size(); i++) {
			BufferedImage img = ov.get(i);	// Original frame
   			BufferedImage new_img = new BufferedImage(frame_width,frame_height,BufferedImage.TYPE_INT_RGB); // New frame
   			
   			for(int y=0; y < img.getHeight(); y++) {
   				for(int x=0; x < img.getWidth(); x++) {
   					// Keep aspect ratio of original video
   					// First, letterbox to find the correct size

   					double x_orig = x*width_scaling_factor;
	   				int xf = (int)(Math.floor(x_orig));

	   				double y_orig = y*height_scaling_factor;
	   				int yf = (int)(Math.floor(y_orig));

	   				int rgb = img.getRGB(x,y);
	   				
	   				// Check if anti-aliasing is ON
	   				if(isAntialiased == 1) {
	   					rgb = avgRGB(x,y,img);
	   				}

	   				new_img.setRGB(xf,yf,rgb);			// (xf, yf)

	   				if((xf+1) < new_width)
						new_img.setRGB((xf+1),yf,rgb);	// (xf+1, yf)

	   				if((yf+1) < new_height)
						new_img.setRGB(xf,(yf+1),rgb);	// (xf, yf+1)

					if((xf+1) < new_width && (yf+1) < new_height)
						new_img.setRGB(xf+1,yf+1,rgb);	// (xf+1, yf+1)

   					/*

   					double distance = Math.sqrt(Math.pow(cx-x,2)+Math.pow(cy-y,2));
   					if(distance < r) {
   						new_img.setRGB(x,y,img.getRGB(x,y));
   						double x_orig = x*width_scaling_factor;
		   				int xf = (int)(Math.floor(x_orig));

		   				double y_orig = y*height_scaling_factor;
		   				int yf = (int)(Math.floor(y_orig));

		   				int rgb = img.getRGB(x,y);
		   				
		   				// Check if anti-aliasing is ON
		   				if(isAntialiased == 1) {
		   					rgb = avgRGB(x,y,img);
		   				}

		   				new_img.setRGB(xf,yf,rgb);			// (xf, yf)

		   				if((xf+1) < new_width)
							new_img.setRGB((xf+1),yf,rgb);	// (xf+1, yf)

		   				if((yf+1) < new_height)
							new_img.setRGB(xf,(yf+1),rgb);	// (xf, yf+1)

						if((xf+1) < new_width && (yf+1) < new_height)
							new_img.setRGB(xf+1,yf+1,rgb);	// (xf+1, yf+1)
   					}
   					else {
   						new_img.setRGB(x,y,0);
   					}
   					*/

					/*
						New pixel location in 1:1 circle = original_w/2 + x - new_w/2
					*
					
   				}
   			}

   			nv.add(new_img);	// Add to output 
   		}
   		*/
	}

	public int getOutput_frame_rate() {
		return output_frame_rate;
	}

}