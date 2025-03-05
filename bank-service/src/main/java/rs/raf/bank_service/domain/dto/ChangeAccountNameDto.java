package rs.raf.bank_service.domain.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Getter
@Setter
@NoArgsConstructor
public class ChangeAccountNameDto {

    @NotNull(message = "New account name cannot be null")
    @Size(min = 3, max = 50, message = "Account name must be between 3 and 50 characters")
    private String newName;

    public ChangeAccountNameDto(String newName) {
        this.newName = newName;
    }
}
