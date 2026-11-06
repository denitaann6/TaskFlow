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
  { id: 9, displayName: "Test User", username: "testuser", role: "MEMBER" },
  { id: 10, displayName: "User C", username: "userc", role: "MEMBER" },
  { id: 11, displayName: "User D", username: "userd", role: "MEMBER" },
  { id: 12, displayName: "User E", username: "usere", role: "MEMBER" },
  { id: 13, displayName: "User F", username: "userf", role: "MEMBER" },
  { id: 14, displayName: "User G", username: "userg", role: "MEMBER" },
  { id: 15, displayName: "User H", username: "userh", role: "MEMBER" },
  { id: 16, displayName: "Test User 2", username: "testuser2", role: "MEMBER" },
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
const xSpacing = 70;
const ySpacing = 80;
const xPadding = 50;
const yPadding = 50;

const treeWidth = Math.max(800, (totalNodes * xSpacing) + (xPadding * 2));
const treeHeight = Math.max(400, ((5 - 1) * ySpacing) + (yPadding * 2) + 20);

nodeCoordinatesMap.forEach((info) => {
  info.x = xPadding + (info.rank * xSpacing);
  info.y = yPadding + (info.depth * ySpacing);
});

let edgesHtml = '';
let nodesHtml = '';

function renderBranches(node) {
  if (!node) return;
  const parentCoord = nodeCoordinatesMap.get(node.id);

  if (node.left) {
    const childCoord = nodeCoordinatesMap.get(node.left.id);
    edgesHtml += `<line class="tree-edge" x1="${parentCoord.x}" y1="${parentCoord.y}" x2="${childCoord.x}" y2="${childCoord.y}" />\n`;
    renderBranches(node.left);
  }
  if (node.right) {
    const childCoord = nodeCoordinatesMap.get(node.right.id);
    edgesHtml += `<line class="tree-edge" x1="${parentCoord.x}" y1="${parentCoord.y}" x2="${childCoord.x}" y2="${childCoord.y}" />\n`;
    renderBranches(node.right);
  }

  const isRoot = (node.id === root.id);
  const displayName = node.displayName || node.username || '';
  const shortName = displayName.length > 8 ? displayName.substring(0, 7) + '…' : displayName;

  nodesHtml += `
    <g class="tree-node" data-user-id="${node.id}">
      <circle cx="${parentCoord.x}" cy="${parentCoord.y}" r="20" fill="#0369a1" stroke="#38bdf8" />
      <text x="${parentCoord.x}" y="${parentCoord.y - 3}" fill="#ffffff" font-size="10.5px" font-weight="800">#${node.id}</text>
      <text x="${parentCoord.x}" y="${parentCoord.y + 8}" fill="#94a3b8" font-size="8px" font-weight="600">${shortName}</text>
      ${isRoot ? `<rect x="${parentCoord.x - 19}" y="${parentCoord.y - 33}" width="38" height="13" rx="6.5" fill="#6366f1"/><text x="${parentCoord.x}" y="${parentCoord.y - 25}" fill="#ffffff" font-size="7.5px" font-weight="700">ROOT</text>` : ''}
    </g>
  `;
}
renderBranches(root);

console.log("Edges count:", (edgesHtml.match(/<line/g) || []).length);
console.log("Nodes count:", (nodesHtml.match(/<circle/g) || []).length);
console.log("SVG dimension:", treeWidth, "x", treeHeight);

