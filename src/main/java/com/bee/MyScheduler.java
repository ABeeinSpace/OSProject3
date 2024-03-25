package com.bee;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

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
    private Semaphore locker;

    /**
     * @param numJobs
     * @param property
     */
    public MyScheduler(int numJobs, String property) {
        this.property = property;
        this.incomingQueue = new LinkedBlockingQueue<>();
        this.outgoingQueue = new LinkedBlockingQueue<>();
        this.locker = new Semaphore(numJobs/2);
    }

    /**
     * This is our main method for the Scheduler. All jobs that come in will eventually have this method run in order to give them CPU time and all that. 
     */
    public void run() {
        // TODO Auto-generated method stub
        // TODO Create Filter based Off Property
        switch (this.property) {
            case "max wait":
                ArrayList<Job> inbetweener = new ArrayList<>();    
                try {
                    this.locker.acquire();
                    inbetweener.add(this.incomingQueue.take());
                    this.locker.release();
                } catch (Exception e) {
                    System.err.println("Failed to transfer data...");
                }
                this.outgoingQueue.add(inbetweener.remove(0));
            case "avg case":
                Job shortestJob = incomingQueue.peek();
                Iterator<Job> incomingIterator = incomingQueue.iterator();
                for (int i = 0; i < incomingQueue.size(); i++) {
                    Job nextJob = incomingIterator.next();
                    if (nextJob.getLength() < shortestJob.getLength()) {
                        shortestJob = nextJob; 
                    }
                }
                incomingQueue.remove(shortestJob);
                this.outgoingQueue.add(shortestJob);
            case "combined":
                // TODO Implement Scheduler Methodology to Maximize Combined (Algorithm: SJF + FCFS)
            case "deadlines":
                Job shortestDeadline = incomingQueue.peek();
                Iterator<Job> secondIncomingIterator = incomingQueue.iterator();
                for (int i = 0; i < incomingQueue.size(); i++) {
                    Job candidate = secondIncomingIterator.next();
                    if (candidate.getDeadline() < shortestDeadline.getDeadline()) {
                        shortestDeadline = candidate;
                    }
                }
                incomingQueue.remove(shortestDeadline);
                outgoingQueue.add(shortestDeadline);
        }
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
