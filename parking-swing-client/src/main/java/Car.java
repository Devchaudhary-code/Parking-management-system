public class Car {
    private final String plate;
    private final String type; // "CAR", "BIKE", etc.

    public Car(String plate, String type) {
        this.plate = plate;
        this.type = type;
    }

    public String getPlate() { return plate; }
    public String getType() { return type; }
}
