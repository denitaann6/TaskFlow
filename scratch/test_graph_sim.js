const tasks = [
  { id: 101, name: "Database Design" },
  { id: 102, name: "trial" },
  { id: 103, name: "Bug Checking" },
  { id: 104, name: "backend" },
  { id: 105, name: "Report Writing" },
  { id: 106, name: "Documentation" },
  { id: 108, name: "Testing Task" },
  { id: 110, name: "backend testing" }
];

const adjList = {
  101: [102, 103],
  102: [104],
  103: [105],
  104: [],
  105: [],
  106: [],
  108: [],
  110: []
};

// BFS
function bfs(startId, adj) {
  const order = [];
  const visited = new Set();
  const queue = [startId];
  visited.add(startId);

  while (queue.length > 0) {
    const curr = queue.shift();
    order.push(curr);

    const neighbors = adj[curr] || [];
    for (const n of neighbors) {
      if (!visited.has(n)) {
        visited.add(n);
        queue.push(n);
      }
    }
  }
  return order;
}

console.log("BFS from 101:", bfs(101, adjList));
console.log("BFS from 102:", bfs(102, adjList));
console.log("BFS from 106:", bfs(106, adjList));

