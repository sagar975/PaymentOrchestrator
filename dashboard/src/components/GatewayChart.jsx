import React from 'react';
import {
    BarChart, Bar, XAxis, YAxis, CartesianGrid,
    Tooltip, Legend, ResponsiveContainer
} from 'recharts';

/**
 * Bar chart showing success/failure per gateway.
 */
const GatewayChart = ({ data, loading }) => {
    if (loading) {
        return <div style={styles.skeleton} />;
    }

    const chartData = data?.gateways?.map(g => ({
        name: g.gatewayName.charAt(0).toUpperCase()
            + g.gatewayName.slice(1),
        Success: g.successfulTransactions,
        Failed: g.failedTransactions,
        'Success Rate': g.successRate
    })) ?? [];

    return (
        <div style={styles.card}>
            <h3 style={styles.title}>Gateway Performance</h3>
            <ResponsiveContainer width="100%" height={300}>
                <BarChart data={chartData}
                    margin={{ top: 10, right: 30, left: 0, bottom: 5 }}>
                    <CartesianGrid strokeDasharray="3 3"
                        stroke="#f1f5f9" />
                    <XAxis dataKey="name"
                        tick={{ fontSize: 13, fill: '#64748b' }} />
                    <YAxis
                        tick={{ fontSize: 12, fill: '#64748b' }} />
                    <Tooltip
                        contentStyle={{
                            borderRadius: '8px',
                            border: '1px solid #e2e8f0'
                        }} />
                    <Legend />
                    <Bar dataKey="Success" fill="#16a34a"
                        radius={[4, 4, 0, 0]} />
                    <Bar dataKey="Failed" fill="#dc2626"
                        radius={[4, 4, 0, 0]} />
                </BarChart>
            </ResponsiveContainer>

            {/* Gateway stats table below chart */}
            <table style={styles.table}>
                <thead>
                    <tr style={styles.tableHeader}>
                        <th style={styles.th}>Gateway</th>
                        <th style={styles.th}>Total</th>
                        <th style={styles.th}>Success</th>
                        <th style={styles.th}>Failed</th>
                        <th style={styles.th}>Success Rate</th>
                        <th style={styles.th}>Volume</th>
                    </tr>
                </thead>
                <tbody>
                    {data?.gateways?.map((g, i) => (
                        <tr key={i} style={i % 2 === 0
                            ? styles.rowEven : styles.rowOdd}>
                            <td style={styles.td}>
                                <span style={styles.gatewayBadge}>
                                    {g.gatewayName}
                                </span>
                            </td>
                            <td style={styles.td}>
                                {g.totalTransactions}
                            </td>
                            <td style={{...styles.td, color: '#16a34a',
                                fontWeight: '600'}}>
                                {g.successfulTransactions}
                            </td>
                            <td style={{...styles.td, color: '#dc2626',
                                fontWeight: '600'}}>
                                {g.failedTransactions}
                            </td>
                            <td style={styles.td}>
                                <span style={{
                                    ...styles.rateBadge,
                                    backgroundColor:
                                        g.successRate >= 80
                                            ? '#dcfce7' : '#fee2e2',
                                    color: g.successRate >= 80
                                        ? '#166534' : '#991b1b'
                                }}>
                                    {g.successRate}%
                                </span>
                            </td>
                            <td style={styles.td}>
                                {parseFloat(g.totalVolume)
                                    .toLocaleString('en-IN', {
                                        minimumFractionDigits: 2
                                    })}
                            </td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    );
};

const styles = {
    card: {
        backgroundColor: '#ffffff',
        borderRadius: '12px',
        padding: '24px',
        boxShadow: '0 1px 3px rgba(0,0,0,0.1)',
        border: '1px solid #e2e8f0',
        marginBottom: '24px'
    },
    skeleton: {
        height: '400px',
        backgroundColor: '#f1f5f9',
        borderRadius: '12px',
        marginBottom: '24px'
    },
    title: {
        fontSize: '16px',
        fontWeight: '600',
        color: '#1e293b',
        marginBottom: '20px',
        marginTop: 0
    },
    table: {
        width: '100%',
        borderCollapse: 'collapse',
        marginTop: '20px',
        fontSize: '14px'
    },
    tableHeader: {
        backgroundColor: '#f8fafc'
    },
    th: {
        padding: '10px 14px',
        textAlign: 'left',
        fontSize: '12px',
        fontWeight: '600',
        color: '#64748b',
        borderBottom: '1px solid #e2e8f0',
        textTransform: 'uppercase',
        letterSpacing: '0.05em'
    },
    td: {
        padding: '10px 14px',
        color: '#1e293b',
        borderBottom: '1px solid #f1f5f9'
    },
    rowEven: { backgroundColor: '#ffffff' },
    rowOdd: { backgroundColor: '#f8fafc' },
    gatewayBadge: {
        backgroundColor: '#eff6ff',
        color: '#1d4ed8',
        padding: '2px 8px',
        borderRadius: '4px',
        fontSize: '12px',
        fontWeight: '600'
    },
    rateBadge: {
        padding: '2px 8px',
        borderRadius: '4px',
        fontSize: '12px',
        fontWeight: '600'
    }
};

export default GatewayChart;