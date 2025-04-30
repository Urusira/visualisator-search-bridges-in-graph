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
        return "["+X+", "+Y+"]";
    }

    public static double minus(Coords a, Coords b) {
        return Math.sqrt(Math.pow((b.getX() - a.getX()), 2)+Math.pow(b.getY()- a.getY(), 2));
    }
}