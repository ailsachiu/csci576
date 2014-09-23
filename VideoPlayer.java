import java.awt.*;
import java.awt.image.*;
import java.io.*;
import javax.swing.*;

public class VideoPlayer {

	public static void main(String[] args) {
		if(args.length < 5) {
			System.out.print("\nUse: VideoPlayer filename width height frame_rate isAntialiased\n");
			System.exit(0);
		}

		String fileName = args[0];									// Name of the input video file
		float width_scaling_factor = Float.parseFloat(args[1]);		// Scaling factor for width
		float height_scaling_factor = Float.parseFloat(args[2]);	// Scaling factor for height
		int output_frame_rate = Integer.parseInt(args[3]);			// Output frame rate
   		int isAntialiased = Integer.parseInt(args[4]);				// 1 = anti-aliasing turned on; 0 = off
   		
	} // End of mainå

}