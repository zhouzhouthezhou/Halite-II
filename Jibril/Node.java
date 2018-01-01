import hlt.Entity;

public class Node {
	private double x;
	private double y;
	private boolean isDiagonal;

	public Node(double x, double y){
		this.x = x;
		this.y = y;
		isDiagonal = false;
	}

	public double getX() {
		return x;
	}

	public double getY() {
		return y;
	}

	public boolean getIsDiagonal() {
		return isDiagonal;
	}
	
	public void setIsDiagonal(boolean isDiagonal) {
		this.isDiagonal = isDiagonal;
	}
	
	public String toString() {
		return "(" + x + ", " + y + ")";
	}
}
