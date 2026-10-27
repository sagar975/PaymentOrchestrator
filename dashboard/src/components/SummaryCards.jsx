import React from 'react';

/**
 * Displays 5 summary metric cards at the top of dashboard.
 */
const SummaryCards = ({ data, loading }) => {
    if (loading) {
        return (
            <div style={styles.grid}>
                {[1,2,3,4,5].map(i => (
                    <div key={i} style={{...styles.card, ...styles.skeleton}} />
                ))}
            </div>
        );
    }

    const cards = [
        {
            title: 'Total Transactions',
            value: data?.totalTransactions ?? 0,
            icon: '💳',
            color: '#1e3a5f'
        },
        {
            title: 'Successful',
            value: data?.successfulTransactions ?? 0,
            icon: '✅',
            color: '#166534'
        },
        {
            title: 'Failed',
            value: data?.failedTransactions ?? 0,
            icon: '❌',
            color: '#991b1b'
        },
        {
            title: 'Success Rate',
            value: `${data?.successRate ?? 0}%`,
            icon: '📈',
            color: '#1d4ed8'
        },
        {
            title: 'Total Volume',
            value: data?.totalVolume
                ? parseFloat(data.totalVolume).toLocaleString('en-IN', {
                    minimumFractionDigits: 2,
                    maximumFractionDigits: 2
                  })
                : '0.00',
            icon: '💰',
            color: '#92400e'
        }
    ];

    return (
        <div style={styles.grid}>
            {cards.map((card, index) => (
                <div key={index} style={styles.card}>
                    <div style={styles.iconRow}>
                        <span style={styles.icon}>{card.icon}</span>
                        <span style={{...styles.title}}>{card.title}</span>
                    </div>
                    <div style={{...styles.value, color: card.color}}>
                        {card.value}
                    </div>
                </div>
            ))}
        </div>
    );
};

const styles = {
    grid: {
        display: 'grid',
        gridTemplateColumns: 'repeat(5, 1fr)',
        gap: '16px',
        marginBottom: '24px'
    },
    card: {
        backgroundColor: '#ffffff',
        borderRadius: '12px',
        padding: '20px',
        boxShadow: '0 1px 3px rgba(0,0,0,0.1)',
        border: '1px solid #e2e8f0'
    },
    skeleton: {
        height: '100px',
        backgroundColor: '#f1f5f9',
        animation: 'pulse 1.5s infinite'
    },
    iconRow: {
        display: 'flex',
        alignItems: 'center',
        gap: '8px',
        marginBottom: '12px'
    },
    icon: {
        fontSize: '20px'
    },
    title: {
        fontSize: '13px',
        color: '#64748b',
        fontWeight: '500'
    },
    value: {
        fontSize: '28px',
        fontWeight: '700',
        color: '#1e293b'
    }
};

export default SummaryCards;