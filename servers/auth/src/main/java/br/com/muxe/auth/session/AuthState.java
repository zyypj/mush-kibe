package br.com.muxe.auth.session;

public enum AuthState {
    LOADING,
    CHALLENGE,
    AWAITING_REGISTER,
    AWAITING_LOGIN,
    PROCESSING,
    AUTHENTICATED
}
