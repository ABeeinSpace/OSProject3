package com.bee;

import java.util.concurrent.LinkedBlockingQueue;

/*
 *
 * Models a thread-safe, non-preemptive Scheduler.
 *
 * @author Aidan Border
 * @author Austin Scheetz
 *
 */
public class MyScheduler {
    private String property; // The parameter we're measuring during this test run
    private LinkedBlockingQueue<Job> incomingQueue; // The queue of jobs that the scheduler needs to work on.
    private LinkedBlockingQueue<Job> outgoingQueue; // The queue housing jobs we've already worked on and completed.

    /**
     * @param numJobs
     * @param property
     */
    public MyScheduler(int numJobs, String property) {
        this.property = property;
        this.incomingQueue = new LinkedBlockingQueue<>();
        this.outgoingQueue = new LinkedBlockingQueue<>();
    }

    /**
     * This is our main method for the Scheduler. All jobs that come in will eventually have this method run in order to give them CPU time and all that. 
     */
    public void run() {
        // TODO Auto-generated method stub
        // TODO Create Filter based Off Property
        // TODO Implement Scheduler Methodology to Maximize maxWait
        // TODO Implement Scheduler Methodology to Maximize Combined
        // TODO Implement Scheduler Methodology to Maximize Deadlines
        // TODO Implement Scheduler Methodology to Maximize avgWait
        throw new UnsupportedOperationException("Unimplemented method 'run'");
    }

    /**
     * @return LinkedBlockingQueue<Job> outgoingQueue
     */
    public LinkedBlockingQueue<Job> getOutgoingQueue() {
        return outgoingQueue;
    }

    /**
     * @return LinkedBlockingQueue<Job> incomingQueue
     */
    public LinkedBlockingQueue<Job> getIncomingQueue() {
        return incomingQueue;
    }

    @Override
    public String toString() {
        return "MyScheduler [property=" + property + ", incomingQueue=" + incomingQueue + ", outgoingQueue="
                + outgoingQueue + "]";
    }
}
