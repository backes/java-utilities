package de.hammacher.util.graph;

/**
 * Class to provide labels for the edges. Used for example when exporting
 * a {@link Graph} to dot via {@link Graph2Dot}.
 *
 * @author Clemens Hammacher
 *
 * @param <NodeType> the type of the graphs nodes
 */
public interface EdgeLabelProvider<NodeType> {

	/**
	 * Determine the label for an edge.
	 *
	 * @param startNode the source node of the edge
	 * @param endNode the target node of the edge
	 * @return a label for the given edge (or <code>null</code> if the edge has no label)
	 */
	String getLabel(NodeType startNode, NodeType endNode);

}