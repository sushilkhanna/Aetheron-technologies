import React, { useState, useEffect } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { Menu, X, Zap, Shield } from 'lucide-react';

const Navbar = () => {
  const [isMenuOpen, setIsMenuOpen] = useState(false);
  const [scrolled, setScrolled] = useState(false);
  const location = useLocation();

  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 20);
    window.addEventListener('scroll', onScroll);
    return () => window.removeEventListener('scroll', onScroll);
  }, []);

  const navLinks = [
    { label: 'Home', to: '/' },
    { label: 'About Us', to: '/about' },
  ];

  const isActive = (path) => location.pathname === path;

  return (
    <nav className={`fixed top-0 w-full z-[60] transition-all duration-300 ${scrolled ? 'glass-dark shadow-2xl' : 'bg-transparent'}`}>
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between items-center h-16">
          {/* Logo */}
          <Link to="/" className="flex items-center gap-2.5 group">
            <div className="w-9 h-9 rounded-xl overflow-hidden p-0.5 bg-gradient-to-tr from-orange-500 to-amber-400 shadow-md shadow-orange-500/20 group-hover:scale-105 transition-transform duration-200">
              <video
                src="/bikepooling.mp4"
                autoPlay
                loop
                muted
                playsInline
                className="w-full h-full object-cover rounded-[10px]"
              />
            </div>
            <span className="text-xl font-bold tracking-tight">
              <span className="text-white">Bike</span>
              <span className="gradient-text">Pool</span>
              <span className="text-white">ing</span>
            </span>
          </Link>

          {/* Right side - Menu and Admin Login */}
          <div className="flex items-center gap-4">
            {/* Desktop links */}
            <div className="hidden md:flex items-center gap-1">
              {navLinks.map(({ label, to }) => (
                <Link
                  key={to}
                  to={to}
                  className={`px-4 py-2 rounded-lg text-sm font-medium transition-all ${
                    isActive(to)
                      ? 'text-orange-DEFAULT bg-orange-DEFAULT bg-opacity-10'
                      : 'text-gray-300 hover:text-white hover:bg-gray-700 hover:bg-opacity-50'
                  }`}
                >
                  {label}
                </Link>
              ))}
            </div>

            {/* Admin Login link */}
            <div className="hidden md:flex items-center">
              <Link
                to="/admin/login"
                className="flex items-center gap-1.5 px-4 py-2 rounded-lg text-sm font-medium text-gray-400 hover:text-orange-400 transition-all hover:bg-gray-800 hover:bg-opacity-50"
              >
                <Shield size={14} />
                <span>Admin</span>
              </Link>
            </div>

            {/* Mobile menu button */}
            <button className="md:hidden text-gray-300 hover:text-white" onClick={() => setIsMenuOpen(!isMenuOpen)}>
              {isMenuOpen ? <X size={24} /> : <Menu size={24} />}
            </button>
          </div>
        </div>
      </div>

      {/* Mobile menu */}
      {isMenuOpen && (
        <div className="md:hidden glass-dark border-t border-white border-opacity-10 px-4 py-4 space-y-1 absolute top-16 left-0 right-0">
          {navLinks.map(({ label, to }) => (
            <Link
              key={to}
              to={to}
              onClick={() => setIsMenuOpen(false)}
              className={`block px-4 py-2.5 rounded-xl text-sm font-medium transition-all ${
                isActive(to) ? 'text-orange-DEFAULT bg-orange-DEFAULT bg-opacity-10' : 'text-gray-300 hover:text-white hover:bg-gray-700 hover:bg-opacity-50'
              }`}
            >
              {label}
            </Link>
          ))}
          <div className="pt-2 border-t border-white border-opacity-10">
            <Link
              to="/admin/login"
              onClick={() => setIsMenuOpen(false)}
              className="flex items-center gap-2 px-4 py-2.5 rounded-xl text-sm font-medium text-gray-400 hover:text-orange-400 transition-all"
            >
              <Shield size={14} />
              Admin Login
            </Link>
          </div>
        </div>
      )}
    </nav>
  );
};

export default Navbar;
