const http = require('http');

function escapeHtml(str) {
  if (!str) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

async function testSvgOutput() {
    const adminReq = http.request({
        hostname: 'localhost',
        port: 8085,
        path: '/api/auth/login',
        method: 'POST',
        headers: { 'Content-Type': 'application/json' }
    });
    
    const loginData = JSON.stringify({ username: 'admin', password: 'Admin@123' });
    
    const cookie = await new Promise((resolve) => {
        adminReq.on('response', (res) => {
            const c = res.headers['set-cookie'][0].split(';')[0];
            resolve(c);
        });
        adminReq.write(loginData);
        adminReq.end();
    });

    const treeData = await new Promise((resolve) => {
        const req = http.request({
            hostname: 'localhost',
            port: 8085,
            path: '/api/admin/users/tree',
            headers: { 'Cookie': cookie }
        }, (res) => {
            let data = '';
            res.on('data', chunk => data += chunk);
            res.on('end', () => resolve(JSON.parse(data)));
        });
        req.end();
    });

    let nodeCoordinatesMap = new Map();
    let currentHighlightedIds = new Set();
    let matchedNodeId = null;

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
    computeCoordinates(treeData.root, 0);

    const totalNodes = nodeCoordinatesMap.size;
    const xSpacing = 90;
    const ySpacing = 100;
    const xPadding = 80;
    const yPadding = 60;

    const treeWidth = Math.max(900, (totalNodes * xSpacing) + (xPadding * 2));
    const treeHeight = Math.max(480, (treeData.height * ySpacing) + (yPadding * 2));

    nodeCoordinatesMap.forEach((info) => {
        info.x = xPadding + (info.rank * xSpacing);
        info.y = yPadding + (info.depth * ySpacing);
    });

    let edgesHtml = '';
    let nodesHtml = '';
    let edgeCount = 0;
    let nodeCount = 0;

    function renderBranches(node) {
        if (!node) return;
        const parentCoord = nodeCoordinatesMap.get(node.id);

        if (node.left) {
            const childCoord = nodeCoordinatesMap.get(node.left.id);
            edgesHtml += `<line x1="${parentCoord.x}" y1="${parentCoord.y}" x2="${childCoord.x}" y2="${childCoord.y}" />\n`;
            edgeCount++;
            renderBranches(node.left);
        }
        if (node.right) {
            const childCoord = nodeCoordinatesMap.get(node.right.id);
            edgesHtml += `<line x1="${parentCoord.x}" y1="${parentCoord.y}" x2="${childCoord.x}" y2="${childCoord.y}" />\n`;
            edgeCount++;
            renderBranches(node.right);
        }

        const isRoot = (node.id === treeData.root.id);
        const displayName = escapeHtml(node.displayName || node.username || '');
        const shortName = displayName.length > 9 ? displayName.substring(0, 8) + '…' : displayName;

        nodesHtml += `<g data-user-id="${node.id}"><circle cx="${parentCoord.x}" cy="${parentCoord.y}" r="26"/><text>#${node.id}</text><text>${shortName}</text></g>\n`;
        nodeCount++;
    }

    renderBranches(treeData.root);

    console.log(`Rendered ${nodeCount} nodes and ${edgeCount} edges.`);
    console.log(`Dimensions: width=${treeWidth}, height=${treeHeight}`);
    console.log(`Node 1 coord:`, nodeCoordinatesMap.get(1));
    console.log(`Node 17 coord:`, nodeCoordinatesMap.get(17));
}

testSvgOutput().catch(console.error);

