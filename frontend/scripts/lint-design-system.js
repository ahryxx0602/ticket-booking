import fs from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)

const srcDir = path.resolve(__dirname, '../src')

// Target scan directories and components for strict Claude+ Design System compliance (REV-08, B3)
export const scanTargets = [
  path.join(srcDir, 'components/ui'),
  path.join(srcDir, 'showcase'),
  path.join(srcDir, 'shared/components/ThemeCustomizer.vue'),
  path.join(srcDir, 'shared/components/NotificationCenter.vue'),
  path.join(srcDir, 'shared/components/feedback'),
  path.join(srcDir, 'app/layouts'),
  path.join(srcDir, 'modules/admin/views/AdminDashboardView.vue'),
  path.join(srcDir, 'modules/admin/views/AdminUsersBookingsView.vue')
]

// Documented exceptions for legacy or non-migrated components (REV-08)
export const documentedExceptions = [
  'FirebaseUploader.vue', // Legacy PrimeVue uploader scheduled for Phase 2
  'IconRegistryDemo.vue'   // Contains icon metadata display
]

// Stateless regular expressions (no global /g flag to avoid RegExp.test() statefulness bugs)
export const forbiddenPatterns = [
  {
    id: 'forbidden-emoji',
    regex: /[\u{1F300}-\u{1F9FF}\u{2600}-\u{26FF}\u{2700}-\u{27BF}]/u,
    message: 'Forbidden emoji in UI component. Use Lucide icons instead.'
  },
  {
    id: 'forbidden-primeicons',
    regex: /from ['"]primeicons['"]|pi pi-/,
    message: 'Forbidden PrimeIcons in Design System components. Use Lucide icons.'
  },
  {
    id: 'forbidden-hardcoded-color',
    regex: /\bbg-(?:blue|orange|red|green|emerald|indigo|slate|gray)-(?:400|500|600|700|800)(?!\/)\b/,
    message: 'Forbidden hardcoded palette class. Use Claude+ semantic tokens (bg-primary, bg-muted, bg-card, bg-destructive).'
  }
]

export function scanContent(content, filePath = '') {
  const violations = []
  const lines = content.split('\n')

  lines.forEach((line, index) => {
    const trimmed = line.trim()
    if (trimmed.startsWith('//') || trimmed.startsWith('/*') || trimmed.startsWith('*')) {
      return
    }

    forbiddenPatterns.forEach(({ id, regex, message }) => {
      // Create a fresh regex check without statefulness
      if (regex.test(line)) {
        violations.push({
          id,
          line: index + 1,
          content: trimmed,
          message,
          filePath
        })
      }
    })
  })

  return violations
}

export function runLint() {
  let errorCount = 0

  function scanFile(filePath) {
    const filename = path.basename(filePath)
    if (documentedExceptions.includes(filename) || filePath.includes('__tests__')) {
      return
    }

    const content = fs.readFileSync(filePath, 'utf-8')
    const violations = scanContent(content, filePath)

    violations.forEach((v) => {
      console.error(`[Design System Lint Error] ${path.relative(srcDir, filePath)}:${v.line}`)
      console.error(`  --> Line: ${v.content}`)
      console.error(`  --> Reason: ${v.message}\n`)
      errorCount++
    })
  }

  function traverse(targetPath) {
    if (!fs.existsSync(targetPath)) return
    const stat = fs.statSync(targetPath)
    if (stat.isFile()) {
      scanFile(targetPath)
      return
    }

    const entries = fs.readdirSync(targetPath, { withFileTypes: true })
    for (const entry of entries) {
      const fullPath = path.join(targetPath, entry.name)
      if (entry.isDirectory()) {
        traverse(fullPath)
      } else if (entry.isFile() && (entry.name.endsWith('.vue') || entry.name.endsWith('.ts') || entry.name.endsWith('.js'))) {
        scanFile(fullPath)
      }
    }
  }

  console.log('--- Running Strengthened Design System Guardrail Lint (REV-08) ---')
  scanTargets.forEach((target) => traverse(target))

  if (errorCount > 0) {
    console.error(`FAILED: Found ${errorCount} Design System violations.`)
    return false
  } else {
    console.log(`PASSED: 0 Design System violations found across all target components.`)
    return true
  }
}

// Direct execution
if (process.argv[1] && fileURLToPath(import.meta.url) === process.argv[1]) {
  const success = runLint()
  process.exit(success ? 0 : 1)
}
