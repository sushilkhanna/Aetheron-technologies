import React, { useState, useEffect, useCallback, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  AlertTriangle, RefreshCw, X, CheckCircle, XCircle,
  Clock, MapPin, Phone, User, Bike, Shield, Eye,
  Navigation, AlertCircle
} from 'lucide-react';
import AdminLayout from '../../components/AdminLayout';
import getApiConfig from '../../config/api';

// ─── Leaflet ─────────────────────────────────────────────────────────────────
import { MapContainer, TileLayer, Marker, Popup, Polyline, useMap } from 'react-leaflet';
import L from 'leaflet';

delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-icon-2x.png',
  iconUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-icon.png',
  shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-shadow.png',
});

const sosIcon = new L.DivIcon({
  className: '',
  html: `<div style="background:#ef4444;width:36px;height:36px;border-radius:50%;border:3px solid white;box-shadow:0 2px 12px rgba(239,68,68,0.6);display:flex;align-items:center;justify-content:center;animation:pulse 1s infinite;">
    <svg width="18" height="18" viewBox="0 0 24 24" fill="white"><path d="M12 2L22 20H2L12 2Z"/><line x1="12" y1="9" x2="12" y2="13" stroke="red" stroke-width="2"/><circle cx="12" cy="16" r="1" fill="red"/></svg>
  </div>`,
  iconSize: [36, 36],
  iconAnchor: [18, 18],
});

// ─── Helpers ──────────────────────────────────────────────────────────────────

const SOS_POLL_MS = 15_000; // re-fetch active alerts every 15s

const isValidLatLng = (lat, lng) =>
  typeof lat === 'number' && typeof lng === 'number' &&
  !isNaN(lat) && !isNaN(lng) &&
  lat >= -90 && lat <= 90 && lng >= -180 && lng <= 180 &&
  !(lat === 0 && lng === 0);

const formatDateTime = (iso) => {
  if (!iso) return '—';
  return new Date(iso).toLocaleString('en-IN', {
    day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit', hour12: true
  });
};

const timeAgo = (iso) => {
  if (!iso) return '';
  const diff = Math.floor((Date.now() - new Date(iso).getTime()) / 1000);
  if (diff < 60) return `${diff}s ago`;
  if (diff < 3600) return `${Math.floor(diff / 60)}m ago`;
  return `${Math.floor(diff / 3600)}h ago`;
};

const STATUS_COLOR = {
  TRIGGERED:  { bg: 'bg-red-500/15', border: 'border-red-500/30', text: 'text-red-400', dot: 'bg-red-400', label: 'Active SOS' },
  RESOLVED:   { bg: 'bg-green-500/15', border: 'border-green-500/30', text: 'text-green-400', dot: 'bg-green-400', label: 'Resolved' },
  FALSE_ALARM:{ bg: 'bg-yellow-500/15', border: 'border-yellow-500/30', text: 'text-yellow-400', dot: 'bg-yellow-400', label: 'False Alarm' },
  EXPIRED:    { bg: 'bg-gray-500/15', border: 'border-gray-500/30', text: 'text-gray-400', dot: 'bg-gray-400', label: 'Expired' },
};

const SosBadge = ({ status }) => {
  const cfg = STATUS_COLOR[status] || STATUS_COLOR.EXPIRED;
  return (
    <span className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold border ${cfg.bg} ${cfg.border} ${cfg.text}`}>
      <span className={`w-1.5 h-1.5 rounded-full ${cfg.dot} ${status === 'TRIGGERED' ? 'animate-pulse' : ''}`} />
      {cfg.label}
    </span>
  );
};

// ─── Trail Map Component ──────────────────────────────────────────────────────

const TrailMap = ({ trail, alert }) => {
  const validPoints = trail
    .map(p => [parseFloat(p.latitude), parseFloat(p.longitude)])
    .filter(([lat, lng]) => isValidLatLng(lat, lng));

  const lastPoint = validPoints[validPoints.length - 1];

  if (!lastPoint) {
    return (
      <div className="flex items-center justify-center h-full bg-gray-900 rounded-lg">
        <p className="text-orange-300/60 text-sm">No location data available</p>
      </div>
    );
  }

  return (
    <MapContainer center={lastPoint} zoom={15} style={{ height: '100%', width: '100%' }} zoomControl>
      <TileLayer
        url="https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png"
        attribution='&copy; <a href="https://carto.com/">CARTO</a>'
      />
      {/* Last known position marker */}
      <Marker position={lastPoint} icon={sosIcon}>
        <Popup>
          <div style={{ color: '#333', fontSize: '12px' }}>
            <strong>🚨 SOS Location</strong><br />
            {alert?.triggeredByName}<br />
            <small>Last update: {trail[trail.length - 1]?.recordedAt ? formatDateTime(trail[trail.length - 1].recordedAt) : '—'}</small>
          </div>
        </Popup>
      </Marker>
      {/* Trail polyline */}
      {validPoints.length >= 2 && (
        <Polyline
          positions={validPoints}
          pathOptions={{ color: '#ef4444', weight: 3, opacity: 0.8, dashArray: '6,6' }}
        />
      )}
    </MapContainer>
  );
};

// ─── SOS Detail Modal ─────────────────────────────────────────────────────────

const SosDetailModal = ({ alert, onClose, onResolve }) => {
  const [trail, setTrail] = useState([]);
  const [trailLoading, setTrailLoading] = useState(true);
  const [resolving, setResolving] = useState(false);
  const trailPollRef = useRef(null);

  const fetchTrail = useCallback(async () => {
    const token = localStorage.getItem('adminToken');
    if (!token || !alert) return;
    try {
      const { baseURL } = getApiConfig();
      const res = await fetch(`${baseURL}/sos/${alert.alertId}/trail`, {
        headers: { 'Authorization': `Bearer ${token}` }
      });
      if (res.ok) {
        const json = await res.json();
        setTrail(json.data || []);
      }
    } catch (err) {
      console.error('Trail fetch failed:', err);
    } finally {
      setTrailLoading(false);
    }
  }, [alert]);

  useEffect(() => {
    fetchTrail();
    if (alert?.status === 'TRIGGERED') {
      trailPollRef.current = setInterval(fetchTrail, 5000);
    }
    return () => clearInterval(trailPollRef.current);
  }, [fetchTrail, alert]);

  const handleResolve = async (falseAlarm = false) => {
    const token = localStorage.getItem('adminToken');
    if (!token) return;
    setResolving(true);
    try {
      const { baseURL } = getApiConfig();
      const endpoint = falseAlarm ? 'false-alarm' : 'resolve';
      const res = await fetch(`${baseURL}/sos/${alert.alertId}/${endpoint}`, {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${token}` }
      });
      if (res.ok) {
        onResolve();
        onClose();
      }
    } catch (err) {
      console.error('Resolve failed:', err);
    } finally {
      setResolving(false);
    }
  };

  if (!alert) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div className="absolute inset-0 bg-black/70 backdrop-blur-sm" onClick={onClose} />
      <div className="relative bg-gray-800 border border-red-500/30 rounded-2xl max-w-2xl w-full max-h-[90vh] overflow-y-auto shadow-2xl shadow-red-500/10">
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-red-500/20 bg-red-500/5">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 bg-red-500 rounded-lg flex items-center justify-center">
              <AlertTriangle size={20} className="text-white" />
            </div>
            <div>
              <h3 className="text-white font-semibold">SOS Alert #{alert.alertId}</h3>
              <p className="text-xs text-red-300/60">Ride #{alert.rideId} · {formatDateTime(alert.triggeredAt)}</p>
            </div>
          </div>
          <div className="flex items-center gap-2">
            <SosBadge status={alert.status} />
            <button onClick={onClose} className="p-2 hover:bg-gray-700 rounded-lg transition-colors ml-2">
              <X size={20} className="text-orange-400" />
            </button>
          </div>
        </div>

        <div className="p-6 space-y-6">
          {/* Who triggered it */}
          <div className="grid grid-cols-2 gap-3">
            <div className="p-4 bg-gray-900/60 rounded-xl border border-red-500/10">
              <p className="text-xs text-orange-400/60 uppercase tracking-wider mb-2">Triggered By</p>
              <p className="text-white font-semibold">{alert.triggeredByName}</p>
              <p className="text-xs text-orange-300/50 mt-1 capitalize">{alert.triggeredByRole?.toLowerCase()}</p>
            </div>
            <div className="p-4 bg-gray-900/60 rounded-xl border border-red-500/10">
              <p className="text-xs text-orange-400/60 uppercase tracking-wider mb-2">Time</p>
              <p className="text-white font-semibold">{formatDateTime(alert.triggeredAt)}</p>
              <p className="text-xs text-red-400/60 mt-1 animate-pulse">{timeAgo(alert.triggeredAt)}</p>
            </div>
          </div>

          {/* Live trail map */}
          <div>
            <p className="text-sm font-semibold text-orange-400 uppercase tracking-wider mb-3 flex items-center gap-2">
              <Navigation size={14} />
              Location Trail
              {alert.status === 'TRIGGERED' && (
                <span className="text-xs text-green-400 font-normal flex items-center gap-1">
                  <span className="w-1.5 h-1.5 bg-green-400 rounded-full animate-pulse" />
                  Live · updates every 5s
                </span>
              )}
            </p>
            <div style={{ height: '280px' }} className="rounded-xl overflow-hidden border border-red-500/20">
              {trailLoading ? (
                <div className="flex items-center justify-center h-full bg-gray-900">
                  <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-red-500" />
                </div>
              ) : (
                <TrailMap trail={trail} alert={alert} />
              )}
            </div>
            <p className="text-xs text-orange-300/40 mt-2">
              {trail.length} location point{trail.length !== 1 ? 's' : ''} recorded
            </p>
          </div>

          {/* Resolution actions — only for active alerts */}
          {alert.status === 'TRIGGERED' && (
            <div className="border-t border-red-500/20 pt-5">
              <p className="text-sm font-semibold text-orange-400 uppercase tracking-wider mb-3">Admin Actions</p>
              <div className="grid grid-cols-2 gap-3">
                <button
                  onClick={() => handleResolve(false)}
                  disabled={resolving}
                  className="flex items-center justify-center gap-2 px-4 py-3 bg-green-500/10 hover:bg-green-500/20 text-green-400 rounded-xl border border-green-500/20 transition-colors font-medium disabled:opacity-50"
                >
                  <CheckCircle size={18} />
                  Mark Resolved
                </button>
                <button
                  onClick={() => handleResolve(true)}
                  disabled={resolving}
                  className="flex items-center justify-center gap-2 px-4 py-3 bg-yellow-500/10 hover:bg-yellow-500/20 text-yellow-400 rounded-xl border border-yellow-500/20 transition-colors font-medium disabled:opacity-50"
                >
                  <XCircle size={18} />
                  False Alarm
                </button>
              </div>
              <p className="text-xs text-orange-300/40 mt-3 text-center">
                Resolving will notify the user and emergency contacts
              </p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

// ═══════════════════════════════════════════════════════════════════════════════
//  MAIN COMPONENT
// ═══════════════════════════════════════════════════════════════════════════════

const SosManagement = () => {
  const navigate = useNavigate();
  const [alerts, setAlerts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedAlert, setSelectedAlert] = useState(null);
  const [lastRefresh, setLastRefresh] = useState(null);
  const pollRef = useRef(null);

  const fetchAlerts = useCallback(async (silent = false) => {
    const token = localStorage.getItem('adminToken');
    if (!token) { navigate('/'); return; }

    if (!silent) setLoading(true);
    try {
      const { baseURL } = getApiConfig();
      const res = await fetch(`${baseURL}/sos/active`, {
        headers: { 'Authorization': `Bearer ${token}` }
      });
      if (res.status === 401 || res.status === 403) {
        localStorage.removeItem('adminToken');
        navigate('/'); return;
      }
      if (res.ok) {
        const json = await res.json();
        setAlerts(json.data || []);
        setLastRefresh(new Date());
      }
    } catch (err) {
      console.error('Failed to fetch SOS alerts:', err);
    } finally {
      if (!silent) setLoading(false);
    }
  }, [navigate]);

  useEffect(() => {
    fetchAlerts();
    pollRef.current = setInterval(() => fetchAlerts(true), SOS_POLL_MS);
    return () => clearInterval(pollRef.current);
  }, [fetchAlerts]);

  const activeCount = alerts.filter(a => a.status === 'TRIGGERED').length;

  return (
    <AdminLayout>
      <main className="p-6">
        {/* Page Header */}
        <div className={`border rounded-xl p-6 mb-6 ${
          activeCount > 0
            ? 'bg-red-500/5 border-red-500/30'
            : 'bg-gray-800 border-orange-500/20'
        }`}>
          <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
            <div className="flex items-center gap-4">
              <div className={`w-14 h-14 rounded-xl flex items-center justify-center shadow-lg flex-shrink-0 ${
                activeCount > 0
                  ? 'bg-red-500 shadow-red-500/30'
                  : 'bg-gray-700'
              }`}>
                <AlertTriangle size={28} className="text-white" />
              </div>
              <div>
                <h1 className="text-2xl font-bold text-white mb-1">SOS Monitoring</h1>
                <p className="text-orange-300 text-sm flex items-center gap-2">
                  {activeCount > 0 ? (
                    <>
                      <span className="w-2 h-2 bg-red-400 rounded-full animate-pulse" />
                      <span className="text-red-400 font-semibold">{activeCount} active alert{activeCount !== 1 ? 's' : ''} need attention</span>
                    </>
                  ) : (
                    <>
                      <span className="w-2 h-2 bg-green-400 rounded-full" />
                      All clear · No active emergencies
                    </>
                  )}
                </p>
              </div>
            </div>
            <div className="flex items-center gap-3">
              {lastRefresh && (
                <p className="text-xs text-orange-300/40">
                  Refreshed {timeAgo(lastRefresh)}
                </p>
              )}
              <button
                onClick={() => fetchAlerts()}
                disabled={loading}
                className="inline-flex items-center gap-2 px-4 py-2.5 bg-orange-500/10 hover:bg-orange-500/20 text-orange-400 rounded-lg transition-colors border border-orange-500/20 disabled:opacity-50"
              >
                <RefreshCw size={16} className={loading ? 'animate-spin' : ''} />
                <span className="font-medium text-sm">Refresh</span>
              </button>
            </div>
          </div>
        </div>

        {/* Active SOS Alert Banner */}
        {activeCount > 0 && (
          <div className="bg-red-500/10 border border-red-500/40 rounded-xl p-4 mb-6 flex items-center gap-3">
            <AlertTriangle size={20} className="text-red-400 flex-shrink-0 animate-pulse" />
            <p className="text-red-300 font-medium text-sm">
              {activeCount} emergency alert{activeCount !== 1 ? 's are' : ' is'} currently active.
              Click "View" to see location trail and resolve.
            </p>
          </div>
        )}

        {/* Stats Row */}
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
          {[
            { label: 'Active', value: alerts.filter(a => a.status === 'TRIGGERED').length, color: 'text-red-400', bg: 'bg-red-500/10', border: 'border-red-500/20' },
            { label: 'Resolved', value: alerts.filter(a => a.status === 'RESOLVED').length, color: 'text-green-400', bg: 'bg-green-500/10', border: 'border-green-500/20' },
            { label: 'False Alarms', value: alerts.filter(a => a.status === 'FALSE_ALARM').length, color: 'text-yellow-400', bg: 'bg-yellow-500/10', border: 'border-yellow-500/20' },
            { label: 'Expired', value: alerts.filter(a => a.status === 'EXPIRED').length, color: 'text-gray-400', bg: 'bg-gray-500/10', border: 'border-gray-500/20' },
          ].map(({ label, value, color, bg, border }) => (
            <div key={label} className={`${bg} border ${border} rounded-xl p-4 text-center`}>
              <p className={`text-3xl font-bold ${color}`}>{value}</p>
              <p className="text-xs text-orange-300/60 mt-1 font-medium">{label}</p>
            </div>
          ))}
        </div>

        {/* Note: /api/sos/active only returns TRIGGERED alerts.
            The stats above will show 0 for non-active until you add a history endpoint. */}

        {/* Alerts List */}
        <div className="bg-gray-800 border border-orange-500/20 rounded-xl overflow-hidden">
          <div className="px-6 py-4 border-b border-orange-500/20 flex items-center justify-between">
            <h3 className="text-white font-semibold flex items-center gap-2">
              <AlertTriangle size={18} className="text-red-400" />
              Active SOS Alerts
              {activeCount > 0 && (
                <span className="px-2 py-0.5 bg-red-500 text-white text-xs rounded-full animate-pulse font-bold">
                  {activeCount}
                </span>
              )}
            </h3>
            <p className="text-xs text-orange-300/40">Auto-refreshes every 15s</p>
          </div>

          {loading ? (
            <div className="p-12 flex items-center justify-center">
              <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-red-500" />
            </div>
          ) : alerts.length === 0 ? (
            <div className="p-16 text-center">
              <div className="w-20 h-20 mx-auto mb-4 bg-green-500/10 rounded-full flex items-center justify-center">
                <Shield size={36} className="text-green-400" />
              </div>
              <h3 className="text-lg font-medium text-white mb-2">No Active SOS Alerts</h3>
              <p className="text-orange-300/60 text-sm">All riders are safe. No emergencies at this time.</p>
            </div>
          ) : (
            <div className="divide-y divide-orange-500/10">
              {alerts.map((alert) => (
                <div key={alert.alertId}
                  className={`p-5 hover:bg-gray-700/30 transition-colors ${
                    alert.status === 'TRIGGERED' ? 'border-l-4 border-red-500' : ''
                  }`}>
                  <div className="flex items-start justify-between gap-4">
                    <div className="flex items-start gap-4 flex-1 min-w-0">
                      {/* Icon */}
                      <div className={`w-10 h-10 rounded-xl flex items-center justify-center flex-shrink-0 ${
                        alert.status === 'TRIGGERED' ? 'bg-red-500' : 'bg-gray-700'
                      }`}>
                        <AlertTriangle size={20} className="text-white" />
                      </div>

                      {/* Details */}
                      <div className="min-w-0 flex-1">
                        <div className="flex items-center gap-3 flex-wrap mb-2">
                          <span className="text-white font-semibold">Alert #{alert.alertId}</span>
                          <SosBadge status={alert.status} />
                          <span className="text-xs text-orange-300/40">Ride #{alert.rideId}</span>
                        </div>

                        <div className="grid grid-cols-1 sm:grid-cols-3 gap-2 text-sm">
                          <div className="flex items-center gap-2 text-orange-300/70">
                            <User size={14} className="text-orange-400/60 flex-shrink-0" />
                            <span className="truncate">
                              {alert.triggeredByName}
                              <span className="text-orange-400/40 ml-1 capitalize">
                                ({alert.triggeredByRole?.toLowerCase()})
                              </span>
                            </span>
                          </div>
                          <div className="flex items-center gap-2 text-orange-300/70">
                            <Clock size={14} className="text-orange-400/60 flex-shrink-0" />
                            <span>{formatDateTime(alert.triggeredAt)}</span>
                          </div>
                          {alert.status === 'TRIGGERED' && (
                            <div className="flex items-center gap-1.5 text-red-400/80">
                              <span className="w-1.5 h-1.5 bg-red-400 rounded-full animate-pulse" />
                              <span className="text-xs font-medium">{timeAgo(alert.triggeredAt)}</span>
                            </div>
                          )}
                        </div>
                      </div>
                    </div>

                    {/* View button */}
                    <button
                      onClick={() => setSelectedAlert(alert)}
                      className={`flex items-center gap-2 px-4 py-2 rounded-lg transition-colors border text-sm font-medium flex-shrink-0 ${
                        alert.status === 'TRIGGERED'
                          ? 'bg-red-500/10 hover:bg-red-500/20 text-red-400 border-red-500/30'
                          : 'bg-orange-500/10 hover:bg-orange-500/20 text-orange-400 border-orange-500/20'
                      }`}
                    >
                      <Eye size={16} />
                      View
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </main>

      {/* Detail Modal */}
      {selectedAlert && (
        <SosDetailModal
          alert={selectedAlert}
          onClose={() => setSelectedAlert(null)}
          onResolve={() => fetchAlerts()}
        />
      )}
    </AdminLayout>
  );
};

export default SosManagement;