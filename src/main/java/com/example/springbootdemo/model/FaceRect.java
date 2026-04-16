package com.example.springbootdemo.model;

/**
 * 人脸框
 */
public class FaceRect implements Cloneable {
    public int left = 0;
    public int top = 0;
    public int right = 0;
    public int bottom = 0;

    public FaceRect() {

    }

    public FaceRect(int left, int top, int right, int bottom) {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
    }

    public int getHeight() {
        return bottom - top + 1;
    }

    public int getWidth() {
        return right - left + 1;
    }

    public boolean isEmpty() {
        if (left == 0 && top == 0 && right == 0 && bottom == 0) {
            return true;
        }

        return false;
    }

    public void reset() {
        left = 0;
        top = 0;
        right = 0;
        bottom = 0;
    }

    public String toString() {
        return "(" + left + ", " + top + ")-" + "(" + right + ", " + bottom + ")";
    }

    @Override
    protected FaceRect clone() {
        // TODO Auto-generated method stub
        return new FaceRect(left, top, right, bottom);
    }
}

