import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.font.TextAttribute;
import java.awt.im.InputMethodHighlight;
import java.awt.image.ColorModel;
import java.awt.image.ImageObserver;
import java.awt.image.ImageProducer;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URL;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;

public class WavParser {

    //static final String filePath = "C:\\Users\\Admin\\Desktop\\Highway To Hell.wav";
    static final String filePath = "C:\\Users\\Administrator\\Desktop\\WAV\\guitar.wav";

    private static String bytesToString(byte[] bytes, int offset, int length)
    {
        try {
            String result = "";
            for (int i = 0; i < length; i++) {
                result += (char)bytes[offset + i];
            }
            return result;
        } catch (ArrayIndexOutOfBoundsException oobEx) {
            return null;
        }
    }

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
            File wavFile = new File(filePath);
            RandomAccessFile raf = new RandomAccessFile(wavFile, "r");

            byte[] wavHeader = new byte[44];
            raf.read(wavHeader);
            int index = 0;

            String riff = bytesToString(wavHeader, index, 4);
            System.out.println(riff);
            index += 4;

            int fileSize = bytesToInt(wavHeader, index, 4, true);
            System.out.printf("Total file size: %d%n", fileSize+44);
            index += 4;

            String WAVEfmt = bytesToString(wavHeader, index, 8);
            System.out.println(WAVEfmt);
            index += 8;

            int fmtLen = bytesToIntLE(wavHeader, index, 4);
            System.out.printf("Length of format data: %d%n", fmtLen);
            index += 4;

            int format = bytesToIntLE(wavHeader, index, 2);
            System.out.printf("Format type code: %d%n", format);
            index += 2;

            int numChn = bytesToIntLE(wavHeader, index, 2);
            System.out.printf("Number of channels: %d%n", numChn);
            index += 2;

            int smpRate = bytesToIntLE(wavHeader, index, 4);
            System.out.printf("Sample rate: %dHz%n", smpRate);
            index += 4;

            int bitRate = bytesToIntLE(wavHeader, index, 4);
            System.out.printf("Total byte rate: %dBps%n", bitRate);
            index += 4;

            int modeCode = bytesToIntLE(wavHeader, index, 2);
            String mode = "";
            switch (modeCode)
            {
                case 1:
                    mode = "8-bit mono";
                    break;
                case 2:
                    mode = numChn == 1 ? "16-bit mono" : "8-bit stereo";
                    break;
                case 4:
                    mode = "16-bit stereo";
                    break;
                default:
                    mode = "unknown";
                    break;
            }
            System.out.printf("Playback mode: %s%n", mode);
            index += 2;

            int smpBits = bytesToIntLE(wavHeader, index, 2);
            System.out.printf("Bits per sample: %d%n", smpBits);
            index += 2;
            int smpBytes = smpBits/8;

            String dataChunk = bytesToString(wavHeader, index, 4);
            index += 4;

            int dataSize = bytesToIntLE(wavHeader, index, 4);
            System.out.printf("Data size: %d%n", dataSize);
            index += 4;

            byte[] wavData = new byte[dataSize];
            raf.read(wavData);

            // The maxValue for a 2-byte signed short is 32767, which corresponds
            //   to the maximum allowed amplitude of our waveform of +/-96dB
            // We generate these max and min values so that we can normalize the
            //   large-ish int values we're getting from each sample to a scaled
            //   vertical offset from a point on the screen, or to an absolute
            //   amplitude value in dB
            double maxValue = Math.pow(2, smpBits)-1;
            double minValue = -1*(maxValue+1);
            System.out.println(maxValue);
            System.out.println(minValue);

            JFrame frame = new JFrame("WAVE");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            frame.setUndecorated(true);

            Toolkit tk = Toolkit.getDefaultToolkit();
            int screenWidth = tk.getScreenSize().width;
            int screenHeight = tk.getScreenSize().height;

            frame.setVisible(true);
            frame.requestFocus();

            Canvas cnv = new Canvas();
            cnv.setSize(screenWidth, screenHeight);
            frame.add(cnv);

            double seconds = (double)dataSize / (double)bitRate;
            System.out.printf("There are %.2f seconds of audio data%n", seconds);
            int numSamples = dataSize / 4;
            short[] firstChannel = new short[numSamples];
            short[] secondChannel = new short[numSamples];
            short[] firstChannelfadeIN = new short[numSamples];
            short[] secondChannelfadeIN = new short[numSamples];
            System.out.printf("There are %d total samples%n", numSamples);
            double spacing = (double)screenWidth / (double)(numSamples);
            double xOffset = 0.0;
            double centerLine = screenHeight / 2;
            double topCenterLine = screenHeight / 4;
            double botCenterLine = centerLine + topCenterLine;

            double lastX = 0.0;
            double lastY = centerLine;
            int smpCount = 0;
            
            double dbThreshold = -5.0;
            double thresholdAmplitude = 32767 * Math.pow(10, dbThreshold / 20);
            
            for(int idx = 0; idx < dataSize; idx += smpBytes*numChn)
            {
                int chn1offset = bytesToIntLE(wavData, idx, smpBytes);
                firstChannel[smpCount] = (short)chn1offset;
                firstChannelfadeIN[smpCount] = (short)chn1offset;
                int chn2offset = bytesToIntLE(wavData, idx+smpBytes, smpBytes);
                secondChannel[smpCount] = (short)chn2offset;
                secondChannelfadeIN[smpCount] = (short)chn2offset;

                if(smpCount % 10 == 0) {
                    Graphics g = cnv.getGraphics();
                    g.setColor(new Color(0x60, 0x60, 0x80));

                    if (chn1offset > 0) {
                        double yOffset = topCenterLine * (chn1offset / maxValue);
                        //g.drawLine((int)lastX, (int)lastY, (int)xOffset, (int)(centerLine-yOffset));
                        g.fillRect((int) xOffset, (int) (topCenterLine - yOffset), 1, 1);
                        //System.out.printf("Offset is +%.4fdB%n", (offset/maxValue)*96);
                        lastY = topCenterLine - yOffset;
                    }
                    if (chn1offset < 0) {
                        double yOffset = topCenterLine * (chn1offset / minValue);
                        //g.drawLine((int)lastX, (int)lastY, (int)xOffset, (int)(centerLine+yOffset));
                        //System.out.printf("Offset is -%.4fdB%n", (offset/minValue)*96);
                        g.fillRect((int) xOffset, (int) (topCenterLine + yOffset), 1, 1);
                        lastY = topCenterLine + yOffset;
                    }
                    if (chn2offset > 0) {
                        double yOffset = topCenterLine * (chn2offset / maxValue);
                        //g.drawLine((int)lastX, (int)lastY, (int)xOffset, (int)(centerLine-yOffset));
                        g.fillRect((int) xOffset, (int) (botCenterLine - yOffset), 1, 1);
                        //System.out.printf("Offset is +%.4fdB%n", (offset/maxValue)*96);
                        lastY = botCenterLine - yOffset;
                    }
                    if (chn2offset < 0) {
                        double yOffset = topCenterLine * (chn2offset / minValue);
                        //g.drawLine((int)lastX, (int)lastY, (int)xOffset, (int)(centerLine+yOffset));
                        //System.out.printf("Offset is -%.4fdB%n", (offset/minValue)*96);
                        g.fillRect((int) xOffset, (int) (botCenterLine + yOffset), 1, 1);
                        lastY = botCenterLine + yOffset;
                    }
                }
                //lastX = xOffset;
                xOffset += spacing;
                smpCount++;
            }
            
            //FADE OUT
            double fadeAmt = 1.0;
            double fadeStep = 1.0 / numSamples;
            //System.out.println(fadeStep);
            for(int i = 0; i < dataSize/(smpBytes*numChn); i++) {
                if (firstChannel[i] > 0) {
                    firstChannel[i] = (short) ((firstChannel[i] / maxValue) * fadeAmt * maxValue);
                } else {
                    firstChannel[i] = (short) ((firstChannel[i] / minValue) * fadeAmt * minValue);
                }
                if (secondChannel[i] > 0) {
                    secondChannel[i] = (short) ((secondChannel[i] / maxValue) * fadeAmt * maxValue);
                } else {
                    secondChannel[i] = (short) ((secondChannel[i] / minValue) * fadeAmt * minValue);
                }
                fadeAmt -= fadeStep;
                //System.out.println(fadeAmt);
            }

            smpCount = 0;
            xOffset = 0.0;
            for(int idx = 0; idx < dataSize / (smpBytes*numChn); idx++)
            {
                int chn1offset = firstChannel[idx];
                int chn2offset = secondChannel[idx];

                if(smpCount % 10 == 0) {
                    Graphics g = cnv.getGraphics();
                    g.setColor(new Color(0xff, 0x00, 0x00));

                    if (chn1offset > 0) {
                        double yOffset = topCenterLine * (chn1offset / maxValue);
                        //g.drawLine((int)lastX, (int)lastY, (int)xOffset, (int)(centerLine-yOffset));
                        g.fillRect((int) xOffset, (int) (topCenterLine - yOffset), 1, 1);
                        //System.out.printf("Offset is +%.4fdB%n", (offset/maxValue)*96);
                        lastY = topCenterLine - yOffset;
                    }
                    if (chn1offset < 0) {
                        double yOffset = topCenterLine * (chn1offset / minValue);
                        //g.drawLine((int)lastX, (int)lastY, (int)xOffset, (int)(centerLine+yOffset));
                        //System.out.printf("Offset is -%.4fdB%n", (offset/minValue)*96);
                        g.fillRect((int) xOffset, (int) (topCenterLine + yOffset), 1, 1);
                        lastY = topCenterLine + yOffset;
                    }
                    if (chn2offset > 0) {
                        double yOffset = topCenterLine * (chn2offset / maxValue);
                        //g.drawLine((int)lastX, (int)lastY, (int)xOffset, (int)(centerLine-yOffset));
                        g.fillRect((int) xOffset, (int) (botCenterLine - yOffset), 1, 1);
                        //System.out.printf("Offset is +%.4fdB%n", (offset/maxValue)*96);
                        lastY = botCenterLine - yOffset;
                    }
                    if (chn2offset < 0) {
                        double yOffset = topCenterLine * (chn2offset / minValue);
                        //g.drawLine((int)lastX, (int)lastY, (int)xOffset, (int)(centerLine+yOffset));
                        //System.out.printf("Offset is -%.4fdB%n", (offset/minValue)*96);
                        g.fillRect((int) xOffset, (int) (botCenterLine + yOffset), 1, 1);
                        lastY = botCenterLine + yOffset;
                    }
                }
                //lastX = xOffset;
                xOffset += spacing;
                smpCount++;
            }
            
            //FADE IN
            double fadeInAmt = 0.0;
            double fadeInStep = 1.0 / numSamples;
            //System.out.println(fadeStep);
            for(int i = 0; i < dataSize/(smpBytes*numChn); i++) {
                if (firstChannelfadeIN[i] > 0) {
                	firstChannelfadeIN[i] = (short) ((firstChannelfadeIN[i] / maxValue) * fadeAmt * maxValue);
                } else {
                	firstChannelfadeIN[i] = (short) ((firstChannelfadeIN[i] / minValue) * fadeAmt * minValue);
                }
                if (secondChannelfadeIN[i] > 0) {
                	secondChannelfadeIN[i] = (short) ((secondChannelfadeIN[i] / maxValue) * fadeAmt * maxValue);
                } else {
                	secondChannelfadeIN[i] = (short) ((secondChannelfadeIN[i] / minValue) * fadeAmt * minValue);
                }
                fadeAmt += fadeStep;
                //System.out.println(fadeAmt);
            }

            smpCount = 0;
            xOffset = 0.0;
            for(int idx = 0; idx < dataSize / (smpBytes*numChn); idx++)
            {
                int chn1offset = firstChannelfadeIN[idx];
                int chn2offset = secondChannelfadeIN[idx];

                if(smpCount % 10 == 0) {
                    Graphics g = cnv.getGraphics();
                    g.setColor(new Color(0x00, 0xff, 0x00));

                    if (chn1offset > 0) {
                        double yOffset = topCenterLine * (chn1offset / maxValue);
                        //g.drawLine((int)lastX, (int)lastY, (int)xOffset, (int)(centerLine-yOffset));
                        g.fillRect((int) xOffset, (int) (topCenterLine - yOffset), 1, 1);
                        //System.out.printf("Offset is +%.4fdB%n", (offset/maxValue)*96);
                        lastY = topCenterLine - yOffset;
                    }
                    if (chn1offset < 0) {
                        double yOffset = topCenterLine * (chn1offset / minValue);
                        //g.drawLine((int)lastX, (int)lastY, (int)xOffset, (int)(centerLine+yOffset));
                        //System.out.printf("Offset is -%.4fdB%n", (offset/minValue)*96);
                        g.fillRect((int) xOffset, (int) (topCenterLine + yOffset), 1, 1);
                        lastY = topCenterLine + yOffset;
                    }
                    if (chn2offset > 0) {
                        double yOffset = topCenterLine * (chn2offset / maxValue);
                        //g.drawLine((int)lastX, (int)lastY, (int)xOffset, (int)(centerLine-yOffset));
                        g.fillRect((int) xOffset, (int) (botCenterLine - yOffset), 1, 1);
                        //System.out.printf("Offset is +%.4fdB%n", (offset/maxValue)*96);
                        lastY = botCenterLine - yOffset;
                    }
                    if (chn2offset < 0) {
                        double yOffset = topCenterLine * (chn2offset / minValue);
                        //g.drawLine((int)lastX, (int)lastY, (int)xOffset, (int)(centerLine+yOffset));
                        //System.out.printf("Offset is -%.4fdB%n", (offset/minValue)*96);
                        g.fillRect((int) xOffset, (int) (botCenterLine + yOffset), 1, 1);
                        lastY = botCenterLine + yOffset;
                    }
                }
                //lastX = xOffset;
                xOffset += spacing;
                smpCount++;
            }
            
            //CLAMP
            smpCount = 0;
            xOffset = 0.0;
            for(int idx = 0; idx < dataSize; idx += smpBytes*numChn) {
                int chn1offset = bytesToIntLE(wavData, idx, smpBytes);
                // Clipping the first channel
                if (Math.abs(chn1offset) > thresholdAmplitude) {
                    chn1offset = (int)(Math.signum(chn1offset) * thresholdAmplitude);
                }
                firstChannel[smpCount] = (short)chn1offset;

                int chn2offset = bytesToIntLE(wavData, idx+smpBytes, smpBytes);
                // Clipping the second channel
                if (Math.abs(chn2offset) > thresholdAmplitude) {
                    chn2offset = (int)(Math.signum(chn2offset) * thresholdAmplitude);
                }
                secondChannel[smpCount] = (short)chn2offset;
                secondChannelfadeIN[smpCount] = (short)chn2offset;

                if(smpCount % 10 == 0) {
                    Graphics g = cnv.getGraphics();
                    g.setColor(new Color(0xff, 0xff, 0x00));

                    if (chn1offset > 0) {
                        double yOffset = topCenterLine * (chn1offset / maxValue);
                        //g.drawLine((int)lastX, (int)lastY, (int)xOffset, (int)(centerLine-yOffset));
                        g.fillRect((int) xOffset, (int) (topCenterLine - yOffset), 1, 1);
                        //System.out.printf("Offset is +%.4fdB%n", (offset/maxValue)*96);
                        lastY = topCenterLine - yOffset;
                    }
                    if (chn1offset < 0) {
                        double yOffset = topCenterLine * (chn1offset / minValue);
                        //g.drawLine((int)lastX, (int)lastY, (int)xOffset, (int)(centerLine+yOffset));
                        //System.out.printf("Offset is -%.4fdB%n", (offset/minValue)*96);
                        g.fillRect((int) xOffset, (int) (topCenterLine + yOffset), 1, 1);
                        lastY = topCenterLine + yOffset;
                    }
                    if (chn2offset > 0) {
                        double yOffset = topCenterLine * (chn2offset / maxValue);
                        //g.drawLine((int)lastX, (int)lastY, (int)xOffset, (int)(centerLine-yOffset));
                        g.fillRect((int) xOffset, (int) (botCenterLine - yOffset), 1, 1);
                        //System.out.printf("Offset is +%.4fdB%n", (offset/maxValue)*96);
                        lastY = botCenterLine - yOffset;
                    }
                    if (chn2offset < 0) {
                        double yOffset = topCenterLine * (chn2offset / minValue);
                        //g.drawLine((int)lastX, (int)lastY, (int)xOffset, (int)(centerLine+yOffset));
                        //System.out.printf("Offset is -%.4fdB%n", (offset/minValue)*96);
                        g.fillRect((int) xOffset, (int) (botCenterLine + yOffset), 1, 1);
                        lastY = botCenterLine + yOffset;
                    }
                }
                //lastX = xOffset;
                xOffset += spacing;
                smpCount++;
            }

        } catch (FileNotFoundException fnfEx) {
            fnfEx.printStackTrace();
        } catch (IOException ioEx) {
            ioEx.printStackTrace();
        }
    }
}
