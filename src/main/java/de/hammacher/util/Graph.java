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

	public static interface EdgeLabelProvider<NodeType> {

		/**
		 * Determine the label for an edge.
		 *
		 * @param startNode the source node of the edge
		 * @param endNode the target node of the edge
		 * @return a label for the given edge (or <code>null</code> if the edge has no label)
		 */
		String getLabel(NodeType startNode, NodeType endNode);

	}

	/**
	 * Returns a collection of all nodes of this graph.
	 *
	 * @return a collection of all nodes of this graph
	 */
	Collection<? extends NodeType> getNodes();

}
