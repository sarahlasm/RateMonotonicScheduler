import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.Semaphore;

/**
* Assignment 3: Rate Monotonic Scheduler
* @author Sarah Lasman
*/

/**
* RMS class sets up the various threads for all three scenarios
*/
public class RMS
{
  /**
  * main calls the three test cases.
  */
  public static void main(String[] args)
  {
    int[] taskLengths = {1, 2, 4, 16};
    System.out.println("Running nominal case...");
    runTests(taskLengths);
    taskLengths[1] = 40000;
    System.out.println("\nRunning overrun in thread t2...");
    runTests(taskLengths);
    taskLengths[2] = 60000;
    taskLengths[1] = 2;
    System.out.println("\nRunning overrun in thread t3...");
    runTests(taskLengths);
  }

  /**
  * runTests sets up the four threads and scheduler thread.
  * @param taskLengths the number of repititions each task has to do, in an array
  */
  public static void runTests(int[] taskLengths)
  {
    Semaphore[] semaphores = new Semaphore[4];
    Semaphore schedulerSem = new Semaphore(1);
    AtomicBoolean run = new AtomicBoolean(true);
    for (int i = 0; i < 4; ++i)
    {
      semaphores[i] = new Semaphore(1);
    }
    MyThread[] threads = new MyThread[4];
    for (int i = 0; i < 4; ++i)
    {
      threads[i] = new MyThread("t" + (i+1), taskLengths[i], semaphores[i], run);
    }
    Thread scheduler = new Thread(new Scheduler(schedulerSem, semaphores, threads, run));
    scheduler.setPriority(10);
    for (int i = 0; i < 4; ++i)
    {
      threads[i].setPriority(9-i);
    }
    scheduler.start();
    for (Thread thread : threads)
    {
      thread.start();
    }
    for (int i = 0; i < 10 * 16 - 1; ++i)
    {
      schedulerSem.release();
    }
    run.set(false);
    schedulerSem.release();
    try
    {
      for (MyThread thread : threads)
      {
        thread.interrupt();
        thread.join();
      }
      scheduler.join();
    }
    catch (Exception e)
    {
      System.out.println(e);
    }
    printInfo(threads);
  }

  /**
  * printInfo prints info.
  */
  public static void printInfo(MyThread[] threads)
  {
    for (MyThread thread : threads)
    {
      System.out.println(thread.name + ": " + thread.countSuccess + " successes and " + thread.countSchedule + " overruns.");
    }
  }
}

/**
* MyThread class is a thread set up for this. It can do work.
*/
class MyThread extends Thread
{
  String name;
  private int numRepititions;
  private Semaphore sem;
  AtomicBoolean run;
  int countSchedule; //how many times has it been scheduled
  int countSuccess; //how many completed runs has it made

  public MyThread(String name, int numRepititions, Semaphore sem, AtomicBoolean run)
  {
    this.name = name;
    this.numRepititions = numRepititions;
    this.sem = sem;
    this.run = run;
    countSuccess = countSchedule = 0;
  }

  /**
  * run tells the thread that if it's scheduled, do work.
  */
  public void run()
  {
    while (run.get())
    {
      if (countSchedule > 0)
      {
        try
        {
          sem.acquire();
          if (!run.get()) break;
          for (int i = 0; i < numRepititions; ++i)
          {
            doWork();
          }
          countSuccess++;
          countSchedule--;
        }
        catch (Exception e) {}
      }
    }
  }

  /**
  * schedule increments countSchedule.
  */
  public void schedule()
  {
    countSchedule++;
  }

  /**
  * doWork does work and takes a long time to do it.
  */
  public void doWork()
  {
    int[][] matrix = new int[10][10];
    for (int i = 0; i < 10; ++i)
    {
      for (int j = 0; j < 10; ++j)
      {
        matrix[i][j] = 1;
      }
    }
    int col = 0;
    int sum = 0;
    while (col < 10)
    {
      for (int i = 0; i < 10; ++i)
      {
        sum += matrix[i][col];
      }
      if (col <= 5) col += 5;
      else col -= 4;
    }
  }
}

/**
* Scheduler class schedules the threads.
*/
class Scheduler implements Runnable
{
  private Semaphore schedulerSem;
  private Semaphore[] semaphores;
  private MyThread[] threads;
  private AtomicBoolean run;

  private int time;

  public Scheduler(Semaphore schedulerSem, Semaphore[] semaphores, MyThread[] threads, AtomicBoolean run)
  {
    this.schedulerSem = schedulerSem;
    this.semaphores = semaphores;
    this.threads = threads;
    this.run = run;
    time = -1;
  }

  /**
  * run
  * Every 1, 2, 4, and 16 units, schedules the appropriate threads.
  */
  public void run()
  {
    while (true)
    {
      try
      {
        schedulerSem.acquire();

        if (!run.get()) break;

        time = (time+1) % 16;

        semaphores[0].release();
        threads[0].schedule();

        if ((time+1) % 2 == 0)
        {
          semaphores[1].release();
          threads[1].schedule();
        }

        if ((time+1) % 4 == 0)
        {
          semaphores[2].release();
          threads[2].schedule();
        }

        if ((time+1) % 16 == 0)
        {
          semaphores[3].release();
          threads[3].schedule();
        }
      }
      catch (Exception e)
      {
        System.out.println(e);
      }
      for (Semaphore semaphore : semaphores)
      {
        semaphore.release();
      }
    }
  }
}
