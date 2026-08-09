import React, { useState, useRef, useEffect, useCallback } from 'react';
import { X, Loader, CheckCircle, Zap, Phone } from 'lucide-react';
import authAPI from './authAPI';

const SignupModal = ({ onClose, onSuccess, onSwitchToLogin }) => {
  const [step, setStep] = useState(1);
  const [form, setForm] = useState({ fullName: '', phone: '' });
  // Backend returns 4-digit OTPs
  const OTP_LENGTH = 4;
  const [otp, setOtp] = useState(Array.from({ length: OTP_LENGTH }, () => ''));
  const [loading, setLoading] = useState(false);
  const [otpLoading, setOtpLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  // Explicit refs to keep hook order stable across renders.
  const otpRefs = [useRef(), useRef(), useRef(), useRef()];
  const cleanPhone = (phone) => {
    return phone.replace(/\D/g, '').slice(-10);
  };

  const resetModal = useCallback(() => {
    setStep(1);
    setForm({ fullName: '', phone: '' });
    setOtp(Array.from({ length: OTP_LENGTH }, () => ''));
    setLoading(false);
    setOtpLoading(false);
    setError('');
    setSuccess('');
    if (typeof onClose === 'function') {
      onClose();
    }
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

  const handleOtpChange = (index, value) => {
    if (!/^\d*$/.test(value)) return;
    const newOtp = [...otp];
    newOtp[index] = value.slice(-1);
    setOtp(newOtp);
    setError('');
    if (value && index < OTP_LENGTH - 1) otpRefs[index + 1].current?.focus();
  };

  const handleOtpKeyDown = (index, e) => {
    if (e.key === 'Backspace' && !otp[index] && index > 0) otpRefs[index - 1].current?.focus();
  };

  
  const handleRegister = async (e) => {
    e.preventDefault();
    const { fullName, phone } = form;
    if (!fullName || !phone) { setError('Please fill in all fields.'); return; }
    const cleanedPhone = cleanPhone(phone);
    if (cleanedPhone.length !== 10) { setError('Phone must be valid 10-digit number.'); return; }
    setLoading(true);
    try {
      const resp = await authAPI.register({ fullName, phone: cleanedPhone });
      if (!resp?.success) {
        setError(resp?.message || 'Registration failed. Try again.');
        setLoading(false);
        return;
      }

      // Step 2: registration OTP verification
      setSuccess('OTP sent to your phone.');
      setOtpLoading(true);
      setStep(2);
      setTimeout(() => otpRefs[0].current?.focus(), 100);
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || 'Registration failed. Try again.');
    } finally { setLoading(false); setOtpLoading(false); }
  };

  const handleResendOTP = async () => {
    setError('');
    setOtpLoading(true);
    try {
      const { fullName, phone } = form;
      const cleanedPhone = cleanPhone(phone);
      await authAPI.register({ fullName, phone: cleanedPhone });
      setOtp(Array.from({ length: OTP_LENGTH }, () => ''));
      setSuccess('OTP resent successfully!');
      setTimeout(() => otpRefs[0].current?.focus(), 50);
    } catch (e) {
      setError('Failed to resend OTP.');
    } finally {
      setOtpLoading(false);
    }
  };

  const handleVerifyOTP = async (e) => {
    e.preventDefault();
    const otpStr = otp.join('');
    if (otpStr.length !== OTP_LENGTH) { setError(`Please enter the complete ${OTP_LENGTH}-digit OTP.`); return; }
    setLoading(true);
    try {
      const cleanedPhone = cleanPhone(form.phone);
      const resp = await authAPI.verifyRegistrationOTP(cleanedPhone, otpStr);
      if (!resp?.success || !resp?.data?.token) {
        setError(resp?.message || 'Invalid OTP. Please try again.');
        return;
      }

      const authData = resp.data;
      localStorage.setItem('token', authData.token);
      localStorage.setItem('user', JSON.stringify({
        fullName: authData.fullName,
        email: authData.email,
        phone: authData.phone,
        role: authData.role,
        dlVerified: authData.dlVerified,
        phoneVerified: authData.phoneVerified,
      }));
      setStep(3);
      setTimeout(() => onSuccess(authData), 800);
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || 'Invalid OTP. Please try again.');
    } finally { setLoading(false); }
  };

  const modalStyle = {
    background: 'linear-gradient(135deg, #12121a, #1a1a2e)',
    border: '1px solid rgba(255,255,255,0.1)',
  };

  return (
    <div
      className="fixed inset-0 z-[99999] flex items-center justify-center px-4 overflow-y-auto py-4"
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
        className="relative w-full max-w-md rounded-2xl overflow-hidden"
        style={{
          ...modalStyle,
          position: 'relative',
          maxHeight: '90vh',
          overflowY: 'auto',
          touchAction: 'pan-y',
          WebkitOverflowScrolling: 'touch',
          margin: '0 auto'
        }}
        onClick={(e) => e.stopPropagation()}
      >
        <div className="h-1 w-full" style={{ background: 'linear-gradient(90deg, #FF7000, #ff9a3c)' }} />

        {/* Step progress */}
        <div className="flex items-center justify-center gap-2 pt-6 px-8">
          {[1, 2, 3].map((s) => (
            <React.Fragment key={s}>
              <div className={`w-8 h-8 rounded-full flex items-center justify-center text-xs font-bold transition-all ${
                step > s ? 'bg-green-500 text-white' : step === s ? 'text-white' : 'text-gray-600'
              }`} style={step === s ? { background: 'linear-gradient(135deg, #FF7000, #ff9a3c)' } : step > s ? {} : { background: 'rgba(255,255,255,0.05)', border: '1px solid rgba(255,255,255,0.1)' }}>
                {step > s ? <CheckCircle size={14} /> : s}
              </div>
              {s < 3 && <div className={`flex-1 h-px transition-all ${step > s ? 'bg-green-500' : 'bg-white bg-opacity-10'}`} style={{ maxWidth: 40 }} />}
            </React.Fragment>
          ))}
        </div>

        <div className="p-8 pt-5">
          <button
            type="button"
            onClick={(e) => { e.preventDefault(); e.stopPropagation(); resetModal(); }}
            className="absolute top-5 right-5 text-gray-500 hover:text-white transition-colors"
          >
            <X size={20} />
          </button>

          {/* Step 1 — Name + Phone */}
          {step === 1 && (
            <>
              <div className="flex items-center gap-2 mb-4">
                <div className="w-9 h-9 rounded-xl flex items-center justify-center" style={{ background: 'linear-gradient(135deg, #FF7000, #ff9a3c)' }}>
                  <Zap size={18} className="text-white" fill="white" />
                </div>
                <span className="text-lg font-bold text-white">BikePooling</span>
              </div>
              <h2 className="text-2xl font-bold text-white mb-1">Create account</h2>
              <p className="text-gray-400 text-sm mb-5">Join thousands of smart commuters</p>

              {error && (
                <div className="mb-4 px-4 py-3 rounded-xl text-sm text-red-300"
                  style={{ background: 'rgba(239,68,68,0.1)', border: '1px solid rgba(239,68,68,0.2)' }}>
                  {error}
                </div>
              )}
              {success && (
                <div className="mb-4 px-4 py-3 rounded-xl text-sm text-green-300"
                  style={{ background: 'rgba(34,197,94,0.1)', border: '1px solid rgba(34,197,94,0.2)' }}>
                  {success}
                </div>
              )}

              
              <form onSubmit={handleRegister} className="space-y-3">
                <div>
                  <label className="block text-xs font-medium text-gray-400 mb-1.5 uppercase tracking-wide">Full Name</label>
                  <input type="text" name="fullName" value={form.fullName} onChange={handleChange}
                    placeholder="John Doe" className="input-dark w-full px-4 py-3 rounded-xl text-sm" required />
                </div>
                <div>
                  <label className="block text-xs font-medium text-gray-400 mb-1.5 uppercase tracking-wide">Phone</label>
                  <input type="tel" name="phone" value={form.phone} onChange={handleChange}
                    placeholder="Enter 10-digit phone number" maxLength={10}
                    className="input-dark w-full px-4 py-3 rounded-xl text-sm" required />
                </div>
                <button type="submit" disabled={loading}
                  className="btn-primary w-full py-3 text-sm mt-1 disabled:opacity-50 flex items-center justify-center gap-2">
                  {loading && <Loader size={15} className="animate-spin" />}
                  <span>{loading ? 'Sending OTP...' : 'Get OTP'}</span>
                </button>
              </form>

              <p className="text-center text-sm text-gray-500 mt-4">
                Already have an account?{' '}
                <button onClick={onSwitchToLogin} className="text-primary-orange font-semibold hover:underline">Log in</button>
              </p>
            </>
          )}

          {/* Step 2 — OTP */}
          {step === 2 && (
            <>
              <div className="w-14 h-14 rounded-2xl flex items-center justify-center mb-4"
                style={{ background: 'linear-gradient(135deg, rgba(255,112,0,0.2), rgba(255,154,60,0.1))', border: '1px solid rgba(255,112,0,0.3)' }}>
                <Phone size={24} className="text-primary-orange" />
              </div>
              <h2 className="text-2xl font-bold text-white mb-1">Verify your phone</h2>
              <p className="text-gray-400 text-sm mb-6">
                OTP sent to <span className="text-white font-medium">{form.phone}</span>
              </p>

              {error && (
                <div className="mb-4 px-4 py-3 rounded-xl text-sm text-red-300"
                  style={{ background: 'rgba(239,68,68,0.1)', border: '1px solid rgba(239,68,68,0.2)' }}>
                  {error}
                </div>
              )}

              <form onSubmit={handleVerifyOTP}>
                <div className="flex gap-3 justify-center mb-6">
                  {otp.map((digit, i) => (
                    <input
                      key={i}
                      ref={otpRefs[i]}
                      type="text"
                      inputMode="numeric"
                      maxLength={1}
                      value={digit}
                      onChange={(e) => handleOtpChange(i, e.target.value)}
                      onKeyDown={(e) => handleOtpKeyDown(i, e)}
                      className="otp-input"
                    />
                  ))}
                </div>
                <button type="submit" disabled={loading}
                  className="btn-primary w-full py-3 text-sm disabled:opacity-50 flex items-center justify-center gap-2">
                  {loading && <Loader size={15} className="animate-spin" />}
                  <span>{loading ? 'Verifying...' : 'Verify OTP'}</span>
                </button>
              </form>

              <p className="text-center text-sm text-gray-500 mt-4">
                Didn't receive it?{' '}
                <button onClick={handleResendOTP} disabled={otpLoading}
                  className="text-primary-orange font-semibold hover:underline disabled:opacity-50">
                  {otpLoading ? 'Sending...' : 'Resend OTP'}
                </button>
              </p>
            </>
          )}

          {/* Step 3 — Success */}
          {step === 3 && (
            <div className="text-center py-8">
              <div className="w-20 h-20 rounded-full flex items-center justify-center mx-auto mb-5"
                style={{ background: 'linear-gradient(135deg, rgba(34,197,94,0.2), rgba(34,197,94,0.1))', border: '1px solid rgba(34,197,94,0.3)' }}>
                <CheckCircle size={40} className="text-green-400" />
              </div>
              <h2 className="text-2xl font-bold text-white mb-2">You're verified!</h2>
              <p className="text-gray-400 text-sm">Logging you in...</p>
              <div className="mt-4 flex justify-center gap-1">
                {[0,1,2].map(i => (
                  <div key={i} className="w-2 h-2 rounded-full bg-primary-orange animate-bounce"
                    style={{ animationDelay: `${i * 0.15}s` }} />
                ))}
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default SignupModal;
