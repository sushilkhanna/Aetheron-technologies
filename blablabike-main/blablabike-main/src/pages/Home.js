import React, { useState, useEffect } from 'react';
import { User, Shield, ArrowRight, Star, CheckCircle, AlertCircle, Smartphone, MapPin, Users, Zap } from 'lucide-react';

const heroBgs = [
  'https://images.unsplash.com/photo-1558618666-fcd25c85cd64?w=1920&h=1080&fit=crop',
  'https://images.unsplash.com/photo-1596176530529-78163a4f7af2?w=1920&h=1080&fit=crop',
  'https://images.unsplash.com/photo-1587474260584-136574528ed5?w=1920&h=1080&fit=crop',
  'https://images.unsplash.com/photo-1486325212027-8081e485255e?w=1920&h=1080&fit=crop',
];

const Home = () => {
  const [bgIndex, setBgIndex] = useState(0);

  useEffect(() => {
    const timer = setInterval(() => setBgIndex(i => (i + 1) % heroBgs.length), 4000);
    return () => clearInterval(timer);
  }, []);

  return (
    <div className="mesh-bg min-h-screen pt-20 sm:pt-24">
      <HeroSection bgIndex={bgIndex} setBgIndex={setBgIndex} />
      <FeaturesSection />
      <HowItWorks />
      <CTASection />
    </div>
  );
};

const HeroSection = ({ bgIndex, setBgIndex }) => (
  <section className="relative min-h-[calc(100vh-5rem)] sm:min-h-[calc(100vh-6rem)] flex items-start overflow-hidden pt-20 sm:pt-24">
    {heroBgs.map((src, i) => (
      <div key={i} className="carousel-slide absolute inset-0 bg-cover bg-center"
        style={{ backgroundImage: `url(${src})`, opacity: i === bgIndex ? 1 : 0 }} />
    ))}
    <div className="absolute inset-0" style={{ background: 'linear-gradient(135deg, rgba(0,0,0,0.85) 0%, rgba(0,0,0,0.6) 50%, rgba(0,0,0,0.75) 100%)' }} />
    <div className="absolute inset-0" style={{ background: 'radial-gradient(ellipse at 20% 50%, rgba(255,112,0,0.12) 0%, transparent 60%)' }} />

    <div className="absolute bottom-8 left-1/2 -translate-x-1/2 flex gap-2 z-20">
      {heroBgs.map((_, i) => (
        <button key={i} onClick={() => setBgIndex(i)}
          className={`h-1.5 rounded-full transition-all duration-300 ${i === bgIndex ? 'w-8 bg-primary-orange' : 'w-1.5 bg-white bg-opacity-30'}`} />
      ))}
    </div>

    <div className="relative z-10 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pt-1 pb-20 w-full">
      <div className="grid lg:grid-cols-2 gap-16 items-center">
        <div>
          <div className="inline-flex items-center gap-2 px-3 py-1.5 rounded-full mb-6 fade-up-1"
            style={{ background: 'rgba(255,112,0,0.1)', border: '1px solid rgba(255,112,0,0.25)' }}>
            <div className="relative w-2 h-2">
              <div className="w-2 h-2 bg-green-400 rounded-full" />
              <div className="absolute inset-0 w-2 h-2 bg-green-400 rounded-full animate-ping" />
            </div>
            <span className="text-xs font-medium" style={{ color: '#ff9a3c' }}>Live in Delhi, Mumbai & Bengaluru</span>
          </div>

          <h1 className="text-5xl md:text-7xl font-black leading-tight mb-6 fade-up-2">
            <span className="text-white">Got a spare seat?</span><br />
            <span className="gradient-text">Share your ride.</span><br />
            <span className="text-white">Split the cost</span>
          </h1>

          <p className="text-lg text-gray-300 mb-8 leading-relaxed max-w-lg fade-up-3">
            Post your daily 2-wheeler commute and let someone going the same way join you. Save fuel. Split the cost.
          </p>

          <div className="grid grid-cols-3 gap-3 mb-8 fade-up-3">
            {[
              { value: '5.4K+', label: 'Active Commuters' },
              { value: '1.8K+', label: 'Verified Riders' },
              { value: '98.6%', label: 'Safety Score' },
            ].map((s, i) => (
              <div key={i} className="rounded-xl p-3 text-center"
                style={{ background: 'rgba(255,255,255,0.05)', border: '1px solid rgba(255,255,255,0.08)' }}>
                <p className="text-xl font-bold gradient-text">{s.value}</p>
                <p className="text-xs text-gray-400 mt-0.5">{s.label}</p>
              </div>
            ))}
          </div>

          <div className="flex flex-col sm:flex-row gap-3 fade-up-4">
            <button
              onClick={() => {
                document.getElementById('download-section')?.scrollIntoView({ behavior: 'smooth' });
              }}
              className="btn-primary px-7 py-3.5 flex items-center justify-center gap-2"
            >
              <Smartphone size={18} />
              <span>Download the App</span>
            </button>
          </div>
        </div>

        <div className="hidden lg:block fade-up-2">
          <div className="rounded-2xl overflow-hidden"
            style={{ background: 'rgba(255,255,255,0.04)', border: '1px solid rgba(255,255,255,0.1)', backdropFilter: 'blur(20px)' }}>
            <div className="px-5 py-4 flex items-center justify-between"
              style={{ borderBottom: '1px solid rgba(255,255,255,0.06)' }}>
              <div className="flex items-center gap-2">
                <div className="w-2 h-2 bg-green-400 rounded-full animate-pulse" />
                <span className="text-white text-sm font-medium">Live Commute Routes</span>
              </div>
              <span className="text-green-400 text-xs font-semibold px-2 py-0.5 rounded bg-green-500/10 border border-green-500/20">Active Now</span>
            </div>
            <div className="p-4 space-y-3">
              {[
                { from: 'Delhi CP', to: 'Gurgaon Cyber City', time: '8:30 AM', seats: 1, rating: 4.9, price: '₹45' },
                { from: 'Mumbai BKC', to: 'Andheri West', time: '9:15 AM', seats: 1, rating: 4.8, price: '₹35' },
                { from: 'Bengaluru Indiranagar', to: 'Whitefield Tech Park', time: '8:00 AM', seats: 1, rating: 4.9, price: '₹50' },
              ].map((r, i) => (
                <div key={i} className="flex items-center justify-between p-3 rounded-lg"
                  style={{ background: 'rgba(255,255,255,0.03)', border: '1px solid rgba(255,255,255,0.05)' }}>
                  <div className="flex items-center gap-3">
                    <div className="w-8 h-8 rounded-lg flex items-center justify-center"
                      style={{ background: 'rgba(255,112,0,0.2)' }}>
                      <span className="text-xs font-bold text-orange-400">{r.seats}</span>
                    </div>
                    <div>
                      <p className="text-sm font-semibold text-white">{r.from} <span className="text-gray-500">→</span> {r.to}</p>
                      <div className="flex items-center gap-2 mt-0.5">
                        <span className="text-xs text-gray-500">{r.time}</span>
                        <span className="text-xs text-gray-600">·</span>
                        <span className="text-xs text-gray-500">{r.seats} seat available</span>
                        <span className="text-xs text-gray-600">·</span>
                        <Star size={9} className="text-yellow-400 fill-yellow-400" />
                        <span className="text-xs text-gray-500">{r.rating}</span>
                      </div>
                    </div>
                  </div>
                  <span className="text-sm font-bold gradient-text">{r.price}</span>
                </div>
              ))}
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
          { n: '01', title: 'Download App', desc: 'Get the BikePooling app from your favorite app store.' },
          { n: '02', title: 'Create Account', desc: 'Sign up and verify your identity with quick KYC.' },
          { n: '03', title: 'Find a Ride', desc: 'Search for riders going your way or post your own commute.' },
          { n: '04', title: 'Share & Save', desc: 'Split the cost, save fuel, and reduce traffic together.' },
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
            desc: 'Every rider goes through KYC verification ensuring safety and trust in every ride.',
            color: 'bg-green-500'
          },
          { 
            icon: Star, 
            title: 'Ratings & Reviews', 
            desc: 'Rate and review your ride partners. Build a trusted reputation in the community.',
            color: 'bg-yellow-500'
          },
          { 
            icon: MapPin, 
            title: 'Smart Matching', 
            desc: 'Our algorithm finds riders heading your way so you never ride alone on your commute.',
            color: 'bg-purple-500'
          },
          { 
            icon: Zap, 
            title: 'Instant Booking', 
            desc: 'Book a ride in seconds. Quick, easy, and hassle-free — right from your phone.',
            color: 'bg-orange-500'
          },
          { 
            icon: AlertCircle, 
            title: '24/7 Support', 
            desc: 'Round-the-clock support team ready to help with any issues or concerns.',
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

const CTASection = () => (
  <section id="download-section" className="py-24 section-light">
    <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
      <div className="mb-12">
        <h2 className="text-4xl font-black text-gray-900 mb-4">
          Ready to Ride Together?
        </h2>
        <p className="text-lg text-gray-600 max-w-2xl mx-auto">
          Download the BikePooling app and start sharing your daily commute. Save money, reduce traffic, and make new connections.
        </p>
      </div>
      
      <div className="flex flex-col sm:flex-row gap-4 justify-center">
        <button 
          className="btn-primary px-8 py-4 flex items-center justify-center gap-2 transform hover:scale-105 transition-all duration-200 shadow-lg hover:shadow-xl"
        >
          <Smartphone size={20} />
          <span>Download for Android</span>
        </button>
        <button 
          className="btn-outline px-8 py-4 flex items-center justify-center gap-2 !border-gray-300 !text-gray-700 hover:!border-orange-400 hover:!text-orange-500 hover:!bg-orange-50"
        >
          <Smartphone size={20} />
          <span>Download for iOS</span>
        </button>
      </div>
      
      <div className="mt-12 flex flex-wrap justify-center gap-8">
        <div className="flex items-center gap-2">
          <CheckCircle className="text-green-500" size={20} />
          <span className="text-gray-600">Free to use</span>
        </div>
        <div className="flex items-center gap-2">
          <CheckCircle className="text-green-500" size={20} />
          <span className="text-gray-600">Verified riders</span>
        </div>
        <div className="flex items-center gap-2">
          <CheckCircle className="text-green-500" size={20} />
          <span className="text-gray-600">24/7 support</span>
        </div>
      </div>
    </div>
  </section>
);

export default Home;
