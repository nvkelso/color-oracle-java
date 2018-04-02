/*
 * Simulator.java
 *
 * Created on February 5, 2007, 11:22 AM
 *
 */
package ika.colororacle;

import ika.colororacle.ColorOracle.Simulation;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.*;

/**
 * A simulator for color-impaired vision (deuteranopia, protanopia and
 * tritanopia).
 *
 * For a description of the algorithm, see: Vienot, F., Brettel, H., Mollon,
 * J.D., (1999). Digital video colourmaps for checking the legibility of
 * displays by dichromats. Color Research and Application 24, 243-252.
 *
 * @author Bernhard Jenny, Institute of Cartography, ETH Zurich.
 */
public class Simulator {

    /**
     * Default screen gamma on Windows is 2.2.
     */
    private static final double GAMMA = 2.2;
    private static final double GAMMA_INV = 1. / GAMMA;
    
    /**
     * A lookup table for the conversion from gamma-corrected sRGB values
     * [0..255] to linear RGB values [0..32767].
     */
    private static final short[] SRGB_TO_LINRGB;

    static {
        // initialize SRGB_TO_LINRGB
        SRGB_TO_LINRGB = new short[256];
        for (int i = 0; i < 256; i++) {
            // compute linear rgb between 0 and 1
            final double lin = (0.992052 * Math.pow(i / 255., GAMMA) + 0.003974);

            // scale linear rgb to 0..32767
            SRGB_TO_LINRGB[i] = (short) (lin * 32767.);
        }
    }
    /**
     * A lookup table for the conversion of linear RGB values [0..255] to
     * gamma-corrected sRGB values [0..255].
     */
    private static final byte[] LINRGB_TO_SRGB;

    static {
        // initialize LINRGB_TO_SRGB
        LINRGB_TO_SRGB = new byte[256];
        for (int i = 0; i < 256; i++) {
            LINRGB_TO_SRGB[i] = (byte) (255. * Math.pow(i / 255., GAMMA_INV));
        }
    }
    /**
     * Use this BufferedImageOp for the simulation.
     */
    private BufferedImageOp op;

    /**
     * Creates a new instance of Simulator
     */
    protected Simulator() {
    }

    /**
     * Filter an image and return a new image with the filtered result.
     *
     * @normal The image with normal vision.
     * @return The image with simulated color vision impairment.
     */
    protected BufferedImage filter(BufferedImage normal) {
        return op.filter(normal, null);
    }

    /**
     * Simulate color impaired vision.
     *
     * @param simulationType The type of impairment to simulate.
     */
    protected void simulate(Simulation simulationType) {
        switch (simulationType) {
            case deutan:
                op = new RedGreenFilter(9591, 23173, -730);
                break;
            case protan:
                op = new RedGreenFilter(3683, 29084, 131);
                break;
            case tritan:
                op = new TritanFilter();
                break;
            case grayscale:
                op = new GrayscaleFilter();
                break;
        }
    }

    /**
     * A red-green blindness filter (deuteranopia and protanopia).
     */
    private class RedGreenFilter implements BufferedImageOp {

        private final int k1;
        private final int k2;
        private final int k3;

        public RedGreenFilter(int k1, int k2, int k3) {
            this.k1 = k1;
            this.k2 = k2;
            this.k3 = k3;
        }

        @Override
        public BufferedImage filter(BufferedImage src, BufferedImage dst) {
            if (dst == null) {
                dst = createCompatibleDestImage(src, null);
            }

            // make sure the two images have the same size, color space, etc.
            // MISSING !!! ???
            DataBufferInt inBuffer = (DataBufferInt) src.getRaster().getDataBuffer();
            DataBufferInt outBuffer = (DataBufferInt) dst.getRaster().getDataBuffer();
            int[] inData = inBuffer.getData();
            int[] outData = outBuffer.getData();

            int prevIn = 0;
            int prevOut = 0;
            final int length = inData.length;
            for (int i = 0; i < length; i++) {
                final int in = inData[i];
                if (in == prevIn) {
                    outData[i] = prevOut;
                } else {

                    final int r = (0xff0000 & in) >> 16;
                    final int g = (0xff00 & in) >> 8;
                    final int b = 0xff & in;

                    // get linear rgb values in the range 0..2^15-1
                    final int r_lin = SRGB_TO_LINRGB[r];
                    final int g_lin = SRGB_TO_LINRGB[g];
                    final int b_lin = SRGB_TO_LINRGB[b];

                    // simulated red and green are identical
                    // scale the matrix values to 0..2^15 for integer computations 
                    // of the simulated protan values.
                    // divide after the computation by 2^15 to rescale.
                    // also divide by 2^15 and multiply by 2^8 to scale the linear rgb to 0..255
                    // total division is by 2^15 * 2^15 / 2^8 = 2^22
                    // shift the bits by 22 places instead of dividing
                    int r_blind = (int) (k1 * r_lin + k2 * g_lin) >> 22;
                    int b_blind = (int) (k3 * r_lin - k3 * g_lin + 32768 * b_lin) >> 22;

                    if (r_blind < 0) {
                        r_blind = 0;
                    } else if (r_blind > 255) {
                        r_blind = 255;
                    }

                    if (b_blind < 0) {
                        b_blind = 0;
                    } else if (b_blind > 255) {
                        b_blind = 255;
                    }

                    // convert reduced linear rgb to gamma corrected rgb
                    int red = LINRGB_TO_SRGB[r_blind];
                    red = red >= 0 ? red : 256 + red; // from unsigned to signed
                    int blue = LINRGB_TO_SRGB[b_blind];
                    blue = blue >= 0 ? blue : 256 + blue; // from unsigned to signed

                    final int out = 0xff000000 | red << 16 | red << 8 | blue;

                    outData[i] = out;
                    prevIn = in;
                    prevOut = out;
                }
            }

            return dst;
        }

        @Override
        public Rectangle2D getBounds2D(BufferedImage src) {
            return src.getRaster().getBounds();
        }

        @Override
        public BufferedImage createCompatibleDestImage(BufferedImage src,
                ColorModel destCM) {
            if (destCM == null) {
                destCM = src.getColorModel();
            }
            int width = src.getWidth();
            int height = src.getHeight();
            BufferedImage image = new BufferedImage(destCM,
                    destCM.createCompatibleWritableRaster(width, height),
                    destCM.isAlphaPremultiplied(), null);
            return image;
        }

        @Override
        public Point2D getPoint2D(Point2D srcPt, Point2D dstPt) {
            if (dstPt == null) {
                dstPt = new Point2D.Float();
            }
            dstPt.setLocation(srcPt.getX(), srcPt.getY());
            return dstPt;
        }

        @Override
        public RenderingHints getRenderingHints() {
            return null;
        }
    }

    /**
     * A filter for simulated Tritanopia.
     */
    private class TritanFilter implements BufferedImageOp {

        @Override
        public BufferedImage filter(BufferedImage src, BufferedImage dst) {

            /* Code for tritan simulation from GIMP 2.2
             *  This could be optimised for speed.
             *  Performs tritan color image simulation based on
             *  Brettel, Vienot and Mollon JOSA 14/10 1997
             *  L,M,S for lambda=475,485,575,660
             *
             * Load the LMS anchor-point values for lambda = 475 & 485 nm (for
             * protans & deutans) and the LMS values for lambda = 575 & 660 nm
             * (for tritans)
             */
            final float anchor_e0 = 0.05059983f + 0.08585369f + 0.00952420f;
            final float anchor_e1 = 0.01893033f + 0.08925308f + 0.01370054f;
            final float anchor_e2 = 0.00292202f + 0.00975732f + 0.07145979f;
            final float inflection = anchor_e1 / anchor_e0;

            /* Set 1: regions where lambda_a=575, set 2: lambda_a=475 */
            final float a1 = -anchor_e2 * 0.007009f;
            final float b1 = anchor_e2 * 0.0914f;
            final float c1 = anchor_e0 * 0.007009f - anchor_e1 * 0.0914f;
            final float a2 = anchor_e1 * 0.3636f - anchor_e2 * 0.2237f;
            final float b2 = anchor_e2 * 0.1284f - anchor_e0 * 0.3636f;
            final float c2 = anchor_e0 * 0.2237f - anchor_e1 * 0.1284f;

            if (dst == null) {
                dst = createCompatibleDestImage(src, null);
            }

            // make sure the two images have the same size, color space, etc.
            // MISSING FIXME
            DataBufferInt inBuffer = (DataBufferInt) src.getRaster().getDataBuffer();
            DataBufferInt outBuffer = (DataBufferInt) dst.getRaster().getDataBuffer();
            int[] inData = inBuffer.getData();
            int[] outData = outBuffer.getData();

            int prevIn = 0;
            int prevOut = 0;
            final int length = inData.length;
            for (int i = 0; i < length; i++) {
                final int in = inData[i];
                if (in == prevIn) {
                    outData[i] = prevOut;
                } else {
                    final int rgb = inData[i];

                    int r = (0xff0000 & rgb) >> 16;
                    int g = (0xff00 & rgb) >> 8;
                    int b = 0xff & rgb;

                    // get linear rgb values in the range 0..2^15-1
                    r = SRGB_TO_LINRGB[r];
                    g = SRGB_TO_LINRGB[g];
                    b = SRGB_TO_LINRGB[b];

                    /* Convert to LMS (dot product with transform matrix) */
                    final float L = (r * 0.05059983f + g * 0.08585369f + b * 0.00952420f) / 32767.f;
                    final float M = (r * 0.01893033f + g * 0.08925308f + b * 0.01370054f) / 32767.f;
                    float S; // = (r * 0.00292202f + g * 0.00975732f + b * 0.07145979f) / 32767.f;

                    final float tmp = M / L;

                    /* See which side of the inflection line we fall... */
                    if (tmp < inflection) {
                        S = -(a1 * L + b1 * M) / c1;
                    } else {
                        S = -(a2 * L + b2 * M) / c2;
                    }

                    /* Convert back to RGB (cross product with transform matrix) */
                    int ired = (int) (255.f * (L * 30.830854f
                            - M * 29.832659f + S * 1.610474f));
                    int igreen = (int) (255.f * (-L * 6.481468f
                            + M * 17.715578f - S * 2.532642f));
                    int iblue = (int) (255.f * (-L * 0.375690f
                            - M * 1.199062f + S * 14.273846f));

                    // convert reduced linear rgb to gamma corrected rgb
                    if (ired < 0) {
                        ired = 0;
                    } else if (ired > 255) {
                        ired = 255;
                    } else {
                        ired = LINRGB_TO_SRGB[ired];
                        ired = ired >= 0 ? ired : 256 + ired; // from unsigned to signed
                    }
                    if (igreen < 0) {
                        igreen = 0;
                    } else if (igreen > 255) {
                        igreen = 255;
                    } else {
                        igreen = LINRGB_TO_SRGB[igreen];
                        igreen = igreen >= 0 ? igreen : 256 + igreen; // from unsigned to signed
                    }
                    if (iblue < 0) {
                        iblue = 0;
                    } else if (iblue > 255) {
                        iblue = 255;
                    } else {
                        iblue = LINRGB_TO_SRGB[iblue];
                        iblue = iblue >= 0 ? iblue : 256 + iblue; // from unsigned to signed
                    }

                    final int out = (int) (ired << 16 | igreen << 8 | iblue | 0xff000000);

                    outData[i] = out;
                    prevIn = in;
                    prevOut = out;
                }
            }

            return dst;
        }

        @Override
        public Rectangle2D getBounds2D(BufferedImage src) {
            return src.getRaster().getBounds();
        }

        @Override
        public BufferedImage createCompatibleDestImage(BufferedImage src,
                ColorModel destCM) {
            if (destCM == null) {
                destCM = src.getColorModel();
            }
            int width = src.getWidth();
            int height = src.getHeight();
            BufferedImage image = new BufferedImage(destCM,
                    destCM.createCompatibleWritableRaster(width, height),
                    destCM.isAlphaPremultiplied(), null);
            return image;
        }

        @Override
        public Point2D getPoint2D(Point2D srcPt, Point2D dstPt) {
            if (dstPt == null) {
                dstPt = new Point2D.Float();
            }
            dstPt.setLocation(srcPt.getX(), srcPt.getY());
            return dstPt;
        }

        @Override
        public RenderingHints getRenderingHints() {
            return null;
        }
    }

    /**
     * A filter for grayscale conversion: perceptual luminance-preserving
     * conversion to grayscale.
     * https://en.wikipedia.org/wiki/Grayscale#Colorimetric_(perceptual_luminance-preserving)_conversion_to_grayscale
     */
    private class GrayscaleFilter implements BufferedImageOp {

        @Override
        public BufferedImage filter(BufferedImage src, BufferedImage dst) {
            if (dst == null) {
                dst = createCompatibleDestImage(src, null);
            }

            // make sure the two images have the same size, color space, etc.
            // MISSING FIXME
            DataBufferInt inBuffer = (DataBufferInt) src.getRaster().getDataBuffer();
            DataBufferInt outBuffer = (DataBufferInt) dst.getRaster().getDataBuffer();
            int[] inData = inBuffer.getData();
            int[] outData = outBuffer.getData();

            int prevIn = 0;
            int prevOut = 0;
            final int length = inData.length;
            for (int i = 0; i < length; i++) {
                final int in = inData[i];
                if (in == prevIn) {
                    outData[i] = prevOut;
                } else {
                    final int rgb = inData[i];

                    final int r = (0xff0000 & rgb) >> 16;
                    final int g = (0xff00 & rgb) >> 8;
                    final int b = 0xff & rgb;

                    // get linear rgb values in the range 0..2^15-1
                    final int r_lin = SRGB_TO_LINRGB[r];
                    final int g_lin = SRGB_TO_LINRGB[g];
                    final int b_lin = SRGB_TO_LINRGB[b];

                    // perceptual luminance-preserving conversion to grayscale
                    // https://en.wikipedia.org/wiki/Grayscale#Colorimetric_(perceptual_luminance-preserving)_conversion_to_grayscale
                    double luminance = 0.2126 * r_lin + 0.7152 * g_lin + 0.0722 * b_lin;
                    int linRGB = ((int) (luminance)) >> 8; // divide by 2^8 to rescale
                    
                    // convert linear rgb to gamma corrected sRGB
                    if (linRGB < 0) {
                        linRGB = 0;
                    } else if (linRGB > 255) {
                        linRGB = 255;
                    } else {
                        linRGB = LINRGB_TO_SRGB[linRGB];
                        linRGB = linRGB >= 0 ? linRGB : 256 + linRGB; // from unsigned to signed
                    }

                    final int out = (int) (linRGB << 16 | linRGB << 8 | linRGB | 0xff000000);

                    outData[i] = out;
                    prevIn = in;
                    prevOut = out;
                }
            }

            return dst;
        }

        @Override
        public Rectangle2D getBounds2D(BufferedImage src) {
            return src.getRaster().getBounds();
        }

        @Override
        public BufferedImage createCompatibleDestImage(BufferedImage src,
                ColorModel destCM) {
            if (destCM == null) {
                destCM = src.getColorModel();
            }
            int width = src.getWidth();
            int height = src.getHeight();
            BufferedImage image = new BufferedImage(destCM,
                    destCM.createCompatibleWritableRaster(width, height),
                    destCM.isAlphaPremultiplied(), null);
            return image;
        }

        @Override
        public Point2D getPoint2D(Point2D srcPt, Point2D dstPt) {
            if (dstPt == null) {
                dstPt = new Point2D.Float();
            }
            dstPt.setLocation(srcPt.getX(), srcPt.getY());
            return dstPt;
        }

        @Override
        public RenderingHints getRenderingHints() {
            return null;
        }
    }
}
