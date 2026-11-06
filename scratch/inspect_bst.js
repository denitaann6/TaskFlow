const http = require('http');

function request(options, postData) {
  return new Promise((resolve, reject) => {
    const req = http.request(options, (res) => {
      let data = '';
      res.on('data', chunk => data += chunk);
      res.on('end', () => {
        try {
          resolve({ status: res.statusCode, headers: res.headers, body: JSON.parse(data) });
        } catch (e) {
          resolve({ status: res.statusCode, headers: res.headers, body: data });
        }
      });
    });
    req.on('error', reject);
    if (postData) req.write(typeof postData === 'string' ? postData : JSON.stringify(postData));
    req.end();
  });
}

async function main() {
  const loginRes = await request({
    hostname: 'localhost',
    port: 8085,
    path: '/api/auth/login',
    method: 'POST',
    headers: { 'Content-Type': 'application/json' }
  }, { username: 'admin', password: 'Admin@123' });

  const cookie = loginRes.headers['set-cookie'].map(c => c.split(';')[0]).join('; ');

  const treeRes = await request({
    hostname: 'localhost',
    port: 8085,
    path: '/api/admin/users/tree',
    method: 'GET',
    headers: { Cookie: cookie }
  });

  const root = treeRes.body.root;
  console.log('Tree summary:', {
    totalUsers: treeRes.body.totalUsers,
    treeHeight: treeRes.body.height,
    leafNodes: treeRes.body.leafCount,
    rootUserId: root ? root.id : null,
    rootUserName: root ? root.displayName : null
  });

  const records = [];

  function traverse(node, parentId) {
    if (!node) return;
    const record = {
      user_id: node.id,
      username: node.username,
      display_name: node.displayName,
      parent_user_id: parentId,
      left_child_id: node.left ? node.left.id : null,
      right_child_id: node.right ? node.right.id : null
    };
    records.push(record);
    traverse(node.left, node.id);
    traverse(node.right, node.id);
  }

  traverse(root, null);

  records.sort((a, b) => a.user_id - b.user_id);
  console.log('\n--- EXACT USER TREE RECORDS ---');
  console.table(records);
}

main().catch(console.error);
