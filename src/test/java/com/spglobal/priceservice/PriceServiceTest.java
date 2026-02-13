import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PriceServiceTest {

    @Test
    public void testGetPrice_WhenValidInput_ShouldReturnCorrectPrice() {
        // Arrange
        PriceService priceService = new PriceService();
        String validInput = "Product123";
        double expectedPrice = 99.99;

        // Act
        double actualPrice = priceService.getPrice(validInput);

        // Assert
        assertEquals(expectedPrice, actualPrice);
    }

    @Test
    public void testGetPrice_WhenInvalidInput_ShouldThrowException() {
        // Arrange
        PriceService priceService = new PriceService();
        String invalidInput = "InvalidProduct";

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            priceService.getPrice(invalidInput);
        });
    }

    @Test
    public void testGetPrice_WhenProductNotAvailable_ShouldReturnZero() {
        // Arrange
        PriceService priceService = new PriceService();
        String unavailableProduct = "NonExistentProduct";

        // Act
        double actualPrice = priceService.getPrice(unavailableProduct);

        // Assert
        assertEquals(0.0, actualPrice);
    }

    @Test
    public void testGetPrice_WithMultipleRequests_ShouldReturnConsistentResults() {
        // Arrange
        PriceService priceService = new PriceService();
        String product = "Product123";

        // Act
        double firstCall = priceService.getPrice(product);
        double secondCall = priceService.getPrice(product);

        // Assert
        assertEquals(firstCall, secondCall);
    }
}