
 // Singly linked list that stores and manages all TaskFlow tasks.
public class TaskLinkedList {

    private Task head;

    // INSERT: Adds a task to the end of the list
    public void insertTask(int taskId, String taskName,
                           String teamMember, String priority, String status) {

        Task newTask = new Task(taskId, taskName, teamMember, priority, status);

        if (head == null) {
            head = newTask;
        } else {
            Task current = head;

            while (current.next != null) {
                current = current.next;
            }

            current.next = newTask;
        }

        System.out.println("Success: Task '" + taskName + "' added to Master List.");
    }

    // DELETE: Removes a task by its ID
    public void deleteTask(int taskId) {
        if (head == null) {
            System.out.println("Error: The task list is empty.");
            return;
        }

        if (head.taskId == taskId) {
            head = head.next;
            System.out.println("Task ID " + taskId + " has been deleted.");
            return;
        }

        Task current = head;

        while (current.next != null && current.next.taskId != taskId) {
            current = current.next;
        }

        if (current.next == null) {
            System.out.println("Error: Task ID " + taskId + " not found.");
        } else {
            current.next = current.next.next;
            System.out.println("Task ID " + taskId + " has been deleted.");
        }
    }

    // TRAVERSE: Displays all tasks
    public void displayTasks() {
        if (head == null) {
            System.out.println("\n--- The Task List is currently empty ---");
            return;
        }

        System.out.println("\n--- MASTER TASK LIST ---");

        Task current = head;

        while (current != null) {
            System.out.println(current);
            current = current.next;
        }

        System.out.println("------------------------");
    }

    // SEARCH: Finds a task by its ID
    public Task findTask(int taskId) {
        Task current = head;

        while (current != null) {
            if (current.taskId == taskId) {
                return current;
            }

            current = current.next;
        }

        return null;
    }
}
