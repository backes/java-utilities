package de.hammacher.util.graph;

/**
 * Class to provide labels for the nodes. Used for example when exporting
 * a {@link Graph} to dot via {@link Graph2Dot}.
 *
 * @author Clemens Hammacher
 *
 * @param <NodeType> the type of the graphs nodes
 */
public interface NodeLabelProvider<NodeType> {

	/**
	 * Determine the label for a node.
	 *
	 * @param startNode the node for which the label should be computed
	 * @return a label for the given node (or <code>null</code> if the node has no label)
	 */
	String getNodeLabel(NodeType node);

}