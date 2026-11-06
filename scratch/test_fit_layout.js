function testLayout() {
  const xSpacing = 58;
  const ySpacing = 72;
  const xPadding = 42;
  const yPadding = 44;
  const nodeRadius = 17;

  const totalRanks = 17;
  const maxRank = totalRanks - 1; // 16
  const maxDepth = 4;

  const treeWidth = (maxRank * xSpacing) + (xPadding * 2);
  const treeHeight = (maxDepth * ySpacing) + (yPadding * 2);

  console.log(`treeWidth: ${treeWidth}px, treeHeight: ${treeHeight}px`);

  // Root node: rank 8, depth 0
  const rootX = xPadding + (8 * xSpacing);
  const rootY = yPadding + (0 * ySpacing);
  console.log(`Root (#9): (${rootX}, ${rootY})`);
  console.log(`Root badge Y: [${rootY - 29}, ${rootY - 17}] (clearance from top: ${rootY - 29}px)`);

  // Leftmost node: rank 0, depth 3 (Node 1)
  const leftX = xPadding + (0 * xSpacing);
  const leftY = yPadding + (3 * ySpacing);
  console.log(`Leftmost (#1): (${leftX}, ${leftY}), Left edge: ${leftX - nodeRadius}px`);

  // Rightmost node: rank 16, depth 4 (Node 17)
  const rightX = xPadding + (16 * xSpacing);
  const rightY = yPadding + (4 * ySpacing);
  console.log(`Rightmost (#17): (${rightX}, ${rightY}), Right edge: ${rightX + nodeRadius}px (clearance: ${treeWidth - (rightX + nodeRadius)}px)`);
  console.log(`Deepest node bottom edge: ${rightY + nodeRadius}px (clearance from bottom: ${treeHeight - (rightY + nodeRadius)}px)`);
}

testLayout();

