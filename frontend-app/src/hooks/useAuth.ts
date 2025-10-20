export const useAuth = () => {
    const token = sessionStorage.getItem('token');
    const username = sessionStorage.getItem('username');
    const role = sessionStorage.getItem('role');

    return {
        isAuthenticated: !!token,
        role,
        username: username,
    }
};