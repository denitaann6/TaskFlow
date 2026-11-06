
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TaskFlowServer {

    private static final int PORT = 8085;

    private static final List<TaskData> tasks = new ArrayList<>();

    public static void main(String[] args) throws IOException {

        HttpServer server = HttpServer.create(
            new InetSocketAddress(PORT), 0
        );

        server.createContext("/api/health", exchange -> {
            addCorsHeaders(exchange);

            if ("OPTIONS".equalsIgnoreCase(
                    exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                exchange.close();
                return;
            }

            send(exchange, 200,
                "{\"status\":\"TaskFlow backend is running\"}");
        });

        server.createContext("/api/tasks",
            TaskFlowServer::handleTasks);

        server.setExecutor(null);
        server.start();

        System.out.println("TaskFlow backend started!");
        System.out.println(
            "Server: http://localhost:" + PORT
        );
        System.out.println("Keep this terminal running.");
    }

    private static void handleTasks(HttpExchange exchange)
            throws IOException {

        addCorsHeaders(exchange);

        String method = exchange.getRequestMethod();

        if ("OPTIONS".equalsIgnoreCase(method)) {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return;
        }

        try {

            // GET: Retrieve, search, and sort tasks
            if ("GET".equalsIgnoreCase(method)) {

                List<TaskData> result =
                    new ArrayList<>(tasks);

                String rawQuery =
                    exchange.getRequestURI().getRawQuery();

                Map<String, String> params =
                    parse(rawQuery == null ? "" : rawQuery);

                String search = params.get("search");
                String algorithm = params.get("algorithm");
                String sort = params.get("sort");

                // Phase 3: Linear Search or Binary Search
                // Searches by task ID (numeric) or task name (string)
                if (search != null && !search.isBlank()) {
                    String query = search.trim();
                    boolean isNumeric = false;
                    int target = -1;
                    try {
                        target = Integer.parseInt(query);
                        isNumeric = true;
                    } catch (NumberFormatException ignored) {
                    }

                    if (isNumeric) {
                        int[] ids = new int[result.size()];

                        for (int i = 0; i < result.size(); i++) {
                            ids[i] = result.get(i).id;
                        }

                        int foundId = -1;

                        if ("binary".equalsIgnoreCase(algorithm)) {
                            // Binary search requires sorted IDs.
                            int[] sortedIds = ids.clone();
                            TaskSearchSort.mergeSort(sortedIds);
                            int index = TaskSearchSort.binarySearch(sortedIds, target);
                            if (index >= 0) {
                                foundId = sortedIds[index];
                            }
                        } else {
                            // Default: Linear Search
                            int index = TaskSearchSort.linearSearch(ids, target);
                            if (index >= 0) {
                                foundId = ids[index];
                            }
                        }

                        if (foundId >= 0) {
                            final int idToKeep = foundId;
                            result.removeIf(task -> task.id != idToKeep);
                        } else {
                            // Also search if task name contains the query
                            final String q = query.toLowerCase();
                            result.removeIf(task -> !task.name.toLowerCase().contains(q));
                        }
                    } else {
                        // Task name-based search (case-insensitive)
                        final String q = query.toLowerCase();
                        result.removeIf(task -> !task.name.toLowerCase().contains(q));
                    }
                }

                // Phase 3: Sorting algorithms
                if ("id".equalsIgnoreCase(sort)) {

                    // Insertion Sort by task ID
                    int[] ids = getTaskIds(result);

                    TaskSearchSort.insertionSort(ids);

                    result = reorderByIds(result, ids);

                } else if ("merge".equalsIgnoreCase(sort)) {

                    // Merge Sort by task ID
                    int[] ids = getTaskIds(result);

                    TaskSearchSort.mergeSort(ids);

                    result = reorderByIds(result, ids);

                } else if ("name".equalsIgnoreCase(sort)) {

                    result.sort(
                        Comparator.comparing(
                            task -> task.name.toLowerCase()
                        )
                    );

                } else if ("priority".equalsIgnoreCase(sort)) {

                    result.sort(
                        Comparator.comparingInt(
                            task -> priorityRank(task.priority)
                        )
                    );
                }

                send(exchange, 200, toJson(result));
                return;
            }

            // POST: Add a new task
            if ("POST".equalsIgnoreCase(method)) {

                String body = readBody(exchange);
                Map<String, String> data = parse(body);

                String idText = data.get("id");
                String name = data.get("name");
                String member = data.get("member");
                String priority = data.get("priority");
                String status = data.get("status");

                if (idText == null || name == null
                        || member == null || priority == null) {

                    send(exchange, 400,
                        "{\"error\":\"Please provide task ID, name, member, and priority.\"}");
                    return;
                }

                int id;

                try {
                    id = Integer.parseInt(idText.trim());
                } catch (NumberFormatException e) {
                    send(exchange, 400,
                        "{\"error\":\"Task ID must be a number.\"}");
                    return;
                }

                if (name.isBlank() || member.isBlank()) {
                    send(exchange, 400,
                        "{\"error\":\"Task name and team member cannot be empty.\"}");
                    return;
                }

                for (TaskData task : tasks) {
                    if (task.id == id) {
                        send(exchange, 409,
                            "{\"error\":\"A task with this ID already exists.\"}");
                        return;
                    }
                }

                if (!priority.equalsIgnoreCase("High")
                        && !priority.equalsIgnoreCase("Medium")
                        && !priority.equalsIgnoreCase("Low")) {

                    send(exchange, 400,
                        "{\"error\":\"Priority must be High, Medium, or Low.\"}");
                    return;
                }

                if (status == null || status.isBlank()) {
                    status = "Pending";
                }

                TaskData newTask = new TaskData(
                    id,
                    name.trim(),
                    member.trim(),
                    normalizePriority(priority),
                    status.trim()
                );

                tasks.add(newTask);

                send(exchange, 201, toJson(newTask));
                return;
            }

            // DELETE: Delete a task using its ID
            if ("DELETE".equalsIgnoreCase(method)) {

                String rawQuery =
                    exchange.getRequestURI().getRawQuery();

                Map<String, String> params =
                    parse(rawQuery == null ? "" : rawQuery);

                String idText = params.get("id");

                if (idText == null) {
                    send(exchange, 400,
                        "{\"error\":\"Please provide a task ID.\"}");
                    return;
                }

                int id;

                try {
                    id = Integer.parseInt(idText.trim());
                } catch (NumberFormatException e) {
                    send(exchange, 400,
                        "{\"error\":\"Task ID must be a number.\"}");
                    return;
                }

                boolean removed = tasks.removeIf(
                    task -> task.id == id
                );

                if (removed) {
                    send(exchange, 200,
                        "{\"message\":\"Task deleted successfully.\"}");
                } else {
                    send(exchange, 404,
                        "{\"error\":\"Task not found.\"}");
                }

                return;
            }

            send(exchange, 405,
                "{\"error\":\"Method not allowed.\"}");

        } catch (Exception e) {

            e.printStackTrace();

            if (exchange.getResponseCode() == -1) {
                send(exchange, 500,
                    "{\"error\":\"An internal server error occurred.\"}");
            }
        }
    }

    // Extract task IDs into an integer array
    private static int[] getTaskIds(List<TaskData> list) {

        int[] ids = new int[list.size()];

        for (int i = 0; i < list.size(); i++) {
            ids[i] = list.get(i).id;
        }

        return ids;
    }

    // Rebuild task list in the order of the sorted IDs
    private static List<TaskData> reorderByIds(
            List<TaskData> original, int[] sortedIds) {

        List<TaskData> ordered = new ArrayList<>();

        for (int id : sortedIds) {
            for (TaskData task : original) {
                if (task.id == id) {
                    ordered.add(task);
                    break;
                }
            }
        }

        return ordered;
    }

    // Priority order: High, Medium, Low
    private static int priorityRank(String priority) {

        if ("High".equalsIgnoreCase(priority)) {
            return 1;
        }

        if ("Medium".equalsIgnoreCase(priority)) {
            return 2;
        }

        return 3;
    }

    private static String normalizePriority(String priority) {

        if ("high".equalsIgnoreCase(priority)) {
            return "High";
        }

        if ("medium".equalsIgnoreCase(priority)) {
            return "Medium";
        }

        return "Low";
    }

    // Convert tasks to JSON
    private static String toJson(List<TaskData> list) {

        StringBuilder json = new StringBuilder("[");
        boolean first = true;

        for (TaskData task : list) {

            if (!first) {
                json.append(",");
            }

            json.append(task.toJson());
            first = false;
        }

        json.append("]");
        return json.toString();
    }

    private static String toJson(TaskData task) {
        return task.toJson();
    }

    // Escape special characters in JSON strings
    private static String escape(String value) {

        if (value == null) {
            return "";
        }

        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }

    // Read POST request body
    private static String readBody(HttpExchange exchange)
            throws IOException {

        try (InputStream input = exchange.getRequestBody()) {
            return new String(
                input.readAllBytes(),
                StandardCharsets.UTF_8
            );
        }
    }

    // Parse URL-encoded parameters
    private static Map<String, String> parse(String text) {

        Map<String, String> params = new HashMap<>();

        if (text == null || text.isBlank()) {
            return params;
        }

        for (String pair : text.split("&")) {

            String[] parts = pair.split("=", 2);

            String key = URLDecoder.decode(
                parts[0], StandardCharsets.UTF_8
            );

            String value = parts.length > 1
                ? URLDecoder.decode(
                    parts[1], StandardCharsets.UTF_8
                )
                : "";

            params.put(key, value);
        }

        return params;
    }

    // Add CORS headers so the frontend can call the API
    private static void addCorsHeaders(HttpExchange exchange) {

        exchange.getResponseHeaders().set(
            "Access-Control-Allow-Origin", "*"
        );

        exchange.getResponseHeaders().set(
            "Access-Control-Allow-Methods",
            "GET, POST, DELETE, OPTIONS"
        );

        exchange.getResponseHeaders().set(
            "Access-Control-Allow-Headers", "Content-Type"
        );
    }

    // Send an HTTP response
    private static void send(
            HttpExchange exchange, int status, String response)
            throws IOException {

        byte[] bytes =
            response.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set(
            "Content-Type", "application/json; charset=UTF-8"
        );

        exchange.sendResponseHeaders(status, bytes.length);

        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    // Task data model used by the API
    private static class TaskData {

        int id;
        String name;
        String member;
        String priority;
        String status;

        TaskData(
                int id,
                String name,
                String member,
                String priority,
                String status) {

            this.id = id;
            this.name = name;
            this.member = member;
            this.priority = priority;
            this.status = status;
        }

        String toJson() {

            return "{"
                + "\"id\":" + id + ","
                + "\"name\":\"" + escape(name) + "\","
                + "\"member\":\"" + escape(member) + "\","
                + "\"priority\":\"" + escape(priority) + "\","
                + "\"status\":\"" + escape(status) + "\""
                + "}";
        }
    }
}
