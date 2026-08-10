# Bike Pooling App - Master API Documentation

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

## 🔤 1. Master Enum Definitions & Allowed Values

Whenever a request or response field references an **Enum**, only the exact string values listed below are accepted.

```typescript
// 1. User Role
enum Role {
  GUEST = "GUEST",
  USER = "USER",
  RIDER = "RIDER",
  DRIVER = "DRIVER",
  ADMIN = "ADMIN"
}

// 2. Gender
enum Gender {
  MALE = "MALE",
  FEMALE = "FEMALE",
  OTHERS = "OTHERS"
}

// 3. Preferred Driver Gender
enum PreferredGender {
  MALE = "MALE",
  FEMALE = "FEMALE",
  ANY = "ANY"
}

// 4. Payment Mode
enum PaymentMode {
  PAY_NOW = "PAY_NOW",
  PAY_ON_COMPLETION = "PAY_ON_COMPLETION"
}

// 5. Scheduled Ride Template Status
enum ScheduledRideStatus {
  ACTIVE = "ACTIVE",
  CANCELLED = "CANCELLED",
  EXPIRED = "EXPIRED"
}

// 6. Booker Application Status (Per Day)
enum ApplicationStatus {
  PENDING = "PENDING",
  CONFIRMED = "CONFIRMED",
  REJECTED = "REJECTED",
  WITHDRAWN = "WITHDRAWN",
  FINISH = "FINISH",
  EXPIRED = "EXPIRED"
}

// 7. Scheduled Ride Instance State
enum RideState {
  OPEN = "OPEN",
  BOOKED = "BOOKED",
  STARTED = "STARTED",             // Location stream to Booker & Admin live
  VERIFIED = "VERIFIED",           // OTP verified; location stream to Booker stops, cache continues
  COMPLETED = "COMPLETED",
  CANCELLED = "CANCELLED",
  EXPIRED = "EXPIRED",
  SOS_TRIGGERED = "SOS_TRIGGERED"
}

// 8. Live Ride State (On-Demand System)
enum LiveRideState {
  LIVE = "LIVE",                   // Driver is LIVE waiting for matching bookers
  CONFIRMED = "CONFIRMED",         // Driver accepted request; location streams to Booker UI
  VERIFIED = "VERIFIED",           // Driver verified OTP; location stream to Booker stops, cache continues for Admin
  COMPLETED = "COMPLETED",         // Ride finished
  CANCELLED = "CANCELLED",         // Ride cancelled
  EXPIRED = "EXPIRED"              // Ride search/live mode expired
}

// 9. SOS Emergency Status
enum SosStatus {
  TRIGGERED = "TRIGGERED",
  RESOLVED = "RESOLVED",
  FALSE_ALARM = "FALSE_ALARM",
  EXPIRED = "EXPIRED"
}
```

---

## 🚗 2. Live On-Demand Ride System (Core Real-Time APIs)

The Live Ride feature allows drivers traveling real-time from origin to destination (e.g. Home -> Office) to go **LIVE**, receive on-demand requests from bookers along their route, verify OTPs, and stream real-time locations safely.

### 2.1 Go Live (Driver)
- **Call:** `POST /api/live-rides/go-live`
- **Auth Required:** Authenticated Driver

```json
// REQUEST
{
  "fromName": "Kothrud, Pune",
  "fromLat": 18.5074000,
  "fromLng": 73.8077000,
  "toName": "Hinjawadi Phase 3, Pune",
  "toLat": 18.5912000,
  "toLng": 73.7389000,
  "extraDistanceKm": 3.0,
  "vehicleId": 5
}

// RESPONSE
{
  "success": true,
  "message": "Live mode started.",
  "data": {
    "liveRideId": 801,
    "driverId": 101,
    "driverName": "Rahul Sharma",
    "driverPhone": "+919876543210",
    "vehicleNumber": "MH12AB1234",
    "fromName": "Kothrud, Pune",
    "toName": "Hinjawadi Phase 3, Pune",
    "state": "LIVE"
  }
}
```

---

### 2.2 Stop Live Mode (Driver)
- **Call:** `POST /api/live-rides/stop-live`
- **Auth Required:** Authenticated Driver

```json
// RESPONSE
{
  "success": true,
  "message": "Live mode stopped.",
  "data": null
}
```

---

### 2.3 Preview Distance & Fare (Booker)
- **Call:** `POST /api/live-rides/preview`
- **Auth Required:** Authenticated

```json
// REQUEST
{
  "pickupName": "Bavdhan Chandani Chowk, Pune",
  "pickupLat": 18.5089000,
  "pickupLng": 73.7925000,
  "dropName": "Wakad Bridge, Pune",
  "dropLat": 18.5980000,
  "dropLng": 73.7620000
}

// RESPONSE
{
  "success": true,
  "message": "Fare preview calculated.",
  "data": {
    "pickupName": "Bavdhan Chandani Chowk, Pune",
    "dropName": "Wakad Bridge, Pune",
    "distanceKm": 10.40,
    "estimatedFare": 62.40
  }
}
```

---

### 2.4 Start Live Ride Search (Booker)
- **Call:** `POST /api/live-rides/search/start`
- **Auth Required:** Authenticated
- **Behavior:** Calculates distance and fare, registers active search request, finds matching drivers within 2 km radius or route corridor, and sends FCM push notifications to drivers.

```json
// REQUEST
{
  "pickupName": "Bavdhan Chandani Chowk, Pune",
  "pickupLat": 18.5089000,
  "pickupLng": 73.7925000,
  "dropName": "Wakad Bridge, Pune",
  "dropLat": 18.5670000,
  "dropLng": 73.9140000,
  "note": "Waiting near bus stop."
}

// RESPONSE
{
  "success": true,
  "message": "Live search started. Notified matching drivers nearby.",
  "data": 1005 // searchRequestId
}
```

---

### 2.5 Accept Live Ride Request (Driver)
- **Call:** `POST /api/live-rides/accept`
- **Auth Required:** Authenticated Driver

```json
// REQUEST
{
  "searchRequestId": 1005
}

// RESPONSE
{
  "success": true,
  "message": "Ride request accepted.",
  "data": {
    "liveRideId": 801,
    "driverId": 101,
    "driverName": "Rahul Sharma",
    "driverPhone": "+919876543210",
    "vehicleNumber": "MH12AB1234",
    "bookerId": 102,
    "bookerName": "Priya Patel",
    "bookerPhone": "+919123456789",
    "pickupName": "Bavdhan Chandani Chowk, Pune",
    "dropName": "Wakad Bridge, Pune",
    "distanceKm": 10.40,
    "fare": 62.40,
    "state": "CONFIRMED",             // Location streams to Booker STOMP topic!
    "bookerOtp": "4821"              // 4-digit OTP shown to Booker
  }
}
```

---

### 2.6 Verify Live Ride OTP (Driver)
- **Call:** `POST /api/live-rides/{liveRideId}/verify-otp?otp=4821`
- **Auth Required:** Authenticated Driver
- **Behavior:** Updates state to `VERIFIED`. STOMP location stream to Booker UI **STOPS** (booker is on bike). Location updates **CONTINUE** saving to cache for safety monitoring and Admin map.

```json
// RESPONSE
{
  "success": true,
  "message": "OTP verified successfully.",
  "data": {
    "liveRideId": 801,
    "state": "VERIFIED"
  }
}
```

---

### 2.7 Complete Live Ride (Driver)
- **Call:** `POST /api/live-rides/{liveRideId}/complete`
- **Auth Required:** Authenticated Driver

```json
// RESPONSE
{
  "success": true,
  "message": "Live ride completed.",
  "data": {
    "liveRideId": 801,
    "state": "COMPLETED"
  }
}
```

---

### 2.8 Get Active Live Ride
- **Call:** `GET /api/live-rides/active`
- **Auth Required:** Authenticated (Driver or Booker)

```json
// RESPONSE
{
  "success": true,
  "message": "Active live ride fetched.",
  "data": {
    "liveRideId": 801,
    "state": "CONFIRMED",
    "bookerOtp": "4821"
  }
}
```

---

### 2.9 Real-Time Live Location Tracking & Safety Rules

1. **Driver STOMP Location Push:**  
   - **Destination:** `/app/live-rides/{liveRideId}/location`
   - **Payload:** `{"lat": 18.5089, "lng": 73.7925, "bearingDegrees": 120.0, "speedKmh": 35.0, "timestamp": 1723245000000}`
2. **Booker STOMP Location Subscribe:**  
   - **Destination:** `/topic/live-rides/{liveRideId}/location`
   - **Active Window:** Broadcasted ONLY in `CONFIRMED` state (when driver is on the way to pick up booker). After OTP verification (`VERIFIED`), broadcast to Booker STOPS automatically.
3. **Safety Monitoring & Auto-Completion Rules:**
   - **Drop Point Arrival:** Driver in 1 km radius of drop point for 5 minutes (or passing drop point) -> Auto-completes live ride and clears location cache.
   - **Route Warning:** Driver >2 km off-route -> Booker receives FCM alert: *"Route is not the selected one, please make sure your safety."*
   - **Off-Route Auto-Completion:** Driver off-route for 20 minutes -> Auto-completes live ride and clears location cache.

---

## 📅 3. Scheduled Ride System (Core Pooling APIs)

*(Refer to Scheduled Ride Section for Template Posting, Multi-Date Search, Applicants Listing, and Confirmation)*
