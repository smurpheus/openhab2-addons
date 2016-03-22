package org.openhab.binding.wifiled.handler;

import org.eclipse.smarthome.core.library.types.HSBType;
import org.eclipse.smarthome.core.library.types.PercentType;

import static java.lang.Math.max;

/**
 * Internal LED state.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class InternalLedState {

    /** Values for the colors red, green, blue from 0 to 1. */
    double r, g, b;
    /** White value from 0 to 1. */
    double w;

    public InternalLedState() {
        this(0, 0, 0, 0);
    }

    public InternalLedState(double r, double g, double b, double w) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.w = w;
    }

    public static InternalLedState fromRGBW(int r, int g, int b, int w) {
        return new InternalLedState(conv(r), conv(g), conv(b), conv(w));
    }

    public InternalLedState withColor(HSBType color) {
        return new InternalLedState(
            color.getRed().doubleValue()   / 100,
            color.getGreen().doubleValue() / 100,
            color.getBlue().doubleValue()  / 100,
            w
        );
    }

    public InternalLedState withBrightness(double brightness) {
        return new InternalLedState(
            r * brightness,
            g * brightness,
            b * brightness,
            w
        );
    }

    public InternalLedState withWhite(double w) {
        return new InternalLedState(
            r,
            g,
            b,
            w
        );
    }

    public PercentType toHSBType() {
        return HSBType.fromRGB(conv(r), conv(g), conv(b));
    }



    /**
     * Fades from this color to the that color according to the given progress value from 0 (this color)
     * to 1 (that color).
     *
     * @param that that color
     * @param progress value between 0 (this color) and 1 (that color)
     * @return faded color
     */
    public InternalLedState fade(InternalLedState that, double progress) {
        double invProgress = 1 - progress;

        return new InternalLedState(
            this.r * invProgress + that.r * progress,
            this.g * invProgress + that.g * progress,
            this.b * invProgress + that.b * progress,
            this.w * invProgress + that.w * progress
        );
    }

    /**
     * Returns the brightness or the RGB color.
     *
     * @return value between 0 and 1
     */
    public double getBrightness() {
        return max(r, max(g, b));
    }

    /**
     * Returns the white value.
     *
     * @return value between 0 and 1
     */
    public double getWhite() {
        return w;
    }

    private static double conv(int v) {
        return (double) v / 255;
    }

    private static int conv(double v) {
        return (int) (v * 255 + 0.5);
    }

    /**
     * Returns red value.
     *
     * @return value between 0 and 255
     */
    public int getR() {
        return conv(r);
    }

    /**
     * Returns green value.
     *
     * @return value between 0 and 255
     */
    public int getG() {
        return conv(g);
    }

    /**
     * Returns blue value.
     *
     * @return value between 0 and 255
     */
    public int getB() {
        return conv(b);
    }

    /**
     * Returns white value.
     *
     * @return value between 0 and 255
     */
    public int getW() {
        return conv(w);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        InternalLedState that = (InternalLedState) o;

        return Double.compare(that.r, r) == 0
            && Double.compare(that.g, g) == 0
            && Double.compare(that.b, b) == 0
            && Double.compare(that.w, w) == 0;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = Double.doubleToLongBits(r);
        result = (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(g);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(b);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(w);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "InternalLedState{" +
            "r=" + r +
            ", g=" + g +
            ", b=" + b +
            ", w=" + w +
            '}';
    }
}
