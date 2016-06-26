public class Foo {
	public static void main(String[] args) {
		Foo f = new Foo();
		int a = 7;
		int b = 14;
		int x = (f.bar(21) + a) * b;
		System.out.println(x);
		int[] m = new int[100];
		for(int i=0; i<m.length; i++)
			m[i] = 0;

	}

	public int bar(int n) { return n + 42; }
}
