import java.util.Scanner;

public class TaskFlowDemo {

    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);

        // ==========================================
        // DATA STRUCTURES
        // ==========================================

        TaskLinkedList taskList = new TaskLinkedList();
        TaskQueue pendingQueue = new TaskQueue();
        TaskStack completedStack = new TaskStack();

        TaskBST taskTree = new TaskBST();
        TaskGraph taskGraph = new TaskGraph();

        int choice;

        do {

            // ==========================================
            // MAIN MENU
            // ==========================================

            System.out.println("\n=========================================");
            System.out.println("          TASKFLOW PROJECT");
            System.out.println("       PROJECT MANAGEMENT SYSTEM");
            System.out.println("=========================================");

            System.out.println("\n--- PHASE 1 : LINEAR DATA STRUCTURES ---");
            System.out.println("1. Add Task");
            System.out.println("2. Delete Task");
            System.out.println("3. Display All Tasks");
            System.out.println("4. Add Pending Task to Queue");
            System.out.println("5. Process Pending Task");
            System.out.println("6. Display Pending Queue");
            System.out.println("7. Complete Task");
            System.out.println("8. Display Completed Tasks");
            System.out.println("9. Check Brackets");

            System.out.println("\n--- PHASE 2 : NON-LINEAR DATA STRUCTURES ---");
            System.out.println("10. Add Task to BST");
            System.out.println("11. Search Task in BST");
            System.out.println("12. Display BST Traversals");
            System.out.println("13. Add Task Dependency");
            System.out.println("14. Display Task Dependencies");
            System.out.println("15. BFS Traversal");

            System.out.println("\n0. Exit");

            System.out.print("\nEnter your choice: ");
            choice = sc.nextInt();
            sc.nextLine();


            // ==========================================
            // PHASE 1
            // ==========================================

            switch (choice) {

                // --------------------------------------
                // 1. ADD TASK
                // --------------------------------------

                case 1:

                    System.out.println("\n--- ADD NEW TASK ---");

                    System.out.print("Enter Task ID: ");
                    int taskId = sc.nextInt();
                    sc.nextLine();

                    System.out.print("Enter Task Name: ");
                    String taskName = sc.nextLine();

                    System.out.print("Enter Assigned Team Member: ");
                    String teamMember = sc.nextLine();

                    System.out.print("Enter Priority (High/Medium/Low): ");
                    String priority = sc.nextLine();

                    System.out.print("Enter Status (Pending/Completed): ");
                    String status = sc.nextLine();

                    taskList.insertTask(
                            taskId,
                            taskName,
                            teamMember,
                            priority,
                            status
                    );

                    break;


                // --------------------------------------
                // 2. DELETE TASK
                // --------------------------------------

                case 2:

                    System.out.println("\n--- DELETE TASK ---");

                    System.out.print("Enter Task ID to delete: ");
                    int deleteId = sc.nextInt();
                    sc.nextLine();

                    taskList.deleteTask(deleteId);

                    break;


                // --------------------------------------
                // 3. DISPLAY ALL TASKS
                // --------------------------------------

                case 3:

                    System.out.println("\n--- ALL PROJECT TASKS ---");

                    taskList.displayTasks();

                    break;


                // --------------------------------------
                // 4. ADD PENDING TASK TO QUEUE
                // --------------------------------------

                case 4:

                    System.out.println("\n--- ADD TASK TO PENDING QUEUE ---");

                    System.out.print("Enter Task ID: ");
                    int queueId = sc.nextInt();
                    sc.nextLine();

                    System.out.print("Enter Task Name: ");
                    String queueName = sc.nextLine();

                    System.out.print("Enter Assigned Team Member: ");
                    String queueMember = sc.nextLine();

                    System.out.print("Enter Priority (High/Medium/Low): ");
                    String queuePriority = sc.nextLine();

                    Task queueTask = new Task(
                            queueId,
                            queueName,
                            queueMember,
                            queuePriority,
                            "Pending"
                    );

                    pendingQueue.enqueue(queueTask);

                    break;


                // --------------------------------------
                // 5. PROCESS PENDING TASK
                // --------------------------------------

                case 5:

                    System.out.println("\n--- PROCESS PENDING TASK ---");

                    Task processedTask = pendingQueue.dequeue();

                    if (processedTask != null) {
                        System.out.println(
                                "Processing Task: " +
                                processedTask.taskName
                        );
                    }

                    break;


                // --------------------------------------
                // 6. DISPLAY PENDING QUEUE
                // --------------------------------------

                case 6:

                    System.out.println("\n--- PENDING TASK QUEUE ---");

                    pendingQueue.displayQueue();

                    break;


                // --------------------------------------
                // 7. COMPLETE TASK
                // --------------------------------------

                case 7:

                    System.out.println("\n--- COMPLETE TASK ---");

                    System.out.print("Enter Task ID: ");
                    int completedId = sc.nextInt();
                    sc.nextLine();

                    System.out.print("Enter Task Name: ");
                    String completedName = sc.nextLine();

                    System.out.print("Enter Assigned Team Member: ");
                    String completedMember = sc.nextLine();

                    System.out.print("Enter Priority (High/Medium/Low): ");
                    String completedPriority = sc.nextLine();

                    Task completedTask = new Task(
                            completedId,
                            completedName,
                            completedMember,
                            completedPriority,
                            "Completed"
                    );

                    completedStack.push(completedTask);

                    break;


                // --------------------------------------
                // 8. DISPLAY COMPLETED TASKS
                // --------------------------------------

                case 8:

                    System.out.println("\n--- COMPLETED TASKS ---");

                    completedStack.displayStack();

                    break;


                // --------------------------------------
                // 9. BRACKET CHECKING
                // --------------------------------------

                case 9:

                    System.out.println("\n--- BRACKET CHECKING ---");

                    System.out.print("Enter an expression: ");
                    String expression = sc.nextLine();

                    if (BracketChecker.isBalanced(expression)) {
                        System.out.println("Result: Balanced");
                    } else {
                        System.out.println("Result: Not Balanced");
                    }

                    break;


                // ======================================
                // PHASE 2
                // ======================================


                // --------------------------------------
                // 10. ADD TASK TO BST
                // --------------------------------------

                case 10:

                    System.out.println("\n--- ADD TASK TO BST ---");

                    System.out.print("Enter Task ID: ");
                    int bstId = sc.nextInt();
                    sc.nextLine();

                    System.out.print("Enter Task Name: ");
                    String bstName = sc.nextLine();

                    System.out.print("Enter Assigned Team Member: ");
                    String bstMember = sc.nextLine();

                    System.out.print("Enter Priority (High/Medium/Low): ");
                    String bstPriority = sc.nextLine();

                    Task bstTask = new Task(
                            bstId,
                            bstName,
                            bstMember,
                            bstPriority,
                            "Pending"
                    );

                    taskTree.insert(bstTask);

                    System.out.println(
                            "Task ID " + bstId +
                            " added to BST."
                    );

                    break;


                // --------------------------------------
                // 11. SEARCH TASK IN BST
                // --------------------------------------

                case 11:

                    System.out.println("\n--- SEARCH TASK IN BST ---");

                    System.out.print("Enter Task ID to search: ");
                    int searchId = sc.nextInt();
                    sc.nextLine();

                    taskTree.search(searchId);

                    break;


                // --------------------------------------
                // 12. BST TRAVERSALS
                // --------------------------------------

                case 12:

                    System.out.println("\n--- BST TRAVERSALS ---");

                    taskTree.displayInorder();
                    taskTree.displayPreorder();
                    taskTree.displayPostorder();

                    break;


                // --------------------------------------
                // 13. ADD TASK DEPENDENCY
                // --------------------------------------

                case 13:

                    System.out.println("\n--- ADD TASK DEPENDENCY ---");

                    System.out.print("Enter first Task ID: ");
                    int firstTask = sc.nextInt();

                    System.out.print("Enter dependent Task ID: ");
                    int secondTask = sc.nextInt();
                    sc.nextLine();

                    taskGraph.addDependency(
                            firstTask,
                            secondTask
                    );

                    break;


                // --------------------------------------
                // 14. DISPLAY TASK DEPENDENCIES
                // --------------------------------------

                case 14:

                    System.out.println("\n--- TASK DEPENDENCY GRAPH ---");

                    taskGraph.displayGraph();

                    break;


                // --------------------------------------
                // 15. BFS TRAVERSAL
                // --------------------------------------

                case 15:

                    System.out.println("\n--- BFS TRAVERSAL ---");

                    System.out.print("Enter starting Task ID: ");
                    int startTask = sc.nextInt();
                    sc.nextLine();

                    taskGraph.bfsTraversal(startTask);

                    break;


                // --------------------------------------
                // 0. EXIT
                // --------------------------------------

                case 0:

                    System.out.println("\n=================================");
                    System.out.println("     TASKFLOW PROGRAM CLOSED");
                    System.out.println("=================================");

                    break;


                // --------------------------------------
                // INVALID CHOICE
                // --------------------------------------

                default:

                    System.out.println(
                            "Invalid choice. Please try again."
                    );
            }

        } while (choice != 0);

        sc.close();
    }
}