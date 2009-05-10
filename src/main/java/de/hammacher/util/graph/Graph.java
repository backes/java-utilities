package de.hammacher.util.graph;

import java.util.Collection;


/**
 * A generic graph interface.
 *
 * @author Clemens Hammacher
 * @param <NodeType> the type of the nodes. must implements {@link Graph.Node}.
 */
public interface Graph<NodeType extends Graph.Node<NodeType>> {

	/**
	 * A node which is contained in a {@link Graph}.
	 *
	 * @author Clemens Hammacher
	 *
	 * @param <NodeType> a more exact type of the nodes. A type implementing
	 * this interface could for example look like that:
	 *
	 * <code>class MyNodeType extends Graph.Node<MyNodeType> { ...</code>
	 *
	 * The benefit is that the graph knows the type of its nodes and for example
	 * the {@link EdgeLabelProvider} does not need any casting to access specific
	 * fields or methods of the nodes' type.
	 */
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
