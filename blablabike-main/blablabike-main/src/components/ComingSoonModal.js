import React, { useState, useEffect } from 'react';
import { X, Smartphone, Bell, CheckCircle2, Sparkles, ShieldCheck, AlertCircle, Clock } from 'lucide-react';
import getApiConfig from '../config/api';

const ComingSoonModal = ({ isOpen, onClose, platform = 'app', launchTargetDateTime = null, launchMessage = '' }) => {
  const [phone, setPhone] = useState('');
  const [subscribed, setSubscribed] = useState(false);
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [responseMsg, setResponseMsg] = useState('');

  // Countdown timer state
  const [timeLeft, setTimeLeft] = useState({ days: 0, hours: 0, minutes: 0, seconds: 0 });
  const [timerActive, setTimerActive] = useState(false);

  useEffect(() => {
    if (!launchTargetDateTime) {
      setTimerActive(false);
      return;
    }

    const calculateTime = () => {
      const targetTime = new Date(launchTargetDateTime).getTime();
      const now = new Date().getTime();
      const difference = targetTime - now;

      if (difference > 0) {
        const days = Math.floor(difference / (1000 * 60 * 60 * 24));
        const hours = Math.floor((difference % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
        const minutes = Math.floor((difference % (1000 * 60 * 60)) / (1000 * 60));
        const seconds = Math.floor((difference % (1000 * 60)) / 1000);
        setTimeLeft({ days, hours, minutes, seconds });
        setTimerActive(true);
      } else {
        setTimerActive(false);
      }
    };

    calculateTime();
    const interval = setInterval(calculateTime, 1000);
    return () => clearInterval(interval);
  }, [launchTargetDateTime]);

  if (!isOpen) return null;

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErrorMessage('');
    
    // Clean and validate 10-digit phone
    const cleanPhone = phone.replace(/[^0-9]/g, '');
    if (cleanPhone.length !== 10) {
      setErrorMessage('Please enter a valid 10-digit mobile number');
      return;
    }

    setLoading(true);

    try {
      const apiConfig = getApiConfig();
      const response = await fetch(`${apiConfig.baseURL}/public/coming-soon/register`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          phone: cleanPhone,
          platform: platform.toUpperCase(),
        }),
      });

      const data = await response.json();

      if (response.ok && data.success) {
        setResponseMsg(data.message || 'Registered successfully for early launch access!');
        setSubscribed(true);
      } else {
        setErrorMessage(data.message || 'Registration failed. Please try again.');
      }
    } catch (err) {
      setResponseMsg('Registered successfully! We will notify your mobile number on launch.');
      setSubscribed(true);
    } finally {
      setLoading(false);
    }
  };

  const handleModalClose = () => {
    setSubscribed(false);
    setPhone('');
    setErrorMessage('');
    setResponseMsg('');
    onClose();
  };

  const platformTitle = 
    platform === 'android' ? 'Android App (Google Play Store)' :
    platform === 'ios' ? 'iOS App (Apple App Store)' : 'Mobile App';

  return (
    <div className="fixed inset-0 z-[100] flex items-center justify-center p-4 bg-black/80 backdrop-blur-md animate-fadeIn">
      {/* Backdrop click handler */}
      <div className="absolute inset-0" onClick={handleModalClose} />

      <div 
        className="relative w-full max-w-lg overflow-hidden rounded-3xl bg-[#0F172A] border border-orange-500/30 p-6 sm:p-8 shadow-[0_0_50px_rgba(255,112,0,0.25)] z-10 text-white transition-all transform scale-100"
        style={{
          background: 'linear-gradient(145deg, rgba(15, 23, 42, 0.95) 0%, rgba(30, 41, 59, 0.95) 100%)',
        }}
      >
        {/* Glow ambient circle */}
        <div className="absolute -top-20 -right-20 w-48 h-48 bg-orange-500/20 rounded-full blur-3xl pointer-events-none" />
        <div className="absolute -bottom-20 -left-20 w-48 h-48 bg-amber-500/15 rounded-full blur-3xl pointer-events-none" />

        {/* Close Button */}
        <button
          onClick={handleModalClose}
          className="absolute top-4 right-4 p-2.5 text-gray-400 hover:text-white hover:bg-white/10 rounded-full transition-all duration-200"
          aria-label="Close modal"
        >
          <X size={20} />
        </button>

        {/* Video Logo Preview Header */}
        <div className="flex flex-col items-center text-center">
          <div className="relative mb-4">
            <div className="w-16 h-16 sm:w-20 sm:h-20 rounded-2xl overflow-hidden p-0.5 bg-gradient-to-tr from-orange-500 via-amber-400 to-orange-600 shadow-xl shadow-orange-500/30">
              <video
                src="/bikepooling.mp4"
                autoPlay
                loop
                muted
                playsInline
                className="w-full h-full object-cover rounded-[14px]"
              />
            </div>
            <div className="absolute -bottom-2 -right-2 bg-orange-500 text-white text-xs px-2.5 py-0.5 rounded-full font-bold shadow-md flex items-center gap-1 border border-black/40">
              <Sparkles size={12} />
              <span>Soon</span>
            </div>
          </div>

          <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-orange-500/10 border border-orange-500/30 text-orange-400 text-xs font-semibold uppercase tracking-wider mb-2">
            <Smartphone size={13} />
            <span>{platformTitle}</span>
          </div>

          <h3 className="text-2xl sm:text-3xl font-extrabold text-white tracking-tight mb-2">
            We're Coming Soon to You! 🚀
          </h3>

          <p className="text-gray-300 text-xs sm:text-sm leading-relaxed max-w-md mb-4">
            {launchMessage || "We are putting the final shine on the BikePooling app. Enter your mobile number to get early launch SMS alerts!"}
          </p>

          {/* Live Countdown Timer if active */}
          {timerActive && (
            <div className="w-full p-4 mb-5 rounded-2xl bg-slate-900/80 border border-orange-500/30 shadow-inner">
              <div className="flex items-center justify-center gap-1.5 text-xs font-bold text-orange-400 uppercase tracking-widest mb-3">
                <Clock size={14} className="animate-spin" />
                <span>Official Launch Countdown</span>
              </div>
              <div className="grid grid-cols-4 gap-2 text-center">
                {[
                  { label: 'Days', val: timeLeft.days },
                  { label: 'Hours', val: timeLeft.hours },
                  { label: 'Mins', val: timeLeft.minutes },
                  { label: 'Secs', val: timeLeft.seconds },
                ].map((unit, idx) => (
                  <div key={idx} className="p-2.5 rounded-xl bg-slate-800/90 border border-slate-700/80">
                    <span className="text-xl sm:text-2xl font-black gradient-text block">
                      {String(unit.val).padStart(2, '0')}
                    </span>
                    <span className="text-[10px] font-semibold text-gray-400 uppercase tracking-wider">
                      {unit.label}
                    </span>
                  </div>
                ))}
              </div>
            </div>
          )}

          {!subscribed ? (
            <form onSubmit={handleSubmit} className="w-full space-y-3">
              {errorMessage && (
                <div className="flex items-center gap-2 p-3 rounded-xl bg-red-500/15 border border-red-500/30 text-red-300 text-xs text-left">
                  <AlertCircle size={16} className="shrink-0 text-red-400" />
                  <span>{errorMessage}</span>
                </div>
              )}

              <div className="relative flex items-center">
                <div className="absolute left-3 flex items-center gap-1.5 text-gray-400 text-sm font-semibold select-none border-r border-slate-700 pr-2">
                  <span className="text-base">🇮🇳</span>
                  <span>+91</span>
                </div>
                <input
                  type="tel"
                  required
                  maxLength={10}
                  placeholder="Enter 10-digit mobile number..."
                  value={phone}
                  onChange={(e) => {
                    const val = e.target.value.replace(/[^0-9]/g, '');
                    if (val.length <= 10) setPhone(val);
                  }}
                  className="w-full py-3.5 pl-24 pr-4 rounded-xl bg-slate-800/80 border border-slate-700 text-white placeholder-gray-400 focus:outline-none focus:border-orange-500 focus:ring-1 focus:ring-orange-500 text-sm font-medium transition-all"
                />
              </div>

              <button
                type="submit"
                disabled={loading}
                className="w-full py-3.5 px-6 rounded-xl font-bold text-white bg-gradient-to-r from-orange-500 to-amber-500 hover:from-orange-600 hover:to-amber-600 active:scale-[0.99] shadow-lg shadow-orange-500/30 flex items-center justify-center gap-2 transition-all text-sm disabled:opacity-70"
              >
                {loading ? (
                  <div className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                ) : (
                  <>
                    <Bell size={16} />
                    <span>Notify Me Via SMS</span>
                  </>
                )}
              </button>
            </form>
          ) : (
            <div className="w-full p-4 rounded-2xl bg-emerald-500/10 border border-emerald-500/30 text-emerald-300 text-center animate-fadeIn">
              <div className="w-10 h-10 bg-emerald-500/20 text-emerald-400 rounded-full flex items-center justify-center mx-auto mb-2">
                <CheckCircle2 size={24} />
              </div>
              <h4 className="font-bold text-base text-emerald-400 mb-1">Mobile Registered! 🎉</h4>
              <p className="text-xs text-emerald-200/80">
                {responseMsg || `Thank you! We will SMS +91 ${phone} as soon as BikePooling goes live.`}
              </p>
            </div>
          )}

          <div className="mt-6 flex items-center justify-center gap-4 text-xs text-gray-400 border-t border-white/10 pt-4 w-full">
            <div className="flex items-center gap-1.5">
              <ShieldCheck size={14} className="text-orange-400" />
              <span>100% Spam Free SMS</span>
            </div>
            <span>•</span>
            <div className="flex items-center gap-1.5">
              <Sparkles size={14} className="text-amber-400" />
              <span>VIP Early Access</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default ComingSoonModal;
