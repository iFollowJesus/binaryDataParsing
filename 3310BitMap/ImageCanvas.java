import java.awt.Canvas;
import java.awt.Color;
import java.awt.Graphics;

public class ImageCanvas extends Canvas {
    private Color[][] pixels;

    public ImageCanvas(Color[][] pixels, int width, int height) {
        this.pixels = pixels;
        setSize(width, height);
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        if (pixels != null) {
            for (int y = 0; y < pixels.length; y++) {
                for (int x = 0; x < pixels[y].length; x++) {
                    g.setColor(pixels[y][x]);
                    g.fillRect(x, pixels.length-y, 1, 1);
                }
            }
        }
    }
}

/*
 * for (int y = 0; y < imgHeight; y++) { for (int x = 0; x < imgWidth; x++) {
 * byte[] pixel = new byte[bytesPerPx]; raf.read(pixel); int R =
 * bytesToInt(pixel, 2, 1, true); R = R < 0 ? R + 256 : R; int G =
 * bytesToInt(pixel, 1, 1, true); G = G < 0 ? G + 256 : G; int B =
 * bytesToInt(pixel, 0, 1, true); B = B < 0 ? B + 256 : B;
 * //System.out.printf("Writing color %d %d %d%n", R, G, B); Color c = new
 * Color(R, G, B); Graphics g = originalCanvas.getGraphics(); g.setColor(c);
 * g.fillRect(x, imgHeight - y, 1, 1); } }
 */