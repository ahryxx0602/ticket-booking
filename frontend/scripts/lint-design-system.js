import fs from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)

const srcDir = path.resolve(__dirname, '../src')
const registryPath = path.resolve(srcDir, 'config/migration-registry.json')

// Load central migration registry
export function loadRegistry() {
  if (fs.existsSync(registryPath)) {
    const raw = fs.readFileSync(registryPath, 'utf-8')
    return JSON.parse(raw)
  }
  return { views: [], documentedExceptions: [] }
}

const registry = loadRegistry()

// Documented exceptions from registry
export const documentedExceptions = registry.documentedExceptions.map((e) => e.component)

// Migrated view paths from registry
export const migratedViews = registry.views
  .filter((v) => v.migrationStatus === 'MIGRATED')
  .map((v) => path.resolve(__dirname, '..', v.viewPath))

// Scan targets: shared UI components + layouts + all migrated production views
export const scanTargets = [
  path.join(srcDir, 'components/ui'),
  path.join(srcDir, 'showcase'),
  path.join(srcDir, 'shared/components/ThemeCustomizer.vue'),
  path.join(srcDir, 'shared/components/NotificationCenter.vue'),
  path.join(srcDir, 'shared/components/feedback'),
  path.join(srcDir, 'app/layouts'),
  ...migratedViews
]

// Stateless regular expressions
export const forbiddenPatterns = [
  {
    id: 'forbidden-emoji',
    regex: /[\u{1F300}-\u{1F9FF}\u{2600}-\u{26FF}\u{2700}-\u{27BF}]/u,
    message: 'Forbidden emoji in UI component. Use Lucide icons instead.',
    migratedOnly: false
  },
  {
    id: 'forbidden-primeicons',
    regex: /from ['"]primeicons['"]|pi pi-/,
    message: 'Forbidden PrimeIcons in Design System components. Use Lucide icons.',
    migratedOnly: false
  },
  {
    id: 'forbidden-hardcoded-color',
    regex: /\bbg-(?:blue|orange|red|green|emerald|indigo|slate|gray)-(?:400|500|600|700|800)(?!\/)\b/,
    message: 'Forbidden hardcoded palette class. Use Claude+ semantic tokens (bg-primary, bg-muted, bg-card, bg-destructive).',
    migratedOnly: false
  },
  {
    id: 'forbidden-legacy-components',
    regex: /import\s+.*\b(AppButton|FormField)\b.*from\s+['"]@\/shared\/components\/(AppButton|FormField)(\.vue)?['"]/,
    message: 'Forbidden legacy component import in migrated view. Use Shadcn Button / Input / Label instead.',
    migratedOnly: true
  },
  {
    id: 'forbidden-primevue-imports',
    regex: /from ['"]primevue(\/.*)?['"]/,
    message: 'Forbidden PrimeVue import in migrated view. Use Shadcn Vue / Reka UI primitives.',
    migratedOnly: true
  },
  {
    id: 'forbidden-native-select',
    regex: /<select[\s>]/,
    message: 'Forbidden native <select> in migrated view. Use approved Shadcn Select primitive.',
    migratedOnly: true
  }
]

export function scanContent(content, filePath = '') {
  const violations = []
  const lines = content.split('\n')
  const resolvedPath = filePath ? path.resolve(filePath) : ''
  const viewEntry = registry.views.find(
    (v) => resolvedPath === path.resolve(__dirname, '..', v.viewPath) || resolvedPath.endsWith(path.basename(v.viewPath))
  )
  const isMigratedView = viewEntry ? viewEntry.migrationStatus === 'MIGRATED' : migratedViews.some((v) => resolvedPath === v || resolvedPath.endsWith(path.basename(v)))
  const viewExceptions = viewEntry?.exceptions || []

  lines.forEach((line, index) => {
    const trimmed = line.trim()
    if (trimmed.startsWith('//') || trimmed.startsWith('/*') || trimmed.startsWith('*')) {
      return
    }

    forbiddenPatterns.forEach(({ id, regex, message, migratedOnly }) => {
      // Rule applies strictly to migrated production views when migratedOnly is true
      if (migratedOnly && (!filePath || !isMigratedView)) {
        return
      }

      // Check documented exceptions for this specific view
      if (id === 'forbidden-legacy-components' && line.includes('FormField') && viewExceptions.includes('legacy-formfield')) {
        return
      }
      if (id === 'forbidden-native-select' && viewExceptions.includes('legacy-native-select')) {
        return
      }

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

  console.log('--- Running Strengthened Central Migration Registry Linter (D0-PRE-05) ---')
  console.log(`Auditing ${migratedViews.length} migrated views and design system components...`)
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
