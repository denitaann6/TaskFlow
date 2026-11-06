
public class Task {
    int taskId;
    String taskName;
    String teamMember;
    String priority;
    String status;

    // Separate links for each data structure
    Task next;       // Linked List
    Task queueNext;  // Queue
    Task stackNext;  // Stack

    public Task(int taskId, String taskName, String teamMember,
                String priority, String status) {
        this.taskId = taskId;
        this.taskName = taskName;
        this.teamMember = teamMember;
        this.priority = priority;
        this.status = status;

        this.next = null;
        this.queueNext = null;
        this.stackNext = null;
    }

    @Override
    public String toString() {
        return "ID: " + taskId
                + " | Task: " + taskName
                + " | Assigned To: " + teamMember
                + " | Priority: " + priority
                + " | Status: " + status;
    }
}
