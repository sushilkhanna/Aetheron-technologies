import React, { useState, useEffect } from 'react';
import { Routes, Route, Navigate, useNavigate, useLocation } from 'react-router-dom';
import Navbar from './components/Navbar';
import Footer from './components/Footer';
import Home from './pages/Home';
import MyProfile from './pages/MyProfile';
import LoginModal from './LoginModal';
import SignupModal from './SignupModal';
import ProfileSection from './components/ProfileSection';
import Notification from './components/Notification';
// Admin Panel
import AdminDashboard from './pages/admin/AdminDashboard';
import UserManagement from './pages/admin/UserManagement';
import DriverKYC from './pages/admin/DriverKYC';
import RideManagement from './pages/admin/RideManagement';

const App = () => {
  const navigate = useNavigate();
  const location = useLocation();

  const [user, setUser] = useState(() => {
    // Initialize user state from localStorage to prevent logout on refresh
    try {
      const storedUser = localStorage.getItem('user');
      const storedToken = localStorage.getItem('token');
      
      // Only restore user if both user and token exist
      if (storedUser && storedToken) {
        return JSON.parse(storedUser);
      }
      return null;
    } catch (error) {
      // Clear corrupted data
      localStorage.removeItem('user');
      localStorage.removeItem('token');
      return null;
    }
  });
  const [showLogin, setShowLogin] = useState(false);
  const [showSignup, setShowSignup] = useState(false);
  const [showProfile, setShowProfile] = useState(false);
  const [notification, setNotification] = useState(null);

  const handleAuthSuccess = (userData) => {
    setUser(userData);
    setShowLogin(false);
    setShowSignup(false);
    
    // Show success notification
    setNotification({
      message: `Welcome back, ${userData.fullName}!`,
      type: 'success'
    });
  };

  const handleSignupSuccess = (userData) => {
    setUser(userData);
    setShowLogin(false);
    setShowSignup(false);
    
    // Show success notification
    setNotification({
      message: `Welcome to BikePooling, ${userData.fullName}!`,
      type: 'success'
    });
  };

  const handleLogout = () => {
    // Show confirmation
    if (window.confirm('Are you sure you want to logout?')) {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      setUser(null);
      
      // Navigate to home page after logout
      navigate('/');
      
      // Show success notification
      setNotification({
        message: 'Logged out successfully!',
        type: 'success'
      });
    }
  };

  const handleUserUpdate = (updated) => setUser(updated);

  // Sync user state with localStorage
  useEffect(() => {
    if (user) {
      localStorage.setItem('user', JSON.stringify(user));
    } else {
      localStorage.removeItem('user');
    }
  }, [user]);

  const navProps = {
    user,
    onLoginClick: () => setShowLogin(true),
    onSignupClick: () => setShowSignup(true),
    onLogout: handleLogout,
    onProfileClick: () => setShowProfile(true),
  };

  return (
    <>
      {showLogin && (
        <LoginModal
          onClose={() => setShowLogin(false)}
          onSuccess={handleAuthSuccess}
          onSwitchToSignup={() => { setShowLogin(false); setShowSignup(true); }}
        />
      )}
      {showSignup && (
        <SignupModal
          onClose={() => setShowSignup(false)}
          onSuccess={handleSignupSuccess}
          onSwitchToLogin={() => { setShowSignup(false); setShowLogin(true); }}
        />
      )}

      <Routes>
        {/* Regular App Routes with Navbar and Footer */}
        <Route path="/" element={
          <>
            <Navbar {...navProps} />
            <Home
              user={user}
              onLoginClick={() => setShowLogin(true)}
              onSignupClick={() => setShowSignup(true)}
              onProfileUpdate={handleUserUpdate}
            />
            <Footer />
          </>
        } />
        <Route path="/profile" element={
          <>
            <Navbar {...navProps} />
            <MyProfile
              user={user}
              onUpdateUser={handleUserUpdate}
            />
            <Footer />
          </>
        } />
        
        {/* Admin Panel Routes - No Navbar or Footer */}
        <Route path="/admin/dashboard" element={<AdminDashboard />} />
        <Route path="/admin/users" element={<UserManagement />} />
        <Route path="/admin/driver-kyc" element={<DriverKYC />} />
        <Route path="/admin/rides" element={<RideManagement />} />
      </Routes>

      {/* Profile Section */}
      {showProfile && user && (
        <ProfileSection
          user={user}
          onUpdateProfile={handleUserUpdate}
          onClose={() => setShowProfile(false)}
        />
      )}

      {/* Notification */}
      {notification && (
        <Notification
          message={notification.message}
          type={notification.type}
          onClose={() => setNotification(null)}
        />
      )}
    </>
  );
};

export default App;
