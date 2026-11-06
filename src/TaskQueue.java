
 // Linked-list-based Queue (FIFO) for pending tasks.
public class TaskQueue {

    private Task front;
    private Task rear;

    public void enqueue(Task task) {
        if (task == null) {
            System.out.println("Cannot enqueue a null task.");
            return;
        }

        task.queueNext = null;

        if (rear == null) {
            front = task;
            rear = task;
        } else {
            rear.queueNext = task;
            rear = task;
        }

        System.out.println("Enqueued: " + task.taskName);
    }

    public Task dequeue() {
        if (front == null) {
            System.out.println("Queue is empty.");
            return null;
        }

        Task removed = front;
        front = front.queueNext;

        if (front == null) {
            rear = null;
        }

        removed.queueNext = null;
        return removed;
    }

    public boolean isEmpty() {
        return front == null;
    }

    public void displayQueue() {
        if (front == null) {
            System.out.println("Pending queue is empty.");
            return;
        }

        System.out.println("\n--- Pending Queue (front to rear) ---");

        Task current = front;
        while (current != null) {
            System.out.println(current);
            current = current.queueNext;
        }
    }
}
