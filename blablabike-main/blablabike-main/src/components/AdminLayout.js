import React, { useState, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import {
  BarChart3, Users, UserCheck, Bike, Activity, LogOut, Menu, X, AlertTriangle, Smartphone
} from 'lucide-react';

const AdminLayout = ({ children }) => {
  const navigate = useNavigate();
  const location = useLocation();
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [adminUser, setAdminUser] = useState(null);

  useEffect(() => {
    const adminData = localStorage.getItem('adminUser');
    if (adminData) {
      try {
        setAdminUser(JSON.parse(adminData));
      } catch (e) {
        localStorage.removeItem('adminToken');
        localStorage.removeItem('adminUser');
      }
    }
  }, []);

  const handleLogout = () => {
    localStorage.removeItem('adminToken');
    localStorage.removeItem('adminUser');
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    window.location.href = '/';
  };

  const menuItems = [
    { icon: BarChart3,    label: 'Dashboard',           path: '/admin/dashboard' },
    { icon: Users,        label: 'User Management',     path: '/admin/users' },
    { icon: Smartphone,   label: 'Coming Soon Users',   path: '/admin/coming-soon-users' },
    { icon: UserCheck,    label: 'Driver KYC',          path: '/admin/driver-kyc' },
    { icon: Bike,         label: 'Ride Management',     path: '/admin/rides' },
    { icon: AlertTriangle,label: 'SOS Monitoring',      path: '/admin/sos' },
  ];

  const isActive = (path) => location.pathname === path;

  // Check if current page is SOS for red accent
  const isSosPage = location.pathname === '/admin/sos';

  return (
    <div className="min-h-screen bg-gray-900">
      {/* Fixed Header */}
      <header className="fixed top-0 left-0 right-0 z-40 bg-black border-b border-orange-500/20">
        <div className="flex items-center justify-between px-6 py-4">
          <div className="flex items-center gap-4">
            <button
              onClick={() => setSidebarOpen(true)}
              className="lg:hidden p-2 hover:bg-gray-800 rounded-lg text-orange-400"
            >
              <Menu size={20} />
            </button>
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 bg-gradient-to-br from-orange-500 to-orange-600 rounded-lg flex items-center justify-center">
                <Activity size={24} className="text-white" />
              </div>
              <div>
                <h1 className="text-xl font-bold text-white">BikePooling</h1>
                <p className="text-sm text-orange-400">Admin Panel</p>
              </div>
            </div>
          </div>

          <div className="flex items-center gap-4">
            <div className="hidden sm:flex items-center gap-3 px-3 py-2 bg-gray-800 rounded-lg">
              <div className="w-8 h-8 bg-orange-500 rounded-full flex items-center justify-center">
                <span className="text-white font-bold text-sm">
                  {adminUser?.fullName?.charAt(0) || 'A'}
                </span>
              </div>
              <div className="hidden md:block">
                <p className="text-sm font-medium text-white truncate">
                  {adminUser?.fullName || 'Admin'}
                </p>
                <p className="text-xs text-orange-400 truncate">
                  {adminUser?.email || 'admin@bikepooling.com'}
                </p>
              </div>
            </div>
            <button
              onClick={handleLogout}
              className="p-2 text-red-400 hover:bg-red-500/10 rounded-lg transition-colors"
              title="Logout"
            >
              <LogOut size={20} />
            </button>
          </div>
        </div>
      </header>

      {/* Sidebar */}
      <div className={`fixed top-16 left-0 z-50 w-64 bg-black border-r border-orange-500/20 h-[calc(100vh-4rem)] transform ${
        sidebarOpen ? 'translate-x-0' : '-translate-x-full'
      } transition-transform duration-300 ease-in-out lg:translate-x-0`}>
        <div className="p-6">
          <div className="flex items-center justify-between mb-6">
            <h2 className="text-lg font-bold text-white">Menu</h2>
            <button
              onClick={() => setSidebarOpen(false)}
              className="lg:hidden p-2 hover:bg-gray-800 rounded-lg text-orange-400"
            >
              <X size={20} />
            </button>
          </div>

          <nav className="space-y-1">
            {menuItems.map((item, index) => {
              const Icon = item.icon;
              const active = isActive(item.path);
              const isSos = item.path === '/admin/sos';

              return (
                <button
                  key={index}
                  onClick={() => {
                    navigate(item.path);
                    setSidebarOpen(false);
                  }}
                  className={`w-full flex items-center gap-3 px-4 py-3 rounded-lg transition-all duration-200 ${
                    active
                      ? isSos
                        ? 'bg-red-500 text-white shadow-lg shadow-red-500/25'
                        : 'bg-orange-500 text-white shadow-lg shadow-orange-500/25'
                      : isSos
                      ? 'text-red-400 hover:bg-red-500/10 hover:text-red-300'
                      : 'text-orange-300 hover:bg-gray-800 hover:text-white'
                  }`}
                >
                  <Icon size={20} className={active && isSos ? 'animate-pulse' : ''} />
                  <span className="font-medium">{item.label}</span>
                  {/* Red dot on SOS when not active — draw attention */}
                  {isSos && !active && (
                    <span className="ml-auto w-2 h-2 bg-red-500 rounded-full animate-pulse" />
                  )}
                </button>
              );
            })}
          </nav>
        </div>
      </div>

      {/* Main Content */}
      <div className="lg:ml-64 pt-16">
        {children}
      </div>

      {/* Mobile overlay */}
      {sidebarOpen && (
        <div
          className="fixed inset-0 bg-black/50 z-40 lg:hidden"
          onClick={() => setSidebarOpen(false)}
        />
      )}
    </div>
  );
};

export default AdminLayout;
