import React, { useState, useEffect } from 'react';
import AdminLayout from '../../components/AdminLayout';
import AdminLaunchControlCard from '../../components/AdminLaunchControlCard';
import { 
  Smartphone, Search, Send, CheckCircle2, Clock, Filter, RefreshCw, X, MessageSquare, AlertCircle
} from 'lucide-react';
import getApiConfig from '../../config/api';

const ComingSoonSubscribers = () => {
  const [subscribers, setSubscribers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);
  const [search, setSearch] = useState('');
  const [notifiedFilter, setNotifiedFilter] = useState('ALL'); // ALL, TRUE, FALSE
  
  // Selection & SMS Modal State
  const [selectedIds, setSelectedIds] = useState([]);
  const [smsModalOpen, setSmsModalOpen] = useState(false);
  const [smsMessage, setSmsMessage] = useState(
    "🚀 BikePooling is launching soon! Get ready to share rides and split fuel costs. Stay tuned for download links!"
  );
  const [targetAll, setTargetAll] = useState(false);
  const [sendingSms, setSendingSms] = useState(false);
  const [statusNotification, setStatusNotification] = useState(null);

  const fetchSubscribers = async () => {
    setLoading(true);
    try {
      const apiConfig = getApiConfig();
      const adminToken = localStorage.getItem('adminToken') || localStorage.getItem('token');

      let url = `${apiConfig.baseURL}/admin/coming-soon/subscribers?page=${page}&size=20`;
      if (search) url += `&search=${encodeURIComponent(search)}`;
      if (notifiedFilter !== 'ALL') url += `&notified=${notifiedFilter === 'TRUE'}`;

      const res = await fetch(url, {
        headers: {
          'Authorization': `Bearer ${adminToken}`,
          'Content-Type': 'application/json'
        }
      });

      if (res.ok) {
        const data = await res.json();
        const content = data.data?.content || data.content || [];
        setSubscribers(content);
        setTotalPages(data.data?.totalPages || data.totalPages || 1);
        setTotalElements(data.data?.totalElements || data.totalElements || 0);
      } else {
        console.error('Failed to fetch coming soon subscribers, HTTP status:', res.status);
      }
    } catch (err) {
      console.error('Error fetching coming soon subscribers:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchSubscribers();
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page, notifiedFilter]);

  const handleSearchSubmit = (e) => {
    e.preventDefault();
    setPage(0);
    fetchSubscribers();
  };

  const handleSelectAll = (e) => {
    if (e.target.checked) {
      setSelectedIds(subscribers.map(s => s.id));
    } else {
      setSelectedIds([]);
    }
  };

  const handleToggleSelect = (id) => {
    if (selectedIds.includes(id)) {
      setSelectedIds(selectedIds.filter(i => i !== id));
    } else {
      setSelectedIds([...selectedIds, id]);
    }
  };

  const handleSendSmsSubmit = async (e) => {
    e.preventDefault();
    if (!smsMessage.trim()) return;

    setSendingSms(true);
    setStatusNotification(null);

    try {
      const apiConfig = getApiConfig();
      const adminToken = localStorage.getItem('adminToken') || localStorage.getItem('token');

      const payload = {
        subscriberIds: targetAll ? [] : selectedIds,
        targetAll: targetAll,
        search: search,
        notified: notifiedFilter === 'ALL' ? null : (notifiedFilter === 'TRUE'),
        message: smsMessage
      };

      const res = await fetch(`${apiConfig.baseURL}/admin/coming-soon/send-sms`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${adminToken}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(payload)
      });

      const data = await res.json();
      if (res.ok && data.success) {
        setStatusNotification({
          type: 'success',
          msg: data.message || `SMS dispatched successfully!`
        });
        setSelectedIds([]);
        setSmsModalOpen(false);
        fetchSubscribers();
      } else {
        setStatusNotification({
          type: 'error',
          msg: data.message || 'Failed to dispatch SMS'
        });
      }
    } catch (err) {
      setStatusNotification({
        type: 'error',
        msg: 'Connection error while dispatching SMS.'
      });
    } finally {
      setSendingSms(false);
    }
  };

  return (
    <AdminLayout>
      <div className="p-6 max-w-7xl mx-auto space-y-6">
        {/* Status Notification Toast */}
        {statusNotification && (
          <div className={`p-4 rounded-xl flex items-center justify-between border ${
            statusNotification.type === 'success' 
              ? 'bg-emerald-500/10 border-emerald-500/30 text-emerald-300' 
              : 'bg-red-500/10 border-red-500/30 text-red-300'
          }`}>
            <div className="flex items-center gap-2">
              {statusNotification.type === 'success' ? <CheckCircle2 size={18} /> : <AlertCircle size={18} />}
              <span className="text-sm font-medium">{statusNotification.msg}</span>
            </div>
            <button onClick={() => setStatusNotification(null)} className="text-gray-400 hover:text-white">
              <X size={16} />
            </button>
          </div>
        )}

        {/* Page Header */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div>
            <div className="flex items-center gap-2">
              <Smartphone size={24} className="text-orange-400" />
              <h1 className="text-2xl font-black text-white">Coming Soon Pre-Registrations</h1>
            </div>
            <p className="text-sm text-gray-400 mt-1">
              Manage mobile numbers collected via the "Coming Soon" modal and dispatch SMS launch alerts.
            </p>
          </div>

          <div className="flex items-center gap-3">
            <button
              onClick={fetchSubscribers}
              className="p-2.5 rounded-xl bg-gray-800 hover:bg-gray-700 text-gray-300 hover:text-white border border-gray-700 transition-all"
              title="Refresh"
            >
              <RefreshCw size={18} className={loading ? 'animate-spin' : ''} />
            </button>

            <button
              onClick={() => {
                setTargetAll(false);
                setSmsModalOpen(true);
              }}
              disabled={selectedIds.length === 0}
              className="px-4 py-2.5 rounded-xl bg-gradient-to-r from-orange-500 to-amber-500 hover:from-orange-600 hover:to-amber-600 disabled:opacity-50 text-white font-bold text-sm flex items-center gap-2 shadow-lg shadow-orange-500/20 transition-all"
            >
              <Send size={16} />
              <span>Send SMS ({selectedIds.length})</span>
            </button>
          </div>
        </div>

        {/* Priority Launch Control Portal */}
        <AdminLaunchControlCard />

        {/* Search & Filter Bar */}
        <div className="bg-gray-900 border border-gray-800 rounded-2xl p-4 flex flex-col md:flex-row gap-4 justify-between items-center">
          <form onSubmit={handleSearchSubmit} className="relative w-full md:w-80">
            <input
              type="text"
              placeholder="Search by mobile number or platform..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="w-full pl-10 pr-4 py-2.5 bg-gray-800 border border-gray-700 rounded-xl text-white text-sm placeholder-gray-400 focus:outline-none focus:border-orange-500"
            />
            <Search size={16} className="absolute left-3.5 top-3 text-gray-400" />
          </form>

          <div className="flex items-center gap-3 w-full md:w-auto">
            <Filter size={16} className="text-gray-400" />
            <select
              value={notifiedFilter}
              onChange={(e) => {
                setNotifiedFilter(e.target.value);
                setPage(0);
              }}
              className="bg-gray-800 border border-gray-700 text-gray-300 text-sm rounded-xl px-3 py-2.5 focus:outline-none focus:border-orange-500"
            >
              <option value="ALL">All Subscribers ({totalElements})</option>
              <option value="FALSE">Pending SMS</option>
              <option value="TRUE">SMS Sent</option>
            </select>
          </div>
        </div>

        {/* Subscribers Table */}
        <div className="bg-gray-900 border border-gray-800 rounded-2xl overflow-hidden shadow-xl">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm text-gray-300">
              <thead className="bg-black/50 text-gray-400 uppercase text-[11px] font-bold tracking-wider border-b border-gray-800">
                <tr>
                  <th className="px-5 py-4 w-10">
                    <input
                      type="checkbox"
                      checked={subscribers.length > 0 && selectedIds.length === subscribers.length}
                      onChange={handleSelectAll}
                      className="rounded border-gray-700 text-orange-500 focus:ring-orange-500 bg-gray-800"
                    />
                  </th>
                  <th className="px-5 py-4">ID</th>
                  <th className="px-5 py-4">Mobile Number</th>
                  <th className="px-5 py-4">Platform</th>
                  <th className="px-5 py-4">Registered Date</th>
                  <th className="px-5 py-4">SMS Status</th>
                  <th className="px-5 py-4 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-800">
                {loading ? (
                  <tr>
                    <td colSpan={7} className="px-5 py-12 text-center text-gray-400">
                      <div className="w-6 h-6 border-2 border-orange-500 border-t-transparent rounded-full animate-spin mx-auto mb-2" />
                      Loading pre-registered subscribers...
                    </td>
                  </tr>
                ) : subscribers.length === 0 ? (
                  <tr>
                    <td colSpan={7} className="px-5 py-12 text-center text-gray-400">
                      No pre-registered subscribers found.
                    </td>
                  </tr>
                ) : (
                  subscribers.map((sub) => (
                    <tr key={sub.id} className="hover:bg-gray-800/50 transition-colors">
                      <td className="px-5 py-4">
                        <input
                          type="checkbox"
                          checked={selectedIds.includes(sub.id)}
                          onChange={() => handleToggleSelect(sub.id)}
                          className="rounded border-gray-700 text-orange-500 focus:ring-orange-500 bg-gray-800"
                        />
                      </td>
                      <td className="px-5 py-4 font-mono text-xs text-gray-400">#{sub.id}</td>
                      <td className="px-5 py-4 font-bold text-white">
                        +91 {sub.phone}
                      </td>
                      <td className="px-5 py-4">
                        <span className={`px-2.5 py-0.5 rounded-full text-xs font-semibold uppercase border ${
                          sub.platform === 'ANDROID' 
                            ? 'bg-emerald-500/10 border-emerald-500/20 text-emerald-400'
                            : sub.platform === 'IOS'
                            ? 'bg-blue-500/10 border-blue-500/20 text-blue-400'
                            : 'bg-orange-500/10 border-orange-500/20 text-orange-400'
                        }`}>
                          {sub.platform || 'APP'}
                        </span>
                      </td>
                      <td className="px-5 py-4 text-xs text-gray-400">
                        {sub.createdAt ? new Date(sub.createdAt).toLocaleString() : 'N/A'}
                      </td>
                      <td className="px-5 py-4">
                        {sub.notified ? (
                          <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-emerald-500/15 border border-emerald-500/30 text-emerald-400">
                            <CheckCircle2 size={12} />
                            <span>Sent {sub.notifiedAt ? new Date(sub.notifiedAt).toLocaleDateString() : ''}</span>
                          </span>
                        ) : (
                          <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-amber-500/15 border border-amber-500/30 text-amber-400">
                            <Clock size={12} />
                            <span>Pending</span>
                          </span>
                        )}
                      </td>
                      <td className="px-5 py-4 text-right">
                        <button
                          onClick={() => {
                            setSelectedIds([sub.id]);
                            setTargetAll(false);
                            setSmsModalOpen(true);
                          }}
                          className="px-3 py-1.5 rounded-lg bg-gray-800 hover:bg-orange-500/20 hover:text-orange-400 border border-gray-700 text-xs font-medium transition-all"
                        >
                          Send SMS
                        </button>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>

          {/* Pagination Footer */}
          {totalPages > 1 && (
            <div className="px-5 py-4 bg-black/40 border-t border-gray-800 flex items-center justify-between text-xs text-gray-400">
              <span>Showing Page {page + 1} of {totalPages} ({totalElements} Total)</span>
              <div className="flex gap-2">
                <button
                  disabled={page === 0}
                  onClick={() => setPage(p => p - 1)}
                  className="px-3 py-1.5 rounded-lg bg-gray-800 border border-gray-700 disabled:opacity-50 hover:text-white"
                >
                  Previous
                </button>
                <button
                  disabled={page >= totalPages - 1}
                  onClick={() => setPage(p => p + 1)}
                  className="px-3 py-1.5 rounded-lg bg-gray-800 border border-gray-700 disabled:opacity-50 hover:text-white"
                >
                  Next
                </button>
              </div>
            </div>
          )}
        </div>

        {/* SMS Composition Modal */}
        {smsModalOpen && (
          <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm">
            <div className="relative w-full max-w-lg bg-gray-900 border border-orange-500/30 rounded-3xl p-6 shadow-2xl text-white space-y-4">
              <div className="flex items-center justify-between border-b border-gray-800 pb-3">
                <div className="flex items-center gap-2">
                  <MessageSquare size={20} className="text-orange-400" />
                  <h3 className="text-lg font-bold">Dispatch SMS Notification</h3>
                </div>
                <button onClick={() => setSmsModalOpen(false)} className="text-gray-400 hover:text-white">
                  <X size={20} />
                </button>
              </div>

              <div className="bg-gray-800/60 p-3 rounded-xl border border-gray-700/60 text-xs text-gray-300 flex items-center justify-between">
                <span>Recipients:</span>
                <span className="font-bold text-orange-400">
                  {targetAll ? `All ${totalElements} Subscribers` : `${selectedIds.length} Selected Subscriber(s)`}
                </span>
              </div>

              <form onSubmit={handleSendSmsSubmit} className="space-y-4">
                <div>
                  <label className="block text-xs font-semibold text-gray-400 mb-1.5">SMS Message Body</label>
                  <textarea
                    rows={4}
                    required
                    value={smsMessage}
                    onChange={(e) => setSmsMessage(e.target.value)}
                    className="w-full p-3 bg-gray-800 border border-gray-700 rounded-xl text-white text-sm focus:outline-none focus:border-orange-500"
                  />
                </div>

                <div className="flex items-center justify-between pt-2">
                  <label className="flex items-center gap-2 text-xs text-gray-400 cursor-pointer">
                    <input
                      type="checkbox"
                      checked={targetAll}
                      onChange={(e) => setTargetAll(e.target.checked)}
                      className="rounded border-gray-700 text-orange-500 focus:ring-orange-500 bg-gray-800"
                    />
                    <span>Target all {totalElements} matching subscribers</span>
                  </label>

                  <div className="flex gap-2">
                    <button
                      type="button"
                      onClick={() => setSmsModalOpen(false)}
                      className="px-4 py-2 rounded-xl bg-gray-800 hover:bg-gray-700 text-xs font-semibold text-gray-300"
                    >
                      Cancel
                    </button>
                    <button
                      type="submit"
                      disabled={sendingSms}
                      className="px-5 py-2 rounded-xl bg-orange-500 hover:bg-orange-600 text-white font-bold text-xs flex items-center gap-1.5 disabled:opacity-50 shadow-md shadow-orange-500/20"
                    >
                      {sendingSms ? (
                        <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
                      ) : (
                        <>
                          <Send size={14} />
                          <span>Dispatch SMS</span>
                        </>
                      )}
                    </button>
                  </div>
                </div>
              </form>
            </div>
          </div>
        )}
      </div>
    </AdminLayout>
  );
};

export default ComingSoonSubscribers;
