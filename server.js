const express = require('express');
const cors = require('cors');

const app = express();
const PORT = process.env.PORT || 5000;

// Middleware
app.use(cors());
app.use(express.json());

// Mock data
const mockUsers = [
  {
    id: 1,
    name: 'Rahul Sharma',
    email: 'rahul@gmail.com',
    phone: '6876543210',
    password: 'Rahul@1234',
    rating: 4.8,
    totalRides: 156,
    verified: true,
    avatar: 'https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=100&h=100&fit=crop&crop=face'
  },
  {
    id: 2,
    name: 'Priya Patel',
    email: 'priya@example.com',
    phone: '8765432109',
    password: 'Priya@1234',
    rating: 4.9,
    totalRides: 203,
    verified: true,
    avatar: 'https://images.unsplash.com/photo-1494790108755-2616b612b786?w=100&h=100&fit=crop&crop=face'
  },
  {
    id: 3,
    name: 'Amit Kumar',
    email: 'amit@example.com',
    phone: '7654321098',
    password: 'Amit@1234',
    rating: 4.7,
    totalRides: 89,
    verified: true,
    avatar: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=100&h=100&fit=crop&crop=face'
  }
];

const mockVehicles = [
  {
    id: 1,
    userId: 1,
    bikeNumber: 'MH01AB1234',
    bikeName: 'Activa 6G',
    bikeModel: 'Activa 6G',
    bikeCompany: 'Honda',
    vehicleType: 'BIKE',
    status: 'ACTIVE',
    createdAt: new Date().toISOString()
  }
];

const mockRides = [
  {
    id: 1,
    driver: mockUsers[0],
    from: 'Electronic City',
    to: 'Koramangala',
    departureTime: '2024-04-14T08:30:00Z',
    arrivalTime: '2024-04-14T09:15:00Z',
    price: 80,
    totalSeats: 4,
    availableSeats: 2,
    vehicleType: 'Scooter',
    vehicleModel: 'Honda Activa',
    route: ['Electronic City', 'Hosur Road', 'Silk Board', 'Koramangala'],
    status: 'active',
    description: 'Daily commute to office. Flexible with timing.'
  },
  {
    id: 2,
    driver: mockUsers[1],
    from: 'Whitefield',
    to: 'Marathahalli',
    departureTime: '2024-04-14T09:00:00Z',
    arrivalTime: '2024-04-14T09:45:00Z',
    price: 60,
    totalSeats: 3,
    availableSeats: 1,
    vehicleType: 'Bike',
    vehicleModel: 'Royal Enfield Classic',
    route: ['Whitefield', 'ITPL', 'Marathahalli'],
    status: 'active',
    description: 'Regular office commute. Punctual and safe rider.'
  },
  {
    id: 3,
    driver: mockUsers[2],
    from: 'HSR Layout',
    to: 'BTM Layout',
    departureTime: '2024-04-14T18:00:00Z',
    arrivalTime: '2024-04-14T18:30:00Z',
    price: 50,
    totalSeats: 3,
    availableSeats: 3,
    vehicleType: 'Scooty',
    vehicleModel: 'TVS Jupiter',
    route: ['HSR Layout', 'Bommanahalli', 'BTM Layout'],
    status: 'active',
    description: 'Return journey from work. All seats available.'
  },
  {
    id: 4,
    driver: mockUsers[0],
    from: 'Indiranagar',
    to: 'MG Road',
    departureTime: '2024-04-14T10:30:00Z',
    arrivalTime: '2024-04-14T11:00:00Z',
    price: 40,
    totalSeats: 2,
    availableSeats: 1,
    vehicleType: 'Bike',
    vehicleModel: 'Bajaj Pulsar',
    route: ['Indiranagar', 'CMH Road', 'MG Road'],
    status: 'active',
    description: 'Quick trip to the city center.'
  },
  {
    id: 5,
    driver: mockUsers[1],
    from: 'Jayanagar',
    to: 'Electronic City',
    departureTime: '2024-04-14T08:00:00Z',
    arrivalTime: '2024-04-14T09:00:00Z',
    price: 90,
    totalSeats: 4,
    availableSeats: 2,
    vehicleType: 'Scooter',
    vehicleModel: 'Honda Activa',
    route: ['Jayanagar', 'Banashankari', 'Electronic City'],
    status: 'active',
    description: 'Morning commute with flexible pickup points.'
  }
];

const mockBookings = [
  {
    id: 1,
    rideId: 1,
    userId: 2,
    passengerName: 'Priya Patel',
    seatsBooked: 1,
    bookingTime: '2024-04-14T07:45:00Z',
    status: 'confirmed',
    pickupPoint: 'Silk Board',
    totalAmount: 80
  },
  {
    id: 2,
    rideId: 2,
    userId: 3,
    passengerName: 'Amit Kumar',
    seatsBooked: 2,
    bookingTime: '2024-04-14T08:30:00Z',
    status: 'confirmed',
    pickupPoint: 'ITPL',
    totalAmount: 120
  }
];

// API Routes

// Get all rides
app.get('/api/rides', (req, res) => {
  const { from, to, date } = req.query;
  let filteredRides = [...mockRides];
  
  if (from) {
    filteredRides = filteredRides.filter(ride => 
      ride.from.toLowerCase().includes(from.toLowerCase())
    );
  }
  
  if (to) {
    filteredRides = filteredRides.filter(ride => 
      ride.to.toLowerCase().includes(to.toLowerCase())
    );
  }
  
  res.json(filteredRides);
});

// Get ride by ID
app.get('/api/rides/:id', (req, res) => {
  const ride = mockRides.find(r => r.id === parseInt(req.params.id));
  if (!ride) {
    return res.status(404).json({ error: 'Ride not found' });
  }
  res.json(ride);
});

// Create new ride
app.post('/api/rides', (req, res) => {
  const newRide = {
    id: mockRides.length + 1,
    driver: mockUsers[0], // Mock driver
    ...req.body,
    status: 'active',
    availableSeats: req.body.totalSeats
  };
  mockRides.push(newRide);
  res.status(201).json(newRide);
});

// Vehicle endpoints
app.get('/api/vehicles', (req, res) => {
  res.json({
    success: true,
    data: mockVehicles
  });
});

app.get('/api/vehicles/my', (req, res) => {
  // Mock user ID 1 for demo
  const userVehicles = mockVehicles.filter(v => v.userId === 1);
  res.json({
    success: true,
    data: userVehicles
  });
});

app.post('/api/vehicles', (req, res) => {
  const { bikeNumber, bikeName, bikeModel, bikeCompany, vehicleType } = req.body;
  
  // Validate required fields
  if (!bikeNumber || !bikeName || !bikeModel || !bikeCompany) {
    return res.status(400).json({
      success: false,
      message: 'All vehicle fields are required'
    });
  }
  
  // Check if vehicle number already exists
  const existingVehicle = mockVehicles.find(v => v.bikeNumber === bikeNumber);
  if (existingVehicle) {
    return res.status(400).json({
      success: false,
      message: 'Vehicle with this number already exists'
    });
  }
  
  const newVehicle = {
    id: mockVehicles.length + 1,
    userId: 1, // Mock user ID
    bikeNumber,
    bikeName,
    bikeModel,
    bikeCompany,
    vehicleType: vehicleType || 'BIKE',
    status: 'ACTIVE',
    createdAt: new Date().toISOString()
  };
  
  mockVehicles.push(newVehicle);
  
  res.status(201).json({
    success: true,
    data: newVehicle,
    message: 'Vehicle added successfully'
  });
});

// Book a ride
app.post('/api/bookings', (req, res) => {
  const { rideId, userId, seatsBooked, pickupPoint } = req.body;
  
  const ride = mockRides.find(r => r.id === rideId);
  if (!ride) {
    return res.status(404).json({ error: 'Ride not found' });
  }
  
  if (ride.availableSeats < seatsBooked) {
    return res.status(400).json({ error: 'Not enough seats available' });
  }
  
  const newBooking = {
    id: mockBookings.length + 1,
    rideId,
    userId,
    passengerName: mockUsers.find(u => u.id === userId)?.name || 'Anonymous',
    seatsBooked,
    pickupPoint,
    bookingTime: new Date().toISOString(),
    status: 'confirmed',
    totalAmount: ride.price * seatsBooked
  };
  
  mockBookings.push(newBooking);
  ride.availableSeats -= seatsBooked;
  
  res.status(201).json(newBooking);
});

// Get user bookings
app.get('/api/bookings/:userId', (req, res) => {
  const userBookings = mockBookings.filter(b => b.userId === parseInt(req.params.userId));
  res.json(userBookings);
});

// Get all users (for demo purposes)
app.get('/api/users', (req, res) => {
  res.json(mockUsers);
});

// Get user by ID
app.get('/api/users/:id', (req, res) => {
  const user = mockUsers.find(u => u.id === parseInt(req.params.id));
  if (!user) {
    return res.status(404).json({ error: 'User not found' });
  }
  res.json(user);
});

// User authentication (mock with phone and OTP)
const otpStore = {}; // Temporary store for OTPs

app.post('/api/auth/register', (req, res) => {
  const { fullName, email, phone, password } = req.body;
  
  // Check if user already exists
  const existingUser = mockUsers.find(u => u.phone === phone || u.email === email);
  if (existingUser) {
    return res.status(400).json({ success: false, message: 'User already exists' });
  }
  
  // Generate mock OTP
  const otp = Math.floor(100000 + Math.random() * 900000).toString();
  otpStore[phone] = { otp, data: { fullName, email, phone, password }, expires: Date.now() + 5 * 60 * 1000 }; // 5 min
  
  console.log(`Mock OTP for ${phone}: ${otp}`);
  
  res.json({
    success: true,
    message: `OTP sent to ${phone}. Please verify to complete registration.`,
    data: null
  });
});

app.post('/api/auth/verify-registration-otp', (req, res) => {
  const { phone, otp } = req.body;
  
  const stored = otpStore[phone];
  if (!stored || stored.otp !== otp || Date.now() > stored.expires) {
    return res.status(400).json({ success: false, message: 'Invalid or expired OTP' });
  }
  
  // Create user
  const newUser = {
    id: mockUsers.length + 1,
    ...stored.data,
    rating: 0,
    totalRides: 0,
    verified: true,
    avatar: `https://images.unsplash.com/photo-${Math.random()}?w=100&h=100&fit=crop&crop=face`
  };
  
  mockUsers.push(newUser);
  delete otpStore[phone];
  
  res.json({
    success: true,
    message: 'Phone verified! Account created successfully.',
    data: {
      token: 'mock-jwt-token-' + newUser.id,
      fullName: newUser.name,
      email: newUser.email,
      phone: newUser.phone,
      role: 'USER',
      kycStatus: 'PENDING'
    }
  });
});

app.post('/api/auth/login/password', (req, res) => {
  const { phone, password } = req.body;
  const user = mockUsers.find(u => u.phone === phone);
  
  if (user && user.password === password) {
    res.json({
      success: true,
      message: 'Login successful',
      data: {
        token: 'mock-jwt-token-' + user.id,
        fullName: user.name,
        email: user.email,
        phone: user.phone,
        role: 'USER',
        kycStatus: 'PENDING'
      }
    });
  } else {
    res.status(401).json({ success: false, message: 'Invalid credentials' });
  }
});

app.post('/api/auth/login/send-otp', (req, res) => {
  const { phone } = req.body;
  const user = mockUsers.find(u => u.phone === phone);
  
  if (!user) {
    return res.status(404).json({ success: false, message: 'User not found' });
  }
  
  // Generate mock OTP
  const otp = Math.floor(100000 + Math.random() * 900000).toString();
  otpStore[phone] = { otp, expires: Date.now() + 5 * 60 * 1000 };
  
  console.log(`Mock OTP for login ${phone}: ${otp}`);
  
  res.json({
    success: true,
    message: `OTP sent to ${phone}`,
    data: null
  });
});

app.post('/api/auth/login/verify-otp', (req, res) => {
  const { phone, otp } = req.body;
  
  const stored = otpStore[phone];
  if (!stored || stored.otp !== otp || Date.now() > stored.expires) {
    return res.status(400).json({ success: false, message: 'Invalid or expired OTP' });
  }
  
  const user = mockUsers.find(u => u.phone === phone);
  delete otpStore[phone];
  
  res.json({
    success: true,
    message: 'Login successful',
    data: {
      token: 'mock-jwt-token-' + user.id,
      fullName: user.name,
      email: user.email,
      phone: user.phone,
      role: 'USER',
      kycStatus: 'PENDING'
    }
  });
});

app.post('/api/auth/change-password', (req, res) => {
  // Mock implementation
  res.json({
    success: true,
    message: 'Password changed successfully.',
    data: null
  });
});

app.get('/api/users/profile', (req, res) => {
  // Mock profile - in real app, get from token
  const mockProfile = {
    fullName: 'Rahul Sharma',
    email: 'rahul@gmail.com',
    phone: '6876543210',
    memberSince: 'April 2026',
    kycStatus: 'PENDING',
    phoneVerified: true,
    aadhaarVerified: false,
    canPostRides: false,
    totalRidesPosted: 0,
    totalRidesBooked: 0,
    vehicles: [],
    averageRating: 0.0,
    totalReviews: 0,
    recentReviews: []
  };
  res.json({
    success: true,
    message: 'Profile fetched successfully',
    data: mockProfile
  });
});

// Get statistics
app.get('/api/stats', (req, res) => {
  const stats = {
    // Mock display values for the UI until the real backend is integrated.
    // Home.js expects strings like "6.2K+" / "4.2K+".
    totalRides: '4.2K+',
    activeRides: mockRides.filter(r => r.status === 'active').length,
    totalUsers: '6.2K+',
    totalBookings: mockBookings.length,
    averageRating: (mockUsers.reduce((acc, user) => acc + user.rating, 0) / mockUsers.length).toFixed(1),
    fuelSaved: '18L+',
  };
  res.json(stats);
});

// Error handling middleware
app.use((err, req, res, next) => {
  console.error(err.stack);
  res.status(500).json({ error: 'Something went wrong!' });
});

app.listen(PORT, () => {
  console.log(`🚀 BlaBlaBike API server running on port ${PORT}`);
  console.log(`📊 Available endpoints:`);
  console.log(`  GET  /api/rides - Get all rides`);
  console.log(`  GET  /api/rides/:id - Get ride by ID`);
  console.log(`  POST /api/rides - Create new ride`);
  console.log(`  POST /api/bookings - Book a ride`);
  console.log(`  GET  /api/bookings/:userId - Get user bookings`);
  console.log(`  GET  /api/users - Get all users`);
  console.log(`  GET  /api/users/:id - Get user by ID`);
  console.log(`  POST /api/auth/login - User login`);
  console.log(`  POST /api/auth/register - User registration`);
  console.log(`  GET  /api/stats - Get platform statistics`);
});
