const fs = require('fs');

function buildBalanced(arr, start, end) {
  if (start > end) return null;
  const mid = start + Math.floor((end - start) / 2);
  const node = {
    id: arr[mid].id,
    displayName: arr[mid].displayName,
    username: arr[mid].username,
    role: arr[mid].role
  };
  node.left = buildBalanced(arr, start, mid - 1);
  node.right = buildBalanced(arr, mid + 1, end);
  return node;
}

const mockUsers = [
  { id: 1, displayName: "System Administrator", username: "admin", role: "ADMIN" },
  { id: 2, displayName: "John", username: "john", role: "MEMBER" },
  { id: 3, displayName: "Arjun", username: "arjun", role: "MEMBER" },
  { id: 4, displayName: "Sheena", username: "sheena", role: "MEMBER" },
  { id: 5, displayName: "Subu", username: "subu", role: "MEMBER" },
  { id: 6, displayName: "Mary", username: "mary", role: "MEMBER" },
  { id: 7, displayName: "Amrith", username: "amrith", role: "MEMBER" },
  { id: 8, displayName: "User A", username: "usera", role: "MEMBER" },
  { id: 9, displayName: "User B", username: "userb", role: "MEMBER" },
  { id: 10, displayName: "User C", username: "userc", role: "MEMBER" },
  { id: 11, displayName: "User D", username: "userd", role: "MEMBER" },
  { id: 12, displayName: "User E", username: "usere", role: "MEMBER" },
  { id: 13, displayName: "User F", username: "userf", role: "MEMBER" },
  { id: 14, displayName: "User G", username: "userg", role: "MEMBER" },
  { id: 15, displayName: "User H", username: "userh", role: "MEMBER" },
  { id: 16, displayName: "Test User", username: "testuser", role: "MEMBER" },
  { id: 17, displayName: "Sneha", username: "sneha", role: "MEMBER" }
];

const root = buildBalanced(mockUsers, 0, mockUsers.length - 1);
const nodeCoordinatesMap = new Map();

let inorderRank = 0;
function computeCoordinates(node, depth = 0) {
  if (!node) return;
  computeCoordinates(node.left, depth + 1);
  nodeCoordinatesMap.set(node.id, {
    node: node,
    rank: inorderRank++,
    depth: depth
  });
  computeCoordinates(node.right, depth + 1);
}
computeCoordinates(root, 0);

const totalNodes = nodeCoordinatesMap.size;
const xSpacing = 95;
const ySpacing = 100;
const xPadding = 80;
const yPadding = 65;

const treeWidth = Math.max(1000, (totalNodes * xSpacing) + (xPadding * 2));
const treeHeight = Math.max(520, (5 * ySpacing) + (yPadding * 2));

nodeCoordinatesMap.forEach((info) => {
  info.x = xPadding + (info.rank * xSpacing);
  info.y = yPadding + (info.depth * ySpacing);
  console.log(`Node #${info.node.id.toString().padStart(2)}: depth=${info.depth}, rank=${info.rank.toString().padStart(2)}, (x=${info.x.toString().padStart(4)}, y=${info.y.toString().padStart(3)})`);
});

console.log(`Total width: ${treeWidth}px, Total height: ${treeHeight}px`);

