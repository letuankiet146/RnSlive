package com.tk.rnslive.dto;

public class PriceDto {

    private double top;
    private double middle;
    private double bottom;

    public PriceDto(double top, double middle, double bottom) {
        this.top = top;
        this.middle = middle;
        this.bottom = bottom;
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
}