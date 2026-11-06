package com.taskflow.taskflow_backend.service;

import com.taskflow.taskflow_backend.entity.Task;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Service implementing a genuine Singly Linked List data structure
 * with explicit Node references (task and next pointer).
 *
 * Supports fundamental Linked List operations:
 * - Insert (Head, Tail, and arbitrary Index)
 * - Delete (by Task ID, at Head)
 * - Traverse (iterating from head to null)
 */
@Service
public class TaskLinkedListService {

    /**
     * Node structure representing an element in the singly linked list.
     * Contains data (Task) and a reference pointer to the next Node.
     */
    public static class Node {
        private Task task;
        private Node next;

        public Node(Task task) {
            this.task = task;
            this.next = null;
        }

        public Node(Task task, Node next) {
            this.task = task;
            this.next = next;
        }

        public Task getTask() {
            return task;
        }

        public void setTask(Task task) {
            this.task = task;
        }

        public Node getNext() {
            return next;
        }

        public void setNext(Node next) {
            this.next = next;
        }
    }

    // Head pointer pointing to the first node of the linked list
    private Node head = null;
    private int size = 0;

    /**
     * INSERT AT HEAD: Adds a new task at the very beginning of the linked list.
     * Time Complexity: O(1)
     */
    public synchronized void insertHead(Task task) {
        if (task == null) return;
        Node newNode = new Node(task);
        newNode.setNext(head);
        head = newNode;
        size++;
    }

    /**
     * INSERT AT TAIL: Appends a new task to the end of the linked list.
     * Time Complexity: O(N)
     */
    public synchronized void insertTail(Task task) {
        if (task == null) return;
        Node newNode = new Node(task);
        if (head == null) {
            head = newNode;
        } else {
            Node current = head;
            while (current.getNext() != null) {
                current = current.getNext();
            }
            current.setNext(newNode);
        }
        size++;
    }

    /**
     * INSERT AT INDEX: Inserts a new task at a specific 0-based position.
     * If index <= 0, inserts at head. If index >= size, inserts at tail.
     * Time Complexity: O(N)
     */
    public synchronized void insertAt(int index, Task task) {
        if (task == null) return;
        if (index <= 0 || head == null) {
            insertHead(task);
            return;
        }
        if (index >= size) {
            insertTail(task);
            return;
        }

        Node current = head;
        for (int i = 0; i < index - 1 && current.getNext() != null; i++) {
            current = current.getNext();
        }
        Node newNode = new Node(task, current.getNext());
        current.setNext(newNode);
        size++;
    }

    /**
     * DELETE BY ID: Traverses the list and removes the node matching the given Task ID.
     * Time Complexity: O(N)
     * @return the deleted Task, or null if not found
     */
    public synchronized Task deleteById(Integer taskId) {
        if (head == null || taskId == null) {
            return null;
        }

        // Case 1: Head node matches
        if (head.getTask() != null && taskId.equals(head.getTask().getId())) {
            Task deletedTask = head.getTask();
            head = head.getNext();
            size--;
            return deletedTask;
        }

        // Case 2: Search in remaining nodes
        Node current = head;
        while (current.getNext() != null) {
            Task nextTask = current.getNext().getTask();
            if (nextTask != null && taskId.equals(nextTask.getId())) {
                Task deletedTask = nextTask;
                // Unlink the node by adjusting the next pointer
                current.setNext(current.getNext().getNext());
                size--;
                return deletedTask;
            }
            current = current.getNext();
        }

        return null; // Not found
    }

    /**
     * DELETE HEAD: Removes and returns the first node from the linked list.
     * Time Complexity: O(1)
     */
    public synchronized Task deleteHead() {
        if (head == null) {
            return null;
        }
        Task deletedTask = head.getTask();
        head = head.getNext();
        size--;
        return deletedTask;
    }

    /**
     * TRAVERSE: Iterates through each node from head to null, returning an ordered list.
     * Time Complexity: O(N)
     */
    public synchronized List<Task> traverse() {
        List<Task> result = new ArrayList<>();
        Node current = head;
        while (current != null) {
            result.add(current.getTask());
            current = current.getNext();
        }
        return result;
    }

    /**
     * Returns a structured step trace of traversing the linked list.
     */
    public synchronized List<String> getTraversalTrace() {
        List<String> trace = new ArrayList<>();
        Node current = head;
        int step = 1;
        while (current != null) {
            Task t = current.getTask();
            String info = (t != null)
                    ? String.format("Step %d: Node at address %s contains Task #%d (\"%s\", Member: %s, Priority: %s, Status: %s) -> Next: %s",
                    step,
                    Integer.toHexString(System.identityHashCode(current)),
                    t.getId(), t.getName(), t.getMember(), t.getPriority(), t.getStatus(),
                    current.getNext() != null ? "Node@" + Integer.toHexString(System.identityHashCode(current.getNext())) : "NULL")
                    : String.format("Step %d: Empty Node -> Next: %s", step, current.getNext());
            trace.add(info);
            current = current.getNext();
            step++;
        }
        return trace;
    }

    /**
     * Generates a readable ASCII diagram of the linked list chain.
     */
    public synchronized String getTraversalVisual() {
        if (head == null) {
            return "[HEAD] -> NULL (Empty Linked List)";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("[HEAD] -> ");
        Node current = head;
        int count = 0;
        while (current != null) {
            Task t = current.getTask();
            if (t != null) {
                sb.append(String.format("[#%d: \"%s\"]", t.getId(), t.getName()));
            } else {
                sb.append("[Null Task]");
            }
            sb.append(" -> ");
            current = current.getNext();
            count++;
        }
        sb.append("NULL (Total Nodes: ").append(count).append(")");
        return sb.toString();
    }

    /**
     * Searches for a task by ID by traversing the linked list nodes.
     * Time Complexity: O(N)
     */
    public synchronized Task searchById(Integer taskId) {
        if (taskId == null) return null;
        Node current = head;
        while (current != null) {
            if (current.getTask() != null && taskId.equals(current.getTask().getId())) {
                return current.getTask();
            }
            current = current.getNext();
        }
        return null;
    }

    /**
     * Populates the linked list with a collection of tasks.
     * Preserves list ordering.
     */
    public synchronized void loadFromTasks(List<Task> tasks) {
        clear();
        if (tasks == null) return;
        for (Task task : tasks) {
            insertTail(task);
        }
    }

    /**
     * Clears all nodes from the linked list.
     */
    public synchronized void clear() {
        head = null;
        size = 0;
    }

    public synchronized int size() {
        return size;
    }

    public synchronized boolean isEmpty() {
        return head == null;
    }

    public synchronized Node getHead() {
        return head;
    }

    /**
     * Backward-compatible helper method that accepts a list of tasks, constructs
     * a temporary linked list using nodes and pointers, traverses it, and returns the list.
     */
    public List<Task> arrangeTasksAsLinkedList(List<Task> tasks) {
        if (tasks == null) return new ArrayList<>();
        Node tempHead = null;
        Node tempTail = null;

        for (Task task : tasks) {
            Node newNode = new Node(task);
            if (tempHead == null) {
                tempHead = newNode;
                tempTail = newNode;
            } else {
                tempTail.setNext(newNode);
                tempTail = newNode;
            }
        }

        List<Task> result = new ArrayList<>();
        Node current = tempHead;
        while (current != null) {
            result.add(current.getTask());
            current = current.getNext();
        }
        return result;
    }
}
