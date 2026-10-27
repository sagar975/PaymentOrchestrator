import axios from 'axios';

const BASE_URL = 'http://localhost:8080/api/v1';

const api = axios.create({
    baseURL: BASE_URL,
    timeout: 10000,
    headers: {
        'Content-Type': 'application/json'
    }
});

export const dashboardApi = {
    getSummary: () =>
        api.get('/dashboard/summary'),

    getGatewayPerformance: () =>
        api.get('/dashboard/gateway-performance'),

    getStatusBreakdown: () =>
        api.get('/dashboard/status-breakdown'),

    getRecentTransactions: (limit = 10) =>
        api.get(`/dashboard/recent-transactions?limit=${limit}`)
};

export default api;