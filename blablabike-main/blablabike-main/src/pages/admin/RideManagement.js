import React, { useState, useEffect, useCallback, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Bike, Search, Filter, Eye, RefreshCw, ChevronLeft, ChevronRight,
  XCircle, AlertCircle, ArrowUpDown, ArrowUp, ArrowDown, CheckCircle,
  MapPin, Navigation, Clock, DollarSign, Users, Activity, Radio,
  TrendingUp, X, Map as MapIcon, Shield,
} from 'lucide-react';
import AdminLayout from '../../components/AdminLayout';
import getApiConfig from '../../config/api';

import {
  Chart as ChartJS,
  CategoryScale, LinearScale, BarElement, PointElement, LineElement,
  Title, Tooltip, Legend, Filler
} from 'chart.js';
import { Bar } from 'react-chartjs-2';

import { MapContainer, TileLayer, Marker, Popup, Polyline, useMap } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';

ChartJS.register(
  CategoryScale, LinearScale, BarElement, PointElement, LineElement,
  Title, Tooltip, Legend, Filler
);

// Fix Leaflet default icon paths
delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-icon-2x.png',
  iconUrl:       'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-icon.png',
  shadowUrl:     'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-shadow.png',
});

// ── Custom marker icons ──────────────────────────────────────────────────────

const driverIcon = new L.DivIcon({
  className: '',
  html: `<div style="background:#FF7000;width:36px;height:36px;border-radius:50%;border:3px solid white;
    box-shadow:0 3px 12px rgba(255,112,0,0.5);display:flex;align-items:center;justify-content:center;">
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2.5">
      <path d="M12 2L19 21l-7-4-7 4z"/>
    </svg>
  </div>`,
  iconSize: [36, 36],
  iconAnchor: [18, 18],
});

const pickupIcon = new L.DivIcon({
  className: '',
  html: `<div style="background:#22c55e;width:32px;height:32px;border-radius:50%;border:3px solid white;
    box-shadow:0 3px 10px rgba(34,197,94,0.5);display:flex;align-items:center;justify-content:center;">
    <svg width="14" height="14" viewBox="0 0 24 24" fill="white">
      <path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z"/>
    </svg>
  </div>`,
  iconSize: [32, 32],
  iconAnchor: [16, 32],
});

const dropIcon = new L.DivIcon({
  className: '',
  html: `<div style="background:#ef4444;width:32px;height:32px;border-radius:50%;border:3px solid white;
    box-shadow:0 3px 10px rgba(239,68,68,0.5);display:flex;align-items:center;justify-content:center;">
    <svg width="14" height="14" viewBox="0 0 24 24" fill="white">
      <rect x="4" y="4" width="16" height="16" rx="3"/>
    </svg>
  </div>`,
  iconSize: [32, 32],
  iconAnchor: [16, 32],
});

// ── Constants ────────────────────────────────────────────────────────────────

const RIDE_STATES    = ['OPEN','LIVE','BOOKED','STARTED','VERIFIED','COMPLETED','CANCELLED','EXPIRED'];
const TABLE_POLL_MS  = 30_000;
const MAP_POLL_MS    = 3_000;

// ── Helpers ──────────────────────────────────────────────────────────────────

const STATUS_CONFIG = {
  OPEN:      { color: 'bg-blue-500/15 text-blue-400 border-blue-500/25',        dot: 'bg-blue-400',    label: 'Open' },
  LIVE:      { color: 'bg-purple-500/15 text-purple-400 border-purple-500/25',  dot: 'bg-purple-400',  label: 'Live', pulse: true },
  BOOKED:    { color: 'bg-cyan-500/15 text-cyan-400 border-cyan-500/25',        dot: 'bg-cyan-400',    label: 'Booked' },
  STARTED:   { color: 'bg-yellow-500/15 text-yellow-400 border-yellow-500/25',  dot: 'bg-yellow-400',  label: 'Started', pulse: true },
  VERIFIED:  { color: 'bg-emerald-500/15 text-emerald-400 border-emerald-500/25',dot:'bg-emerald-400', label: 'Verified', pulse: true },
  COMPLETED: { color: 'bg-green-500/15 text-green-400 border-green-500/25',     dot: 'bg-green-400',   label: 'Completed' },
  CANCELLED: { color: 'bg-red-500/15 text-red-400 border-red-500/25',           dot: 'bg-red-400',     label: 'Cancelled' },
  EXPIRED:   { color: 'bg-gray-500/15 text-gray-400 border-gray-500/25',        dot: 'bg-gray-400',    label: 'Expired' },
};

const StatusBadge = ({ state }) => {
  const cfg = STATUS_CONFIG[state] || STATUS_CONFIG.OPEN;
  return (
    <span className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium border ${cfg.color}`}>
      <span className={`w-1.5 h-1.5 rounded-full ${cfg.dot} ${cfg.pulse ? 'animate-pulse' : ''}`} />
      {cfg.label}
    </span>
  );
};

const formatDateTime = (iso) => {
  if (!iso) return '—';
  return new Date(iso).toLocaleString('en-IN', {
    day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit', hour12: true
  });
};

const isValidLatLng = (lat, lng) =>
  typeof lat === 'number' && typeof lng === 'number' &&
  !isNaN(lat) && !isNaN(lng) &&
  lat >= -90 && lat <= 90 && lng >= -180 && lng <= 180 &&
  !(lat === 0 && lng === 0);

// ── Map: smooth pan when driver moves & container resize handling ────────────

const MapController = ({ position }) => {
  const map = useMap();

  useEffect(() => {
    // Invalidate map size after mount so Leaflet resizes correctly in modal
    const timer = setTimeout(() => {
      if (map) {
        map.invalidateSize();
      }
    }, 200);
    return () => clearTimeout(timer);
  }, [map]);

  useEffect(() => {
    if (position && isValidLatLng(position[0], position[1])) {
      map.panTo(position, { animate: true, duration: 0.8 });
    }
  }, [map, position]);

  return null;
};

// ── Live Map Modal ───────────────────────────────────────────────────────────
// Uses OpenStreetMap (free, no key). Shows:
//   - Driver marker (orange, animated)
//   - Booker pickup marker (green pin)
//   - Booker drop marker (red square)
//   - Driven path so far (orange solid line)
//   - Remaining route to drop (orange dashed line)
//   - Auto-pans to follow driver every 3s

const LiveMapModal = ({ ride, locationRef, locationVersion, onClose }) => {
  // Read current location from ref (not state — avoids remounting map)
  const loc = locationRef.current;

  if (!ride) return null;

  // Waiting for first ping
  if (!loc) {
    return (
      <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
        <div className="absolute inset-0 bg-black/70 backdrop-blur-sm" onClick={onClose} />
        <div className="relative bg-gray-800 border border-orange-500/30 rounded-2xl p-12 text-center shadow-2xl">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-orange-500 mx-auto mb-4" />
          <p className="text-orange-300 font-medium">Waiting for driver location…</p>
          <p className="text-orange-300/50 text-sm mt-1">Updates every 3 seconds</p>
          <button onClick={onClose} className="mt-5 px-4 py-2 text-sm text-orange-400 hover:bg-gray-700 rounded-lg border border-orange-500/20 transition-colors">
            Cancel
          </button>
        </div>
      </div>
    );
  }

  const driverLat = parseFloat(loc.currentLat);
  const driverLng = parseFloat(loc.currentLng);

  if (!isValidLatLng(driverLat, driverLng)) {
    return (
      <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
        <div className="absolute inset-0 bg-black/70 backdrop-blur-sm" onClick={onClose} />
        <div className="relative bg-gray-800 border border-orange-500/30 rounded-2xl p-12 text-center shadow-2xl">
          <MapIcon size={48} className="text-orange-400 mx-auto mb-4" />
          <p className="text-white font-semibold mb-2">No valid location data</p>
          <p className="text-orange-300/60 text-sm mb-4">Driver hasn't sent coordinates yet.</p>
          <button onClick={onClose} className="px-4 py-2 text-sm text-orange-400 hover:bg-gray-700 rounded-lg border border-orange-500/20 transition-colors">
            Close
          </button>
        </div>
      </div>
    );
  }

  const driverPos  = [driverLat, driverLng];

  // Pickup = driver's route start (fromLat/fromLng)
  const pickupLat  = parseFloat(loc.fromLat);
  const pickupLng  = parseFloat(loc.fromLng);
  const pickupPos  = isValidLatLng(pickupLat, pickupLng) ? [pickupLat, pickupLng] : null;

  // Booker pickup (where booker boards)
  const bookerPickupLat = parseFloat(loc.bookerPickupLat);
  const bookerPickupLng = parseFloat(loc.bookerPickupLng);
  const bookerPickupPos = loc.bookerDropSet && isValidLatLng(bookerPickupLat, bookerPickupLng)
    ? [bookerPickupLat, bookerPickupLng] : null;

  // Booker drop
  const dropLat    = parseFloat(loc.bookerDropLat);
  const dropLng    = parseFloat(loc.bookerDropLng);
  const dropPos    = loc.bookerDropSet && isValidLatLng(dropLat, dropLng) ? [dropLat, dropLng] : null;

  // Ride destination (driver's toLat/toLng)
  const destLat    = parseFloat(loc.toLat);
  const destLng    = parseFloat(loc.toLng);
  const destPos    = isValidLatLng(destLat, destLng) ? [destLat, destLng] : null;

  // Path lines:
  //   solid orange = driver position to where they're heading next
  //   dashed orange = remaining to final destination
  const finalDest  = dropPos || destPos;
  const routeLine  = finalDest ? [driverPos, finalDest] : [];

  const lastUpdate = loc.lastUpdatedAt
    ? new Date(loc.lastUpdatedAt).toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: true })
    : '—';

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div className="absolute inset-0 bg-black/70 backdrop-blur-sm" onClick={onClose} />
      <div className="relative bg-gray-900 border border-orange-500/30 rounded-2xl max-w-3xl w-full overflow-hidden shadow-2xl shadow-orange-500/10">

        {/* Header */}
        <div className="flex items-center justify-between px-5 py-4 border-b border-orange-500/20 bg-gray-900">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 bg-orange-500 rounded-lg flex items-center justify-center">
              <Navigation size={20} className="text-white" />
            </div>
            <div>
              <h3 className="text-white font-semibold">Live Tracking — Ride #{ride.instanceId || ride.rideId || ride.id}</h3>
              <p className="text-xs text-orange-300/60">Driver: {ride.driverName} · Last update: {lastUpdate}</p>
            </div>
          </div>
          <div className="flex items-center gap-3">
            <div className="flex items-center gap-1.5 text-xs text-green-400 bg-green-500/10 px-3 py-1.5 rounded-full border border-green-500/20">
              <span className="w-2 h-2 bg-green-400 rounded-full animate-pulse" />
              Live · 3s updates
            </div>
            <button onClick={onClose} className="p-2 hover:bg-gray-700 rounded-lg transition-colors">
              <X size={20} className="text-orange-400" />
            </button>
          </div>
        </div>

        {/* Map — rendered ONCE, never remounts because key={ride.rideId} is on the outer modal */}
        <div style={{ height: '460px', position: 'relative' }}>
          <MapContainer
            center={driverPos}
            zoom={14}
            style={{ height: '100%', width: '100%' }}
            zoomControl
          >
            {/* OpenStreetMap tiles — free, no API key */}
            <TileLayer
              url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
              attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
            />

            {/* Smoothly pan map when driver moves & fix modal sizing */}
            <MapController position={driverPos} />

            {/* Driver marker */}
            <Marker position={driverPos} icon={driverIcon}>
              <Popup>
                <div style={{ fontSize: '12px', color: '#333', minWidth: '120px' }}>
                  <strong>🏍️ {ride.driverName}</strong><br />
                  <span style={{ color: '#666' }}>Updated: {lastUpdate}</span>
                </div>
              </Popup>
            </Marker>

            {/* Ride start / driver pickup */}
            {pickupPos && (
              <Marker position={pickupPos} icon={pickupIcon}>
                <Popup>
                  <div style={{ fontSize: '12px', color: '#333' }}>
                    <strong>📍 Ride Start</strong><br />{ride.fromName || 'From'}
                  </div>
                </Popup>
              </Marker>
            )}

            {/* Booker pickup point (where booker boards) */}
            {bookerPickupPos && (
              <Marker position={bookerPickupPos} icon={pickupIcon}>
                <Popup>
                  <div style={{ fontSize: '12px', color: '#333' }}>
                    <strong>🟢 Booker Pickup</strong><br />Booker boards here
                  </div>
                </Popup>
              </Marker>
            )}

            {/* Booker drop (or destination if no booker) */}
            {dropPos ? (
              <Marker position={dropPos} icon={dropIcon}>
                <Popup>
                  <div style={{ fontSize: '12px', color: '#333' }}>
                    <strong>🏁 Booker Drop</strong><br />Drop point
                  </div>
                </Popup>
              </Marker>
            ) : destPos ? (
              <Marker position={destPos} icon={dropIcon}>
                <Popup>
                  <div style={{ fontSize: '12px', color: '#333' }}>
                    <strong>🏁 Destination</strong><br />{ride.toName || 'To'}
                  </div>
                </Popup>
              </Marker>
            ) : null}

            {/* Route line: driver → destination (dashed) */}
            {routeLine.length === 2 && (
              <Polyline
                positions={routeLine}
                pathOptions={{ color: '#FF7000', weight: 3, opacity: 0.8, dashArray: '10, 8' }}
              />
            )}
          </MapContainer>
        </div>

        {/* Legend bar */}
        <div className="flex items-center justify-between px-5 py-3 bg-gray-900 border-t border-orange-500/20">
          <div className="flex items-center gap-5 text-xs text-orange-300/60 flex-wrap">
            <span className="flex items-center gap-1.5">
              <span style={{ background:'#FF7000', width:16, height:16, borderRadius:'50%', display:'inline-block', border:'2px solid white' }} />
              Driver (live)
            </span>
            <span className="flex items-center gap-1.5">
              <span style={{ background:'#22c55e', width:14, height:14, borderRadius:'50%', display:'inline-block', border:'2px solid white' }} />
              Pickup point
            </span>
            <span className="flex items-center gap-1.5">
              <span style={{ background:'#ef4444', width:14, height:14, borderRadius:'3px', display:'inline-block', border:'2px solid white' }} />
              Drop point
            </span>
            <span className="flex items-center gap-1.5">
              <svg width="24" height="4"><line x1="0" y1="2" x2="24" y2="2" stroke="#FF7000" strokeWidth="2.5" strokeDasharray="6,4"/></svg>
              Route ahead
            </span>
          </div>
          <StatusBadge state={ride.state} />
        </div>
      </div>
    </div>
  );
};

// ── Ride Detail Modal ────────────────────────────────────────────────────────

const InfoRow = ({ icon, label, value }) => (
  <div className="flex items-center gap-2.5 p-3 bg-gray-900/60 rounded-lg">
    <div className="text-orange-400 flex-shrink-0">{icon}</div>
    <div className="min-w-0 flex-1">
      <p className="text-[10px] text-orange-400/60 uppercase tracking-wider">{label}</p>
      <p className="text-sm font-medium text-white truncate">{value}</p>
    </div>
  </div>
);

const RideDetailModal = ({ ride, onClose, onOpenMap }) => {
  if (!ride) return null;

  const events = [];
  if (ride.createdAt)   events.push({ time: ride.createdAt,   label: 'Ride Posted',              color: 'bg-blue-400',    icon: <Bike size={12} /> });
  if (ride.state === 'LIVE') events.push({ time: ride.createdAt, label: 'Live Mode Activated',   color: 'bg-purple-400',  icon: <Radio size={12} /> });
  if (ride.bookerName && !['OPEN','LIVE','EXPIRED'].includes(ride.state))
    events.push({ time: ride.departAt, label: `Booked by ${ride.bookerName}`, color: 'bg-cyan-400', icon: <Users size={12} /> });
  if (ride.startedAt)   events.push({ time: ride.startedAt,   label: 'Ride Started',             color: 'bg-yellow-400',  icon: <Navigation size={12} /> });
  if (ride.verifiedAt)  events.push({ time: ride.verifiedAt,  label: 'OTP Verified at Pickup',   color: 'bg-emerald-400', icon: <CheckCircle size={12} /> });
  if (ride.completedAt) events.push({ time: ride.completedAt, label: 'Ride Completed',            color: 'bg-green-400',   icon: <CheckCircle size={12} /> });
  if (ride.cancelledAt) events.push({ time: ride.cancelledAt, label: 'Ride Cancelled',            color: 'bg-red-400',     icon: <XCircle size={12} /> });
  if (ride.state === 'EXPIRED') events.push({ time: ride.departAt, label: 'Ride Expired (no booking)', color: 'bg-gray-400', icon: <Clock size={12} /> });
  events.sort((a, b) => new Date(a.time) - new Date(b.time));

  const canTrack = ['LIVE','STARTED','VERIFIED'].includes(ride.state);

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div className="absolute inset-0 bg-black/60 backdrop-blur-sm" onClick={onClose} />
      <div className="relative bg-gray-800 border border-orange-500/30 rounded-2xl max-w-xl w-full max-h-[90vh] overflow-y-auto p-6 shadow-2xl shadow-orange-500/10">
        <button onClick={onClose} className="absolute top-4 right-4 p-1.5 hover:bg-gray-700 rounded-lg transition-colors z-10">
          <X size={20} className="text-orange-400" />
        </button>

        <div className="flex items-center gap-4 mb-6">
          <div className="w-14 h-14 bg-gradient-to-br from-orange-500 to-orange-600 rounded-xl flex items-center justify-center shadow-lg shadow-orange-500/20 flex-shrink-0">
            <Bike size={24} className="text-white" />
          </div>
          <div className="min-w-0 flex-1">
            <div className="flex items-center gap-3">
              <h2 className="text-xl font-bold text-white">Ride #{ride.instanceId || ride.rideId || ride.id}</h2>
              <StatusBadge state={ride.state} />
            </div>
            <p className="text-sm text-orange-300/60 mt-0.5">
              {ride.isLiveRide ? '🔴 Live Ride' : '📝 Posted Ride'}
            </p>
          </div>
        </div>

        <div className="grid grid-cols-2 gap-3 mb-6">
          <InfoRow icon={<Users size={14} />}    label="Driver"    value={ride.driverName} />
          <InfoRow icon={<Users size={14} />}    label="Booker"    value={ride.bookerName || '—'} />
          <InfoRow icon={<MapPin size={14} />}   label="From"      value={ride.fromName} />
          <InfoRow icon={<MapPin size={14} />}   label="To"        value={ride.toName} />
          <InfoRow icon={<Clock size={14} />}    label="Departure" value={formatDateTime(ride.departAt)} />
          <InfoRow icon={<Navigation size={14}/>} label="Distance" value={`${ride.distanceKm} km`} />
          <InfoRow icon={<DollarSign size={14}/>} label="Fare"     value={`₹${ride.fare}`} />
          <InfoRow icon={<Shield size={14} />}   label="Payment"   value={ride.paymentMode || '—'} />
        </div>

        {events.length > 0 && (
          <div className="border-t border-orange-500/20 pt-5">
            <h3 className="text-sm font-semibold text-orange-400 uppercase tracking-wider mb-4">Ride Timeline</h3>
            <div className="relative pl-8">
              <div className="absolute left-[11px] top-2 bottom-2 w-0.5 bg-orange-500/20 rounded-full" />
              {events.map((event, idx) => (
                <div key={idx} className="relative flex items-start gap-4 mb-4 last:mb-0">
                  <div className={`absolute left-[-21px] top-0.5 w-5 h-5 rounded-full ${event.color} flex items-center justify-center shadow-md`}>
                    <span className="text-white">{event.icon}</span>
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-medium text-white">{event.label}</p>
                    <p className="text-xs text-orange-300/50 mt-0.5">{formatDateTime(event.time)}</p>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {canTrack && (
          <div className="mt-6 pt-4 border-t border-orange-500/20">
            <button
              onClick={() => { onClose(); onOpenMap(ride); }}
              className="w-full flex items-center justify-center gap-2 px-4 py-3 bg-orange-500/10 hover:bg-orange-500/20 text-orange-400 rounded-xl transition-colors border border-orange-500/20 font-medium"
            >
              <MapIcon size={18} />
              Track Live on Map
            </button>
          </div>
        )}
      </div>
    </div>
  );
};

// ═══════════════════════════════════════════════════════════════════════════════
//  MAIN COMPONENT
// ═══════════════════════════════════════════════════════════════════════════════

const RideManagement = () => {
  const navigate = useNavigate();

  const [stats, setStats]             = useState(null);
  const [rides, setRides]             = useState([]);
  const [loading, setLoading]         = useState(true);
  const [error, setError]             = useState(null);

  const [page, setPage]               = useState(0);
  const [size, setSize]               = useState(20);
  const [totalPages, setTotalPages]   = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  const [searchInput, setSearchInput] = useState('');
  const [searchTerm, setSearchTerm]   = useState('');
  const [filterState, setFilterState] = useState('all');
  const [dateFrom, setDateFrom]       = useState('');
  const [dateTo, setDateTo]           = useState('');
  const [showFilters, setShowFilters] = useState(false);
  const [sortBy, setSortBy]           = useState('departAt');
  const [sortDir, setSortDir]         = useState('desc');

  const [selectedRide, setSelectedRide] = useState(null);
  const [mapRide, setMapRide]           = useState(null);

  // Location stored in a ref → changing it does NOT remount MapContainer
  const locationRef    = useRef(null);
  const [locationVersion, setLocationVersion] = useState(0);

  const [toast, setToast]             = useState(null);
  const toastRef                      = useRef(null);
  const debounceRef                   = useRef(null);
  const mapPollRef                    = useRef(null);
  const tablePollRef                  = useRef(null);

  // ── Auth ─────────────────────────────────────────────────────────────────
  const getToken = useCallback(() => {
    const t = localStorage.getItem('adminToken');
    if (!t) { navigate('/'); return null; }
    return t;
  }, [navigate]);

  // ── Stats ─────────────────────────────────────────────────────────────────
  const fetchStats = useCallback(async () => {
    const token = getToken();
    if (!token) return;
    try {
      const { baseURL } = getApiConfig();
      const res = await fetch(`${baseURL}/admin/rides/stats`, {
        headers: { Authorization: `Bearer ${token}` }
      });
      if (res.status === 401 || res.status === 403) { localStorage.removeItem('adminToken'); navigate('/'); return; }
      if (!res.ok) return;
      const json = await res.json();
      setStats(json.data || json);
    } catch (err) {
      console.error('Stats fetch failed:', err);
    }
  }, [getToken, navigate]);

  // ── Rides table ───────────────────────────────────────────────────────────
  const fetchRides = useCallback(async (silent = false) => {
    const token = getToken();
    if (!token) return;
    if (!silent) { setLoading(true); setError(null); }
    try {
      const { baseURL } = getApiConfig();
      const p = new URLSearchParams();
      p.set('page', page); p.set('size', size);
      p.set('sortBy', sortBy); p.set('sortDir', sortDir);
      if (filterState !== 'all') p.set('state', filterState);
      if (searchTerm.trim()) p.set('search', searchTerm.trim());
      if (dateFrom) p.set('from', dateFrom);
      if (dateTo)   p.set('to', dateTo);

      const res = await fetch(`${baseURL}/admin/rides?${p.toString()}`, {
        headers: { Authorization: `Bearer ${token}` }
      });
      if (res.status === 401 || res.status === 403) { localStorage.removeItem('adminToken'); navigate('/'); return; }
      if (!res.ok) throw new Error(`Server error: ${res.status}`);
      const json = await res.json();
      const paged = json.data;
      setRides(paged.content || []);
      setTotalPages(paged.totalPages || 0);
      setTotalElements(paged.totalElements || 0);
    } catch (err) {
      if (!silent) setError(err.message);
    } finally {
      if (!silent) setLoading(false);
    }
  }, [page, size, sortBy, sortDir, filterState, searchTerm, dateFrom, dateTo, navigate, getToken]);

  // ── Location polling ──────────────────────────────────────────────────────
  const fetchLocation = useCallback(async (rideId) => {
    const token = getToken();
    if (!token) return;
    try {
      const { baseURL } = getApiConfig();
      const res = await fetch(`${baseURL}/admin/rides/${rideId}/location`, {
        headers: { Authorization: `Bearer ${token}` }
      });
      if (res.ok) {
        const json = await res.json();
        const loc  = json.data;
        if (loc) {
          const rawCurrentLat = parseFloat(loc.currentLat ?? loc.lat);
          const rawCurrentLng = parseFloat(loc.currentLng ?? loc.lng);
          const rawFromLat    = parseFloat(loc.fromLat);
          const rawFromLng    = parseFloat(loc.fromLng);

          // If live driver location is not set yet, fallback to ride start location (fromLat/fromLng)
          let effectiveLat = rawCurrentLat;
          let effectiveLng = rawCurrentLng;
          if (!isValidLatLng(effectiveLat, effectiveLng) && isValidLatLng(rawFromLat, rawFromLng)) {
            effectiveLat = rawFromLat;
            effectiveLng = rawFromLng;
          }

          // Update ref without state → MapContainer never remounts
          locationRef.current = {
            ...loc,
            currentLat: effectiveLat,
            currentLng: effectiveLng,
            fromLat:    rawFromLat,
            fromLng:    rawFromLng,
            toLat:      parseFloat(loc.toLat),
            toLng:      parseFloat(loc.toLng),
            bookerPickupLat: parseFloat(loc.bookerPickupLat || 0),
            bookerPickupLng: parseFloat(loc.bookerPickupLng || 0),
            bookerDropLat:   parseFloat(loc.bookerDropLat || 0),
            bookerDropLng:   parseFloat(loc.bookerDropLng || 0),
          };
          // Bump version so LiveMapModal re-reads the ref
          setLocationVersion(v => v + 1);
        }
      }
    } catch (err) {
      console.error('Location fetch failed:', err);
    }
  }, [getToken]);

  // ── Effects ───────────────────────────────────────────────────────────────
  useEffect(() => { fetchStats(); fetchRides(); }, [fetchStats, fetchRides]);

  useEffect(() => {
    tablePollRef.current = setInterval(() => { fetchStats(); fetchRides(true); }, TABLE_POLL_MS);
    return () => clearInterval(tablePollRef.current);
  }, [fetchStats, fetchRides]);

  useEffect(() => {
    if (debounceRef.current) clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(() => { setPage(0); setSearchTerm(searchInput); }, 400);
    return () => clearTimeout(debounceRef.current);
  }, [searchInput]);

  useEffect(() => {
    if (mapRide) {
      const targetId = mapRide.instanceId || mapRide.rideId || mapRide.id;
      locationRef.current = null;
      setLocationVersion(0);
      if (targetId) {
        fetchLocation(targetId);
        mapPollRef.current = setInterval(() => fetchLocation(targetId), MAP_POLL_MS);
      }
    } else {
      clearInterval(mapPollRef.current);
    }
    return () => clearInterval(mapPollRef.current);
  }, [mapRide, fetchLocation]);

  // ── Helpers ───────────────────────────────────────────────────────────────
  const handleSort = (col) => {
    if (sortBy === col) setSortDir(d => d === 'asc' ? 'desc' : 'asc');
    else { setSortBy(col); setSortDir('asc'); }
    setPage(0);
  };

  const clearFilters = () => {
    setSearchInput(''); setSearchTerm('');
    setFilterState('all'); setDateFrom(''); setDateTo('');
    setSortBy('departAt'); setSortDir('desc'); setPage(0);
  };

  const hasActiveFilters = searchTerm || filterState !== 'all' || dateFrom || dateTo;

  const showToast = (message, type) => {
    setToast({ message, type });
    if (toastRef.current) clearTimeout(toastRef.current);
    toastRef.current = setTimeout(() => setToast(null), 3500);
  };

  const pageNumbers = () => {
    const pages = [], max = 5;
    let start = Math.max(0, page - Math.floor(max / 2));
    let end   = Math.min(totalPages, start + max);
    if (end - start < max) start = Math.max(0, end - max);
    for (let i = start; i < end; i++) pages.push(i);
    return pages;
  };

  const SortIcon = ({ column }) => {
    if (sortBy !== column) return <ArrowUpDown size={14} className="text-orange-400/40" />;
    return sortDir === 'asc'
      ? <ArrowUp size={14} className="text-orange-400" />
      : <ArrowDown size={14} className="text-orange-400" />;
  };

  // ── Sub-components ────────────────────────────────────────────────────────
  const KpiCard = ({ icon: Icon, title, value, color }) => (
    <div className="bg-gray-800 border border-orange-500/20 rounded-xl p-5 hover:border-orange-500/40 transition-all">
      <div className="flex items-center justify-between mb-3">
        <div className={`p-2.5 rounded-lg ${color}`}><Icon size={20} className="text-white" /></div>
      </div>
      <h3 className="text-2xl font-bold text-white mb-0.5">
        {typeof value === 'number' ? value.toLocaleString('en-IN') : value}
      </h3>
      <p className="text-orange-300/70 text-xs font-medium">{title}</p>
    </div>
  );

  const RideHealthChart = ({ daily }) => {
    if (!daily || daily.length === 0) return null;
    const chartData = {
      labels: daily.map(d => d.date),
      datasets: [
        { label: 'Completed', data: daily.map(d => d.completed), backgroundColor: 'rgba(16,185,129,0.8)', borderColor: 'rgba(16,185,129,1)', borderWidth: 1, borderRadius: 4, stack: 'rides' },
        { label: 'Cancelled', data: daily.map(d => d.cancelled), backgroundColor: 'rgba(244,63,94,0.8)',  borderColor: 'rgba(244,63,94,1)',  borderWidth: 1, borderRadius: 4, stack: 'rides' },
        { label: 'Expired',   data: daily.map(d => d.expired),   backgroundColor: 'rgba(245,158,11,0.8)',borderColor: 'rgba(245,158,11,1)', borderWidth: 1, borderRadius: 4, stack: 'rides' },
        { label: 'Earnings (₹)', data: daily.map(d => d.earning), type: 'line', borderColor: '#FF7000', backgroundColor: 'rgba(255,112,0,0.1)', borderWidth: 2, pointRadius: 4, pointBackgroundColor: '#FF7000', fill: true, tension: 0.4, yAxisID: 'y1' },
      ],
    };
    const chartOptions = {
      responsive: true, maintainAspectRatio: false,
      interaction: { mode: 'index', intersect: false },
      plugins: {
        legend: { position: 'top', labels: { color: '#fdba74', font: { size: 11 }, usePointStyle: true, padding: 16 } },
        tooltip: { backgroundColor: 'rgba(15,15,25,0.95)', titleColor: '#fff', bodyColor: '#fdba74', borderColor: 'rgba(255,112,0,0.3)', borderWidth: 1, cornerRadius: 8, padding: 12 },
      },
      scales: {
        x:  { grid: { color: 'rgba(255,112,0,0.06)' }, ticks: { color: '#fdba74', font: { size: 11 } } },
        y:  { stacked: true, position: 'left', grid: { color: 'rgba(255,112,0,0.06)' }, ticks: { color: '#fdba74', font: { size: 11 } }, title: { display: true, text: 'Rides', color: '#fdba74' } },
        y1: { position: 'right', grid: { drawOnChartArea: false }, ticks: { color: '#FF7000', font: { size: 11 }, callback: v => '₹' + v.toLocaleString('en-IN') }, title: { display: true, text: 'Earnings', color: '#FF7000' } },
      },
    };
    return (
      <div className="bg-gray-800 border border-orange-500/20 rounded-xl p-6">
        <div className="flex items-center justify-between mb-6">
          <div>
            <h3 className="text-lg font-semibold text-white">Ride Health Overview</h3>
            <p className="text-xs text-orange-300/60 mt-1">Daily ride outcomes · Last 7 days</p>
          </div>
          <span className="flex items-center gap-2 px-3 py-1.5 rounded-full text-xs font-medium bg-orange-500/10 text-orange-400 border border-orange-500/20">
            <Activity size={12} /> 7-Day View
          </span>
        </div>
        <div style={{ height: '320px' }}><Bar data={chartData} options={chartOptions} /></div>
      </div>
    );
  };

  const TableSkeleton = () => (
    <div className="space-y-3 p-6">
      {[...Array(8)].map((_, i) => (
        <div key={i} className="flex items-center gap-4 animate-pulse">
          <div className="w-12 h-5 bg-gray-700 rounded" />
          <div className="flex-1 space-y-2">
            <div className="h-4 bg-gray-700 rounded w-1/3" />
            <div className="h-3 bg-gray-700/60 rounded w-1/4" />
          </div>
          <div className="h-6 w-16 bg-gray-700 rounded-full" />
        </div>
      ))}
    </div>
  );

  // ── Render ────────────────────────────────────────────────────────────────
  return (
    <AdminLayout>
      <main className="p-6">

        {/* Page Header */}
        <div className="bg-gray-800 border border-orange-500/20 rounded-xl p-6 mb-6">
          <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
            <div>
              <h1 className="text-2xl font-bold text-white mb-1">Ride Management</h1>
              <p className="text-orange-300 text-sm flex items-center gap-2">
                Monitor all ride activities · Auto-refreshes every 30s
                <span className="w-1.5 h-1.5 bg-green-400 rounded-full animate-pulse" />
              </p>
            </div>
            <button onClick={() => { fetchStats(); fetchRides(); }} disabled={loading}
              className="inline-flex items-center gap-2 px-4 py-2.5 bg-orange-500/10 hover:bg-orange-500/20 text-orange-400 rounded-lg transition-colors border border-orange-500/20 disabled:opacity-50">
              <RefreshCw size={16} className={loading ? 'animate-spin' : ''} />
              <span className="font-medium text-sm">Refresh</span>
            </button>
          </div>
        </div>

        {/* KPI Cards */}
        {stats && (
          <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-4 mb-6">
            <KpiCard icon={Bike}        title="Total Today"    value={stats.totalToday}        color="bg-orange-500" />
            <KpiCard icon={Activity}    title="Active"         value={stats.activeToday}        color="bg-blue-500" />
            <KpiCard icon={Radio}       title="Live"           value={stats.liveToday}          color="bg-purple-500" />
            <KpiCard icon={CheckCircle} title="Completed"      value={stats.completedToday}     color="bg-emerald-500" />
            <KpiCard icon={XCircle}     title="Cancelled"      value={stats.cancelledToday}     color="bg-rose-500" />
            <KpiCard icon={TrendingUp}  title="Success Rate"   value={`${stats.successRatePct}%`} color="bg-gradient-to-br from-orange-500 to-amber-500" />
          </div>
        )}

        {/* Revenue */}
        {stats && (
          <div className="bg-gradient-to-r from-orange-500/10 via-orange-500/5 to-transparent border border-orange-500/20 rounded-xl p-5 mb-6 flex items-center justify-between">
            <div className="flex items-center gap-4">
              <div className="w-12 h-12 bg-gradient-to-br from-orange-500 to-amber-500 rounded-xl flex items-center justify-center shadow-lg shadow-orange-500/25">
                <DollarSign size={24} className="text-white" />
              </div>
              <div>
                <p className="text-xs text-orange-300/60 font-medium uppercase tracking-wider">Today's Revenue</p>
                <p className="text-3xl font-bold text-white">₹{stats.revenueToday?.toLocaleString('en-IN')}</p>
              </div>
            </div>
            <div className="hidden sm:flex items-center gap-6 text-sm">
              <div className="text-center">
                <p className="text-orange-300/50 text-xs">Avg / Ride</p>
                <p className="text-white font-semibold">
                  ₹{stats.completedToday > 0 ? Math.round(stats.revenueToday / stats.completedToday) : 0}
                </p>
              </div>
            </div>
          </div>
        )}

        {/* Chart */}
        {stats?.daily && <div className="mb-6"><RideHealthChart daily={stats.daily} /></div>}

        {/* Search & Filters */}
        <div className="bg-gray-800 border border-orange-500/20 rounded-xl p-6 mb-6">
          <div className="flex flex-col lg:flex-row gap-4">
            <div className="flex-1 relative">
              <Search size={20} className="absolute left-3 top-1/2 -translate-y-1/2 text-orange-400" />
              <input type="text" placeholder="Search by ride ID, driver, booker, or location…"
                value={searchInput} onChange={e => setSearchInput(e.target.value)}
                className="w-full pl-10 pr-4 py-3 bg-gray-700 border border-orange-500/30 rounded-lg focus:outline-none focus:ring-2 focus:ring-orange-500 text-white placeholder-orange-300/50 transition-all" />
              {searchInput && (
                <button onClick={() => setSearchInput('')} className="absolute right-3 top-1/2 -translate-y-1/2 text-orange-400/60 hover:text-orange-400">
                  <XCircle size={16} />
                </button>
              )}
            </div>
            <button onClick={() => setShowFilters(!showFilters)}
              className={`inline-flex items-center gap-2 px-4 py-3 border rounded-lg transition-colors ${showFilters || hasActiveFilters ? 'border-orange-500/60 bg-orange-500/10 text-orange-400' : 'border-orange-500/30 hover:bg-gray-700 text-orange-300'}`}>
              <Filter size={20} /><span className="font-medium">Filters</span>
              {hasActiveFilters && <span className="w-2 h-2 bg-orange-500 rounded-full animate-pulse" />}
            </button>
            {hasActiveFilters && (
              <button onClick={clearFilters} className="inline-flex items-center gap-2 px-4 py-3 border border-red-500/30 text-red-400 rounded-lg hover:bg-red-500/10 transition-colors">
                <XCircle size={16} /><span className="text-sm font-medium">Clear</span>
              </button>
            )}
          </div>
          {showFilters && (
            <div className="mt-4 pt-4 border-t border-orange-500/20 grid grid-cols-1 sm:grid-cols-3 gap-4">
              <div>
                <label className="block text-sm font-medium text-orange-300 mb-2">Status</label>
                <select value={filterState} onChange={e => { setFilterState(e.target.value); setPage(0); }}
                  className="w-full px-3 py-2.5 bg-gray-700 border border-orange-500/30 rounded-lg focus:outline-none focus:ring-2 focus:ring-orange-500 text-white">
                  <option value="all">All Status</option>
                  {RIDE_STATES.map(s => <option key={s} value={s}>{STATUS_CONFIG[s]?.label || s}</option>)}
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-orange-300 mb-2">From Date</label>
                <input type="date" value={dateFrom} onChange={e => { setDateFrom(e.target.value); setPage(0); }}
                  className="w-full px-3 py-2.5 bg-gray-700 border border-orange-500/30 rounded-lg focus:outline-none focus:ring-2 focus:ring-orange-500 text-white" />
              </div>
              <div>
                <label className="block text-sm font-medium text-orange-300 mb-2">To Date</label>
                <input type="date" value={dateTo} onChange={e => { setDateTo(e.target.value); setPage(0); }}
                  className="w-full px-3 py-2.5 bg-gray-700 border border-orange-500/30 rounded-lg focus:outline-none focus:ring-2 focus:ring-orange-500 text-white" />
              </div>
            </div>
          )}
        </div>

        {/* Error */}
        {error && (
          <div className="bg-red-500/10 border border-red-500/30 rounded-xl p-4 mb-6 flex items-center gap-3">
            <AlertCircle size={20} className="text-red-400 flex-shrink-0" />
            <p className="text-red-400 text-sm">{error}</p>
            <button onClick={() => fetchRides()} className="ml-auto px-3 py-1.5 bg-red-500/20 text-red-400 rounded-lg text-sm hover:bg-red-500/30 transition-colors">Retry</button>
          </div>
        )}

        {/* Table */}
        <div className="bg-gray-800 border border-orange-500/20 rounded-xl overflow-hidden">
          <div className="px-6 py-4 border-b border-orange-500/20 flex items-center justify-between">
            <h3 className="text-white font-semibold flex items-center gap-2">
              <Bike size={18} className="text-orange-400" />
              All Rides
              <span className="text-xs text-orange-300/50 font-normal ml-1">
                {totalElements > 0 ? `(${totalElements.toLocaleString()} total)` : ''}
              </span>
            </h3>
          </div>

          {loading ? <TableSkeleton /> : rides.length === 0 ? (
            <div className="p-12 text-center">
              <div className="w-20 h-20 mx-auto mb-4 bg-orange-500/10 rounded-full flex items-center justify-center">
                <Bike size={36} className="text-orange-400" />
              </div>
              <h3 className="text-lg font-medium text-white mb-2">No rides found</h3>
              <p className="text-orange-300/70 max-w-md mx-auto mb-6">
                {hasActiveFilters ? 'No rides match your current filters.' : 'No ride data available yet.'}
              </p>
              {hasActiveFilters && (
                <button onClick={clearFilters} className="px-4 py-2 bg-orange-500/10 text-orange-400 rounded-lg hover:bg-orange-500/20 transition-colors border border-orange-500/20">
                  Clear Filters
                </button>
              )}
            </div>
          ) : (
            <>
              <div className="overflow-x-auto">
                <table className="w-full">
                  <thead className="bg-gray-900/80 border-b border-orange-500/20">
                    <tr>
                      {['ID','Driver','Booker','Status','Route','Departure','Dist','Actions'].map(h => (
                        <th key={h} className="px-4 py-3 text-left text-xs font-semibold text-orange-400 uppercase tracking-wider"
                          onClick={h === 'Departure' ? () => handleSort('departAt') : undefined}
                          style={h === 'Departure' ? { cursor: 'pointer' } : {}}>
                          {h === 'Departure'
                            ? <span className="inline-flex items-center gap-1">Departure <SortIcon column="departAt" /></span>
                            : h}
                        </th>
                      ))}
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-orange-500/10">
                    {rides.map((ride, idx) => {
                      const canTrack = ['LIVE','STARTED','VERIFIED'].includes(ride.state);
                      return (
                        <tr key={ride.instanceId || ride.rideId || idx} className="hover:bg-gray-700/30 transition-colors">
                          <td className="px-4 py-3">
                            <span className="text-sm font-mono text-white/80">#{ride.instanceId || ride.rideId || ride.id}</span>
                          </td>
                          <td className="px-4 py-3">
                            <div className="flex items-center gap-2">
                              <div className="w-8 h-8 bg-gradient-to-br from-orange-500 to-orange-600 rounded-full flex items-center justify-center flex-shrink-0">
                                <span className="text-white font-bold text-xs">{ride.driverName?.charAt(0)?.toUpperCase() || '?'}</span>
                              </div>
                              <span className="text-sm text-white truncate max-w-[120px]">{ride.driverName}</span>
                            </div>
                          </td>
                          <td className="px-4 py-3">
                            {ride.bookerName ? (
                              <div className="flex items-center gap-2">
                                <div className="w-8 h-8 bg-gradient-to-br from-blue-500 to-blue-600 rounded-full flex items-center justify-center flex-shrink-0">
                                  <span className="text-white font-bold text-xs">{ride.bookerName.charAt(0).toUpperCase()}</span>
                                </div>
                                <span className="text-sm text-white truncate max-w-[120px]">{ride.bookerName}</span>
                              </div>
                            ) : <span className="text-sm text-gray-500">—</span>}
                          </td>
                          <td className="px-4 py-3"><StatusBadge state={ride.state} /></td>
                          <td className="px-4 py-3">
                            <div className="max-w-[200px]">
                              <p className="text-xs text-green-400/80 truncate flex items-center gap-1">
                                <span className="w-1.5 h-1.5 bg-green-400 rounded-full flex-shrink-0" />{ride.fromName}
                              </p>
                              <p className="text-xs text-red-400/80 truncate flex items-center gap-1 mt-0.5">
                                <span className="w-1.5 h-1.5 bg-red-400 rounded-full flex-shrink-0" />{ride.toName}
                              </p>
                            </div>
                          </td>
                          <td className="px-4 py-3">
                            <span className="text-sm text-white/70">{formatDateTime(ride.departAt)}</span>
                          </td>
                          <td className="px-4 py-3">
                            <span className="text-sm text-white/70">{ride.distanceKm} km</span>
                          </td>
                          <td className="px-4 py-3">
                            <div className="flex items-center gap-1.5">
                              <button onClick={() => setSelectedRide(ride)}
                                className="p-2 hover:bg-orange-500/10 rounded-lg transition-colors" title="View Details">
                                <Eye size={16} className="text-orange-400" />
                              </button>
                              {canTrack && (
                                <button onClick={() => setMapRide(ride)}
                                  className="p-2 hover:bg-blue-500/10 rounded-lg transition-colors" title="Track on Map">
                                  <MapIcon size={16} className="text-blue-400" />
                                </button>
                              )}
                            </div>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>

              {/* Pagination */}
              <div className="px-6 py-4 border-t border-orange-500/20 flex flex-col sm:flex-row items-center justify-between gap-4">
                <div className="flex items-center gap-4 text-sm text-orange-300/70">
                  <span>Page <span className="font-medium text-white">{page + 1}</span> of <span className="font-medium text-white">{totalPages || 1}</span></span>
                  <span className="text-orange-500/30">|</span>
                  <div className="flex items-center gap-2">
                    <span>Show</span>
                    <select value={size} onChange={e => { setSize(Number(e.target.value)); setPage(0); }}
                      className="px-2 py-1 bg-gray-700 border border-orange-500/30 rounded text-white text-sm focus:outline-none focus:ring-1 focus:ring-orange-500">
                      <option value={10}>10</option>
                      <option value={20}>20</option>
                      <option value={50}>50</option>
                    </select>
                    <span>per page</span>
                  </div>
                </div>
                <div className="flex items-center gap-1.5">
                  <button onClick={() => setPage(0)} disabled={page === 0}
                    className="px-2.5 py-1.5 text-sm border border-orange-500/20 rounded-lg text-orange-300 hover:bg-gray-700 disabled:opacity-30 disabled:cursor-not-allowed transition-colors">First</button>
                  <button onClick={() => setPage(p => Math.max(0, p - 1))} disabled={page === 0}
                    className="p-1.5 border border-orange-500/20 rounded-lg text-orange-300 hover:bg-gray-700 disabled:opacity-30 disabled:cursor-not-allowed transition-colors">
                    <ChevronLeft size={18} />
                  </button>
                  {pageNumbers().map(p => (
                    <button key={p} onClick={() => setPage(p)}
                      className={`w-9 h-9 text-sm rounded-lg font-medium transition-all ${p === page ? 'bg-orange-500 text-white shadow-lg shadow-orange-500/25' : 'text-orange-300 hover:bg-gray-700 border border-orange-500/20'}`}>
                      {p + 1}
                    </button>
                  ))}
                  <button onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))} disabled={page >= totalPages - 1}
                    className="p-1.5 border border-orange-500/20 rounded-lg text-orange-300 hover:bg-gray-700 disabled:opacity-30 disabled:cursor-not-allowed transition-colors">
                    <ChevronRight size={18} />
                  </button>
                  <button onClick={() => setPage(totalPages - 1)} disabled={page >= totalPages - 1}
                    className="px-2.5 py-1.5 text-sm border border-orange-500/20 rounded-lg text-orange-300 hover:bg-gray-700 disabled:opacity-30 disabled:cursor-not-allowed transition-colors">Last</button>
                </div>
              </div>
            </>
          )}
        </div>
      </main>

      {/* Ride Detail Modal */}
      {selectedRide && (
        <RideDetailModal
          ride={selectedRide}
          onClose={() => setSelectedRide(null)}
          onOpenMap={ride => setMapRide(ride)}
        />
      )}

      {/* Live Map Modal — key={rideId} so MapContainer remounts only when switching rides, not on location updates */}
      {mapRide && (
        <LiveMapModal
          key={mapRide.instanceId || mapRide.rideId || mapRide.id}
          ride={mapRide}
          locationRef={locationRef}
          locationVersion={locationVersion}
          onClose={() => { setMapRide(null); locationRef.current = null; setLocationVersion(0); }}
        />
      )}

      {/* Toast */}
      {toast && (
        <div className={`fixed bottom-6 right-6 z-[60] flex items-center gap-3 px-5 py-3 rounded-xl shadow-2xl border ${
          toast.type === 'success' ? 'bg-green-500/15 border-green-500/30 text-green-400' : 'bg-red-500/15 border-red-500/30 text-red-400'
        }`}>
          {toast.type === 'success' ? <CheckCircle size={18} /> : <AlertCircle size={18} />}
          <span className="text-sm font-medium">{toast.message}</span>
          <button onClick={() => setToast(null)} className="ml-2 hover:opacity-70 transition-opacity"><XCircle size={14} /></button>
        </div>
      )}
    </AdminLayout>
  );
};

export default RideManagement;
