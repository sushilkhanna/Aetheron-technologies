import axios from 'axios';
import getApiConfig from './config/api';

const apiConfig = getApiConfig();

const api = axios.create({
  baseURL: apiConfig.baseURL,
  timeout: apiConfig.timeout,
  headers: {
    'Content-Type': 'application/json'
  }
});

api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

api.interceptors.response.use(
  (response) => {
    return response;
  },
  (error) => {
    if (error.response?.status === 401) {
      // Token expired or invalid - clear localStorage and redirect to login
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      window.location.href = '/';
    }
    return Promise.reject(error);
  }
);

const publicApi = axios.create({
  baseURL: apiConfig.baseURL,
  timeout: apiConfig.timeout,
  headers: {
    'Content-Type': 'application/json'
  }
});

const authAPI = {
  // Registration (sends OTP; OTP verification completes account creation)
  register: async (userData) => {
    const res = await publicApi.post('/auth/register', userData);
    return res.data;
  },

  // Registration OTP verification (creates account + returns token/profile)
  verifyRegistrationOTP: async (phone, otp) => {
    const res = await publicApi.post('/auth/verify-registration-otp', { phone, otp });
    return res.data;
  },

  // Login with phone + password
  login: async ({ phone, password }) => {
    const requestData = { phone: String(phone), password: String(password) };
    
    try {
      const res = await publicApi.post('/auth/login/password', requestData);
      return res.data;
    } catch (err) {
      throw err;
    }
  },

  // Login with phone + OTP
  loginSendOtp: async (phone) => {
    const requestData = { phone: String(phone) };
    
    try {
      const res = await publicApi.post('/auth/login/send-otp', requestData);
      return res.data;
    } catch (err) {
      throw err;
    }
  },

  loginVerifyOtp: async ({ phone, otp }) => {
    try {
      const requestData = { phone: String(phone), otp: String(otp) };
      const res = await publicApi.post('/auth/login/verify-otp', requestData);
      return res.data;
    } catch (err) {
      throw err;
    }
  },

  // Get user profile
  getProfile: async () => {
    try {
      const res = await api.get('/users/profile');
      return res.data;
    } catch (err) {
      throw err;
    }
  },

  // Update user profile
  updateProfile: async (profileData) => {
    try {
      const res = await api.put('/users/profile', profileData);
      return res.data;
    } catch (err) {
      throw err;
    }
  },

  // Get user posts
  getUserPosts: async () => {
    try {
      const res = await api.get('/users/posts');
      return res.data;
    } catch (err) {
      throw err;
    }
  },

  // Get user ratings
  getUserRatings: async () => {
    try {
      const res = await api.get('/users/ratings');
      return res.data;
    } catch (err) {
      throw err;
    }
  },

  // Get user rides
  getUserRides: async () => {
    try {
      const res = await api.get('/users/rides');
      return res.data;
    } catch (err) {
      throw err;
    }
  },

  // Get user earnings
  getUserEarnings: async () => {
    try {
      const res = await api.get('/users/earnings');
      return res.data;
    } catch (err) {
      throw err;
    }
  },

  // Change password
  changePassword: async (passwordData) => {
    try {
      const res = await api.post('/auth/change-password', passwordData);
      return res.data;
    } catch (err) {
      throw err;
    }
  },

  // DigiLocker APIs
  initiateDigiLocker: async () => {
    try {
      const res = await api.post('/digilocker/initiate');
      return res.data;
    } catch (err) {
      throw err;
    }
  },

  getDigiLockerStatus: async () => {
    try {
      const res = await api.get('/digilocker/status');
      return res.data;
    } catch (err) {
      throw err;
    }
  },

  // Get KYC status - dedicated endpoint for real-time status
  getKycStatus: async () => {
    try {
      // Try dedicated KYC status endpoint first
      const res = await api.get('/kyc/status');
      return res.data;
    } catch (error) {
      // Fallback to profile endpoint if dedicated endpoint doesn't exist
      try {
        console.log('KYC status endpoint not found, using profile endpoint as fallback');
        const profileRes = await api.get('/users/profile');
        return {
          success: true,
          data: {
            status: profileRes.data.kycStatus || 'PENDING'
          }
        };
      } catch (profileError) {
        throw profileError;
      }
    }
  },

  submitDocuments: async (documentData) => {
    try {
      const res = await api.post('/digilocker/submit-documents', documentData);
      return res.data;
    } catch (err) {
      throw err;
    }
  },

  // Vehicle APIs
  addVehicle: async (vehicleData) => {
    try {
      const res = await api.post('/vehicles', vehicleData);
      return res.data;
    } catch (err) {
      throw err;
    }
  },

  updateVehicle: async (vehicleId, vehicleData) => {
    try {
      const res = await api.put(`/vehicles/${vehicleId}`, vehicleData);
      return res.data;
    } catch (err) {
      throw err;
    }
  },

  deleteVehicle: async (vehicleId) => {
    try {
      const res = await api.delete(`/vehicles/${vehicleId}`);
      return res.data;
    } catch (err) {
      throw err;
    }
  },

  getMyVehicles: async () => {
    try {
      const res = await api.get('/vehicles/my');
      return res.data;
    } catch (err) {
      throw err;
    }
  },

  deactivateVehicle: async (vehicleId) => {
    try {
      const res = await api.patch(`/vehicles/${vehicleId}/deactivate`);
      return res.data;
    } catch (err) {
      throw err;
    }
  },

  activateVehicle: async (vehicleId) => {
    try {
      const res = await api.patch(`/vehicles/${vehicleId}/activate`);
      return res.data;
    } catch (err) {
      throw err;
    }
  },

  // Rides APIs
  getRides: async () => {
    try {
      const res = await api.get('/rides');
      return res.data;
    } catch (err) {
      throw err;
    }
  },

  getRide: async (rideId) => {
    try {
      const res = await api.get(`/rides/${rideId}`);
      return res.data;
    } catch (err) {
      throw err;
    }
  },

  createRide: async (rideData) => {
    try {
      const res = await api.post('/rides', rideData);
      return res.data;
    } catch (err) {
      throw err;
    }
  },

  // Stats API
  getStats: async () => {
    try {
      const res = await api.get('/stats');
      return res.data;
    } catch (err) {
      throw err;
    }
  }
};

export default authAPI;
