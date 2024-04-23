package com.bee;

import java.util.Comparator;
// import java.util.ArrayList;
// import java.util.Iterator;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
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
  private final int numJobs;
  private final LinkedBlockingQueue<Job> workQueue;
  private final LinkedBlockingQueue<Job> doneQueue;
  private final LinkedBlockingQueue<Job> bufferOfShame;

  /**
   * @param numJobs  The number of jobs we're going to use for this run
   * @param property The property/attribute to maximize for this run
   */
  public MyScheduler(int numJobs, String property) {
    this.property = property;
    this.numJobs = numJobs;
    this.incomingQueue = new LinkedBlockingQueue<>(numJobs / 4);
    this.outgoingQueue = new LinkedBlockingQueue<>(1);
    this.workQueue = new LinkedBlockingQueue<>(numJobs / 4);
    this.doneQueue = new LinkedBlockingQueue<>(numJobs / 4);
    this.bufferOfShame = new LinkedBlockingQueue<>(numJobs / 4);
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
        for (int i = 0; i < numJobs; i++) {
          try {
            doneQueue.put(this.workQueue.take());
          } catch (Exception e) {
            System.out.println("It broke!");
          }
        }
        break;

      case "avg wait":
        PriorityBlockingQueue<Job> avgWaitQueue = new PriorityBlockingQueue<Job>(8, new Comparator<Job>() {
          public int compare(Job jobA, Job jobB) {
            if (jobA.getLength() > jobB.getLength()) {
              return 1;
            } else if (jobA.getLength() < jobB.getLength()) {
              return -1;
            } else {
              return 0;
            }
          }
        });
        for (int i = 0; i < numJobs; i++) {
          try {
            Job heck = workQueue.take();
            avgWaitQueue.put(heck);
            // Job shortestJob = this.workQueue.take();
            // long shortestWait = shortestJob.getLength();
            // Job incomingJob = shortestJob;
            // for (int j = 0; j < numJobs; j++) {
            // if (incomingJob.getLength() < shortestWait) {
            // shortestJob = incomingJob;
            // shortestWait = shortestJob.getLength();
            // } else {
            // this.workQueue.put(shortestJob);
            // }
            // }
            // this.workQueue.take();
            // doneQueue.put(shortestJob);
          } catch (Exception e) {
            // TODO: handle exception
          }
        }
        for (int i = 0; i < numJobs; i++) {
          try {
            Job heck = avgWaitQueue.take();
            doneQueue.put(heck);
          } catch (Exception e) {
            // TODO: handle exception
          }
        }
        break;

      case "combined":
        PriorityBlockingQueue<Job> combinedQueue = new PriorityBlockingQueue<Job>(8, new Comparator<Job>() {
          public int compare(Job jobA, Job jobB) {
            if (jobA.getLength() > jobB.getLength()) {
              return 1;
            } else if (jobA.getLength() < jobB.getLength()) {
              return -1;
            } else {
              return 0;
            }
          }
        });
        for (int i = 0; i < numJobs; i++) {
          if (workQueue.size() == 1) {
            // Use FCFS if there aren't multiple jobs in the queue to be processed
            try {
              this.locker.acquire();
              doneQueue.put(this.workQueue.take());
              this.locker.release();
            } catch (Exception e) {
              System.err.println("Failed to take from work Queue!!!");
            }
          } else {
            // Use SJF if there are multiple jobs in the queue waiting to be
            // processed
            for (int j = 0; j < numJobs; j++) {
              try {
                Job heck = workQueue.take();
                combinedQueue.put(heck);
              } catch (Exception e) {
                // TODO: handle exception
              }
            }

            for (int k = 0; k < numJobs; k++) {
              try {
                Job heck = combinedQueue.take();
                doneQueue.put(heck);
              } catch (Exception e) {
                //TODO: handle exception
              }
            }

          }
        }
        break;

      case "deadlines":
        // Burke hint for deadlines: Use a "buffer" for the jobs that wont make
        // their deadline
        PriorityBlockingQueue<Job> deadlinesQueue = new PriorityBlockingQueue<Job>(8, new Comparator<Job>() {
          public int compare(Job jobA, Job jobB) {
            if (jobA.getDeadline() > jobB.getDeadline()) {
              return 1;
            } else if (jobA.getDeadline() < jobB.getDeadline()) {
              return -1;
            } else {
              return 0;
            }
          }
        });
        long previousJobRuntime = 0;
        for (int i = 0; i < numJobs; i++) { // Preventing a potential NullPointerException if we
                                            // try to grab things from workQueue before
                                            // incomingThread has had a chance to set up the queue

          long currentTime = System.currentTimeMillis();

          if ((currentTime + workQueue.peek().getLength() + previousJobRuntime) > workQueue.peek().getDeadline()) {
            try {
              Job bitch = workQueue.take();
              bufferOfShame.put(bitch);

            } catch (Exception e) {
              // TODO: handle exception
            }
          } else {
            try {
              Job notBitch = workQueue.take();
              deadlinesQueue.put(notBitch);
              previousJobRuntime = notBitch.getLength();

            } catch (Exception e) {
              // TODO: handle exception
            }
          }
          // if ((currentTime + deadlinesQueue.peek().getLength() +
          // runningJob.getLength()) <= deadlinesQueue.peek().getDeadline()) {
          // doneQueue.add(deadlinesQueue.remove());
          // } else{
          // bufferOfShame.add(deadlinesQueue.remove());
          // }
        }
        for (Job heck : deadlinesQueue) {
          try {
            Job runningJob = deadlinesQueue.take();
            doneQueue.put(runningJob);
          } catch (Exception e) {
            // TODO: handle exception
          }
        }
        // Do the jobs in the buffer of shame. We have to do every job, but these
        // jobs would've been late so they get punted to the back to think about
        // what theyve done.
        for (Job job : bufferOfShame) {
          try {
            bufferOfShame.take();
            doneQueue.put(job);

          } catch (Exception e) {
            // TODO: handle exception
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
      // TODO: handle exception
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
      // TODO: handle exception
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
