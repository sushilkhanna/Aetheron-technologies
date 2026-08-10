import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  UserCheck,
  Search,
  RefreshCw,
  FileText,
  CheckCircle,
  XCircle,
  Clock,
  Shield,
  Filter,
  Eye,
  AlertTriangle,
  ChevronLeft,
  ChevronRight
} from 'lucide-react';
import AdminLayout from '../../components/AdminLayout';

const getApiConfig = () => {
  let baseURL = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';
  if (baseURL.endsWith('/')) baseURL = baseURL.slice(0, -1);
  if (!baseURL.endsWith('/api')) baseURL = `${baseURL}/api`;
  return { baseURL };
};

const DriverKYC = () => {
  const navigate = useNavigate();

  // State
  const [requests, setRequests] = useState([]);
  const [stats, setStats] = useState({
    pendingCount: 0,
    aadhaarPending: 0,
    dlPending: 0,
    totalVerified: 0,
    verifiedApi: 0,
    verifiedAdmin: 0,
    rejectedCount: 0
  });
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);
  const [error, setError] = useState(null);

  // Filters & Pagination
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [typeFilter, setTypeFilter] = useState('ALL');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);

  // Modals
  const [selectedRequest, setSelectedRequest] = useState(null);
  const [showDetailModal, setShowDetailModal] = useState(false);
  const [showRejectModal, setShowRejectModal] = useState(false);
  const [rejectReason, setRejectReason] = useState('');

  const getToken = useCallback(() => {
    return localStorage.getItem('adminToken') || localStorage.getItem('token');
  }, []);

  // Fetch Stats
  const fetchStats = useCallback(async () => {
    const token = getToken();
    if (!token) return;
    try {
      const { baseURL } = getApiConfig();
      const res = await fetch(`${baseURL}/admin/kyc/stats`, {
        headers: { Authorization: `Bearer ${token}` }
      });
      if (res.status === 401 || res.status === 403) {
        localStorage.removeItem('adminToken');
        navigate('/');
        return;
      }
      if (res.ok) {
        const json = await res.json();
        setStats(json.data || json);
      }
    } catch (err) {
      console.error('KYC stats fetch failed:', err);
    }
  }, [getToken, navigate]);

  // Fetch Requests
  const fetchRequests = useCallback(async (silent = false) => {
    const token = getToken();
    if (!token) return;
    if (!silent) {
      setLoading(true);
      setError(null);
    }
    try {
      const { baseURL } = getApiConfig();
      const p = new URLSearchParams();
      p.set('page', page);
      p.set('size', 10);
      if (statusFilter !== 'ALL') p.set('status', statusFilter);
      if (typeFilter !== 'ALL') p.set('type', typeFilter);
      if (searchTerm.trim()) p.set('search', searchTerm.trim());

      const res = await fetch(`${baseURL}/admin/kyc?${p.toString()}`, {
        headers: { Authorization: `Bearer ${token}` }
      });

      if (res.status === 401 || res.status === 403) {
        localStorage.removeItem('adminToken');
        navigate('/');
        return;
      }

      if (!res.ok) throw new Error(`Server error: ${res.status}`);

      const json = await res.json();
      const paged = json.data || json;
      setRequests(paged.content || []);
      setTotalPages(paged.totalPages || 1);
      setTotalElements(paged.totalElements || 0);
    } catch (err) {
      console.error('Fetch KYC requests error:', err);
      setError(err.message || 'Failed to load KYC requests');
    } finally {
      if (!silent) setLoading(false);
    }
  }, [getToken, navigate, page, statusFilter, typeFilter, searchTerm]);

  useEffect(() => {
    fetchStats();
    fetchRequests();
  }, [fetchStats, fetchRequests]);

  // Handle Approve
  const handleApprove = async (id) => {
    const token = getToken();
    if (!token) return;
    if (!window.confirm('Are you sure you want to approve this KYC verification?')) return;

    try {
      setActionLoading(true);
      const { baseURL } = getApiConfig();
      const res = await fetch(`${baseURL}/admin/kyc/${id}/approve`, {
        method: 'POST',
        headers: {
          Authorization: `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });

      if (!res.ok) throw new Error(`Approval failed (status ${res.status})`);

      setShowDetailModal(false);
      fetchStats();
      fetchRequests(true);
    } catch (err) {
      alert(err.message || 'Error approving request');
    } finally {
      setActionLoading(false);
    }
  };

  // Handle Reject
  const handleRejectSubmit = async () => {
    if (!selectedRequest) return;
    const token = getToken();
    if (!token) return;

    try {
      setActionLoading(true);
      const { baseURL } = getApiConfig();
      const res = await fetch(`${baseURL}/admin/kyc/${selectedRequest.id}/reject`, {
        method: 'POST',
        headers: {
          Authorization: `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ reason: rejectReason || 'Document details verification failed' })
      });

      if (!res.ok) throw new Error(`Rejection failed (status ${res.status})`);

      setShowRejectModal(false);
      setShowDetailModal(false);
      setRejectReason('');
      fetchStats();
      fetchRequests(true);
    } catch (err) {
      alert(err.message || 'Error rejecting request');
    } finally {
      setActionLoading(false);
    }
  };

  // Helper status badge renderer
  const renderStatusBadge = (status, label) => {
    switch (status) {
      case 'VERIFIED_BY_API':
        return (
          <span className="inline-flex items-center gap-1.5 px-3 py-1 bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 rounded-full text-xs font-semibold">
            <Shield size={14} />
            {label || 'Verified by API Key'}
          </span>
        );
      case 'VERIFIED_BY_ADMIN':
        return (
          <span className="inline-flex items-center gap-1.5 px-3 py-1 bg-blue-500/10 text-blue-400 border border-blue-500/20 rounded-full text-xs font-semibold">
            <CheckCircle size={14} />
            {label || 'Verified by Admin'}
          </span>
        );
      case 'PENDING_ADMIN':
        return (
          <span className="inline-flex items-center gap-1.5 px-3 py-1 bg-amber-500/10 text-amber-400 border border-amber-500/20 rounded-full text-xs font-semibold">
            <Clock size={14} />
            {label || 'Pending Admin Verification'}
          </span>
        );
      case 'REJECTED':
        return (
          <span className="inline-flex items-center gap-1.5 px-3 py-1 bg-red-500/10 text-red-400 border border-red-500/20 rounded-full text-xs font-semibold">
            <XCircle size={14} />
            {label || 'Rejected'}
          </span>
        );
      default:
        return (
          <span className="inline-flex items-center gap-1.5 px-3 py-1 bg-gray-700 text-gray-300 rounded-full text-xs font-semibold">
            {status}
          </span>
        );
    }
  };

  return (
    <AdminLayout>
      <main className="p-6">
        {/* Page Header */}
        <div className="bg-gray-800 border border-orange-500/20 rounded-xl p-6 mb-8 flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
          <div>
            <h1 className="text-2xl font-bold text-white mb-1 flex items-center gap-2">
              <UserCheck className="text-orange-400" size={28} />
              Driver KYC Approval & Document Verification
            </h1>
            <p className="text-orange-300/80 text-sm">
              Review Aadhaar card and Driving License (DL) verifications sent for manual Admin approval
            </p>
          </div>
          <button
            onClick={() => { fetchStats(); fetchRequests(); }}
            className="flex items-center gap-2 px-4 py-2 bg-gray-700 hover:bg-gray-600 text-white rounded-lg text-sm transition-colors border border-gray-600"
          >
            <RefreshCw size={16} className={loading ? 'animate-spin' : ''} />
            Refresh
          </button>
        </div>

        {/* Stats Grid */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
          <div className="bg-gray-800 border border-amber-500/30 rounded-xl p-5">
            <div className="flex justify-between items-center mb-2">
              <span className="text-xs font-semibold text-amber-400 uppercase tracking-wider">Pending Review</span>
              <Clock size={20} className="text-amber-400" />
            </div>
            <div className="text-2xl font-bold text-white">{stats.pendingCount || 0}</div>
            <p className="text-xs text-gray-400 mt-1">Awaiting manual admin approval</p>
          </div>

          <div className="bg-gray-800 border border-blue-500/30 rounded-xl p-5">
            <div className="flex justify-between items-center mb-2">
              <span className="text-xs font-semibold text-blue-400 uppercase tracking-wider">Aadhaar Pending</span>
              <FileText size={20} className="text-blue-400" />
            </div>
            <div className="text-2xl font-bold text-white">{stats.aadhaarPending || 0}</div>
            <p className="text-xs text-gray-400 mt-1">Aadhaar verification requests</p>
          </div>

          <div className="bg-gray-800 border border-purple-500/30 rounded-xl p-5">
            <div className="flex justify-between items-center mb-2">
              <span className="text-xs font-semibold text-purple-400 uppercase tracking-wider">DL Pending</span>
              <Shield size={20} className="text-purple-400" />
            </div>
            <div className="text-2xl font-bold text-white">{stats.dlPending || 0}</div>
            <p className="text-xs text-gray-400 mt-1">Driving license requests</p>
          </div>

          <div className="bg-gray-800 border border-emerald-500/30 rounded-xl p-5">
            <div className="flex justify-between items-center mb-2">
              <span className="text-xs font-semibold text-emerald-400 uppercase tracking-wider">Total Verified</span>
              <CheckCircle size={20} className="text-emerald-400" />
            </div>
            <div className="text-2xl font-bold text-white">{stats.totalVerified || 0}</div>
            <p className="text-xs text-gray-400 mt-1">API: {stats.verifiedApi || 0} | Admin: {stats.verifiedAdmin || 0}</p>
          </div>
        </div>

        {/* Search & Filters Section */}
        <div className="bg-gray-800 border border-orange-500/20 rounded-xl p-6 mb-6">
          <div className="grid grid-cols-1 md:grid-cols-12 gap-4">
            {/* Search Input */}
            <div className="md:col-span-6 relative">
              <Search size={18} className="absolute left-3 top-1/2 -translate-y-1/2 text-orange-400" />
              <input
                type="text"
                placeholder="Search by driver name, email, phone, Aadhaar, or DL number..."
                value={searchTerm}
                onChange={(e) => { setSearchTerm(e.target.value); setPage(0); }}
                className="w-full pl-10 pr-4 py-2.5 bg-gray-700 border border-orange-500/30 rounded-lg focus:outline-none focus:ring-2 focus:ring-orange-500 text-white text-sm placeholder-gray-400"
              />
            </div>

            {/* Status Filter */}
            <div className="md:col-span-3">
              <select
                value={statusFilter}
                onChange={(e) => { setStatusFilter(e.target.value); setPage(0); }}
                className="w-full px-3 py-2.5 bg-gray-700 border border-orange-500/30 rounded-lg focus:outline-none focus:ring-2 focus:ring-orange-500 text-white text-sm"
              >
                <option value="ALL">All Statuses</option>
                <option value="PENDING_ADMIN">Pending Admin Verification</option>
                <option value="VERIFIED_BY_ADMIN">Verified by Admin</option>
                <option value="VERIFIED_BY_API">Verified by API Key</option>
                <option value="REJECTED">Rejected</option>
              </select>
            </div>

            {/* Document Type Filter */}
            <div className="md:col-span-3">
              <select
                value={typeFilter}
                onChange={(e) => { setTypeFilter(e.target.value); setPage(0); }}
                className="w-full px-3 py-2.5 bg-gray-700 border border-orange-500/30 rounded-lg focus:outline-none focus:ring-2 focus:ring-orange-500 text-white text-sm"
              >
                <option value="ALL">All Document Types</option>
                <option value="AADHAAR">Aadhaar Card</option>
                <option value="DRIVING_LICENSE">Driving License (DL)</option>
              </select>
            </div>
          </div>
        </div>

        {/* Requests Table Section */}
        <div className="bg-gray-800 border border-orange-500/20 rounded-xl overflow-hidden shadow-xl">
          {error && (
            <div className="p-4 bg-red-500/10 border-b border-red-500/20 text-red-400 text-sm flex items-center gap-2">
              <AlertTriangle size={18} />
              {error}
            </div>
          )}

          {loading ? (
            <div className="py-16 text-center">
              <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-orange-500 mx-auto mb-4"></div>
              <p className="text-orange-300 text-sm">Loading driver KYC requests...</p>
            </div>
          ) : requests.length === 0 ? (
            <div className="text-center py-16 px-4">
              <UserCheck size={48} className="text-orange-400/50 mx-auto mb-4" />
              <h3 className="text-lg font-semibold text-white mb-1">No KYC Requests Found</h3>
              <p className="text-gray-400 text-sm max-w-md mx-auto">
                No verification requests matched your search criteria or status filter.
              </p>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm text-gray-300">
                <thead className="bg-gray-900/60 text-xs uppercase tracking-wider text-orange-400 border-b border-orange-500/20">
                  <tr>
                    <th className="px-6 py-4">Driver Info</th>
                    <th className="px-6 py-4">Document Type</th>
                    <th className="px-6 py-4">Document Number</th>
                    <th className="px-6 py-4">Verification Status</th>
                    <th className="px-6 py-4">Submitted At</th>
                    <th className="px-6 py-4 text-right">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-700/50">
                  {requests.map((item) => (
                    <tr key={item.id} className="hover:bg-gray-700/40 transition-colors">
                      {/* Driver Info */}
                      <td className="px-6 py-4">
                        <div className="font-semibold text-white">{item.driverName || 'N/A'}</div>
                        <div className="text-xs text-gray-400">{item.phone || item.email || 'No contact'}</div>
                      </td>

                      {/* Document Type */}
                      <td className="px-6 py-4">
                        <span className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md text-xs font-semibold ${
                          item.kycType === 'AADHAAR'
                            ? 'bg-blue-500/10 text-blue-400 border border-blue-500/20'
                            : 'bg-purple-500/10 text-purple-400 border border-purple-500/20'
                        }`}>
                          <FileText size={14} />
                          {item.kycType === 'AADHAAR' ? 'Aadhaar Card' : 'Driving License (DL)'}
                        </span>
                      </td>

                      {/* Document Number */}
                      <td className="px-6 py-4 font-mono text-white text-xs">
                        {item.documentNumber}
                      </td>

                      {/* Status */}
                      <td className="px-6 py-4">
                        {renderStatusBadge(item.status, item.statusLabel)}
                      </td>

                      {/* Submitted At */}
                      <td className="px-6 py-4 text-xs text-gray-400">
                        {item.submittedAt ? new Date(item.submittedAt).toLocaleString() : 'N/A'}
                      </td>

                      {/* Actions */}
                      <td className="px-6 py-4 text-right space-x-2">
                        <button
                          onClick={() => { setSelectedRequest(item); setShowDetailModal(true); }}
                          className="px-3 py-1.5 bg-gray-700 hover:bg-gray-600 text-white rounded text-xs transition-colors inline-flex items-center gap-1"
                          title="View Details"
                        >
                          <Eye size={14} />
                          View
                        </button>

                        {item.status === 'PENDING_ADMIN' && (
                          <>
                            <button
                              onClick={() => handleApprove(item.id)}
                              disabled={actionLoading}
                              className="px-3 py-1.5 bg-emerald-600 hover:bg-emerald-500 text-white rounded text-xs font-medium transition-colors inline-flex items-center gap-1"
                            >
                              <CheckCircle size={14} />
                              Approve
                            </button>

                            <button
                              onClick={() => { setSelectedRequest(item); setShowRejectModal(true); }}
                              disabled={actionLoading}
                              className="px-3 py-1.5 bg-red-600 hover:bg-red-500 text-white rounded text-xs font-medium transition-colors inline-flex items-center gap-1"
                            >
                              <XCircle size={14} />
                              Reject
                            </button>
                          </>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          {/* Pagination Footer */}
          {totalPages > 1 && (
            <div className="px-6 py-4 bg-gray-900/40 border-t border-orange-500/20 flex items-center justify-between text-xs text-gray-400">
              <div>
                Showing page <span className="font-semibold text-white">{page + 1}</span> of{' '}
                <span className="font-semibold text-white">{totalPages}</span> ({totalElements} requests)
              </div>
              <div className="flex gap-2">
                <button
                  onClick={() => setPage(p => Math.max(0, p - 1))}
                  disabled={page === 0}
                  className="px-3 py-1.5 bg-gray-700 hover:bg-gray-600 disabled:opacity-40 text-white rounded flex items-center gap-1"
                >
                  <ChevronLeft size={14} />
                  Prev
                </button>
                <button
                  onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}
                  disabled={page >= totalPages - 1}
                  className="px-3 py-1.5 bg-gray-700 hover:bg-gray-600 disabled:opacity-40 text-white rounded flex items-center gap-1"
                >
                  Next
                  <ChevronRight size={14} />
                </button>
              </div>
            </div>
          )}
        </div>

        {/* View Detail Modal */}
        {showDetailModal && selectedRequest && (
          <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm">
            <div className="bg-gray-800 border border-orange-500/30 rounded-xl max-w-lg w-full p-6 shadow-2xl relative">
              <h3 className="text-xl font-bold text-white mb-4 flex items-center gap-2">
                <FileText className="text-orange-400" />
                KYC Verification Request Details
              </h3>

              <div className="space-y-3 text-sm text-gray-300">
                <div className="bg-gray-700/50 p-3 rounded-lg flex justify-between items-center">
                  <span className="text-gray-400">Driver Name:</span>
                  <span className="font-semibold text-white">{selectedRequest.driverName}</span>
                </div>

                <div className="bg-gray-700/50 p-3 rounded-lg flex justify-between items-center">
                  <span className="text-gray-400">Email / Phone:</span>
                  <span className="font-semibold text-white">{selectedRequest.phone || selectedRequest.email}</span>
                </div>

                <div className="bg-gray-700/50 p-3 rounded-lg flex justify-between items-center">
                  <span className="text-gray-400">Verification Purpose:</span>
                  <span className="font-semibold text-orange-300">
                    {selectedRequest.kycType === 'AADHAAR' ? 'Aadhaar Card Verification' : 'Driving License Verification'}
                  </span>
                </div>

                <div className="bg-gray-700/50 p-3 rounded-lg flex justify-between items-center">
                  <span className="text-gray-400">Document Number:</span>
                  <span className="font-mono font-bold text-white">{selectedRequest.documentNumber}</span>
                </div>

                <div className="bg-gray-700/50 p-3 rounded-lg flex justify-between items-center">
                  <span className="text-gray-400">Current Status:</span>
                  {renderStatusBadge(selectedRequest.status, selectedRequest.statusLabel)}
                </div>

                {selectedRequest.rejectionReason && (
                  <div className="bg-red-500/10 border border-red-500/30 p-3 rounded-lg">
                    <span className="text-xs text-red-400 font-semibold uppercase">Rejection Reason:</span>
                    <p className="text-sm text-red-300 mt-1">{selectedRequest.rejectionReason}</p>
                  </div>
                )}
              </div>

              <div className="mt-6 flex justify-end gap-3">
                <button
                  onClick={() => setShowDetailModal(false)}
                  className="px-4 py-2 bg-gray-700 hover:bg-gray-600 text-white rounded-lg text-sm"
                >
                  Close
                </button>
                {selectedRequest.status === 'PENDING_ADMIN' && (
                  <>
                    <button
                      onClick={() => handleApprove(selectedRequest.id)}
                      disabled={actionLoading}
                      className="px-4 py-2 bg-emerald-600 hover:bg-emerald-500 text-white rounded-lg text-sm font-semibold"
                    >
                      Approve
                    </button>
                    <button
                      onClick={() => setShowRejectModal(true)}
                      disabled={actionLoading}
                      className="px-4 py-2 bg-red-600 hover:bg-red-500 text-white rounded-lg text-sm font-semibold"
                    >
                      Reject
                    </button>
                  </>
                )}
              </div>
            </div>
          </div>
        )}

        {/* Reject Reason Modal */}
        {showRejectModal && selectedRequest && (
          <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm">
            <div className="bg-gray-800 border border-red-500/30 rounded-xl max-w-md w-full p-6 shadow-2xl">
              <h3 className="text-lg font-bold text-white mb-2 flex items-center gap-2">
                <XCircle className="text-red-400" />
                Reject Verification Request
              </h3>
              <p className="text-xs text-gray-400 mb-4">
                Specify why this {selectedRequest.kycType} verification for {selectedRequest.driverName} is being rejected.
              </p>

              <textarea
                rows={3}
                placeholder="Enter rejection reason (e.g., Invalid document number, name mismatch...)"
                value={rejectReason}
                onChange={(e) => setRejectReason(e.target.value)}
                className="w-full p-3 bg-gray-700 border border-gray-600 rounded-lg text-white text-sm focus:outline-none focus:border-red-500 mb-4 placeholder-gray-400"
              />

              <div className="flex justify-end gap-3">
                <button
                  onClick={() => setShowRejectModal(false)}
                  className="px-4 py-2 bg-gray-700 hover:bg-gray-600 text-white rounded-lg text-sm"
                >
                  Cancel
                </button>
                <button
                  onClick={handleRejectSubmit}
                  disabled={actionLoading}
                  className="px-4 py-2 bg-red-600 hover:bg-red-500 text-white rounded-lg text-sm font-semibold"
                >
                  Confirm Rejection
                </button>
              </div>
            </div>
          </div>
        )}
      </main>
    </AdminLayout>
  );
};

export default DriverKYC;
