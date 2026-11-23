const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const CLIENT_UID = "alice_client_1";
const CRAFTSMAN_UID = "bob_craftsman_2";
const OTHER_CRAFTSMAN_UID = "omar_craftsman_3";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
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

test("Users: unauthenticated rejected, owner profile updates succeed, role lock cannot be bypassed", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(CLIENT_UID).get());

  const clientDb = testEnv.authenticatedContext(CLIENT_UID).firestore();
  const ts = new Date(Date.now() - 2000);

  await assertSucceeds(
    clientDb.collection("users").doc(CLIENT_UID).set({
      userId: CLIENT_UID,
      fullName: "يونس العلمي",
      email: "youness@example.com",
      phone: "+212 661-112233",
      city: "الدار البيضاء",
      role: "client",
      roleLocked: true,
      specialtyId: "plumbing",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Editing phone & bio & avatarUri succeeds
  await assertSucceeds(
    clientDb.collection("users").doc(CLIENT_UID).update({
      phone: "+212 662-998877",
      city: "الرباط",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Attempting to switch locked role fails
  await assertFails(
    clientDb.collection("users").doc(CLIENT_UID).update({
      role: "craftsman",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Cross-user read rejected
  const craftDb = testEnv.authenticatedContext(CRAFTSMAN_UID).firestore();
  await assertFails(craftDb.collection("users").doc(CLIENT_UID).get());
});

test("Craftsmen directory & portfolio: owner writes succeed, non-owner denied, online query works", async () => {
  const craftDb = testEnv.authenticatedContext(CRAFTSMAN_UID).firestore();
  const clientDb = testEnv.authenticatedContext(CLIENT_UID).firestore();
  const ts = new Date(Date.now() - 3000);

  await assertSucceeds(
    craftDb.collection("craftsmen_directory").doc(CRAFTSMAN_UID).set({
      craftsmanId: CRAFTSMAN_UID,
      displayName: "كريم بنعلي",
      specialtyId: "plumbing",
      specialtyName: "سباكة",
      city: "الدار البيضاء",
      bio: "خبير ترصيص وصيانة السخانات",
      experienceYears: 8,
      rating: 4.9,
      completedJobs: 120,
      basePriceDh: 130,
      lat: 33.5822,
      lng: -7.6324,
      online: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Client can query online craftsmen
  await assertSucceeds(
    clientDb.collection("craftsmen_directory").where("online", "==", true).get()
  );

  // Unfiltered directory list query fails when offline docs exist or unfiltered
  await assertFails(clientDb.collection("craftsmen_directory").get());

  // Craftsman manages their own portfolio
  await assertSucceeds(
    craftDb
      .collection("craftsmen_directory")
      .doc(CRAFTSMAN_UID)
      .collection("portfolio")
      .doc("work_1")
      .set({
        craftsmanId: CRAFTSMAN_UID,
        title: "تركيب سخان غاز",
        description: "إصلاح كامل بالضمان",
        categoryName: "سباكة",
        createdAt: ts,
        updatedAt: ts,
      })
  );

  await assertSucceeds(
    clientDb
      .collection("craftsmen_directory")
      .doc(CRAFTSMAN_UID)
      .collection("portfolio")
      .get()
  );

  // Client cannot add items to craftsman portfolio
  await assertFails(
    clientDb
      .collection("craftsmen_directory")
      .doc(CRAFTSMAN_UID)
      .collection("portfolio")
      .doc("work_2")
      .set({
        craftsmanId: CLIENT_UID,
        title: "Fake",
        description: "Test",
        categoryName: "سباكة",
        createdAt: ts,
        updatedAt: ts,
      })
  );
});

test("Service requests, isolated offers, chat, live GPS, and reviews enforce zero-trust rules", async () => {
  const clientDb = testEnv.authenticatedContext(CLIENT_UID).firestore();
  const craftDb = testEnv.authenticatedContext(CRAFTSMAN_UID).firestore();
  const otherCraftDb = testEnv.authenticatedContext(OTHER_CRAFTSMAN_UID).firestore();
  const ts = new Date(Date.now() - 4000);
  const reqId = "req_101";

  // Client creates searching service request
  await assertSucceeds(
    clientDb.collection("service_requests").doc(reqId).set({
      clientId: CLIENT_UID,
      clientName: "يونس",
      categoryId: "plumbing",
      categoryName: "سباكة",
      title: "تسرب تحت الحوض",
      description: "تسرب مستمر في المطبخ",
      addressLabel: "المعاريف، الدار البيضاء",
      lat: 33.5822,
      lng: -7.6324,
      proposedPrice: 120,
      paymentMethod: "cash",
      urgent: true,
      status: "searching",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Queries aligned with rules
  await assertSucceeds(
    clientDb.collection("service_requests").where("clientId", "==", CLIENT_UID).get()
  );
  await assertSucceeds(
    craftDb.collection("service_requests").where("status", "==", "searching").get()
  );
  await assertFails(craftDb.collection("service_requests").get());

  // Craftsman submits an offer
  await assertSucceeds(
    craftDb
      .collection("service_requests")
      .doc(reqId)
      .collection("offers")
      .doc("offer_bob")
      .set({
        requestId: reqId,
        clientId: CLIENT_UID,
        craftsmanId: CRAFTSMAN_UID,
        craftsmanName: "كريم بنعلي",
        craftsmanPhone: "+212 661-234510",
        craftsmanRating: 4.9,
        completedJobs: 120,
        specialty: "سباكة",
        offeredPrice: 140,
        etaMinutes: 8,
        distanceKm: 1.4,
        note: "متوفر الآن",
        status: "pending",
        expiresAtMillis: Date.now() + 60000,
        createdAt: ts,
        updatedAt: ts,
      })
  );

  // Client views all offers for their request
  await assertSucceeds(
    clientDb
      .collection("service_requests")
      .doc(reqId)
      .collection("offers")
      .where("clientId", "==", CLIENT_UID)
      .get()
  );

  // Zero-competitor visibility: another craftsman CANNOT read Bob's offer!
  await assertFails(
    otherCraftDb
      .collection("service_requests")
      .doc(reqId)
      .collection("offers")
      .doc("offer_bob")
      .get()
  );
  await assertFails(
    otherCraftDb
      .collection("service_requests")
      .doc(reqId)
      .collection("offers")
      .where("clientId", "==", CLIENT_UID)
      .get()
  );

  // Bob can read only his own offer
  await assertSucceeds(
    craftDb
      .collection("service_requests")
      .doc(reqId)
      .collection("offers")
      .where("craftsmanId", "==", CRAFTSMAN_UID)
      .get()
  );

  // Client accepts Bob's offer
  await assertSucceeds(
    clientDb.collection("service_requests").doc(reqId).update({
      status: "accepted",
      acceptedPrice: 140,
      assignedCraftsmanId: CRAFTSMAN_UID,
      assignedCraftsmanName: "كريم بنعلي",
      assignedCraftsmanPhone: "+212 661-234510",
      assignedCraftsmanRating: 4.9,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Bob publishes real-time GPS coordinates
  await assertSucceeds(
    craftDb.collection("service_requests").doc(reqId).update({
      craftsmanLat: 33.583,
      craftsmanLng: -7.631,
      updatedAt: new Date(Date.now() - 800),
    })
  );

  // Participating chat messages succeed for participants, fail for outsider
  await assertSucceeds(
    craftDb
      .collection("service_requests")
      .doc(reqId)
      .collection("messages")
      .doc("msg_1")
      .set({
        requestId: reqId,
        memberUids: [CLIENT_UID, CRAFTSMAN_UID],
        senderId: CRAFTSMAN_UID,
        senderName: "كريم بنعلي",
        senderRole: "craftsman",
        text: "أنا في الطريق إليك",
        createdAt: new Date(Date.now() - 600),
      })
  );
  await assertSucceeds(
    clientDb
      .collection("service_requests")
      .doc(reqId)
      .collection("messages")
      .where("memberUids", "array-contains", CLIENT_UID)
      .get()
  );
  await assertFails(
    otherCraftDb
      .collection("service_requests")
      .doc(reqId)
      .collection("messages")
      .doc("msg_1")
      .get()
  );

  // Advance job to completed and enforce terminal lock
  await assertSucceeds(
    clientDb.collection("service_requests").doc(reqId).update({
      status: "completed",
      updatedAt: new Date(Date.now() - 400),
    })
  );
  await assertFails(
    craftDb.collection("service_requests").doc(reqId).update({
      craftsmanLat: 33.59,
      craftsmanLng: -7.64,
      updatedAt: new Date(Date.now() - 200),
    })
  );

  // Submit review
  await assertSucceeds(
    clientDb.collection("reviews").doc("rev_1").set({
      requestId: reqId,
      clientId: CLIENT_UID,
      craftsmanId: CRAFTSMAN_UID,
      rating: 5,
      tags: ["سريع", "متقن"],
      comment: "خدمة ممتازة",
      createdAt: new Date(Date.now() - 200),
    })
  );
});
