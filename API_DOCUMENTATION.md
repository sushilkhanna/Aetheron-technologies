# Bike Pooling App - Complete Backend API Documentation

**Target Audience:** Frontend Engineer / Mobile App Developers / Integration Team  
**Base URL:** `http://<your-server-domain-or-ip>:8080`  
**Authentication Header:** `Authorization: Bearer <JWT_TOKEN>` *(Required for all protected endpoints)*  
**Response Wrapper:** All standard REST endpoints return responses wrapped inside `ApiResponse<T>`:
```json
{
  "success": true,
  "message": "Human-readable status message",
  "data": { ... }
}
```

---

## 💬 7. Streamlined WebSocket Chat System (3 Core Endpoints)

The Chat System is simplified into **3 core endpoints**:

1. **Inbox Person List:** `GET /api/chat/conversations` — Retrieves the list of persons/conversations with last message summary.
2. **Read Chat History:** `GET /api/chat/messages/{otherUserId}` — Reads messages with a particular person by user ID or applicant ID. **Automatically marks incoming messages as `read = true` upon fetching**.
3. **Send Message (WebSocket STOMP & REST Fallback):** STOMP `/app/chat.send` (or REST `POST /api/chat/send`) — Sends real-time message to a particular person. **ALLOWED ONLY IF sender & receiver have a present ride booked or active application**. Real-time WebSocket broadcast & FCM push notification included.

---

### 7.1 Inbox Person List
- **API Call:** `GET /api/chat/conversations`
- **Auth Required:** Yes (`Bearer <token>`)

**Response DTO:**
```json
{
  "success": true,
  "message": "Conversations fetched.",
  "data": [
    {
      "templateId": 501,
      "routeSummary": "Hinjawadi -> Kharadi",
      "otherUserId": 102,
      "otherUserName": "Priya Patel",
      "lastMessage": "Hey, I am waiting near the main gate.",
      "lastMessageTime": "2026-08-09T14:45:00",
      "lastMessageRead": false,
      "lastMessageFromMe": true,
      "unreadCount": 0
    }
  ]
}
```

---

### 7.2 Read Chat Messages (By User ID / Applicant ID)
- **API Call:** `GET /api/chat/messages/{otherUserId}` *(or `/api/chat/messages/applicant/{applicantId}`)*
- **Auth Required:** Yes (`Bearer <token>`)
- **Behavior:** Automatically marks all incoming unread messages from `otherUserId` to current user as **`read = true`**!

**Response DTO:**
```json
{
  "success": true,
  "message": "Messages fetched.",
  "data": {
    "content": [
      {
        "id": 8001,
        "templateId": 501,
        "senderId": 101,
        "senderName": "Rahul Sharma",
        "receiverId": 102,
        "receiverName": "Priya Patel",
        "content": "Hey, I am waiting near the main gate.",
        "read": true,                  // Automatically marked as read!
        "createdAt": "2026-08-09T14:45:00"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1,
    "first": true,
    "last": true
  }
}
```

---

### 7.3 Send Chat Message (WebSocket STOMP & REST)
- **WebSocket STOMP Destination:** `/app/chat.send`
- **REST Fallback Call:** `POST /api/chat/send`
- **Auth Required:** Yes (`Bearer <token>`)
- **Present Ride Booking Enforcement:** Allowed **ONLY IF** user has a present ride booked (`CONFIRMED`, `BOOKED`, `STARTED`, `VERIFIED` or active application) with the recipient.

**Request Payload:**
```json
{
  "receiverId": 102,                   // Direct user ID, applicant user ID, OR applicationId (301)
  "templateId": 501,                   // Optional: Scheduled ride template ID
  "content": "Hey, I am waiting near the main gate."
}
```

**REST Response DTO:**
```json
{
  "success": true,
  "message": "Message sent.",
  "data": {
    "id": 8001,
    "templateId": 501,
    "senderId": 101,
    "senderName": "Rahul Sharma",
    "receiverId": 102,
    "receiverName": "Priya Patel",
    "content": "Hey, I am waiting near the main gate.",
    "read": false,
    "createdAt": "2026-08-09T14:45:00"
  }
}
```
