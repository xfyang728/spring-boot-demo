package com.example.springbootdemo.controller;

//import org.bytedeco.javacv.CanvasFrame;
//import org.bytedeco.javacv.Frame;
//import org.bytedeco.javacv.OpenCVFrameGrabber;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.swing.*;

/**
 * Controller-->service接口-->serviceImpl-->dao接口-->daoImpl-->mapper-->db
 */
@RestController
@RequestMapping("demo")
public class democontroller {
    @GetMapping("test")
    public String HHH() {

        try {
            main();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return "super.toString()";
    }

    public void main() throws Exception {

//        // Create grabber
//        OpenCVFrameGrabber grabber = new OpenCVFrameGrabber(0);
//        // 0表示调用第一个摄像头，如果有多个摄像头可以使用1,2,3...
//        grabber.start();
//
//        // Create window
//        CanvasFrame canvas = new CanvasFrame("Camera");
//        canvas.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//
//        // Process frames
//        while (true) {
//            // Read frame
//            Frame frame = grabber.grab();
//
//            // Show frame
//            canvas.showImage(frame);
//        }
    }
}
