import java.awt.*;
import java.awt.image.*;
import java.io.*;
import javax.swing.*;
import java.util.ArrayList;

public class VideoPlayer {

	public static void main(String[] args) {
	/*
		if(args.length < 6) {
			System.out.print("Use: VideoPlayer filename width height frame_rate isAntialiased\n");
			System.exit(0);
		}
	*/
		String fileName = args[0];									// Name of the input video file
		int width = Integer.parseInt(args[1]);
		int height = Integer.parseInt(args[2]);
		/*
		float width_scaling_factor = Float.parseFloat(args[1]);		// Scaling factor for width
		float height_scaling_factor = Float.parseFloat(args[2]);	// Scaling factor for height
		int output_frame_rate = Integer.parseInt(args[3]);			// Output frame rate
   		int isAntialiased = Integer.parseInt(args[4]);				// 1 = anti-aliasing turned on; 0 = off
   		*/

   		ArrayList<BufferedImage> ov = new ArrayList<BufferedImage>();
		
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
						ind++;			// skip to the next R
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

   		// Use a label to display the image
	    JFrame frame = new JFrame();
	    JLabel label = new JLabel(new ImageIcon(ov.get(0)));
	    frame.getContentPane().add(label, BorderLayout.CENTER);
	    frame.pack();
	    frame.setVisible(true);

	    System.out.println(ov.size() + "\n");
	    for(int i=1; i < ov.size(); i++) {
	  	    label.setIcon(new ImageIcon(ov.get(i)));
	    	// do fps here
	    	try {
	    		Thread.sleep(1000/30);
	    	} catch(InterruptedException e) {
	    		e.printStackTrace();
	    	}
	    }
	    

	} // End of main

}