const API_TYPE = import.meta.env.VITE_API_TYPE || 'aws';
const AWS_BASE_URL = import.meta.env.VITE_BASE_URL;
const K8S_BASE_URL = import.meta.env.VITE_BASE_URL2;
const getBaseUrl = () => (API_TYPE === "k8s" ? K8S_BASE_URL : AWS_BASE_URL) ;

const BASE_URL = getBaseUrl();

export const API = {
  SIGNUP_API: `${BASE_URL}${import.meta.env.VITE_SIGNUP_API}`,
  LOGIN_API: `${BASE_URL}${import.meta.env.VITE_LOGIN_API}`,
  LOCATIONS_OPTIONS: `${BASE_URL}${import.meta.env.VITE_LOCATIONS_OPTIONS}`,
  POPULAR_DISHES: `${BASE_URL}${import.meta.env.VITE_POPULAR_DISHES}`,
  LOCATIONS: `${BASE_URL}${import.meta.env.VITE_LOCATIONS}`,
  TABLES: `${BASE_URL}${import.meta.env.VITE_TABLES}`,
  BOOKING_CLIENTS: `${BASE_URL}${import.meta.env.VITE_BOOKING_CLIENTS}`,
  RESERVATIONS: `${BASE_URL}${import.meta.env.VITE_RESERVATIONS}`,
  DELETE_RESERVATION: `${BASE_URL}${import.meta.env.VITE_DELETE_RESERVATION}`,
  REVIEW_API: `${BASE_URL}${import.meta.env.VITE_REVIEW_API}`,
  GET_DISHES: `${BASE_URL}${import.meta.env.VITE_DISHES}`,
  USERS_PROFILE: `${BASE_URL}${import.meta.env.VITE_USERS_PROFILE}`,
  FEEDBACKS_POST: `${BASE_URL}${import.meta.env.VITE_FEEDBACKPOST}`,
  CUSTOMERS: `${BASE_URL}${import.meta.env.VITE_CUSTOMERS}`,
  WAITER_BOOKING: `${BASE_URL}${import.meta.env.VITE_BOOKING_WAITER}`,
  PASSWORD: `${BASE_URL}${import.meta.env.VITE_PASSWORD}`,
};
