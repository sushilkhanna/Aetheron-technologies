import React, { useState, useEffect, useRef, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { 
  Users, UserCheck, Bike, DollarSign, 
  BarChart3, Activity, AlertCircle, Wifi, WifiOff
} from 'lucide-react';
import AdminLayout from '../../components/AdminLayout';
import getApiConfig from '../../config/api';

const AdminDashboard = () => {
  const navigate = useNavigate();
  const [adminUser, setAdminUser] = useState(null);
  const [stats, setStats] = useState({
    totalUsers: 0,
    activeRides: 0,
    ridesToday: 0,
    revenueToday: 0
  });
  const [loading, setLoading] = useState(true);
  const [sseStatus, setSseStatus] = useState('connecting'); // 'connecting' | 'connected' | 'disconnected'
  const abortControllerRef = useRef(null);
  const reconnectTimeoutRef = useRef(null);
  const reconnectAttemptRef = useRef(0);

  // Build the SSE stream URL from the same base config the rest of the app uses
  const getStreamUrl = useCallback(() => {
    const { baseURL } = getApiConfig(); // e.g. "http://localhost:8080/api"
    return `${baseURL}/admin/metrics/stream`;
  }, []);

  // Build the one-shot REST URL for initial fetch
  const getMetricsUrl = useCallback(() => {
    const { baseURL } = getApiConfig();
    return `${baseURL}/admin/metrics`;
  }, []);

  // ---------- Initial REST fetch (fast first paint) ----------
  const fetchInitialMetrics = useCallback(async () => {
    const token = localStorage.getItem('adminToken');
    if (!token) return;

    try {
      const res = await fetch(getMetricsUrl(), {
        headers: { 'Authorization': `Bearer ${token}` }
      });
      if (res.ok) {
        const data = await res.json();
        setStats({
          totalUsers: data.totalUsers ?? 0,
          activeRides: data.activeRides ?? 0,
          ridesToday: data.ridesToday ?? 0,
          revenueToday: data.revenueToday ?? 0
        });
      }
    } catch (err) {
      console.warn('Initial metrics fetch failed, SSE will provide data:', err);
    } finally {
      setLoading(false);
    }
  }, [getMetricsUrl]);

  // ---------- SSE via fetch + ReadableStream ----------
  // We use fetch instead of EventSource so we can send the Authorization header.
  const connectSSE = useCallback(() => {
    const token = localStorage.getItem('adminToken');
    if (!token) return;

    // Abort any existing connection
    if (abortControllerRef.current) {
      abortControllerRef.current.abort();
    }

    const controller = new AbortController();
    abortControllerRef.current = controller;
    setSseStatus('connecting');

    fetch(getStreamUrl(), {
      headers: {
        'Authorization': `Bearer ${token}`,
        'Accept': 'text/event-stream'
      },
      signal: controller.signal
    })
      .then((response) => {
        if (!response.ok) {
          throw new Error(`SSE response not ok: ${response.status}`);
        }

        setSseStatus('connected');
        reconnectAttemptRef.current = 0; // reset backoff on successful connect

        const reader = response.body.getReader();
        const decoder = new TextDecoder();
        let buffer = '';

        const read = () => {
          reader.read().then(({ done, value }) => {
            if (done) {
              // Stream ended by server — reconnect
              setSseStatus('disconnected');
              scheduleReconnect();
              return;
            }

            buffer += decoder.decode(value, { stream: true });

            // SSE events are separated by double-newlines
            const parts = buffer.split('\n\n');
            // The last element may be an incomplete event, keep it in the buffer
            buffer = parts.pop() || '';

            for (const part of parts) {
              const dataLine = part
                .split('\n')
                .find((line) => line.startsWith('data:'));
              if (dataLine) {
                try {
                  const jsonStr = dataLine.slice(5).trim(); // strip "data:"
                  const data = JSON.parse(jsonStr);
                  setStats({
                    totalUsers: data.totalUsers ?? 0,
                    activeRides: data.activeRides ?? 0,
                    ridesToday: data.ridesToday ?? 0,
                    revenueToday: data.revenueToday ?? 0
                  });
                  setLoading(false);
                } catch (parseErr) {
                  console.warn('Failed to parse SSE event data:', parseErr);
                }
              }
            }

            read(); // continue reading
          }).catch((err) => {
            if (err.name !== 'AbortError') {
              console.warn('SSE read error:', err);
              setSseStatus('disconnected');
              scheduleReconnect();
            }
          });
        };

        read();
      })
      .catch((err) => {
        if (err.name !== 'AbortError') {
          console.warn('SSE connection failed:', err);
          setSseStatus('disconnected');
          scheduleReconnect();
        }
      });
  }, [getStreamUrl]);

  // Exponential backoff reconnect: 2s, 4s, 8s, … capped at 30s
  const scheduleReconnect = useCallback(() => {
    const attempt = reconnectAttemptRef.current;
    const delay = Math.min(2000 * Math.pow(2, attempt), 30000);
    reconnectAttemptRef.current = attempt + 1;

    console.log(`SSE reconnecting in ${delay / 1000}s (attempt ${attempt + 1})`);
    reconnectTimeoutRef.current = setTimeout(() => {
      connectSSE();
    }, delay);
  }, [connectSSE]);

  // ---------- Lifecycle ----------
  useEffect(() => {
    // Try to load admin user info for display (optional)
    const adminData = localStorage.getItem('adminUser');
    if (adminData) {
      try { setAdminUser(JSON.parse(adminData)); } catch {}
    }

    // Backend API secures admin endpoints — no frontend auth guard needed
    // If the token is invalid, the API calls below will return 401/403

    // 1. Fetch metrics immediately via REST for a fast first paint
    fetchInitialMetrics();

    // 2. Open an SSE stream for live updates every 5s
    connectSSE();

    // Cleanup on unmount
    return () => {
      if (abortControllerRef.current) {
        abortControllerRef.current.abort();
      }
      if (reconnectTimeoutRef.current) {
        clearTimeout(reconnectTimeoutRef.current);
      }
    };
  }, [navigate, fetchInitialMetrics, connectSSE]);

  // ---------- Sub-components ----------
  const StatCard = ({ icon: Icon, title, value, color }) => (
    <div className="bg-gray-800 border border-orange-500/20 rounded-xl p-6 hover:border-orange-500/40 transition-all">
      <div className="flex items-center justify-between mb-4">
        <div className={`p-3 rounded-lg ${color}`}>
          <Icon size={24} className="text-white" />
        </div>
      </div>
      <h3 className="text-2xl font-bold text-white mb-1">{typeof value === 'number' ? value.toLocaleString() : value}</h3>
      <p className="text-orange-300 text-sm">{title}</p>
    </div>
  );

  const SseStatusBadge = () => {
    const config = {
      connected: { icon: Wifi, label: 'Live', color: 'text-green-400', bg: 'bg-green-500/10', border: 'border-green-500/30', dot: 'bg-green-400' },
      connecting: { icon: Wifi, label: 'Connecting…', color: 'text-yellow-400', bg: 'bg-yellow-500/10', border: 'border-yellow-500/30', dot: 'bg-yellow-400' },
      disconnected: { icon: WifiOff, label: 'Reconnecting…', color: 'text-red-400', bg: 'bg-red-500/10', border: 'border-red-500/30', dot: 'bg-red-400' }
    };
    const { icon: StatusIcon, label, color, bg, border, dot } = config[sseStatus];

    return (
      <div className={`inline-flex items-center gap-2 px-3 py-1.5 rounded-full text-xs font-medium ${color} ${bg} border ${border}`}>
        <span className={`w-2 h-2 rounded-full ${dot} ${sseStatus === 'connected' ? 'animate-pulse' : ''}`} />
        <StatusIcon size={12} />
        <span>{label}</span>
      </div>
    );
  };

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
              <div className="flex flex-col items-end gap-2">
                <SseStatusBadge />
                <div className="text-right">
                  <p className="text-sm text-orange-400">Today's Date</p>
                  <p className="text-sm font-medium text-white">{new Date().toLocaleDateString()}</p>
                </div>
              </div>
            </div>
          </div>

          {/* Stats Grid — mapped to AdminMetricsDTO fields */}
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
            <StatCard icon={Users} title="Total Users" value={stats.totalUsers} color="bg-orange-500" />
            <StatCard icon={Bike} title="Active Rides" value={stats.activeRides} color="bg-blue-500" />
            <StatCard icon={Activity} title="Rides Today" value={stats.ridesToday} color="bg-green-500" />
            <StatCard icon={DollarSign} title="Today's Revenue" value={`₹${stats.revenueToday.toLocaleString()}`} color="bg-yellow-500" />
          </div>

          {/* Charts Row */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-8">
            <div className="bg-gray-800 border border-orange-500/20 rounded-xl p-6">
              <h3 className="text-lg font-semibold text-white mb-6">Revenue Overview</h3>
              <div className="h-64 flex items-center justify-center bg-gray-900 rounded-lg border border-orange-500/10">
                <div className="text-center">
                  <BarChart3 size={48} className="text-orange-400 mx-auto mb-4" />
                  <p className="text-orange-300">Revenue Chart</p>
                  <p className="text-sm text-orange-400">₹{stats.revenueToday.toLocaleString()} today</p>
                </div>
              </div>
            </div>

            <div className="bg-gray-800 border border-orange-500/20 rounded-xl p-6">
              <h3 className="text-lg font-semibold text-white mb-6">Ride Status</h3>
              <div className="space-y-4">
                <div className="flex items-center justify-between p-4 bg-gray-900 rounded-lg">
                  <div className="flex items-center gap-3">
                    <div className="w-3 h-3 bg-green-500 rounded-full"></div>
                    <span className="text-sm text-orange-300">Rides Today</span>
                  </div>
                  <span className="text-sm font-medium text-white">{stats.ridesToday.toLocaleString()}</span>
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
                  <span className="font-medium">Pending KYC</span>
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
