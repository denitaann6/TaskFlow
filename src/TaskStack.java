
 // Linked-list-based Stack (LIFO) for recently completed tasks.
public class TaskStack {

    private Task top;

    public void push(Task task) {
        if (task == null) {
            System.out.println("Cannot push a null task.");
            return;
        }

        task.stackNext = top;
        top = task;

        System.out.println("Completed & pushed: " + task.taskName);
    }

    public Task pop() {
        if (top == null) {
            System.out.println("No completed tasks yet.");
            return null;
        }

        Task removed = top;
        top = top.stackNext;
        removed.stackNext = null;

        return removed;
    }

    public boolean isEmpty() {
        return top == null;
    }

    public void displayStack() {
        if (top == null) {
            System.out.println("No completed tasks yet.");
            return;
        }

        System.out.println("\n--- Recently Completed (top to bottom) ---");

        Task current = top;
        while (current != null) {
            System.out.println(current);
            current = current.stackNext;
        }
    }
}
