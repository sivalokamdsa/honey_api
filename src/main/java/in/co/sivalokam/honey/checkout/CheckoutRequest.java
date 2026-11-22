package in.co.sivalokam.honey.checkout;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

public record CheckoutRequest(
        @NotEmpty(message = "Add at least one item")
        @Size(max = 20, message = "A cart can hold at most 20 different items")
        List<@Valid CartItemRequest> items,

        @NotNull(message = "Customer details are required")
        @Valid
        CustomerRequest customer
) {

    public record CartItemRequest(
            @NotBlank(message = "Product is required")
            @Size(max = 64)
            String productId,

            @NotNull(message = "Quantity is required")
            @Min(value = 1, message = "Quantity must be between 1 and 20")
            @Max(value = 20, message = "Quantity must be between 1 and 20")
            Integer quantity
    ) {}

    public record CustomerRequest(
            @NotBlank(message = "Name is required")
            @Size(min = 2, max = 100, message = "Name must be 2 to 100 characters")
            String name,

            @NotBlank(message = "Phone is required")
            @Pattern(regexp = "^(?:\\+?91|0)?[\\s-]*[6-9](?:[\\s-]*\\d){9}$",
                    message = "Enter a valid 10-digit Indian mobile number")
            String phone,

            @Email(message = "Enter a valid email")
            @Size(max = 254, message = "Email is too long")
            String email
    ) {}
}
