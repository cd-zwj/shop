const DEFAULT_API_BASE_URL = 'http://localhost:8080/api';

export const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL?.trim() || DEFAULT_API_BASE_URL;

