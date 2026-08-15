import React, { useState, useEffect } from 'react';
import { Shield, CheckCircle, AlertCircle, Smartphone, MapPin, Users, Zap, Award, Lock, Leaf, Sparkles } from 'lucide-react';
import getApiConfig from '../config/api';
import ComingSoonModal from '../components/ComingSoonModal';

const heroBgs = [
  'https://images.unsplash.com/photo-1558618666-fcd25c85cd64?w=1920&h=1080&fit=crop',
  'https://images.unsplash.com/photo-1596176530529-78163a4f7af2?w=1920&h=1080&fit=crop',
  'https://images.unsplash.com/photo-1587474260584-136574528ed5?w=1920&h=1080&fit=crop',
  'https://images.unsplash.com/photo-1486325212027-8081e485255e?w=1920&h=1080&fit=crop',
];

const Home = () => {
  const [bgIndex, setBgIndex] = useState(0);
  const [modalOpen, setModalOpen] = useState(false);
  const [selectedPlatform, setSelectedPlatform] = useState('app');
  const [launchConfig, setLaunchConfig] = useState(null);

  useEffect(() => {
    const timer = setInterval(() => setBgIndex(i => (i + 1) % heroBgs.length), 4000);
    return () => clearInterval(timer);
  }, []);

  useEffect(() => {
    const fetchLaunchConfig = async () => {
      try {
        const { baseURL } = getApiConfig();
        const res = await fetch(`${baseURL}/public/launch-config`);
        if (res.ok) {
          const data = await res.json();
          if (data.data) setLaunchConfig(data.data);
        }
      } catch (err) {
        console.warn('Could not fetch public launch config:', err);
      }
    };
    fetchLaunchConfig();
  }, []);

  const handleOpenModal = (platform = 'app') => {
    if (launchConfig && launchConfig.launchMode === 'LIVE_LAUNCHED') {
      let targetUrl = '';
      if (platform === 'android') targetUrl = launchConfig.androidAppUrl;
      else if (platform === 'ios') targetUrl = launchConfig.iosAppUrl;
      else targetUrl = launchConfig.androidAppUrl || launchConfig.iosAppUrl;

      if (targetUrl && targetUrl.trim().length > 0) {
        window.open(targetUrl.trim(), '_blank');
        return;
      }
    }
    setSelectedPlatform(platform);
    setModalOpen(true);
  };

  return (
    <div className="mesh-bg min-h-screen pt-20 sm:pt-24">
      <HeroSection 
        bgIndex={bgIndex} 
        setBgIndex={setBgIndex} 
        onOpenDownloadModal={handleOpenModal} 
      />
      <FeaturesSection />
      <HowItWorks />
      <CTASection onOpenDownloadModal={handleOpenModal} />
      <ComingSoonModal 
        isOpen={modalOpen} 
        onClose={() => setModalOpen(false)} 
        platform={selectedPlatform} 
        launchTargetDateTime={launchConfig?.launchTargetDateTime}
        launchMessage={launchConfig?.launchMessage}
      />
    </div>
  );
};

const HeroSection = ({ bgIndex, setBgIndex, onOpenDownloadModal }) => (
  <section className="relative min-h-[calc(100vh-5rem)] sm:min-h-[calc(100vh-6rem)] flex items-start overflow-hidden pt-16 sm:pt-20">
    {heroBgs.map((src, i) => (
      <div key={i} className="carousel-slide absolute inset-0 bg-cover bg-center"
        style={{ backgroundImage: `url(${src})`, opacity: i === bgIndex ? 1 : 0 }} />
    ))}
    <div className="absolute inset-0" style={{ background: 'linear-gradient(135deg, rgba(0,0,0,0.88) 0%, rgba(0,0,0,0.65) 50%, rgba(0,0,0,0.8) 100%)' }} />
    <div className="absolute inset-0" style={{ background: 'radial-gradient(ellipse at 20% 50%, rgba(255,112,0,0.15) 0%, transparent 65%)' }} />

    <div className="absolute bottom-8 left-1/2 -translate-x-1/2 flex gap-2 z-20">
      {heroBgs.map((_, i) => (
        <button key={i} onClick={() => setBgIndex(i)}
          className={`h-1.5 rounded-full transition-all duration-300 ${i === bgIndex ? 'w-8 bg-primary-orange' : 'w-1.5 bg-white bg-opacity-30'}`} />
      ))}
    </div>

    <div className="relative z-10 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pt-1 pb-20 w-full">
      <div className="grid lg:grid-cols-2 gap-12 items-center">
        <div>
          {/* Top Heading Branding with Video Logo */}
          <div className="flex items-center gap-3.5 mb-6 fade-up-1">
            <div className="w-14 h-14 rounded-2xl overflow-hidden p-0.5 bg-gradient-to-tr from-orange-500 via-amber-400 to-orange-600 shadow-xl shadow-orange-500/30 border border-orange-400/40 transform hover:scale-105 transition-transform duration-300">
              <video
                src="/bikepooling.mp4"
                autoPlay
                loop
                muted
                playsInline
                className="w-full h-full object-cover rounded-[14px]"
              />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <span className="text-2xl sm:text-3xl font-black tracking-tight text-white">
                  Bike<span className="gradient-text">Pool</span>ing
                </span>
                <span className="px-2 py-0.5 rounded-full bg-orange-500/20 border border-orange-500/30 text-orange-400 text-[10px] font-bold uppercase tracking-wider">
                  Official
                </span>
              </div>
              <span className="text-xs text-gray-300 font-medium">Smart 2-Wheeler Ride Sharing Platform</span>
            </div>
          </div>

          <h1 className="text-4xl sm:text-5xl md:text-6xl font-black leading-tight mb-6 fade-up-2">
            <span className="text-white">Got a spare seat?</span><br />
            <span className="gradient-text">Share your ride.</span><br />
            <span className="text-white">Split the cost.</span>
          </h1>

          <p className="text-base sm:text-lg text-gray-300 mb-8 leading-relaxed max-w-lg fade-up-3">
            Post your daily 2-wheeler commute and let someone going the same way join you. Save fuel costs, reduce traffic, and travel safely.
          </p>

          {/* Authentic Trust Commitments Grid (Replaces old stat counters) */}
          <div className="grid grid-cols-3 gap-3 mb-8 fade-up-3">
            {[
              { title: 'Govt DB Verified', desc: 'Instant DL & Aadhaar KYC' },
              { title: '100% Legal', desc: 'MV Act Cost Sharing' },
              { title: 'Safe & Encrypted', desc: '256-bit SSL & Live SOS' },
            ].map((item, i) => (
              <div key={i} className="rounded-xl p-3 text-center bg-white/5 border border-white/10 backdrop-blur-sm">
                <p className="text-xs sm:text-sm font-bold text-orange-400 mb-0.5">{item.title}</p>
                <p className="text-[11px] text-gray-400">{item.desc}</p>
              </div>
            ))}
          </div>

          <div className="flex flex-col sm:flex-row gap-3 fade-up-4">
            <button
              onClick={() => onOpenDownloadModal('app')}
              className="btn-primary px-7 py-3.5 flex items-center justify-center gap-2 shadow-lg shadow-orange-500/25"
            >
              <Smartphone size={18} />
              <span>Download the App</span>
            </button>
          </div>
        </div>

        {/* Authentic Trust & Compliance Certificate Showcase (Replaces old mock routes) */}
        <div className="hidden lg:block fade-up-2">
          <div className="rounded-2xl overflow-hidden bg-slate-900/60 border border-white/10 backdrop-blur-xl shadow-2xl p-6 relative">
            <div className="absolute -top-10 -right-10 w-36 h-36 bg-orange-500/10 rounded-full blur-2xl pointer-events-none" />
            
            <div className="pb-4 mb-4 border-b border-white/10 flex items-center justify-between">
              <div className="flex items-center gap-3">
                <div className="w-9 h-9 rounded-xl bg-orange-500/20 text-orange-400 border border-orange-500/30 flex items-center justify-center">
                  <Award size={20} />
                </div>
                <div>
                  <h3 className="text-base font-bold text-white">Trust & Compliance</h3>
                  <p className="text-xs text-gray-400">Verified Mobility Standards</p>
                </div>
              </div>
              <span className="text-xs font-semibold px-2.5 py-1 rounded-full bg-emerald-500/15 border border-emerald-500/30 text-emerald-400 flex items-center gap-1.5">
                <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse" />
                Govt API Ready
              </span>
            </div>

            <div className="space-y-3.5">
              {[
                {
                  icon: Shield,
                  title: 'Aadhaar & Driving Licence KYC',
                  desc: 'Instant real-time verification powered by Official Government Database APIs.',
                  badge: 'Verified Identity',
                  color: 'text-blue-400 bg-blue-500/10 border-blue-500/20'
                },
                {
                  icon: Award,
                  title: 'Motor Vehicles Act Compliant',
                  desc: 'Non-commercial 2-wheeler peer-to-peer fuel cost sharing model.',
                  badge: '100% Legal',
                  color: 'text-orange-400 bg-orange-500/10 border-orange-500/20'
                },
                {
                  icon: Lock,
                  title: 'Bank-Grade Data Privacy & SOS',
                  desc: 'Encrypted communication, emergency contact sharing & route monitoring.',
                  badge: 'ISO Encrypted',
                  color: 'text-purple-400 bg-purple-500/10 border-purple-500/20'
                },
                {
                  icon: Leaf,
                  title: 'Eco-Mobility & Carbon Reduction',
                  desc: 'Dedicated to reducing urban traffic congestion and daily carbon emissions.',
                  badge: 'Green Initiative',
                  color: 'text-emerald-400 bg-emerald-500/10 border-emerald-500/20'
                }
              ].map((cert, idx) => (
                <div key={idx} className="p-3.5 rounded-xl bg-white/[0.03] border border-white/[0.06] flex items-start gap-3.5 hover:bg-white/[0.05] transition-all">
                  <div className="p-2 rounded-lg bg-orange-500/10 text-orange-400 border border-orange-500/20 shrink-0 mt-0.5">
                    <cert.icon size={18} />
                  </div>
                  <div className="flex-1">
                    <div className="flex items-center justify-between mb-1">
                      <h4 className="text-sm font-bold text-white">{cert.title}</h4>
                      <span className={`text-[10px] font-bold px-2 py-0.5 rounded border ${cert.color}`}>
                        {cert.badge}
                      </span>
                    </div>
                    <p className="text-xs text-gray-400 leading-relaxed">{cert.desc}</p>
                  </div>
                </div>
              ))}
            </div>

            <div className="mt-5 pt-3.5 border-t border-white/10 flex items-center justify-between text-xs text-gray-400">
              <div className="flex items-center gap-1.5">
                <CheckCircle size={14} className="text-emerald-400" />
                <span>Verified Governance</span>
              </div>
              <div className="flex items-center gap-1.5">
                <Sparkles size={14} className="text-amber-400" />
                <span>Smart Match Algorithm</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </section>
);

const HowItWorks = () => (
  <section className="py-24 section-light">
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
      <div className="text-center mb-14">
        <p className="text-xs font-bold uppercase tracking-widest text-primary-orange mb-3">How it works</p>
        <h2 className="text-4xl font-black text-gray-900">Ride Together in 4 Steps</h2>
      </div>
      <div className="grid md:grid-cols-4 gap-6">
        {[
          { n: '01', title: 'Download App', desc: 'Get the BikePooling app from your app store once launched.' },
          { n: '02', title: 'Create Account', desc: 'Sign up and verify your identity with quick Govt KYC.' },
          { n: '03', title: 'Find a Ride', desc: 'Search for riders going your way or post your daily commute.' },
          { n: '04', title: 'Share & Save', desc: 'Split fuel costs, reduce traffic, and ride together safely.' },
        ].map((step, i) => (
          <div key={i} className="bg-white rounded-2xl p-6 shadow-sm hover:shadow-xl transition-all card-hover border border-gray-100">
            <div className="w-12 h-12 rounded-xl flex items-center justify-center mb-4 text-white font-black text-sm"
              style={{ background: 'linear-gradient(135deg, #FF7000, #ff9a3c)' }}>
              {step.n}
            </div>
            <h3 className="font-bold text-gray-900 mb-2">{step.title}</h3>
            <p className="text-gray-500 text-sm leading-relaxed">{step.desc}</p>
          </div>
        ))}
      </div>
    </div>
  </section>
);

const FeaturesSection = () => (
  <section className="py-24" style={{ background: '#0d0d15' }}>
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
      <div className="text-center mb-14">
        <p className="text-xs font-bold uppercase tracking-widest text-primary-orange mb-3">Features</p>
        <h2 className="text-4xl font-black text-white">Why Choose BikePooling?</h2>
      </div>
      <div className="grid md:grid-cols-3 gap-8">
        {[
          { 
            icon: Users, 
            title: 'Community Driven', 
            desc: 'Join a growing community of smart commuters who share rides and split costs daily.',
            color: 'bg-blue-500'
          },
          { 
            icon: Shield, 
            title: 'Verified Riders', 
            desc: 'Every rider completes Aadhaar and Driving Licence verification for maximum trust.',
            color: 'bg-green-500'
          },
          { 
            icon: Award, 
            title: 'Cost Sharing', 
            desc: 'Fair, transparent cost sharing compliant with Indian transport regulations.',
            color: 'bg-yellow-500'
          },
          { 
            icon: MapPin, 
            title: 'Smart Route Match', 
            desc: 'Intelligent matching algorithm finds commuters heading along your exact route.',
            color: 'bg-purple-500'
          },
          { 
            icon: Zap, 
            title: 'Instant Ride Setup', 
            desc: 'Post or request a ride in seconds right from your phone.',
            color: 'bg-orange-500'
          },
          { 
            icon: AlertCircle, 
            title: 'Emergency SOS', 
            desc: 'Built-in emergency alert button with real-time location sharing.',
            color: 'bg-red-500'
          }
        ].map((feature, i) => (
          <div key={i} className="text-center">
            <div className={`w-16 h-16 ${feature.color} rounded-2xl flex items-center justify-center mx-auto mb-6`}>
              <feature.icon size={32} className="text-white" />
            </div>
            <h3 className="text-xl font-bold text-white mb-4">{feature.title}</h3>
            <p className="text-gray-300 leading-relaxed">{feature.desc}</p>
          </div>
        ))}
      </div>
    </div>
  </section>
);

const CTASection = ({ onOpenDownloadModal }) => (
  <section id="download-section" className="py-24 section-light">
    <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
      <div className="mb-12">
        <h2 className="text-4xl font-black text-gray-900 mb-4">
          Ready to Ride Together?
        </h2>
        <p className="text-lg text-gray-600 max-w-2xl mx-auto">
          Get ready for the BikePooling mobile app. Share your daily commute, save fuel costs, and make daily travel effortless.
        </p>
      </div>
      
      <div className="flex flex-col sm:flex-row gap-4 justify-center">
        <button 
          onClick={() => onOpenDownloadModal('android')}
          className="btn-primary px-8 py-4 flex items-center justify-center gap-2 transform hover:scale-105 transition-all duration-200 shadow-lg hover:shadow-xl"
        >
          <Smartphone size={20} />
          <span>Download for Android</span>
        </button>
        <button 
          onClick={() => onOpenDownloadModal('ios')}
          className="btn-outline px-8 py-4 flex items-center justify-center gap-2 !border-gray-300 !text-gray-700 hover:!border-orange-400 hover:!text-orange-500 hover:!bg-orange-50"
        >
          <Smartphone size={20} />
          <span>Download for iOS</span>
        </button>
      </div>
      
      <div className="mt-12 flex flex-wrap justify-center gap-8">
        <div className="flex items-center gap-2">
          <CheckCircle className="text-green-500" size={20} />
          <span className="text-gray-600">Free to register</span>
        </div>
        <div className="flex items-center gap-2">
          <CheckCircle className="text-green-500" size={20} />
          <span className="text-gray-600">Govt KYC Verified</span>
        </div>
        <div className="flex items-center gap-2">
          <CheckCircle className="text-green-500" size={20} />
          <span className="text-gray-600">24/7 Safety SOS</span>
        </div>
      </div>
    </div>
  </section>
);

export default Home;
