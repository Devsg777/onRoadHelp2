/**
 * Import function triggers from their respective submodules:
 *
 * const {onCall} = require("firebase-functions/v2/https");
 * const {onDocumentWritten} = require("firebase-functions/v2/firestore");
 *
 * See a full list of supported triggers at https://firebase.google.com/docs/functions
 */

const {onRequest} = require("firebase-functions/v2/https");
const logger = require("firebase-functions/logger");

// Create and deploy your first functions
// https://firebase.google.com/docs/functions/get-started

// exports.helloWorld = onRequest((request, response) => {
//   logger.info("Hello logs!", {structuredData: true});
//   response.send("Hello from Firebase!");
// });
const functions = require('firebase-functions');
const admin = require('firebase-admin');
admin.initializeApp();

exports.sendSOSNotificationToNearbyHelpers = functions.firestore
    .document('sos_requests/{requestId}')
    .onCreate(async (snapshot, context) => {
        const sosData = snapshot.data();
        const customerLocation = sosData.location;
        const problem = sosData.problem;
        const requestId = context.params.requestId;

        if (!customerLocation || !problem) {
            console.log('SOS request missing location or problem.');
            return null;
        }

        // Adjust the radius (in degrees) as needed
        const proximityRadius = 0.05; // Approximately 5.5 km

        const nearbyHelpersSnapshot = await admin.firestore()
            .collection('helpers')
            .where('latitude', '>', customerLocation.latitude - proximityRadius)
            .where('latitude', '<', customerLocation.latitude + proximityRadius)
            .where('longitude', '>', customerLocation.longitude - proximityRadius)
            .where('longitude', '<', customerLocation.longitude + proximityRadius)
            .get();

        const payload = {
            notification: {
                title: 'New SOS Request!',
                body: `A customer needs help with: ${problem} nearby. Tap to view details.`,
                sound: 'default',
            },
            data: {
                requestId: requestId,
                problem: problem,
                latitude: customerLocation.latitude.toString(),
                longitude: customerLocation.longitude.toString(),
            },
        };

        const promises = [];
        nearbyHelpersSnapshot.forEach(helperDoc => {
            const helperToken = helperDoc.data().fcmToken;
            if (helperToken) {
                promises.push(admin.messaging().sendToDevice(helperToken, payload));
                console.log('Notification sent to helper:', helperDoc.id, 'Token:', helperToken);
            } else {
                console.log('Helper', helperDoc.id, 'has no FCM token.');
            }
        });

        const results = await Promise.allSettled(promises);
        results.forEach((result, index) => {
            if (result.status === 'rejected') {
                console.error('Failed to send notification:', result.reason);
            }
        });

        console.log('SOS notifications sent.');
        return null;
    });