import React, { useState, useEffect } from 'react';
import { Rocket, Clock, ExternalLink, Save, CheckCircle2, AlertCircle, Sparkles, Smartphone } from 'lucide-react';
import getApiConfig from '../config/api';

const AdminLaunchControlCard = ({ onConfigUpdated }) => {
  const [launchMode, setLaunchMode] = useState('COMING_SOON'); // COMING_SOON vs LIVE_LAUNCHED
  const [launchTargetDateTime, setLaunchTargetDateTime] = useState('');
  const [androidAppUrl, setAndroidAppUrl] = useState('');
  const [iosAppUrl, setIosAppUrl] = useState('');
  const [launchMessage, setLaunchMessage] = useState('');

  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [statusNotification, setStatusNotification] = useState(null);

  const isLinksRequired = launchMode === 'LIVE_LAUNCHED' || Boolean(launchTargetDateTime);

  const fetchLaunchConfig = async () => {
    setLoading(true);
    try {
      const apiConfig = getApiConfig();
      const adminToken = localStorage.getItem('adminToken') || localStorage.getItem('token');

      const res = await fetch(`${apiConfig.baseURL}/admin/launch-config`, {
        headers: {
          'Authorization': `Bearer ${adminToken}`,
          'Content-Type': 'application/json'
        }
      });

      if (res.ok) {
        const data = await res.json();
        if (data.data) {
          const cfg = data.data;
          setLaunchMode(cfg.launchMode || 'COMING_SOON');
          setLaunchTargetDateTime(cfg.launchTargetDateTime ? cfg.launchTargetDateTime.slice(0, 16) : '');
          setAndroidAppUrl(cfg.androidAppUrl || '');
          setIosAppUrl(cfg.iosAppUrl || '');
          setLaunchMessage(cfg.launchMessage || '');
        }
      }
    } catch (err) {
      console.error('Error fetching launch config:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchLaunchConfig();
  }, []);

  const handleSave = async (e) => {
    e.preventDefault();
    setStatusNotification(null);

    // Client-side validation for restriction rules
    if (isLinksRequired) {
      if (!androidAppUrl.trim()) {
        setStatusNotification({
          type: 'error',
          msg: 'Android Google Play Store link is required when setting mode to Live or when launch timer is set.'
        });
        return;
      }
      if (!iosAppUrl.trim()) {
        setStatusNotification({
          type: 'error',
          msg: 'Apple App Store link is required when setting mode to Live or when launch timer is set.'
        });
        return;
      }
    }

    setSaving(true);

    try {
      const apiConfig = getApiConfig();
      const adminToken = localStorage.getItem('adminToken') || localStorage.getItem('token');

      const payload = {
        launchMode: launchMode,
        launchTargetDateTime: launchTargetDateTime ? launchTargetDateTime + ':00' : null,
        androidAppUrl: androidAppUrl.trim(),
        iosAppUrl: iosAppUrl.trim(),
        launchMessage: launchMessage.trim()
      };

      const res = await fetch(`${apiConfig.baseURL}/admin/launch-config`, {
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
          msg: data.message || 'Launch configuration saved successfully!'
        });
        if (data.data) {
          setLaunchMode(data.data.launchMode || launchMode);
          setLaunchTargetDateTime(data.data.launchTargetDateTime ? data.data.launchTargetDateTime.slice(0, 16) : '');
        }
        if (onConfigUpdated) onConfigUpdated(data.data);
      } else {
        setStatusNotification({
          type: 'error',
          msg: data.message || 'Failed to update launch configuration'
        });
      }
    } catch (err) {
      setStatusNotification({
        type: 'error',
        msg: 'Network error while updating launch settings'
      });
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <div className="bg-gray-900 border border-gray-800 rounded-2xl p-6 text-center text-gray-400">
        <div className="w-5 h-5 border-2 border-orange-500 border-t-transparent rounded-full animate-spin mx-auto mb-2" />
        <span>Loading Launch Controller...</span>
      </div>
    );
  }

  return (
    <div className="bg-slate-900/90 border border-orange-500/30 rounded-3xl p-6 sm:p-7 shadow-2xl relative overflow-hidden text-white">
      {/* Background glow accent */}
      <div className="absolute -top-12 -right-12 w-36 h-36 bg-orange-500/10 rounded-full blur-2xl pointer-events-none" />

      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-5 border-b border-white/10 mb-6">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-2xl bg-gradient-to-br from-orange-500 to-amber-500 text-white flex items-center justify-center shadow-lg shadow-orange-500/30">
            <Rocket size={22} />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h2 className="text-lg font-bold text-white">Platform Launch Control Portal</h2>
              <span className={`px-2.5 py-0.5 rounded-full text-[10px] font-extrabold uppercase tracking-wider border ${
                launchMode === 'LIVE_LAUNCHED' 
                  ? 'bg-emerald-500/20 border-emerald-500/40 text-emerald-400' 
                  : 'bg-amber-500/20 border-amber-500/40 text-amber-400'
              }`}>
                {launchMode === 'LIVE_LAUNCHED' ? '🚀 App Live' : '⏳ Coming Soon'}
              </span>
            </div>
            <p className="text-xs text-gray-400">Configure launch timer, mode, and Play Store / App Store download links.</p>
          </div>
        </div>
      </div>

      {/* Notification Toast */}
      {statusNotification && (
        <div className={`p-3.5 mb-6 rounded-xl flex items-center justify-between text-xs font-semibold border ${
          statusNotification.type === 'success'
            ? 'bg-emerald-500/15 border-emerald-500/30 text-emerald-300'
            : 'bg-red-500/15 border-red-500/30 text-red-300'
        }`}>
          <div className="flex items-center gap-2">
            {statusNotification.type === 'success' ? <CheckCircle2 size={16} /> : <AlertCircle size={16} />}
            <span>{statusNotification.msg}</span>
          </div>
          <button onClick={() => setStatusNotification(null)} className="opacity-70 hover:opacity-100">✕</button>
        </div>
      )}

      <form onSubmit={handleSave} className="space-y-6">
        {/* Launch Mode Toggle Buttons */}
        <div>
          <label className="block text-xs font-bold text-gray-300 uppercase tracking-wider mb-2.5">
            Set Priority Launch Status
          </label>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <button
              type="button"
              onClick={() => setLaunchMode('COMING_SOON')}
              className={`p-4 rounded-2xl border text-left transition-all flex items-center justify-between ${
                launchMode === 'COMING_SOON'
                  ? 'bg-orange-500/20 border-orange-500 text-white shadow-lg shadow-orange-500/15'
                  : 'bg-gray-800/50 border-gray-700/60 text-gray-400 hover:border-gray-600'
              }`}
            >
              <div className="flex items-center gap-3">
                <Clock size={20} className={launchMode === 'COMING_SOON' ? 'text-orange-400' : 'text-gray-500'} />
                <div>
                  <h4 className="font-bold text-sm">Keep as Coming Soon</h4>
                  <p className="text-xs text-gray-400">Shows timer / SMS pre-registration modal</p>
                </div>
              </div>
              {launchMode === 'COMING_SOON' && <CheckCircle2 size={18} className="text-orange-400 shrink-0" />}
            </button>

            <button
              type="button"
              onClick={() => setLaunchMode('LIVE_LAUNCHED')}
              className={`p-4 rounded-2xl border text-left transition-all flex items-center justify-between ${
                launchMode === 'LIVE_LAUNCHED'
                  ? 'bg-emerald-500/20 border-emerald-500 text-white shadow-lg shadow-emerald-500/15'
                  : 'bg-gray-800/50 border-gray-700/60 text-gray-400 hover:border-gray-600'
              }`}
            >
              <div className="flex items-center gap-3">
                <Rocket size={20} className={launchMode === 'LIVE_LAUNCHED' ? 'text-emerald-400' : 'text-gray-500'} />
                <div>
                  <h4 className="font-bold text-sm">App Live Launched</h4>
                  <p className="text-xs text-gray-400">Buttons open App Store & Play Store directly</p>
                </div>
              </div>
              {launchMode === 'LIVE_LAUNCHED' && <CheckCircle2 size={18} className="text-emerald-400 shrink-0" />}
            </button>
          </div>
        </div>

        {/* Inputs Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
          {/* Target Countdown DateTime */}
          <div>
            <label className="block text-xs font-semibold text-gray-300 mb-1.5 flex items-center gap-1.5">
              <Clock size={14} className="text-orange-400" />
              <span>Target Launch Countdown Timer (Optional)</span>
            </label>
            <input
              type="datetime-local"
              value={launchTargetDateTime}
              onChange={(e) => setLaunchTargetDateTime(e.target.value)}
              className="w-full px-4 py-3 rounded-xl bg-gray-800/80 border border-gray-700 text-white text-sm focus:outline-none focus:border-orange-500"
            />
            <p className="text-[11px] text-gray-400 mt-1">
              If fixed, backend auto-switches status to Live once this time passes.
            </p>
          </div>

          {/* Custom Launch Message */}
          <div>
            <label className="block text-xs font-semibold text-gray-300 mb-1.5 flex items-center gap-1.5">
              <Sparkles size={14} className="text-amber-400" />
              <span>Custom Announcement Banner</span>
            </label>
            <input
              type="text"
              placeholder="e.g. Launching soon across Delhi, Mumbai & Bengaluru!"
              value={launchMessage}
              onChange={(e) => setLaunchMessage(e.target.value)}
              className="w-full px-4 py-3 rounded-xl bg-gray-800/80 border border-gray-700 text-white text-sm focus:outline-none focus:border-orange-500"
            />
          </div>

          {/* Android Play Store URL */}
          <div>
            <label className="block text-xs font-semibold text-gray-300 mb-1.5 flex items-center gap-1.5">
              <Smartphone size={14} className="text-emerald-400" />
              <span>
                Android Google Play Store Link {isLinksRequired && <span className="text-red-400 font-bold">*</span>}
              </span>
            </label>
            <input
              type="url"
              required={isLinksRequired}
              placeholder="https://play.google.com/store/apps/details?id=com.bikepooling.app"
              value={androidAppUrl}
              onChange={(e) => setAndroidAppUrl(e.target.value)}
              className={`w-full px-4 py-3 rounded-xl bg-gray-800/80 border text-white text-sm focus:outline-none focus:border-orange-500 ${
                isLinksRequired && !androidAppUrl.trim() ? 'border-amber-500/60' : 'border-gray-700'
              }`}
            />
            <p className="text-[11px] text-gray-400 mt-1">
              {isLinksRequired ? 'Required for Live mode or fixed target timer' : 'Optional for standard Coming Soon mode'}
            </p>
          </div>

          {/* iOS App Store URL */}
          <div>
            <label className="block text-xs font-semibold text-gray-300 mb-1.5 flex items-center gap-1.5">
              <ExternalLink size={14} className="text-blue-400" />
              <span>
                Apple App Store Link {isLinksRequired && <span className="text-red-400 font-bold">*</span>}
              </span>
            </label>
            <input
              type="url"
              required={isLinksRequired}
              placeholder="https://apps.apple.com/app/bikepooling/id123456789"
              value={iosAppUrl}
              onChange={(e) => setIosAppUrl(e.target.value)}
              className={`w-full px-4 py-3 rounded-xl bg-gray-800/80 border text-white text-sm focus:outline-none focus:border-orange-500 ${
                isLinksRequired && !iosAppUrl.trim() ? 'border-amber-500/60' : 'border-gray-700'
              }`}
            />
            <p className="text-[11px] text-gray-400 mt-1">
              {isLinksRequired ? 'Required for Live mode or fixed target timer' : 'Optional for standard Coming Soon mode'}
            </p>
          </div>
        </div>

        {/* Action Button */}
        <div className="flex items-center justify-end pt-2 border-t border-white/10">
          <button
            type="submit"
            disabled={saving}
            className="px-6 py-3 rounded-xl font-bold text-white bg-gradient-to-r from-orange-500 to-amber-500 hover:from-orange-600 hover:to-amber-600 shadow-lg shadow-orange-500/25 flex items-center gap-2 text-sm transition-all disabled:opacity-70"
          >
            {saving ? (
              <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
            ) : (
              <>
                <Save size={16} />
                <span>Save Launch Configuration</span>
              </>
            )}
          </button>
        </div>
      </form>
    </div>
  );
};

export default AdminLaunchControlCard;

