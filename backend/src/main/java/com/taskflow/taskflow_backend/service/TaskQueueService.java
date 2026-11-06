package com.taskflow.taskflow_backend.service;

import com.taskflow.taskflow_backend.entity.Task;
import com.taskflow.taskflow_backend.entity.TaskQueueEntry;
import com.taskflow.taskflow_backend.repository.TaskQueueRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service implementing a genuine FIFO (First-In, First-Out) Queue
 * backed by a custom singly linked list with front (head) and rear (tail) pointers,
 * persisted durably to the MySQL task_queue table.
 *
 * Characteristics:
 * - ENQUEUE: appends a task to the rear (O(1)) and persists with next sequence order
 * - DEQUEUE: removes the earliest queued task from the front (O(1)) and deletes from task_queue
 * - FRONT / PEEK: inspects the next task at the front without removal (O(1))
 * - Persistent across page refresh and server restarts.
 */
@Service
public class TaskQueueService {

    /**
     * Node structure for the linked-list based FIFO Queue.
     * Contains the Task payload and a pointer to the next queued Node.
     */
    private static class Node {
        Task task;
        Node next;

        Node(Task task) {
            this.task = task;
            this.next = null;
        }
    }

    // Front pointer: where dequeue and peek operations occur (earliest task)
    private Node front = null;

    // Rear pointer: where enqueue operations occur (latest arrival)
    private Node rear = null;

    private int size = 0;
    private boolean initialized = false;

    @Autowired(required = false)
    private TaskQueueRepository queueRepository;

    public TaskQueueService() {
    }

    public TaskQueueService(TaskQueueRepository queueRepository) {
        this.queueRepository = queueRepository;
    }

    @PostConstruct
    public synchronized void initFromDatabase() {
        if (queueRepository != null) {
            try {
                List<TaskQueueEntry> entries = queueRepository.findAllByOrderBySequenceOrderAsc();
                clearInMemory();
                for (TaskQueueEntry entry : entries) {
                    if (entry.getTask() != null) {
                        enqueueInMemory(entry.getTask());
                    }
                }
                initialized = true;
            } catch (Exception e) {
                // Table might not be ready yet or in a testing context
            }
        }
    }

    /**
     * ENQUEUE: Inserts a task at the rear of the queue and persists to database.
     * Time Complexity: O(1) in-memory
     */
    public synchronized void enqueue(Task task) {
        if (task == null) return;

        enqueueInMemory(task);

        if (queueRepository != null && task.getId() != null) {
            try {
                queueRepository.deleteByTaskId(task.getId());
                long nextSeq = queueRepository.findMaxSequenceOrder() + 1;
                queueRepository.save(new TaskQueueEntry(task, nextSeq, LocalDateTime.now()));
            } catch (Exception ignored) {
            }
        }
    }

    private void enqueueInMemory(Task task) {
        removeInMemoryById(task.getId());

        Node newNode = new Node(task);
        if (rear == null) {
            front = newNode;
            rear = newNode;
        } else {
            rear.next = newNode;
            rear = newNode;
        }
        size++;
        initialized = true;
    }

    /**
     * DEQUEUE: Removes and returns the task from the front of the queue (FIFO).
     * Deletes the persistent queue entry.
     * Time Complexity: O(1) in-memory
     * @return the front Task, or null if the queue is empty
     */
    public synchronized Task dequeue() {
        if (front == null) {
            return null;
        }
        Task task = front.task;
        front = front.next;
        if (front == null) {
            rear = null;
        }
        size--;

        if (queueRepository != null && task != null && task.getId() != null) {
            try {
                queueRepository.deleteByTaskId(task.getId());
            } catch (Exception ignored) {
            }
        }
        return task;
    }

    /**
     * FRONT / PEEK: Inspects the task at the front of the queue without dequeuing it.
     * Time Complexity: O(1)
     */
    public synchronized Task peek() {
        if (!initialized && queueRepository != null) {
            initFromDatabase();
        }
        return (front != null) ? front.task : null;
    }

    /**
     * Checks if the queue is empty.
     */
    public synchronized boolean isEmpty() {
        return front == null;
    }

    /**
     * Returns the number of tasks currently in the queue.
     */
    public synchronized int size() {
        return size;
    }

    /**
     * Clears all nodes from the in-memory queue.
     */
    public synchronized void clear() {
        clearInMemory();
    }

    private void clearInMemory() {
        front = null;
        rear = null;
        size = 0;
    }

    /**
     * Synchronizes the queue from a list of tasks and database repository.
     * Only tasks with status 'Pending' are placed into the FIFO queue.
     */
    public synchronized void synchronizePendingTasks(List<Task> tasks) {
        if (queueRepository != null && tasks != null) {
            try {
                // Remove queue entries whose tasks are no longer Pending
                List<TaskQueueEntry> existing = queueRepository.findAllByOrderBySequenceOrderAsc();
                for (TaskQueueEntry entry : existing) {
                    if (entry.getTask() == null || !"Pending".equalsIgnoreCase(entry.getTask().getStatus())) {
                        queueRepository.delete(entry);
                    }
                }

                // Add any missing Pending tasks
                for (Task task : tasks) {
                    if (task != null && "Pending".equalsIgnoreCase(task.getStatus())) {
                        Optional<TaskQueueEntry> entry = queueRepository.findByTaskId(task.getId());
                        if (entry.isEmpty()) {
                            long nextSeq = queueRepository.findMaxSequenceOrder() + 1;
                            queueRepository.save(new TaskQueueEntry(task, nextSeq, LocalDateTime.now()));
                        }
                    }
                }

                initFromDatabase();
                return;
            } catch (Exception ignored) {
            }
        }

        // Fallback for tests without repository
        clearInMemory();
        if (tasks == null) return;

        tasks.stream()
                .filter(task -> task.getStatus() != null && task.getStatus().equalsIgnoreCase("Pending"))
                .sorted(java.util.Comparator.comparing(Task::getId, java.util.Comparator.nullsLast(Integer::compareTo)))
                .forEach(this::enqueueInMemory);
        initialized = true;
    }

    /**
     * Returns the pending tasks in FIFO queue order as a list for presentation.
     */
    public synchronized List<Task> getPendingQueue(List<Task> tasks) {
        if (!initialized && queueRepository != null) {
            initFromDatabase();
        } else if (tasks != null) {
            synchronizePendingTasks(tasks);
        }
        return toList();
    }

    /**
     * Processes (dequeues) the next pending task from the front of the queue.
     * Time Complexity: O(1)
     */
    public synchronized Task processNextPendingTask(List<Task> tasks) {
        if (!initialized || isEmpty()) {
            synchronizePendingTasks(tasks);
        }
        return dequeue();
    }

    /**
     * Removes a task from anywhere in the queue by its ID (e.g., when updated to In Progress/Completed, or deleted).
     * Time Complexity: O(N) in-memory
     */
    public synchronized void removeById(Integer id) {
        if (id == null) {
            return;
        }

        removeInMemoryById(id);

        if (queueRepository != null) {
            try {
                queueRepository.deleteByTaskId(id);
            } catch (Exception ignored) {
            }
        }
    }

    private void removeInMemoryById(Integer id) {
        if (front == null || id == null) {
            return;
        }

        // Case 1: Front matches
        if (front.task != null && id.equals(front.task.getId())) {
            front = front.next;
            if (front == null) {
                rear = null;
            }
            size--;
            return;
        }

        // Case 2: In the body or at rear
        Node current = front;
        while (current.next != null) {
            if (current.next.task != null && id.equals(current.next.task.getId())) {
                if (current.next == rear) {
                    rear = current;
                }
                current.next = current.next.next;
                size--;
                return;
            }
            current = current.next;
        }
    }

    /**
     * Converts the current queue into an ordered list (from front to rear).
     */
    public synchronized List<Task> toList() {
        List<Task> list = new ArrayList<>();
        Node current = front;
        while (current != null) {
            list.add(current.task);
            current = current.next;
        }
        return list;
    }

    /**
     * Returns a formatted ASCII visual of the FIFO queue.
     */
    public synchronized String getQueueVisual() {
        if (front == null) {
            return "[FRONT] (Queue is Empty) [REAR]";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("[FRONT / NEXT TO PROCESS] ➔ ");
        Node current = front;
        while (current != null) {
            sb.append(String.format("[#%d: \"%s\"]", current.task.getId(), current.task.getName()));
            if (current.next != null) {
                sb.append(" ➔ ");
            }
            current = current.next;
        }
        sb.append(" ➔ [REAR / LATEST ARRIVAL]");
        return sb.toString();
    }
}
