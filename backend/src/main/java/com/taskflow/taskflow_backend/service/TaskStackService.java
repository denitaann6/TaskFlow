package com.taskflow.taskflow_backend.service;

import com.taskflow.taskflow_backend.entity.CompletedTaskStackEntry;
import com.taskflow.taskflow_backend.entity.PoppedTaskHistoryEntry;
import com.taskflow.taskflow_backend.entity.Task;
import com.taskflow.taskflow_backend.repository.CompletedTaskStackRepository;
import com.taskflow.taskflow_backend.repository.PoppedTaskHistoryRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Service implementing a genuine Stack (LIFO - Last In, First Out)
 * using a custom linked-list structure with Node references,
 * persisted durably to the MySQL completed_task_stack table,
 * and logging all Pop events to popped_tasks_history.
 *
 * Characteristics:
 * - PUSH: adds completed task to the top (O(1)) and persists to completed_task_stack
 * - POP: removes top task from active stack (O(1)), removes from completed_task_stack,
 *        and records a durable entry into popped_tasks_history without altering tasks table.
 * - PEEK: returns top task without removing (O(1)) and without recording history
 * - Bracket validation: uses a custom linked-list character stack to evaluate balanced expressions.
 */
@Service
public class TaskStackService {

    // Node structure for linked-list based Stack of Tasks
    private static class Node {
        Task task;
        Node next;

        Node(Task task, Node next) {
            this.task = task;
            this.next = next;
        }
    }

    // Top pointer of the Stack
    private Node top = null;
    private int size = 0;
    private boolean initialized = false;

    @Autowired(required = false)
    private CompletedTaskStackRepository stackRepository;

    @Autowired(required = false)
    private PoppedTaskHistoryRepository poppedRepository;

    public TaskStackService() {
    }

    public TaskStackService(CompletedTaskStackRepository stackRepository, PoppedTaskHistoryRepository poppedRepository) {
        this.stackRepository = stackRepository;
        this.poppedRepository = poppedRepository;
    }

    @PostConstruct
    public synchronized void initFromDatabase() {
        if (stackRepository != null) {
            try {
                List<CompletedTaskStackEntry> entries = stackRepository.findAllByOrderByStackOrderDesc();
                clearInMemory();
                // To construct stack with highest stackOrder at the top, reverse and push
                List<CompletedTaskStackEntry> reversed = new ArrayList<>(entries);
                Collections.reverse(reversed);
                for (CompletedTaskStackEntry entry : reversed) {
                    if (entry.getTask() != null) {
                        pushInMemory(entry.getTask());
                    }
                }
                initialized = true;
            } catch (Exception e) {
                // Testing or table initialization context
            }
        }
    }

    /**
     * PUSH: Adds a task to the top of the stack (O(1)) and persists to database.
     */
    public synchronized void push(Task task) {
        if (task == null) {
            return;
        }

        pushInMemory(task);

        if (stackRepository != null && task.getId() != null) {
            try {
                stackRepository.deleteByTaskId(task.getId());
                long nextOrder = stackRepository.findMaxStackOrder() + 1;
                stackRepository.save(new CompletedTaskStackEntry(task, nextOrder, LocalDateTime.now()));
            } catch (Exception ignored) {
            }
        }
    }

    private void pushInMemory(Task task) {
        initialized = true;
        removeInMemoryById(task.getId());
        top = new Node(task, top);
        size++;
    }

    /**
     * POP: Removes and returns the task from the top of the stack (O(1)).
     */
    public synchronized Task pop() {
        return pop("Administrator");
    }

    /**
     * POP with user auditing: Removes top task and logs who performed the pop operation.
     * Enforces strict stack behavior:
     * 1. Only a task currently in the active stack can be popped.
     * 2. Exactly one active stack entry is removed.
     * 3. Exactly one history entry is created.
     * 4. Failed or empty pops create NO history entries.
     * 5. Atomically managed within a database transaction.
     */
    @Transactional
    public synchronized Task pop(String poppedBy) {
        if (stackRepository != null) {
            // Find the true top task from the active stack in MySQL
            Optional<CompletedTaskStackEntry> topEntryOpt = stackRepository.findTopByOrderByStackOrderDesc();
            if (topEntryOpt.isEmpty()) {
                // Active stack in database is empty - no pop can occur!
                clearInMemory();
                return null;
            }

            CompletedTaskStackEntry topEntry = topEntryOpt.get();
            Task topTask = topEntry.getTask();
            Long originalOrder = topEntry.getStackOrder();

            if (topTask == null || topTask.getId() == null) {
                stackRepository.delete(topEntry);
                clearInMemory();
                return null;
            }

            // Remove from active stack table
            stackRepository.delete(topEntry);

            // Record durable history event
            if (poppedRepository != null) {
                PoppedTaskHistoryEntry history = new PoppedTaskHistoryEntry(
                        topTask.getId(),
                        topTask.getName(),
                        topTask.getMember(),
                        topTask.getPriority(),
                        topTask.getStatus(),
                        LocalDateTime.now(),
                        poppedBy != null && !poppedBy.trim().isEmpty() ? poppedBy.trim() : "Administrator",
                        originalOrder
                );
                poppedRepository.save(history);
            }

            // Update in-memory stack
            removeInMemoryById(topTask.getId());

            return topTask;
        }

        // Fallback for standalone unit test execution without database repository
        if (top == null) {
            return null;
        }
        Task poppedTask = top.task;
        top = top.next;
        size--;
        return poppedTask;
    }

    /**
     * PEEK: Returns the top task without removing it (O(1)).
     * Read-only: Does not modify the stack or write to history.
     * @return the top Task, or null if empty
     */
    public synchronized Task peek() {
        if (stackRepository != null) {
            Optional<CompletedTaskStackEntry> topEntryOpt = stackRepository.findTopByOrderByStackOrderDesc();
            return topEntryOpt.map(CompletedTaskStackEntry::getTask).orElse(null);
        }
        return (top != null) ? top.task : null;
    }

    /**
     * Checks if the stack is empty.
     */
    public synchronized boolean isEmpty() {
        if (stackRepository != null) {
            return stackRepository.count() == 0;
        }
        return top == null;
    }

    /**
     * Returns current depth of the stack.
     */
    public synchronized int size() {
        if (stackRepository != null) {
            return (int) stackRepository.count();
        }
        return size;
    }

    /**
     * Clears all nodes from the in-memory stack.
     */
    public synchronized void clear() {
        clearInMemory();
    }

    private void clearInMemory() {
        top = null;
        size = 0;
    }

    /**
     * Checks if the stack has been initialized from data source.
     */
    public synchronized boolean isInitialized() {
        return initialized;
    }

    /**
     * Checks if a specific task ID is currently present on the active completed stack.
     */
    public synchronized boolean containsTaskId(Integer taskId) {
        if (taskId == null) return false;
        if (stackRepository != null) {
            return stackRepository.findByTaskId(taskId).isPresent();
        }
        Node current = top;
        while (current != null) {
            if (current.task != null && taskId.equals(current.task.getId())) {
                return true;
            }
            current = current.next;
        }
        return false;
    }

    /**
     * Removes a task from the stack by its ID (keeps stack clean upon deletion/reopening).
     */
    public synchronized void removeById(Integer id) {
        if (id == null) {
            return;
        }

        removeInMemoryById(id);

        if (stackRepository != null) {
            try {
                stackRepository.deleteByTaskId(id);
            } catch (Exception ignored) {
            }
        }
    }

    private void removeInMemoryById(Integer id) {
        if (top == null || id == null) {
            return;
        }

        if (top.task.getId() != null && top.task.getId().equals(id)) {
            top = top.next;
            size--;
            return;
        }

        Node current = top;
        while (current.next != null) {
            if (current.next.task.getId() != null && current.next.task.getId().equals(id)) {
                current.next = current.next.next;
                size--;
                return;
            }
            current = current.next;
        }
    }

    /**
     * Returns all completed tasks in LIFO order (top of the stack first).
     * Populates strictly from active completed_task_stack table.
     * Prevents resurrecting previously popped tasks into the active stack.
     */
    public synchronized List<Task> getCompletedStack(List<Task> allTasks) {
        if (stackRepository != null) {
            if (!initialized) {
                initFromDatabase();
            }

            // Sync any newly completed tasks that are neither in active stack nor in popped history
            if (allTasks != null) {
                for (Task task : allTasks) {
                    if (task != null && "Completed".equalsIgnoreCase(task.getStatus())) {
                        boolean inStack = stackRepository.findByTaskId(task.getId()).isPresent();
                        boolean inHistory = poppedRepository != null && poppedRepository.existsByTaskId(task.getId());
                        if (!inStack && !inHistory) {
                            push(task);
                        }
                    }
                }
                initFromDatabase();
            }

            List<Task> result = new ArrayList<>();
            List<CompletedTaskStackEntry> entries = stackRepository.findAllByOrderByStackOrderDesc();
            for (CompletedTaskStackEntry e : entries) {
                if (e.getTask() != null) {
                    result.add(e.getTask());
                }
            }
            return result;
        } else if (!initialized && allTasks != null) {
            for (Task task : allTasks) {
                if (task.getStatus() != null && task.getStatus().equalsIgnoreCase("Completed")) {
                    pushInMemory(task);
                }
            }
            initialized = true;
        }

        List<Task> result = new ArrayList<>();
        Node current = top;
        while (current != null) {
            result.add(current.task);
            current = current.next;
        }
        return result;
    }

    /**
     * Retrieves all durable historical popped task records.
     */
    public List<PoppedTaskHistoryEntry> getPoppedHistory() {
        if (poppedRepository != null) {
            return poppedRepository.findAllByOrderByPoppedAtDesc();
        }
        return Collections.emptyList();
    }

    /**
     * Returns a formatted ASCII visual of the stack.
     */
    public synchronized String getStackVisual() {
        List<Task> currentStack = (stackRepository != null) ? getCompletedStack(null) : toInMemoryList();
        if (currentStack.isEmpty()) {
            return "+---------------------------------+\n|        [STACK IS EMPTY]         |\n+---------------------------------+";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("+---------------------------------+\n");
        Task topItem = currentStack.get(0);
        String topStr = String.format("[TOP] #%d: \"%s\"", topItem.getId(), truncate(topItem.getName(), 21));
        sb.append(String.format("| %-31s |\n", topStr));
        for (int i = 1; i < currentStack.size(); i++) {
            Task task = currentStack.get(i);
            String item = String.format("#%d: \"%s\"", task.getId(), truncate(task.getName(), 25));
            sb.append(String.format("| %-31s |\n", item));
        }
        sb.append("+---------------------------------+\n");
        sb.append("Total Elements on Stack: ").append(currentStack.size());
        return sb.toString();
    }

    private List<Task> toInMemoryList() {
        List<Task> list = new ArrayList<>();
        Node curr = top;
        while (curr != null) {
            list.add(curr.task);
            curr = curr.next;
        }
        return list;
    }

    private String truncate(String str, int max) {
        if (str == null) return "";
        return str.length() <= max ? str : str.substring(0, max - 3) + "...";
    }

    // =========================================================================
    // CUSTOM LINKED-LIST CHARACTER STACK FOR BALANCED BRACKET EXPRESSIONS
    // =========================================================================

    private static class CharNode {
        char val;
        CharNode next;

        CharNode(char val, CharNode next) {
            this.val = val;
            this.next = next;
        }
    }

    /**
     * Evaluates whether brackets (), {}, and [] are balanced using a custom linked-list stack.
     * Time Complexity: O(N), Space Complexity: O(N)
     */
    public boolean isBalanced(String expression) {
        if (expression == null || expression.isEmpty()) {
            return true;
        }

        CharNode charTop = null;

        for (char ch : expression.toCharArray()) {
            if (ch == '(' || ch == '{' || ch == '[') {
                charTop = new CharNode(ch, charTop); // PUSH
            } else if (ch == ')' || ch == '}' || ch == ']') {
                if (charTop == null) {
                    return false; // Closing bracket with empty stack
                }
                char open = charTop.val;
                charTop = charTop.next; // POP
                if (!matches(open, ch)) {
                    return false; // Mismatched bracket types
                }
            }
        }

        return charTop == null; // Balanced only if stack is completely empty at the end
    }

    /**
     * Detailed bracket validation with step trace and error analysis.
     */
    public Map<String, Object> validateWithTrace(String expression) {
        Map<String, Object> result = new HashMap<>();
        result.put("expression", expression != null ? expression : "");

        if (expression == null || expression.isEmpty()) {
            result.put("balanced", true);
            result.put("message", "Balanced (Empty string contains no unbalanced brackets)");
            result.put("trace", List.of("Expression is empty; stack remains empty."));
            result.put("errorDetail", null);
            return result;
        }

        CharNode charTop = null;
        int depth = 0;
        List<String> trace = new ArrayList<>();
        String errorDetail = null;
        boolean balanced = true;

        char[] chars = expression.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char ch = chars[i];
            if (ch == '(' || ch == '{' || ch == '[') {
                charTop = new CharNode(ch, charTop);
                depth++;
                trace.add(String.format("Index %d ['%c']: PUSH opening bracket onto stack (Current stack depth: %d)", i, ch, depth));
            } else if (ch == ')' || ch == '}' || ch == ']') {
                if (charTop == null) {
                    errorDetail = String.format("Syntax Error at index %d: Closing bracket '%c' has no corresponding opening bracket.", i, ch);
                    trace.add(String.format("Index %d ['%c']: POP attempted on EMPTY stack! Unbalanced.", i, ch));
                    balanced = false;
                    break;
                }
                char open = charTop.val;
                charTop = charTop.next;
                depth--;
                if (!matches(open, ch)) {
                    errorDetail = String.format("Mismatch Error at index %d: Found closing '%c', but top of stack was '%c'.", i, ch, open);
                    trace.add(String.format("Index %d ['%c']: POP '%c' DOES NOT MATCH '%c'! Mismatched bracket types.", i, ch, open, ch));
                    balanced = false;
                    break;
                } else {
                    trace.add(String.format("Index %d ['%c']: POP matched '%c' with '%c' successfully (Stack depth: %d)", i, ch, open, ch, depth));
                }
            }
        }

        if (balanced && charTop != null) {
            balanced = false;
            StringBuilder remaining = new StringBuilder();
            CharNode curr = charTop;
            while (curr != null) {
                remaining.append(curr.val).append(" ");
                curr = curr.next;
            }
            errorDetail = "Unclosed brackets remaining in stack: " + remaining.toString().trim();
            trace.add("End of expression reached: Stack is NOT empty. Unclosed brackets remaining.");
        }

        result.put("balanced", balanced);
        result.put("message", balanced ? "Balanced" : "Not Balanced");
        result.put("trace", trace);
        result.put("errorDetail", errorDetail);
        return result;
    }

    private boolean matches(char open, char close) {
        return (open == '(' && close == ')') ||
               (open == '{' && close == '}') ||
               (open == '[' && close == ']');
    }
}
