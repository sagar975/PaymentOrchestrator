import React, { useState, useEffect, useCallback } from 'react';
import SummaryCards from './components/SummaryCards';
import GatewayChart from './components/GatewayChart';
import { dashboardApi } from './services/api';

function App() {
    const [summary, setSummary] = useState(null);
    const [gatewayPerformance, setGatewayPerformance] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [lastRefresh, setLastRefresh] = useState(new Date());

    const fetchData = useCallback(async () => {
        try {
            setError(null);
            const [summaryRes, gatewayRes] = await Promise.all([
                dashboardApi.getSummary(),
                dashboardApi.getGatewayPerformance()
            ]);
            setSummary(summaryRes.data);
            setGatewayPerformance(gatewayRes.data);
            setLastRefresh(new Date());
        } catch (err) {
            setError('Failed to load dashboard data. ' +
                'Make sure the API server is running on port 8080.');
            console.error('Dashboard error:', err);
        } finally {
            setLoading(false);
        }
    }, []);

    // Load on mount
    useEffect(() => {
        fetchData();
    }, [fetchData]);

    // Auto-refresh every 30 seconds
    useEffect(() => {
        const interval = setInterval(fetchData, 30000);
        return () => clearInterval(interval);
    }, [fetchData]);

    return (
        <div style={styles.app}>
            {/* Header */}
            <div style={styles.header}>
                <div style={styles.headerLeft}>
                    <span style={styles.logo}>💳</span>
                    <div>
                        <h1 style={styles.headerTitle}>
                            Payment Orchestration Dashboard
                        </h1>
                        <p style={styles.headerSubtitle}>
                            Real-time payment metrics across all gateways
                        </p>
                    </div>
                </div>
                <div style={styles.headerRight}>
                    <span style={styles.refreshTime}>
                        Last updated: {lastRefresh.toLocaleTimeString()}
                    </span>
                    <button style={styles.refreshBtn}
                        onClick={fetchData}>
                        🔄 Refresh
                    </button>
                </div>
            </div>

            {/* Main content */}
            <div style={styles.content}>
                {error && (
                    <div style={styles.errorBanner}>
                        ⚠️ {error}
                    </div>
                )}

                <SummaryCards data={summary} loading={loading} />
                <GatewayChart
                    data={gatewayPerformance}
                    loading={loading} />
            </div>

            {/* Footer */}
            <div style={styles.footer}>
                Payment Orchestration Platform — 
                Razorpay · PayU · Stripe · Checkout.com
            </div>
        </div>
    );
}

const styles = {
    app: {
        minHeight: '100vh',
        backgroundColor: '#f8fafc',
        fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif'
    },
    header: {
        backgroundColor: '#1e3a5f',
        padding: '20px 32px',
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center'
    },
    headerLeft: {
        display: 'flex',
        alignItems: 'center',
        gap: '16px'
    },
    logo: {
        fontSize: '36px'
    },
    headerTitle: {
        color: '#ffffff',
        fontSize: '22px',
        fontWeight: '700',
        margin: 0
    },
    headerSubtitle: {
        color: '#93c5fd',
        fontSize: '13px',
        margin: '4px 0 0 0'
    },
    headerRight: {
        display: 'flex',
        alignItems: 'center',
        gap: '16px'
    },
    refreshTime: {
        color: '#93c5fd',
        fontSize: '13px'
    },
    refreshBtn: {
        backgroundColor: '#2563eb',
        color: '#ffffff',
        border: 'none',
        borderRadius: '8px',
        padding: '8px 16px',
        fontSize: '13px',
        cursor: 'pointer',
        fontWeight: '500'
    },
    content: {
        padding: '32px',
        maxWidth: '1400px',
        margin: '0 auto'
    },
    errorBanner: {
        backgroundColor: '#fee2e2',
        border: '1px solid #fecaca',
        borderRadius: '8px',
        padding: '12px 16px',
        marginBottom: '24px',
        color: '#991b1b',
        fontSize: '14px'
    },
    footer: {
        textAlign: 'center',
        padding: '20px',
        color: '#94a3b8',
        fontSize: '13px',
        borderTop: '1px solid #e2e8f0'
    }
};

export default App;