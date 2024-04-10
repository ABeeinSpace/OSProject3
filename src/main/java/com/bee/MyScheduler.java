package com.bee;

// import java.util.ArrayList;
// import java.util.Iterator;
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
    private final String property; // The parameter we're measuring during this test run
    private final LinkedBlockingQueue<Job> incomingQueue; // The queue of jobs that the scheduler needs to work on.
    private final LinkedBlockingQueue<Job> outgoingQueue; // The queue housing jobs we've already worked on and
                                                          // completed.
    private final Semaphore locker;
    private final LinkedBlockingQueue<Job> workQueue;
    private final LinkedBlockingQueue<Job> doneQueue;
    private final LinkedBlockingQueue<Job> wontMakeDeadlineBuffer;

    /**
     * @param numJobs  The number of jobs we're going to use for this run
     * @param property The property/attribute to maximize for this run
     */
    public MyScheduler(int numJobs, String property) {
        this.property = property;
        this.incomingQueue = new LinkedBlockingQueue<>(numJobs / 4);
        this.outgoingQueue = new LinkedBlockingQueue<>(1);
        this.workQueue = new LinkedBlockingQueue<>(numJobs / 4);
        this.doneQueue = new LinkedBlockingQueue<>(numJobs / 4);
        this.wontMakeDeadlineBuffer = new LinkedBlockingQueue<>(numJobs / 4);
        this.locker = new Semaphore(numJobs / 5);
    }

    /**
     * This is our main method for the Scheduler. All jobs that come in will
     * eventually have this method run in order to give them CPU time and all
     * that.
     */
    public void run() {
        // ArrayList<Job> inbetweener = new ArrayList<>();

        Thread incomingThread = new Thread(this::getJobs);

        Thread outgoingThread = new Thread(this::handleFinishedJobs);

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
                int i = 0;
                while (i < this.workQueue.size()) {
                    Job shortestJob = this.workQueue.peek();
                    long shortestWait = shortestJob.getLength();
                    for (Job incomingJob : this.workQueue) {
                        if (incomingJob.getLength() < shortestWait) {
                            shortestJob = incomingJob;
                            shortestWait = shortestJob.getLength();
                        }
                    }
                    this.workQueue.remove(shortestJob);
                    // inbetweener.add(shortestJob);
                    doneQueue.add(shortestJob);
                    i++;
                }
                break;

            case "combined":
                for (int j = 0; j < this.workQueue.size(); j++) {
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
                        for (Job incomingJob : workQueue) {
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
                // Burke hint for deadlines: Use a "buffer" for the jobs that wont make
                // their deadline
                for (int k = 0; k < this.workQueue.size(); k++) {
                    Job shortestDeadline = workQueue.peek();
                    long earliestDeadline = shortestDeadline.getDeadline();
                    for (Job candidate : workQueue) {
                        if (candidate.getDeadline() < earliestDeadline) {
                            shortestDeadline = candidate;
                            earliestDeadline = candidate.getDeadline();
                        }
                    }

                    long currentTime = System.currentTimeMillis();
                    Job fauxJob = new Job(0, property, shortestDeadline.getDeadline());
                    if ((currentTime + shortestDeadline.getLength()) <= earliestDeadline) {
                        locker.tryAcquire();
                        workQueue.remove(shortestDeadline);
                        doneQueue.add(shortestDeadline);
                        locker.release();
                    } else {
                        locker.tryAcquire();
                        workQueue.remove(shortestDeadline);
                        wontMakeDeadlineBuffer.add(fauxJob);
                        locker.release();
                    }
                }
                for (Job job : wontMakeDeadlineBuffer) {
                    wontMakeDeadlineBuffer.remove(job);
                    doneQueue.add(job);
                }
                break;
        }
    }

    /**
     * Moves elements from the incomingQueue to the workQueue to be used by the
     * scheduler
     */
    private void getJobs() {
        for (Job element : incomingQueue) {
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
