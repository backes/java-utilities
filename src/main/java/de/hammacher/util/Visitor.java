package de.hammacher.util;


public interface Visitor<T> {

	public static class YVisitor<T> implements Visitor<T> {

		private final Visitor<? super T>[] visitors;

		public YVisitor(Visitor<? super T> ... visitors) {
			this.visitors = visitors;
		}

		public void visit(T obj) {
			for (Visitor<? super T> vis : this.visitors)
				vis.visit(obj);
		}

	}

	void visit(T obj);

}
