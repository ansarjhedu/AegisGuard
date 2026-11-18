const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_123";
const BOB_UID = "bob_uid_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validSessionPayload(uid, overrides = {}) {
  const pastTime = new Date(Date.now() - 5000);
  return {
    userId: uid,
    deviceName: "Home Desktop PC",
    deviceModel: "Windows Workstation",
    platform: "Windows 11",
    clientType: "Google Chrome (Gmail Web)",
    ipAddress: "192.168.10.24",
    locationSource: "User-Verified Location",
    userEmail: "ansarjhedu@gmail.com",
    latitude: 31.5204,
    longitude: 74.3587,
    accuracyMeters: 10.0,
    locationAddress: "Lahore, Punjab, Pakistan",
    batteryLevelPercent: 100,
    charging: true,
    online: true,
    aegisGuardInstalled: false,
    trackingMode: "GOOGLE_LOCATION",
    lockedRemotely: false,
    signedOutRemotely: false,
    networkCarrierOrWifi: "Wi-Fi / Ethernet Session",
    lastSeenAtMillis: Date.now(),
    createdAt: pastTime,
    updatedAt: pastTime,
    ...overrides,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot list, read, or write sessions", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("sessions").get()
  );
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("sessions")
      .doc("node_01")
      .set(validSessionPayload(ALICE_UID))
  );
});

test("Authenticated owner: valid Kotlin-mirrored create, telemetry update, remote security update, and list query succeed", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const sessionsRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("sessions");

  // 1. Empty collection list succeeds for owner
  await assertSucceeds(sessionsRef.get());

  // 2. Create mobile & desktop sessions (without @DocumentId `id` field, optional pairedToken omitted)
  await assertSucceeds(
    sessionsRef.doc("node_mobile_01").set(
      validSessionPayload(ALICE_UID, {
        deviceName: "Google Pixel 8",
        platform: "Android 15",
        clientType: "AegisGuard Master (Active)",
        aegisGuardInstalled: true,
        charging: false,
        batteryLevelPercent: 86,
      })
    )
  );

  await assertSucceeds(
    sessionsRef.doc("node_desktop_01").set(
      validSessionPayload(ALICE_UID, {
        deviceName: "Desktop PC 1",
        trackingMode: "AEGIS_DEVICE_TOKEN",
        pairedToken: "AEGIS-TK-9F82-B4E1",
      })
    )
  );

  // 3. List all active sessions succeeds
  await assertSucceeds(sessionsRef.get());

  // 4. Heartbeat / location update preserving createdAt succeeds
  await assertSucceeds(
    sessionsRef.doc("node_mobile_01").update({
      locationAddress: "Islamabad, Pakistan",
      latitude: 33.6844,
      longitude: 73.0479,
      batteryLevelPercent: 82,
      lastSeenAtMillis: Date.now(),
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // 5. Remote security lockdown / sign-out update succeeds
  await assertSucceeds(
    sessionsRef.doc("node_desktop_01").update({
      lockedRemotely: true,
      signedOutRemotely: true,
      online: false,
      lastSeenAtMillis: Date.now(),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // 6. Owner delete succeeds
  await assertSucceeds(sessionsRef.doc("node_desktop_01").delete());
});

test("Authenticated user cannot read, list, create, update, or delete another user's sessions", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context
      .firestore()
      .collection("users")
      .doc(BOB_UID)
      .collection("sessions")
      .doc("bob_desktop_01")
      .set(validSessionPayload(BOB_UID));
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobSessions = aliceDb
    .collection("users")
    .doc(BOB_UID)
    .collection("sessions");

  await assertFails(bobSessions.get());
  await assertFails(bobSessions.doc("bob_desktop_01").get());
  await assertFails(
    bobSessions.doc("alice_spoof").set(validSessionPayload(ALICE_UID))
  );
  await assertFails(
    bobSessions.doc("bob_desktop_01").update({
      signedOutRemotely: true,
      online: false,
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(bobSessions.doc("bob_desktop_01").delete());
});

test("Security bounds reject @DocumentId field, ghost fields, immutable mutation, future timestamps, and invalid values", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const sessionsRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("sessions");

  // Reject `@DocumentId` id key stored in document body
  await assertFails(
    sessionsRef
      .doc("bad_id_doc")
      .set(validSessionPayload(ALICE_UID, { id: "bad_id_doc" }))
  );

  // Reject ghost fields (shadow write)
  await assertFails(
    sessionsRef
      .doc("bad_ghost_doc")
      .set(validSessionPayload(ALICE_UID, { isAdmin: true }))
  );

  // Reject invalid trackingMode enum
  await assertFails(
    sessionsRef
      .doc("bad_enum_doc")
      .set(validSessionPayload(ALICE_UID, { trackingMode: "FAKE_MODE" }))
  );

  // Reject oversized deviceName (> 120 chars)
  await assertFails(
    sessionsRef
      .doc("bad_size_doc")
      .set(validSessionPayload(ALICE_UID, { deviceName: "X".repeat(121) }))
  );

  // Reject future timestamp (> request.time)
  await assertFails(
    sessionsRef
      .doc("bad_time_doc")
      .set(
        validSessionPayload(ALICE_UID, {
          createdAt: new Date(Date.now() + 60000),
        })
      )
  );

  // Reject invalid document ID characters
  await assertFails(
    sessionsRef
      .doc("bad id with spaces")
      .set(validSessionPayload(ALICE_UID))
  );

  // Create valid doc, then prove createdAt and userId cannot be mutated
  await assertSucceeds(
    sessionsRef.doc("valid_doc").set(validSessionPayload(ALICE_UID))
  );

  await assertFails(
    sessionsRef.doc("valid_doc").update({
      createdAt: new Date(Date.now() - 100),
      updatedAt: new Date(Date.now() - 100),
    })
  );

  await assertFails(
    sessionsRef.doc("valid_doc").update({
      userId: BOB_UID,
      updatedAt: new Date(Date.now() - 100),
    })
  );
});

test("Location history subcollection: owner create/query/delete succeed; cross-user, updates, future timestamps, and @DocumentId id fail", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  const aliceHistory = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("locationHistory");

  const validEntry = {
    userId: ALICE_UID,
    deviceId: "node_desktop_pc_1",
    deviceName: "Gulberg Desktop PC",
    locationAddress: "Gulberg, Lahore, Pakistan",
    locationSource: "Per-Device Verified Location",
    latitude: 31.5102,
    longitude: 74.3441,
    accuracyMeters: 12.0,
    recordedAtMillis: Date.now() - 1000,
    eventNote: "Device location calibrated to Gulberg",
    createdAt: new Date(Date.now() - 2000),
  };

  // Unauthenticated denied
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("locationHistory")
      .get()
  );

  // Owner create + ordered list query succeed
  await assertSucceeds(aliceHistory.doc("hist_01").set(validEntry));
  await assertSucceeds(
    aliceHistory.orderBy("recordedAtMillis", "desc").limit(100).get()
  );

  // Immutable history rejects updates
  await assertFails(
    aliceHistory.doc("hist_01").update({ locationAddress: "Tampered" })
  );

  // Reject @DocumentId `id` inside payload
  await assertFails(
    aliceHistory.doc("hist_bad_id").set({ ...validEntry, id: "hist_bad_id" })
  );

  // Reject future createdAt
  await assertFails(
    aliceHistory
      .doc("hist_bad_time")
      .set({ ...validEntry, createdAt: new Date(Date.now() + 60000) })
  );

  // Cross-user read/create/delete denied
  const bobViewAlice = bobDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("locationHistory");
  await assertFails(bobViewAlice.get());
  await assertFails(bobViewAlice.doc("hist_01").delete());

  // Owner delete succeeds
  await assertSucceeds(aliceHistory.doc("hist_01").delete());
});

test("Session locationPoints subcollection: enforces owner isolation, sessionId match, coordinate/accuracy bounds, and immutable timestamps", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  const sessionPoints = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("sessions")
    .doc("node_desktop_pc_1")
    .collection("locationPoints");

  const validPoint = {
    userId: ALICE_UID,
    sessionId: "node_desktop_pc_1",
    deviceName: "Gulberg Desktop PC",
    latitude: 31.5102,
    longitude: 74.3441,
    accuracyMeters: 8.5,
    verticalAccuracyMeters: 4.0,
    speedMetersPerSec: 0.0,
    bearingDegrees: 90.0,
    provider: "fused_gps",
    locationAddress: "Gulberg, Lahore, Pakistan",
    capturedAtMillis: Date.now() - 1000,
    eventNote: "Session Geolocation Fix",
    createdAt: new Date(Date.now() - 2000),
  };

  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("sessions")
      .doc("node_desktop_pc_1")
      .collection("locationPoints")
      .get()
  );

  // Owner create & ordered query succeed
  await assertSucceeds(sessionPoints.doc("pt_01").set(validPoint));
  await assertSucceeds(
    sessionPoints.orderBy("capturedAtMillis", "desc").limit(50).get()
  );

  // Reject mismatched sessionId in document vs path
  await assertFails(
    sessionPoints.doc("pt_mismatch").set({ ...validPoint, sessionId: "other_session" })
  );

  // Reject negative accuracy or out-of-range coordinates
  await assertFails(
    sessionPoints.doc("pt_bad_acc").set({ ...validPoint, accuracyMeters: -5.0 })
  );
  await assertFails(
    sessionPoints.doc("pt_bad_lat").set({ ...validPoint, latitude: 120.0 })
  );

  // Reject future createdAt & updates
  await assertFails(
    sessionPoints.doc("pt_future").set({ ...validPoint, createdAt: new Date(Date.now() + 60000) })
  );
  await assertFails(sessionPoints.doc("pt_01").update({ accuracyMeters: 1.0 }));

  // Cross-user access denied
  const bobViewAlice = bobDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("sessions")
    .doc("node_desktop_pc_1")
    .collection("locationPoints");
  await assertFails(bobViewAlice.get());
  await assertFails(bobViewAlice.doc("pt_01").delete());

  // Owner delete succeeds
  await assertSucceeds(sessionPoints.doc("pt_01").delete());
});
