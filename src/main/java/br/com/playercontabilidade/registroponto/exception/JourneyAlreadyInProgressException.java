package br.com.playercontabilidade.registroponto.exception;

public class JourneyAlreadyInProgressException extends RuntimeException {

    public JourneyAlreadyInProgressException(String message) {
        super(message);
    }
}
