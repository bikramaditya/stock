class Triangle {
	public static double area(Point A, Point B, Point C) {
		double area = (A.x * (B.y - C.y) + B.x * (C.y - A.y) + C.x * (A.y - B.y))/2.0d;
		return Math.abs(area);
	}
}