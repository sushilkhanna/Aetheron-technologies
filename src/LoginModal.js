import React, { useState, useRef, useEffect, useCallback } from 'react';
import { Mail, Lock, Eye, EyeOff, User, AlertCircle, X, Zap, Loader, Shield } from 'lucide-react';
import authAPI from './authAPI';

const LoginModal = ({ onClose, onSuccess, onSwitchToSignup }) => {
  const [form, setForm] = useState({ phone: '', password: '' });
  const [loading, setLoading] = useState(false);
  const [otpLoading, setOtpLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [showPassword, setShowPassword] = useState(false);

  const [loginMethod, setLoginMethod] = useState('password');
  const [otpSent, setOtpSent] = useState(false);

  const OTP_LENGTH = 6;
  const [otp, setOtp] = useState(Array.from({ length: OTP_LENGTH }, () => ''));
  const otpRefs = [useRef(), useRef(), useRef(), useRef(), useRef(), useRef()];

  const cleanPhone = (phone) => {
    return phone.replace(/\D/g, '').slice(-10);
  };

  const resetModal = useCallback(() => {
    setForm({ phone: '', password: '' });
    setOtp(Array.from({ length: OTP_LENGTH }, () => ''));
    setOtpSent(false);
    setLoginMethod('password');
    setError('');
    setSuccess('');
    setShowPassword(false);
    setLoading(false);
    setOtpLoading(false);
    onClose();
  }, [OTP_LENGTH, onClose]);

  // Handle overlay click
  const handleOverlayClick = (e) => {
    if (e.target === e.currentTarget) {
      resetModal();
    }
  };

  // Handle ESC key
  useEffect(() => {
    const handleEscKey = (e) => {
      if (e.key === 'Escape') {
        resetModal();
      }
    };

    document.addEventListener('keydown', handleEscKey);
    return () => document.removeEventListener('keydown', handleEscKey);
  }, [resetModal]);

  const handleChange = (e) => {
    const { name, value } = e.target;
    
    // Phone input validation - only allow digits
    if (name === 'phone') {
      const digitsOnly = value.replace(/\D/g, '');
      
      // Check if user tried to enter non-digits
      if (value !== digitsOnly && value.length > 0) {
        setError('Phone number can only contain digits (0-9)');
        // Clear error after 2 seconds
        setTimeout(() => setError(''), 2000);
      } else {
        setError('');
      }
      
      setForm({ ...form, [name]: digitsOnly });
    } else {
      setForm({ ...form, [name]: value });
      setError('');
    }
    
    setSuccess('');
  };

  // ---------------- OTP INPUT ----------------
  const handleOtpChange = (index, value) => {
    if (!/^\d*$/.test(value)) return;

    const newOtp = [...otp];
    newOtp[index] = value.slice(-1);
    setOtp(newOtp);

    if (value && index < OTP_LENGTH - 1) {
      otpRefs[index + 1].current?.focus();
    }
  };

  const handleOtpKeyDown = (index, e) => {
    if (e.key === 'Backspace' && !otp[index] && index > 0) {
      otpRefs[index - 1].current?.focus();
    }
  };

  // ---------------- SEND OTP ----------------
  const handleSendOtp = async () => {
    const phone = cleanPhone(form.phone);

    // Strict phone validation for OTP login
    if (phone.length !== 10) {
      setError('Please enter a valid 10-digit phone number');
      return;
    }

    // Additional validation: ensure input contains only digits
    const phoneDigitsOnly = form.phone.replace(/\D/g, '');
    if (phoneDigitsOnly.length !== 10) {
      setError('Phone number must contain exactly 10 digits');
      return;
    }

    setOtpLoading(true);
    setError('');
    setSuccess('');

    try {
      const resp = await authAPI.loginSendOtp(phone);

      if (resp) {
        setOtpSent(true);
        setSuccess('OTP sent!');
        setTimeout(() => otpRefs[0].current?.focus(), 100);
      } else {
        setError('Failed to send OTP');
      }
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || 'Backend validation failed');
    } finally {
      setOtpLoading(false);
    }
  };

  // ---------------- VERIFY OTP ----------------
  const handleVerifyOtp = async (e) => {
    e.preventDefault();

    const phone = cleanPhone(form.phone);
    const otpStr = otp.join('');


    if (phone.length !== 10) {
      setError('Invalid phone number');
      return;
    }

    if (otpStr.length !== 6) {
      setError('Enter complete OTP');
      return;
    }

    setLoading(true);

    try {
      const resp = await authAPI.loginVerifyOtp({ phone: phone, otp: otpStr });


      if (resp?.success && resp?.data?.token) {
        const authData = resp.data;

        localStorage.setItem('token', authData.token);
        localStorage.setItem('user', JSON.stringify(authData));

        // Check if user is admin and redirect accordingly
        if (authData.role === 'ADMIN') {
          localStorage.setItem('adminToken', authData.token);
          localStorage.setItem('adminUser', JSON.stringify(authData));
          setSuccess('Admin login successful!');
          setTimeout(() => {
            window.location.href = '/admin/dashboard';
          }, 1000);
          return;
        }

        setSuccess('Login successful!');

        setTimeout(() => onSuccess(authData), 1000);
        return;
      }

      setError(resp?.message || 'Invalid OTP');
    } catch (err) {
      setError(err?.response?.data?.message || 'Validation failed');
    } finally {
      setLoading(false);
    }
  };

  // ---------------- PASSWORD LOGIN ----------------
  const handleSubmit = async (e) => {
    e.preventDefault();

    const phone = cleanPhone(form.phone);

    if (phone.length !== 10 || !form.password) {
      setError('Fill all fields correctly');
      return;
    }

    // DEMO ADMIN MODE - Frontend only (for testing)
    if (phone === '9999999999' && form.password === 'Admin@123') {
      const demoAdminData = {
        id: 999,
        fullName: 'Demo Admin',
        email: 'admin@bikepooling.com',
        phone: '9999999999',
        role: 'ADMIN',
        token: 'demo-admin-token-' + Date.now()
      };

      localStorage.setItem('token', demoAdminData.token);
      localStorage.setItem('adminToken', demoAdminData.token);
      localStorage.setItem('user', JSON.stringify(demoAdminData));
      localStorage.setItem('adminUser', JSON.stringify(demoAdminData));

      setSuccess('Demo Admin login successful!');
      setTimeout(() => {
        window.location.href = '/admin/dashboard';
      }, 1000);
      return;
    }

    setLoading(true);

    try {
      const resp = await authAPI.login({
        phone,
        password: form.password,
      });

      if (resp?.success && resp?.data?.token) {
        const authData = resp.data;

        localStorage.setItem('token', authData.token);
        localStorage.setItem('user', JSON.stringify(authData));

        // Check if user is admin and redirect accordingly
        if (authData.role === 'ADMIN') {
          localStorage.setItem('adminToken', authData.token);
          localStorage.setItem('adminUser', JSON.stringify(authData));
          setSuccess('Admin login successful!');
          setTimeout(() => {
            window.location.href = '/admin/dashboard';
          }, 1000);
          return;
        }

        setSuccess('Login successful!');
        setTimeout(() => onSuccess(authData), 1000);
        return;
      }

      setError(resp?.message || 'Login failed');
    } catch (err) {
      setError(err?.response?.data?.message || 'Login failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div 
      className="fixed inset-0 z-[99999] flex items-center justify-center p-4 sm:p-6 lg:p-8"
      style={{ 
        background: 'rgba(0,0,0,0.95)', 
        backdropFilter: 'blur(12px)',
        WebkitBackdropFilter: 'blur(12px)', // Safari mobile support
        position: 'fixed',
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        width: '100vw',
        height: '100vh',
        touchAction: 'none',
        overscrollBehavior: 'contain'
      }}
      onClick={handleOverlayClick}
    >

      <div 
        className="w-full max-w-md sm:max-w-lg lg:max-w-xl rounded-2xl overflow-hidden bg-[#12121a] border border-gray-700 shadow-2xl relative transform transition-all scale-100"
        style={{
          position: 'relative',
          maxHeight: '90vh',
          overflowY: 'auto',
          touchAction: 'pan-y',
          WebkitOverflowScrolling: 'touch',
          margin: '0 auto'
        }}
        onClick={(e) => e.stopPropagation()}
      >
        
        {/* Header */}
        <div className="relative p-6 sm:p-8 border-b border-gray-700">
          <button 
            onClick={resetModal} 
            className="absolute right-6 top-6 text-gray-400 hover:text-white transition-colors p-2 rounded-lg hover:bg-gray-800 z-10"
          >
            <X size={20} />
          </button>
          
          <div className="text-center">
            <div className="inline-flex items-center justify-center w-12 h-12 bg-gradient-to-r from-orange-500 to-orange-600 rounded-full mb-4">
              <Zap size={24} className="text-white" />
            </div>
            <h2 className="text-2xl sm:text-3xl font-bold text-white mb-2">Welcome Back</h2>
            <p className="text-gray-400 text-sm">Login to your BikePooling account</p>
          </div>
        </div>

        {/* Body */}
        <div className="p-6 sm:p-8">
          {/* Login Method Tabs */}
          {!otpSent && (
            <div className="flex gap-2 p-1 bg-gray-800 rounded-lg mb-6">
              <button
                type="button"
                onClick={() => setLoginMethod('password')}
                className={`flex-1 py-2 px-4 rounded-md text-sm font-medium transition-all ${
                  loginMethod === 'password'
                    ? 'bg-orange-500 text-white shadow-lg'
                    : 'text-gray-400 hover:text-white hover:bg-gray-700'
                }`}
              >
                Password
              </button>
              <button
                type="button"
                onClick={() => setLoginMethod('otp')}
                className={`flex-1 py-2 px-4 rounded-md text-sm font-medium transition-all ${
                  loginMethod === 'otp'
                    ? 'bg-orange-500 text-white shadow-lg'
                    : 'text-gray-400 hover:text-white hover:bg-gray-700'
                }`}
              >
                OTP
              </button>
            </div>
          )}

          {/* Alerts */}
          {error && (
            <div className="mb-4 p-3 rounded-lg text-sm text-red-300 bg-red-900/20 border border-red-800/50">
              {error}
            </div>
          )}
          {success && (
            <div className="mb-4 p-3 rounded-lg text-sm text-green-300 bg-green-900/20 border border-green-800/50">
              {success}
            </div>
          )}

          {/* ---------------- OTP SCREEN ---------------- */}
          {otpSent ? (
            <form onSubmit={handleVerifyOtp} className="space-y-6">
              <div>
                <label className="block text-sm font-medium text-gray-400 mb-2">Phone Number</label>
                <input 
                  value={form.phone} 
                  disabled 
                  className="w-full px-4 py-3 bg-gray-800 border border-gray-600 rounded-lg text-gray-300 cursor-not-allowed" 
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-400 mb-4 text-center">Enter 6-Digit OTP</label>
                <div className="flex gap-2 sm:gap-3 justify-center">
                  {otp.map((d, i) => (
                    <input
                      key={i}
                      ref={otpRefs[i]}
                      maxLength={1}
                      value={d}
                      onChange={(e) => handleOtpChange(i, e.target.value)}
                      onKeyDown={(e) => handleOtpKeyDown(i, e)}
                      className="w-12 h-12 sm:w-14 sm:h-14 text-center text-lg font-semibold bg-gray-800 border border-gray-600 rounded-lg text-white focus:border-orange-500 focus:outline-none focus:ring-2 focus:ring-orange-500/20 transition-all"
                    />
                  ))}
                </div>
              </div>

              <button 
                type="submit" 
                disabled={loading}
                className="w-full py-3 px-4 bg-gradient-to-r from-orange-500 to-orange-600 text-white rounded-lg font-medium hover:from-orange-600 hover:to-orange-700 disabled:opacity-50 disabled:cursor-not-allowed transition-all flex items-center justify-center gap-2"
              >
                {loading && <Loader size={16} className="animate-spin" />}
                {loading ? 'Verifying...' : 'Verify OTP'}
              </button>

              <div className="text-center">
                <button
                  type="button"
                  onClick={handleSendOtp}
                  disabled={otpLoading}
                  className="text-sm text-orange-400 hover:text-orange-300 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                >
                  {otpLoading ? 'Sending...' : 'Resend OTP'}
                </button>
              </div>
            </form>

          ) : (

            /* ---------------- LOGIN SCREEN ---------------- */
            <form onSubmit={handleSubmit} className="space-y-6">
              <div>
                <label className="block text-sm font-medium text-gray-400 mb-2">Phone Number</label>
                <input
                  type="tel"
                  name="phone"
                  value={form.phone}
                  onChange={handleChange}
                  placeholder="Enter 10-digit phone number"
                  className="w-full px-4 py-3 bg-gray-800 border border-gray-600 rounded-lg text-white placeholder-gray-400 focus:border-orange-500 focus:outline-none focus:ring-2 focus:ring-orange-500/20 transition-all"
                  required
                />
              </div>

              {loginMethod === 'password' && (
                <div>
                  <label className="block text-sm font-medium text-gray-400 mb-2">Password</label>
                  <div className="relative">
                    <input
                      type={showPassword ? 'text' : 'password'}
                      name="password"
                      value={form.password}
                      onChange={handleChange}
                      placeholder="Enter your password"
                      className="w-full px-4 py-3 bg-gray-800 border border-gray-600 rounded-lg text-white placeholder-gray-400 focus:border-orange-500 focus:outline-none focus:ring-2 focus:ring-orange-500/20 transition-all pr-12"
                      required
                    />
                    <button
                      type="button"
                      onClick={() => setShowPassword(!showPassword)}
                      className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-300 transition-colors"
                    >
                      {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                    </button>
                  </div>
                </div>
              )}

              {/* Submit Button */}
              {loginMethod === 'password' ? (
                <button
                  type="submit"
                  disabled={loading}
                  className="w-full py-3 px-4 bg-gradient-to-r from-orange-500 to-orange-600 text-white rounded-lg font-medium hover:from-orange-600 hover:to-orange-700 disabled:opacity-50 disabled:cursor-not-allowed transition-all flex items-center justify-center gap-2"
                >
                  {loading && <Loader size={16} className="animate-spin" />}
                  {loading ? 'Signing in...' : 'Login'}
                </button>
              ) : (
                <button
                  type="button"
                  onClick={handleSendOtp}
                  disabled={otpLoading}
                  className="w-full py-3 px-4 bg-gradient-to-r from-orange-500 to-orange-600 text-white rounded-lg font-medium hover:from-orange-600 hover:to-orange-700 disabled:opacity-50 disabled:cursor-not-allowed transition-all flex items-center justify-center gap-2"
                >
                  {otpLoading && <Loader size={16} className="animate-spin" />}
                  {otpLoading ? 'Sending...' : 'Send OTP'}
                </button>
              )}
            </form>
          )}

          {/* Footer */}
          <div className="mt-6 pt-6 border-t border-gray-700 text-center">
            <p className="text-sm text-gray-400 mb-3">
              Don't have an account?{' '}
              <button 
                onClick={onSwitchToSignup} 
                className="text-orange-400 hover:text-orange-300 font-medium transition-colors"
              >
                Sign up free
              </button>
            </p>
            <div className="flex items-center justify-center gap-2 text-xs text-gray-500">
              <Shield size={12} />
              <span>Admin Access: Use Phone 9999999999, Password Admin@123</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default LoginModal;