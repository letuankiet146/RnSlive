package org.tk.rnslive.dto;

public class PriceDto {

    private double top;
    private double middle;
    private double bottom;
    private long timestamp;

    public PriceDto(double top, double middle, double bottom, long timestamp) {
        this.top = top;
        this.middle = middle;
        this.bottom = bottom;
        this.timestamp = timestamp;
    }

    public double getTop() {
        return top;
    }

    public double getMiddle() {
        return middle;
    }

    public double getBottom() {
        return bottom;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "PriceDto{" +
                "top=" + top +
                ", middle=" + middle +
                ", bottom=" + bottom +
                ", timestamp=" + timestamp +
                '}';
    }
}