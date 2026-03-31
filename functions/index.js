const admin = require("firebase-admin");
const {onDocumentCreated} = require("firebase-functions/v2/firestore");

admin.initializeApp();

exports.sendChatMessageNotification = onDocumentCreated(
  "conversations/{conversationId}/messages/{messageId}",
  async (event) => {
    const snapshot = event.data;
    if (!snapshot) return;

    const message = snapshot.data();
    const conversationId = event.params.conversationId;
    const senderId = message.senderId;
    const senderName = message.senderDisplayName || "New message";
    const messageText = message.text || "Open chat";

    const conversationRef = admin.firestore().collection("conversations").doc(conversationId);
    const conversationDoc = await conversationRef.get();
    if (!conversationDoc.exists) return;

    const conversation = conversationDoc.data() || {};
    const participantIds = Array.isArray(conversation.participantIds) ? conversation.participantIds : [];
    const recipientIds = participantIds.filter((id) => id && id !== senderId);
    if (recipientIds.length === 0) return;

    const recipientDocs = await admin.firestore().getAll(
      ...recipientIds.map((recipientId) =>
        admin.firestore().collection("users").doc(recipientId)
      )
    );

    const tokens = recipientDocs
      .map((doc) => doc.exists ? doc.data()?.fcmToken : null)
      .filter((token) => typeof token === "string" && token.length > 0);

    if (tokens.length === 0) return;

    const payload = {
      notification: {
        title: senderName,
        body: messageText,
      },
      data: {
        conversationId,
        senderId: senderId || "",
        messageId: snapshot.id,
      },
      tokens,
    };

    const response = await admin.messaging().sendEachForMulticast(payload);
    const invalidTokens = [];

    response.responses.forEach((sendResponse, index) => {
      if (!sendResponse.success) {
        const code = sendResponse.error?.code || "";
        if (
          code === "messaging/registration-token-not-registered" ||
          code === "messaging/invalid-registration-token"
        ) {
          invalidTokens.push(tokens[index]);
        }
      }
    });

    if (invalidTokens.length === 0) return;

    await Promise.all(
      recipientDocs
        .filter((doc) => doc.exists && invalidTokens.includes(doc.data()?.fcmToken))
        .map((doc) => doc.ref.update({fcmToken: admin.firestore.FieldValue.delete()}))
    );
  }
);
