import fs from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)

const srcDir = path.resolve(__dirname, '../src')

// Target directories for strict design system compliance (Primitives & Showcase)
const scanTargets = [
  path.join(srcDir, 'components/ui')
]

const forbiddenPatterns = [
  {
    regex: /[\u{1F300}-\u{1F9FF}\u{2600}-\u{26FF}\u{2700}-\u{27BF}]/u,
    message: 'Forbidden emoji in UI component. Use Lucide icons instead.'
  },
  {
    regex: /from ['"]primeicons['"]|pi pi-/g,
    message: 'Forbidden PrimeIcons in UI primitives. Use Lucide icons.'
  },
  {
    regex: /bg-(?:blue|orange|red|green|emerald|indigo|slate|gray)-(?:400|500|600|700|800)(?!\/)/g,
    message: 'Forbidden hardcoded color class. Use Claude+ semantic tokens (bg-primary, bg-muted, bg-card, bg-destructive).'
  }
]

let errorCount = 0

function scanFile(filePath) {
  const content = fs.readFileSync(filePath, 'utf-8')
  const lines = content.split('\n')

  lines.forEach((line, index) => {
    if (filePath.includes('__tests__') || line.trim().startsWith('//') || line.trim().startsWith('/*')) {
      return
    }

    forbiddenPatterns.forEach(({ regex, message }) => {
      if (regex.test(line)) {
        console.error(`[Design System Lint Error] ${path.relative(srcDir, filePath)}:${index + 1}`)
        console.error(`  --> Line: ${line.trim()}`)
        console.error(`  --> Reason: ${message}\n`)
        errorCount++
      }
    })
  })
}

function traverse(dir) {
  if (!fs.existsSync(dir)) return
  const entries = fs.readdirSync(dir, { withFileTypes: true })
  for (const entry of entries) {
    const fullPath = path.join(dir, entry.name)
    if (entry.isDirectory()) {
      traverse(fullPath)
    } else if (entry.isFile() && (entry.name.endsWith('.vue') || entry.name.endsWith('.ts') || entry.name.endsWith('.js'))) {
      scanFile(fullPath)
    }
  }
}

console.log('--- Running Design System Guardrail Lint for UI Primitives ---')
scanTargets.forEach(target => traverse(target))

if (errorCount > 0) {
  console.error(`FAILED: Found ${errorCount} Design System violations.`)
  process.exit(1)
} else {
  console.log(`PASSED: 0 Design System violations found across UI primitives.`)
  process.exit(0)
}
