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
    private LinkedBlockingQueue<Job> outgoingQueue; // The queue housing jobs we've already worked on and
                                                    // completed.
    private Semaphore locker;

    /**
     * @param numJobs  The number of jobs we're going to use for this run
     * @param property The property/attribute to maximize for this run
     */
    public MyScheduler(int numJobs, String property) {
        this.property = property;
        this.incomingQueue = new LinkedBlockingQueue<>(numJobs);
        this.outgoingQueue = new LinkedBlockingQueue<>(numJobs);
        this.locker = new Semaphore(numJobs / 2);
    }

    /**
     * This is our main method for the Scheduler. All jobs that come in will
     * eventually have this method run in order to give them CPU time and all
     * that.
     */
    public void run() {
        // TODO Create Filter based Off Property
        ArrayList<Job> inbetweener = new ArrayList<>();
        switch (this.property) {
            case "max wait":
                try {
                    this.locker.acquire();
                    inbetweener.add(this.incomingQueue.take());
                    this.locker.release();
                } catch (Exception e) {
                    System.err.println("Failed to transfer data...");
                }
                this.outgoingQueue.add(inbetweener.remove(0));
                break;

            case "avg wait":
                for (int i = 0; i < this.incomingQueue.size(); i++) {
                    Job shortestJob = incomingQueue.peek();
                    long shortestWait = shortestJob.getLength();
                    Iterator<Job> secondIncomingIterator = incomingQueue.iterator();
                    while (secondIncomingIterator.hasNext()) {
                        Job incomingJob = secondIncomingIterator.next();
                        if (incomingJob.getLength() < shortestWait) {
                            shortestJob = incomingJob;
                            shortestWait = shortestJob.getLength();
                        }
                    }
                    inbetweener.add(shortestJob);
                    incomingQueue.remove(shortestJob);
                    this.outgoingQueue.add(inbetweener.remove(0));
                }
                break;

            case "combined":
                for (int i = 0; i < this.incomingQueue.size(); i++) {
                    if (incomingQueue.size() == 1) {
                        // Use FCFS if there are multiple jobs in the queue to be processed
                        try {
                            this.locker.acquire();
                            inbetweener.add(this.incomingQueue.take());
                            this.locker.release();
                        } catch (Exception e) {
                            System.err.println("Failed to take from Incoming Queue!!!");
                        }
                        this.outgoingQueue.add(inbetweener.remove(0));
                    } else {
                        // Use SJF if there are multiple jobs in the queue waiting to be
                        // processed
                        Job shortestCombinedJob = incomingQueue.peek();
                        long shortestCombinedWait = shortestCombinedJob.getLength();
                        Iterator<Job> secondIncomingIterator = incomingQueue.iterator();
                        while (secondIncomingIterator.hasNext()) {
                            Job incomingJob = secondIncomingIterator.next();
                            if (incomingJob.getLength() < shortestCombinedWait) {
                                shortestCombinedJob = incomingJob;
                                shortestCombinedWait = shortestCombinedJob.getLength();
                            }
                        }
                        incomingQueue.remove(shortestCombinedJob);
                        inbetweener.add(shortestCombinedJob);
                        this.outgoingQueue.add(inbetweener.remove(0));
                    }
                }
                break;

            case "deadlines":
                for (int i = 0; i < this.incomingQueue.size(); i++) {
                    Job shortestDeadline = incomingQueue.peek();
                    Long earliestDeadline = shortestDeadline.getDeadline();
                    Iterator<Job> tertiaryIncomingIterator = incomingQueue.iterator();
                    while (tertiaryIncomingIterator.hasNext()) {
                        Job candidate = tertiaryIncomingIterator.next();
                        if (candidate.getDeadline() < earliestDeadline) {
                            shortestDeadline = candidate;
                            earliestDeadline = candidate.getDeadline();
                        }
                    }
                    
                    long currentTime = System.currentTimeMillis();
                    Job fauxJob = new Job(0, property, shortestDeadline.getDeadline());
                    if ((currentTime + shortestDeadline.getLength()) > earliestDeadline) {
                        incomingQueue.remove(shortestDeadline);
                        outgoingQueue.add(fauxJob);
                    }
                    else{
                        incomingQueue.remove(shortestDeadline);
                        outgoingQueue.add(shortestDeadline);
                    }
                }
                break;
        }
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
        return "MyScheduler [property=" + property +
                ", incomingQueue=" + incomingQueue +
                ", outgoingQueue=" + outgoingQueue + "]";
    }
}
