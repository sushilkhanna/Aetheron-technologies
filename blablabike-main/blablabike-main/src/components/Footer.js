import React from 'react';
import { Link } from 'react-router-dom';
import { Twitter, Instagram, Linkedin, Shield, Mail } from 'lucide-react';

const GMAIL = 'officialbikepooling.in@gmail.com';
const TWITTER_USERNAME = 'offibikepooling';

const socialLinks = [
  {
    icon: Twitter,
    label: 'Twitter',
    href: `https://x.com/${TWITTER_USERNAME}`,
  },
  {
    icon: Instagram,
    label: 'Instagram',
    href: '#',
  },
  {
    icon: Linkedin,
    label: 'LinkedIn',
    href: '#',
  },
];

const Footer = () => (
  <footer style={{ background: '#080810', borderTop: '1px solid rgba(255,255,255,0.06)' }}>
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
      <div className="grid md:grid-cols-4 gap-8 mb-10">
        <div className="md:col-span-2">
          <div className="flex items-center gap-2.5 mb-3">
            <div className="w-9 h-9 rounded-xl overflow-hidden p-0.5 bg-gradient-to-tr from-orange-500 to-amber-400 shadow-md shadow-orange-500/20">
              <video
                src="/bikepooling.mp4"
                autoPlay
                loop
                muted
                playsInline
                className="w-full h-full object-cover rounded-[10px]"
              />
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

          {/* Social Links */}
          <div className="flex gap-3 mt-4">
            {socialLinks.map(({ icon: Icon, label, href }, i) => (
              <a
                key={i}
                href={href}
                target="_blank"
                rel="noopener noreferrer"
                aria-label={label}
                className="w-8 h-8 rounded-lg flex items-center justify-center text-gray-500 hover:text-white transition-all"
                style={{ background: 'rgba(255,255,255,0.05)', border: '1px solid rgba(255,255,255,0.08)' }}
              >
                <Icon size={14} />
              </a>
            ))}
          </div>

          {/* Email Contact */}
          <a
            href={`https://mail.google.com/mail/?view=cm&to=${GMAIL}`}
            target="_blank"
            rel="noopener noreferrer"
            className="inline-flex items-center gap-2 mt-4 text-sm text-gray-400 hover:text-orange-400 transition-colors"
          >
            <Mail size={14} />
            <span>{GMAIL}</span>
          </a>
        </div>
        <div>
          <p className="text-xs font-semibold uppercase tracking-widest text-gray-500 mb-4">Platform</p>
          <div className="space-y-2.5">
            <Link to="/" className="block text-gray-400 hover:text-white text-sm transition-colors">Home</Link>
            <Link to="/about" className="block text-gray-400 hover:text-white text-sm transition-colors">About Us</Link>
            <Link to="/admin/login" className="flex items-center gap-1.5 text-gray-400 hover:text-white text-sm transition-colors">
              <Shield size={12} />
              Admin Portal
            </Link>
          </div>
        </div>
        <div>
          <p className="text-xs font-semibold uppercase tracking-widest text-gray-500 mb-4">Legal & Policy</p>
          <div className="space-y-2.5">
            <Link to="/privacy-policy" className="block text-gray-400 hover:text-white text-sm transition-colors">Privacy Policy</Link>
            <Link to="/terms-and-conditions" className="block text-gray-400 hover:text-white text-sm transition-colors">Terms & Conditions</Link>
            <Link to="/refund-policy" className="block text-gray-400 hover:text-white text-sm transition-colors">Refund Policy</Link>
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

