// Run Firestore integration tests without a live Firebase project or manual ADB setup.
// Prompt: "make all test cases for this, stress test it to the limit, and with all the
// other test cases from before all together so the app works".
const { spawn, spawnSync } = require('node:child_process');
const fs = require('node:fs');
const net = require('node:net');
const os = require('node:os');
const path = require('node:path');

const testClass = process.argv[2] || 'ReservationRulesFirestoreTest';
if (!['ReservationRulesFirestoreTest', 'ForegroundLotteryDrawTest', 'CampsiteOwnerMessagesTest', 'all'].includes(testClass)) {
  throw new Error(`Unsupported Firestore test class: ${testClass}`);
}
// Gradle's single 150-test instrumentation session exhausted the AVD. Keep one
// Firestore emulator running, but start fresh instrumentation for each group.
const allGroups = [
  'CampsiteLifecycleTest,CampsiteOwnerMessagesTest,CampsiteOwnerRulesTest,CampsitePhotoTest',
  'DatabaseAdminsTest,DatabaseCampsiteOwnersTest,DatabaseCampsitesTest,DatabaseEntrantsTest,DatabaseSearchTest,DatabaseUpdateAndDeleteTest,DeviceIdentityTest,TestDataGeneratorTest',
  'EntrantCampsiteDetailsTest',
  'EntrantLoginScreenTest,EntrantMessagesTest,EntrantProfileTest',
  'EntrantReservationsListTest,EntrantSearchTest,OwnerReservationViewsTest,ProfilePhotoTest,ExampleInstrumentedTest',
  'QrCodeScanTest,SharedLotteryTest,adminUITest,ReviewerQrFirestoreTest,ReviewerFirestoreSeed',
  'ForegroundLotteryDrawTest,ReservationRulesFirestoreTest'
];

const root = path.resolve(__dirname, '..');
const windows = process.platform === 'win32';
const npx = windows ? 'npx.cmd' : 'npx';
const gradle = path.join(root, windows ? 'gradlew.bat' : 'gradlew');
const sdkProperty = fs.readFileSync(path.join(root, 'local.properties'), 'utf8')
  .match(/^sdk\.dir=(.*)$/m)?.[1];
const sdk = process.env.ANDROID_HOME || process.env.ANDROID_SDK_ROOT ||
  sdkProperty?.replace(/\\:/g, ':').replace(/\\\\/g, '\\');
if (!sdk) throw new Error('Set ANDROID_HOME or sdk.dir in local.properties.');
const adb = path.join(sdk, 'platform-tools', windows ? 'adb.exe' : 'adb');
if (!fs.existsSync(adb)) throw new Error(`ADB is missing: ${adb}`);

function run(command, args) {
  const result = spawnSync(command, args, { cwd: root, stdio: 'inherit', shell: windows });
  if (result.error) throw result.error;
  if (result.status !== 0) throw new Error(`${command} exited with ${result.status}`);
}

function portOpen() {
  return new Promise(resolve => {
    const socket = net.connect(8188, '127.0.0.1');
    socket.once('connect', () => { socket.destroy(); resolve(true); });
    socket.once('error', () => resolve(false));
    socket.setTimeout(1000, () => { socket.destroy(); resolve(false); });
  });
}

function stopTree(child) {
  if (!child || child.exitCode !== null) return;
  if (windows) spawnSync('taskkill', ['/PID', String(child.pid), '/T', '/F'], { stdio: 'ignore' });
  else child.kill('SIGTERM');
}

async function main() {
  if (await portOpen()) throw new Error('Port 8188 is already in use; stop that service before running this task.');
  const emulatorWorkDir = fs.mkdtempSync(path.join(os.tmpdir(), 'olives-firestore-test-'));
  const firebase = spawn(npx, [
    '--yes', '--offline', 'firebase-tools', 'emulators:start', '--only', 'firestore',
    '--project', 'demo-olives-lottery', '--config', path.join(__dirname, 'firebase-test.json')
  ], { cwd: emulatorWorkDir, stdio: 'inherit', shell: windows });
  let reversed = false;
  try {
    let ready = false;
    for (let attempt = 0; attempt < 90; attempt++) {
      if (await portOpen()) { ready = true; break; }
      if (firebase.exitCode !== null) throw new Error(`Firestore emulator exited with ${firebase.exitCode}`);
      await new Promise(resolve => setTimeout(resolve, 1000));
    }
    if (!ready) throw new Error('Firestore emulator did not start within 90 seconds.');
    run(adb, ['reverse', 'tcp:8188', 'tcp:8188']);
    reversed = true;
    for (const group of testClass === 'all' ? allGroups : [testClass]) {
      const classes = group.split(',').map(name => `com.example.code.${name}`).join(',');
      console.log(`Running Android tests: ${group}`);
      run(gradle, [':app:connectedDebugAndroidTest', '--offline', '--console=plain',
        `-Pandroid.testInstrumentationRunnerArguments.class=${classes}`,
        '-Pandroid.testInstrumentationRunnerArguments.firestoreEmulatorHost=127.0.0.1',
        '-Pandroid.testInstrumentationRunnerArguments.firestoreEmulatorPort=8188']);
    }
  } finally {
    if (reversed) spawnSync(adb, ['reverse', '--remove', 'tcp:8188'], { stdio: 'inherit' });
    stopTree(firebase);
    const tempRoot = fs.realpathSync(os.tmpdir());
    const resolvedWorkDir = fs.realpathSync(emulatorWorkDir);
    if (path.dirname(resolvedWorkDir) !== tempRoot ||
        !path.basename(resolvedWorkDir).startsWith('olives-firestore-test-')) {
      throw new Error(`Refusing to remove unexpected emulator directory: ${resolvedWorkDir}`);
    }
    fs.rmSync(emulatorWorkDir, { recursive: true, force: true });
  }
}

main().catch(error => { console.error(error); process.exitCode = 1; });
