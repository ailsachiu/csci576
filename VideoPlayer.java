import java.awt.*;
import java.awt.image.*;
import java.io.*;
import javax.swing.*;
import java.util.ArrayList;
import java.lang.Math;

public class VideoPlayer {

	public static void main(String[] args) {
	
		if(args.length < 6) {
			System.out.print("Use: VideoPlayer filename width height frame_rate isAntialiased\n");
			System.exit(0);
		}

		String fileName = args[0];									// Name of the input video file
		double width_scaling_factor = Double.parseDouble(args[1]);	// Scaling factor for width
		double height_scaling_factor = Double.parseDouble(args[2]);	// Scaling factor for height
		int output_frame_rate = Integer.parseInt(args[3]);			// Output frame rate
   		int isAntialiased = Integer.parseInt(args[4]);				// 1 = anti-aliasing turned on; 0 = off
   		
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

	   				new_img.setRGB(xf,yf,img.getRGB(x,y));			// (xf, yf)

	   				if((xf+1) < new_width)
						new_img.setRGB((xf+1),yf,img.getRGB(x,y));	// (xf+1, yf)

	   				if((yf+1) < new_height)
						new_img.setRGB(xf,(yf+1),img.getRGB(x,y));	// (xf, yf+1)

					if((xf+1) < new_width && (yf+1) < new_height)
						new_img.setRGB(xf+1,yf+1,img.getRGB(x,y));	// (xf+1, yf+1)
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

}