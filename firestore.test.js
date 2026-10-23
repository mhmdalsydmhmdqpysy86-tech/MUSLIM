const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

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

function buildValidRoomTask(uid, ts) {
  return {
    userId: uid,
    roomNumber: "304",
    roomTitle: "غرفة الملكية #304",
    floorNumber: 3,
    priority: "HIGH",
    status: "PENDING",
    subtitleNote: "تسجيل خروج النزيل قبل 5 دقائق",
    targetMinutes: 15,
    iotStatusLabel: "IoT: نقص مناشف",
    iotSeverity: "WARNING",
    qualityScore: 84,
    assignedStaff: "أحمد السالم",
    alertActive: false,
    alertMessage: "",
    createdAt: ts,
    updatedAt: ts,
  };
}

function buildValidInspection(uid, ts) {
  return {
    userId: uid,
    roomNumber: "304",
    roomTitle: "غرفة الملكية #304",
    qualityScore: 84,
    issuesCount: 2,
    durationSeconds: 108,
    beddingIssues: 1,
    towelIssues: 1,
    surfaceIssues: 0,
    maintenanceIssues: 0,
    summaryText: "تم رصد وسادة غير متناسقة ونقص مناشف",
    notifiedHousekeeper: true,
    createdAt: ts,
    updatedAt: ts,
  };
}

function buildValidConsumable(uid, ts) {
  return {
    userId: uid,
    itemName: "شامبو فاخر بالأرغان 50 مل",
    category: "SHAMPOO",
    currentStock: 14,
    minThreshold: 25,
    maxCapacity: 120,
    unitLabel: "عبوة",
    storageLocation: "مخزن الدور 3",
    managementAlertSent: true,
    alertNote: "تنبيه للإدارة: مخزون الشامبو دون الحد الأدنى",
    createdAt: ts,
    updatedAt: ts,
  };
}

test("Unauthenticated user: cannot list or read room_tasks or consumables", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/room_tasks`).get());
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/consumables`).get());
  await assertFails(unauthDb.doc(`users/${ALICE_UID}/consumables/item_shampoo`).get());
});

test("Authenticated owner: can create, read, list, and update own room_task", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date(Date.now() - 1000);

  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/room_tasks/room_304`).set(buildValidRoomTask(ALICE_UID, now))
  );

  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/room_tasks`).where("userId", "==", ALICE_UID).get()
  );

  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/room_tasks/room_304`).update({
      status: "NEEDS_FIX",
      alertActive: true,
      alertMessage: "يرجى تعديل الوسادة وتزويد المناشف",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Authenticated owner: can create, query, and update consumables stock & management alerts", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date(Date.now() - 1000);

  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/consumables/item_shampoo`).set(buildValidConsumable(ALICE_UID, now))
  );

  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/consumables`).where("userId", "==", ALICE_UID).get()
  );

  // Valid stock deduction & alert trigger
  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/consumables/item_shampoo`).update({
      currentStock: 10,
      managementAlertSent: true,
      alertNote: "طلب توريد عاجل قيد المراجعة من الإدارة",
      updatedAt: new Date(Date.now() - 400),
    })
  );

  // Reject negative stock poisoning
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/consumables/item_shampoo`).update({
      currentStock: -5,
      updatedAt: new Date(Date.now() - 300),
    })
  );

  // Reject invalid category enum
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/consumables/item_badcat`).set({
      ...buildValidConsumable(ALICE_UID, now),
      category: "INVALID_CATEGORY",
    })
  );
});

test("Cross-user isolation: Bob cannot read or modify Alice's consumables or room_tasks", async () => {
  const now = new Date(Date.now() - 1000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().doc(`users/${ALICE_UID}/room_tasks/room_304`).set(buildValidRoomTask(ALICE_UID, now));
    await ctx.firestore().doc(`users/${ALICE_UID}/consumables/item_soap`).set(buildValidConsumable(ALICE_UID, now));
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.doc(`users/${ALICE_UID}/room_tasks/room_304`).get());
  await assertFails(bobDb.doc(`users/${ALICE_UID}/consumables/item_soap`).get());
  await assertFails(bobDb.collection(`users/${ALICE_UID}/consumables`).get());
  await assertFails(
    bobDb.doc(`users/${ALICE_UID}/consumables/item_soap`).update({
      currentStock: 0,
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Authenticated owner: can create and list inspection logs; immutable createdAt enforced", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date(Date.now() - 1000);

  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/inspections/insp_1`).set(buildValidInspection(ALICE_UID, now))
  );

  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/inspections`).where("userId", "==", ALICE_UID).get()
  );

  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/inspections/insp_1`).update({
      qualityScore: 100,
      updatedAt: new Date(Date.now() - 500),
    })
  );
});
