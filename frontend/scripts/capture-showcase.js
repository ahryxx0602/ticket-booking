import { chromium } from 'playwright'
import { spawn } from 'child_process'
import path from 'path'
import fs from 'fs'

const SCREENSHOT_DIR = path.resolve('../docs/frontend-v2/screenshots-foundation')
if (!fs.existsSync(SCREENSHOT_DIR)) {
  fs.mkdirSync(SCREENSHOT_DIR, { recursive: true })
}

const VIEWPORTS = [
  { name: '320px', width: 320, height: 600 },
  { name: '390px', width: 390, height: 844 },
  { name: '768px', width: 768, height: 1024 },
  { name: '1024px', width: 1024, height: 768 },
  { name: '1440px', width: 1440, height: 900 },
  { name: '1920px', width: 1920, height: 1080 }
]

async function run() {
  console.log('Starting Vite server on port 5174...')
  const devServer = spawn('npm', ['run', 'dev', '--', '--port', '5174'], {
    shell: true,
    cwd: path.resolve('.'),
    stdio: 'pipe'
  })

  // Wait for server ready
  await new Promise((resolve, reject) => {
    devServer.stdout.on('data', (data) => {
      const msg = data.toString()
      if (msg.includes('http://localhost:5174') || msg.includes('Local:')) {
        console.log('Vite dev server is ready.')
        resolve(true)
      }
    })
    devServer.stderr.on('data', (data) => {
      // console.error(data.toString())
    })
    setTimeout(() => resolve(true), 5000)
  })

  console.log('Launching browser...')
  const browser = await chromium.launch({ headless: true })
  const context = await browser.newContext()
  const page = await context.newPage()

  try {
    const url = 'http://localhost:5174/showcase'
    console.log(`Navigating to ${url}...`)
    await page.goto(url, { waitUntil: 'networkidle', timeout: 30000 })
    await page.waitForTimeout(1000)

    for (const vp of VIEWPORTS) {
      console.log(`Capturing ${vp.name}...`)
      await page.setViewportSize({ width: vp.width, height: vp.height })
      await page.waitForTimeout(500)

      // Light mode
      await page.evaluate(() => {
        document.documentElement.classList.remove('dark')
      })
      await page.waitForTimeout(300)
      await page.screenshot({
        path: path.join(SCREENSHOT_DIR, `showcase-light-${vp.name}.png`),
        fullPage: true
      })

      // Dark mode
      await page.evaluate(() => {
        document.documentElement.classList.add('dark')
      })
      await page.waitForTimeout(300)
      await page.screenshot({
        path: path.join(SCREENSHOT_DIR, `showcase-dark-${vp.name}.png`),
        fullPage: true
      })
    }

    // Reset to light and desktop for specific tabs
    await page.setViewportSize({ width: 1440, height: 900 })
    await page.evaluate(() => document.documentElement.classList.remove('dark'))

    // Tab: Controls
    console.log('Capturing Controls tab...')
    await page.click('button:has-text("Controls")')
    await page.waitForTimeout(500)
    await page.screenshot({
      path: path.join(SCREENSHOT_DIR, 'showcase-controls-1440px.png'),
      fullPage: true
    })

    // Tab: Tables
    console.log('Capturing Tables & Cards tab...')
    await page.click('button:has-text("Tables & Cards")')
    await page.waitForTimeout(500)
    await page.screenshot({
      path: path.join(SCREENSHOT_DIR, 'showcase-tables-1440px.png'),
      fullPage: true
    })

    // Tab: Feedback
    console.log('Capturing Feedback tab...')
    await page.click('button:has-text("Feedback")')
    await page.waitForTimeout(500)
    await page.screenshot({
      path: path.join(SCREENSHOT_DIR, 'showcase-feedback-1440px.png'),
      fullPage: true
    })

    console.log('All screenshots captured successfully!')
  } catch (err) {
    console.error('Error during capture:', err)
  } finally {
    await browser.close()
    devServer.kill()
    process.exit(0)
  }
}

run()
