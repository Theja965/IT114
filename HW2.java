public class HW2 {
  
	public static int sum(int num) {
		if (num >= 2) {
			return num + sum(num - 1);
		}
		return 0;
	}

	public static void main(String[] args) {
		System.out.println(sum(13));
	}
}
