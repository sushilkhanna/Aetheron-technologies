import React from 'react';
import { Link } from 'react-router-dom';
import { Zap, Twitter, Instagram, Linkedin } from 'lucide-react';

const Footer = () => (
  <footer style={{ background: '#080810', borderTop: '1px solid rgba(255,255,255,0.06)' }}>
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
      <div className="grid md:grid-cols-4 gap-8 mb-10">
        <div className="md:col-span-2">
          <div className="flex items-center gap-2 mb-3">
            <div className="w-8 h-8 rounded-lg flex items-center justify-center"
              style={{ background: 'linear-gradient(135deg, #FF7000, #ff9a3c)' }}>
              <Zap size={16} className="text-white" fill="white" />
            </div>
            <span className="text-xl font-bold">
              <span className="text-white">Bike</span>
              <span className="gradient-text">Pool</span>
              <span className="text-white">ing</span>
            </span>
          </div>
          <p className="text-gray-500 text-sm leading-relaxed max-w-xs">
            India's smartest 2-wheeler ride-sharing platform. Share your commute, split the cost, save the planet.
          </p>
          <div className="flex gap-3 mt-4">
            {[Twitter, Instagram, Linkedin].map((Icon, i) => (
              <button key={i} className="w-8 h-8 rounded-lg flex items-center justify-center text-gray-500 hover:text-white transition-all"
                style={{ background: 'rgba(255,255,255,0.05)', border: '1px solid rgba(255,255,255,0.08)' }}>
                <Icon size={14} />
              </button>
            ))}
          </div>
        </div>
        <div>
          <p className="text-xs font-semibold uppercase tracking-widest text-gray-500 mb-4">Platform</p>
          <div className="space-y-2.5">
            {[['Browse Rides', '/browse'], ['Post a Ride', '/post-ride'], ['Dashboard', '/dashboard']].map(([label, to]) => (
              <Link key={to} to={to} className="block text-gray-400 hover:text-white text-sm transition-colors">{label}</Link>
            ))}
          </div>
        </div>
        <div>
          <p className="text-xs font-semibold uppercase tracking-widest text-gray-500 mb-4">Cities</p>
          <div className="space-y-2.5">
            {['Delhi', 'Mumbai', 'Bengaluru'].map(c => (
              <p key={c} className="text-gray-400 text-sm flex items-center gap-2">
                <span className="w-1.5 h-1.5 rounded-full bg-green-500 inline-block" />
                {c}
              </p>
            ))}
          </div>
        </div>
      </div>
      <div className="pt-6 flex flex-col md:flex-row items-center justify-between gap-3"
        style={{ borderTop: '1px solid rgba(255,255,255,0.06)' }}>
        <p className="text-gray-600 text-xs">© 2024 BikePooling. All rights reserved.</p>
        <p className="text-gray-600 text-xs">Made with ❤️ for Indian commuters</p>
      </div>
    </div>
  </footer>
);

export default Footer;
