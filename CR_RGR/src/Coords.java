public class Coords {
    private double X;
    private double Y;

    public void setX(double x) {
        X = x;
    }

    public double getX() {
        return X;
    }

    public void setY(double y) {
        Y = y;
    }

    public double getY() {
        return Y;
    }

    public Coords(double xCoord, double yCoord) {
        X = xCoord;
        Y = yCoord;
    }

    @Override
    public String toString() {
        return X+", "+Y;
    }
}