const { execSync } = require('child_process');
const fs = require('fs');
const path = require('path');

function findMmdFiles(dir, fileList = []) {
  const files = fs.readdirSync(dir);
  for (const file of files) {
    const filePath = path.join(dir, file);
    if (fs.statSync(filePath).isDirectory()) {
      findMmdFiles(filePath, fileList);
    } else if (filePath.endsWith('.mmd')) {
      fileList.push(filePath);
    }
  }
  return fileList;
}

const mmdFiles = findMmdFiles(path.join(__dirname, 'docs'));
console.log(`Found ${mmdFiles.length} files.`);

for (const mmd of mmdFiles) {
  const png = mmd.replace('.mmd', '.png');
  console.log(`Generating ${png}...`);
  execSync(`npx -y @mermaid-js/mermaid-cli -i "${mmd}" -o "${png}" -s 4 -b white -c "docs/mermaid-config.json"`, { stdio: 'inherit' });
}
