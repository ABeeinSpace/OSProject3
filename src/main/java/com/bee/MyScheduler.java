package com.bee;

import java.util.Comparator;
// import java.util.ArrayList;
// import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;

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
  // private final Semaphore locker;
  private final int numJobs;
  private final PriorityBlockingQueue<Job> workQueue;
  private final LinkedBlockingQueue<Job> doneQueue;

  /**
   * @param numJobs  The number of jobs we're going to use for this run
   * @param property The property/attribute to maximize for this run
   */
  public MyScheduler(int numJobs, String property) {
    this.property = property;
    this.numJobs = numJobs;
    this.incomingQueue = new LinkedBlockingQueue<>(numJobs / 5);
    this.outgoingQueue = new LinkedBlockingQueue<>(1);
    this.workQueue = createWorkQueue();
    this.doneQueue = new LinkedBlockingQueue<>(numJobs / 4);
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
        for (int i = 0; i < numJobs; i++) {
          try {
            // locker.acquire();
            doneQueue.put(this.workQueue.take());
            // locker.release();
          } catch (Exception e) {
            System.out.println("It broke!");
          }
        }
        break;

      case "avg wait":
        for (int i = 0; i < numJobs; i++) {
          try {
            outgoingQueue.put(workQueue.take());
          } catch (Exception e) {
            System.out.println("It broke!");
          }
        }
        break;

      case "combined":
      // Use more than just the length of the jobs to determine combined
        for (int i = 0; i < numJobs; i++) {
          if (workQueue.size() == 1) {
            // Use FCFS if there aren't multiple jobs in the queue to be processed
            try {
              doneQueue.put(this.workQueue.take());
            } catch (Exception e) {
              System.err.println("Failed to take from work Queue!!!");
            }
          } else {
            // Use SJF if there are multiple jobs in the queue waiting to be
            // processed
            // for (int j = 0; j < numJobs; j++) {
              try {
                Job heck = workQueue.take();
                outgoingQueue.put(heck);
              } catch (Exception e) {
                System.out.println("It broke!");
              }
            // }
          }
        }
        break;

      case "deadlines":
        // Burke hint for deadlines: Use a "buffer" for the jobs that wont make
        // their deadline
        long previousJobRuntime = 0;
        long currentTime = 0;
        LinkedBlockingQueue<Job> bufferOfShame = new LinkedBlockingQueue<>(numJobs / 4);
        for (int i = 0; i < numJobs; i++) {
          try {
            Job currentJob = workQueue.take();
            if ((currentTime + currentJob.getLength() + previousJobRuntime) > currentJob.getDeadline()) {
              // locker.acquire();
              bufferOfShame.put(currentJob);
              // locker.release();
              currentTime++;
              previousJobRuntime = 1;
            } else {
              // locker.acquire();
              outgoingQueue.put(currentJob);
              // locker.release();
              currentTime += currentJob.getLength();
              previousJobRuntime = currentJob.getLength();
            }
          } catch (Exception e) {
              System.out.println("It broke!");
          }
        }
        // Do the jobs in the buffer of shame. We have to do every job, but these
        // jobs would've been late so they get punted to the back to think about
        // what they've done.
        for (Job job : bufferOfShame) {
          try {
            Job heck = bufferOfShame.take();
            outgoingQueue.put(heck);
          } catch (Exception e) {
            System.out.println("It broke!");
          }
        }
        break;
    }
  }

  /**
   * Moves elements from the incomingQueue to the workQueue to be used by the
   * scheduler
   */
  private void getJobs() {
    try {
      for (int i = 0; i < numJobs; i++) {
        Job element = incomingQueue.take();
        workQueue.put(element);
      }
    } catch (Exception e) {
      System.out.println("It broke!");
    }
  }

  /**
   * Move elements from doneQueue to the outgoingQueue
   */
  private void handleFinishedJobs() {
    try {
      for (int i = 0; i < numJobs; i++) {
        Job element = doneQueue.take();
        outgoingQueue.put(element);
      }
    } catch (Exception e) {
      System.out.println("It broke!");
    }
  }

  /**
   * @return
   */
  private PriorityBlockingQueue<Job> createWorkQueue() {
    PriorityBlockingQueue<Job> workQueue;
    if (Objects.equals(property, "deadlines")) {
      workQueue = new PriorityBlockingQueue<>(numJobs, new Comparator<Job>() {
        public int compare(Job jobA, Job jobB) {
           return Long.compare(jobA.getDeadline(), jobB.getDeadline());
        }
      });
    } else if (Objects.equals(property, "combined")) {
      workQueue = new PriorityBlockingQueue<>(numJobs, new Comparator<Job>() {
        public int compare(Job jobA, Job jobB) {
          if (jobA.getTimeCreated() < jobB.getTimeCreated()) { // Job A arrived before Job B
            if (jobA.getLength() < jobB.getLength()) { // Job A is shorter than Job B
              return -1;
            } else if (jobA.getLength() > jobB.getLength()) { // Job A is longer than Job B
              return 1;
            } else { // The jobs are the same length
              return 0;
            }
          } else if (jobA.getTimeCreated() > jobB.getTimeCreated()) { // Job A arrived after Job B
            if (jobA.getLength() < jobB.getLength()) { // Job A is shorter than Job B
              return -1;
            } else if (jobA.getLength() > jobB.getLength()) { // Job A is longer than Job B
              return 1;
            } else { // The jobs are the same length
              return 0;
            }
          } else { // Job A and Job B were created at the same time.
            if (jobA.getLength() < jobB.getLength()) { // Job A is shorter than Job B
              return -1;
            } else if (jobA.getLength() > jobB.getLength()) { // Job A is longer than Job B
              return 1;
            } else { // The jobs are the same length
              return 0;
            }
          }
            // return (Long.compare(jobA.getLength(), jobB.getLength()) + Long.compare(jobB.getWaitTime(), jobA.getWaitTime()));
        }
      });
    }
    else {
      workQueue = new PriorityBlockingQueue<>(numJobs, new Comparator<Job>() {
        public int compare(Job jobA, Job jobB) {
            return Long.compare(jobA.getLength(), jobB.getLength());
        }
      });
    }
    return workQueue;
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
