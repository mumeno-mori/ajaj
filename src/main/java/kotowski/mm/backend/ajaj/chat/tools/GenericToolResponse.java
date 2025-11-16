package kotowski.mm.backend.ajaj.chat.tools;

public record GenericToolResponse<T>(
        boolean success,
        String errorMessage,
        T data) {
}
