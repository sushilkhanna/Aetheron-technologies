import React, { useEffect } from 'react';
import { Shield, Lock, Eye, FileText, Smartphone, Server, UserCheck, RefreshCw, ChevronRight } from 'lucide-react';
import { Link } from 'react-router-dom';

const PrivacyPolicy = () => {
  useEffect(() => {
    window.scrollTo(0, 0);
  }, []);

  return (
    <div className="mesh-bg min-h-screen pt-24 pb-20 text-gray-200">
      <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
        
        {/* Breadcrumb Navigation */}
        <div className="flex items-center gap-2 text-xs text-gray-400 mb-6">
          <Link to="/" className="hover:text-orange-400 transition-colors">Home</Link>
          <ChevronRight size={12} />
          <span className="text-gray-200">Privacy Policy</span>
        </div>

        {/* Header Banner */}
        <div className="rounded-3xl p-8 mb-10 relative overflow-hidden"
          style={{ background: 'linear-gradient(135deg, rgba(255,112,0,0.15) 0%, rgba(20,20,30,0.8) 100%)', border: '1px solid rgba(255,112,0,0.3)' }}>
          <div className="flex items-center gap-4 mb-4">
            <div className="w-12 h-12 rounded-2xl flex items-center justify-center text-white"
              style={{ background: 'linear-gradient(135deg, #FF7000, #ff9a3c)' }}>
              <Shield size={26} />
            </div>
            <div>
              <h1 className="text-3xl sm:text-4xl font-black text-white">Privacy Policy</h1>
              <p className="text-sm text-gray-400 mt-1">Effective Date: August 11, 2026 | Version 1.2</p>
            </div>
          </div>
          <p className="text-gray-300 text-sm leading-relaxed max-w-2xl">
            BikePooling ("we", "our", or "us") is committed to safeguarding your privacy and ensuring the security of your personal data. This Privacy Policy details how we collect, use, store, and protect your information when using our 2-wheeler ride-sharing platform across India.
          </p>
        </div>

        {/* Policy Content Sections */}
        <div className="space-y-8">

          {/* Section 1 */}
          <div className="glass-dark rounded-2xl p-6 sm:p-8 border border-white border-opacity-10">
            <div className="flex items-center gap-3 mb-4 text-orange-400 font-bold text-lg">
              <Eye size={20} />
              <h2>1. Information We Collect</h2>
            </div>
            <div className="space-y-4 text-sm text-gray-300 leading-relaxed">
              <p>We collect essential information required to operate a secure, transparent, and efficient commute-sharing network:</p>
              <ul className="list-disc pl-5 space-y-2 text-gray-400">
                <li><strong className="text-white">Account Information:</strong> Name, verified mobile phone number, email address, gender, and profile photo.</li>
                <li><strong className="text-white">Identity & KYC Data:</strong> Govt-issued ID (Aadhaar verification status) and Driving License details for ride offerers (drivers).</li>
                <li><strong className="text-white">Vehicle Details:</strong> Registration Certificate (RC) information, vehicle model, and license plate number for verified riders.</li>
                <li><strong className="text-white">Geolocation & Live Tracking:</strong> Real-time GPS location during active ride matching, route navigation, and emergency SOS situations.</li>
                <li><strong className="text-white">Communication Data:</strong> In-app chat messages, ride support logs, and transactional SMS notifications.</li>
              </ul>
            </div>
          </div>

          {/* Section 2 */}
          <div className="glass-dark rounded-2xl p-6 sm:p-8 border border-white border-opacity-10">
            <div className="flex items-center gap-3 mb-4 text-orange-400 font-bold text-lg">
              <Server size={20} />
              <h2>2. How We Use Your Information</h2>
            </div>
            <div className="space-y-3 text-sm text-gray-300 leading-relaxed">
              <p>Your data is processed strictly for legitimate operational purposes:</p>
              <div className="grid sm:grid-cols-2 gap-4 pt-2">
                <div className="p-4 rounded-xl" style={{ background: 'rgba(255,255,255,0.03)', border: '1px solid rgba(255,255,255,0.06)' }}>
                  <h3 className="font-semibold text-white mb-1">Smart Route Matching</h3>
                  <p className="text-xs text-gray-400">Pairing commuters travelling on identical daily routes in real time.</p>
                </div>
                <div className="p-4 rounded-xl" style={{ background: 'rgba(255,255,255,0.03)', border: '1px solid rgba(255,255,255,0.06)' }}>
                  <h3 className="font-semibold text-white mb-1">Safety & SOS Monitoring</h3>
                  <p className="text-xs text-gray-400">Monitoring ride deviations and enabling 24/7 SOS emergency response protocols.</p>
                </div>
                <div className="p-4 rounded-xl" style={{ background: 'rgba(255,255,255,0.03)', border: '1px solid rgba(255,255,255,0.06)' }}>
                  <h3 className="font-semibold text-white mb-1">Cost Sharing Settlement</h3>
                  <p className="text-xs text-gray-400">Calculating non-commercial fuel and commute contribution shares accurately.</p>
                </div>
                <div className="p-4 rounded-xl" style={{ background: 'rgba(255,255,255,0.03)', border: '1px solid rgba(255,255,255,0.06)' }}>
                  <h3 className="font-semibold text-white mb-1">Trust & KYC Ratings</h3>
                  <p className="text-xs text-gray-400">Displaying verified badges, ratings, and feedback to ensure community safety.</p>
                </div>
              </div>
            </div>
          </div>

          {/* Section 3 */}
          <div className="glass-dark rounded-2xl p-6 sm:p-8 border border-white border-opacity-10">
            <div className="flex items-center gap-3 mb-4 text-orange-400 font-bold text-lg">
              <Lock size={20} />
              <h2>3. Data Protection & Security</h2>
            </div>
            <div className="space-y-3 text-sm text-gray-300 leading-relaxed">
              <p>
                We adhere to strict data protection regulations including India's Digital Personal Data Protection (DPDP) Act, 2023 and the Information Technology Act, 2000.
              </p>
              <ul className="list-disc pl-5 space-y-2 text-gray-400">
                <li>All data in transit is encrypted using 256-bit SSL/TLS protocol.</li>
                <li>We do <strong className="text-white">NOT sell, rent, or trade</strong> your personal information to third-party advertisers.</li>
                <li>Access to sensitive KYC documents is restricted strictly to authorized verification systems and compliance officers.</li>
              </ul>
            </div>
          </div>

          {/* Section 4 */}
          <div className="glass-dark rounded-2xl p-6 sm:p-8 border border-white border-opacity-10">
            <div className="flex items-center gap-3 mb-4 text-orange-400 font-bold text-lg">
              <UserCheck size={20} />
              <h2>4. User Rights & Data Deletion</h2>
            </div>
            <div className="space-y-3 text-sm text-gray-300 leading-relaxed">
              <p>You maintain full control over your personal data:</p>
              <ul className="list-disc pl-5 space-y-2 text-gray-400">
                <li><strong className="text-white">Access & Rectification:</strong> You may update your profile details and preferences at any time via the app settings.</li>
                <li><strong className="text-white">Account Deletion:</strong> You can request complete account deletion through your profile settings or by emailing <span className="text-orange-400 font-medium">privacy@bikepooling.in</span>. Upon account deletion, all personal data is permanently purged within 30 days, subject to legal compliance retention.</li>
              </ul>
            </div>
          </div>

          {/* Section 5 */}
          <div className="glass-dark rounded-2xl p-6 sm:p-8 border border-white border-opacity-10">
            <div className="flex items-center gap-3 mb-4 text-orange-400 font-bold text-lg">
              <FileText size={20} />
              <h2>5. Contact & Grievance Officer</h2>
            </div>
            <p className="text-sm text-gray-300 leading-relaxed mb-4">
              If you have any questions, concerns, or grievances regarding this Privacy Policy or data handling practices, please contact our designated Grievance Officer:
            </p>
            <div className="p-4 rounded-xl" style={{ background: 'rgba(255,112,0,0.08)', border: '1px solid rgba(255,112,0,0.2)' }}>
              <p className="text-sm font-bold text-white">Grievance Redressal Cell - BikePooling</p>
              <p className="text-xs text-gray-300 mt-1">Email: <a href="https://mail.google.com/mail/?view=cm&to=officialbikepooling.in@gmail.com" target="_blank" rel="noopener noreferrer" className="text-orange-400 underline">officialbikepooling.in@gmail.com</a></p>
              <p className="text-xs text-gray-300">Support Hours: Monday to Saturday, 9:00 AM – 6:00 PM IST</p>
            </div>
          </div>

        </div>

      </div>
    </div>
  );
};

export default PrivacyPolicy;
