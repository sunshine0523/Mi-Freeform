package com.sunshine.freeform.systemapi;

import android.graphics.Rect;

import java.util.Objects;

public final class Size {
    private final int width;
    private final int height;

    public Size(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }


    public Rect toRect() {
        return new Rect(0, 0, width, height);
    }

    @Override
    public int hashCode() {
        return Objects.hash(width, height);
    }

    @Override
    public String toString() {
        return "Size{" + "width=" + width + ", height=" + height + '}';
    }
}
