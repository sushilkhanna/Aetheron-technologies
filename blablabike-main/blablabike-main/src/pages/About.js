import React, { useEffect, useState, useRef } from 'react';
import { Link } from 'react-router-dom';
import {
  ChevronRight, Heart, Target, Eye, Rocket, Users, Shield, Leaf,
  MapPin, Smartphone, Award, Zap, Clock, Globe, TrendingUp,
  CheckCircle, ArrowRight, Star, Bike
} from 'lucide-react';

/* ─── Animated counter hook ─── */
const useCountUp = (end, duration = 2000) => {
  const [count, setCount] = useState(0);
  const ref = useRef(null);
  const started = useRef(false);

  useEffect(() => {
    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting && !started.current) {
          started.current = true;
          const startTime = performance.now();
          const tick = (now) => {
            const elapsed = now - startTime;
            const progress = Math.min(elapsed / duration, 1);
            const eased = 1 - Math.pow(1 - progress, 3);
            setCount(Math.floor(eased * end));
            if (progress < 1) requestAnimationFrame(tick);
          };
          requestAnimationFrame(tick);
        }
      },
      { threshold: 0.3 }
    );
    if (ref.current) observer.observe(ref.current);
    return () => observer.disconnect();
  }, [end, duration]);

  return [count, ref];
};

/* ─── Fade-in-on-scroll wrapper ─── */
const FadeInSection = ({ children, className = '', delay = 0 }) => {
  const ref = useRef(null);
  const [visible, setVisible] = useState(false);

  useEffect(() => {
    const observer = new IntersectionObserver(
      ([entry]) => { if (entry.isIntersecting) setVisible(true); },
      { threshold: 0.15 }
    );
    if (ref.current) observer.observe(ref.current);
    return () => observer.disconnect();
  }, []);

  return (
    <div
      ref={ref}
      className={className}
      style={{
        opacity: visible ? 1 : 0,
        transform: visible ? 'translateY(0)' : 'translateY(40px)',
        transition: `opacity 0.7s ${delay}s ease-out, transform 0.7s ${delay}s ease-out`,
      }}
    >
      {children}
    </div>
  );
};

/* ═══════════════════════════════════════════════
   ABOUT PAGE
   ═══════════════════════════════════════════════ */
const About = () => {
  useEffect(() => { window.scrollTo(0, 0); }, []);

  return (
    <div className="mesh-bg min-h-screen pt-24 pb-20 text-gray-200">
      <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8">

        {/* ─── Breadcrumb ─── */}
        <div className="flex items-center gap-2 text-xs text-gray-400 mb-6">
          <Link to="/" className="hover:text-orange-400 transition-colors">Home</Link>
          <ChevronRight size={12} />
          <span className="text-gray-200">About Us</span>
        </div>

        {/* ─── Hero Banner ─── */}
        <FadeInSection>
          <div
            className="rounded-3xl p-8 sm:p-10 mb-14 relative overflow-hidden"
            style={{
              background: 'linear-gradient(135deg, rgba(255,112,0,0.18) 0%, rgba(20,20,30,0.85) 100%)',
              border: '1px solid rgba(255,112,0,0.3)',
            }}
          >
            <div className="absolute -top-16 -right-16 w-48 h-48 bg-orange-500/10 rounded-full blur-3xl pointer-events-none" />
            <div className="absolute -bottom-20 -left-10 w-40 h-40 bg-purple-500/10 rounded-full blur-3xl pointer-events-none" />

            <div className="flex items-center gap-4 mb-5 relative z-10">
              <div
                className="w-14 h-14 rounded-2xl flex items-center justify-center text-white shadow-lg shadow-orange-500/30"
                style={{ background: 'linear-gradient(135deg, #FF7000, #ff9a3c)' }}
              >
                <Heart size={28} />
              </div>
              <div>
                <h1 className="text-3xl sm:text-4xl font-black text-white">About BikePooling</h1>
                <p className="text-sm text-gray-400 mt-1">Our Story · Mission · Values</p>
              </div>
            </div>
            <p className="text-gray-300 text-base sm:text-lg leading-relaxed max-w-3xl relative z-10">
              We're building India's most trusted 2-wheeler ride-sharing community — connecting everyday commuters who share the same route, so everyone saves fuel, cuts traffic, and rides safer together.
            </p>
          </div>
        </FadeInSection>

        {/* ─── Our Story ─── */}
        <FadeInSection delay={0.1}>
          <section className="mb-14">
            <div className="flex items-center gap-3 mb-6">
              <div className="w-10 h-10 rounded-xl flex items-center justify-center bg-orange-500/15 border border-orange-500/25 text-orange-400">
                <Rocket size={20} />
              </div>
              <h2 className="text-2xl sm:text-3xl font-black text-white">Our Story</h2>
            </div>
            <div className="glass-dark rounded-2xl p-6 sm:p-8 border border-white border-opacity-10">
              <div className="space-y-4 text-sm sm:text-base text-gray-300 leading-relaxed">
                <p>
                  It started with a simple observation on the streets of India — millions of 2-wheelers zipping through traffic every morning, each carrying just one person, burning fuel, adding to congestion, and missing an obvious opportunity.
                </p>
                <p>
                  <strong className="text-white">What if every rider with a spare seat could share it with someone headed the same way?</strong>
                </p>
                <p>
                  That question sparked BikePooling. We're a team of engineers and mobility enthusiasts from across India who believe that sustainable, affordable daily commuting shouldn't require a car or expensive cab rides. Your 2-wheeler already has a pillion seat — let's put it to work.
                </p>
                <p>
                  We built BikePooling from the ground up with <span className="text-orange-400 font-semibold">safety first</span>, <span className="text-orange-400 font-semibold">government-verified identity</span>, and <span className="text-orange-400 font-semibold">100% legal cost-sharing</span> at its core. Every rider is Aadhaar and Driving Licence verified. Every route is tracked. Every ride has SOS protection.
                </p>
              </div>
            </div>
          </section>
        </FadeInSection>

        {/* ─── Mission & Vision Cards ─── */}
        <FadeInSection delay={0.15}>
          <section className="mb-14">
            <div className="grid sm:grid-cols-2 gap-6">
              {/* Mission */}
              <div className="rounded-2xl p-6 sm:p-8 relative overflow-hidden"
                style={{ background: 'linear-gradient(160deg, rgba(255,112,0,0.12) 0%, rgba(15,15,25,0.9) 100%)', border: '1px solid rgba(255,112,0,0.2)' }}
              >
                <div className="absolute -top-10 -right-10 w-28 h-28 bg-orange-500/10 rounded-full blur-2xl pointer-events-none" />
                <div className="flex items-center gap-3 mb-4 relative z-10">
                  <div className="w-10 h-10 rounded-xl flex items-center justify-center text-orange-400 bg-orange-500/15 border border-orange-500/25">
                    <Target size={20} />
                  </div>
                  <h3 className="text-xl font-bold text-white">Our Mission</h3>
                </div>
                <p className="text-sm text-gray-300 leading-relaxed relative z-10">
                  To make daily 2-wheeler commuting <strong className="text-white">affordable, safe, and social</strong> for every Indian — by enabling peer-to-peer ride sharing that is fully legal, government-verified, and eco-friendly.
                </p>
              </div>

              {/* Vision */}
              <div className="rounded-2xl p-6 sm:p-8 relative overflow-hidden"
                style={{ background: 'linear-gradient(160deg, rgba(124,58,237,0.12) 0%, rgba(15,15,25,0.9) 100%)', border: '1px solid rgba(124,58,237,0.2)' }}
              >
                <div className="absolute -top-10 -right-10 w-28 h-28 bg-purple-500/10 rounded-full blur-2xl pointer-events-none" />
                <div className="flex items-center gap-3 mb-4 relative z-10">
                  <div className="w-10 h-10 rounded-xl flex items-center justify-center text-purple-400 bg-purple-500/15 border border-purple-500/25">
                    <Eye size={20} />
                  </div>
                  <h3 className="text-xl font-bold text-white">Our Vision</h3>
                </div>
                <p className="text-sm text-gray-300 leading-relaxed relative z-10">
                  A future where every 2-wheeler pillion seat is occupied — reducing India's urban traffic by millions of vehicles, cutting carbon emissions, and creating a <strong className="text-white">connected commuter community</strong> nationwide.
                </p>
              </div>
            </div>
          </section>
        </FadeInSection>

        {/* ─── Impact Numbers ─── */}
        <FadeInSection delay={0.2}>
          <ImpactStats />
        </FadeInSection>

        {/* ─── Core Values ─── */}
        <FadeInSection delay={0.25}>
          <section className="mb-14">
            <div className="flex items-center gap-3 mb-6">
              <div className="w-10 h-10 rounded-xl flex items-center justify-center bg-orange-500/15 border border-orange-500/25 text-orange-400">
                <Star size={20} />
              </div>
              <h2 className="text-2xl sm:text-3xl font-black text-white">What We Stand For</h2>
            </div>
            <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-5">
              {[
                {
                  icon: Shield,
                  title: 'Safety First',
                  desc: 'Every rider is KYC-verified with Aadhaar & DL. Live GPS tracking and one-tap SOS ensure you\'re never alone on the road.',
                  color: 'text-blue-400',
                  bg: 'bg-blue-500/10',
                  border: 'border-blue-500/20',
                },
                {
                  icon: Award,
                  title: '100% Legal',
                  desc: 'Non-commercial fuel cost sharing under the Motor Vehicles Act. No grey areas — just genuine peer-to-peer commute sharing.',
                  color: 'text-orange-400',
                  bg: 'bg-orange-500/10',
                  border: 'border-orange-500/20',
                },
                {
                  icon: Leaf,
                  title: 'Eco Impact',
                  desc: 'Every shared ride means one fewer vehicle on the road. We\'re committed to measurably reducing India\'s urban carbon footprint.',
                  color: 'text-emerald-400',
                  bg: 'bg-emerald-500/10',
                  border: 'border-emerald-500/20',
                },
                {
                  icon: Users,
                  title: 'Community Trust',
                  desc: 'Transparent ratings, ride history, and verified profiles build a community where riders trust each other before they meet.',
                  color: 'text-purple-400',
                  bg: 'bg-purple-500/10',
                  border: 'border-purple-500/20',
                },
                {
                  icon: Zap,
                  title: 'Smart Technology',
                  desc: 'AI-powered route matching, real-time fare estimation, and intelligent scheduling make ride sharing effortless.',
                  color: 'text-amber-400',
                  bg: 'bg-amber-500/10',
                  border: 'border-amber-500/20',
                },
                {
                  icon: Heart,
                  title: 'Affordability',
                  desc: 'Save up to 50% on your daily commute costs. Split fuel, not your wallet — making mobility accessible for everyone.',
                  color: 'text-rose-400',
                  bg: 'bg-rose-500/10',
                  border: 'border-rose-500/20',
                },
              ].map((value, i) => (
                <div
                  key={i}
                  className={`glass-dark rounded-2xl p-5 border border-white border-opacity-10 hover:border-opacity-20 transition-all duration-300 group`}
                  style={{ cursor: 'default' }}
                >
                  <div className={`w-11 h-11 rounded-xl flex items-center justify-center ${value.bg} ${value.color} border ${value.border} mb-4 group-hover:scale-110 transition-transform duration-300`}>
                    <value.icon size={20} />
                  </div>
                  <h3 className="text-base font-bold text-white mb-2">{value.title}</h3>
                  <p className="text-xs sm:text-sm text-gray-400 leading-relaxed">{value.desc}</p>
                </div>
              ))}
            </div>
          </section>
        </FadeInSection>

        {/* ─── How We're Different ─── */}
        <FadeInSection delay={0.3}>
          <section className="mb-14">
            <div className="flex items-center gap-3 mb-6">
              <div className="w-10 h-10 rounded-xl flex items-center justify-center bg-orange-500/15 border border-orange-500/25 text-orange-400">
                <TrendingUp size={20} />
              </div>
              <h2 className="text-2xl sm:text-3xl font-black text-white">How We're Different</h2>
            </div>
            <div className="glass-dark rounded-2xl p-6 sm:p-8 border border-white border-opacity-10">
              <div className="space-y-5">
                {[
                  {
                    title: '2-Wheeler Exclusive',
                    desc: 'Unlike generic ride-share apps, we\'re purpose-built for India\'s 200M+ two-wheelers — the most popular mode of daily transport.',
                    icon: Bike,
                  },
                  {
                    title: 'Government-Grade KYC',
                    desc: 'Real-time Aadhaar & Driving Licence verification via official government database APIs. No fake profiles, ever.',
                    icon: Shield,
                  },
                  {
                    title: 'Not a Cab Service',
                    desc: 'We\'re a cost-sharing community, not a commercial taxi aggregator. Riders share fuel costs on their existing commute — fully legal under Indian law.',
                    icon: CheckCircle,
                  },
                  {
                    title: 'Route Intelligence',
                    desc: 'Our smart algorithm matches you with people heading along your exact daily route at your regular time — no detours, no waiting.',
                    icon: MapPin,
                  },
                  {
                    title: 'SOS & Live Tracking',
                    desc: 'One-tap emergency alerts, live ride tracking shared with trusted contacts, and 24/7 incident response for total peace of mind.',
                    icon: Smartphone,
                  },
                ].map((item, i) => (
                  <div key={i} className="flex items-start gap-4 p-4 rounded-xl hover:bg-white/[0.03] transition-all">
                    <div className="w-10 h-10 rounded-lg bg-orange-500/10 text-orange-400 border border-orange-500/20 flex items-center justify-center shrink-0 mt-0.5">
                      <item.icon size={18} />
                    </div>
                    <div>
                      <h4 className="text-sm font-bold text-white mb-1">{item.title}</h4>
                      <p className="text-xs sm:text-sm text-gray-400 leading-relaxed">{item.desc}</p>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </section>
        </FadeInSection>

        {/* ─── Timeline / Journey ─── */}
        <FadeInSection delay={0.35}>
          <section className="mb-14">
            <div className="flex items-center gap-3 mb-6">
              <div className="w-10 h-10 rounded-xl flex items-center justify-center bg-orange-500/15 border border-orange-500/25 text-orange-400">
                <Clock size={20} />
              </div>
              <h2 className="text-2xl sm:text-3xl font-black text-white">Our Journey</h2>
            </div>
            <div className="relative">
              {/* Vertical timeline line */}
              <div className="absolute left-5 top-0 bottom-0 w-px bg-gradient-to-b from-orange-500/50 via-orange-500/20 to-transparent hidden sm:block" />

              <div className="space-y-6">
                {[
                  { year: '2024', title: 'The Idea Was Born', desc: 'Identified the gap in India\'s 2-wheeler commuting space. Research and concept validation began.' },
                  { year: '2025', title: 'Building the Platform', desc: 'Core engineering — route matching engine, KYC integration, safety systems, and mobile app development.' },
                  { year: '2026', title: 'Launch & Beyond', desc: 'Platform launch with Govt-verified KYC, live SOS, and smart matching. Scaling across Indian cities.' },
                ].map((milestone, i) => (
                  <div key={i} className="flex items-start gap-5 relative">
                    <div className="w-10 h-10 rounded-full flex items-center justify-center shrink-0 z-10 font-bold text-xs"
                      style={{ background: 'linear-gradient(135deg, #FF7000, #ff9a3c)', color: 'white' }}>
                      {milestone.year.slice(-2)}
                    </div>
                    <div className="glass-dark rounded-xl p-5 border border-white border-opacity-10 flex-1">
                      <p className="text-[10px] font-bold uppercase tracking-widest text-orange-400 mb-1">{milestone.year}</p>
                      <h4 className="text-base font-bold text-white mb-1">{milestone.title}</h4>
                      <p className="text-xs sm:text-sm text-gray-400 leading-relaxed">{milestone.desc}</p>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </section>
        </FadeInSection>

        {/* ─── Contact / Get in Touch ─── */}
        <FadeInSection delay={0.4}>
          <section className="mb-8">
            <div className="rounded-2xl p-6 sm:p-8 relative overflow-hidden"
              style={{
                background: 'linear-gradient(135deg, rgba(255,112,0,0.12) 0%, rgba(15,15,25,0.9) 100%)',
                border: '1px solid rgba(255,112,0,0.25)',
              }}
            >
              <div className="absolute -top-14 -right-14 w-44 h-44 bg-orange-500/8 rounded-full blur-3xl pointer-events-none" />
              <div className="relative z-10 text-center">
                <div className="w-14 h-14 rounded-2xl flex items-center justify-center mx-auto mb-4 text-white shadow-lg shadow-orange-500/25"
                  style={{ background: 'linear-gradient(135deg, #FF7000, #ff9a3c)' }}>
                  <Globe size={26} />
                </div>
                <h2 className="text-2xl font-black text-white mb-2">Want to Know More?</h2>
                <p className="text-sm text-gray-400 mb-6 max-w-lg mx-auto">
                  We'd love to hear from you — whether you're a potential rider, partner, investor, or just curious about what we're building.
                </p>
                <div className="flex flex-col sm:flex-row gap-3 justify-center items-center">
                  <a
                    href="https://mail.google.com/mail/?view=cm&to=officialbikepooling.in@gmail.com"
                    target="_blank"
                    rel="noopener noreferrer"
                    className="btn-primary px-6 py-3 flex items-center gap-2 text-sm shadow-lg shadow-orange-500/25"
                  >
                    <span>officialbikepooling.in@gmail.com</span>
                    <ArrowRight size={16} />
                  </a>
                  <Link
                    to="/"
                    className="btn-outline px-6 py-3 flex items-center gap-2 text-sm"
                  >
                    <span>Back to Home</span>
                  </Link>
                </div>
              </div>
            </div>
          </section>
        </FadeInSection>

      </div>
    </div>
  );
};

/* ─── Impact Stats Sub-component ─── */
const ImpactStats = () => {
  const stats = [
    { end: 200, suffix: 'M+', label: '2-Wheelers in India', icon: Bike },
    { end: 50, suffix: '%', label: 'Commute Cost Savings', icon: TrendingUp },
    { end: 24, suffix: '/7', label: 'Safety & SOS Support', icon: Shield },
    { end: 100, suffix: '%', label: 'Legal & Compliant', icon: Award },
  ];

  return (
    <section className="mb-14">
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        {stats.map((stat, i) => {
          // eslint-disable-next-line react-hooks/rules-of-hooks
          const [count, ref] = useCountUp(stat.end, 1800);
          return (
            <div
              key={i}
              ref={ref}
              className="glass-dark rounded-2xl p-5 sm:p-6 border border-white border-opacity-10 text-center group hover:border-orange-500/30 transition-all duration-300"
            >
              <div className="w-11 h-11 rounded-xl flex items-center justify-center mx-auto mb-3 bg-orange-500/10 text-orange-400 border border-orange-500/20 group-hover:scale-110 transition-transform duration-300">
                <stat.icon size={20} />
              </div>
              <p className="text-2xl sm:text-3xl font-black text-white mb-1">
                {count}<span className="gradient-text">{stat.suffix}</span>
              </p>
              <p className="text-xs text-gray-400 font-medium">{stat.label}</p>
            </div>
          );
        })}
      </div>
    </section>
  );
};

export default About;
