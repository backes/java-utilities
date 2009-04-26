package de.hammacher.util;

import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

import de.hammacher.util.Filter.FilterNone;
import de.hammacher.util.Graph.Node;


public class GraphUtils {

	private static class DFSIterator<NodeType extends Node<NodeType>> implements Iterator<NodeType> {

		private final UniqueQueue<NodeType> queue = new UniqueQueue<NodeType>();

		public DFSIterator(NodeType startNode) {
			this.queue.add(startNode);
		}

		public boolean hasNext() {
			return !this.queue.isEmpty();
		}

		public NodeType next() {
			NodeType ret = this.queue.poll();
			if (ret == null)
				throw new NoSuchElementException();
			this.queue.addAll(ret.getSuccessors());
			return ret;
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}

	}

	private GraphUtils() {
		// private
	}

	public static <NodeType extends Node<NodeType>> Set<NodeType> getReachable(NodeType startNode) {
		return getReachable(startNode, FilterNone.get());
	}

	public static <NodeType extends Node<NodeType>> Set<NodeType> getReachable(NodeType startNode, Filter<? super NodeType> filter) {
		if (filter.filter(startNode))
			return Collections.emptySet();

		UniqueQueue<NodeType> queue = new UniqueQueue<NodeType>();
		queue.add(startNode);
		NodeType node;
		while ((node = queue.poll()) != null) {
			for (NodeType succ : node.getSuccessors())
				if (!filter.filter(succ))
					queue.add(succ);
		}

		return queue.getSeenElements();
	}

	public static <NodeType extends Node<NodeType>> void DFS(NodeType startNode,
			Visitor<? super NodeType> visitor) {
		UniqueQueue<NodeType> queue = new UniqueQueue<NodeType>();
		queue.add(startNode);
		NodeType node;
		while ((node = queue.poll()) != null) {
			visitor.visit(node);
			queue.addAll(node.getSuccessors());
		}
	}

	public static <NodeType extends Node<NodeType>> Iterator<NodeType> getDFSIterator(NodeType startNode) {
		return new DFSIterator<NodeType>(startNode);
	}

}
