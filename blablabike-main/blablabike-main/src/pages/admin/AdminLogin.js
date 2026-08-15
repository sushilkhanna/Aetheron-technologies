import React, { useState, useRef, useEffect, useCallback } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { Zap, Loader, Shield, Phone, Lock, ArrowLeft } from 'lucide-react';
import authAPI from '../../authAPI';

const AdminLogin = () => {
  const navigate = useNavigate();
  const [form, setForm] = useState({ phone: '' });
  const [loading, setLoading] = useState(false);
  const [otpLoading, setOtpLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [otpSent, setOtpSent] = useState(false);

  const OTP_LENGTH = 4;
  const [otp, setOtp] = useState(Array.from({ length: OTP_LENGTH }, () => ''));
  const otpRefs = [useRef(), useRef(), useRef(), useRef()];

  // If already logged in, redirect to dashboard
  useEffect(() => {
    const adminToken = localStorage.getItem('adminToken');
    const adminData = localStorage.getItem('adminUser');
    if (adminToken && adminData) {
      try {
        JSON.parse(adminData); // validate JSON
        navigate('/admin/dashboard', { replace: true });
      } catch {
        localStorage.removeItem('adminToken');
        localStorage.removeItem('adminUser');
      }
    }
  }, [navigate]);

  const cleanPhone = (phone) => phone.replace(/\D/g, '').slice(-10);

  const handleChange = (e) => {
    const { name, value } = e.target;
    if (name === 'phone') {
      const digitsOnly = value.replace(/\D/g, '');
      if (value !== digitsOnly && value.length > 0) {
        setError('Phone number can only contain digits (0-9)');
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

  // OTP input handlers
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

  // Send OTP
  const handleSendOtp = async () => {
    const phone = cleanPhone(form.phone);
    if (phone.length !== 10) {
      setError('Please enter a valid 10-digit phone number');
      return;
    }

    setOtpLoading(true);
    setError('');
    setSuccess('');

    try {
      const resp = await authAPI.adminLoginSendOtp(phone);
      if (resp) {
        setOtpSent(true);
        setSuccess('OTP sent!');
        setTimeout(() => otpRefs[0].current?.focus(), 100);
      } else {
        setError('Failed to send OTP');
      }
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || 'Failed to send OTP. Make sure your phone is registered as an admin.');
    } finally {
      setOtpLoading(false);
    }
  };

  // Verify OTP
  const handleVerifyOtp = async (e) => {
    e.preventDefault();
    const phone = cleanPhone(form.phone);
    const otpStr = otp.join('');

    if (phone.length !== 10) {
      setError('Invalid phone number');
      return;
    }
    if (otpStr.length !== OTP_LENGTH) {
      setError('Enter complete OTP');
      return;
    }

    setLoading(true);
    setError('');

    try {
      const resp = await authAPI.adminLoginVerifyOtp({ phone, otp: otpStr });

      if (resp?.success && resp?.data?.token) {
        const authData = resp.data;

        // Backend API secures admin endpoints — no frontend role check needed

        // Store admin session
        localStorage.setItem('adminToken', authData.token);
        localStorage.setItem('adminUser', JSON.stringify(authData));
        localStorage.setItem('token', authData.token);
        localStorage.setItem('user', JSON.stringify(authData));

        setSuccess('Admin login successful!');
        setTimeout(() => {
          navigate('/admin/dashboard', { replace: true });
        }, 800);
        return;
      }

      setError(resp?.message || 'Invalid OTP');
    } catch (err) {
      setError(err?.response?.data?.message || 'Verification failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen mesh-bg flex items-center justify-center px-4 py-12">
      {/* Background decorative elements */}
      <div className="absolute inset-0 overflow-hidden pointer-events-none">
        <div className="absolute -top-40 -right-40 w-80 h-80 rounded-full opacity-10"
          style={{ background: 'radial-gradient(circle, #FF7000, transparent 70%)' }} />
        <div className="absolute -bottom-40 -left-40 w-80 h-80 rounded-full opacity-10"
          style={{ background: 'radial-gradient(circle, #FF7000, transparent 70%)' }} />
      </div>

      <div className="w-full max-w-md relative z-10">
        {/* Back to home link */}
        <Link
          to="/"
          className="inline-flex items-center gap-2 text-gray-400 hover:text-white text-sm mb-8 transition-colors group"
        >
          <ArrowLeft size={16} className="group-hover:-translate-x-1 transition-transform" />
          Back to Home
        </Link>

        {/* Login card */}
        <div
          className="rounded-2xl overflow-hidden"
          style={{
            background: 'linear-gradient(135deg, #12121a, #1a1a2e)',
            border: '1px solid rgba(255,255,255,0.1)',
            boxShadow: '0 25px 50px rgba(0,0,0,0.5), 0 0 80px rgba(255,112,0,0.05)'
          }}
        >
          {/* Top accent bar */}
          <div className="h-1 w-full" style={{ background: 'linear-gradient(90deg, #FF7000, #ff9a3c)' }} />

          {/* Header */}
          <div className="p-8 pb-0 text-center">
            <div className="inline-flex items-center justify-center w-16 h-16 rounded-2xl mb-5"
              style={{ background: 'linear-gradient(135deg, #FF7000, #ff9a3c)', boxShadow: '0 8px 25px rgba(255,112,0,0.3)' }}>
              <Lock size={28} className="text-white" />
            </div>
            <h1 className="text-2xl font-bold text-white mb-1">Admin Portal</h1>
            <p className="text-gray-400 text-sm">Authorized personnel only</p>
          </div>

          {/* Body */}
          <div className="p-8 pt-6">
            {/* Alerts */}
            {error && (
              <div className="mb-5 p-3.5 rounded-xl text-sm text-red-300 flex items-start gap-2"
                style={{ background: 'rgba(239,68,68,0.1)', border: '1px solid rgba(239,68,68,0.2)' }}>
                <Shield size={16} className="mt-0.5 flex-shrink-0" />
                <span>{error}</span>
              </div>
            )}
            {success && (
              <div className="mb-5 p-3.5 rounded-xl text-sm text-green-300"
                style={{ background: 'rgba(34,197,94,0.1)', border: '1px solid rgba(34,197,94,0.2)' }}>
                {success}
              </div>
            )}

            {/* OTP verification screen */}
            {otpSent ? (
              <form onSubmit={handleVerifyOtp} className="space-y-6">
                <div>
                  <label className="block text-xs font-medium text-gray-400 mb-2 uppercase tracking-wide">Phone Number</label>
                  <div className="relative flex items-center">
                    <div className="absolute left-3.5 flex items-center gap-1.5 text-gray-400 text-sm font-bold select-none border-r border-gray-700/60 pr-3 z-10">
                      <span className="text-base">🇮🇳</span>
                      <span>+91</span>
                    </div>
                    <input
                      value={form.phone}
                      disabled
                      className="w-full py-3.5 pl-24 pr-4 rounded-xl bg-gray-800/40 border border-gray-700/60 text-gray-300 font-medium cursor-not-allowed text-sm"
                    />
                  </div>
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-400 mb-4 text-center">Enter 4-Digit OTP</label>
                  <div className="flex gap-3 justify-center">
                    {otp.map((d, i) => (
                      <input
                        key={i}
                        ref={otpRefs[i]}
                        maxLength={1}
                        value={d}
                        onChange={(e) => handleOtpChange(i, e.target.value)}
                        onKeyDown={(e) => handleOtpKeyDown(i, e)}
                        className="otp-input"
                      />
                    ))}
                  </div>
                </div>

                <button
                  type="submit"
                  disabled={loading}
                  className="btn-primary w-full py-3.5 text-sm disabled:opacity-50 flex items-center justify-center gap-2"
                >
                  {loading && <Loader size={16} className="animate-spin" />}
                  <span>{loading ? 'Verifying...' : 'Verify & Login'}</span>
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
              /* Phone input screen */
              <form onSubmit={(e) => { e.preventDefault(); handleSendOtp(); }} className="space-y-6">
                <div>
                  <label className="block text-xs font-medium text-gray-400 mb-2 uppercase tracking-wide">Phone Number</label>
                  <div className="relative flex items-center">
                    <div className="absolute left-3.5 flex items-center gap-1.5 text-gray-300 text-sm font-bold select-none border-r border-gray-700 pr-3 z-10">
                      <span className="text-base">🇮🇳</span>
                      <span>+91</span>
                    </div>
                    <input
                      type="tel"
                      name="phone"
                      value={form.phone}
                      onChange={handleChange}
                      placeholder="Enter 10-digit phone number"
                      maxLength={10}
                      className="w-full py-3.5 pl-24 pr-4 rounded-xl bg-gray-800/80 border border-gray-700 text-white placeholder-gray-400 focus:outline-none focus:border-orange-500 focus:ring-1 focus:ring-orange-500 text-sm font-medium transition-all"
                      required
                    />
                  </div>
                </div>

                <button
                  type="submit"
                  disabled={otpLoading}
                  className="btn-primary w-full py-3.5 text-sm disabled:opacity-50 flex items-center justify-center gap-2"
                >
                  {otpLoading && <Loader size={16} className="animate-spin" />}
                  <span>{otpLoading ? 'Sending OTP...' : 'Send OTP'}</span>
                </button>
              </form>
            )}

            {/* Footer */}
            <div className="mt-8 pt-6 text-center" style={{ borderTop: '1px solid rgba(255,255,255,0.06)' }}>
              <div className="flex items-center justify-center gap-2 text-xs text-gray-500">
                <Shield size={12} />
                <span>Secured with OTP verification · Admin access only</span>
              </div>
            </div>
          </div>
        </div>

        {/* Branding below card */}
        <div className="mt-8 text-center">
          <div className="inline-flex items-center gap-2">
            <div className="w-6 h-6 rounded-md flex items-center justify-center"
              style={{ background: 'linear-gradient(135deg, #FF7000, #ff9a3c)' }}>
              <Zap size={12} className="text-white" fill="white" />
            </div>
            <span className="text-sm font-semibold">
              <span className="text-white">Bike</span>
              <span className="gradient-text">Pool</span>
              <span className="text-white">ing</span>
            </span>
          </div>
        </div>
      </div>
    </div>
  );
};

export default AdminLogin;
