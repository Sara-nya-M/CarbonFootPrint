import { useState, useEffect } from 'react';
import './App.css';

const API_BASE = '/api';

function App() {
  // Authentication & session state
  const [token, setToken] = useState(localStorage.getItem('token') || '');
  const [userEmail, setUserEmail] = useState(localStorage.getItem('userEmail') || '');
  
  // Auth Form state
  const [isRegister, setIsRegister] = useState(false);
  const [authEmail, setAuthEmail] = useState('');
  const [authPassword, setAuthPassword] = useState('');
  const [authPhone, setAuthPhone] = useState('');
  const [authError, setAuthError] = useState('');
  const [authSuccess, setAuthSuccess] = useState('');

  // Dashboard Data State
  const [dashboardData, setDashboardData] = useState(null);
  const [todayActivity, setTodayActivity] = useState(null);
  const [historyList, setHistoryList] = useState([]);
  const [activeTab, setActiveTab] = useState('dashboard');
  const [loading, setLoading] = useState(false);

  // Activity Form State
  const [formDate, setFormDate] = useState(new Date().toISOString().split('T')[0]);
  const [transportType, setTransportType] = useState('NONE');
  const [transportDistance, setTransportDistance] = useState(0);
  const [electricityUnits, setElectricityUnits] = useState(0);
  const [foodHabit, setFoodHabit] = useState('VEG');
  const [plasticItems, setPlasticItems] = useState(0);
  const [formError, setFormError] = useState('');
  const [formSuccess, setFormSuccess] = useState('');

  // Auto-clear success/error messages
  useEffect(() => {
    if (authError || authSuccess) {
      const timer = setTimeout(() => {
        setAuthError('');
        setAuthSuccess('');
      }, 5000);
      return () => clearTimeout(timer);
    }
  }, [authError, authSuccess]);

  useEffect(() => {
    if (formError || formSuccess) {
      const timer = setTimeout(() => {
        setFormError('');
        setFormSuccess('');
      }, 5000);
      return () => clearTimeout(timer);
    }
  }, [formError, formSuccess]);

  // Load user data on token update
  useEffect(() => {
    if (token) {
      fetchDashboard();
      fetchTodayActivity();
      fetchHistory();
    }
  }, [token]);

  const handleLogin = async (e) => {
    e.preventDefault();
    setAuthError('');
    try {
      const response = await fetch(`${API_BASE}/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email: authEmail, password: authPassword }),
      });
      const data = await response.json();
      if (!response.ok) {
        throw new Error(data.error || 'Login failed');
      }
      localStorage.setItem('token', data.token);
      localStorage.setItem('userEmail', data.email);
      setToken(data.token);
      setUserEmail(data.email);
      setAuthEmail('');
      setAuthPassword('');
    } catch (err) {
      setAuthError(err.message);
    }
  };

  const handleRegister = async (e) => {
    e.preventDefault();
    setAuthError('');
    setAuthSuccess('');
    try {
      const response = await fetch(`${API_BASE}/auth/register`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email: authEmail, password: authPassword, phoneNumber: authPhone }),
      });
      const data = await response.json();
      if (!response.ok) {
        // Validation check
        const fields = Object.keys(data).filter(k => k !== 'error');
        if (fields.length > 0) {
          throw new Error(`${fields[0]}: ${data[fields[0]]}`);
        }
        throw new Error(data.error || 'Registration failed');
      }
      setAuthSuccess('Registration successful! Please login.');
      setIsRegister(false);
      setAuthPassword('');
      setAuthPhone('');
    } catch (err) {
      setAuthError(err.message);
    }
  };

  const handleLogout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('userEmail');
    setToken('');
    setUserEmail('');
    setDashboardData(null);
    setTodayActivity(null);
    setHistoryList([]);
    setActiveTab('dashboard');
  };

  const fetchDashboard = async () => {
    try {
      const response = await fetch(`${API_BASE}/dashboard`, {
        headers: { 'Authorization': `Bearer ${token}` }
      });
      if (response.status === 403 || response.status === 401) {
        handleLogout();
        return;
      }
      if (!response.ok) throw new Error('Failed to fetch dashboard');
      const data = await response.json();
      setDashboardData(data);
    } catch (err) {
      console.error(err);
    }
  };

  const fetchTodayActivity = async () => {
    try {
      const response = await fetch(`${API_BASE}/activities/today`, {
        headers: { 'Authorization': `Bearer ${token}` }
      });
      if (!response.ok) throw new Error('Failed to fetch today\'s activity');
      const data = await response.json();
      if (data && data.id) {
        setTodayActivity(data);
        // Pre-fill form
        setTransportType(data.transportType);
        setTransportDistance(data.transportDistance);
        setElectricityUnits(data.electricityUnits);
        setFoodHabit(data.foodHabit);
        setPlasticItems(data.plasticItems);
        if (data.date) {
          setFormDate(data.date);
        }
      } else {
        setTodayActivity(null);
        // Reset form
        setTransportType('NONE');
        setTransportDistance(0);
        setElectricityUnits(0);
        setFoodHabit('VEG');
        setPlasticItems(0);
        setFormDate(new Date().toISOString().split('T')[0]);
      }
    } catch (err) {
      console.error(err);
    }
  };

  const fetchHistory = async () => {
    try {
      const response = await fetch(`${API_BASE}/activities/history`, {
        headers: { 'Authorization': `Bearer ${token}` }
      });
      if (!response.ok) throw new Error('Failed to fetch history');
      const data = await response.json();
      setHistoryList(data);
    } catch (err) {
      console.error(err);
    }
  };

  const handleLogActivity = async (e) => {
    e.preventDefault();
    setFormError('');
    setFormSuccess('');
    setLoading(true);

    try {
      const response = await fetch(`${API_BASE}/activities`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify({
          date: formDate,
          transportType,
          transportDistance: parseFloat(transportDistance),
          electricityUnits: parseFloat(electricityUnits),
          foodHabit,
          plasticItems: parseInt(plasticItems),
        })
      });
      const data = await response.json();
      if (!response.ok) {
        throw new Error(data.error || 'Failed to log activity');
      }
      setFormSuccess('Activity logged successfully!');
      fetchDashboard();
      fetchTodayActivity();
      fetchHistory();
      
      // Auto redirect to dashboard
      setTimeout(() => {
        setActiveTab('dashboard');
      }, 1000);
    } catch (err) {
      setFormError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleDownloadReport = async () => {
    try {
      const response = await fetch(`${API_BASE}/activities/report/pdf`, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });
      if (!response.ok) throw new Error('Failed to generate PDF');
      const blob = await response.blob();
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `ecotrack_report_${new Date().toISOString().split('T')[0]}.pdf`;
      document.body.appendChild(a);
      a.click();
      a.remove();
      window.URL.revokeObjectURL(url);
    } catch (err) {
      alert('Error downloading report: ' + err.message);
    }
  };

  // Helper for computing SVG Chart Path
  const renderTrendChart = () => {
    if (!dashboardData || !dashboardData.weeklyTrend || dashboardData.weeklyTrend.length === 0) {
      return <div className="empty-state">No trend data available. Log activities to start tracking.</div>;
    }

    const trend = dashboardData.weeklyTrend;
    const maxVal = Math.max(...trend.map(d => d.carbon), 5); // Baseline of 5
    const height = 150;
    const width = 500;
    const paddingX = 40;
    const paddingY = 20;

    const points = trend.map((d, i) => {
      const x = paddingX + (i / (trend.length - 1)) * (width - paddingX * 2);
      const y = height - paddingY - (d.carbon / maxVal) * (height - paddingY * 2);
      return { x, y, date: d.date, carbon: d.carbon };
    });

    const linePath = points.reduce((acc, p, i) => {
      return acc + (i === 0 ? `M ${p.x} ${p.y}` : ` L ${p.x} ${p.y}`);
    }, '');

    const areaPath = points.length > 0
      ? `${linePath} L ${points[points.length - 1].x} ${height - paddingY} L ${points[0].x} ${height - paddingY} Z`
      : '';

    return (
      <div className="chart-container">
        <svg viewBox={`0 0 ${width} ${height}`} className="chart-svg">
          <defs>
            <linearGradient id="chart-gradient" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stopColor="#10b981" stopOpacity="0.4" />
              <stop offset="100%" stopColor="#10b981" stopOpacity="0.0" />
            </linearGradient>
          </defs>
          {/* Grid lines */}
          <line x1={paddingX} y1={paddingY} x2={width - paddingX} y2={paddingY} className="chart-grid-line" />
          <line x1={paddingX} y1={height / 2} x2={width - paddingX} y2={height / 2} className="chart-grid-line" />
          <line x1={paddingX} y1={height - paddingY} x2={width - paddingX} y2={height - paddingY} className="chart-grid-line" />

          {/* Area fill */}
          {areaPath && <path d={areaPath} className="chart-area" />}

          {/* Trend line */}
          {linePath && <path d={linePath} className="chart-path" />}

          {/* Highlight Points */}
          {points.map((p, i) => {
            // Get date suffix
            const dateObj = new Date(p.date);
            const dateStr = dateObj.toLocaleDateString(undefined, { month: 'short', day: 'numeric' });
            return (
              <g key={i}>
                <circle cx={p.x} cy={p.y} r="5" className="chart-point" />
                {/* Labels */}
                <text x={p.x} y={height - 4} className="chart-text">{dateStr}</text>
                <text x={p.x} y={p.y - 8} className="chart-text" style={{ fontWeight: '600', fill: 'var(--text-main)' }}>
                  {p.carbon.toFixed(1)}
                </text>
              </g>
            );
          })}
        </svg>
      </div>
    );
  };

  // If not logged in, render Auth Form
  if (!token) {
    return (
      <div className="auth-wrapper">
        <div className="auth-card">
          <div style={{ display: 'flex', justifyContent: 'center', marginBottom: '12px', fontSize: '48px' }}>🌱</div>
          <h1 className="auth-title">EcoTrack</h1>
          <p className="auth-subtitle">Monitor and reduce your daily carbon footprint</p>
          
          {authError && <div className="alert-error">{authError}</div>}
          {authSuccess && <div className="alert-success">{authSuccess}</div>}

          <form onSubmit={isRegister ? handleRegister : handleLogin}>
            <div className="form-group">
              <label className="form-label">Email Address</label>
              <input
                type="email"
                required
                className="form-input"
                placeholder="you@example.com"
                value={authEmail}
                onChange={(e) => setAuthEmail(e.target.value)}
              />
            </div>
            <div className="form-group">
              <label className="form-label">Password</label>
              <input
                type="password"
                required
                className="form-input"
                placeholder="••••••••"
                value={authPassword}
                onChange={(e) => setAuthPassword(e.target.value)}
              />
            </div>
            {isRegister && (
              <div className="form-group">
                <label className="form-label">Phone Number (Optional)</label>
                <input
                  type="tel"
                  className="form-input"
                  placeholder="+1 (555) 000-0000"
                  value={authPhone}
                  onChange={(e) => setAuthPhone(e.target.value)}
                />
              </div>
            )}
            <button type="submit" className="btn-primary">
              {isRegister ? 'Create Account' : 'Sign In'}
            </button>
          </form>

          <div className="auth-switch">
            {isRegister ? (
              <>
                Already have an account?{' '}
                <a href="#" className="auth-link" onClick={() => { setIsRegister(false); setAuthError(''); }}>
                  Sign In
                </a>
              </>
            ) : (
              <>
                Don't have an account?{' '}
                <a href="#" className="auth-link" onClick={() => { setIsRegister(true); setAuthError(''); }}>
                  Sign Up
                </a>
              </>
            )}
          </div>
        </div>
      </div>
    );
  }

  // Loaded constants for score circle calculation
  const score = dashboardData ? dashboardData.ecoScore : 0;
  const circumference = 2 * Math.PI * 50; // r = 50 -> 314.159
  const dashOffset = circumference - (score / 100) * circumference;

  return (
    <div className="app-container">
      {/* Header Navbar */}
      <nav className="navbar">
        <div className="nav-brand">
          <span>🌱</span>
          <span>EcoTrack</span>
        </div>
        <div className="nav-menu">
          <button
            className={`nav-item ${activeTab === 'dashboard' ? 'active' : ''}`}
            onClick={() => setActiveTab('dashboard')}
          >
            Dashboard
          </button>
          <button
            className={`nav-item ${activeTab === 'log' ? 'active' : ''}`}
            onClick={() => setActiveTab('log')}
          >
            Log Activity
          </button>
          <button
            className={`nav-item ${activeTab === 'history' ? 'active' : ''}`}
            onClick={() => setActiveTab('history')}
          >
            History
          </button>
        </div>
        <div className="nav-user">
          <span className="user-email">{userEmail}</span>
          <button className="btn-secondary" onClick={handleLogout}>
            Logout
          </button>
        </div>
      </nav>

      {/* Main Tabs */}
      {activeTab === 'dashboard' && (
        <div className="dashboard-grid">
          <div className="dashboard-left">
            {/* Today's Carbon footprint overview */}
            <div className="widget-card">
              <div className="widget-header">
                <span>Overview Status</span>
                <span style={{ fontSize: '14px', color: 'var(--text-muted)' }}>
                  {new Date().toLocaleDateString(undefined, { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' })}
                </span>
              </div>
              <div className="overview-card-content">
                <div className="score-circle">
                  <svg className="score-svg">
                    <circle cx="65" cy="65" r="50" className="score-bg-circle" />
                    <circle
                      cx="65"
                      cy="65"
                      r="50"
                      className="score-fill-circle"
                      strokeDasharray={circumference}
                      strokeDashoffset={dashOffset}
                    />
                  </svg>
                  <div className="score-text">
                    <span className="score-num">{score}</span>
                    <span className="score-lbl">Eco Score</span>
                  </div>
                </div>
                <div className="emission-details">
                  <span className="form-label" style={{ marginBottom: '2px', color: 'var(--text-muted)' }}>TODAY'S EMISSION</span>
                  <div className="emission-value">
                    {dashboardData ? dashboardData.todayCarbon.toFixed(2) : '0.00'} <span>kg CO₂</span>
                  </div>
                  {todayActivity ? (
                    <span style={{ fontSize: '13px', color: 'var(--primary)', fontWeight: '600' }}>
                      ✓ Activity logged for today
                    </span>
                  ) : (
                    <span style={{ fontSize: '13px', color: 'var(--accent-orange)', fontWeight: '600' }}>
                      ⚠ You haven't logged today's activity yet
                    </span>
                  )}
                </div>
              </div>
            </div>

            {/* Weekly Trend Line Chart */}
            <div className="widget-card">
              <div className="widget-header">
                <span>Weekly Carbon Trend</span>
                <span style={{ fontSize: '12px', color: 'var(--text-muted)' }}>Last 7 Days (kg CO₂)</span>
              </div>
              {renderTrendChart()}
            </div>

            {/* Badges shelves */}
            <div className="widget-card">
              <div className="widget-header">
                <span>Achievements & Badges</span>
              </div>
              <div className="badge-shelf">
                <div className={`badge-item ${dashboardData?.badges.includes('ECO_BEGINNER') ? 'earned' : 'locked'}`}>
                  <span className="badge-icon">🔰</span>
                  <span className="badge-name">Eco Beginner</span>
                  <span className="badge-status">
                    {dashboardData?.badges.includes('ECO_BEGINNER') ? 'Earned' : 'Locked'}
                  </span>
                </div>
                <div className={`badge-item ${dashboardData?.badges.includes('GREEN_WARRIOR') ? 'earned' : 'locked'}`}>
                  <span className="badge-icon">⚔️</span>
                  <span className="badge-name">Green Warrior</span>
                  <span className="badge-status">
                    {dashboardData?.badges.includes('GREEN_WARRIOR') ? 'Earned' : 'Locked'}
                  </span>
                </div>
                <div className={`badge-item ${dashboardData?.badges.includes('SUSTAINABILITY_HERO') ? 'earned' : 'locked'}`}>
                  <span className="badge-icon">🦸‍♂️</span>
                  <span className="badge-name">Eco Hero</span>
                  <span className="badge-status">
                    {dashboardData?.badges.includes('SUSTAINABILITY_HERO') ? 'Earned' : 'Locked'}
                  </span>
                </div>
              </div>
            </div>
          </div>

          <div className="dashboard-right">
            {/* Streaks Card */}
            <div className="widget-card">
              <div className="widget-header">
                <span>Tracking Streaks</span>
              </div>
              <div className="streak-wrapper">
                <span className="streak-icon">🔥</span>
                <div className="streak-values">
                  <div className="streak-current">{dashboardData?.currentStreak || 0} Days Current</div>
                  <div className="streak-max">Record Max Streak: {dashboardData?.maxStreak || 0} days</div>
                </div>
              </div>
            </div>

            {/* Daily Challenge Card */}
            <div className="widget-card challenge-card">
              <div className="widget-header">
                <span>Today's Daily Challenge</span>
              </div>
              <p className="challenge-text">
                {dashboardData?.dailyChallenge || 'Log your activities to see challenges.'}
              </p>
            </div>

            {/* Category Breakdown Progress indicators */}
            <div className="widget-card">
              <div className="widget-header">
                <span>Emissions Breakdown</span>
                <span style={{ fontSize: '12px', color: 'var(--text-muted)' }}>Today</span>
              </div>
              <div className="breakdown-wrapper">
                {todayActivity && dashboardData ? (
                  <div className="breakdown-bars">
                    {/* Transport Bar */}
                    <div className="breakdown-row">
                      <span className="category-name">🚗 Commute</span>
                      <div className="progress-bar-container">
                        <div
                          className="progress-bar-fill"
                          style={{
                            width: `${Math.min(100, (dashboardData.categoryBreakdown.transport / Math.max(dashboardData.todayCarbon, 1)) * 100)}%`,
                            backgroundColor: 'var(--accent-blue)',
                          }}
                        />
                      </div>
                      <span className="category-value">{dashboardData.categoryBreakdown.transport.toFixed(1)} kg</span>
                    </div>

                    {/* Food Bar */}
                    <div className="breakdown-row">
                      <span className="category-name">🍔 Diet</span>
                      <div className="progress-bar-container">
                        <div
                          className="progress-bar-fill"
                          style={{
                            width: `${Math.min(100, (dashboardData.categoryBreakdown.food / Math.max(dashboardData.todayCarbon, 1)) * 100)}%`,
                            backgroundColor: 'var(--accent-orange)',
                          }}
                        />
                      </div>
                      <span className="category-value">{dashboardData.categoryBreakdown.food.toFixed(1)} kg</span>
                    </div>

                    {/* Electricity Bar */}
                    <div className="breakdown-row">
                      <span className="category-name">⚡ Power</span>
                      <div className="progress-bar-container">
                        <div
                          className="progress-bar-fill"
                          style={{
                            width: `${Math.min(100, (dashboardData.categoryBreakdown.electricity / Math.max(dashboardData.todayCarbon, 1)) * 100)}%`,
                            backgroundColor: 'var(--accent-purple)',
                          }}
                        />
                      </div>
                      <span className="category-value">{dashboardData.categoryBreakdown.electricity.toFixed(1)} kg</span>
                    </div>

                    {/* Plastic Bar */}
                    <div className="breakdown-row">
                      <span className="category-name">🥤 Plastic</span>
                      <div className="progress-bar-container">
                        <div
                          className="progress-bar-fill"
                          style={{
                            width: `${Math.min(100, (dashboardData.categoryBreakdown.plastic / Math.max(dashboardData.todayCarbon, 1)) * 100)}%`,
                            backgroundColor: 'var(--accent-red)',
                          }}
                        />
                      </div>
                      <span className="category-value">{dashboardData.categoryBreakdown.plastic.toFixed(2)} kg</span>
                    </div>
                  </div>
                ) : (
                  <div className="empty-state">
                    No breakdown available. Please log today's activity.
                  </div>
                )}
              </div>
            </div>

            {/* Suggestions list */}
            <div className="widget-card">
              <div className="widget-header">
                <span>Recommendations</span>
              </div>
              <ul className="bullet-list">
                {dashboardData?.suggestions.map((item, idx) => (
                  <li key={idx} className="bullet-item">
                    <span className="bullet-icon">💡</span>
                    <span>{item}</span>
                  </li>
                ))}
              </ul>
            </div>

            {/* Insights list */}
            <div className="widget-card">
              <div className="widget-header">
                <span>Weekly Insights</span>
              </div>
              <ul className="bullet-list">
                {dashboardData?.insights.map((item, idx) => (
                  <li key={idx} className="bullet-item info">
                    <span className="bullet-icon">📈</span>
                    <span>{item}</span>
                  </li>
                ))}
              </ul>
            </div>
          </div>
        </div>
      )}

      {activeTab === 'log' && (
        <div className="widget-card form-card">
          <div className="widget-header">
            <span>Log Daily Activity</span>
          </div>
          {formError && <div className="alert-error">{formError}</div>}
          {formSuccess && <div className="alert-success">{formSuccess}</div>}

          <form onSubmit={handleLogActivity}>
            <div className="form-group">
              <label className="form-label">Activity Date</label>
              <input
                type="date"
                required
                className="form-input"
                value={formDate}
                onChange={(e) => setFormDate(e.target.value)}
              />
            </div>

            <div className="form-grid">
              <div className="form-group">
                <label className="form-label">Transport Type</label>
                <select
                  className="form-select"
                  value={transportType}
                  onChange={(e) => setTransportType(e.target.value)}
                >
                  <option value="NONE">Walk/Cycle/None</option>
                  <option value="CAR">Car (Petrol/Diesel)</option>
                  <option value="BIKE">Motorbike</option>
                  <option value="BUS">Public Bus</option>
                </select>
              </div>

              <div className="form-group">
                <label className="form-label">Travel Distance (km)</label>
                <input
                  type="number"
                  min="0"
                  step="0.1"
                  required
                  className="form-input"
                  value={transportDistance}
                  onChange={(e) => setTransportDistance(e.target.value)}
                />
              </div>
            </div>

            <div className="form-grid">
              <div className="form-group">
                <label className="form-label">Electricity Usage (Units/kWh)</label>
                <input
                  type="number"
                  min="0"
                  step="0.1"
                  required
                  className="form-input"
                  value={electricityUnits}
                  onChange={(e) => setElectricityUnits(e.target.value)}
                />
              </div>

              <div className="form-group">
                <label className="form-label">Food Habits</label>
                <select
                  className="form-select"
                  value={foodHabit}
                  onChange={(e) => setFoodHabit(e.target.value)}
                >
                  <option value="VEG">Vegetarian / Vegan Diet</option>
                  <option value="NON_VEG">Non-Vegetarian Diet</option>
                </select>
              </div>
            </div>

            <div className="form-group">
              <label className="form-label">Single-use Plastic Items Used</label>
              <input
                type="number"
                min="0"
                required
                className="form-input"
                value={plasticItems}
                onChange={(e) => setPlasticItems(e.target.value)}
              />
            </div>

            <button type="submit" className="btn-primary" disabled={loading}>
              {loading ? 'Logging...' : 'Save Activity Entry'}
            </button>
          </form>
        </div>
      )}

      {activeTab === 'history' && (
        <div className="widget-card">
          <div className="history-header">
            <h2 style={{ fontSize: '20px', fontWeight: '700' }}>Logged Carbon History</h2>
            {historyList.length > 0 && (
              <button className="btn-pdf" onClick={handleDownloadReport}>
                <span>📄</span> Export PDF Report
              </button>
            )}
          </div>
          {historyList.length === 0 ? (
            <div className="empty-state">
              <div className="empty-icon">📂</div>
              <h3>No activity history found</h3>
              <p style={{ marginTop: '4px' }}>Log your daily emissions to populate the tracker history log.</p>
            </div>
          ) : (
            <div className="table-wrapper">
              <table className="history-table">
                <thead>
                  <tr>
                    <th>Date</th>
                    <th>Commute (Type & Distance)</th>
                    <th>Commute CO₂</th>
                    <th>Power CO₂</th>
                    <th>Diet CO₂</th>
                    <th>Plastic CO₂</th>
                    <th>Total Carbon</th>
                  </tr>
                </thead>
                <tbody>
                  {historyList.map((item) => (
                    <tr key={item.id}>
                      <td style={{ fontWeight: '600' }}>{item.date}</td>
                      <td>{item.transportType} ({item.transportDistance} km)</td>
                      <td>{item.transportCarbon.toFixed(2)} kg</td>
                      <td>{item.electricityCarbon.toFixed(2)} kg</td>
                      <td>{item.foodCarbon.toFixed(2)} kg</td>
                      <td>{item.plasticCarbon.toFixed(2)} kg</td>
                      <td style={{ fontWeight: '700', color: 'var(--primary)' }}>
                        {item.carbonFootprintTotal.toFixed(2)} kg CO₂
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}
    </div>
  );
}

export default App;
