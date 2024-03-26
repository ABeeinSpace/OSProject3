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
    private LinkedBlockingQueue<Job> workQueue;
    private LinkedBlockingQueue<Job> doneQueue;

    /**
     * @param numJobs  The number of jobs we're going to use for this run
     * @param property The property/attribute to maximize for this run
     */
    public MyScheduler(int numJobs, String property) {
        this.property = property;
        this.incomingQueue = new LinkedBlockingQueue<>(numJobs);
        this.outgoingQueue = new LinkedBlockingQueue<>(numJobs);
        this.workQueue = new LinkedBlockingQueue<>(numJobs);
        this.doneQueue = new LinkedBlockingQueue<>(numJobs);
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
        
        Thread incomingThread = new Thread(() -> {
            getJobs();
        });

        Thread outgoingThread = new Thread(() -> {
            handleFinishedJobs();
        });

        incomingThread.start();
        outgoingThread.start();

        switch (this.property) {
            case "max wait":
                try {
                    this.locker.acquire();
                    doneQueue.add(this.workQueue.take());
                    this.locker.release();
                } catch (Exception e) {
                    System.err.println("Failed to transfer data...");
                }
                break;

            case "avg wait":
                for (int i = 0; i < this.workQueue.size(); i++) {
                    Job shortestJob = this.workQueue.peek();
                    long shortestWait = shortestJob.getLength();
                    Iterator<Job> incomingIterator = this.workQueue.iterator();
                    while (incomingIterator.hasNext()) {
                        Job incomingJob = incomingIterator.next();
                        if (incomingJob.getLength() < shortestWait) {
                            shortestJob = incomingJob;
                            shortestWait = shortestJob.getLength();
                        }
                    }
                    this.workQueue.remove(shortestJob);
                    // inbetweener.add(shortestJob);
                    doneQueue.add(shortestJob);
                }

                break;

            case "combined":
                for (int i = 0; i < this.workQueue.size(); i++) {
                    if (workQueue.size() == 1) {
                        // Use FCFS if there aren't multiple jobs in the queue to be processed
                        try {
                            this.locker.acquire();
                            doneQueue.add(this.workQueue.take());
                            this.locker.release();
                        } catch (Exception e) {
                            System.err.println("Failed to take from work Queue!!!");
                        }
                    } else {
                        // Use SJF if there are multiple jobs in the queue waiting to be
                        // processed
                        Job shortestCombinedJob = workQueue.peek();
                        long shortestCombinedWait = shortestCombinedJob.getLength();
                        Iterator<Job> secondIncomingIterator = workQueue.iterator();
                        while (secondIncomingIterator.hasNext()) {
                            Job incomingJob = secondIncomingIterator.next();
                            if (incomingJob.getLength() < shortestCombinedWait) {
                                shortestCombinedJob = incomingJob;
                                shortestCombinedWait = shortestCombinedJob.getLength();
                            }
                        }
                        workQueue.remove(shortestCombinedJob);
                        doneQueue.add(shortestCombinedJob);
                        // inbetweener.add(shortestCombinedJob);
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
                    incomingQueue.remove(shortestDeadline);
                    outgoingQueue.add(shortestDeadline);
                }
                break;
        }
    }

    /**
     * Moves elements from the incomingQueue to the workQueue to be used by the scheduler
     */
    private void getJobs() {
        for (Job element: incomingQueue) {
            workQueue.offer(element);
            incomingQueue.remove(element);
        }
    }

    /**
     * Move elements from doneQueue to the outgoingQueue
     */
    private void handleFinishedJobs() {
        while (!doneQueue.isEmpty()) {
            outgoingQueue.offer(doneQueue.remove());
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
