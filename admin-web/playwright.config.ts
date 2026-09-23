import { defineConfig } from '@playwright/test'

export default defineConfig({
  testDir: './tests',
  timeout: 150_000,
  expect: { timeout: 12_000 },
  fullyParallel: false,
  workers: 1,
  retries: 0,
  reporter: [['list'], ['json', { outputFile: 'test-results/report.json' }]],
  use: {
    baseURL: 'http://localhost:5173',
    viewport: { width: 1920, height: 1080 },
    locale: 'zh-CN',
    video: { mode: 'on', size: { width: 1920, height: 1080 } },
    screenshot: 'only-on-failure',
    actionTimeout: 15_000
  },
  outputDir: 'test-results',
  webServer: {
    command: 'npm run dev',
    url: 'http://localhost:5173',
    reuseExistingServer: true,
    timeout: 90_000
  }
})
