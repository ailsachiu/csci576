import java.awt.*;
import java.awt.image.*;
import java.io.*;
import javax.swing.*;
import java.util.ArrayList;
import java.lang.Math;

public class VideoPlayer {

	public static void main(String[] args) {
	
		if(args.length < 6) {
			System.out.print("Use: VideoPlayer filename width_scale_factor height_scale_factor frame_rate anti-aliasing\n");
			System.exit(0);
		}

		String fileName = args[0];									// Name of the input video file
		double width_scaling_factor = Double.parseDouble(args[1]);	// Scaling factor for width
		double height_scaling_factor = Double.parseDouble(args[2]);	// Scaling factor for height
		int output_frame_rate = Integer.parseInt(args[3]);			// Output frame rate
   		int isAntialiased = Integer.parseInt(args[4]);				// 1 = anti-aliasing turned on; 0 = off
   		int option = Integer.parseInt(args[5]);						// 0 = default, 1 = analysis of aspect ratio

   		// Standard video dimensions
   		int width = 352;
   		int height = 288;

   		ArrayList<BufferedImage> ov = new ArrayList<BufferedImage>();	// Stores original video
		ArrayList<BufferedImage> nv = new ArrayList<BufferedImage>();	// Stores output video

   		try {
   			File file = new File(args[0]);
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

   		// Resize the original video
   		for(int i=0; i < ov.size(); i++) {
   			BufferedImage img = ov.get(i);
   			int new_width = (int)(img.getWidth()*width_scaling_factor);
   			int new_height = (int)(img.getHeight()*height_scaling_factor);
   			BufferedImage new_img = new BufferedImage(new_width,new_height,BufferedImage.TYPE_INT_RGB);

   			for(int y=0; y < img.getHeight(); y++) {
	   			for(int x=0; x < img.getWidth(); x++) {
	   				double x_orig = x*width_scaling_factor;
	   				int xf = (int)(Math.floor(x_orig));

	   				double y_orig = y*height_scaling_factor;
	   				int yf = (int)(Math.floor(y_orig));

	   				int rgb = img.getRGB(x,y);
	   				// Check if anti-aliasing is ON
	   				if(isAntialiased == 1) {
	   					//rgb = VideoPlayer.avgRGB(x,y,img);

	   					Color c = new Color(img.getRGB(x,y));
						int rsum = c.getRed();
						int	gsum = c.getGreen();
						int	bsum = c.getBlue();
	   					int count = 1;

						int w = img.getWidth();
						int h = img.getHeight();

						// First row
						if(x-1 > -1 && y-1 > -1) {
							c = new Color(img.getRGB(x-1,y-1));
							rsum += c.getRed();
							gsum += c.getGreen();
							bsum += c.getBlue();
							count++;
						}
						if(y-1 > -1) {
							c = new Color(img.getRGB(x,y-1));
							rsum += c.getRed();
							gsum += c.getGreen();
							bsum += c.getBlue();
							count++;
						}
						if(x+1 < w && y-1 > -1) {
							c = new Color(img.getRGB(x+1,y-1));
							rsum += c.getRed();
							gsum += c.getGreen();
							bsum += c.getBlue();
							count++;
						}

						// Second row
						if(x-1 > -1) {
							c = new Color(img.getRGB(x-1,y));
							rsum += c.getRed();
							gsum += c.getGreen();
							bsum += c.getBlue();
							count++;
						}
						if(x+1 < w) {
							c = new Color(img.getRGB(x+1,y));
							rsum += c.getRed();
							gsum += c.getGreen();
							bsum += c.getBlue();
							count++;
						}

						// Third row
						if(x-1 > -1 && y+1 < h) {
							c = new Color(img.getRGB(x-1,y+1));
							rsum += c.getRed();
							gsum += c.getGreen();
							bsum += c.getBlue();
							count++;
						}
						if(y+1 < h) {
							c = new Color(img.getRGB(x,y+1));
							rsum += c.getRed();
							gsum += c.getGreen();
							bsum += c.getBlue();
							count++;
						}
						if(x+1 < w && y+1 < h) {
							c = new Color(img.getRGB(x+1,y+1));
							rsum += c.getRed();
							gsum += c.getGreen();
							bsum += c.getBlue();
							count++;
						}
						
						int a = 0;
						int r = (int)(rsum / count);
						int g = (int)(gsum / count);
						int b = (int)(bsum / count);
						rgb = ((a << 24) + (r << 16) + (g << 8) + b);
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


   		// Use a label to display the image
	    JFrame frame = new JFrame();
	    JLabel label = new JLabel(new ImageIcon(nv.get(0)));
	    frame.getContentPane().add(label, BorderLayout.CENTER);
	    frame.pack();
	    frame.setVisible(true);

	    System.out.println("Size of ov: " + ov.size() + ", size of nv: " + nv.size());
	    //label.setIcon(new ImageIcon(nv.get(150)));
	
	    // Set frame rate
	    for(int i=1; i < nv.size(); i++) {
	  	    label.setIcon(new ImageIcon(nv.get(i)));
	    	try {
	    		Thread.sleep(1000/output_frame_rate); // 10000/30 should be default
	    	} catch(InterruptedException e) {
	    		e.printStackTrace();
	    	}
	    }    

	} // End of main

	/**
	 * Returns int RGB average of 3x3 where pixel (x,y) is in the center for a given BufferedImage img
	 * Check image boundaries
	**/
	/*
	int avgRGB(int x, int y, BufferedImage img) {
	
	}
*/
}