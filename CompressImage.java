import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.util.Arrays;
import javax.swing.*;

public class CompressImage {
	static final int width = 512;
	static final int height = 512;

	BufferedImage imgOne;
	public static JFrame frame = new JFrame();

	private static double[][] redChannel = new double[height][width]; // converted to double for DWT computation
	private static double[][] greenChannel = new double[height][width];
	private static double[][] blueChannel = new double[height][width];

	private static int[][] redMatrix = new int[height][width]; // original img Red channel
	private static int[][] greenMatrix = new int[height][width]; // original img Green channel
	private static int[][] blueMatrix = new int[height][width]; // original img Blue channel

	

	public static double[][] copyArray(double[][] source) {
		double[][] destination = new double[source.length][];
		for (int i = 0; i < source.length; i++) {
			destination[i] = Arrays.copyOf(source[i], source[i].length);
		}
		return destination;
	}

	// read image
	private void readImageRGB(int width, int height, String imgPath, BufferedImage img) {
		try {
			int frameLength = width * height * 3;

			File file = new File(imgPath);
			RandomAccessFile raf = new RandomAccessFile(file, "r");
			raf.seek(0);

			long len = frameLength;
			byte[] bytes = new byte[(int) len];

			raf.read(bytes);

			int ind = 0;
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					// byte a = 0;
					byte r = bytes[ind];
					byte g = bytes[ind + height * width];
					byte b = bytes[ind + height * width * 2];

					int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
					img.setRGB(x, y, pix);
					ind++;
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// display image and accept arguments
	public void showIms(String[] args) {

		this.imgOne = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		readImageRGB(width, height, args[0], imgOne);
		int n = Integer.parseInt(args[1]);
		if (n > 9) {
			System.out.println("Enter n value from 0 to 9 or -1 for progressive analysis for correct results");
			System.exit(0);
		}

		if (n >= 0 && n <= 9) {
			int totalCoeff = (int) Math.pow(2, n);

			BufferedImage imgTwo = dwtCompression(this.imgOne, totalCoeff);

			// Use a label to display the image
			JFrame frame = new JFrame();
			JLabel Label1 = new JLabel(new ImageIcon(imgTwo));
			JPanel jpanel = new JPanel();
			jpanel.add(Label1);

			frame.getContentPane().add(jpanel, BorderLayout.CENTER);
			frame.pack();
			frame.setVisible(true);
			frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

		} else {
			System.out.println("Progressive analysis");
			// progressiveAnalysis(imgOne);
			progressiveAnalysis(imgOne);
			System.out.println("\nCompleted Analysis");
		}
	}

	public static BufferedImage dwtCompression(BufferedImage imgOne, int totalCoeff) {

		int levels = 9;

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int rgb = imgOne.getRGB(x, y);
				redMatrix[y][x] = (rgb >> 16) & 0xFF; // Extracting red
				greenMatrix[y][x] = (rgb >> 8) & 0xFF; // Extracting green
				blueMatrix[y][x] = rgb & 0xFF; // Extracting blue
			}
		}
		

		redChannel = new double[height][width]; // converted to double for DWT computation
		greenChannel = new double[height][width];
		blueChannel = new double[height][width];

		for (int j = 0; j < height; j++) { // converting to double
			for (int i = 0; i < width; i++) {
				redChannel[j][i] = (double) redMatrix[j][i];
				greenChannel[j][i] = (double) greenMatrix[j][i];
				blueChannel[j][i] = (double) blueMatrix[j][i];
			}
		}

		dwt2D(redChannel, levels);
		dwt2D(greenChannel, levels);
		dwt2D(blueChannel, levels);

		ZeroCoeff(redChannel, totalCoeff);
		ZeroCoeff(greenChannel, totalCoeff);
		ZeroCoeff(blueChannel, totalCoeff);

		idwt(redChannel, levels);
		idwt(greenChannel, levels);
		idwt(blueChannel, levels);


		for (int j = 0; j < height; j++) { // converting back to int form
			for (int i = 0; i < width; i++) {
				redMatrix[j][i] = (int) redChannel[j][i];
				greenMatrix[j][i] = (int) greenChannel[j][i];
				blueMatrix[j][i] = (int) blueChannel[j][i];
			}
		}

		for (int y = 0; y < height; y++) { // setting the new RGB values
			for (int x = 0; x < width; x++) {
				int rgb = (255 << 24) | (redMatrix[y][x] << 16) | ((greenMatrix[y][x] << 8))
						| (blueMatrix[y][x]);
				imgOne.setRGB(x, y, rgb);
			}
		}
		return imgOne;
	}

	// 1 dimensional for each row or column
	public static void dwtHaar1d(double[] row_or_col) {
		int len = row_or_col.length;
		double[] temp = new double[len];
		for (int i = 0; i < len / 2; i++) { // processing pair of pixels to compute coefficients
			int k = 2 * i;
			double sum = (row_or_col[k] + row_or_col[k + 1]) / 2; // average
			double diff = (row_or_col[k] - row_or_col[k + 1]) / 2; // difference
			temp[i] = sum;
			temp[i + len / 2] = diff; // stored beyond range of orginal array
		}
		for (int i = 0; i < row_or_col.length; i++) // back into row_or_col array
			row_or_col[i] = temp[i];
	}

	public static void dwt2D(double[][] channel, int numLevels) {
		double[] row;
		double[] col;
		int rows = channel[0].length; // initial rows
		int columns = channel[1].length; // initial columns

		for (int k = 0; k < numLevels; k++) {
			int l = (int) Math.pow(2, k); // level
			// System.out.println("Coeffi in DWT: " + l);
			int currLevelRows = rows / l;
			int currLevelCols = columns / l;

			row = new double[currLevelCols];
			for (int i = 0; i < currLevelRows; i++) {
				for (int j = 0; j < row.length; j++)
					row[j] = channel[j][i];

				dwtHaar1d(row); // Perform dwt on the rows

				for (int j = 0; j < row.length; j++) {
					channel[j][i] = row[j];
				}
			}
			col = new double[currLevelRows];
			for (int j = 0; j < currLevelCols; j++) {
				for (int i = 0; i < col.length; i++) {
					col[i] = channel[j][i];
				}

				dwtHaar1d(col); // Perform dwt on the columns

				for (int i = 0; i < col.length; i++) {
					channel[j][i] = col[i];
				}
			}

		}

	}

	// 1 dimensional for each row or column
	public static void idwtHaar1d(double[] row_or_col) {
		double[] temp = new double[row_or_col.length];

		int len = row_or_col.length / 2;

		for (int i = 0; i < len; i++) { // looping 0 to len-1, process a pair, compute inverse coefficients
			temp[2 * i] = row_or_col[i] + row_or_col[i + len];
			temp[2 * i + 1] = row_or_col[i] - row_or_col[i + len];
		}

		for (int i = 0; i < row_or_col.length; i++) // back into row_or_col araay
			row_or_col[i] = temp[i];
	}

	public static void idwt(double[][] channel, int numLevels) {
		double[] row;
		double[] col;
		int rows = channel[0].length;
		int columns = channel[1].length;

		for (int k = numLevels; k >= 1; k--) {
			int l = (int) Math.pow(2, k - 1);

			int currLevelCols = columns / l;
			int currLevelRows = rows / l;

			col = new double[currLevelRows];
			for (int j = 0; j < currLevelCols; j++) {
				for (int i = 0; i < col.length; i++) {
					col[i] = channel[j][i];
				}

				idwtHaar1d(col);

				for (int i = 0; i < col.length; i++)
					channel[j][i] = col[i];
			}

			row = new double[currLevelCols];
			for (int i = 0; i < currLevelRows; i++) {
				for (int j = 0; j < row.length; j++)
					row[j] = channel[j][i];

				idwtHaar1d(row);

				for (int j = 0; j < row.length; j++)
					channel[j][i] = row[j];
			}
		}
	}

	public static double[][] ZeroCoeff(double[][] channel, int totalCoeff) {
		for (int i = 0; i < 512; i++) {
			for (int j = 0; j < 512; j++) {
				if (i >= totalCoeff || j >= totalCoeff)
					channel[i][j] = 0;
			}
		}
		return channel;
	}

	public static void progressiveAnalysis(BufferedImage imgOne) {

		int levels = 9;
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int rgb = imgOne.getRGB(x, y);
				redMatrix[y][x] = (rgb >> 16) & 0xFF; // Extracting red
				greenMatrix[y][x] = (rgb >> 8) & 0xFF; // Extracting green
				blueMatrix[y][x] = rgb & 0xFF; // Extracting blue
			}
		}
		
		for (int j = 0; j < height; j++) { // converting to double
			for (int i = 0; i < width; i++) {
				redChannel[j][i] = (double) redMatrix[j][i];
				greenChannel[j][i] = (double) greenMatrix[j][i];
				blueChannel[j][i] = (double) blueMatrix[j][i];
			}
		}

		//encode once
		dwt2D(redChannel, levels);
		dwt2D(greenChannel, levels);
		dwt2D(blueChannel, levels);

		for (int lev = 0; lev <= levels; lev++) {

			int totalCoeff = (int) Math.pow(2, lev);
			double[][] originalRedChannel = copyArray(redChannel);
			double[][] originalGreenChannel = copyArray(greenChannel);
			double[][] originalBlueChannel = copyArray(blueChannel);

			System.out.println("Processing at level: " + lev);

			//Zero out unrequired coefficients
			double[][] newredchannel = ZeroCoeff(originalRedChannel, totalCoeff);
			double[][] newgreenchannel = ZeroCoeff(originalGreenChannel, totalCoeff);
			double[][] newbluechannel = ZeroCoeff(originalBlueChannel, totalCoeff);

			//decode here
			idwt(newredchannel, levels);
			idwt(newgreenchannel, levels);
			idwt(newbluechannel, levels);

			for (int y = 0; y < height; y++) { // setting the new RGB values in img
				for (int x = 0; x < width; x++) {
					int rgb = (255 << 24) | ((int) newredchannel[y][x] << 16) | ((int) newgreenchannel[y][x] << 8) | (int) newbluechannel[y][x];
					imgOne.setRGB(x, y, rgb);
				}
			}

			if (lev == 0) {
				JLabel Label1 = new JLabel(new ImageIcon(imgOne));
				JPanel jpanel = new JPanel();
				jpanel.add(Label1);

				frame.getContentPane().add(jpanel, BorderLayout.CENTER);
				frame.pack();
				frame.setVisible(true);
				frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
			} else {
				frame.repaint();
			}

			try {
				Thread.sleep(400);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

	}

	public static void main(String[] args) {
		CompressImage ren = new CompressImage();
		ren.showIms(args);
	}

}
