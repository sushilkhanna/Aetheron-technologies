import React, { useEffect } from 'react';
import { RefreshCw, Clock, CheckCircle, AlertCircle, HelpCircle, ChevronRight, CreditCard } from 'lucide-react';
import { Link } from 'react-router-dom';

const RefundPolicy = () => {
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
          <span className="text-gray-200">Refund & Cancellation Policy</span>
        </div>

        {/* Header Banner */}
        <div className="rounded-3xl p-8 mb-10 relative overflow-hidden"
          style={{ background: 'linear-gradient(135deg, rgba(255,112,0,0.15) 0%, rgba(20,20,30,0.8) 100%)', border: '1px solid rgba(255,112,0,0.3)' }}>
          <div className="flex items-center gap-4 mb-4">
            <div className="w-12 h-12 rounded-2xl flex items-center justify-center text-white"
              style={{ background: 'linear-gradient(135deg, #FF7000, #ff9a3c)' }}>
              <RefreshCw size={26} />
            </div>
            <div>
              <h1 className="text-3xl sm:text-4xl font-black text-white">Refund & Cancellation Policy</h1>
              <p className="text-sm text-gray-400 mt-1">Effective Date: August 11, 2026 | Version 1.2</p>
            </div>
          </div>
          <p className="text-gray-300 text-sm leading-relaxed max-w-2xl">
            At BikePooling, we aim to ensure fair, transparent, and hassle-free cancellation & refund rules for both riders and bookers.
          </p>
        </div>

        {/* Policy Content Sections */}
        <div className="space-y-8">

          {/* Section 1 */}
          <div className="glass-dark rounded-2xl p-6 sm:p-8 border border-white border-opacity-10">
            <div className="flex items-center gap-3 mb-4 text-orange-400 font-bold text-lg">
              <Clock size={20} />
              <h2>1. Passenger Cancellation Rules</h2>
            </div>
            <div className="space-y-4 text-sm text-gray-300 leading-relaxed">
              <p>Cancellation charges depend on how far in advance the ride is cancelled before scheduled departure:</p>
              <div className="grid sm:grid-cols-2 gap-4">
                <div className="p-4 rounded-xl" style={{ background: 'rgba(34,197,94,0.08)', border: '1px solid rgba(34,197,94,0.2)' }}>
                  <h3 className="font-bold text-green-400 mb-1">More than 30 Minutes Before Ride</h3>
                  <p className="text-xs text-gray-300"><strong className="text-white">100% Full Refund</strong>. Zero cancellation fees applied.</p>
                </div>
                <div className="p-4 rounded-xl" style={{ background: 'rgba(234,179,8,0.08)', border: '1px solid rgba(234,179,8,0.2)' }}>
                  <h3 className="font-bold text-yellow-400 mb-1">Less than 30 Minutes Before Ride</h3>
                  <p className="text-xs text-gray-300">Small nominal convenience fee (up to ₹20) retained to compensate driver's travel time; balance refunded.</p>
                </div>
              </div>
            </div>
          </div>

          {/* Section 2 */}
          <div className="glass-dark rounded-2xl p-6 sm:p-8 border border-white border-opacity-10">
            <div className="flex items-center gap-3 mb-4 text-orange-400 font-bold text-lg">
              <CheckCircle size={20} />
              <h2>2. Driver Cancellation & No-Show</h2>
            </div>
            <div className="space-y-3 text-sm text-gray-300 leading-relaxed">
              <ul className="list-disc pl-5 space-y-2 text-gray-400">
                <li><strong className="text-white">Cancelled by Driver:</strong> If the ride is cancelled by the driver at any point, the passenger receives an <strong className="text-white">instant 100% full refund</strong>. Driver account receives a trust rating reduction.</li>
                <li><strong className="text-white">Passenger No-Show:</strong> If a passenger fails to reach the designated pickup location within 10 minutes of agreed time without notice, the ride may be marked as No-Show with a partial fee deduction to compensate driver effort.</li>
              </ul>
            </div>
          </div>

          {/* Section 3 */}
          <div className="glass-dark rounded-2xl p-6 sm:p-8 border border-white border-opacity-10">
            <div className="flex items-center gap-3 mb-4 text-orange-400 font-bold text-lg">
              <CreditCard size={20} />
              <h2>3. Refund Processing Timelines</h2>
            </div>
            <div className="space-y-3 text-sm text-gray-300 leading-relaxed">
              <p>Approved refunds are processed automatically through our secure payment gateway:</p>
              <div className="p-4 rounded-xl space-y-2 text-xs text-gray-300" style={{ background: 'rgba(255,255,255,0.03)', border: '1px solid rgba(255,255,255,0.06)' }}>
                <div className="flex justify-between items-center py-1 border-b border-white/10">
                  <span>UPI / Wallet Refunds</span>
                  <span className="font-semibold text-green-400">Instant (Within 2-4 hours)</span>
                </div>
                <div className="flex justify-between items-center py-1 border-b border-white/10">
                  <span>Debit / Credit Cards</span>
                  <span className="font-semibold text-orange-400">2 - 5 Business Days</span>
                </div>
                <div className="flex justify-between items-center py-1">
                  <span>Net Banking</span>
                  <span className="font-semibold text-orange-400">3 - 7 Business Days</span>
                </div>
              </div>
            </div>
          </div>

          {/* Section 4 */}
          <div className="glass-dark rounded-2xl p-6 sm:p-8 border border-white border-opacity-10">
            <div className="flex items-center gap-3 mb-4 text-orange-400 font-bold text-lg">
              <HelpCircle size={20} />
              <h2>4. Dispute Resolution & Support</h2>
            </div>
            <p className="text-sm text-gray-300 leading-relaxed mb-4">
              If you experienced an unfair ride cancellation, driver deviation, or payment processing glitch, please report it within 48 hours of ride completion:
            </p>
            <div className="p-4 rounded-xl" style={{ background: 'rgba(255,112,0,0.08)', border: '1px solid rgba(255,112,0,0.2)' }}>
              <p className="text-sm font-bold text-white">BikePooling Payments Desk</p>
              <p className="text-xs text-gray-300 mt-1">Email: <a href="https://mail.google.com/mail/?view=cm&to=officialbikepooling.in@gmail.com" target="_blank" rel="noopener noreferrer" className="text-orange-400 underline">officialbikepooling.in@gmail.com</a></p>
              <p className="text-xs text-gray-300">In-App Support: Profile → Help & Support → Dispute Ride</p>
            </div>
          </div>

        </div>

      </div>
    </div>
  );
};

export default RefundPolicy;
