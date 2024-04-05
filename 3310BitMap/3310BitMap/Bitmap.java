import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;

public class Bitmap {

    static final String filePath = "C:\\Users\\Admin\\Desktop\\utpb.bmp";

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


            JFrame frame = new JFrame("UTPB.bmp");
            frame.setSize(imgWidth, imgHeight);
            frame.setUndecorated(true);
            frame.setVisible(true);

            Canvas canvas = new Canvas();
            canvas.setSize(imgWidth, imgHeight);
            frame.add(canvas);

            for (int y = 0; y < imgHeight; y++)
            {
                for (int x = 0; x < imgWidth; x++)
                {
                    byte[] pixel = new byte[bytesPerPx];
                    raf.read(pixel);
                    int R = bytesToInt(pixel, 2, 1, true);
                    R = R < 0 ? R + 256 : R;
                    int G = bytesToInt(pixel, 1, 1, true);
                    G = G < 0 ? G + 256 : G;
                    int B = bytesToInt(pixel, 0, 1, true);
                    B = B < 0 ? B + 256 : B;
                    //System.out.printf("Writing color %d %d %d%n", R, G, B);
                    Color c = new Color(R, G, B);
                    Graphics g = canvas.getGraphics();
                    g.setColor(c);
                    g.fillRect(x, imgHeight - y, 1, 1);
                }
            }


        } catch (FileNotFoundException fnfEx) {
            fnfEx.printStackTrace();
        } catch (IOException ioEx) {
            ioEx.printStackTrace();
        }
    }
}
