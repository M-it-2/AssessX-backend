package AssessX_backend.dto;

import java.time.LocalDateTime;

public class HintResponseDto {

    private final String hint;
    private final LocalDateTime hintUsedAt;

    public HintResponseDto(String hint, LocalDateTime hintUsedAt) {
        this.hint = hint;
        this.hintUsedAt = hintUsedAt;
    }

    public String getHint() { return hint; }
    public LocalDateTime getHintUsedAt() { return hintUsedAt; }
}
