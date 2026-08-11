import React, { useEffect } from 'react';
import { Routes, Route, Navigate, useNavigate, useLocation } from 'react-router-dom';
import Navbar from './components/Navbar';
import Footer from './components/Footer';
import Home from './pages/Home';
import PrivacyPolicy from './pages/PrivacyPolicy';
import TermsConditions from './pages/TermsConditions';
import RefundPolicy from './pages/RefundPolicy';

// Admin Panel
import AdminLogin from './pages/admin/AdminLogin';
import AdminDashboard from './pages/admin/AdminDashboard';
import UserManagement from './pages/admin/UserManagement';
import DriverKYC from './pages/admin/DriverKYC';
import RideManagement from './pages/admin/RideManagement';
import SosManagement from './pages/admin/SosManagement';

const App = () => {
  const navigate = useNavigate();
  const location = useLocation();

  // Auto-redirect admin users to the admin dashboard on page load / refresh
  useEffect(() => {
    // Only redirect if we're on a non-admin route
    if (location.pathname.startsWith('/admin')) return;

    try {
      const adminToken = localStorage.getItem('adminToken');
      const adminData  = localStorage.getItem('adminUser');

      if (adminToken && adminData) {
        JSON.parse(adminData); // validate JSON
        // Backend API secures admin endpoints — no frontend role check needed
        navigate('/admin/dashboard', { replace: true });
      }
    } catch {
      // Corrupted admin data — clean up silently
      localStorage.removeItem('adminToken');
      localStorage.removeItem('adminUser');
    }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []); // run once on mount

  return (
    <>
      <Routes>
        {/* Public Landing Page */}
        <Route path="/" element={
          <>
            <Navbar />
            <Home />
            <Footer />
          </>
        } />

        {/* Legal Pages */}
        <Route path="/privacy-policy" element={
          <>
            <Navbar />
            <PrivacyPolicy />
            <Footer />
          </>
        } />
        <Route path="/terms-and-conditions" element={
          <>
            <Navbar />
            <TermsConditions />
            <Footer />
          </>
        } />
        <Route path="/refund-policy" element={
          <>
            <Navbar />
            <RefundPolicy />
            <Footer />
          </>
        } />

        {/* Admin Login */}
        <Route path="/admin/login" element={<AdminLogin />} />

        {/* Admin Panel Routes - No Navbar or Footer */}
        <Route path="/admin/dashboard" element={<AdminDashboard />} />
        <Route path="/admin/users" element={<UserManagement />} />
        <Route path="/admin/driver-kyc" element={<DriverKYC />} />
        <Route path="/admin/rides" element={<RideManagement />} />
        <Route path="/admin/sos" element={<SosManagement />} />

        {/* Catch-all: redirect unknown routes to home */}
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </>
  );
};

export default App;
