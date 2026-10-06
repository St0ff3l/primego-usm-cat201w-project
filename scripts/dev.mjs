import { spawn } from 'node:child_process'
import { existsSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const rootDir = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const envPath = resolve(rootDir, '.env')
const frontendDir = resolve(rootDir, 'frontend')
const backendDir = resolve(rootDir, 'backend')
const appJar = resolve(backendDir, 'ruoyi-admin/target/ruoyi-admin.jar')
const requiredVariables = [
  'PRIMEGO_DB_URL',
  'PRIMEGO_DB_USERNAME',
  'PRIMEGO_DB_PASSWORD',
  'PRIMEGO_REDIS_HOST',
  'PRIMEGO_REDIS_PORT',
  'PRIMEGO_REDIS_PASSWORD',
]

if (!existsSync(envPath)) {
  console.error('[dev] Missing root .env. Copy .env.example to .env and set the server MySQL/Redis connection values first.')
  process.exit(1)
}

try {
  process.loadEnvFile(envPath)
} catch (error) {
  console.error(`[dev] Could not read .env: ${error.message}`)
  process.exit(1)
}

const unconfiguredVariables = requiredVariables.filter((name) => {
  const value = process.env[name]?.trim()
  return !value || /^(change_me|.*\.example.*)$/i.test(value)
})

if (unconfiguredVariables.length > 0) {
  console.error(`[dev] Set real server values in .env for: ${unconfiguredVariables.join(', ')}`)
  process.exit(1)
}

if (!existsSync(resolve(frontendDir, 'node_modules/.bin/vite'))) {
  console.error('[dev] Storefront dependencies are missing. Run `npm --prefix frontend ci` once, then retry.')
  process.exit(1)
}

const children = new Set()
let shuttingDown = false

function stopAll(signal, code = 0) {
  if (shuttingDown) return
  shuttingDown = true
  process.exitCode = code

  for (const child of children) {
    if (child.exitCode === null && child.signalCode === null) child.kill(signal)
  }
}

process.on('SIGINT', () => {
  console.log('\n[dev] Stopping storefront and backend...')
  stopAll('SIGTERM')
})
process.on('SIGTERM', () => stopAll('SIGTERM'))

function startService(label, command, args, cwd) {
  const child = spawn(command, args, { cwd, env: process.env, stdio: 'inherit' })
  children.add(child)

  child.once('error', (error) => {
    console.error(`[dev] Could not start ${label}: ${error.message}`)
    children.delete(child)
    stopAll('SIGTERM', 1)
  })

  child.once('exit', (code, signal) => {
    children.delete(child)
    if (shuttingDown) return

    const exitCode = code === 0 ? 1 : (code ?? 1)
    console.error(`[dev] ${label} stopped${signal ? ` (${signal})` : ` (exit ${code})`}; stopping the other process.`)
    stopAll('SIGTERM', exitCode)
  })

  return child
}

function buildBackend() {
  console.log('[dev] Building the backend (Maven, tests skipped)...')

  return new Promise((resolveBuild) => {
    const child = spawn('mvn', ['-pl', 'ruoyi-admin', '-am', '-DskipTests', 'package'], {
      cwd: backendDir,
      env: process.env,
      stdio: 'inherit',
    })
    children.add(child)

    child.once('error', (error) => {
      children.delete(child)
      console.error(`[dev] Could not run Maven: ${error.message}`)
      if (!shuttingDown) stopAll('SIGTERM', 1)
      resolveBuild(1)
    })

    child.once('exit', (code, signal) => {
      children.delete(child)
      resolveBuild(code ?? (signal ? 1 : 0))
    })
  })
}

console.log('[dev] Starting the PrimeGo storefront at http://localhost:5173')
startService('storefront', 'npm', ['run', 'dev'], frontendDir)

const buildExitCode = await buildBackend()
if (!shuttingDown) {
  if (buildExitCode !== 0) {
    console.error('[dev] Backend build failed; stopping the storefront.')
    stopAll('SIGTERM', buildExitCode)
  } else if (!existsSync(appJar)) {
    console.error(`[dev] Backend build succeeded but the application jar was not found: ${appJar}`)
    stopAll('SIGTERM', 1)
  } else {
    console.log('[dev] Starting the API at http://localhost:8080')
    startService('backend', 'java', ['-jar', appJar], backendDir)
  }
}
