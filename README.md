# Java Multithreading and Concurrency Performance Optimization

Each instance of an application that we run runs independently from other processes. Normally they're way more processes than course. Each 
process may have one or more threads and all these threads are competing with each other to be executed on the CPU. Even if we have multiple 
cores, there are still way more threads than cores. So the operating system will have to run one thread, then stop it, run another thread, stop 
it. And so on the act of stopping one thread, scheduling it out, scheduling in another thread and starting it is called a context switch, which 
is not a 'cheap' operation as it needs to store the resources of the current thread from caches, memory, etc... and load the new threads one.
Thrashing is a situation where the operating system is spending more time context switching between threads than executing the actual threads.
Thread consumes less resources than processes so context switching between threads is less expensive.

## Thread fundamentals: Thread Creation

A way to create a thread `new Thread(Runnable target)` you can pass a Runnable or a functional interface. You can retrieve the current thread by 
using the `Thread.currentThread()` from the thread class, and get its name by `getName()` method and set it with `setName(name)`. You can set the 
priority of a thread by using `setPriority(int priority)` method, where priority is an integer between 1 and 10. The default priority is 5. To 
start a thread call the `start()` method on the thread instance.

A thread uncaught exception would bring down the thread and print the stack trace to the console. You can set a default uncaught exception handler 
for the thread by using:

```java
thread.setUncaughtExceptionHandler((t, e) -> {
    System.out.println("Uncaught exception in thread " + t.getName() + ": " + e.getMessage()); //we usually clean resources here
}); // The argument is a java.lang.Thread.UncaughtExceptionHandler which contains a method uncaughtException(Thread t, Throwable e)
```

Another way to create a Thread is by extending the Thread class and overriding the `run()` method, which also allows us the access methods 
directly on the thread instance (instead of using `Thread.currentThread()`).

## Thread Coordination

### Thread stoppage

Threads consume resources even when the thread is not doing anything, it's still consuming memory and some kernel resources. So if we created a 
thread that already finished its work, but the application is still running, we would like to clean up those resources consumed by that unused thread.
But we might want to stop a thread also if it is missbehaving or taking too long to complete a task. There are several ways to stop a thread
`interrupt()` is a method on the thread instance sets the interrupt flag to true. We can use it when the method the thread is running throws an 
`InterruptException`. If the thread doesn't handle the interrupt properly, it will continue running and might complete even after the main thread 
has finished.
Daemon threads are threads that run in the background and do not prevent our application from exiting if the main thread terminates (for example 
a thread thad saves into a file automatically and we want to still exit the application even if the thread is still running). We can set a thread 
to be a daemon thread by calling `setDaemon(true)` method on the thread instance before starting it, the thread won't be stopped with an interrupt 
signal. When all non-daemon threads finish execution, the JVM will exit, and any remaining daemon threads will be terminated abruptly.

### Thread coordination

If a thread calculation depends on some other thread to finish first, we can use the `join()` method on the thread instance that we want to wait 
for. This method blocks the calling thread until the thread on which `join()` was called has finished executing. We can pass a maximum wait time 
to the join method to indicate the maximum amount of time the calling thread should wait for the target thread to finish.

## Performance and optimization

Let's define two performance definitions, the latency and the throughput. The latency is measured in time units and is defined as the time to 
completion of a single task. Throughput is the amount of tasks completed in a given period of time and is measured in tasks per unit of time. To 
reduce latency, we should strive to divide the taks into subtasks and execute them in parallel, in theory the parallelisation should match the 
number of cores. Adding just a single additional thread will be counterproductive, and in fact will reduce the performance and increase the latency.
That additional thread we'll keep pushing the other threads from their core back and forth for resulting in context, switches, bad cash, performance, 
and extra memory consumption. In reality the optimal number of threads is usually higher than the number of cores, because some threads might be 
waiting for I/O operations to complete. 
To increase throughput, we can increase it by adding more threads, and the performance of the task will be T/N being T the time to complete the 
task and N the number of threads. But again, adding too many threads will result in context switching and thrashing, so we need to find a balance. 
There are techniques such as thread pooling and cash friendly nonblocking cues, that helps us to minimize the rest of the contributors to the 
cost and we can achieve almost optimal throughput. The thread pooling thread poolling is nothing more than just creating the threads once and 
reusing them for future tasks instead of recreating the threads each and every time from scratch:

```java
public static void startServer(String text) throws IOException {
    // The second argument is the backlog of connections, which is set to 0 as the thread pool will handle incoming requests 
    HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);  
    server.createContext("/search", new SomeHandler(text)); // SomeHandler implements the HttpHandler interface which recives an HttpExchange instance 
    Executor executor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
    server.setExecutor(executor); // Assigns the executor to handle incoming requests
    server.start();
}
```

## Data Sharing between threads

### Stack 

Stack is the memory region where methods are executed, each tread has its own. Whenever we pass arguments to a method they are passed on the stack.
All the local variables are also stored on the stack. You can think of the stack and the method pointer as the overall state of each thread execution. 
LIFO (Last In First Out) is the principle behind the stack, so the last method called is the first one to be completed and removed from the stack. 
All the variables on the stack belongs to a particular thread and other threads haven't got access to them. The stack is statically allocated when 
the thread is created so if you call too many methods it might overflow.

### Heap

The heap is a shared memory region which belongs to the process, all the threads share data allocated on the heap. All objects are stored on the heap,
static variables, member of classes, etc... The heap is governed and managed by the garbage collector, which automatically frees up memory that is 
no longer referenced by any thread. In this way, member of classes exists as long as their parents exists too. Static variables on the contrary 
stays there as long as the process exists. 

## The Concurrency Challenges and Solutions

The `synchronized` keyword in java is a locking mechanism design to prevent access to a block of code by multiple threads at the same time. It can 
be used in a method in a class or block of code (in case we don't want to synchronize the whole method). In this second case we need to provide an 
object to synchronize on that would serve as a lock. In reality synchronize a method is like synchronizing on `this` object. In the case of the 
block synchronization we can do it on different objects providing more flexibility. 
The synchronized method or block is reentrant, meaning that if a thread already holds the lock and tries to acquire it again, it will succeed 
without blocking itself. This is useful when a synchronized method calls another synchronized method on the same object or when a thread enters a 
synchronized method or block, it acquires the lock associated with the object being synchronized on. Other threads that try to enter the same 
synchronized method or block will be blocked until the lock is released.

### Atomic operations

Which operations are atomic in java?

    * All reference assignments and reads are atomic in java. 
    * All primitive types except long and double are atomic (the later are not because they are 64 bits).
    * Long or double variables declared as `volatile` are atomic.
    * All operations on `java.util.concurrent.atomic` package are atomic.

### Race condition and data races

A race condition can happen when multiple threads access a shared resource and at least one is modifying it. The time of the thread scheduling may 
cause incorrect results. The core of the problem is non atomic operations performed on the shared resource. 
A data race happens when two or more threads access the same memory location concurrently, and at least one of the accesses is a write operation, 
and the threads are not using any synchronization mechanisms to coordinate their access to that memory location. Data Race could happen because 
compiler and CPU optimizations that could reorder instructions, making the operations non atomic. To avoid this we can either declare the method 
or enclose the code with the `synchronized` keyword or declare the shared variable as `volatile` which will reduce the overhead of locking and 
will guarantee order. Specifically declaring a shared variable as volatile guarantees that code that comes before access to a volatile variable 
will be executed before that access, instruction and code that comes after access to a volatile variable will be executed after the access 
instruction, this is equivalent to a memory fence or a memory barrier.
In general, every shared variable that is modified by at least one thread has to be either guarded by a `synchronized` block or declared `volatile`.

### Locking strategies and deadlocks

A deadlock is a situation where every thread is trying to make progress, but cannot because they're waiting for another party to release a lock 
they depend upon. A way to avoid deadlocks is to follow these techniques:

    * Mutual exclusion: Only one thread can access a resource at a time
    * Hold and wait: Threads should not hold onto resources while waiting for others
    * No preemption: Resources cannot be forcibly taken away from threads
    * Circular wait: Impose an ordering on resource acquisition to prevent circular dependencies (this is the easiest to implement)

## Advanced Locking

### ReentrantLock

The reentrant lock works like the `synchronized` keyword applied on an object, but unlike the `synchonized` keyword, it requires explicit locking and
unlocking:

```java
Lock lockObject = new ReentrantLock();
// Some code
lockObject.lock();
// Use resource
lockObject.unlock();
```

We need to be careful to always unlock the lock in a finally block to avoid deadlocks in case of an exception. So why we botter to use it? It 
provides a few additional features over the synchronized keyword:

    * Ability to try to acquire the lock without blocking indefinitely (tryLock())
    * Ability to interrupt a thread waiting to acquire a lock (lockInterruptibly())
    * Ability to implement fair locking policies (fair locks)

It also has some useful methods like `getQueueThreads()` which returns a collection of threads waiting to acquire the lock, `getOwner()` which 
returns the current thread that owns the lock, `isHeldByCurrentThread()` which is self explanatory, and `isLocked()` which queries if any thread 
holds the lock at the moment. The reentrant lock also controls over the lock fairness, unlike the `synchronized` keyword. A fair lock grants 
access to the longest-waiting thread, while a non-fair lock can grant access to threads in a more arbitrary order. To activate the fairness policy 
pass `true` to the constructor.
The reentrant lock provides with an interruptible method `lockInterruptibly()` which allows a thread to attempt to acquire the lock while still 
being responsive to interrupts. Generally, if in a particular thread we try to acquire a lock object while another thread is already holding this 
lock, the current thread would get suspended and not wake up until the lock is released. In this case, calling the interrupt method to wake up 
the suspended thread would not do anything. But if instead of calling the lock method, we call the `lockInterruptibly()` while another thread is 
already holding the lock, then we can still get out of this suspension.
But the most powerful operation is the `tryLock()` method, which attempts to acquire the lock without blocking. If the lock is available, it 
acquires it and returns true. This feature is very useful to avoid deadlocks and in scenarios such as video processing, low latency trading 
systems, or user interface applications. 

### ReentrantReadWriteLock

The ReentrantReadWriteLock is a lock that allows multiple threads to read a shared resource concurrently while ensuring exclusive access for writing.
This is useful in scenarios where read operations are more frequent than write operations, and improves performance by allowing concurrent reads. 
It provides two types of locks: a read lock and a write lock.

```java
ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
// We use the write lock to block critical sections that modify the shared resource
rwLock.writelock().lock(); // Only a single thread is allowed to acquire the lock if there is no read locks held
try {
    // Modify shared resource
} finally {
    rwLock.writelock().unlock();
}
// To read the resource we do:
rwLock.readlock().lock(); // Multiple threads can acquire the read lock simultaneously but not if there is a write lock held
try {
    // Read shared resource
} finally{
    rwLock.readlock().unlock();
}
```

## Inter-thread communication

### Semaphore introduction

A semaphore is like a permit issuing and enforcing authority. It can be used to restrict the number of users to a particular resource or a group 
of resources. Unlike locks that allow only one user per resource, the semaphore can restrict any given number of users to a resource. You define 
the number of permits on semaphore creation: `new Semaphore(5)`. A `BinarySemaphore` is a semaphore with only one permit, working as a lock. Usage:

```java
semaphore.acquire();
resource(); // some action over a resource
semaphore.release();
```

A semaphore is different than a lock:

    * A sempahore doesn't have a notion of owner thread (any thread can release a permit, not necessarily the one that acquired it)
    * Many threads can acquire a permit
    * The same thread can acquire multiple permits
    * The binary semaphore is not reentrant (which could lead to deadlocks if the same thread tries to acquire it multiple times).

A semaphore is particularly useful in producer/consumer scenarios where you can have one of the side waiting for the other side to produce or 
consume items. If for example the consumer is faster than the producer, it will have to wait for a semaphore permit to be released by the producer 
before attempting to consume the item, spending most of the time idle.

A condition variable is a synchronization primitive that enables threads to wait until a particular condition is met. Condition variables are 
typically used in conjunction with locks to allow threads to wait for specific conditions while holding a lock. In Java, condition variables are 
implemented using the `Condition` interface, which is part of the `java.util.concurrent.locks` package. To create a condition variable, you first 
need to create a lock and then obtain the condition:

```java
Lock lock = new ReentrantLock();
Condition condition = lock.condition();

lock.lock();
try {
    while(username == null || password == null) {
        condition.await(); // This releases the lock and puts the thread to sleep until signaled
    }    
} finally {
    lock.unlock();
}
```

Then another thread can signal the condition:

```java

lock.lock();
try {
        username = userTextBox.getText();
        password = passTextBox.getText();
        condition.signal(); // This wakes up the thread waiting on the condition, if there are multiple threads, only one is awaken up
        // use condition.signalAll() to wake up all waiting threads
} finally {
    lock.unlock(); // even if the condition has been signaled the thread above must wait until the lock is released
}
```

### wait(), notify() and notifyAll()

The `wait()`, `notify()`, and `notifyAll()` methods are part of the Object class in Java and are used for inter-thread communication.

    * wait(): This method is called on an object to make the current thread wait until another thread calls notify() or notifyAll() on the same object.
      When a thread calls wait(), it releases the lock it holds on the object and enters a waiting state. The thread will remain in this state until 
      it is notified or interrupted.
    * notify(): This method is called on an object to wake up a single thread that is waiting on that object's monitor (i.e., has called wait() on 
      that object). If multiple threads are waiting, only one is awakened.
    * notifyAll(): This method is called on an object to wake up all threads that are waiting on that object's monitor. All waiting threads will
      be awakened, but only one will be able to acquire the lock and proceed.

In order to use these methods, the calling thread must hold the lock on the object. This is typically done using a synchronized block or method.

```java
synchronized(this) {
    if(notCompleted){
        this.wait(); // this releases the lock on this object and uaits until notified
    }    
    // other tasks
}
// Then on another thread we can call
synchronized(this) {
    // some code
    this.notify(); // wakes up a single thread waiting on this object
    // or this.notifyAll(); to wake up all waiting threads
}
```

## Lock-Free algorithms, Data-structures & techniques

A way to guarantee thread safety is to use atomic variables from the `java.util.concurrent.atomic` package. These variables provide atomic operations
that are thread-safe without the need for explicit synchronization. Examples include `AtomicInteger`, `AtomicLong`, `AtomicBoolean` and others. 

```java
AtomicInteger atomicInteger= new AtomicInteger(3);
atomicInteger.incrementAndGet(); // similar to a ++integer operation but thread safe (there is a decrement version too)
atomicInteger.getAndIncrement(); // similar to a integer++ operation but thread safe (there is a decrement version too)
atomicInteger.addAndGet(3);
atomicInteger.getAndAdd(-3);
```

`AtomicReference<V>` is another useful class that allows us to atomically update references to objects. It acts like a wrapper for the object 
reference and provides atomic operations to get and set the reference. It also provides a `compareAndSet(expectedValue, newValue)` method that 
atomically sets the value to newValue if the current value is equal to expectedValue.

## Threading models for High Performance IO

From a computer design perspective, the memory and CPU are a logical unit where the CPU has direct access to the memory without OS involvement, and 
the rest of the components such as disk, network etc... are peripherals that the CPU can access through a controller operation which requires OS 
involvement. In some cases the peripherals has direct access to the memory through DMA (Direct Memory Access) controllers, which offloads the CPU 
from copying data from the peripheral to the memory. When a thread performs an I/O operation, it typically involves waiting for the peripheral to 
complete the operation, which can take a significant amount of time compared to CPU operations. During this waiting period the CPU could be idle 
resulting in poor resource utilization (IO bound operation).  When a task involve blocking calls, having the same number of threads than cores 
doesn't necessarily give us the best performance, because some threads will be idle waiting for IO operations to complete and also doesn't give us 
the best CPU utilization. This is because even if we have a few blocking calls, the performance of the application will be impacted by this 
blocking calls.

There are multiple techniques in order to improve the performance in an IO bound application

### Thread per task model

In this model, each incoming request is handled by a separate thread. This approach is simple to implement and understand, but it can lead to
resource exhaustion like memory or reaching the maximum number of threads allowed by the OS, and poor performance under high load as the number of 
threads increases. This can be implemented with a dynamic thread pool as a new thread is created for each incoming request and the thread is 
terminated once the request is completed which can be achieved using the `Executors.newCachedThreadPool()` method. We can play safer by caping the 
number of threads with a fixed thread pool set to a high number, but this would lower the throughput under high load as there would be a 
significant number of context switches. This can lead to a situation in which the CPU is spending more time context switching between threads than 
executing the actual threads, leading to thrashing.

### Asynchronous, non blocking IO model with thread per core model

In this model, a small number of threads (typically equal to the number of CPU cores) handle all incoming requests using non-blocking I/O operations.
When a thread initiates an I/O operation, it doesn't block and wait for the operation to complete. Instead, it registers a callback or uses a
future/promise mechanism to be notified when the operation is done. This allows the thread to continue processing other requests while waiting for 
I/O operations to complete. This model can significantly improve resource utilization and throughput, especially under high load, as threads are 
not idle while waiting for I/O. However, it can be more complex to implement and requires careful management of callbacks and state.
This model leads to hard API design to which the JDK provides a thin layer of abstraction. Usually you work with third party libraries such as 
netty, vert.x, webflux, etc... which provide a more user friendly API over the low level NIO API provided by the JDK.

## Virtual Threads and High Performance IO

Virtual threads were introduced in the jdk 21, and they are managed by the JVM as opposed to normal threads which are managed by the OS. They also 
don't come with a fixed stack size. The virtual thread is allocated on the heap as any other object and can be garbage collected when not used 
anymore. Unlike platform threads, virtual threads are cheap and fast to creat in large quantities. The JVM is responsible for scheduling virtual 
threads onto a smaller number of platform threads, which allows for efficient use of system resources. When the JVM wants to run a virtual thread, 
it 'mount' it into a platform thread (named carrier), and when the virtual thread performs a blocking operation, the JVM 'demounts' it from the 
platform thread, allowing the platform thread to run other virtual threads. To create a platform thread we can use:

```java 
Thread.startVirtualThread(() -> {
    // Task to be executed in the virtual thread
});
// Or if we don't want the thread to start immediately
Thread.ofVirtual().unstarted(() -> {
    // Task to be executed in the virtual thread
});
```

In the same way, you can create a platform thread with `Thread.ofPlatform().unstarted(runnable);`. Virtual threads have some considerations 
regarding performance compared with platform threads:

    * If virtual threads represent only CPU operations then there is no performance gain as it is an abstraction for scheduling tasks on a pool of 
      threads
    * If the virtual thread performs blocking IO operations, then there is a significant performance gain as the carrier thread can be used to 
      run other virtual threads while the original virtual thread is blocked.

Many of the blocking operations were refactored to support virtual threads. Some of this operations include `sleep()`, `ReentrantLock.lock()`, 
`Semaphore.acquire()`... Executors also comes with new methods to create pools of virtual threads. For example instead of using the `Executors.
newCachedThreadPool()` we can use `Executors.newVirtualThreadPerTaskExecutor()` which creates a thread pool that creates a new virtual thread per 
task.

Virtual threads are deamon threads, meaning the threads won't prevent the application from terminating. Also setting the priority of a virtual 
thread makes no difference. 