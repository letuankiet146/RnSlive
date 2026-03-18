package org.tk.spot.connector.dto;

public class PriceDto {

    private final double top;
    private final double middle;
    private final double bottom;
    private final long timestamp;

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
}

