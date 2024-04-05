import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;

public class Bitmap {
	
	static final String filePath = "C:\\Users\\Administrator\\Desktop\\3310BitMap\\utpb.bmp";

    protected static int bytesToInt(byte[] bytes, int offset, int length, boolean littleEndian) {
        return (littleEndian ? bytesToIntLE(bytes, offset, length) : bytesToIntBE(bytes, offset, length));
    }

    /**
     * Converts a little-endian sequence of bytes to an {@code int}.
     *
     * @param  bytes   the byte sequence that is to be converted.
     * @param  offset  the offset to {@code bytes} at which the sequence starts.
     * @param  length  the length of the byte sequence.
     * @return the {@code int} that results from the conversion of the byte sequence.
     * @since  1.0
     * @see    #bytesToIntBE(byte[], int, int)
     */

    private static int bytesToIntLE(byte[] bytes, int offset, int length) {
        int i = offset + length;
        int value = bytes[--i];
        while (--i >= offset) {
            value <<= 8;
            value |= bytes[i] & 0xFF;
        }
        return value;
    }

    /**
     * Converts a big-endian sequence of bytes to an {@code int}.
     *
     * @param  bytes   the byte sequence that is to be converted.
     * @param  offset  the offset to {@code bytes} at which the sequence starts.
     * @param  length  the length of the byte sequence.
     * @return the {@code int} that results from the conversion of the byte sequence.
     * @since  1.0
     * @see    #bytesToIntLE(byte[], int, int)
     */

    private static int bytesToIntBE(byte[] bytes, int offset, int length) {
        int endOffset = offset + length;
        int value = bytes[offset];
        while (++offset < endOffset) {
            value <<= 8;
            value |= bytes[offset] & 0xFF;
        }
        return value;
    }
    
    private static Color[][] colorArray(RandomAccessFile raf, int imgHeight, int imgWidth, int bytesPerPx) throws IOException{
    	// Create a 2D array to store pixel colors
    	Color[][] imagePixels = new Color[imgHeight][imgWidth];

    	// Reading and storing pixel data
    	for (int y = 0; y < imgHeight; y++) {
    	    for (int x = 0; x < imgWidth; x++) {
    	        byte[] pixel = new byte[bytesPerPx];
    	        raf.read(pixel);
    	        int R = bytesToInt(pixel, 2, 1, true);
    	        R = R < 0 ? R + 256 : R;
    	        int G = bytesToInt(pixel, 1, 1, true);
    	        G = G < 0 ? G + 256 : G;
    	        int B = bytesToInt(pixel, 0, 1, true);
    	        B = B < 0 ? B + 256 : B;

    	        imagePixels[y][x] = new Color(R, G, B);
    	    }
    	}
		return imagePixels;
    	
    }
    
    private static Color[][] imgBlurKernel(Color[][] imagePixels, int imgHeight, int imgWidth){
    	Color[][] blurredImage = new Color[imgHeight][imgWidth];

    	for (int y = 1; y < imgHeight - 1; y++) {
    	    for (int x = 1; x < imgWidth - 1; x++) {
    	        int sumR = 0, sumG = 0, sumB = 0;
    	        for (int ky = -1; ky <= 1; ky++) {
    	            for (int kx = -1; kx <= 1; kx++) {
    	                Color pixelColor = imagePixels[y + ky][x + kx];
    	                sumR += pixelColor.getRed();
    	                sumG += pixelColor.getGreen();
    	                sumB += pixelColor.getBlue();
    	            }
    	        }
    	        int avgR = sumR / 9;
    	        int avgG = sumG / 9;
    	        int avgB = sumB / 9;
    	        blurredImage[y][x] = new Color(avgR, avgG, avgB);
    	    }
    	}
    	return blurredImage;
    	
    }
    
    public static Color[][] applyEdgeDetectionKernel(Color[][] imagePixels) {
        // Define the edge detection kernel
        int[][] kernel = {
            {0, 1, 0},
            {1, -4, 1},
            {0, 1, 0}
        };

        // Create an output image array initialized to black
        Color[][] outputImage = new Color[imagePixels.length][imagePixels[0].length];
        for (Color[] row : outputImage) {
            Arrays.fill(row, new Color(0, 0, 0));
        }

        // Apply the kernel to each pixel in the input image
        for (int y = 1; y < imagePixels.length - 1; y++) {
            for (int x = 1; x < imagePixels[y].length - 1; x++) {
                int redSum = 0, greenSum = 0, blueSum = 0;

                // Apply the kernel to the neighboring pixels
                for (int ky = -1; ky <= 1; ky++) {
                    for (int kx = -1; kx <= 1; kx++) {
                        Color pixel = imagePixels[y + ky][x + kx];
                        int weight = kernel[ky + 1][kx + 1];

                        redSum += weight * pixel.getRed();
                        greenSum += weight * pixel.getGreen();
                        blueSum += weight * pixel.getBlue();
                    }
                }

                // Clamp the values to the 0-255 range
                int red = Math.min(255, Math.max(0, redSum));
                int green = Math.min(255, Math.max(0, greenSum));
                int blue = Math.min(255, Math.max(0, blueSum));

                // Set the output pixel to the result of the convolution
                outputImage[y][x] = new Color(red, green, blue);
            }
        }

        return outputImage;
    }

    public static Color[][] applyGaussianBlur(Color[][] imagePixels) {
        // Define the edge detection kernel
        int[][] kernel = {
            {1, 2, 1},
            {2, 4, 2},
            {1, 2, 1}
        };
        
        int kernelWeight =16;

        // Create an output image array initialized to black
        Color[][] outputImage = new Color[imagePixels.length][imagePixels[0].length];
        for (Color[] row : outputImage) {
            Arrays.fill(row, new Color(0, 0, 0));
        }

        // Apply the kernel to each pixel in the input image
        for (int y = 1; y < imagePixels.length - 1; y++) {
            for (int x = 1; x < imagePixels[y].length - 1; x++) {
                int redSum = 0, greenSum = 0, blueSum = 0;

                // Apply the kernel to the neighboring pixels
                for (int ky = -1; ky <= 1; ky++) {
                    for (int kx = -1; kx <= 1; kx++) {
                        Color pixel = imagePixels[y + ky][x + kx];
                        int weight = kernel[ky + 1][kx + 1];

                        redSum += weight * pixel.getRed();
                        greenSum += weight * pixel.getGreen();
                        blueSum += weight * pixel.getBlue();
                    }
                }

               // Normalize the sum by dividing by the kernel's weight and clamp the values to the 0-255 range
                int red = Math.min(255, Math.max(0, redSum/kernelWeight));
                int green = Math.min(255, Math.max(0, greenSum/kernelWeight));
                int blue = Math.min(255, Math.max(0, blueSum/kernelWeight));

                // Set the output pixel to the result of the convolution
                outputImage[y][x] = new Color(red, green, blue);
            }
        }

        return outputImage;
    }

    public static void drawImageOnCanvas(Canvas canvas, Color[][] pixels, int imgHeight) {
        Graphics g = canvas.getGraphics();
        for (int y = 0; y < pixels.length; y++) {
            for (int x = 0; x < pixels[y].length; x++) {
                g.setColor(pixels[y][x]);
                g.fillRect(x, y, 1, 1);
            }
        }
    }

    public static void main(String[] args)
    {
        try {
            File bmpFile = new File(filePath);
            RandomAccessFile raf = new RandomAccessFile(bmpFile, "r");

            byte[] bmpHeader = new byte[14];
            raf.read(bmpHeader);

            String magic = "";
            magic += (char)bmpHeader[0];
            magic += (char)bmpHeader[1];
            System.out.println(magic); // Should be "BM"

            int firstPxAddr = bytesToInt(bmpHeader, 10, 4, true);
            System.out.printf("File offset of first pixel data: %d%n", firstPxAddr);

            byte[] dibHeaderSize = new byte[4];
            raf.read(dibHeaderSize);

            int dibHdrSize = bytesToInt(dibHeaderSize, 0, 4, true);

            byte[] dibHeader = new byte[dibHdrSize-4];
            raf.read(dibHeader);

            int imgWidth = bytesToInt(dibHeader, 0, 4, true);
            System.out.printf("Image width: %d%n", imgWidth);

            int imgHeight = bytesToInt(dibHeader, 4, 4, true);
            System.out.printf("Image height: %d%n", imgHeight);

            int bytesPerPx = bytesToInt(dibHeader, 10, 2, true) / 8;
            System.out.printf("Bytes per pixel: %d%n", bytesPerPx);
            
            Color imagePixels[][] = new Color[imgHeight][imgWidth];
            imagePixels=colorArray(raf, imgHeight, imgWidth, bytesPerPx);
            
            Color processedImagePixels[][]= new Color[imgHeight][imgWidth];
            processedImagePixels = imgBlurKernel(imagePixels, imgHeight, imgWidth);
            
            Color edgeDetection[][] = new Color[imgHeight][imgWidth];
            edgeDetection = applyEdgeDetectionKernel(imagePixels);
            
            Color gausianBlur[][] = new Color[imgHeight][imgWidth];
            gausianBlur = applyGaussianBlur(imagePixels);


         // In your main method or setup method
            JFrame frame = new JFrame("Bitmap Display");
            frame.setLayout(new FlowLayout());
            frame.setSize(imgWidth * 3, imgHeight* 4);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            ImageCanvas originalCanvas = new ImageCanvas(imagePixels, imgWidth, imgHeight);
            frame.add(originalCanvas);

            ImageCanvas processedCanvas = new ImageCanvas(processedImagePixels, imgWidth, imgHeight);
            frame.add(processedCanvas);
            
            ImageCanvas edgeDetectionCanvas = new ImageCanvas(edgeDetection, imgWidth, imgHeight);
            frame.add(edgeDetectionCanvas);
            
            ImageCanvas gausianBlurCanvas = new ImageCanvas(gausianBlur, imgWidth, imgHeight);
            frame.add(gausianBlurCanvas);
            

            frame.setVisible(true);
			 

        } catch (FileNotFoundException fnfEx) {
            fnfEx.printStackTrace();
        } catch (IOException ioEx) {
            ioEx.printStackTrace();
        }
    }
}
