package de.hammacher.util;

import java.util.Collection;

/**
 * A generic graph interface.
 *
 * @author Clemens Hammacher
 * @param <NodeType> the type of the nodes. must implements {@link Graph.Node}.
 */
public interface Graph<NodeType extends Graph.Node<NodeType>> {

	public static interface Node<NodeType extends Node<NodeType>> {

		/**
		 * Returns a collection of all successors of this node.
		 * All these successors must be contained in the collection returned
		 * by <code>getGraph().getNodes()</code>.
		 *
		 * @return a collection of all successors of a node
		 */
		Collection<? extends NodeType> getSuccessors();

		String getLabel();

	}

	/**
	 * Returns a collection of all nodes of this graph.
	 *
	 * @return a collection of all nodes of this graph
	 */
	Collection<? extends NodeType> getNodes();

}
