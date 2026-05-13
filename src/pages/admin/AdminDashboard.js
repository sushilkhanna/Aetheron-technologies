import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { 
  Users, UserCheck, Bike, DollarSign, 
  BarChart3, Activity, AlertCircle
} from 'lucide-react';
import AdminLayout from '../../components/AdminLayout';

const AdminDashboard = () => {
  const navigate = useNavigate();
  const [adminUser, setAdminUser] = useState(null);
  const [stats, setStats] = useState({
    totalUsers: 0,
    activeUsers: 0,
    verifiedDrivers: 0,
    pendingKYC: 0,
    activeRides: 0,
    completedRides: 0,
    todayRevenue: 0,
    monthlyRevenue: 0
  });
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const adminToken = localStorage.getItem('adminToken');
    const adminData = localStorage.getItem('adminUser');
    
    if (!adminToken || !adminData) {
      navigate('/');
      return;
    }
    
    setAdminUser(JSON.parse(adminData));
    loadDashboardData();
  }, [navigate]);

  const loadDashboardData = async () => {
    try {
      setLoading(true);
      // TODO: Replace with actual API call
      // const response = await fetch('/api/admin/dashboard/stats', {
      //   headers: { 'Authorization': `Bearer ${localStorage.getItem('adminToken')}` }
      // });
      // const data = await response.json();
      // setStats(data);
      
      // Temporary: Set to 0 until API is implemented
      setStats({
        totalUsers: 0,
        activeUsers: 0,
        verifiedDrivers: 0,
        pendingKYC: 0,
        activeRides: 0,
        completedRides: 0,
        todayRevenue: 0,
        monthlyRevenue: 0
      });
    } catch (error) {
      console.error('Error loading dashboard data:', error);
    } finally {
      setLoading(false);
    }
  };

  const StatCard = ({ icon: Icon, title, value, change, changeType, color }) => (
    <div className="bg-gray-800 border border-orange-500/20 rounded-xl p-6 hover:border-orange-500/40 transition-all">
      <div className="flex items-center justify-between mb-4">
        <div className={`p-3 rounded-lg ${color}`}>
          <Icon size={24} className="text-white" />
        </div>
        {change && (
          <div className={`flex items-center gap-1 text-sm font-medium ${
            changeType === 'increase' ? 'text-green-400' : 'text-red-400'
          }`}>
            <span className="text-lg">{changeType === 'increase' ? '↑' : '↓'}</span>
            {change}
          </div>
        )}
      </div>
      <h3 className="text-2xl font-bold text-white mb-1">{typeof value === 'number' ? value.toLocaleString() : value}</h3>
      <p className="text-orange-300 text-sm">{title}</p>
    </div>
  );

  if (loading) {
    return (
      <AdminLayout>
        <div className="min-h-screen flex items-center justify-center">
          <div className="text-center">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-orange-500 mx-auto mb-4"></div>
            <p className="text-orange-300">Loading dashboard...</p>
          </div>
        </div>
      </AdminLayout>
    );
  }

  return (
    <AdminLayout>
      {/* Dashboard Content */}
      <main className="p-6">
          {/* Welcome Section */}
          <div className="bg-gray-800 border border-orange-500/20 rounded-xl p-6 mb-8">
            <div className="flex items-center justify-between">
              <div>
                <h1 className="text-2xl font-bold text-white mb-2">Dashboard</h1>
                <p className="text-orange-300">Welcome back, {adminUser?.fullName || 'Admin'}</p>
              </div>
              <div className="text-right">
                <p className="text-sm text-orange-400">Today's Date</p>
                <p className="text-sm font-medium text-white">{new Date().toLocaleDateString()}</p>
              </div>
            </div>
          </div>

          {/* Stats Grid */}
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
            <StatCard icon={Users} title="Total Users" value={stats.totalUsers} change="+12%" changeType="increase" color="bg-orange-500" />
            <StatCard icon={UserCheck} title="Verified Drivers" value={stats.verifiedDrivers} change="+8%" changeType="increase" color="bg-green-500" />
            <StatCard icon={Bike} title="Active Rides" value={stats.activeRides} change="+5%" changeType="increase" color="bg-blue-500" />
            <StatCard icon={DollarSign} title="Today's Revenue" value={`₹${stats.todayRevenue.toLocaleString()}`} change="+15%" changeType="increase" color="bg-yellow-500" />
          </div>

          {/* Charts Row */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-8">
            <div className="bg-gray-800 border border-orange-500/20 rounded-xl p-6">
              <h3 className="text-lg font-semibold text-white mb-6">Revenue Overview</h3>
              <div className="h-64 flex items-center justify-center bg-gray-900 rounded-lg border border-orange-500/10">
                <div className="text-center">
                  <BarChart3 size={48} className="text-orange-400 mx-auto mb-4" />
                  <p className="text-orange-300">Revenue Chart</p>
                  <p className="text-sm text-orange-400">₹{stats.monthlyRevenue.toLocaleString()} this month</p>
                </div>
              </div>
            </div>

            <div className="bg-gray-800 border border-orange-500/20 rounded-xl p-6">
              <h3 className="text-lg font-semibold text-white mb-6">Ride Status</h3>
              <div className="space-y-4">
                <div className="flex items-center justify-between p-4 bg-gray-900 rounded-lg">
                  <div className="flex items-center gap-3">
                    <div className="w-3 h-3 bg-green-500 rounded-full"></div>
                    <span className="text-sm text-orange-300">Completed</span>
                  </div>
                  <span className="text-sm font-medium text-white">{stats.completedRides.toLocaleString()}</span>
                </div>
                <div className="flex items-center justify-between p-4 bg-gray-900 rounded-lg">
                  <div className="flex items-center gap-3">
                    <div className="w-3 h-3 bg-orange-500 rounded-full"></div>
                    <span className="text-sm text-orange-300">Active</span>
                  </div>
                  <span className="text-sm font-medium text-white">{stats.activeRides.toLocaleString()}</span>
                </div>
              </div>
            </div>
          </div>

          {/* Quick Actions */}
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            <div className="bg-gray-800 border border-orange-500/20 rounded-xl p-6">
              <h3 className="text-lg font-semibold text-white mb-4">Quick Actions</h3>
              <div className="space-y-3">
                <button onClick={() => navigate('/admin/driver-kyc')} className="w-full flex items-center gap-3 px-4 py-3 bg-yellow-500/10 hover:bg-yellow-500/20 text-yellow-400 rounded-lg transition-colors border border-yellow-500/20">
                  <AlertCircle size={20} />
                  <span className="font-medium">Pending KYC ({stats.pendingKYC})</span>
                </button>
                <button onClick={() => navigate('/admin/users')} className="w-full flex items-center gap-3 px-4 py-3 bg-orange-500/10 hover:bg-orange-500/20 text-orange-400 rounded-lg transition-colors border border-orange-500/20">
                  <Users size={20} />
                  <span className="font-medium">Manage Users</span>
                </button>
                <button onClick={() => navigate('/admin/rides')} className="w-full flex items-center gap-3 px-4 py-3 bg-blue-500/10 hover:bg-blue-500/20 text-blue-400 rounded-lg transition-colors border border-blue-500/20">
                  <Bike size={20} />
                  <span className="font-medium">Active Rides ({stats.activeRides})</span>
                </button>
              </div>
            </div>

            <div className="lg:col-span-2 bg-gray-800 border border-orange-500/20 rounded-xl p-6">
              <h3 className="text-lg font-semibold text-white mb-4">Recent Activity</h3>
              <div className="text-center py-12">
                <Activity size={48} className="text-orange-400 mx-auto mb-4" />
                <p className="text-orange-300">No recent activity</p>
                <p className="text-sm text-orange-400 mt-2">Activity will appear here once API is implemented</p>
              </div>
            </div>
          </div>
        </main>
      </AdminLayout>
  );
};

export default AdminDashboard;
