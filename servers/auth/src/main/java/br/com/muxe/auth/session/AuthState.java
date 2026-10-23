package br.com.muxe.auth.session;

public enum AuthState {
    LOADING,
    AWAITING_REGISTER,
    AWAITING_LOGIN,
    PROCESSING,
    AUTHENTICATED
}
