package com.jd;

import java.util.Timer;

/**
 * @author: Max.Yurin
 */
public class Main {
    public static void main(String[] args) {
//        Timer timer = new Timer();
        DriveServiceTask driveServiceTask = new DriveServiceTask();
        driveServiceTask.run();
//        timer.scheduleAtFixedRate(driveServiceTask, /*5 * 60 * 1000*/ 0, 10 * 60 * 1000); //10 minutes
    }
}
