package com.bee;

import java.util.concurrent.LinkedBlockingQueue;


/*
 *
 * Models a thread-safe thread scheduler 
 *
 * @author Aidan Border
 * @author Quade Leonard
 *
 */
public class MyScheduler {
    private int numJobs;
    private String property;
    private LinkedBlockingQueue<Job> incomingJobsQueue;

    public MyScheduler(int numJobs, String property) {
        this.numJobs = numJobs;
        this.property = property;
        this.incomingJobsQueue = new LinkedBlockingQueue<Job>();
    }

    public void run() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'run'");
    }

    public LinkedBlockingQueue<Job> getIncomingQueue() {
        return incomingJobsQueue;
    }
    public LinkedBlockingQueue<Job> getOutgoingQueue() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getOutgoingQueue'");
    }

}
