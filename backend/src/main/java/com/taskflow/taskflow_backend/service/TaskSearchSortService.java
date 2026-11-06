package com.taskflow.taskflow_backend.service;

import com.taskflow.taskflow_backend.entity.Task;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service implementing Phase 3 Searching and Sorting Data Structures & Algorithms:
 * 1. Linear Search: Sequential O(N) lookup by Task ID or Task Name.
 * 2. Binary Search: Divide-and-Conquer O(log N) lookup on a properly sorted collection by Task ID or Task Name.
 * 3. Insertion Sort: In-place incremental O(N^2) sorting by Task ID or Task Name without modifying MySQL persistence.
 * 4. Quick Sort: Divide-and-Conquer O(N log N) pivot-partitioned sorting by Task ID or Task Name without modifying MySQL persistence.
 */
@Service
public class TaskSearchSortService {

    // ==========================================
    // 1. LINEAR SEARCH (Sequential O(N))
    // ==========================================

    /**
     * Performs Linear Search across task records by Task ID or Task Name.
     * Evaluates elements one-by-one from index 0 to N-1.
     *
     * @param tasks the list of tasks from the database
     * @param query the search key (numeric ID or text string)
     * @return map with search result, matched task, comparison count, and step trace
     */
    public Map<String, Object> linearSearch(List<Task> tasks, String query) {
        return linearSearch(tasks, query, "all");
    }

    /**
     * Performs Linear Search with explicit field targeting ("id", "name", or "all").
     *
     * @param tasks the list of tasks from the database
     * @param query the search key
     * @param searchBy "id", "name", or "all"
     * @return map with search result, matched task, comparison count, and step trace
     */
    public Map<String, Object> linearSearch(List<Task> tasks, String query, String searchBy) {
        Map<String, Object> result = new LinkedHashMap<>();
        List<String> trace = new ArrayList<>();
        String mode = (searchBy != null && !searchBy.isBlank()) ? searchBy.trim().toLowerCase() : "all";

        if (tasks == null || tasks.isEmpty()) {
            result.put("found", false);
            result.put("task", null);
            result.put("index", -1);
            result.put("comparisons", 0);
            result.put("trace", List.of("Task list is empty. No records to search."));
            result.put("algorithm", "Linear Search (Sequential O(N))");
            result.put("searchBy", mode);
            result.put("message", "Task list is empty.");
            return result;
        }

        if (query == null || query.trim().isEmpty()) {
            result.put("found", false);
            result.put("task", null);
            result.put("index", -1);
            result.put("comparisons", 0);
            result.put("trace", List.of("Search query is empty."));
            result.put("algorithm", "Linear Search (Sequential O(N))");
            result.put("searchBy", mode);
            result.put("message", "Please enter a Task ID or Task Name to search.");
            return result;
        }

        String trimmedQuery = query.trim();
        Integer queryId = null;
        if (!"name".equals(mode)) {
            try {
                queryId = Integer.parseInt(trimmedQuery);
            } catch (NumberFormatException ignored) {
                // query is text, not integer
            }
        }

        int comparisons = 0;
        Task foundTask = null;
        int foundIndex = -1;

        for (int i = 0; i < tasks.size(); i++) {
            comparisons++;
            Task current = tasks.get(i);
            String currentName = current.getName() != null ? current.getName() : "";
            Integer currentId = current.getId();

            boolean matches = false;
            if ("id".equals(mode)) {
                matches = queryId != null && currentId != null && currentId.equals(queryId);
            } else if ("name".equals(mode)) {
                matches = currentName.toLowerCase().contains(trimmedQuery.toLowerCase());
            } else {
                // "all" mode
                boolean matchesId = queryId != null && currentId != null && currentId.equals(queryId);
                boolean matchesName = currentName.toLowerCase().contains(trimmedQuery.toLowerCase());
                matches = matchesId || matchesName;
            }

            if (matches) {
                foundTask = current;
                foundIndex = i;
                trace.add(String.format("Step %d (Index %d): Evaluated Task [ID: %s, Name: \"%s\"] -> MATCH FOUND!",
                        comparisons, i, currentId, currentName));
                break;
            } else {
                trace.add(String.format("Step %d (Index %d): Evaluated Task [ID: %s, Name: \"%s\"] -> No match, advancing sequentially",
                        comparisons, i, currentId, currentName));
            }
        }

        result.put("found", foundTask != null);
        result.put("task", foundTask);
        result.put("index", foundIndex);
        result.put("comparisons", comparisons);
        result.put("trace", trace);
        result.put("algorithm", "Linear Search (Sequential O(N))");
        result.put("searchBy", mode);

        if (foundTask != null) {
            result.put("message", String.format("Task found at index %d after %d comparison(s) using Linear Search (O(N)).",
                    foundIndex, comparisons));
        } else {
            result.put("message", String.format("Query \"%s\" not found after scanning all %d task record(s) sequentially (O(N) worst case).",
                    trimmedQuery, comparisons));
        }

        return result;
    }

    // ==========================================
    // 2. BINARY SEARCH (Divide & Conquer O(log N))
    // ==========================================

    /**
     * Performs Binary Search on Task records by Task ID.
     * Pre-requisite: Input must be sorted. This method sorts a cloned collection by Task ID first.
     */
    public Map<String, Object> binarySearch(List<Task> tasks, Integer targetId) {
        return binarySearch(tasks, targetId != null ? String.valueOf(targetId) : null, "id");
    }

    /**
     * Performs Binary Search with selectable key: "id" or "name".
     * Strictly verifies and guarantees that tasks are sorted by the selected key before searching.
     *
     * @param tasks the list of tasks from the database
     * @param query the search key (numeric ID or task name)
     * @param searchBy "id" or "name"
     * @return map with search result, matched task, comparisons, step trace, and sorted tasks
     */
    public Map<String, Object> binarySearch(List<Task> tasks, String query, String searchBy) {
        Map<String, Object> result = new LinkedHashMap<>();
        List<String> trace = new ArrayList<>();
        String field = (searchBy != null && "name".equalsIgnoreCase(searchBy.trim())) ? "name" : "id";

        if (tasks == null || tasks.isEmpty()) {
            result.put("found", false);
            result.put("task", null);
            result.put("sortedIndex", -1);
            result.put("comparisons", 0);
            result.put("sortedKeys", List.of());
            result.put("trace", List.of("Task list is empty. No records to search."));
            result.put("algorithm", "Binary Search (Divide & Conquer O(log N))");
            result.put("searchBy", field);
            result.put("message", "Task list is empty.");
            return result;
        }

        if (query == null || query.trim().isEmpty()) {
            result.put("found", false);
            result.put("task", null);
            result.put("sortedIndex", -1);
            result.put("comparisons", 0);
            result.put("sortedKeys", List.of());
            result.put("trace", List.of("Search query is empty."));
            result.put("algorithm", "Binary Search (Divide & Conquer O(log N))");
            result.put("searchBy", field);
            result.put("message", "Please enter a valid search query.");
            return result;
        }

        String trimmedQuery = query.trim();

        if ("id".equals(field)) {
            Integer targetId;
            try {
                targetId = Integer.parseInt(trimmedQuery);
            } catch (NumberFormatException e) {
                result.put("found", false);
                result.put("task", null);
                result.put("sortedIndex", -1);
                result.put("comparisons", 0);
                result.put("sortedKeys", List.of());
                result.put("trace", List.of("Invalid Task ID: \"" + trimmedQuery + "\" is not a valid integer."));
                result.put("algorithm", "Binary Search (Divide & Conquer O(log N))");
                result.put("searchBy", field);
                result.put("message", "Task ID must be a numeric integer for Binary Search.");
                return result;
            }

            // Step 1: Pre-condition — Binary Search requires a sorted collection.
            List<Task> sortedTasks = insertionSortList(tasks, "id");
            List<Integer> sortedIds = sortedTasks.stream().map(Task::getId).toList();
            result.put("sortedKeys", sortedIds);
            result.put("sortedTaskIds", sortedIds); // backwards-compatibility

            trace.add("Pre-search requirement verified: Tasks sorted by Task ID in ascending order: " + sortedIds);

            // Step 2: Binary Search execution
            int low = 0;
            int high = sortedTasks.size() - 1;
            int comparisons = 0;
            Task foundTask = null;
            int foundIndex = -1;

            while (low <= high) {
                comparisons++;
                int mid = low + (high - low) / 2;
                Task midTask = sortedTasks.get(mid);
                Integer midId = midTask.getId();

                if (midId == null) {
                    high = mid - 1;
                    continue;
                }

                if (midId.equals(targetId)) {
                    foundTask = midTask;
                    foundIndex = mid;
                    trace.add(String.format("Comparison %d: Range [%d..%d], Mid index %d (ID %d) -> Target %d MATCHES Mid ID! Match found.",
                            comparisons, low, high, mid, midId, targetId));
                    break;
                } else if (targetId < midId) {
                    trace.add(String.format("Comparison %d: Range [%d..%d], Mid index %d (ID %d) -> Target %d < %d (Went LEFT, new range [%d..%d])",
                            comparisons, low, high, mid, midId, targetId, midId, low, mid - 1));
                    high = mid - 1;
                } else {
                    trace.add(String.format("Comparison %d: Range [%d..%d], Mid index %d (ID %d) -> Target %d > %d (Went RIGHT, new range [%d..%d])",
                            comparisons, low, high, mid, midId, targetId, midId, mid + 1, high));
                    low = mid + 1;
                }
            }

            result.put("found", foundTask != null);
            result.put("task", foundTask);
            result.put("sortedIndex", foundIndex);
            result.put("comparisons", comparisons);
            result.put("trace", trace);
            result.put("algorithm", "Binary Search (Divide & Conquer O(log N))");
            result.put("searchBy", "id");

            if (foundTask != null) {
                result.put("message", String.format("Task ID %d found at sorted index %d after %d comparison(s) using Binary Search (O(log N)).",
                        targetId, foundIndex, comparisons));
            } else {
                result.put("message", String.format("Task ID %d not found after %d comparison(s) in sorted collection of %d tasks.",
                        targetId, comparisons, sortedTasks.size()));
            }

            return result;
        } else {
            // Search by Name (alphabetic)
            List<Task> sortedTasks = insertionSortList(tasks, "name");
            List<String> sortedNames = sortedTasks.stream().map(Task::getName).toList();
            result.put("sortedKeys", sortedNames);

            trace.add("Pre-search requirement verified: Tasks sorted alphabetically by Task Name: " + sortedNames);

            int low = 0;
            int high = sortedTasks.size() - 1;
            int comparisons = 0;
            Task foundTask = null;
            int foundIndex = -1;
            String targetName = trimmedQuery.toLowerCase();

            while (low <= high) {
                comparisons++;
                int mid = low + (high - low) / 2;
                Task midTask = sortedTasks.get(mid);
                String midName = midTask.getName() != null ? midTask.getName().trim() : "";
                int cmp = targetName.compareTo(midName.toLowerCase());

                if (cmp == 0) {
                    foundTask = midTask;
                    foundIndex = mid;
                    trace.add(String.format("Comparison %d: Range [%d..%d], Mid index %d (\"%s\") -> EXACT MATCH FOUND!",
                            comparisons, low, high, mid, midName));
                    break;
                } else if (cmp < 0) {
                    trace.add(String.format("Comparison %d: Range [%d..%d], Mid index %d (\"%s\") -> Target \"%s\" < \"%s\" (Went LEFT, new range [%d..%d])",
                            comparisons, low, high, mid, midName, trimmedQuery, midName, low, mid - 1));
                    high = mid - 1;
                } else {
                    trace.add(String.format("Comparison %d: Range [%d..%d], Mid index %d (\"%s\") -> Target \"%s\" > \"%s\" (Went RIGHT, new range [%d..%d])",
                            comparisons, low, high, mid, midName, trimmedQuery, midName, mid + 1, high));
                    low = mid + 1;
                }
            }

            result.put("found", foundTask != null);
            result.put("task", foundTask);
            result.put("sortedIndex", foundIndex);
            result.put("comparisons", comparisons);
            result.put("trace", trace);
            result.put("algorithm", "Binary Search (Divide & Conquer O(log N))");
            result.put("searchBy", "name");

            if (foundTask != null) {
                result.put("message", String.format("Task \"%s\" found at sorted index %d after %d comparison(s) using Binary Search (O(log N)).",
                        foundTask.getName(), foundIndex, comparisons));
            } else {
                result.put("message", String.format("Task \"%s\" not found after %d comparison(s) in sorted collection of %d tasks.",
                        trimmedQuery, comparisons, sortedTasks.size()));
            }

            return result;
        }
    }

    // ==========================================
    // 3. INSERTION SORT (Incremental O(N^2))
    // ==========================================

    /**
     * Performs Insertion Sort on a cloned copy of the database task list.
     * Does NOT mutate the database records.
     *
     * @param tasks the list of tasks from the database
     * @param sortBy the field to sort by: "id" (numeric) or "name" (alphabetic)
     * @return map containing sorted task list, comparison count, shift count, and duration
     */
    public Map<String, Object> insertionSort(List<Task> tasks, String sortBy) {
        String field = (sortBy != null && !sortBy.isBlank()) ? sortBy.trim().toLowerCase() : "id";
        List<Task> sorted = new ArrayList<>(tasks != null ? tasks : List.of());

        int comparisons = 0;
        int shifts = 0;
        long startNanos = System.nanoTime();

        for (int i = 1; i < sorted.size(); i++) {
            Task key = sorted.get(i);
            int j = i - 1;

            while (j >= 0) {
                comparisons++;
                if (compareTasks(sorted.get(j), key, field) > 0) {
                    sorted.set(j + 1, sorted.get(j)); // Shift element right
                    shifts++;
                    j--;
                } else {
                    break;
                }
            }
            sorted.set(j + 1, key); // Place key in its sorted position
        }

        long durationNanos = System.nanoTime() - startNanos;

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("sortedTasks", sorted);
        response.put("sortBy", field);
        response.put("totalTasks", sorted.size());
        response.put("comparisons", comparisons);
        response.put("shifts", shifts);
        response.put("timeTakenNanos", durationNanos);
        response.put("algorithm", "Insertion Sort (Incremental O(N^2))");
        response.put("explanation", "Builds the sorted list element-by-element by repeatedly taking the next item and inserting it into its correct position among previously sorted items.");
        response.put("message", String.format("Successfully sorted %d tasks by %s using Insertion Sort (%d comparisons, %d shifts).",
                sorted.size(), field.toUpperCase(), comparisons, shifts));

        return response;
    }

    /**
     * Helper method that returns a sorted cloned list using Insertion Sort.
     */
    public List<Task> insertionSortList(List<Task> tasks, String sortBy) {
        Map<String, Object> sortResult = insertionSort(tasks, sortBy);
        @SuppressWarnings("unchecked")
        List<Task> sorted = (List<Task>) sortResult.get("sortedTasks");
        return sorted != null ? sorted : new ArrayList<>();
    }

    // ==========================================
    // 4. QUICK SORT (Divide & Conquer O(N log N))
    // ==========================================

    /**
     * Performs Quick Sort on a cloned copy of the database task list.
     * Does NOT mutate the database records.
     *
     * @param tasks the list of tasks from the database
     * @param sortBy the field to sort by: "id" (numeric) or "name" (alphabetic)
     * @return map containing sorted task list, comparison count, swap count, and duration
     */
    public Map<String, Object> quickSort(List<Task> tasks, String sortBy) {
        String field = (sortBy != null && !sortBy.isBlank()) ? sortBy.trim().toLowerCase() : "id";
        List<Task> sorted = new ArrayList<>(tasks != null ? tasks : List.of());

        int[] metrics = new int[2]; // metrics[0] = comparisons, metrics[1] = swaps
        long startNanos = System.nanoTime();

        if (sorted.size() > 1) {
            quickSortRecursive(sorted, 0, sorted.size() - 1, field, metrics);
        }

        long durationNanos = System.nanoTime() - startNanos;

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("sortedTasks", sorted);
        response.put("sortBy", field);
        response.put("totalTasks", sorted.size());
        response.put("comparisons", metrics[0]);
        response.put("swaps", metrics[1]);
        response.put("timeTakenNanos", durationNanos);
        response.put("algorithm", "Quick Sort (Divide & Conquer O(N log N))");
        response.put("explanation", "Selects a pivot element and partitions the array into two sub-arrays: elements smaller than the pivot and elements greater than the pivot, then recursively sorts the sub-arrays.");
        response.put("message", String.format("Successfully sorted %d tasks by %s using Quick Sort (%d comparisons, %d swaps).",
                sorted.size(), field.toUpperCase(), metrics[0], metrics[1]));

        return response;
    }

    private void quickSortRecursive(List<Task> list, int low, int high, String field, int[] metrics) {
        if (low < high) {
            int pivotIndex = partition(list, low, high, field, metrics);
            quickSortRecursive(list, low, pivotIndex - 1, field, metrics);
            quickSortRecursive(list, pivotIndex + 1, high, field, metrics);
        }
    }

    private int partition(List<Task> list, int low, int high, String field, int[] metrics) {
        Task pivot = list.get(high);
        int i = low - 1;

        for (int j = low; j < high; j++) {
            metrics[0]++; // comparison
            if (compareTasks(list.get(j), pivot, field) <= 0) {
                i++;
                if (i != j) {
                    swap(list, i, j);
                    metrics[1]++; // swap
                }
            }
        }
        if (i + 1 != high) {
            swap(list, i + 1, high);
            metrics[1]++; // swap
        }
        return i + 1;
    }

    private void swap(List<Task> list, int i, int j) {
        Task temp = list.get(i);
        list.set(i, list.get(j));
        list.set(j, temp);
    }

    /**
     * Compares two tasks based on the given sort field.
     */
    private int compareTasks(Task t1, Task t2, String sortBy) {
        if (t1 == null && t2 == null) return 0;
        if (t1 == null) return -1;
        if (t2 == null) return 1;

        if ("name".equals(sortBy)) {
            String n1 = t1.getName() != null ? t1.getName() : "";
            String n2 = t2.getName() != null ? t2.getName() : "";
            return n1.compareToIgnoreCase(n2);
        } else if ("priority".equals(sortBy)) {
            int r1 = priorityRank(t1.getPriority());
            int r2 = priorityRank(t2.getPriority());
            return Integer.compare(r1, r2);
        } else {
            // Default: sort by Task ID (numeric)
            Integer id1 = t1.getId() != null ? t1.getId() : 0;
            Integer id2 = t2.getId() != null ? t2.getId() : 0;
            return id1.compareTo(id2);
        }
    }

    private int priorityRank(String priority) {
        if ("High".equalsIgnoreCase(priority)) return 1;
        if ("Medium".equalsIgnoreCase(priority)) return 2;
        return 3; // Low or other
    }
}
