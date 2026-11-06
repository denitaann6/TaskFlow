function buildBalanced(arr, start, end) {
  if (start > end) return null;
  const mid = start + Math.floor((end - start) / 2);
  const node = { id: arr[mid] };
  node.left = buildBalanced(arr, start, mid - 1);
  node.right = buildBalanced(arr, mid + 1, end);
  return node;
}

function height(n) {
  if (!n) return 0;
  return 1 + Math.max(height(n.left), height(n.right));
}

function leaves(n) {
  if (!n) return 0;
  if (!n.left && !n.right) return 1;
  return leaves(n.left) + leaves(n.right);
}

function inorder(n, res = []) {
  if (!n) return res;
  inorder(n.left, res);
  res.push(n.id);
  inorder(n.right, res);
  return res;
}

function preorder(n, res = []) {
  if (!n) return res;
  res.push(n.id);
  preorder(n.left, res);
  preorder(n.right, res);
  return res;
}

function postorder(n, res = []) {
  if (!n) return res;
  postorder(n.left, res);
  postorder(n.right, res);
  res.push(n.id);
  return res;
}

function search(n, target) {
  let comps = 0;
  let curr = n;
  const path = [];
  while (curr) {
    comps++;
    if (target === curr.id) {
      path.push("Match: " + curr.id);
      return { found: true, comps, path };
    } else if (target < curr.id) {
      path.push("Branch LEFT from " + curr.id);
      curr = curr.left;
    } else {
      path.push("Branch RIGHT from " + curr.id);
      curr = curr.right;
    }
  }
  path.push("Null reached");
  return { found: false, comps, path };
}

const ids = [1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17];
const root = buildBalanced(ids, 0, ids.length - 1);
console.log("Root ID:", root.id);
console.log("Height:", height(root));
console.log("Leaf count:", leaves(root));
console.log("Inorder:", inorder(root).join(", "));
console.log("Preorder:", preorder(root).join(", "));
console.log("Postorder:", postorder(root).join(", "));
console.log("Search 4:", JSON.stringify(search(root, 4), null, 2));
console.log("Search 99:", JSON.stringify(search(root, 99), null, 2));

