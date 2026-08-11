import React, { useEffect } from 'react';
import { FileText, ShieldAlert, CheckCircle, Bike, AlertTriangle, ChevronRight, Scale } from 'lucide-react';
import { Link } from 'react-router-dom';

const TermsConditions = () => {
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
          <span className="text-gray-200">Terms & Conditions</span>
        </div>

        {/* Header Banner */}
        <div className="rounded-3xl p-8 mb-10 relative overflow-hidden"
          style={{ background: 'linear-gradient(135deg, rgba(255,112,0,0.15) 0%, rgba(20,20,30,0.8) 100%)', border: '1px solid rgba(255,112,0,0.3)' }}>
          <div className="flex items-center gap-4 mb-4">
            <div className="w-12 h-12 rounded-2xl flex items-center justify-center text-white"
              style={{ background: 'linear-gradient(135deg, #FF7000, #ff9a3c)' }}>
              <Scale size={26} />
            </div>
            <div>
              <h1 className="text-3xl sm:text-4xl font-black text-white">Terms & Conditions</h1>
              <p className="text-sm text-gray-400 mt-1">Effective Date: August 11, 2026 | Version 1.2</p>
            </div>
          </div>
          <p className="text-gray-300 text-sm leading-relaxed max-w-2xl">
            Welcome to BikePooling. By creating an account, browsing, or using our 2-wheeler commute-sharing services, you agree to comply with and be bound by the following terms and conditions.
          </p>
        </div>

        {/* Policy Content Sections */}
        <div className="space-y-8">

          {/* Section 1 */}
          <div className="glass-dark rounded-2xl p-6 sm:p-8 border border-white border-opacity-10">
            <div className="flex items-center gap-3 mb-4 text-orange-400 font-bold text-lg">
              <Bike size={20} />
              <h2>1. Nature of Platform & Non-Commercial Cost Sharing</h2>
            </div>
            <div className="space-y-3 text-sm text-gray-300 leading-relaxed">
              <p>
                BikePooling is a tech-enabled peer-to-peer 2-wheeler ride-matching application that connects fellow daily commuters travelling along matching routes.
              </p>
              <div className="p-4 rounded-xl text-yellow-300 text-xs leading-relaxed" style={{ background: 'rgba(234, 179, 8, 0.1)', border: '1px solid rgba(234, 179, 8, 0.25)' }}>
                <strong>Important Legal Notice:</strong> BikePooling is strictly a cost-sharing platform and NOT a commercial taxi, motor-cab, or commercial transport service. Ride contributions split actual travel costs (fuel, wear and tear) and do not constitute commercial profit or hire wage under Indian Motor Vehicles regulations.
              </div>
            </div>
          </div>

          {/* Section 2 */}
          <div className="glass-dark rounded-2xl p-6 sm:p-8 border border-white border-opacity-10">
            <div className="flex items-center gap-3 mb-4 text-orange-400 font-bold text-lg">
              <CheckCircle size={20} />
              <h2>2. User Eligibility & KYC Requirements</h2>
            </div>
            <div className="space-y-3 text-sm text-gray-300 leading-relaxed">
              <p>To register and use BikePooling, you must fulfill the following criteria:</p>
              <ul className="list-disc pl-5 space-y-2 text-gray-400">
                <li>Be at least <strong className="text-white">18 years of age</strong> and legally capable of entering into binding contracts.</li>
                <li>Complete mobile phone verification and Govt identity verification (Aadhaar / Driving License KYC) when offering rides.</li>
                <li>For Ride Offerers (Drivers): Possess a valid Indian Driving License, active Vehicle Registration Certificate (RC), and valid third-party motor insurance.</li>
              </ul>
            </div>
          </div>

          {/* Section 3 */}
          <div className="glass-dark rounded-2xl p-6 sm:p-8 border border-white border-opacity-10">
            <div className="flex items-center gap-3 mb-4 text-orange-400 font-bold text-lg">
              <ShieldAlert size={20} />
              <h2>3. Safety Guidelines & Mandatory Helmet Policy</h2>
            </div>
            <div className="space-y-4 text-sm text-gray-300 leading-relaxed">
              <p>Safety is our highest priority. All users must strictly abide by road safety rules:</p>
              <div className="grid sm:grid-cols-2 gap-4">
                <div className="p-4 rounded-xl" style={{ background: 'rgba(255,255,255,0.03)', border: '1px solid rgba(255,255,255,0.06)' }}>
                  <h3 className="font-semibold text-white mb-1">Mandatory Protective Helmets</h3>
                  <p className="text-xs text-gray-400">Both rider and passenger MUST wear ISI-marked protective helmets during the entire duration of the commute.</p>
                </div>
                <div className="p-4 rounded-xl" style={{ background: 'rgba(255,255,255,0.03)', border: '1px solid rgba(255,255,255,0.06)' }}>
                  <h3 className="font-semibold text-white mb-1">Zero Substance Policy</h3>
                  <p className="text-xs text-gray-400">Riding under the influence of alcohol, drugs, or illegal substances leads to immediate permanent ban and law enforcement reporting.</p>
                </div>
                <div className="p-4 rounded-xl" style={{ background: 'rgba(255,255,255,0.03)', border: '1px solid rgba(255,255,255,0.06)' }}>
                  <h3 className="font-semibold text-white mb-1">Pillion Rider Limit</h3>
                  <p className="text-xs text-gray-400">Rides are strictly limited to 1 pillion passenger per 2-wheeler as prescribed by traffic law.</p>
                </div>
                <div className="p-4 rounded-xl" style={{ background: 'rgba(255,255,255,0.03)', border: '1px solid rgba(255,255,255,0.06)' }}>
                  <h3 className="font-semibold text-white mb-1">SOS & Emergency Support</h3>
                  <p className="text-xs text-gray-400">In-app 24/7 SOS button triggers immediate alerts to emergency contacts and BikePooling safety desk.</p>
                </div>
              </div>
            </div>
          </div>

          {/* Section 4 */}
          <div className="glass-dark rounded-2xl p-6 sm:p-8 border border-white border-opacity-10">
            <div className="flex items-center gap-3 mb-4 text-orange-400 font-bold text-lg">
              <AlertTriangle size={20} />
              <h2>4. Code of Conduct & Account Termination</h2>
            </div>
            <div className="space-y-3 text-sm text-gray-300 leading-relaxed">
              <p>BikePooling reserves the right to suspend or terminate accounts engaging in:</p>
              <ul className="list-disc pl-5 space-y-2 text-gray-400">
                <li>Harassment, abusive language, or inappropriate behavior towards fellow commuters.</li>
                <li>Offline cash extortion or charging amounts exceeding the calculated cost-sharing fare.</li>
                <li>Fake profile creation, KYC document tampering, or identity impersonation.</li>
              </ul>
            </div>
          </div>

          {/* Section 5 */}
          <div className="glass-dark rounded-2xl p-6 sm:p-8 border border-white border-opacity-10">
            <div className="flex items-center gap-3 mb-4 text-orange-400 font-bold text-lg">
              <Scale size={20} />
              <h2>5. Limitation of Liability & Jurisdiction</h2>
            </div>
            <p className="text-sm text-gray-300 leading-relaxed">
              BikePooling provides route-matching technology in good faith. Users are responsible for personal insurance, vehicle maintenance, and adhering to motor vehicle regulations. Disputes arising under these Terms shall be subject to the exclusive jurisdiction of the courts in Delhi, India.
            </p>
          </div>

        </div>

      </div>
    </div>
  );
};

export default TermsConditions;
