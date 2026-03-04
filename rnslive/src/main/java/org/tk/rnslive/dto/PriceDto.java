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
}